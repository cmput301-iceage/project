package com.example.campsitelottery

/**
 * Per-field validation errors for the profile edit form. A null field means that
 * field is valid.
 *
 * @property name error for the name field, if any.
 * @property email error for the email field, if any.
 * @property phoneNumber error for the phone number field, if any.
 */
data class ProfileInputErrors(
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
) {
    /** True when every field is valid. */
    val isValid: Boolean get() = name == null && email == null && phoneNumber == null
}

/**
 * Validates and normalizes the contact details a user enters on their profile (US 7.3).
 *
 * Name is required. Email and phone number are optional, but must be well formed
 * when given.
 */
object ProfileValidator {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    /** Characters people commonly type between phone digits. */
    private val PHONE_SEPARATORS = Regex("[\\s().-]")

    /** Shortest and longest digit counts accepted (E.164 allows up to 15). */
    private const val MIN_PHONE_DIGITS = 7
    private const val MAX_PHONE_DIGITS = 15

    /**
     * Checks the raw form input. Leading and trailing whitespace is ignored.
     *
     * @return the errors found; [ProfileInputErrors.isValid] is true if there are none.
     */
    fun validate(name: String, email: String, phoneNumber: String): ProfileInputErrors {
        val trimmedEmail = email.trim()
        val trimmedPhone = phoneNumber.trim()
        return ProfileInputErrors(
            name = if (name.isBlank()) "Name is required" else null,
            email = when {
                trimmedEmail.isEmpty() || EMAIL_REGEX.matches(trimmedEmail) -> null
                else -> "Enter a valid email address"
            },
            phoneNumber = if (trimmedPhone.isEmpty() || isValidPhone(trimmedPhone)) null
            else "Enter a valid phone number",
        )
    }

    /** Trims [name] for storage. */
    fun normalizeName(name: String): String = name.trim()

    /** Trims [email] for storage, turning a blank email into null. */
    fun normalizeEmail(email: String): String? = email.trim().ifEmpty { null }

    /** Trims [phoneNumber] for storage, turning a blank phone number into null. */
    fun normalizePhone(phoneNumber: String): String? = phoneNumber.trim().ifEmpty { null }

    private fun isValidPhone(phone: String): Boolean {
        val digits = phone.removePrefix("+").replace(PHONE_SEPARATORS, "")
        return digits.all { it.isDigit() } && digits.length in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS
    }
}
