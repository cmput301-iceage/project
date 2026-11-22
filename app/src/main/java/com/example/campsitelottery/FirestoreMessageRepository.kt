/**
 * Purpose: Firestore-backed MessageRepository using the `messages` collection.
 * Role / Design Pattern: Repository Pattern implementation.
 * Outstanding Issues: None.
 */
package com.example.campsitelottery

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Date

private const val MESSAGES_COLLECTION = "messages"

/**
 * Stores messages as top-level documents in [MESSAGES_COLLECTION].
 */
class FirestoreMessageRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : MessageRepository {

    private val messages get() = db.collection(MESSAGES_COLLECTION)

    override fun sendMessage(
        fromUserId: String,
        toUserId: String,
        body: String,
        campsiteId: String,
        reservationId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val trimmed = body.trim()
        if (fromUserId.isBlank() || toUserId.isBlank()) {
            onFailure(IllegalArgumentException("Missing sender or recipient."))
            return
        }
        if (trimmed.isEmpty()) {
            onFailure(IllegalArgumentException("Message cannot be empty."))
            return
        }
        if (fromUserId == toUserId) {
            onFailure(IllegalArgumentException("You cannot message yourself."))
            return
        }

        val message = Message(
            fromUserId = fromUserId,
            toUserId = toUserId,
            campsiteId = campsiteId,
            reservationId = reservationId,
            body = trimmed,
            createdAt = Date(),
        )
        messages.add(message)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    override fun listenAllMessages(
        onSuccess: (List<Message>) -> Unit,
        onFailure: (Exception) -> Unit,
    ): ListenerRegistration {
        return messages
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(Message::class.java) }.getOrNull()
                }.orEmpty()
                onSuccess(list)
            }
    }

    override fun listenMessagesForUser(
        userId: String,
        onSuccess: (List<Message>) -> Unit,
        onFailure: (Exception) -> Unit,
    ): ListenerRegistration {
        // Listen to all and filter client-side so we don't need a composite
        // OR query / two listeners for from+to.
        return messages
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(Message::class.java) }.getOrNull()
                }
                    ?.filter { it.fromUserId == userId || it.toUserId == userId }
                    .orEmpty()
                onSuccess(list)
            }
    }
}
