package com.example.campsitelottery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterCampsiteScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var maxOccupancy by remember { mutableStateOf("1") }
    var pricePerNight by remember { mutableStateOf("0.0") }
    var rules by remember { mutableStateOf("") }
    var cancellationPolicy by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") } // comma separated tags

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Button(onClick = { navController.popBackStack() }, shape = RectangleShape) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Register New Campsite", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Campsite Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description & Amenities") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Location / Address") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = maxOccupancy, onValueChange = { maxOccupancy = it }, label = { Text("Max Occupancy") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = pricePerNight, onValueChange = { pricePerNight = it }, label = { Text("Price Per Night ($)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = rules, onValueChange = { rules = it }, label = { Text("Campsite Rules") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = cancellationPolicy, onValueChange = { cancellationPolicy = it }, label = { Text("Cancellation Policy") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(value = tagsInput, onValueChange = { tagsInput = it }, label = { Text("Tags (comma separated)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Construct the Campsite object using your existing model class
                val newCampsite = Campsite(
                    name = name,
                    description = description,
                    address = address,
                    maxOccupancy = maxOccupancy.toIntOrNull() ?: 1,
                    pricePerNight = pricePerNight.toDoubleOrNull() ?: 0.0,
                    rules = rules,
                    cancellationPolicy = cancellationPolicy,
                    tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    ownerId = "CURRENT_USER_ID" // Replace with actual logged-in user identifier
                )

                // Save to Firestore collection "campsites"
                FirebaseFirestore.getInstance().collection("campsites")
                    .add(newCampsite)
                    .addOnSuccessListener {
                        navController.popBackStack() // Return to dashboard on success
                    }
            },
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Publish Campsite")
        }
    }
}