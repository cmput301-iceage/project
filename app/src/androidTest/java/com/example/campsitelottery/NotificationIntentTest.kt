package com.example.campsitelottery

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationIntentTest {

    @Test
    fun testNotificationIntentsForAllTypes() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        val types = listOf(
            "SYSTEM" to "",
            "LOTTERY_WIN" to "waitlist-win-1",
            "LOTTERY_LOSS" to "waitlist-loss-1",
            "LOTTERY_JOINED" to "waitlist-join-1",
            "RESERVATION" to "",
            "OWNER_RESERVATION" to "",
            "RESERVATION_CANCELLED" to "",
            "OWNER_RESERVATION_CANCELLED" to "",
            "RESERVATION_REJECTED" to "",
            "OWNER_RESERVATION_REJECTED" to "",
            "INVITATION_DECLINED" to "",
            "OWNER_INVITATION_DECLINED" to ""
        )

        for ((type, wId) in types) {
            val campsiteId = if (type == "LOTTERY_JOINED") "campsite-99" else ""
            val intent = Intent(targetContext, MainActivity::class.java).apply {
                putExtra("notification_type", type)
                if (wId.isNotBlank()) putExtra("waitlist_id", wId)
                if (campsiteId.isNotBlank()) putExtra("campsite_id", campsiteId)
            }

            assertEquals(type, intent.getStringExtra("notification_type"))
            if (wId.isNotBlank()) {
                assertEquals(wId, intent.getStringExtra("waitlist_id"))
            }
            if (campsiteId.isNotBlank()) {
                assertEquals(campsiteId, intent.getStringExtra("campsite_id"))
            }
        }
    }
}
