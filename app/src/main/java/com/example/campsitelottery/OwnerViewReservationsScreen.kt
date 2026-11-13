package com.example.campsitelottery

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@Composable
fun OwnerViewReservationsScreen(
    navController: NavController,
    ownerId : String
) {

    val backgroundColor = Color(0xFFFEFAE0)
    val buttonColor = Color(0xFF283618)

    var showActive by remember { mutableStateOf(true) }
    var showPast by remember { mutableStateOf(false) }
    var showCancelled by remember { mutableStateOf(false) }
    var showPending by remember {mutableStateOf(false)}

    var reservationList by remember { mutableStateOf(listOf<Reservation>()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    var campsiteNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var entrantNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }


    DisposableEffect(ownerId) {
        val db = FirebaseFirestore.getInstance()


        val listener = db.collection("reservations")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {

                    val reservations = snapshot.documents.mapNotNull { doc ->

                        val data = doc.data ?: return@mapNotNull null

                        val dates = (data["reservedDates"] as? List<*>)
                            ?.mapNotNull { (it as? Timestamp)?.toDate() }
                            ?: emptyList()

                        data["reservedDates"] = dates

                        val reservation = doc.toObject(Reservation::class.java)
                        reservation?.reservedDates = dates

                        reservation
                    }

                    scope.launch { // launch coroutine, gets a snapshot, so not real time update, but reasonable
                        campsiteNames = reservations.map { it.campsiteId }
                            .distinct()
                            .associateWith { id ->
                                db.collection("campsites").document(id)
                                    .get().await().getString("name") ?: ""
                            }

                        entrantNames = reservations.map { it.entrantId }
                            .distinct()
                            .associateWith { id ->
                                db.collection("users").document(id)
                                    .get().await().getString("name") ?: ""
                            }
                    }

                    reservationList = reservations
                    isLoading = false
                }
            }

        onDispose {
            listener.remove()
        }
    }

    Column(
        modifier = Modifier.background(backgroundColor)
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

        // checkbox filters for state of
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showActive,
                    onCheckedChange = { showActive = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = buttonColor,     // Box fill color when checked
                        uncheckedColor = buttonColor,   // Border color when unchecked
                        checkmarkColor = Color.White          // Color of the checkmark icon
                    )
                )
                Text("Active")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showPast,
                    onCheckedChange = { showPast = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = buttonColor,     // Box fill color when checked
                        uncheckedColor = buttonColor,   // Border color when unchecked
                        checkmarkColor = Color.White          // Color of the checkmark icon
                    )
                )
                Text("Past")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showCancelled,
                    onCheckedChange = { showCancelled = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = buttonColor,     // Box fill color when checked
                        uncheckedColor = buttonColor,   // Border color when unchecked
                        checkmarkColor = Color.White          // Color of the checkmark icon
                    )
                )
                Text("Cancelled")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showPending,
                    onCheckedChange = { showPending = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = buttonColor,     // Box fill color when checked
                        uncheckedColor = buttonColor,   // Border color when unchecked
                        checkmarkColor = Color.White          // Color of the checkmark icon
                    )
                )
                Text("Pending")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Reservations", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // filter reservations
        val filteredReservations = reservationList.filter { res ->

            val status = res.status as? ReservationStatus ?: ""

            // apply checkbox filters
            (showActive && status == ReservationStatus.CONFIRMED) ||
                    (showPast && status == ReservationStatus.COMPLETED) ||
                    (showCancelled && (status == ReservationStatus.CANCELLED_BY_ENTRANT || status == ReservationStatus.REJECTED_BY_OWNER)) ||
                    (showPending && status == ReservationStatus.PENDING)

        }

        // Handle loading, empty, and populated states
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            reservationList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You don't have any matching reservations.", fontSize = 16.sp)
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredReservations) { res ->
                        // get info for card
                        val campsiteName = campsiteNames[res.campsiteId] ?: "Loading..."
                        val entrantName = entrantNames[res.entrantId] ?: "Loading..."
                        var resStatus = "Loading..."

                        if (res.status == ReservationStatus.COMPLETED) resStatus = "Past"
                        else if (res.status == ReservationStatus.PENDING) resStatus = "Pending"
                        else if (res.status == ReservationStatus.CONFIRMED) resStatus ="Active"
                        else if (res.status == ReservationStatus.REJECTED_BY_OWNER) resStatus = "Owner Cancelled"
                        else "Entrant Cancelled"



                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    navController.navigate("ownerViewReservationDetails/${res.reservationId}")
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = buttonColor,
                                contentColor = backgroundColor
                            )

                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .background(buttonColor)
                                    .fillMaxSize()
                            ) {
                                Text(campsiteName, fontSize = 12.sp )
                                Text(entrantName)
                                Text(text = "Status: $resStatus")
                            }
                        }
                    }
                }
            }
        }

    }

}