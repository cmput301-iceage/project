package com.example.campsitelottery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun OwnerDashboardScreen(navController: NavController) {
    // TODO: Replace with real data fetched from Firestore filtered by current owner's ID
    var myCampsites by remember { mutableStateOf(listOf<Campsite>()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { navController.popBackStack() }, shape = RectangleShape) {
                Text("Back")
            }
            Button(onClick = { navController.navigate("registerCampsite") }, shape = RectangleShape) {
                Text("+ Register New Campsite")
            }
        }

        Spacer(modifier = System(16.dp) ?: Modifier.height(16.dp))
        Text("My Campsites", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(myCampsites) { campsite ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            // Navigate to view/edit details for this campsite
                            navController.navigate("campsiteDetail/${campsite.campsiteId}")
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = campsite.name, fontSize = 20.sp)
                        Text(text = "Location: ${campsite.address}")
                        Text(text = "Max Occupancy: ${campsite.maxOccupancy} | Price: $${campsite.pricePerNight}/night")
                    }
                }
            }
        }
    }
}