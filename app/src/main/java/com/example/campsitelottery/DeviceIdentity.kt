package com.example.campsitelottery

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

/**
 * Identifies the current user by their device, so no username or password is needed (US 9.1).
 *
 * The identifier is Android's [Settings.Secure.ANDROID_ID], which is unique per
 * device, user and app signing key, and survives app reinstalls on Android 8.0+.
 * It is used directly as the user's Firestore document ID.
 */
object DeviceIdentity {

    /**
     * Returns a stable identifier for this device.
     *
     * @param context any context; only used to reach the content resolver.
     * @return the device's ANDROID_ID.
     * @throws IllegalStateException if the platform returns no ID, which should not
     *   happen on a real device.
     */
    @SuppressLint("HardwareIds") // Identifying the device is the point of US 9.1
    fun getDeviceId(context: Context): String {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        check(!id.isNullOrBlank()) { "Device returned no ANDROID_ID" }
        return id
    }
}
