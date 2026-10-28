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

@Composable
fun ProfileScreen(
    navController : NavController
) {
    // Format Screen Here
    //TODO: connect these to the objects/model storing these values
    var profileName  = ""
    var profileEmail = ""
    var profilePhoneNum = ""

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

            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.popBackStack() },
                shape = RectangleShape,

                ) {
                Text("Delete Profile")
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            //TODO: copy whats on the UI
            //TODO: add edit buttons
            Text("Name: FILLER")
            Text("Email: FILLER")
            Text("Phone Number: FILLER")
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
                modifier = Modifier.testTag("toPolicyButton"),
                shape = RectangleShape
            ) {
                Text("Opt out Notifications")
            }
        }


    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val dummyNav = rememberNavController()
    ProfileScreen(dummyNav)
}