package com.example.campsitelottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
