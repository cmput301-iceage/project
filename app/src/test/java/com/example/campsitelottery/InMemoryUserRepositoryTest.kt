package com.example.campsitelottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Unit tests for [InMemoryUserRepository], the fake used by previews and tests. */
class InMemoryUserRepositoryTest {

    @Test
    fun getOrCreateUser_createsEntrantProfileForNewDevice() {
        val repo = InMemoryUserRepository()

        var result: User? = null
        repo.getOrCreateUser("device-1", { result = it }, { fail("unexpected failure: $it") })

        assertEquals("device-1", result?.userId)
        assertTrue(result!!.isEntrant)
        assertTrue(result!!.notificationsEnabled)
        assertNotNull(repo.peek("device-1"))
    }

    @Test
    fun getOrCreateUser_returnsExistingProfileForKnownDevice() {
        val existing = User(userId = "device-1", name = "Jane", email = "jane@example.com")
        val repo = InMemoryUserRepository(listOf(existing))

        var result: User? = null
        repo.getOrCreateUser("device-1", { result = it }, { fail("unexpected failure: $it") })

        assertEquals(existing, result)
    }

    @Test
    fun getOrCreateUser_reportsFailure() {
        val repo = InMemoryUserRepository()
        val error = RuntimeException("offline")
        repo.failWith = error

        var reported: Exception? = null
        repo.getOrCreateUser("device-1", { fail("should not succeed") }, { reported = it })

        assertSame(error, reported)
        assertNull(repo.peek("device-1"))
    }

    @Test
    fun updateContactInfo_changesOnlyContactFields() {
        val existing = User(userId = "device-1", name = "Old", isOwner = true, notificationsEnabled = false)
        val repo = InMemoryUserRepository(listOf(existing))

        var saved = false
        repo.updateContactInfo(
            "device-1", "Jane", "jane@example.com", "7805550123",
            onSuccess = { saved = true },
            onFailure = { fail("unexpected failure: $it") },
        )

        assertTrue(saved)
        val stored = repo.peek("device-1")!!
        assertEquals("Jane", stored.name)
        assertEquals("jane@example.com", stored.email)
        assertEquals("7805550123", stored.phoneNumber)
        assertTrue(stored.isOwner)
        assertFalse(stored.notificationsEnabled)
    }

    @Test
    fun updateContactInfo_failsForMissingProfile() {
        val repo = InMemoryUserRepository()

        var reported: Exception? = null
        repo.updateContactInfo(
            "nobody", "Jane", "jane@example.com", null,
            onSuccess = { fail("should not succeed") },
            onFailure = { reported = it },
        )

        assertNotNull(reported)
        assertNull(repo.peek("nobody"))
    }

    @Test
    fun deleteUser_removesProfile() {
        val repo = InMemoryUserRepository(listOf(User(userId = "device-1", name = "Jane")))

        var deleted = false
        repo.deleteUser("device-1", { deleted = true }, { fail("unexpected failure: $it") })

        assertTrue(deleted)
        assertNull(repo.peek("device-1"))
    }

    @Test
    fun getOrCreateUser_afterDelete_createsFreshProfile() {
        val repo = InMemoryUserRepository(listOf(User(userId = "device-1", name = "Jane")))
        repo.deleteUser("device-1", {}, { fail("unexpected failure: $it") })

        var result: User? = null
        repo.getOrCreateUser("device-1", { result = it }, { fail("unexpected failure: $it") })

        assertEquals("", result?.name)
    }

    @Test
    fun deleteUser_reportsFailureAndKeepsProfile() {
        val repo = InMemoryUserRepository(listOf(User(userId = "device-1")))
        repo.failWith = RuntimeException("offline")

        var reported: Exception? = null
        repo.deleteUser("device-1", { fail("should not succeed") }, { reported = it })

        assertNotNull(reported)
        assertNotNull(repo.peek("device-1"))
    }

    @Test
    fun setNotificationsEnabled_optsOutAndBackIn() {
        val repo = InMemoryUserRepository(listOf(User(userId = "device-1", name = "Jane")))

        repo.setNotificationsEnabled("device-1", false, {}, { fail("unexpected failure: $it") })
        assertFalse(repo.peek("device-1")!!.notificationsEnabled)
        assertEquals("Jane", repo.peek("device-1")!!.name)

        repo.setNotificationsEnabled("device-1", true, {}, { fail("unexpected failure: $it") })
        assertTrue(repo.peek("device-1")!!.notificationsEnabled)
    }

    @Test
    fun setNotificationsEnabled_failsForMissingProfile() {
        val repo = InMemoryUserRepository()

        var reported: Exception? = null
        repo.setNotificationsEnabled("nobody", false, { fail("should not succeed") }, { reported = it })

        assertNotNull(reported)
    }
}
