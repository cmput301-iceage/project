package com.example.campsitelottery

import java.util.Date
import kotlin.random.Random

/**
 * Encapsulates the results of a lottery draw execution.
 *
 * @property waitlist The updated [Waitlist] object with [Waitlist.isDrawn] set to true and [Waitlist.selectedWinnerIds] populated.
 * @property winners List of entrant user IDs who were selected as winners.
 * @property losers List of entrant user IDs who were not selected in the draw.
 */
data class WaitlistDrawResult(
    val waitlist: Waitlist,
    val winners: List<String>,
    val losers: List<String>
)

/**
 * Pure domain logic helper for executing lottery draws and validating registration completion.
 */
object LotteryDrawPolicy {

    /**
     * Executes the drawing logic for a [Waitlist].
     *
     * If the lottery is already drawn, returns the current waitlist and entrant win/loss partition.
     * Otherwise, randomly selects up to [Waitlist.numberOfWinnersToDraw] entrants from [Waitlist.joinedEntrantIds].
     *
     * @param waitlist The waitlist to draw.
     * @param random Random number generator (overridable for testing).
     * @return [WaitlistDrawResult] containing the updated [Waitlist], winner IDs, and loser IDs.
     */
    fun draw(
        waitlist: Waitlist,
        random: Random = Random.Default
    ): WaitlistDrawResult {
        if (waitlist.isDrawn) {
            val winners = waitlist.selectedWinnerIds.toList()
            val losers = waitlist.joinedEntrantIds.filter { it !in winners }
            return WaitlistDrawResult(waitlist, winners, losers)
        }

        val pool = waitlist.joinedEntrantIds.distinct()
        if (pool.isEmpty()) {
            val updated = waitlist.copy(
                isDrawn = true,
                selectedWinnerIds = mutableListOf()
            )
            return WaitlistDrawResult(updated, emptyList(), emptyList())
        }

        val quota = waitlist.numberOfWinnersToDraw.coerceAtLeast(0)
        val winnersCount = minOf(quota, pool.size)

        val winners = pool.shuffled(random).take(winnersCount)
        val losers = pool.filter { it !in winners }

        val updated = waitlist.copy(
            isDrawn = true,
            selectedWinnerIds = winners.toMutableList()
        )

        return WaitlistDrawResult(updated, winners, losers)
    }

    /**
     * Determines whether a [Waitlist] is eligible for automatic drawing based on current date.
     *
     * @param waitlist The waitlist to check.
     * @param now Current date/time reference.
     * @return True if the registration end date is set, the current date is on or after registration end date, and draw has not run.
     */
    fun shouldDraw(waitlist: Waitlist, now: Date = Date()): Boolean {
        if (waitlist.isDrawn) return false
        val endDate = waitlist.registrationEndDate ?: return false
        return !now.before(endDate)
    }
}
