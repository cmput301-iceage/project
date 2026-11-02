package com.example.campsitelottery

import com.google.firebase.firestore.ListenerRegistration

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

    /**
     * Updates the contact details on an existing profile (US 7.3). Other fields,
     * such as roles and notification preference, are left untouched.
     *
     * Callers should validate and normalize input with [ProfileValidator] first.
     *
     * @param userId the profile to update.
     * @param name the new name.
     * @param email the new email address.
     * @param phoneNumber the new phone number, or null to clear it.
     * @param onSuccess called once the change is saved.
     * @param onFailure receives the error if the profile could not be updated,
     *   including when it does not exist.
     */
    fun updateContactInfo(
        userId: String,
        name: String,
        email: String,
        phoneNumber: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    )

    /**
     * Permanently deletes the profile stored under [userId] (US 8.1). Succeeds even
     * if the profile does not exist.
     *
     * @param userId the profile to delete.
     * @param onSuccess called once the profile is gone.
     * @param onFailure receives the error if the profile could not be deleted.
     */
    fun deleteUser(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    )

    /**
     * Opts the user in to or out of notifications (US 10.1) by setting
     * [User.notificationsEnabled]. Anything that sends notifications must check
     * that flag first.
     *
     * @param userId the profile to change.
     * @param enabled true to receive notifications, false to opt out.
     * @param onSuccess called once the preference is saved.
     * @param onFailure receives the error if the preference could not be saved,
     *   including when the profile does not exist.
     */
    fun setNotificationsEnabled(
        userId: String,
        enabled: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    )

    /**
     * Listens for changes to the Firestore users collection and retrieves
     * an updated list of users whenever a document is added, modified, or deleted.
     *
     * Each user is represented as a map containing their document fields
     * and their Firestore document ID under the key "userId".
     *
     * @param onSuccess callback invoked whenever the user collection changes.
     * @param onFailure callback invoked if the listener encounters an error.
     * @return A ListenerRegistration that can be used to stop listening.
     */
    fun getUsers(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit,
    ) : ListenerRegistration
}
