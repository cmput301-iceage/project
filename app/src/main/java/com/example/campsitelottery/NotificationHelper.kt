package com.example.campsitelottery

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import android.app.PendingIntent
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

@SuppressLint("StaticFieldLeak")
object NotificationHelper {

    private lateinit var context: Context
    fun initialize(context: Context) {
        this.context = context.applicationContext
        createNotificationChannel()
    }
    private var requestPermissionCallback: (() -> Unit)? = null
    fun setPermissionRequester(callback: () -> Unit) {
        requestPermissionCallback = callback
    }
    fun requestPermission() {
        requestPermissionCallback?.invoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ready_channel",
                "Ready Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(
        id: Int,
        title: String,
        message: String,
        userState: UserState,
        type: String = "SYSTEM",
        waitlistId: String = "",
        campsiteId: String = ""
    ) {
        // check android notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission()
            return
        }

        // check user notification preference
        if (userState is UserState.Ready && !userState.user.notificationsEnabled) {
            sendNotificationsDisabledNotification()
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            if (waitlistId.isNotBlank()) putExtra("waitlist_id", waitlistId)
            if (campsiteId.isNotBlank()) putExtra("campsite_id", campsiteId)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            flags
        )

        // build and send notification
        val builder = NotificationCompat.Builder(context, "ready_channel")
            .setSmallIcon(R.drawable.appicon_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    fun postNotification(item: NotificationItem) {
        if (item.userId.isBlank()) return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(item.userId).get()
            .addOnSuccessListener { userDoc ->
                val user = userDoc.toObject(User::class.java)
                val enabled = user?.notificationsEnabled ?: true
                if (!enabled) return@addOnSuccessListener

                db.collection("notifications").add(item)

                sendNotification(
                    id = (item.waitlistId.hashCode() xor item.userId.hashCode() xor item.title.hashCode()),
                    title = item.title,
                    message = item.message,
                    userState = UserState.Ready(user ?: User(userId = item.userId)),
                    type = item.type,
                    waitlistId = item.waitlistId,
                    campsiteId = item.campsiteId
                )
            }
    }

    fun sendReservationNotification(reservation: Reservation) {
        val checkInStr = reservation.checkInDate?.let(BookingPolicy::format) ?: ""
        val checkOutStr = reservation.checkOutDate?.let(BookingPolicy::format) ?: ""

        // Notification for User
        postNotification(
            NotificationItem(
                userId = reservation.entrantId,
                ownerId = reservation.ownerId,
                campsiteId = reservation.campsiteId,
                waitlistId = reservation.waitlistId,
                title = "Reservation Confirmed! ⛺",
                message = "Your reservation for '${reservation.campsiteName}' ($checkInStr to $checkOutStr) is confirmed. Confirmation ID: ${reservation.reservationId}. Total: $${"%.2f".format(reservation.totalPrice)}.",
                type = "RESERVATION",
                timestamp = Date(),
                isRead = false
            )
        )

        // Notification for Owner
        if (reservation.ownerId.isNotBlank() && reservation.ownerId != reservation.entrantId) {
            postNotification(
                NotificationItem(
                    userId = reservation.ownerId,
                    ownerId = reservation.ownerId,
                    campsiteId = reservation.campsiteId,
                    waitlistId = reservation.waitlistId,
                    title = "New Reservation Received! ⛺",
                    message = "An entrant has reserved '${reservation.campsiteName}' ($checkInStr to $checkOutStr). Total: $${"%.2f".format(reservation.totalPrice)}.",
                    type = "OWNER_RESERVATION",
                    timestamp = Date(),
                    isRead = false
                )
            )
        }
    }

    fun sendReservationCancelledByEntrantNotification(reservation: Reservation) {
        val checkInStr = reservation.checkInDate?.let(BookingPolicy::format) ?: ""
        val checkOutStr = reservation.checkOutDate?.let(BookingPolicy::format) ?: ""

        // User Notification
        postNotification(
            NotificationItem(
                userId = reservation.entrantId,
                ownerId = reservation.ownerId,
                campsiteId = reservation.campsiteId,
                waitlistId = reservation.waitlistId,
                title = "Reservation Cancelled",
                message = "Your reservation for '${reservation.campsiteName}' ($checkInStr to $checkOutStr) has been cancelled.",
                type = "RESERVATION_CANCELLED",
                timestamp = Date(),
                isRead = false
            )
        )

        // Owner Notification
        if (reservation.ownerId.isNotBlank() && reservation.ownerId != reservation.entrantId) {
            postNotification(
                NotificationItem(
                    userId = reservation.ownerId,
                    ownerId = reservation.ownerId,
                    campsiteId = reservation.campsiteId,
                    waitlistId = reservation.waitlistId,
                    title = "Reservation Cancelled by Entrant",
                    message = "The reservation for '${reservation.campsiteName}' ($checkInStr to $checkOutStr) was cancelled by the entrant.",
                    type = "OWNER_RESERVATION_CANCELLED",
                    timestamp = Date(),
                    isRead = false
                )
            )
        }
    }

    fun sendReservationRejectedByOwnerNotification(reservation: Reservation) {
        val checkInStr = reservation.checkInDate?.let(BookingPolicy::format) ?: ""
        val checkOutStr = reservation.checkOutDate?.let(BookingPolicy::format) ?: ""

        // User Notification
        postNotification(
            NotificationItem(
                userId = reservation.entrantId,
                ownerId = reservation.ownerId,
                campsiteId = reservation.campsiteId,
                waitlistId = reservation.waitlistId,
                title = "Reservation Rejected by Owner",
                message = "Your reservation for '${reservation.campsiteName}' ($checkInStr to $checkOutStr) was rejected by the owner.",
                type = "RESERVATION_REJECTED",
                timestamp = Date(),
                isRead = false
            )
        )

        // Owner Notification
        if (reservation.ownerId.isNotBlank() && reservation.ownerId != reservation.entrantId) {
            postNotification(
                NotificationItem(
                    userId = reservation.ownerId,
                    ownerId = reservation.ownerId,
                    campsiteId = reservation.campsiteId,
                    waitlistId = reservation.waitlistId,
                    title = "Reservation Rejected",
                    message = "You rejected the reservation for '${reservation.campsiteName}' ($checkInStr to $checkOutStr).",
                    type = "OWNER_RESERVATION_REJECTED",
                    timestamp = Date(),
                    isRead = false
                )
            )
        }
    }

    fun sendInvitationDeclinedNotification(waitlist: Waitlist, entrantId: String) {
        // User Notification
        postNotification(
            NotificationItem(
                userId = entrantId,
                ownerId = waitlist.ownerId,
                waitlistId = waitlist.waitlistId,
                title = "Invitation Declined",
                message = "You declined the winning invitation for '${waitlist.lotteryName}'.",
                type = "INVITATION_DECLINED",
                timestamp = Date(),
                isRead = false
            )
        )

        // Owner Notification
        if (waitlist.ownerId.isNotBlank() && waitlist.ownerId != entrantId) {
            postNotification(
                NotificationItem(
                    userId = waitlist.ownerId,
                    ownerId = waitlist.ownerId,
                    waitlistId = waitlist.waitlistId,
                    title = "Winning Invitation Declined",
                    message = "An entrant declined their winning invitation for '${waitlist.lotteryName}'.",
                    type = "OWNER_INVITATION_DECLINED",
                    timestamp = Date(),
                    isRead = false
                )
            )
        }
    }

    private fun sendNotificationsDisabledNotification() {
        // We'll implement this in the next section
    }

}