package com.example.campsitelottery

/**
 * [UserRepository] that keeps profiles in memory. Callbacks run synchronously.
 *
 * Used by Compose previews and unit tests so they don't need Firebase.
 *
 * @param initialUsers profiles to start with.
 */
class InMemoryUserRepository(initialUsers: List<User> = emptyList()) : UserRepository {

    private val users = initialUsers.associateBy { it.userId }.toMutableMap()

    /**
     * When set, every operation fails with this exception instead of running.
     * Lets tests exercise error handling.
     */
    var failWith: Exception? = null

    /** Returns the stored profile for [userId], or null if there is none. */
    fun peek(userId: String): User? = users[userId]?.copy()

    override fun getOrCreateUser(
        userId: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        failWith?.let { return onFailure(it) }
        val user = users.getOrPut(userId) { User(userId = userId) }
        onSuccess(user.copy())
    }

    override fun updateContactInfo(
        userId: String,
        name: String,
        email: String,
        phoneNumber: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        failWith?.let { return onFailure(it) }
        val user = users[userId] ?: return onFailure(NoSuchElementException("No profile for $userId"))
        users[userId] = user.copy(name = name, email = email, phoneNumber = phoneNumber)
        onSuccess()
    }

    override fun deleteUser(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        failWith?.let { return onFailure(it) }
        users.remove(userId)
        onSuccess()
    }

    override fun setNotificationsEnabled(
        userId: String,
        enabled: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        failWith?.let { return onFailure(it) }
        val user = users[userId] ?: return onFailure(NoSuchElementException("No profile for $userId"))
        users[userId] = user.copy(notificationsEnabled = enabled)
        onSuccess()
    }
}
