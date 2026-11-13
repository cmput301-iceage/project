package com.example.campsitelottery

import android.R.attr.onClick
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


@Composable
fun OwnerViewLotteriesScreen(
    navController: NavController,
    ownerId : String
) {

    val backgroundColor = Color(0xFFFEFAE0)
    val buttonColor = Color(0xFF283618)

    var lotteryList by remember { mutableStateOf(listOf<Waitlist>()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    var campsiteNames: Map<String, String> by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var entrantNames: Map<String, String> by remember { mutableStateOf<Map<String, String>>(emptyMap()) }



    DisposableEffect(ownerId) {
        val db = FirebaseFirestore.getInstance()


        val listener = db.collection("waitlists")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {

                    val lotteries = snapshot.documents.mapNotNull {
                        it.toObject(Waitlist::class.java)
                    }

                    scope.launch { // launch coroutine, gets a snapshot, so not real time update, but reasonable
                        try {
                            campsiteNames = lotteries
                                .flatMap { it.campsiteIds }
                                .distinct()
                                .associateWith { id ->
                                    db.collection("campsites").document(id)
                                        .get().await().getString("name") ?: ""
                                }

                            entrantNames = lotteries
                                .flatMap { it.joinedEntrantIds }
                                .distinct()
                                .associateWith { id ->
                                    db.collection("users").document(id)
                                        .get().await().getString("name") ?: ""
                                }
                        } catch(e: Exception) {
                            Log.e("ViewLotteries", "Failed to lookup names", e)
                            campsiteNames = lotteries
                                .flatMap { it.campsiteIds }
                                .distinct()
                                .associateWith { it }   // fall back to IDs
                            entrantNames = lotteries
                                .flatMap { it.joinedEntrantIds }
                                .distinct()
                                .associateWith { it }   // fall back to IDs
                        }
                    }

                    lotteryList = lotteries
                    isLoading = false
                }
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



        Spacer(modifier = Modifier.height(16.dp))
        Text("  Lotteries", fontSize = 28.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // filter reservations


        // Handle loading, empty, and populated states
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            lotteryList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You don't have any current lotteries.", fontSize = 16.sp)
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(lotteryList) { lottery ->
                        // get info for card
                         val campsiteName = lottery.campsiteIds.joinToString(", ") { id ->
                             campsiteNames[id] ?: id
                         }
                        val entrantName = lottery.joinedEntrantIds ?: "Loading..."
                        val regEndDate = formatDate(lottery.registrationEndDate)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),

                            onClick = { navController.navigate("ownerViewLotteryDetails/${lottery.waitlistId}") },

                            colors = CardDefaults.cardColors(
                                containerColor = buttonColor,
                                contentColor = backgroundColor
                            )

                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(lottery.lotteryName, fontSize = 18.sp)
                                Text(campsiteName)
                                Text(text = "   Draw On: $regEndDate")

                            }
                        }
                    }
                }
            }
        }

    }

}