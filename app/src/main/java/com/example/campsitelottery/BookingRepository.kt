package com.example.campsitelottery

import java.util.Date

/** The exact price and terms displayed when the entrant gave affirmative consent. */
data class BookingRequest(
    val waitlistId: String,
    val entrantId: String,
    val campsiteId: String,
    val checkIn: Date,
    val checkOut: Date,
    val rulesAccepted: Boolean,
    val acceptedRules: String,
    val acceptedCancellationPolicy: String,
    val pricePerNight: Double,
)

data class BookingOptions(
    val waitlist: Waitlist,
    val campsites: List<Campsite>,
    val unavailableNights: Map<String, Set<Long>>,
    val reservation: Reservation? = null,
)

/**
 * Winner booking operations. Each call invokes exactly one completion callback.
 * A consumed win cannot be booked again, including after cancellation; its original
 * reservation remains available as history. Notifications/CSV are separate flows.
 */
interface BookingRepository {
    fun load(
        waitlistId: String,
        entrantId: String,
        onSuccess: (BookingOptions) -> Unit,
        onFailure: (Exception) -> Unit,
    )

    fun confirm(request: BookingRequest, onSuccess: (Reservation) -> Unit, onFailure: (Exception) -> Unit)

    fun decline(
        waitlistId: String,
        entrantId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    )
}
