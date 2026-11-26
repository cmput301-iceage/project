package com.example.campsitelottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class NotificationTest {

    @Test
    fun defaultNotificationItem_hasExpectedDefaults() {
        val item = NotificationItem()
        assertEquals("", item.notificationId)
        assertEquals("", item.userId)
        assertEquals("", item.title)
        assertEquals("", item.message)
        assertEquals("SYSTEM", item.type)
        assertEquals("", item.waitlistId)
        assertFalse(item.isRead)
    }

    @Test
    fun lotteryWinNotificationItem_hasWinProperties() {
        val now = Date()
        val item = NotificationItem(
            userId = "user-1",
            title = "Lottery Won! 🎉",
            message = "Congratulations! You won the lottery for 'Pine Creek'. You can now book your reservation.",
            type = "LOTTERY_WIN",
            waitlistId = "waitlist-123",
            timestamp = now,
            isRead = false
        )
        assertEquals("user-1", item.userId)
        assertEquals("Lottery Won! 🎉", item.title)
        assertEquals("LOTTERY_WIN", item.type)
        assertEquals("waitlist-123", item.waitlistId)
        assertFalse(item.isRead)
    }

    @Test
    fun lotteryLossNotificationItem_hasLossProperties() {
        val now = Date()
        val item = NotificationItem(
            userId = "user-2",
            title = "Lottery Results",
            message = "Unfortunately, you were not selected in the lottery for 'Pine Creek'.",
            type = "LOTTERY_LOSS",
            waitlistId = "waitlist-123",
            timestamp = now,
            isRead = false
        )
        assertEquals("user-2", item.userId)
        assertEquals("Lottery Results", item.title)
        assertEquals("LOTTERY_LOSS", item.type)
        assertEquals("waitlist-123", item.waitlistId)
    }

    @Test
    fun reservationNotificationItem_hasReservationProperties() {
        val now = Date()
        val item = NotificationItem(
            userId = "user-1",
            title = "Reservation Confirmed! ⛺",
            message = "Your reservation for 'Site 10' (2030-01-02 to 2030-01-04) is confirmed. Confirmation ID: res-1. Total: $50.00.",
            type = "RESERVATION",
            waitlistId = "waitlist-123",
            ownerId = "owner-99",
            campsiteId = "site-10",
            timestamp = now,
            isRead = false
        )
        assertEquals("user-1", item.userId)
        assertEquals("Reservation Confirmed! ⛺", item.title)
        assertEquals("RESERVATION", item.type)
        assertEquals("waitlist-123", item.waitlistId)
        assertEquals("owner-99", item.ownerId)
        assertEquals("site-10", item.campsiteId)
    }

    @Test
    fun joinedWaitlistNotificationItem_storesOwnerAndCampsiteDetails() {
        val now = Date()
        val item = NotificationItem(
            userId = "user-3",
            title = "Lottery Joined! ⛺",
            message = "You have successfully joined the waitlist for 'Pine Creek'.",
            type = "LOTTERY_JOINED",
            waitlistId = "waitlist-555",
            ownerId = "owner-12",
            campsiteId = "campsite-88",
            timestamp = now,
            isRead = false
        )
        assertEquals("user-3", item.userId)
        assertEquals("LOTTERY_JOINED", item.type)
        assertEquals("owner-12", item.ownerId)
        assertEquals("campsite-88", item.campsiteId)
        assertEquals("waitlist-555", item.waitlistId)
    }

    @Test
    fun ownerReservationNotificationItem_hasOwnerType() {
        val item = NotificationItem(
            userId = "owner-1",
            ownerId = "owner-1",
            title = "New Reservation Received! ⛺",
            message = "An entrant has reserved 'Pine Creek'.",
            type = "OWNER_RESERVATION"
        )
        assertEquals("owner-1", item.userId)
        assertEquals("OWNER_RESERVATION", item.type)
    }

    @Test
    fun cancellationAndRejectionNotificationTypes_areConstructed() {
        val cancel = NotificationItem(type = "RESERVATION_CANCELLED", title = "Reservation Cancelled")
        val reject = NotificationItem(type = "RESERVATION_REJECTED", title = "Reservation Rejected")
        val decline = NotificationItem(type = "INVITATION_DECLINED", title = "Invitation Declined")

        assertEquals("RESERVATION_CANCELLED", cancel.type)
        assertEquals("RESERVATION_REJECTED", reject.type)
        assertEquals("INVITATION_DECLINED", decline.type)
    }
}
