package com.example.campsitelottery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

/**
 * Shows the current device user's profile and lets them edit their name, email and
 * phone number (US 7.3), delete their profile (US 8.1) or opt out of notifications
 * (US 10.1), plus links to their reservations, notifications and lotteries.
 *
 * @param navController used to go back and to open the linked screens.
 * @param userState the current user's profile, as loaded by [CampsiteLotteryApp].
 * @param userRepository where profile changes are saved.
 * @param onUserChanged called after a profile change or notification preference is
 *   saved, with a function that applies that change to a [User]. The caller applies
 *   it to its latest copy of the user, so overlapping saves don't undo each other.
 * @param onUserDeleted called after the profile has been deleted.
 * @param onRetry called to load the profile again: after a failed load, or to create
 *   a fresh profile after deleting one.
 */
@Composable
fun ProfileScreen(
    navController : NavController,
    userState: UserState,
    userRepository: UserRepository,
    onUserChanged: ((User) -> User) -> Unit,
    onUserDeleted: () -> Unit,
    onRetry: () -> Unit = {},
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var isSavingNotifications by remember { mutableStateOf(false) }
    var notificationError by remember { mutableStateOf<String?>(null) }

    fun toggleNotifications(user: User) {
        val enabled = !user.notificationsEnabled
        isSavingNotifications = true
        notificationError = null
        userRepository.setNotificationsEnabled(
            user.userId,
            enabled,
            onSuccess = {
                isSavingNotifications = false
                onUserChanged { it.copy(notificationsEnabled = enabled) }
            },
            onFailure = {
                isSavingNotifications = false
                notificationError = "Couldn't update notification settings: ${it.message}"
            },
        )
    }

    if (showDeleteDialog && userState is UserState.Ready) {
        DeleteProfileDialog(
            onConfirm = {
                showDeleteDialog = false
                isDeleting = true
                deleteError = null
                userRepository.deleteUser(
                    userState.user.userId,
                    onSuccess = {
                        isDeleting = false
                        onUserDeleted()
                    },
                    onFailure = {
                        isDeleting = false
                        deleteError = "Couldn't delete your profile: ${it.message}"
                    },
                )
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Column(
        // Scrollable + imePadding so the edit form's Save button stays reachable in
        // landscape, with large fonts, or with the keyboard open.
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),

        ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Absolute.Left

        ) {
            Button(
                onClick = { navController.popBackStack() },
                shape = RectangleShape,
                modifier = Modifier.testTag("toBackButton")

            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showDeleteDialog = true },
                enabled = userState is UserState.Ready && !isDeleting,
                shape = RectangleShape,
                modifier = Modifier.testTag("deleteProfileButton"),
                ) {
                Text(if (isDeleting) "Deleting..." else "Delete Profile")
            }

        }

        deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth().testTag("profileInfo")
        ) {
            when (userState) {
                is UserState.Loading -> Text("Loading profile...")
                is UserState.Deleted -> {
                    Text("Your profile has been deleted.")
                    Button(
                        onClick = onRetry,
                        shape = RectangleShape,
                        modifier = Modifier.testTag("createProfileButton"),
                    ) {
                        Text("Create New Profile")
                    }
                }
                is UserState.Failed -> {
                    Text(
                        "Couldn't load your profile: ${userState.message}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onRetry, shape = RectangleShape) { Text("Retry") }
                }
                is UserState.Ready ->
                    if (isEditing) {
                        EditProfileForm(
                            user = userState.user,
                            userRepository = userRepository,
                            onSaved = { change ->
                                onUserChanged(change)
                                isEditing = false
                            },
                            onCancel = { isEditing = false },
                        )
                    } else {
                        ProfileDetails(userState.user)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { isEditing = true },
                            shape = RectangleShape,
                            modifier = Modifier.testTag("editProfileButton"),
                        ) {
                            Text("Edit Profile")
                        }
                    }
            }
        }

        Spacer(modifier = Modifier.height(200.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //TODO:
            Button(
                onClick = { navController.navigate("reservation") },
                modifier = Modifier.fillMaxWidth().testTag("toReserveButton"),
                shape = RectangleShape
            ) {
                Text("View Reservations")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("notification") },
                modifier = Modifier.fillMaxWidth().testTag("toNotificationButton"),
                shape = RectangleShape
            ) {
                Text("View Notifications")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("lottery") },
                modifier = Modifier.fillMaxWidth().testTag("toLotteryButton"),
                shape = RectangleShape
            ) {
                Text("View Lotteries")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        notificationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { }, //TODO: use a popout to display app policy and guidelines
                modifier = Modifier.testTag("toPolicyButton"),
                shape = RectangleShape
            ) {
                Text(text = "Policy and Guidelines")

            }

            Spacer(modifier = Modifier.width(16.dp))

            val readyUser = (userState as? UserState.Ready)?.user
            Button(
                onClick = { readyUser?.let(::toggleNotifications) },
                enabled = readyUser != null && !isSavingNotifications,
                modifier = Modifier.testTag("toOutNotificationButton"),
                shape = RectangleShape
            ) {
                Text(
                    if (readyUser?.notificationsEnabled == false) "Opt in Notifications"
                    else "Opt out Notifications"
                )
            }
        }


    }
}

/** Asks the user to confirm before their profile is permanently deleted. */
@Composable
private fun DeleteProfileDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete profile?") },
        text = { Text("This permanently deletes your name, email and phone number. This can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirmDeleteButton")) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancelDeleteButton")) {
                Text("Cancel")
            }
        },
    )
}

/** Read-only view of a user's contact details. Blank fields show as "Not set". */
@Composable
private fun ProfileDetails(user: User) {
    Text("Name: ${user.name.ifBlank { "Not set" }}")
    Text("Email: ${user.email?.ifBlank { null } ?: "Not set"}")
    Text("Phone Number: ${user.phoneNumber?.ifBlank { null } ?: "Not set"}")
}

/**
 * Form for editing a user's contact details. Validates with [ProfileValidator] and
 * saves through [userRepository]; [onSaved] receives a function that applies the
 * saved change to a [User].
 */
@Composable
private fun EditProfileForm(
    user: User,
    userRepository: UserRepository,
    onSaved: ((User) -> User) -> Unit,
    onCancel: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(user.name) }
    var email by rememberSaveable { mutableStateOf(user.email.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(user.phoneNumber.orEmpty()) }
    var errors by remember { mutableStateOf(ProfileInputErrors()) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    fun save() {
        errors = ProfileValidator.validate(name, email, phone)
        if (!errors.isValid) return

        val newName = ProfileValidator.normalizeName(name)
        val newEmail = ProfileValidator.normalizeEmail(email)
        val newPhone = ProfileValidator.normalizePhone(phone)
        isSaving = true
        saveError = null
        userRepository.updateContactInfo(
            userId = user.userId,
            name = newName,
            email = newEmail,
            phoneNumber = newPhone,
            onSuccess = {
                isSaving = false
                onSaved { it.copy(name = newName, email = newEmail, phoneNumber = newPhone) }
            },
            onFailure = {
                isSaving = false
                saveError = "Couldn't save your profile: ${it.message}"
            },
        )
    }

    ProfileTextField(
        label = "Name",
        value = name,
        onValueChange = { name = it },
        error = errors.name,
        keyboardType = KeyboardType.Text,
        tag = "nameField",
    )
    ProfileTextField(
        label = "Email (optional)",
        value = email,
        onValueChange = { email = it },
        error = errors.email,
        keyboardType = KeyboardType.Email,
        tag = "emailField",
    )
    ProfileTextField(
        label = "Phone Number (optional)",
        value = phone,
        onValueChange = { phone = it },
        error = errors.phoneNumber,
        keyboardType = KeyboardType.Phone,
        tag = "phoneField",
    )

    saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

    Spacer(modifier = Modifier.height(8.dp))
    Row {
        Button(
            onClick = ::save,
            enabled = !isSaving,
            shape = RectangleShape,
            modifier = Modifier.testTag("saveProfileButton"),
        ) {
            Text(if (isSaving) "Saving..." else "Save")
        }
        Spacer(modifier = Modifier.width(16.dp))
        OutlinedButton(
            onClick = onCancel,
            enabled = !isSaving,
            shape = RectangleShape,
            modifier = Modifier.testTag("cancelEditButton"),
        ) {
            Text("Cancel")
        }
    }
}

/** Single-line text field that shows [error] underneath when it is non-null. */
@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    keyboardType: KeyboardType,
    tag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

/** Preview of [ProfileScreen] with sample data, no Firebase required. */
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val dummyNav = rememberNavController()
    val user = User(userId = "preview", name = "Jane Camper", email = "jane@example.com")
    ProfileScreen(
        dummyNav,
        userState = UserState.Ready(user),
        userRepository = remember { InMemoryUserRepository(listOf(user)) },
        onUserChanged = {},
        onUserDeleted = {},
    )
}