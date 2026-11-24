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
import androidx.core.content.ContextCompat

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
        userState: UserState
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

        // build and send notification
        val builder = NotificationCompat.Builder(context, "ready_channel")
            .setSmallIcon(R.drawable.appicon_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
    private fun sendNotificationsDisabledNotification() {
        // We'll implement this in the next section
    }

}