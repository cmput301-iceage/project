package com.example.campsitelottery

/**
 * Reads and writes [User] profiles.
 *
 * Screens depend on this interface rather than on Firestore directly, so they can be
 * previewed and tested with [InMemoryUserRepository]. The real implementation is
 * [FirestoreUserRepository].
 *
 * All operations are asynchronous: exactly one of the success or failure callbacks
 * is invoked once the operation finishes.
 */
interface UserRepository {

    /**
     * Loads the profile stored under [userId], creating an empty entrant profile if
     * none exists yet. This is how a device is registered the first time the app runs.
     *
     * @param userId the user's device ID (see [DeviceIdentity]).
     * @param onSuccess receives the existing or newly created user.
     * @param onFailure receives the error if the profile could not be read or created.
     */
    fun getOrCreateUser(
        userId: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit,
    )
}
