package com.example.campsitelottery

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun OwnerViewLotteryDetails(
    navController: NavController,
    waitlistId: String?
) {

    // colors for styling
    val backgroundColor = Color(0xFFFEFAE0)
    val buttonColor = Color(0xFF283618)

    // vars for loading vals and coroutine scope var
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // hold info for display
    var waitlist by remember { mutableStateOf<Waitlist?>(null) }
    var campsiteNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var entrantNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }



    DisposableEffect(waitlistId) {
        val db = FirebaseFirestore.getInstance()

        val listener = db.collection("waitlists")
            .document(waitlistId!!)
            .addSnapshotListener { snapshot, error ->

                if (error != null || snapshot == null || !snapshot.exists()) {
                    waitlist = null
                    isLoading = false
                    return@addSnapshotListener
                }

                val wlist = snapshot.toObject(Waitlist::class.java)

                if (wlist != null) {
                    scope.launch { // one-time reads
                        try {
                            campsiteNames = wlist.campsiteIds
                                .filter { it.isNotBlank() }
                                .distinct()
                                .associateWith { id ->
                                    db.collection("campsites").document(id)
                                        .get().await().getString("name") ?: id
                                }

                            entrantNames = wlist.joinedEntrantIds
                                .filter { it.isNotBlank() }
                                .distinct()
                                .associateWith { id ->
                                    db.collection("users").document(id)
                                        .get().await().getString("name") ?: id
                                }
                        } catch (e: Exception) {
                            Log.e("Waitlist", "Name lookup failed", e)
                            campsiteNames = wlist.campsiteIds
                                .filter { it.isNotBlank() }
                                .distinct()
                                .associateWith { it }   // fall back to IDs
                            entrantNames = wlist.joinedEntrantIds
                                .filter { it.isNotBlank() }
                                .distinct()
                                .associateWith { it }   // fall back to IDs

                        }
                    }
                }


                waitlist = wlist
                isLoading = false
            }

        onDispose {
            listener.remove()
        }
    }

    Column(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { navController.popBackStack() },
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                )
            ) {
                Text("< Back")
            }

            Button(
                onClick = { navController.navigate("editLottery/${waitlistId}") },
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor
                )
            ) {
                Text("Edit")
            }
        }

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            waitlist == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Waitlist not found")
                }
            }

            else -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(waitlist!!.lotteryName, style = MaterialTheme.typography.headlineMedium)




                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .background(buttonColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoRow("Lottery Name", waitlist!!.lotteryName)
                            InfoRow("OwnerID", waitlist!!.ownerId)
                            InfoRow("WaitlistID", waitlistId)

                            Row() {
                                Text("Registered Campsites", color = backgroundColor)
                            }
                            for(site in waitlist!!.campsiteIds) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = backgroundColor,
                                        contentColor = buttonColor
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .background(backgroundColor)
                                    ) {
                                        Text(
                                            campsiteNames[site] ?: site,
                                            fontSize = 20.sp,
                                            color = buttonColor
                                        )
                                    }
                                }
                            }

                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .background(buttonColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoRow(
                                "Number of Winners",
                                waitlist!!.numberOfWinnersToDraw.toString()
                            )
                            InfoRow(
                                "Waitlist Capacity",
                                waitlist!!.maxWaitlistCapacity.toString()
                            )
                            InfoRow(
                                "Has Drawn",
                                if (waitlist!!.isDrawn) "Drawn" else "Not Drawn"
                            )
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .background(buttonColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoRow(
                                "Registration Start Date",
                                dateLongUIFormat(waitlist!!.registrationStartDate)
                            )
                            InfoRow(
                                "Registration End Date",
                                dateLongUIFormat(waitlist!!.registrationEndDate)
                            )
                            InfoRow(
                                "Booking Start Date",
                                dateLongUIFormat(waitlist!!.bookingStartDate)
                            )
                            InfoRow(
                                "Booking End Date",
                                dateLongUIFormat(waitlist!!.bookingEndDate)
                            )

                        }
                    }





                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = buttonColor,
                            contentColor = backgroundColor
                        )
                    ) {
                        Column(
                            Modifier
                                .background(buttonColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Row() {
                                Text("Entrants", color = backgroundColor)
                            }
                            if (waitlist!!.joinedEntrantIds.isNotEmpty()) {
                                for (entrant in waitlist!!.joinedEntrantIds) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = backgroundColor,
                                            contentColor = buttonColor
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .background(backgroundColor)
                                        ) {
                                            Text(
                                                entrantNames[entrant] ?: entrant,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                            else {
                                Text("No Entrants Yet")
                            }

                        }
                    }


                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = buttonColor,
                            contentColor = backgroundColor
                        )
                    ) {
                        Column(
                            Modifier
                                .background(buttonColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Row() {
                                Text("Winners", color = backgroundColor)
                            }

                            if (waitlist!!.selectedWinnerIds.isNotEmpty()) {
                                for (entrant in waitlist!!.selectedWinnerIds) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = backgroundColor,
                                            contentColor = buttonColor
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .background(backgroundColor)
                                        ) {
                                            Text(
                                                entrantNames[entrant] ?: entrant,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                            else {
                                Text("No Winners Yet")
                            }


                        }
                    }


                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = buttonColor,
                            contentColor = backgroundColor
                        )
                    ) {
                        Column(
                            Modifier
                                .background(buttonColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row() {
                                Text("Declined Winners", color = backgroundColor)
                            }

                            if (waitlist!!.declinedEntrantIds.isNotEmpty()) {
                                for (entrant in waitlist!!.declinedEntrantIds) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = backgroundColor,
                                            contentColor = buttonColor
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .background(backgroundColor)
                                        ) {
                                            Text(
                                                entrantNames[entrant] ?: entrant,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                            else {
                                Text("No Declined Winners")
                            }
                        }
                    }
                }
            }
        }
    }
}