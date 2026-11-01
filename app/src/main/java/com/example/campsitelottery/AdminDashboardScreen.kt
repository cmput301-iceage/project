package com.example.campsitelottery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    userState : UserState,
    userRepository: UserRepository
) {

    val backgroundColor = Color(0xFFFEFAE0)
    val buttonColor = Color(0xFF283618)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        ) {

        // Row for back button, header and notif log
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { navController.popBackStack() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                modifier = Modifier
                    .testTag("backButton")

            ) {
                Text("< Back")
            }

            Text(
                text = "Admin",
                fontSize = 35.sp,
                modifier = Modifier.testTag("adminHeader")
            )

            Button(
                onClick = { navController.navigate("adminNotifications") },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                modifier = Modifier.testTag("toNotificationsButton")

            ) {
                // unicode for bell icon
                Text("\uD83D\uDD14")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { navController.navigate("adminProfiles") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .weight(1f)
                    .testTag("toProfilesButton")

            ) {
                Text(
                    text = "Profiles",
                    fontSize = 22.sp,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = { navController.navigate("adminCampsites") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .weight(1f)
                    .testTag("toCampsitesButton")

            ) {
                Text(
                    text = "Campsites",
                    fontSize = 22.sp,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = { navController.navigate("adminImages") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .weight(1f)
                    .testTag("toImagesButton")

            ) {
                Text(
                    text = "Images",
                    fontSize = 22.sp,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = { navController.navigate("adminMessages") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                ),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .weight(1f)
                    .testTag("toMessagesButton")

            ) {
                Text(
                    text = "Messages",
                    fontSize = 22.sp,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }
        }

    }

}








/** Preview of [AdminDashboardScreen] with sample data, no Firebase required. */
@Preview(showBackground = true)
@Composable
fun AdminDashboardScreenPreview() {
    val dummyNav = rememberNavController()
    val user = User(userId = "preview", name = "Jane Camper", email = "jane@example.com")
    AdminDashboardScreen(
        dummyNav,
        userState = UserState.Ready(user),
        userRepository = remember { InMemoryUserRepository(listOf(user)) }
    )
}