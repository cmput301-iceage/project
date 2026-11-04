package com.example.campsitelottery

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * What the user has typed or chosen on the home screen to narrow down the campsite list.
 *
 * @property query free-text search (US 1.1); blank matches everything.
 * @property location text the campsite's address must contain (US 1.2); blank matches everything.
 * @property guests number of people who need to fit (US 1.2); null for any.
 * @property dateMillis a day the user wants to camp (US 1.2), as UTC midnight in epoch
 *   milliseconds, which is what the Material date picker returns; null for any day.
 */
data class CampsiteFilter(
    val query: String = "",
    val location: String = "",
    val guests: Int? = null,
    val dateMillis: Long? = null,
) {
    /** True if any filter other than the search text is set. */
    val hasFilters: Boolean get() = location.isNotBlank() || guests != null || dateMillis != null
}

/**
 * Searches and filters campsites on the device. Kept free of Android and Firebase
 * so it can be unit tested.
 */
object CampsiteSearch {

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    /**
     * Returns the campsites that match every part of [filter], in their original order.
     *
     * @param campsites every campsite to choose from.
     * @param filter the user's search and filter criteria.
     * @param waitlists every lottery, used for the date filter; a campsite is only
     *   bookable on a date if one of its lotteries has a booking period covering it.
     */
    fun apply(
        campsites: List<Campsite>,
        filter: CampsiteFilter,
        waitlists: List<Waitlist> = emptyList(),
    ): List<Campsite> = campsites.filter { campsite ->
        matchesQuery(campsite, filter.query) &&
            matchesLocation(campsite, filter.location) &&
            fitsGuests(campsite, filter.guests) &&
            (filter.dateMillis == null || isBookableOn(campsite, filter.dateMillis, waitlists))
    }

    /**
     * True if every word in [query] appears, ignoring case, somewhere in the
     * campsite's name, address, description or tags (US 1.1). So "lake tent" matches
     * a campsite named "Lakeside" tagged "tent". A blank query matches every campsite.
     */
    fun matchesQuery(campsite: Campsite, query: String): Boolean {
        val words = query.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val haystack = listOf(campsite.name, campsite.address, campsite.description)
            .plus(campsite.tags)
            .joinToString(" ")
        return words.all { haystack.contains(it, ignoreCase = true) }
    }

    /** True if the campsite's address contains [location], ignoring case and surrounding spaces. */
    fun matchesLocation(campsite: Campsite, location: String): Boolean =
        location.isBlank() || campsite.address.contains(location.trim(), ignoreCase = true)

    /** True if the campsite holds at least [guests] people, or [guests] is null. */
    fun fitsGuests(campsite: Campsite, guests: Int?): Boolean =
        guests == null || campsite.maxOccupancy >= guests

    /**
     * True if some lottery that includes [campsite] has a booking period overlapping
     * the day starting at [dateMillis] (UTC midnight). Lotteries without both booking
     * dates set are ignored.
     */
    fun isBookableOn(campsite: Campsite, dateMillis: Long, waitlists: List<Waitlist>): Boolean {
        val dayStart = dateMillis
        val dayEnd = dateMillis + DAY_MILLIS
        return waitlists.any { waitlist ->
            val start = waitlist.bookingStartDate?.time ?: return@any false
            val end = waitlist.bookingEndDate?.time ?: return@any false
            campsite.campsiteId in waitlist.campsiteIds && start < dayEnd && end >= dayStart
        }
    }

    /**
     * Parses a guest count typed by the user.
     *
     * @return the count, null if [text] is blank (no guest filter), or null and
     *   [isValidGuestInput] false if it is not a positive whole number.
     */
    fun parseGuests(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it > 0 }

    /** True if [text] is blank or a positive whole number. */
    fun isValidGuestInput(text: String): Boolean = text.isBlank() || parseGuests(text) != null

    /**
     * Short labels for the filters that are set, shown above the results (US 1.3),
     * e.g. `["Jasper", "4+ guests", "Nov 6, 2026"]`. The search text is not included.
     */
    fun describe(filter: CampsiteFilter, locale: Locale = Locale.getDefault()): List<String> =
        listOfNotNull(
            filter.location.trim().ifEmpty { null },
            filter.guests?.let { if (it == 1) "1 guest" else "$it+ guests" },
            filter.dateMillis?.let { formatDate(it, locale) },
        )

    /** Formats a date-picker value (UTC midnight) as e.g. "Nov 6, 2026". */
    fun formatDate(dateMillis: Long, locale: Locale = Locale.getDefault()): String =
        SimpleDateFormat("MMM d, yyyy", locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(dateMillis))

    private val WHITESPACE = Regex("\\s+")
}
