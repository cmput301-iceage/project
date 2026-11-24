/**
 * Purpose: Model for in-app messages between entrants and campsite owners.
 * Role / Design Pattern: Model layer entity stored in the Firestore `messages` collection.
 * Outstanding Issues: None.
 */
package com.example.campsitelottery

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * An in-app message between two users (US 01.14.01, US 02.12.01).
 * Admins review all of these (US 03.12.01).
 *
 * @property messageId Firestore document ID.
 * @property fromUserId Sender's device/user ID.
 * @property toUserId Recipient's device/user ID.
 * @property campsiteId Related campsite, when known.
 * @property reservationId Related reservation, when messaging about a booking.
 * @property body Message text.
 * @property createdAt When the message was sent.
 */
data class Message(
    @DocumentId
    val messageId: String = "",
    var fromUserId: String = "",
    var toUserId: String = "",
    var campsiteId: String = "",
    var reservationId: String = "",
    var body: String = "",
    var createdAt: Date = Date(),
) {
    constructor() : this(
        messageId = "",
        fromUserId = "",
        toUserId = "",
        campsiteId = "",
        reservationId = "",
        body = "",
        createdAt = Date(),
    )
}
