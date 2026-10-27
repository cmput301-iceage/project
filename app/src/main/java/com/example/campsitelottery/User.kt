package com.example.campsitelottery

import com.google.firebase.firestore.DocumentId


/**
 * A user in the Campsite Reservation System
 * Supports Entrants, Campsite Owner, and Admin Roles
**/

data class User(
    @DocumentId
    val userId: String = "",
    var name: String = "",
    var email: String? = null,
    var phoneNumber: String? = null,
    var profileImageUrl: String? = null,
    var notificationsEnabled: Boolean = true,

    // Role flags
    var isEntrant: Boolean = true,
    var isOwner: Boolean = false,
    var isAdmin: Boolean = false
) {
    // Required empty constructor for Firestore deserialization
    constructor() : this(
        userId = "",
        name = "",
        email = null,
        phoneNumber = null,
        profileImageUrl = null,
        notificationsEnabled = true,
        isEntrant = true,
        isOwner = false,
        isAdmin = false
    )
}