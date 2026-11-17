package com.example.campsitelottery

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Date-only booking rules shared by the UI and persistence layer.
 *
 * A booking period lists available *nights*, including its end day, matching
 * [CampsiteSearch]. Checkout is exclusive and may be the following day. New
 * bookings store calendar dates at UTC midnight, independently of device timezone.
 * Legacy Canadian-local-midnight dates normalize to the same UTC calendar day.
 */
object BookingPolicy {
    const val DAY_MILLIS = 86_400_000L
    // Leave room for the reservation/waitlist writes in Firestore's 500-write limit.
    const val MAX_NIGHTS = 365

    fun day(date: Date): Long = Math.floorDiv(date.time, DAY_MILLIS) * DAY_MILLIS

    fun format(date: Date): String = formatter().format(date)

    fun parse(text: String): Date? {
        if (!text.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
        return runCatching { formatter().parse(text) }.getOrNull()
    }

    private fun formatter() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

    /** Returns occupied nights, not checkout; rejects empty or unbounded ranges. */
    fun nights(checkIn: Date, checkOut: Date): List<Long> {
        val first = day(checkIn)
        val end = day(checkOut)
        val count = (end - first) / DAY_MILLIS
        require(count in 1..MAX_NIGHTS.toLong()) {
            "Choose a checkout after check-in, with at most $MAX_NIGHTS nights."
        }
        return List(count.toInt()) { first + it * DAY_MILLIS }
    }

    fun blocksDates(reservation: Reservation): Boolean =
        reservation.status == ReservationStatus.CONFIRMED ||
            reservation.status == ReservationStatus.PENDING

    fun occupiedNights(reservation: Reservation): Set<Long> {
        if (!blocksDates(reservation)) return emptySet()
        if (reservation.reservedDates.isNotEmpty()) return reservation.reservedDates.map(::day).toSet()
        val start = requireNotNull(reservation.checkInDate) {
            "An active reservation has missing dates. Contact the campsite owner before booking."
        }
        val end = requireNotNull(reservation.checkOutDate) {
            "An active reservation has missing dates. Contact the campsite owner before booking."
        }
        // Old records may describe a longer stay than new requests allow.
        val count = (day(end) - day(start)) / DAY_MILLIS
        require(count in 1L..3660L) { "An existing reservation has invalid dates." }
        return (0 until count.toInt()).map { day(start) + it * DAY_MILLIS }.toSet()
    }

    fun requireWinner(waitlist: Waitlist, entrantId: String) {
        require(entrantId.isNotBlank()) { "A user ID is required." }
        require(waitlist.isDrawn && entrantId in waitlist.selectedWinnerIds) {
            "Only selected lottery winners can reserve these dates."
        }
        require(entrantId !in waitlist.declinedEntrantIds) { "You have declined this invitation." }
        require(entrantId !in waitlist.bookedEntrantIds) { "This lottery win has already been used." }
    }

    fun validate(
        request: BookingRequest,
        waitlist: Waitlist,
        campsite: Campsite,
        unavailable: Set<Long>,
        now: Date,
    ): List<Long> {
        requireWinner(waitlist, request.entrantId)
        require(request.campsiteId == campsite.campsiteId && request.campsiteId in waitlist.campsiteIds) {
            "Choose a campsite included in this lottery."
        }
        require(campsite.ownerId.isNotBlank() && campsite.ownerId == waitlist.ownerId) {
            "This campsite no longer belongs to the lottery owner."
        }
        require(request.rulesAccepted) { "Read and accept the campsite rules and cancellation policy first." }
        require(request.acceptedRules == campsite.rules &&
            request.acceptedCancellationPolicy == campsite.cancellationPolicy) {
            "The campsite terms changed. Refresh availability and read the updated terms."
        }
        require(request.pricePerNight == campsite.pricePerNight &&
            campsite.pricePerNight.isFinite() && campsite.pricePerNight >= 0) {
            "The campsite price changed or is invalid. Refresh availability."
        }
        val nights = nights(request.checkIn, request.checkOut)
        val start = waitlist.bookingStartDate?.let(::day)
        val end = waitlist.bookingEndDate?.let(::day)
        require(start != null && end != null && start <= end) { "This lottery has no valid stay period." }
        require(nights.first() >= start && nights.last() <= end && nights.first() >= day(now)) {
            "Choose future nights within the lottery stay period."
        }
        require(nights.none { it in unavailable }) { "Some selected nights are no longer available. Refresh availability." }
        require((nights.size * campsite.pricePerNight).isFinite()) { "The reservation price is invalid." }
        return nights
    }

    /** One immutable reservation per win; retries cannot create duplicate bookings. */
    fun reservationId(waitlistId: String, entrantId: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$waitlistId\u0000$entrantId".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
