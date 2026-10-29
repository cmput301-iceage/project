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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController



@Composable
fun HomeScreen(
    navController : NavController
) {

    val dummyCampsite = mutableListOf<String>("Campsite 1", "Campsite 2", "Campsite 3")

    // Format Screen Here
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),

    ) {

        // where campsite search name is stored
        var campsiteName by remember { mutableStateOf("") }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {}, //TODO: Navigate to admin screen
                shape = RectangleShape,

            ) {
                Text("ADMIN")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {}, //TODO: Navigate to owner screen
                shape = RectangleShape,

            ) {
                Text("Owner")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = { navController.navigate("profile") },
                shape = CircleShape,
                modifier = Modifier.size(64.dp).testTag("toProfileButton")
            ) {
                // TODO: Implement proper function to display photo
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center

        ) {

            OutlinedTextField(
                value = campsiteName,
                onValueChange = { campsiteName = it },
                label = { Text( text = "Search for Campsites", textAlign = TextAlign.Center) },
                modifier = Modifier.width(600.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center

        ) {

            Button(
                onClick = {}, //TODO: make button search for campsites
                shape = RectangleShape,
                //modifier = Modifier.size(64.dp)
            ) {
                Text("Search")
            }
            // TODO: add a way to automatically search/display for campsites

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {}, //TODO: make search using filters
                shape = RectangleShape,
                //modifier = Modifier.size(64.dp)
            ) {
                Text("Filter")
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(700.dp)
        ) {


            //TODO: find a way to connect model to display objects

            items(dummyCampsite) { campsite ->
                Text(
                    text = campsite,
                    fontSize = 60.sp,
                    modifier = Modifier.testTag("toCampsitesButton").fillMaxWidth().clickable { //
                        navController.navigate("campsiteInfo")
                        //TODO: find a way to pass campsite info into the screen
                        }
                )
            }

        }


    }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val dummyNav = rememberNavController()
    HomeScreen(dummyNav)
}