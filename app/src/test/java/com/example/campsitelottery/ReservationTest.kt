package com.example.campsitelottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for the [Reservation] status helpers and defaults. */
class ReservationTest {

    private fun withStatus(status: ReservationStatus) = Reservation(status = status)

    @Test
    fun isActive_onlyForConfirmed() {
        val active = ReservationStatus.entries.filter { withStatus(it).isActive() }
        assertEquals(listOf(ReservationStatus.CONFIRMED), active)
    }

    @Test
    fun isPast_forCompletedCancelledAndRejected() {
        val past = ReservationStatus.entries.filter { withStatus(it).isPast() }
        assertEquals(
            listOf(
                ReservationStatus.CANCELLED_BY_ENTRANT,
                ReservationStatus.REJECTED_BY_OWNER,
                ReservationStatus.COMPLETED,
            ),
            past,
        )
    }

    @Test
    fun pending_isNeitherActiveNorPast() {
        val pending = withStatus(ReservationStatus.PENDING)
        assertFalse(pending.isActive())
        assertFalse(pending.isPast())
    }

    @Test
    fun noStatus_isBothActiveAndPast() {
        ReservationStatus.entries.forEach { status ->
            val reservation = withStatus(status)
            assertFalse(status.name, reservation.isActive() && reservation.isPast())
        }
    }

    @Test
    fun statusChange_updatesHelpers() {
        val reservation = withStatus(ReservationStatus.CONFIRMED)
        assertTrue(reservation.isActive())

        reservation.status = ReservationStatus.CANCELLED_BY_ENTRANT

        assertFalse(reservation.isActive())
        assertTrue(reservation.isPast())
    }

    @Test
    fun noArgConstructor_startsPendingWithNoDatesAndRulesNotAccepted() {
        val reservation = Reservation()
        assertEquals(ReservationStatus.PENDING, reservation.status)
        assertFalse(reservation.rulesAccepted)
        assertNull(reservation.checkInDate)
        assertNull(reservation.checkOutDate)
        assertTrue(reservation.reservedDates.isEmpty())
        assertEquals(0.0, reservation.totalPrice, 0.0)
    }
}
