package com.example.campsitelottery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun OwnerViewReservationDetailScreen(
    navController: NavController,
    reservationId: String?
) {
    // colors for styling
    val backgroundColor = Color(0xFFFEFAE0)
    val buttonColor = Color(0xFF283618)

    // vars for loading vals and coroutine scope var
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // hold info for display
    var reservation by remember { mutableStateOf<Reservation?>(null) }
    var campsiteName by remember { mutableStateOf("") }
    var entrantName by remember { mutableStateOf("") }


    DisposableEffect(reservationId) {
        val db = FirebaseFirestore.getInstance()

        val listener = db.collection("reservations")
            .document(reservationId!!)
            .addSnapshotListener { snapshot, error ->

                if (error != null || snapshot == null || !snapshot.exists()) {
                    reservation = null
                    isLoading = false
                    return@addSnapshotListener
                }

                val dates = (snapshot.get("reservedDates") as? List<*>)
                    ?.mapNotNull { (it as? Timestamp)?.toDate() }
                    ?: emptyList()


                val res = snapshot.toObject(Reservation::class.java)
                res?.reservedDates = dates



                if (res != null) {
                    scope.launch { // one-time reads, not real time, but reasonable
                        campsiteName = db.collection("campsites").document(res.campsiteId)
                            .get().await().getString("name") ?: ""

                        entrantName = db.collection("users").document(res.entrantId)
                            .get().await().getString("name") ?: ""
                    }
                }

                reservation = res
                isLoading = false
            }

        onDispose {
            listener.remove()
        }
    }

    Column(
        modifier = Modifier
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp)
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

        }

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            reservation == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Reservation not found")
                }
            }

            else -> {


                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Reservation", style = MaterialTheme.typography.headlineMedium)

                    var resStatus = "Loading..."

                    if (reservation!!.status == ReservationStatus.COMPLETED) resStatus = "Past"
                    else if (reservation!!.status == ReservationStatus.PENDING) resStatus =
                        "Pending"
                    else if (reservation!!.status == ReservationStatus.CONFIRMED) resStatus =
                        "Active"
                    else if (reservation!!.status == ReservationStatus.REJECTED_BY_OWNER) resStatus =
                        "Owner Cancelled"
                    else "Entrant Cancelled"

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .background(buttonColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoRow("Status", resStatus)
                            InfoRow("Total price", "$${"%.2f".format(reservation!!.totalPrice)}")
                            InfoRow(
                                "Rules accepted",
                                if (reservation!!.rulesAccepted) "Yes" else "No"
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
                            InfoRow("Campsite", campsiteName.ifBlank { reservation!!.campsiteId })
                            InfoRow("Campsite ID", reservation!!.campsiteId)
                            InfoRow("Entrant", entrantName.ifBlank { reservation!!.entrantId })
                            InfoRow("Entrant ID", reservation!!.entrantId)
                            InfoRow("Owner ID", reservation!!.ownerId)
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .background(buttonColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoRow("Check-in", reservation!!.checkInDate.toString())
                            InfoRow("Check-out", reservation!!.checkOutDate.toString())

                            Text("Reserved dates", style = MaterialTheme.typography.labelLarge, color = backgroundColor)
                            reservation!!.reservedDates.forEach { date ->
                                Text("- ${date.toString()}", color = backgroundColor)
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
                            InfoRow("Created", reservation!!.createdAt.toString())
                            InfoRow("Updated", reservation!!.updatedAt.toString())
                        }
                    }

                    Row() {
                        if (reservation!!.status == ReservationStatus.CONFIRMED || reservation!!.status == ReservationStatus.PENDING) {
                            Button(
                                modifier = Modifier
                                    .fillMaxSize(),
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = buttonColor
                                ),
                                onClick = {
                                    FirebaseFirestore.getInstance()
                                        .collection("reservations")
                                        .document(reservationId!!)
                                        .update(
                                            mapOf(
                                                "status" to ReservationStatus.REJECTED_BY_OWNER,
                                                "updatedAt" to FieldValue.serverTimestamp()
                                            )
                                        )
                                        .addOnSuccessListener { navController.popBackStack() }

                                }
                            ) {
                                Text("Reject Reservation")
                            }
                        }
                    }
                }
            }

        }
    }

}

