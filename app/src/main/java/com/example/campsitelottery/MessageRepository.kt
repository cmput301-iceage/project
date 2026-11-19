/**
 * Purpose: Abstraction for sending and listing in-app messages.
 * Role / Design Pattern: Repository Pattern interface.
 * Outstanding Issues: None.
 */
package com.example.campsitelottery

import com.google.firebase.firestore.ListenerRegistration

/**
 * Reads and writes [Message] documents in the shared messaging pipeline.
 */
interface MessageRepository {

    /**
     * Sends a new message. Exactly one of the callbacks is invoked.
     */
    fun sendMessage(
        fromUserId: String,
        toUserId: String,
        body: String,
        campsiteId: String = "",
        reservationId: String = "",
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    )

    /**
     * Live list of every message (admin review, US 03.12.01).
     */
    fun listenAllMessages(
        onSuccess: (List<Message>) -> Unit,
        onFailure: (Exception) -> Unit,
    ): ListenerRegistration

    /**
     * Live list of messages where [userId] is sender or recipient.
     */
    fun listenMessagesForUser(
        userId: String,
        onSuccess: (List<Message>) -> Unit,
        onFailure: (Exception) -> Unit,
    ): ListenerRegistration
}
