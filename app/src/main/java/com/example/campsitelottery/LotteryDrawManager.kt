package com.example.campsitelottery

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

/**
 * Manager handling drawing lottery winners, updating Firestore waitlists,
 * and dispatching win/loss notifications to entrants.
 */
object LotteryDrawManager {

    private const val TAG = "LotteryDrawManager"
    private const val WAITLISTS_COLLECTION = "waitlists"
    private const val USERS_COLLECTION = "users"
    private const val NOTIFICATIONS_COLLECTION = "notifications"

    private val db get() = FirebaseFirestore.getInstance()

    /**
     * Checks if a lottery registration window has closed, and if so, performs the draw.
     *
     * @param waitlist The target waitlist.
     * @param onComplete Callback invoked with true if a draw was executed, false otherwise.
     */
    fun checkAndDrawIfExpired(
        waitlist: Waitlist,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (!LotteryDrawPolicy.shouldDraw(waitlist, Date())) {
            onComplete?.invoke(false)
            return
        }
        executeDraw(waitlist, onComplete)
    }

    /**
     * Manually triggers a draw for a waitlist regardless of the registration end date.
     * Used by administrators to run instant demo draws.
     *
     * @param waitlist The target waitlist.
     * @param onComplete Callback invoked with true if draw succeeded, false otherwise.
     */
    fun drawManually(
        waitlist: Waitlist,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (waitlist.isDrawn) {
            onComplete?.invoke(false)
            return
        }
        executeDraw(waitlist, onComplete)
    }

    /**
     * Queries all undrawn waitlists in Firestore and executes drawing for any whose registration has ended.
     */
    fun checkAndDrawAllExpiredLotteries(onComplete: (() -> Unit)? = null) {
        db.collection(WAITLISTS_COLLECTION)
            .whereEqualTo("isDrawn", false)
            .get()
            .addOnSuccessListener { snapshot ->
                val expiredWaitlists = snapshot.documents.mapNotNull { doc ->
                    doc.bookingWaitlist()
                }.filter { LotteryDrawPolicy.shouldDraw(it, Date()) }

                if (expiredWaitlists.isEmpty()) {
                    onComplete?.invoke()
                    return@addOnSuccessListener
                }

                var pendingCount = expiredWaitlists.size
                for (waitlist in expiredWaitlists) {
                    executeDraw(waitlist) {
                        pendingCount--
                        if (pendingCount <= 0) {
                            onComplete?.invoke()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching undrawn waitlists", e)
                onComplete?.invoke()
            }
    }

    private fun executeDraw(
        waitlist: Waitlist,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val result = LotteryDrawPolicy.draw(waitlist)

        val updates = mapOf(
            "isDrawn" to true,
            "drawn" to true,
            "selectedWinnerIds" to result.winners
        )

        db.collection(WAITLISTS_COLLECTION)
            .document(waitlist.waitlistId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully drawn lottery: ${waitlist.waitlistId}")
                sendNotifications(result.waitlist, result.winners, result.losers)
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update drawn lottery status", e)
                onComplete?.invoke(false)
            }
    }

    fun sendNotifications(
        waitlist: Waitlist,
        winners: List<String>,
        losers: List<String>
    ) {
        val allEntrants = (winners + losers).distinct()
        if (allEntrants.isEmpty()) return

        for (userId in allEntrants) {
            if (userId.isBlank()) continue
            val isWinner = userId in winners
            db.collection(USERS_COLLECTION).document(userId).get()
                .addOnSuccessListener { userDoc ->
                    val user = userDoc.toObject(User::class.java)
                    val notificationsEnabled = user?.notificationsEnabled ?: true
                    if (!notificationsEnabled) {
                        Log.d(TAG, "Skipping notification for $userId (notifications disabled)")
                        return@addOnSuccessListener
                    }

                    val title = if (isWinner) "Lottery Won! 🎉" else "Lottery Results"
                    val message = if (isWinner) {
                        "Congratulations! You won the lottery for '${waitlist.lotteryName}'. You can now book your reservation."
                    } else {
                        "Unfortunately, you were not selected in the lottery for '${waitlist.lotteryName}'."
                    }
                    val type = if (isWinner) "LOTTERY_WIN" else "LOTTERY_LOSS"

                    val notification = NotificationItem(
                        userId = userId,
                        title = title,
                        message = message,
                        type = type,
                        waitlistId = waitlist.waitlistId,
                        timestamp = Date(),
                        isRead = false
                    )

                    db.collection(NOTIFICATIONS_COLLECTION)
                        .add(notification)
                        .addOnSuccessListener {
                            Log.d(TAG, "Created notification for user $userId")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to create notification for $userId", e)
                        }

                    NotificationHelper.sendNotification(
                        id = (waitlist.waitlistId.hashCode() xor userId.hashCode()),
                        title = title,
                        message = message,
                        userState = UserState.Ready(user ?: User(userId = userId))
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to lookup user profile for notification", e)
                }
        }
    }
}
