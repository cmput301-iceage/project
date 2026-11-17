package com.example.campsitelottery

import org.junit.Assert.*
import org.junit.Test
import java.util.Date
import java.util.TimeZone

class BookingPolicyTest {
    private fun date(text: String) = BookingPolicy.parse(text)!!
    private val now = date("2029-12-01")
    private fun lottery() = Waitlist(
        waitlistId = "lottery", ownerId = "owner", campsiteIds = listOf("site", "other"),
        isDrawn = true, selectedWinnerIds = mutableListOf("winner"),
        bookingStartDate = date("2030-01-01"), bookingEndDate = date("2030-01-10"),
    )
    private fun site() = Campsite(
        campsiteId = "site", ownerId = "owner", pricePerNight = 25.0,
        rules = "Quiet after 10", cancellationPolicy = "Seven days notice",
    )
    private fun request() = BookingRequest(
        "lottery", "winner", "site", date("2030-01-02"), date("2030-01-04"),
        true, "Quiet after 10", "Seven days notice", 25.0,
    )
    private fun validate(
        request: BookingRequest = request(),
        waitlist: Waitlist = lottery(),
        campsite: Campsite = site(),
        unavailable: Set<Long> = emptySet(),
        clock: Date = now,
    ) = BookingPolicy.validate(request, waitlist, campsite, unavailable, clock)

    private fun rejects(block: () -> Unit) {
        assertThrows(IllegalArgumentException::class.java, block)
    }

    @Test fun parsesOnlyStrictCalendarDates() {
        assertEquals("2030-02-28", BookingPolicy.format(date("2030-02-28")))
        listOf("", "2030-2-01", "2030-02-29", "2030-13-01", "2030-01-01junk", "2030-01-01 ").forEach {
            assertNull(it, BookingPolicy.parse(it))
        }
        assertNotNull(BookingPolicy.parse("2032-02-29"))
    }

    @Test fun datesDoNotChangeWithDeviceTimeZoneOrDst() {
        val old = TimeZone.getDefault()
        try {
            for (zone in listOf("America/Edmonton", "Pacific/Auckland", "Asia/Kolkata")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertEquals(3, BookingPolicy.nights(date("2030-03-09"), date("2030-03-12")).size)
                assertEquals("2030-03-10", BookingPolicy.format(date("2030-03-10")))
            }
        } finally { TimeZone.setDefault(old) }
    }

    @Test fun canadianLocalMidnightUsesSameCalendarDay() {
        val localMidnight = Date(date("2030-01-02").time + 7 * 3_600_000)
        assertEquals(date("2030-01-02").time, BookingPolicy.day(localMidnight))
    }

    @Test fun negativeEpochDaysRoundDown() {
        assertEquals(-BookingPolicy.DAY_MILLIS, BookingPolicy.day(Date(-1)))
    }

    @Test fun checkoutDoesNotOccupyANight() {
        assertEquals(listOf(date("2030-01-02").time, date("2030-01-03").time), validate())
    }

    @Test fun endDayIsLastAvailableNight() {
        assertEquals(listOf(date("2030-01-10").time),
            validate(request = request().copy(checkIn = date("2030-01-10"), checkOut = date("2030-01-11"))))
        rejects { validate(request().copy(checkIn = date("2030-01-10"), checkOut = date("2030-01-12"))) }
    }

    @Test fun rejectsEmptyReversedAndOverlongStays() {
        rejects { BookingPolicy.nights(date("2030-01-02"), date("2030-01-02")) }
        rejects { BookingPolicy.nights(date("2030-01-02"), date("2030-01-01")) }
        rejects { BookingPolicy.nights(date("2030-01-01"), date("2031-01-02")) }
        assertEquals(365, BookingPolicy.nights(date("2030-01-01"), date("2031-01-01")).size)
    }

    @Test fun onlyDrawnSelectedWinnersMayBook() {
        rejects { validate(waitlist = lottery().copy(isDrawn = false)) }
        rejects { validate(request = request().copy(entrantId = "loser")) }
        rejects { validate(waitlist = lottery().copy(declinedEntrantIds = mutableListOf("winner"))) }
        rejects { validate(waitlist = lottery().copy(bookedEntrantIds = mutableListOf("winner"))) }
    }

    @Test fun campsiteMustBeIncludedAndOwnedByLotteryOwner() {
        rejects { validate(waitlist = lottery().copy(campsiteIds = listOf("other"))) }
        rejects { validate(campsite = site().copy(ownerId = "different-owner")) }
        rejects { validate(campsite = site().copy(campsiteId = "other")) }
    }

    @Test fun requiresAffirmativeConsentToExactDisplayedTerms() {
        rejects { validate(request().copy(rulesAccepted = false)) }
        rejects { validate(campsite = site().copy(rules = "Updated rules")) }
        rejects { validate(campsite = site().copy(cancellationPolicy = "No refunds")) }
    }

    @Test fun rejectsChangedNegativeNonfiniteAndOverflowingPrices() {
        rejects { validate(campsite = site().copy(pricePerNight = 30.0)) }
        for (price in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE)) {
            rejects { validate(request().copy(pricePerNight = price), campsite = site().copy(pricePerNight = price)) }
        }
        assertEquals(2, validate(request().copy(pricePerNight = 0.0), campsite = site().copy(pricePerNight = 0.0)).size)
    }

    @Test fun rejectsPastAndOutOfPeriodNights() {
        rejects { validate(request().copy(checkIn = date("2029-12-31"))) }
        rejects { validate(clock = date("2030-01-03")) }
        rejects { validate(waitlist = lottery().copy(bookingStartDate = null)) }
        rejects { validate(waitlist = lottery().copy(bookingEndDate = date("2029-01-01"))) }
    }

    @Test fun registrationDeadlineIsNotAnAcceptanceDeadline() {
        assertEquals(2, validate(waitlist = lottery().copy(registrationEndDate = date("2029-01-01"))).size)
    }

    @Test fun conflictingNightsFailButAdjacentCheckoutIsAllowed() {
        rejects { validate(unavailable = setOf(date("2030-01-03").time)) }
        assertEquals(2, validate(unavailable = setOf(date("2030-01-04").time)).size)
    }

    @Test fun pendingAndConfirmedReservationsBlockButCancelledDoNot() {
        ReservationStatus.entries.forEach { status ->
            val reservation = Reservation(status = status, reservedDates = listOf(date("2030-01-02")))
            val expected = if (status == ReservationStatus.CONFIRMED || status == ReservationStatus.PENDING) 1 else 0
            assertEquals(status.name, expected, BookingPolicy.occupiedNights(reservation).size)
        }
    }

    @Test fun legacyCheckInOutRangeIsUsedWithoutReservedDates() {
        val reservation = Reservation(status = ReservationStatus.CONFIRMED,
            checkInDate = date("2030-01-02"), checkOutDate = date("2030-01-04"))
        assertEquals(2, BookingPolicy.occupiedNights(reservation).size)
        assertFalse(date("2030-01-04").time in BookingPolicy.occupiedNights(reservation))
    }

    @Test fun activeLegacyRecordsWithMissingDatesFailClosed() {
        listOf(ReservationStatus.PENDING, ReservationStatus.CONFIRMED).forEach { status ->
            rejects { BookingPolicy.occupiedNights(Reservation(status = status)) }
            rejects { BookingPolicy.occupiedNights(Reservation(status = status, checkInDate = date("2030-01-01"))) }
        }
    }

    @Test fun reservationIdsAreStableAndUnambiguous() {
        val id = BookingPolicy.reservationId("lottery", "winner")
        assertEquals(64, id.length)
        assertEquals(id, BookingPolicy.reservationId("lottery", "winner"))
        assertNotEquals(id, BookingPolicy.reservationId("lottery", "other"))
        assertNotEquals(BookingPolicy.reservationId("ab", "c"), BookingPolicy.reservationId("a", "bc"))
    }
}
