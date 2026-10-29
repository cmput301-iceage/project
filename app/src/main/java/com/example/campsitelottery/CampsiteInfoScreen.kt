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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun CampsiteInfoScreen(
    navController : NavController
    //TODO: find a way to pass the clicked campsite into this screen
) {

    val dummyCampsiteChosen = "Camp 1"
    val dummyCampsiteInfo = "Lorem ipsum dolor sit amet, " +
                            "consectetur adipiscing elit, sed do eiusmod tempor incididunt " +
                            "ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
                            "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo " +
                            "consequat. Duis aute irure dolor in reprehenderit in voluptate velit " +
                            "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat" +
                            " cupidatat non proident, sunt in culpa qui officia deserunt mollit " +
                            "anim id est laborum."


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),

        ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Absolute.Left
        ) {
            Button(
                onClick = { navController.popBackStack() },
                shape = RectangleShape,
                modifier = Modifier.testTag("toBackButton")
                ) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = dummyCampsiteChosen, fontSize = 60.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = dummyCampsiteInfo, fontSize = 20.sp)

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),

            ) {

            Button(
                onClick = { }, //TODO: make the button actually rate the campsite
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
                ) {
                Text("Rate Campsite")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { }, //TODO: make the button join lottery/waitlist
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
                ) {
                Text("Join Lottery/Waitlist")
            }

        }



    }
}

@Preview(showBackground = true)
@Composable
fun CampsiteInfoScreenPreview() {
    val dummyNav = rememberNavController()
    CampsiteInfoScreen(dummyNav)
}