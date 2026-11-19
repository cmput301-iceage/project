/**
 * Purpose: In-memory MessageRepository for Compose previews and unit tests.
 * Role / Design Pattern: Fake Repository.
 * Outstanding Issues: None.
 */
package com.example.campsitelottery

import com.google.firebase.firestore.ListenerRegistration
import java.util.Date
import java.util.UUID

class InMemoryMessageRepository(
    initial: List<Message> = emptyList(),
) : MessageRepository {

    private val messages = initial.toMutableList()
    private val listeners = mutableListOf<(List<Message>) -> Unit>()

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
        if (trimmed.isEmpty()) {
            onFailure(IllegalArgumentException("Message cannot be empty."))
            return
        }
        messages.add(
            0,
            Message(
                messageId = UUID.randomUUID().toString(),
                fromUserId = fromUserId,
                toUserId = toUserId,
                campsiteId = campsiteId,
                reservationId = reservationId,
                body = trimmed,
                createdAt = Date(),
            ),
        )
        notifyListeners()
        onSuccess()
    }

    override fun listenAllMessages(
        onSuccess: (List<Message>) -> Unit,
        onFailure: (Exception) -> Unit,
    ): ListenerRegistration {
        listeners.add(onSuccess)
        onSuccess(messages.toList())
        return object : ListenerRegistration {
            override fun remove() {
                listeners.remove(onSuccess)
            }
        }
    }

    override fun listenMessagesForUser(
        userId: String,
        onSuccess: (List<Message>) -> Unit,
        onFailure: (Exception) -> Unit,
    ): ListenerRegistration {
        val wrapped: (List<Message>) -> Unit = { all ->
            onSuccess(all.filter { it.fromUserId == userId || it.toUserId == userId })
        }
        listeners.add(wrapped)
        wrapped(messages.toList())
        return object : ListenerRegistration {
            override fun remove() {
                listeners.remove(wrapped)
            }
        }
    }

    private fun notifyListeners() {
        val copy = messages.toList()
        listeners.toList().forEach { it(copy) }
    }
}
