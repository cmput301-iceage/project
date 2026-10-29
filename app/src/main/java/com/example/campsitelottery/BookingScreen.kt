package com.example.campsitelottery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun BookingScreen(
    navController : NavController
) {
    // Format Screen Here
    val dummyAvailableDatesList = mutableListOf<String>("Available Date 1", "Available Date 2")

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

        Spacer(modifier = Modifier.height(25.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(500.dp)
        ) {


            //TODO: find a way to connect model to display objects

            items(dummyAvailableDatesList) { availableDate ->
                Text(
                    text = availableDate,
                    fontSize = 30.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { }, //TODO: make the button actually reserve the date
                    shape = RectangleShape,

                    ) {
                    Text("Reserve")
                }

            }

        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { }, //TODO: make the button work
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth()

            ) {
            Text("Decline Reservation")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BookingScreenPreview() {
    val dummyNav = rememberNavController()
    BookingScreen(dummyNav)
}