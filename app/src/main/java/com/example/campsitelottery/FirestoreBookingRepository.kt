package com.example.campsitelottery

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.Date

/**
 * Books a win and its per-night claims in one transaction. All booking writers must
 * use this protocol: external writers bypassing the claims are not serialized.
 * Existing reservations are also checked so pre-claim records remain unavailable.
 * Device identity/authorization still depends on the app's existing Firebase setup;
 * these client validations are not a replacement for server-side security rules.
 */
class FirestoreBookingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val now: () -> Date = { Date() },
) : BookingRepository {
    private val waitlists get() = db.collection("waitlists")
    private val campsites get() = db.collection("campsites")
    private val reservations get() = db.collection("reservations")

    override fun load(
        waitlistId: String,
        entrantId: String,
        onSuccess: (BookingOptions) -> Unit,
        onFailure: (Exception) -> Unit,
    ) = attempt(onFailure) {
        requireIds(waitlistId, entrantId)
        val reservationTask = reservations.document(BookingPolicy.reservationId(waitlistId, entrantId)).get(Source.SERVER)
        val task = reservationTask.continueWithTask { result ->
            val existing = result.result.toObject(Reservation::class.java)
            if (existing != null) {
                require(existing.entrantId == entrantId && existing.waitlistId == waitlistId)
                // Reopening a saved confirmation must not depend on other sites,
                // current lottery membership, or even the continued listing.
                Tasks.forResult(BookingOptions(Waitlist(waitlistId = waitlistId), emptyList(), emptyMap(), existing))
            } else {
                waitlists.document(waitlistId).get(Source.SERVER).continueWithTask { loaded ->
                    val waitlist = loaded.result.bookingWaitlist() ?: error("Lottery not found.")
                    BookingPolicy.requireWinner(waitlist, entrantId)
                    val ids = waitlist.campsiteIds.distinct()
                    require(ids.isNotEmpty() && ids.all { id -> validId(id) }) { "This lottery has no valid campsites." }
                    val siteTasks = ids.map { id -> campsites.document(id).get(Source.SERVER) }
                    val stayTasks = ids.map { id -> reservations.whereEqualTo("campsiteId", id).get(Source.SERVER) }
                    val claimTasks = ids.map { id -> campsites.document(id).collection("bookingDays").get(Source.SERVER) }
                    Tasks.whenAllComplete(siteTasks + stayTasks + claimTasks).continueWith {
                        val sites = siteTasks.mapNotNull { task -> task.result.toObject(Campsite::class.java) }
                        val unavailable = ids.indices.associate { index ->
                            val id = ids[index]
                            val stays = stayTasks[index].result.documents.associate { doc ->
                                doc.id to (doc.toObject(Reservation::class.java)
                                    ?: error("An existing reservation could not be read."))
                            }
                            val occupied = stays.values.flatMap(BookingPolicy::occupiedNights).toSet()
                            val claimed = claimTasks[index].result.documents.mapNotNull { claim ->
                                val date = BookingPolicy.parse(claim.id)
                                    ?: error("A booking claim has an invalid date. Contact the campsite owner.")
                                val holder = stays[claim.getString("reservationId")]
                                if (holder == null || holder.campsiteId != id || BookingPolicy.blocksDates(holder))
                                    BookingPolicy.day(date) else null
                            }
                            id to (occupied + claimed)
                        }
                        BookingOptions(waitlist, sites, unavailable)
                    }
                }
            }
        }
        finish(task, onSuccess, onFailure)
    }

    override fun confirm(
        request: BookingRequest,
        onSuccess: (Reservation) -> Unit,
        onFailure: (Exception) -> Unit,
    ) = attempt(onFailure) {
        requireIds(request.waitlistId, request.entrantId)
        require(validId(request.campsiteId)) { "Choose a valid campsite." }
        val nights = BookingPolicy.nights(request.checkIn, request.checkOut)
        val waitlistRef = waitlists.document(request.waitlistId)
        val siteRef = campsites.document(request.campsiteId)
        val reservationRef = reservations.document(BookingPolicy.reservationId(request.waitlistId, request.entrantId))
        // Android transactions cannot query a collection. Re-read the returned
        // legacy documents inside the transaction, while claims protect new writes.
        val task = reservations.whereEqualTo("campsiteId", request.campsiteId)
            .get(Source.SERVER).continueWithTask { result ->
                val legacyRefs = result.result.documents.map { it.reference }
                db.runTransaction { transaction ->
                    val waitlist = transaction.get(waitlistRef).bookingWaitlist() ?: error("Lottery not found.")
                    val existing = transaction.get(reservationRef).toObject(Reservation::class.java)
                    if (existing != null) {
                        require(existing.entrantId == request.entrantId && existing.waitlistId == request.waitlistId)
                        return@runTransaction existing
                    }
                    val site = transaction.get(siteRef).toObject(Campsite::class.java) ?: error("Campsite not found.")
                    val legacyNights = legacyRefs.flatMap { ref ->
                        transaction.get(ref).toObject(Reservation::class.java)
                            ?.let(BookingPolicy::occupiedNights).orEmpty()
                    }.toSet()
                    val claims = nights.map { night ->
                        siteRef.collection("bookingDays").document(BookingPolicy.format(Date(night)))
                    }
                    val claimSnapshots = claims.map { transaction.get(it) }
                    val linked = claimSnapshots.mapNotNull { it.getString("reservationId") }.distinct()
                        .associateWith { id ->
                            require(validId(id)) { "A booking claim is invalid. Contact the campsite owner." }
                            transaction.get(reservations.document(id)).toObject(Reservation::class.java)
                        }
                    val claimedNights = nights.zip(claimSnapshots).mapNotNull { (night, claim) ->
                        if (!claim.exists()) null
                        else {
                            val id = claim.getString("reservationId")
                                ?: error("A booking claim is invalid. Contact the campsite owner.")
                            val holder = linked[id]
                            // Missing linked records are ambiguous, so fail closed.
                            if (holder == null || holder.campsiteId != request.campsiteId ||
                                BookingPolicy.blocksDates(holder)) night else null
                        }
                    }.toSet()
                    BookingPolicy.validate(request, waitlist, site, legacyNights + claimedNights, now())
                    val timestamp = now()
                    val reservation = Reservation(
                        reservationId = reservationRef.id,
                        waitlistId = request.waitlistId,
                        campsiteId = site.campsiteId,
                        campsiteName = site.name,
                        entrantId = request.entrantId,
                        ownerId = site.ownerId,
                        checkInDate = Date(nights.first()),
                        checkOutDate = Date(nights.last() + BookingPolicy.DAY_MILLIS),
                        reservedDates = nights.map(::Date),
                        totalPrice = nights.size * site.pricePerNight,
                        status = ReservationStatus.CONFIRMED,
                        rulesAccepted = true,
                        acceptedRules = site.rules,
                        acceptedCancellationPolicy = site.cancellationPolicy,
                        createdAt = timestamp,
                        updatedAt = timestamp,
                    )
                    // All reads above must precede every write.
                    transaction.set(reservationRef, reservation)
                    claims.forEach { transaction.set(it, mapOf("reservationId" to reservationRef.id)) }
                    transaction.update(waitlistRef, "bookedEntrantIds", FieldValue.arrayUnion(request.entrantId))
                    reservation
                }
            }
        finish(task, onSuccess, onFailure)
    }

    override fun decline(
        waitlistId: String,
        entrantId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) = attempt(onFailure) {
        requireIds(waitlistId, entrantId)
        val ref = waitlists.document(waitlistId)
        val existingRef = reservations.document(BookingPolicy.reservationId(waitlistId, entrantId))
        val task = db.runTransaction { transaction ->
            val waitlist = transaction.get(ref).bookingWaitlist() ?: error("Lottery not found.")
            require(!transaction.get(existingRef).exists()) { "A booked reservation cannot be declined. Use reservation cancellation." }
            if (entrantId in waitlist.declinedEntrantIds) return@runTransaction Unit
            BookingPolicy.requireWinner(waitlist, entrantId)
            transaction.update(ref, "declinedEntrantIds", FieldValue.arrayUnion(entrantId))
            Unit
        }
        finish(task, { onSuccess() }, onFailure)
    }

    private fun <T> finish(task: Task<T>, onSuccess: (T) -> Unit, onFailure: (Exception) -> Unit) {
        task.addOnSuccessListener(onSuccess).addOnFailureListener(onFailure)
    }

    private fun attempt(onFailure: (Exception) -> Unit, action: () -> Unit) {
        try { action() } catch (e: Exception) { onFailure(e) }
    }

    private fun validId(id: String): Boolean = id.isNotBlank() && '/' !in id && '\u0000' !in id

    private fun requireIds(waitlistId: String, entrantId: String) {
        require(validId(waitlistId) && validId(entrantId)) { "A lottery and user ID are required." }
    }
}

/** Accept both the original Kotlin boolean field name and existing `isDrawn` data. */
internal fun DocumentSnapshot.bookingWaitlist(): Waitlist? =
    toObject(Waitlist::class.java)?.also {
        it.isDrawn = getBoolean("isDrawn") ?: getBoolean("drawn") ?: false
    }
