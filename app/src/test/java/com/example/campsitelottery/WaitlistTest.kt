package com.example.campsitelottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/** Unit tests for the [Waitlist] capacity and registration window helpers. */
class WaitlistTest {

    private val hour = 60L * 60 * 1000
    private fun hoursFromNow(hours: Long) = Date(System.currentTimeMillis() + hours * hour)

    // --- isFull ---

    @Test
    fun isFull_withNoCapacityLimit_isNeverFull() {
        val waitlist = Waitlist(maxWaitlistCapacity = null, joinedEntrantIds = mutableListOf("a", "b", "c"))
        assertFalse(waitlist.isFull())
    }

    @Test
    fun isFull_belowCapacity_isFalse() {
        val waitlist = Waitlist(maxWaitlistCapacity = 2, joinedEntrantIds = mutableListOf("a"))
        assertFalse(waitlist.isFull())
    }

    @Test
    fun isFull_atCapacity_isTrue() {
        val waitlist = Waitlist(maxWaitlistCapacity = 2, joinedEntrantIds = mutableListOf("a", "b"))
        assertTrue(waitlist.isFull())
    }

    @Test
    fun isFull_overCapacity_isTrue() {
        val waitlist = Waitlist(maxWaitlistCapacity = 1, joinedEntrantIds = mutableListOf("a", "b"))
        assertTrue(waitlist.isFull())
    }

    @Test
    fun isFull_updatesAsEntrantsJoinAndLeave() {
        val waitlist = Waitlist(maxWaitlistCapacity = 1)
        assertFalse(waitlist.isFull())

        waitlist.joinedEntrantIds.add("a")
        assertTrue(waitlist.isFull())

        waitlist.joinedEntrantIds.remove("a")
        assertFalse(waitlist.isFull())
    }

    // --- isRegistrationOpen ---

    @Test
    fun isRegistrationOpen_insideWindow_isTrue() {
        val waitlist = Waitlist(registrationStartDate = hoursFromNow(-1), registrationEndDate = hoursFromNow(1))
        assertTrue(waitlist.isRegistrationOpen())
    }

    @Test
    fun isRegistrationOpen_beforeWindow_isFalse() {
        val waitlist = Waitlist(registrationStartDate = hoursFromNow(1), registrationEndDate = hoursFromNow(2))
        assertFalse(waitlist.isRegistrationOpen())
    }

    @Test
    fun isRegistrationOpen_afterWindow_isFalse() {
        val waitlist = Waitlist(registrationStartDate = hoursFromNow(-2), registrationEndDate = hoursFromNow(-1))
        assertFalse(waitlist.isRegistrationOpen())
    }

    @Test
    fun isRegistrationOpen_afterDraw_isFalse() {
        val waitlist = Waitlist(
            registrationStartDate = hoursFromNow(-1),
            registrationEndDate = hoursFromNow(1),
            isDrawn = true,
        )
        assertFalse(waitlist.isRegistrationOpen())
    }

    @Test
    fun isRegistrationOpen_withMissingDates_isFalse() {
        assertFalse(Waitlist().isRegistrationOpen())
        assertFalse(Waitlist(registrationStartDate = hoursFromNow(-1)).isRegistrationOpen())
        assertFalse(Waitlist(registrationEndDate = hoursFromNow(1)).isRegistrationOpen())
    }

    // --- Construction ---

    @Test
    fun noArgConstructor_matchesDefaults() {
        assertEquals(Waitlist(waitlistId = ""), Waitlist())
    }

    @Test
    fun defaults_drawOneWinnerWithNoCapacityLimit() {
        val waitlist = Waitlist()
        assertEquals(1, waitlist.numberOfWinnersToDraw)
        assertEquals(null, waitlist.maxWaitlistCapacity)
        assertFalse(waitlist.isDrawn)
    }

    @Test
    fun defaults_doNotShareEntrantListsBetweenInstances() {
        val first = Waitlist()
        val second = Waitlist()
        assertNotSame(first.joinedEntrantIds, second.joinedEntrantIds)

        first.joinedEntrantIds.add("a")
        assertTrue(second.joinedEntrantIds.isEmpty())
    }
}
