package com.example.campsitelottery

/**
 * What the user has typed or chosen on the home screen to narrow down the campsite list.
 *
 * @property query free-text search (US 1.1); blank matches everything.
 */
data class CampsiteFilter(
    val query: String = "",
)

/**
 * Searches and filters campsites on the device. Kept free of Android and Firebase
 * so it can be unit tested.
 */
object CampsiteSearch {

    /**
     * Returns the campsites that match [filter], in their original order.
     *
     * @param campsites every campsite to choose from.
     * @param filter the user's search criteria.
     */
    fun apply(campsites: List<Campsite>, filter: CampsiteFilter): List<Campsite> =
        campsites.filter { matchesQuery(it, filter.query) }

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

    private val WHITESPACE = Regex("\\s+")
}
