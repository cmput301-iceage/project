package com.example.campsitelottery

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun CampsiteLotteryApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("lottery") { LotteryScreen(navController) }
        composable("booking") { BookingScreen(navController) }
        composable("notification") { NotificationScreen(navController) }
        composable("reservation") { ReservationScreen(navController) }
        composable("campsiteInfo") { CampsiteInfoScreen(navController) }
    }
}