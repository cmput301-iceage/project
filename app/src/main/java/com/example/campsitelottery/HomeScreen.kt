package com.example.campsitelottery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Home screen: lists every campsite and lets the user search them (US 1.1).
 * Tapping a campsite opens its details.
 *
 * @param navController used to open the profile, dashboards and campsite details.
 * @param userState the current user; decides whether the Admin and Owner buttons show.
 * @param userRepository where user profiles are stored.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    userState: UserState,
    userRepository: UserRepository
) {
    // State to hold all campsites fetched from Firestore
    var campsitesList by remember { mutableStateOf(listOf<Campsite>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch all campsites in real-time from Firestore
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("campsites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val loadedCampsites = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Campsite::class.java)
                    }
                    campsitesList = loadedCampsites
                    isLoading = false
                }
            }
    }

    // Search text; results update as the user types
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val shownCampsites = remember(campsitesList, searchQuery) {
        CampsiteSearch.apply(campsitesList, CampsiteFilter(query = searchQuery))
    }
    val focusManager = LocalFocusManager.current

    // Format Screen Here
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // hide button if user is not an `admin`
            if (userState is UserState.Ready && userState.user.isAdmin) {
                Button(
                    onClick = { navController.navigate("adminDashboard") },
                    shape = RectangleShape,
                    modifier = Modifier.testTag("adminButton")
                ) {
                    Text("ADMIN")
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // hide button if user is not an `owner`
            if (userState is UserState.Ready && (userState.user.isOwner || userState.user.isAdmin)) {
                Button(
                    onClick = { navController.navigate("ownerDashboard") },
                    shape = RectangleShape,
                    modifier = Modifier.testTag("ownerButton")
                ) {
                    Text("Owner")
                }
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
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(text = "Search for Campsites", textAlign = TextAlign.Center) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth().testTag("searchField")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Results already update while typing; this just closes the keyboard
            Button(
                onClick = { focusManager.clearFocus() },
                shape = RectangleShape,
                modifier = Modifier.testTag("searchButton"),
            ) {
                Text("Search")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {}, //TODO: make search using filters
                shape = RectangleShape,
            ) {
                Text("Filter")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Replaced the dummy list view with real Firestore cards styled like OwnerDashboardScreen
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            campsitesList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                    Text("No campsites available.", fontSize = 16.sp)
                }
            }
            shownCampsites.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                    Text("No campsites match \"${searchQuery.trim()}\".", fontSize = 16.sp)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(700.dp)
                ) {
                    items(shownCampsites, key = { it.campsiteId }) { campsite ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("toCampsitesButton")
                                .clickable {
                                    // Pass the actual clicked campsite ID to the info screen
                                    navController.navigate("campsiteInfo/${campsite.campsiteId}")
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
    }
}

/** Preview of [HomeScreen]. Campsites only load on a device, so the list shows its loading state. */
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val dummyNav = rememberNavController()
    val user = User(userId = "preview", name = "Jane Camper", email = "jane@example.com")
    HomeScreen(
        dummyNav,
        userState = UserState.Ready(user),
        userRepository = remember { InMemoryUserRepository(listOf(user)) },
    )
}