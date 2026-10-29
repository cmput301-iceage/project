package com.example.campsitelottery

import androidx.compose.foundation.clickable
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
fun ReservationScreen(
    navController : NavController
) {
    val dummyReservationList = mutableListOf<String>("Reservation 1", "Reservation 2")

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
            modifier = Modifier.fillMaxWidth().height(700.dp)
        ) {


            //TODO: find a way to connect model to display objects

            items(dummyReservationList) { reservation ->
                Text(
                    text = reservation,
                    fontSize = 30.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { }, //TODO: make the button actually cancel reservation
                    shape = RectangleShape,

                    ) {
                    Text("Cancel")
                }

            }

        }



    }
}


@Preview(showBackground = true)
@Composable
fun ReservationScreenPreview() {
    val dummyNav = rememberNavController()
    ReservationScreen(dummyNav)
}