package com.example.campsitelottery

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Represents a lottery waitlist listing for one or more campsites.
 * Handles entrant pools, winner selection, and booking time windows.
 */
data class Waitlist(
    @DocumentId
    val waitlistId: String = "",
    var campsiteIds: List<String> = emptyList(),  // Supports single or multi-campsite lotteries
    var ownerId: String = "",

    // Configurable time windows
    var registrationStartDate: Date? = null,
    var registrationEndDate: Date? = null,
    var bookingStartDate: Date? = null,
    var bookingEndDate: Date? = null,

    // Capacity & Drawing settings
    var numberOfWinnersToDraw: Int = 1,
    var maxWaitlistCapacity: Int? = null, // Optional waitlist capacity limit
    var isDrawn: Boolean = false, // Tracks whether automatic draw has executed

    // Entrant Pools (Tracked by Entrant User IDs)
    var joinedEntrantIds: MutableList<String> = mutableListOf(), // Active waiting list
    var selectedWinnerIds: MutableList<String> = mutableListOf(), // Entrants selected by lottery
    var declinedEntrantIds: MutableList<String> = mutableListOf() // Entrants who declined their win
) {
    // Required empty constructor
    constructor() : this(
        waitlistId = "",
        campsiteIds = emptyList(),
        ownerId = "",
        registrationStartDate = null,
        registrationEndDate = null,
        bookingStartDate = null,
        bookingEndDate = null,
        numberOfWinnersToDraw = 1,
        maxWaitlistCapacity = null,
        isDrawn = false,
        joinedEntrantIds = mutableListOf(),
        selectedWinnerIds = mutableListOf(),
        declinedEntrantIds = mutableListOf()
    )

    // Helper to check if the waitlist has room for another entrant
    fun isFull(): Boolean {
        val capacity = maxWaitlistCapacity ?: return false
        return joinedEntrantIds.size >= capacity
    }

    // Helper to check if registration is currently active based on current system time
    fun isRegistrationOpen(): Boolean {
        val now = Date()
        val start = registrationStartDate ?: return false
        val end = registrationEndDate ?: return false
        return !isDrawn && now.after(start) && now.before(end)
    }
}