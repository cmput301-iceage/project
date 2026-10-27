package com.example.campsitelottery

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Represents the status lifecycle of a campsite reservation.
 */
enum class ReservationStatus {
    PENDING, // Won lottery, awaiting user confirmation/booking
    CONFIRMED,// Successfully booked and active
    CANCELLED_BY_ENTRANT, // Cancelled by the entrant
    REJECTED_BY_OWNER, // Rejected by the campsite owner
    COMPLETED // Past reservation
}

/**
 * Represents a campsite reservation instance in the system.
 */
data class Reservation(
    @DocumentId
    val reservationId: String = "",
    var campsiteId: String = "",
    var entrantId: String = "",
    var ownerId: String = "",

    // Booking dates & details
    var checkInDate: Date? = null,
    var checkOutDate: Date? = null,
    var reservedDates: List<Date> = emptyList(), // Supports multi date selections
    var totalPrice: Double = 0.0,

    // Status and Policy Compliance
    var status: ReservationStatus = ReservationStatus.PENDING,
    var rulesAccepted: Boolean = false,

    // Timestamps
    var createdAt: Date = Date(),
    var updatedAt: Date = Date()
) {
    // Required empty constructor
    constructor() : this(
        reservationId = "",
        campsiteId = "",
        entrantId = "",
        ownerId = "",
        checkInDate = null,
        checkOutDate = null,
        reservedDates = emptyList(),
        totalPrice = 0.0,
        status = ReservationStatus.PENDING,
        rulesAccepted = false,
        createdAt = Date(),
        updatedAt = Date()
    )

    /**
     * Helper to check if the reservation is currently active/confirmed.
     */
    fun isActive(): Boolean {
        return status == ReservationStatus.CONFIRMED
    }
}