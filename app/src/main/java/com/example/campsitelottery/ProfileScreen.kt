package com.example.campsitelottery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

/**
 * Shows the current device user's profile, plus links to their reservations,
 * notifications and lotteries.
 *
 * @param navController used to go back and to open the linked screens.
 * @param userState the current user's profile, as loaded by [CampsiteLotteryApp].
 * @param onRetry called when the user asks to retry after the profile failed to load.
 */
@Composable
fun ProfileScreen(
    navController : NavController,
    userState: UserState,
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),

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
                onClick = { }, //TODO: make it actually delete the profile
                shape = RectangleShape,

                ) {
                Text("Delete Profile")
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth().testTag("profileInfo")
        ) {
            when (userState) {
                is UserState.Loading -> Text("Loading profile...")
                is UserState.Failed -> {
                    Text(
                        "Couldn't load your profile: ${userState.message}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onRetry, shape = RectangleShape) { Text("Retry") }
                }
                is UserState.Ready -> ProfileDetails(userState.user)
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

        Spacer(modifier = Modifier.weight(1f))

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

            Button(
                onClick = { }, //TODO: make it actually opt out of notifications
                modifier = Modifier.testTag("toOutNotificationButton"),
                shape = RectangleShape
            ) {
                Text("Opt out Notifications")
            }
        }


    }
}

/** Read-only view of a user's contact details. Blank fields show as "Not set". */
@Composable
private fun ProfileDetails(user: User) {
    Text("Name: ${user.name.ifBlank { "Not set" }}")
    Text("Email: ${user.email?.ifBlank { null } ?: "Not set"}")
    Text("Phone Number: ${user.phoneNumber?.ifBlank { null } ?: "Not set"}")
}

/** Preview of [ProfileScreen] with sample data, no Firebase required. */
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val dummyNav = rememberNavController()
    ProfileScreen(
        dummyNav,
        userState = UserState.Ready(
            User(userId = "preview", name = "Jane Camper", email = "jane@example.com")
        ),
    )
}