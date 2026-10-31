package com.example.campsitelottery

import com.google.firebase.firestore.FirebaseFirestore

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
                val existing = snapshot.toObject(User::class.java)
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
        email: String,
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

    companion object {
        /** Name of the Firestore collection that holds user profiles. */
        const val USERS_COLLECTION = "users"
    }
}
