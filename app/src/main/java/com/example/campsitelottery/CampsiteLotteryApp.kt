package com.example.campsitelottery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Root composable: identifies the device's user and hosts navigation between screens.
 *
 * On first composition the device's profile is loaded, or created if this is the
 * device's first launch (US 9.1). The resulting [UserState] is held here so every
 * screen sees the same current user.
 *
 * @param userRepository where profiles are stored; Firestore by default.
 * @param deviceId this device's ID; read from [DeviceIdentity] by default.
 */
@Composable
fun CampsiteLotteryApp(
    userRepository: UserRepository = remember { FirestoreUserRepository() },
    deviceId: String = LocalContext.current.let { context ->
        remember(context) { DeviceIdentity.getDeviceId(context) }
    },
) {
    val navController = rememberNavController()
    var userState by remember { mutableStateOf<UserState>(UserState.Loading) }

    val loadUser = {
        userState = UserState.Loading
        userRepository.getOrCreateUser(
            deviceId,
            onSuccess = { userState = UserState.Ready(it) },
            onFailure = { userState = UserState.Failed(it.message ?: "Could not load profile") },
        )
    }
    LaunchedEffect(deviceId) { loadUser() }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                navController,
                userState = userState,
                userRepository = userRepository
            )
        }
        composable("profile") {
            ProfileScreen(
                navController = navController,
                userState = userState,
                userRepository = userRepository,
                onUserChanged = { change ->
                    // Apply to the latest user, not the one the screen captured, so
                    // overlapping saves (e.g. edit + notification toggle) don't undo each other.
                    (userState as? UserState.Ready)?.let { userState = UserState.Ready(change(it.user)) }
                },
                onUserDeleted = {
                    userState = UserState.Deleted
                    // The delete can complete late (e.g. after reconnecting); only leave
                    // the profile screen if the user is still on it.
                    if (navController.currentDestination?.route == "profile") {
                        navController.popBackStack("home", inclusive = false)
                    }
                },
                onRetry = loadUser,
            )
        }
        composable("lottery") { LotteryScreen(navController) }
        composable("booking") { BookingScreen(navController) }
        composable("notification") { NotificationScreen(navController) }
        composable("reservation") { ReservationScreen(navController) }
        composable("campsiteInfo") { CampsiteInfoScreen(navController) }
        composable("adminDashboard") {
            AdminDashboardScreen(
                navController,
                userState = userState,
                userRepository = userRepository
            )}
        composable("adminNotifications") {AdminNotificationLogScreen(navController)}
        composable("adminProfiles") {
            AdminProfilesScreen(
                navController,
                userState = userState,
                userRepository = userRepository
            )}
        composable("adminCampsites") {AdminCampsitesScreen(navController)}
        composable("adminImages") {AdminImagesScreen(navController)}
        composable("adminMessages") {AdminMessagesScreen(navController)}
    }
}


