package com.example.campsitelottery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CampsiteInfoScreen(
    navController: NavController,
    campsiteId: String // Receives campsite ID from navigation route
) {
    var campsite by remember { mutableStateOf<Campsite?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch the specific campsite document from Firestore using the campsiteId
    LaunchedEffect(campsiteId) {
        if (campsiteId.isNotBlank()) {
            FirebaseFirestore.getInstance()
                .collection("campsites")
                .document(campsiteId)
                .get()
                .addOnSuccessListener { document ->
                    campsite = document.toObject(Campsite::class.java)
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

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

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            campsite == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Campsite details not found.", fontSize = 20.sp)
                }
            }
            else -> {
                // Display actual campsite data fetched from Firestore
                Text(text = campsite!!.name, fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Location: ${campsite!!.address}", fontSize = 18.sp)
                Text(text = "Max Occupancy: ${campsite!!.maxOccupancy} | Price: $${campsite!!.pricePerNight}/night", fontSize = 16.sp)

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = campsite!!.description, fontSize = 18.sp)

                if (campsite!!.rules.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Rules: ${campsite!!.rules}", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { }, // TODO: make the button actually rate the campsite
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rate Campsite")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { }, // TODO: make the button join lottery/waitlist
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
    CampsiteInfoScreen(dummyNav, campsiteId = "preview_id")
}