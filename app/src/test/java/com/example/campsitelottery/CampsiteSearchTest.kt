package com.example.campsitelottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.Locale

/** Unit tests for [CampsiteSearch]. */
class CampsiteSearchTest {

    private val lakeside = Campsite(
        campsiteId = "1",
        name = "Lakeside Grove",
        address = "12 Shore Rd, Jasper, AB",
        description = "Quiet sites by the water",
        maxOccupancy = 6,
        tags = listOf("tent", "pet friendly"),
    )
    private val ridge = Campsite(
        campsiteId = "2",
        name = "Ridge Top",
        address = "88 Summit Way, Canmore, AB",
        description = "RV hookups with mountain views",
        maxOccupancy = 2,
        tags = listOf("rv"),
    )
    private val all = listOf(lakeside, ridge)

    @Test
    fun blankQuery_matchesEverything() {
        assertEquals(all, CampsiteSearch.apply(all, CampsiteFilter(query = "  ")))
    }

    @Test
    fun query_matchesNameIgnoringCase() {
        assertEquals(listOf(lakeside), CampsiteSearch.apply(all, CampsiteFilter(query = "LAKESIDE")))
    }

    @Test
    fun query_matchesAddressDescriptionAndTags() {
        assertTrue(CampsiteSearch.matchesQuery(ridge, "canmore"))
        assertTrue(CampsiteSearch.matchesQuery(ridge, "mountain"))
        assertTrue(CampsiteSearch.matchesQuery(lakeside, "pet"))
    }

    @Test
    fun multiWordQuery_requiresEveryWord() {
        assertTrue(CampsiteSearch.matchesQuery(lakeside, "lake tent"))
        assertFalse(CampsiteSearch.matchesQuery(lakeside, "lake rv"))
    }

    @Test
    fun noMatches_returnsEmptyList() {
        assertEquals(emptyList<Campsite>(), CampsiteSearch.apply(all, CampsiteFilter(query = "beach")))
    }

    // --- Filters (US 1.2) ---

    /** Nov 6, 2026 00:00 UTC, as the Material date picker would return it. */
    private val nov6 = 1793923200000L
    private val day = 24L * 60 * 60 * 1000

    private fun lottery(vararg campsiteIds: String, startMillis: Long?, endMillis: Long?) = Waitlist(
        campsiteIds = campsiteIds.toList(),
        bookingStartDate = startMillis?.let(::Date),
        bookingEndDate = endMillis?.let(::Date),
    )

    @Test
    fun location_matchesAddressIgnoringCaseAndSpaces() {
        assertEquals(listOf(lakeside), CampsiteSearch.apply(all, CampsiteFilter(location = "  jasper ")))
        assertEquals(all, CampsiteSearch.apply(all, CampsiteFilter(location = "AB")))
    }

    @Test
    fun location_doesNotMatchName() {
        assertFalse(CampsiteSearch.matchesLocation(ridge, "Ridge"))
    }

    @Test
    fun guests_excludesCampsitesThatAreTooSmall() {
        assertEquals(listOf(lakeside), CampsiteSearch.apply(all, CampsiteFilter(guests = 3)))
        assertEquals(all, CampsiteSearch.apply(all, CampsiteFilter(guests = 2)))
        assertEquals(emptyList<Campsite>(), CampsiteSearch.apply(all, CampsiteFilter(guests = 7)))
    }

    @Test
    fun date_matchesOnlyCampsitesWithABookingPeriodCoveringIt() {
        val waitlists = listOf(lottery("1", startMillis = nov6 - 2 * day, endMillis = nov6 + 2 * day))
        assertEquals(listOf(lakeside), CampsiteSearch.apply(all, CampsiteFilter(dateMillis = nov6), waitlists))
    }

    @Test
    fun date_includesFirstAndLastDayOfBookingPeriod() {
        val waitlists = listOf(lottery("1", startMillis = nov6, endMillis = nov6 + 3 * day))
        assertTrue(CampsiteSearch.isBookableOn(lakeside, nov6, waitlists))
        assertTrue(CampsiteSearch.isBookableOn(lakeside, nov6 + 3 * day, waitlists))
        assertFalse(CampsiteSearch.isBookableOn(lakeside, nov6 - day, waitlists))
        assertFalse(CampsiteSearch.isBookableOn(lakeside, nov6 + 4 * day, waitlists))
    }

    @Test
    fun date_matchesEveryCampsiteInAMultiSiteLottery() {
        val waitlists = listOf(lottery("1", "2", startMillis = nov6, endMillis = nov6))
        assertEquals(all, CampsiteSearch.apply(all, CampsiteFilter(dateMillis = nov6), waitlists))
    }

    @Test
    fun date_ignoresLotteriesWithoutBookingDates() {
        val waitlists = listOf(lottery("1", startMillis = null, endMillis = nov6 + day))
        assertFalse(CampsiteSearch.isBookableOn(lakeside, nov6, waitlists))
    }

    @Test
    fun date_withNoLotteries_matchesNothing() {
        assertEquals(emptyList<Campsite>(), CampsiteSearch.apply(all, CampsiteFilter(dateMillis = nov6)))
    }

    @Test
    fun filtersAndQuery_combine() {
        val filter = CampsiteFilter(query = "views", location = "AB", guests = 2)
        assertEquals(listOf(ridge), CampsiteSearch.apply(all, filter))
    }

    @Test
    fun hasFilters_ignoresSearchText() {
        assertFalse(CampsiteFilter(query = "lake").hasFilters)
        assertTrue(CampsiteFilter(guests = 1).hasFilters)
        assertTrue(CampsiteFilter(dateMillis = nov6).hasFilters)
    }

    @Test
    fun parseGuests_acceptsOnlyPositiveWholeNumbers() {
        assertEquals(4, CampsiteSearch.parseGuests(" 4 "))
        assertNull(CampsiteSearch.parseGuests(""))
        assertNull(CampsiteSearch.parseGuests("0"))
        assertNull(CampsiteSearch.parseGuests("-2"))
        assertNull(CampsiteSearch.parseGuests("2.5"))
        assertTrue(CampsiteSearch.isValidGuestInput(""))
        assertFalse(CampsiteSearch.isValidGuestInput("abc"))
    }

    @Test
    fun formatDate_usesUtcDay() {
        assertEquals("Nov 6, 2026", CampsiteSearch.formatDate(nov6, Locale.CANADA))
    }

    // --- Filter summary (US 1.3) ---

    @Test
    fun describe_listsOnlyFiltersThatAreSet() {
        assertEquals(emptyList<String>(), CampsiteSearch.describe(CampsiteFilter(query = "lake")))
        assertEquals(
            listOf("Jasper", "4+ guests", "Nov 6, 2026"),
            CampsiteSearch.describe(CampsiteFilter(location = " Jasper ", guests = 4, dateMillis = nov6), Locale.CANADA),
        )
    }

    @Test
    fun describe_singularGuest() {
        assertEquals(listOf("1 guest"), CampsiteSearch.describe(CampsiteFilter(guests = 1)))
    }
}
