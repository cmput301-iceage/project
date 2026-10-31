package com.example.campsitelottery

import org.junit.Assert.assertEquals
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
}
