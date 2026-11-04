package com.example.campsitelottery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
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
    campsiteId: String
) {
    val context = LocalContext.current
    val currentDeviceId = remember { DeviceIdentity.getDeviceId(context) }

    var campsite by remember { mutableStateOf<Campsite?>(null) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch campsite details and current user role from Firestore
    LaunchedEffect(campsiteId) {
        val db = FirebaseFirestore.getInstance()

        // 1. Fetch Campsite
        db.collection("campsites").document(campsiteId).get()
            .addOnSuccessListener { doc ->
                campsite = doc.toObject(Campsite::class.java)

                // 2. Fetch Current User Profile to check Admin status
                db.collection("users").document(currentDeviceId).get()
                    .addOnSuccessListener { userDoc ->
                        currentUser = userDoc.toObject(User::class.java)
                        isLoading = false
                    }
                    .addOnFailureListener {
                        isLoading = false
                    }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    // Permission checks
    val isOwnerOfSite = campsite != null && campsite!!.ownerId == currentDeviceId
    val isAdmin = currentUser?.isAdmin == true
    val canEdit = isOwnerOfSite                  // Only the owner can edit details
    val canDelete = isOwnerOfSite || isAdmin     // Owner of the site or Admin can delete

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
            // CONDITIONAL: Edit Button (Only visible if the user is the owner)
            if (canEdit) {
                Button(
                    onClick = {
                        navController.navigate("registerCampsite/$campsiteId")
                    },
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Campsite Details")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // CONDITIONAL: Delete Button (Visible if user is the campsite owner OR an admin)
            if (canDelete) {
                Button(
                    onClick = {
                        FirebaseFirestore.getInstance()
                            .collection("campsites")
                            .document(campsiteId)
                            .delete()
                            .addOnSuccessListener {
                                navController.popBackStack() // Return back after successful deletion
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth().testTag("deleteCampsiteButton")
                ) {
                    Text("Delete Campsite", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { }, // TODO: Rate campsite action
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rate Campsite")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { }, // TODO: Join lottery/waitlist action
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