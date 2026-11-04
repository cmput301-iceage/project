package com.example.campsitelottery

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * [UserRepository] backed by the Firestore `users` collection, with one document per
 * user keyed by device ID.
 *
 * @param db the Firestore instance to use; defaults to the app's default instance.
 */
class FirestoreUserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserRepository {

    private val users get() = db.collection(USERS_COLLECTION)

    override fun getOrCreateUser(
        userId: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        val doc = users.document(userId)
        doc.get()
            .addOnSuccessListener { snapshot ->
                // Exceptions thrown in a success listener don't reach the failure
                // listener; they crash the app. Route bad documents to onFailure.
                val existing = try {
                    snapshot.toObject(User::class.java)
                } catch (e: RuntimeException) {
                    onFailure(e)
                    return@addOnSuccessListener
                }
                if (existing != null) {
                    onSuccess(existing)
                    return@addOnSuccessListener
                }
                val newUser = User(userId = userId)
                doc.set(newUser)
                    .addOnSuccessListener { onSuccess(newUser) }
                    .addOnFailureListener(onFailure)
            }
            .addOnFailureListener(onFailure)
    }

    override fun updateContactInfo(
        userId: String,
        name: String,
        email: String?,
        phoneNumber: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        // update() (not set()) so only these fields change, and it fails if the
        // profile was deleted instead of silently recreating it.
        users.document(userId)
            .update(
                mapOf(
                    "name" to name,
                    "email" to email,
                    "phoneNumber" to phoneNumber,
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    // TODO: once the waitlist/reservation schema (#198) lands, also remove this
    //  user from any waitlists they joined so they can't win a lottery after deleting.
    override fun deleteUser(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        users.document(userId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }

    override fun setNotificationsEnabled(
        userId: String,
        enabled: Boolean,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        users.document(userId)
            .update("notificationsEnabled", enabled)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onFailure)
    }



    override fun getUsers(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit,
    ): ListenerRegistration {
        return users.addSnapshotListener { snapshot, error ->

            if (error != null) {
                onFailure(error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val userList = snapshot.documents.map { document ->
                    document.data.orEmpty() + ("userId" to document.id)
                }

                onSuccess(userList)
            }
        }
    }

    companion object {
        /** Name of the Firestore collection that holds user profiles. */
        const val USERS_COLLECTION = "users"
    }
}
