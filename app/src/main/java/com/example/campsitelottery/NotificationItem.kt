package com.example.campsitelottery

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Data model representing a system or lottery notification message stored in Firestore.
 *
 * @property notificationId Auto-generated Firestore document ID.
 * @property userId Recipient entrant user or device ID.
 * @property title Notification summary title (e.g. "Lottery Won! 🎉").
 * @property message Detailed body description text.
 * @property type Category type string ("LOTTERY_WIN", "LOTTERY_LOSS", "SYSTEM").
 * @property waitlistId Linked lottery waitlist ID, if applicable.
 * @property timestamp Date and time when the notification was generated.
 * @property isRead Flag indicating if the user has viewed the notification.
 */
data class NotificationItem(
    @DocumentId
    val notificationId: String = "",
    var userId: String = "",
    var title: String = "",
    var message: String = "",
    var type: String = "SYSTEM",
    var waitlistId: String = "",
    var ownerId: String = "",
    var campsiteId: String = "",
    var timestamp: Date = Date(),
    var isRead: Boolean = false
) {
    /**
     * Required no-argument constructor for Firestore deserialization.
     */
    constructor() : this(
        notificationId = "",
        userId = "",
        title = "",
        message = "",
        type = "SYSTEM",
        waitlistId = "",
        ownerId = "",
        campsiteId = "",
        timestamp = Date(),
        isRead = false
    )
}
