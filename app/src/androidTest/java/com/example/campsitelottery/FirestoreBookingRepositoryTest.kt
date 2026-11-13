package com.example.campsitelottery

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Opt-in emulator-only integration tests. Never connect these tests to live data:
 * pass bookingEmulatorHost and bookingEmulatorPort instrumentation arguments.
 */
@RunWith(AndroidJUnit4::class)
class FirestoreBookingRepositoryTest {
    private lateinit var app: FirebaseApp
    private lateinit var db: FirebaseFirestore
    private lateinit var repository: FirestoreBookingRepository
    private lateinit var prefix: String
    private fun date(text: String) = BookingPolicy.parse(text)!!

    @Before fun setUp() {
        val args = InstrumentationRegistry.getArguments()
        val host = args.getString("bookingEmulatorHost")
        Assume.assumeTrue("Requires an explicitly configured local Firestore emulator.", host != null)
        require(host == "127.0.0.1" || host == "10.0.2.2" || host == "localhost") { "Only a local emulator is supported." }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefix = UUID.randomUUID().toString()
        app = FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
            .setApplicationId("1:123456789:android:bookingtests")
            .setApiKey("emulator-only-not-a-real-key").setProjectId("demo-winner-booking")
            .build(), prefix)
        db = FirebaseFirestore.getInstance(app)
        db.useEmulator(host!!, args.getString("bookingEmulatorPort")?.toInt() ?: 8188)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        repository = FirestoreBookingRepository(db) { date("2029-12-01") }
        seedLottery(lotteryId)
        await(db.collection("campsites").document(siteId).set(Campsite(
            ownerId = "owner", name = "Test campsite", pricePerNight = 25.0,
            rules = "Quiet", cancellationPolicy = "Seven days",
        )))
    }

    @After fun tearDown() {
        if (::db.isInitialized) await(db.terminate())
        if (::app.isInitialized) app.delete()
    }

    private val lotteryId get() = "$prefix-lottery"
    private val siteId get() = "$prefix-site"

    private fun seedLottery(id: String, drawnField: String = "isDrawn") {
        await(db.collection("waitlists").document(id).set(mapOf(
            "ownerId" to "owner", "campsiteIds" to listOf(siteId),
            drawnField to true, "selectedWinnerIds" to listOf("winner", "other"),
            "joinedEntrantIds" to listOf("winner", "other"), "lotteryName" to "Test lottery",
            "bookingStartDate" to date("2030-01-01"), "bookingEndDate" to date("2030-01-10"),
        )))
    }

    private fun request(entrant: String = "winner", lottery: String = lotteryId) = BookingRequest(
        lottery, entrant, siteId, date("2030-01-02"), date("2030-01-04"),
        true, "Quiet", "Seven days", 25.0,
    )

    private fun <T> await(task: Task<T>): T = Tasks.await(task, 25, TimeUnit.SECONDS)
    private fun <T> result(operation: ((T) -> Unit, (Exception) -> Unit) -> Unit): CompletableFuture<T> =
        CompletableFuture<T>().also { future -> operation({ future.complete(it) }, { future.completeExceptionally(it) }) }
    private fun confirm(request: BookingRequest = request()) = result<Reservation> { ok, fail -> repository.confirm(request, ok, fail) }
    private fun load() = result<BookingOptions> { ok, fail -> repository.load(lotteryId, "winner", ok, fail) }.get(25, TimeUnit.SECONDS)
    private fun <T> fails(future: CompletableFuture<T>) {
        assertThrows(java.util.concurrent.ExecutionException::class.java) { future.get(25, TimeUnit.SECONDS) }
    }
    private fun stays() = await(db.collection("reservations").whereEqualTo("campsiteId", siteId).get()).documents

    @Test fun confirmsPersistsConsentAndReopensTheSameReservation() {
        val saved = confirm().get(25, TimeUnit.SECONDS)
        assertEquals(ReservationStatus.CONFIRMED, saved.status)
        assertEquals(50.0, saved.totalPrice, 0.0)
        assertEquals(2, saved.reservedDates.size)
        val stored = await(db.collection("reservations").document(saved.reservationId).get()).toObject(Reservation::class.java)!!
        assertTrue(stored.rulesAccepted)
        assertEquals("Quiet", stored.acceptedRules)
        assertEquals("Seven days", stored.acceptedCancellationPolicy)
        assertEquals("Test campsite", stored.campsiteName)
        assertEquals(saved.reservationId, load().reservation!!.reservationId)
        assertEquals(listOf("winner"), await(db.collection("waitlists").document(lotteryId).get()).get("bookedEntrantIds"))
    }

    @Test fun repeatedConcurrentRequestsCreateOnlyOneReservation() {
        val first = confirm()
        val second = confirm()
        assertEquals(first.get(25, TimeUnit.SECONDS).reservationId, second.get(25, TimeUnit.SECONDS).reservationId)
        assertEquals(1, stays().size)
    }

    @Test fun competingWinnersAcrossLotteriesCannotDoubleBook() {
        val otherLottery = "$prefix-other-lottery"
        seedLottery(otherLottery)
        val futures = listOf(confirm(), confirm(request("other", otherLottery)))
        val results = futures.map { runCatching { it.get(25, TimeUnit.SECONDS) } }
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.isFailure })
        assertEquals(1, stays().size)
    }

    @Test fun declineAndConfirmCannotBothSucceed() {
        val booked = confirm()
        val declined = result<Unit> { ok, fail -> repository.decline(lotteryId, "winner", { ok(Unit) }, fail) }
        val outcomes = listOf(runCatching { booked.get(25, TimeUnit.SECONDS) }, runCatching { declined.get(25, TimeUnit.SECONDS) })
        assertEquals(1, outcomes.count { it.isSuccess })
        assertEquals(1, outcomes.count { it.isFailure })
        val snapshot = await(db.collection("waitlists").document(lotteryId).get())
        val didDecline = (snapshot.get("declinedEntrantIds") as? List<*>)?.contains("winner") == true
        assertEquals(if (didDecline) 0 else 1, stays().size)
    }

    @Test fun cancelledClaimsAreReusableWithoutOverwritingHistory() {
        val first = confirm().get(25, TimeUnit.SECONDS)
        await(db.collection("reservations").document(first.reservationId)
            .update("status", ReservationStatus.CANCELLED_BY_ENTRANT.name))
        val second = confirm(request("other")).get(25, TimeUnit.SECONDS)
        assertNotEquals(first.reservationId, second.reservationId)
        assertEquals(2, stays().size)
        assertEquals(ReservationStatus.CANCELLED_BY_ENTRANT.name,
            await(db.collection("reservations").document(first.reservationId).get()).getString("status"))
        // A consumed win remains history, not a second invitation.
        assertEquals(ReservationStatus.CANCELLED_BY_ENTRANT, confirm().get(25, TimeUnit.SECONDS).status)
    }

    @Test fun legacyReservationsWithoutClaimsStillBlockConflictingNights() {
        await(db.collection("reservations").document("$prefix-legacy").set(Reservation(
            campsiteId = siteId, status = ReservationStatus.CONFIRMED,
            checkInDate = date("2030-01-03"), checkOutDate = date("2030-01-05"),
        )))
        assertTrue(date("2030-01-03").time in load().unavailableNights[siteId].orEmpty())
        fails(confirm())
        assertEquals(1, stays().size)
    }

    @Test fun malformedBlockingReservationFailsClosed() {
        await(db.collection("reservations").document("$prefix-malformed").set(Reservation(
            campsiteId = siteId, status = ReservationStatus.CONFIRMED,
        )))
        assertThrows(java.util.concurrent.ExecutionException::class.java) { load() }
        fails(confirm())
        assertEquals(1, stays().size)
    }

    @Test fun danglingClaimIsUnavailableInPreviewAndConfirmation() {
        await(db.collection("campsites").document(siteId).collection("bookingDays").document("2030-01-03")
            .set(mapOf("reservationId" to "$prefix-missing")))
        assertTrue(date("2030-01-03").time in load().unavailableNights[siteId].orEmpty())
        fails(confirm())
        assertTrue(stays().isEmpty())
    }

    @Test fun confirmationSurvivesDeletedLotteryAndUnrelatedMalformedSite() {
        val first = confirm().get(25, TimeUnit.SECONDS)
        await(db.collection("waitlists").document(lotteryId).delete())
        await(db.collection("campsites").document(siteId).delete())
        assertEquals(first.reservationId, load().reservation!!.reservationId)
        assertEquals("Test campsite", load().reservation!!.campsiteName)
    }

    @Test fun cancelledClaimForAnotherCampsiteStillFailsClosed() {
        val unrelated = "$prefix-unrelated"
        await(db.collection("reservations").document(unrelated).set(Reservation(
            campsiteId = "$prefix-another-site", status = ReservationStatus.CANCELLED_BY_ENTRANT,
        )))
        val claim = db.collection("campsites").document(siteId).collection("bookingDays").document("2030-01-03")
        await(claim.set(mapOf("reservationId" to unrelated)))
        assertTrue(date("2030-01-03").time in load().unavailableNights[siteId].orEmpty())
        fails(confirm())
        assertTrue(stays().isEmpty())
        assertEquals(unrelated, await(claim.get()).getString("reservationId"))
    }

    @Test fun changedTermsPriceAndMissingConsentCannotBeSaved() {
        await(db.collection("campsites").document(siteId).update("rules", "Updated terms"))
        fails(confirm())
        await(db.collection("campsites").document(siteId).update("rules", "Quiet", "pricePerNight", 30.0))
        fails(confirm())
        fails(confirm(request().copy(pricePerNight = 30.0, rulesAccepted = false)))
        assertTrue(stays().isEmpty())
    }

    @Test fun nonWinnerAndDeclinedWinnerCannotBook() {
        fails(confirm(request("loser")))
        result<Unit> { ok, fail -> repository.decline(lotteryId, "winner", { ok(Unit) }, fail) }.get(25, TimeUnit.SECONDS)
        fails(confirm())
        assertTrue(stays().isEmpty())
    }

    @Test fun readsBothDrawnFieldSpellings() {
        assertTrue(load().waitlist.isDrawn)
        seedLottery(lotteryId, "drawn")
        assertTrue(load().waitlist.isDrawn)
    }
}
