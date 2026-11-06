package com.example.campsitelottery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [ProfileValidator] (US 7.3). */
class ProfileValidatorTest {

    @Test
    fun validInputWithoutPhone_isValid() {
        assertTrue(ProfileValidator.validate("Jane", "jane@example.com", "").isValid)
    }

    @Test
    fun validInputWithPhone_isValid() {
        assertTrue(ProfileValidator.validate("Jane", "jane@example.com", "(780) 555-0123").isValid)
        assertTrue(ProfileValidator.validate("Jane", "jane@example.com", "+1 780.555.0123").isValid)
    }

    @Test
    fun blankName_isRejected() {
        val errors = ProfileValidator.validate("   ", "jane@example.com", "")
        assertNotNull(errors.name)
        assertFalse(errors.isValid)
    }

    @Test
    fun missingEmail_isAllowed() {
        assertTrue(ProfileValidator.validate("Jane", "", "").isValid)
        assertTrue(ProfileValidator.validate("Jane", "   ", "").isValid)
    }

    @Test
    fun malformedEmail_isRejected() {
        listOf("jane", "jane@", "jane@example", "@example.com", "jane@example.c", "ja ne@example.com")
            .forEach { assertNotNull(it, ProfileValidator.validate("Jane", it, "").email) }
    }

    @Test
    fun emailWithSurroundingWhitespace_isAccepted() {
        assertNull(ProfileValidator.validate("Jane", "  jane@example.com ", "").email)
    }

    @Test
    fun malformedPhone_isRejected() {
        listOf("123", "780-555-01x3", "1234567890123456", "++17805550123")
            .forEach { assertNotNull(it, ProfileValidator.validate("Jane", "jane@example.com", it).phoneNumber) }
    }

    @Test
    fun phoneDigitCount_acceptsSevenToFifteen() {
        assertNotNull(ProfileValidator.validate("Jane", "", "123456").phoneNumber)
        assertNull(ProfileValidator.validate("Jane", "", "1234567").phoneNumber)
        assertNull(ProfileValidator.validate("Jane", "", "123456789012345").phoneNumber)
        assertNotNull(ProfileValidator.validate("Jane", "", "1234567890123456").phoneNumber)
    }

    @Test
    fun phoneDigitCount_ignoresPlusAndSeparators() {
        assertNull(ProfileValidator.validate("Jane", "", "+1 (23) 456-7").phoneNumber)
        assertNotNull(ProfileValidator.validate("Jane", "", "+ ( ) - .").phoneNumber)
    }

    @Test
    fun blankPhone_isAllowed() {
        assertNull(ProfileValidator.validate("Jane", "", "   ").phoneNumber)
    }

    @Test
    fun errors_areReportedPerField() {
        val errors = ProfileValidator.validate("", "jane", "123")
        assertNotNull(errors.name)
        assertNotNull(errors.email)
        assertNotNull(errors.phoneNumber)
    }

    @Test
    fun isValid_isFalseIfAnyFieldHasAnError() {
        assertTrue(ProfileInputErrors().isValid)
        assertFalse(ProfileInputErrors(name = "error").isValid)
        assertFalse(ProfileInputErrors(email = "error").isValid)
        assertFalse(ProfileInputErrors(phoneNumber = "error").isValid)
    }

    @Test
    fun normalize_trimsAndClearsBlankPhone() {
        assertEquals("Jane", ProfileValidator.normalizeName("  Jane "))
        assertEquals("jane@example.com", ProfileValidator.normalizeEmail(" jane@example.com "))
        assertNull(ProfileValidator.normalizeEmail("   "))
        assertNull(ProfileValidator.normalizePhone("   "))
        assertEquals("780 555 0123", ProfileValidator.normalizePhone(" 780 555 0123 "))
    }
}
