package com.example.campsitelottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import kotlin.random.Random

class LotteryDrawPolicyTest {

    @Test
    fun draw_withMoreEntrantsThanWinners_selectsExactNumberOfWinners() {
        val waitlist = Waitlist(
            waitlistId = "w1",
            numberOfWinnersToDraw = 2,
            joinedEntrantIds = mutableListOf("user1", "user2", "user3", "user4")
        )

        val result = LotteryDrawPolicy.draw(waitlist, random = Random(42))

        assertTrue(result.waitlist.isDrawn)
        assertEquals(2, result.winners.size)
        assertEquals(2, result.losers.size)
        assertEquals(4, result.winners.size + result.losers.size)
        assertTrue(result.winners.all { it in waitlist.joinedEntrantIds })
        assertTrue(result.losers.all { it in waitlist.joinedEntrantIds })
        assertTrue(result.winners.none { it in result.losers })
    }

    @Test
    fun draw_withFewerEntrantsThanWinners_selectsAllEntrantsAsWinners() {
        val waitlist = Waitlist(
            waitlistId = "w2",
            numberOfWinnersToDraw = 5,
            joinedEntrantIds = mutableListOf("user1", "user2")
        )

        val result = LotteryDrawPolicy.draw(waitlist, random = Random(42))

        assertTrue(result.waitlist.isDrawn)
        assertEquals(2, result.winners.size)
        assertTrue(result.losers.isEmpty())
        assertTrue(result.winners.containsAll(listOf("user1", "user2")))
    }

    @Test
    fun draw_withZeroEntrants_setsDrawnWithNoWinnersOrLosers() {
        val waitlist = Waitlist(
            waitlistId = "w3",
            numberOfWinnersToDraw = 3,
            joinedEntrantIds = mutableListOf()
        )

        val result = LotteryDrawPolicy.draw(waitlist)

        assertTrue(result.waitlist.isDrawn)
        assertTrue(result.winners.isEmpty())
        assertTrue(result.losers.isEmpty())
    }

    @Test
    fun draw_alreadyDrawnLottery_returnsUnchangedWinnersAndLosers() {
        val waitlist = Waitlist(
            waitlistId = "w4",
            isDrawn = true,
            joinedEntrantIds = mutableListOf("user1", "user2", "user3"),
            selectedWinnerIds = mutableListOf("user1")
        )

        val result = LotteryDrawPolicy.draw(waitlist)

        assertTrue(result.waitlist.isDrawn)
        assertEquals(listOf("user1"), result.winners)
        assertEquals(listOf("user2", "user3"), result.losers)
    }

    @Test
    fun shouldDraw_returnsTrueWhenRegistrationEndIsPast() {
        val past = Date(System.currentTimeMillis() - 10_000)
        val waitlist = Waitlist(
            registrationEndDate = past,
            isDrawn = false
        )

        assertTrue(LotteryDrawPolicy.shouldDraw(waitlist))
    }

    @Test
    fun shouldDraw_returnsFalseWhenRegistrationEndIsFuture() {
        val future = Date(System.currentTimeMillis() + 10_000)
        val waitlist = Waitlist(
            registrationEndDate = future,
            isDrawn = false
        )

        assertFalse(LotteryDrawPolicy.shouldDraw(waitlist))
    }

    @Test
    fun shouldDraw_returnsFalseWhenAlreadyDrawn() {
        val past = Date(System.currentTimeMillis() - 10_000)
        val waitlist = Waitlist(
            registrationEndDate = past,
            isDrawn = true
        )

        assertFalse(LotteryDrawPolicy.shouldDraw(waitlist))
    }

    @Test
    fun shouldDraw_returnsFalseWhenRegistrationEndIsNull() {
        val waitlist = Waitlist(
            registrationEndDate = null,
            isDrawn = false
        )

        assertFalse(LotteryDrawPolicy.shouldDraw(waitlist))
    }
}
