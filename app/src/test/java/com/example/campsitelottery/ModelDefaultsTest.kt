package com.example.campsitelottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the defaults on [User] and [Campsite]. Firestore builds these through their
 * no-arg constructors, so those have to stay in step with the primary constructors.
 */
class ModelDefaultsTest {

    @Test
    fun user_noArgConstructorMatchesDefaults() {
        assertEquals(User(userId = ""), User())
    }

    @Test
    fun user_newProfileIsAnEntrantOnly() {
        val user = User()
        assertTrue(user.isEntrant)
        assertFalse(user.isOwner)
        assertFalse(user.isAdmin)
    }

    @Test
    fun user_newProfileIsOptedInToNotifications() {
        assertTrue(User().notificationsEnabled)
    }

    @Test
    fun user_contactDetailsAreOptional() {
        val user = User()
        assertNull(user.email)
        assertNull(user.phoneNumber)
        assertNull(user.profileImageUrl)
    }

    @Test
    fun campsite_noArgConstructorMatchesDefaults() {
        assertEquals(Campsite(campsiteId = ""), Campsite())
    }

    @Test
    fun campsite_defaultsToOneGuestAndNoOptionalDetails() {
        val campsite = Campsite()
        assertEquals(1, campsite.maxOccupancy)
        assertNull(campsite.location)
        assertNull(campsite.imageUrl)
        assertTrue(campsite.amenities.isEmpty())
        assertTrue(campsite.tags.isEmpty())
    }
}
