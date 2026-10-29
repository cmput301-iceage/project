package com.example.campsitelottery


import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CampsiteLotteryTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>() // access MainActivity

    @Test
    fun navigate_to_reservation_then_back_to_home() {
        // navigate to profile screen
        composeTestRule
            .onNodeWithTag("toProfileButton")
            .performClick()

        // navigate to reservations screen
        composeTestRule
            .onNodeWithTag("toReserveButton")
            .performClick()

        // verify ____ screen
        //composeTestRule
        //    .onNodeWithText("Reservations 1")
        //    .assertExists()

        // back to home
        composeTestRule
            .onNodeWithTag("toBackButton")
            .performClick()

        composeTestRule
            .onNodeWithTag("toBackButton")
            .performClick()

        // verify home screen
        //composeTestRule
        //    .onNodeWithText("ADMIN")
        //    .assertExists()
    }

    @Test
    fun navigate_to_notification_then_back_to_home() {
        // navigate to profile screen
        composeTestRule
            .onNodeWithTag("toProfileButton")
            .performClick()

        // navigate to notification screen
        composeTestRule
            .onNodeWithTag("toNotificationButton")
            .performClick()

        // verify ____ screen
        //composeTestRule
        //    .onNodeWithText("Reservations 1")
        //    .assertExists()

        // back to home
        composeTestRule
            .onNodeWithTag("toBackButton")
            .performClick()

        composeTestRule
            .onNodeWithTag("toBackButton")
            .performClick()

        // verify home screen
        //composeTestRule
        //    .onNodeWithText("ADMIN")
        //
    }

    @Test
    fun navigate_to_booking_then_back_to_home() {
        // navigate to profile screen
        composeTestRule
            .onNodeWithTag("toProfileButton")
            .performClick()

        // navigate to lottery screen
        composeTestRule
            .onNodeWithTag("toLotteryButton")
            .performClick()

        // navigate to booking screen
        composeTestRule
            .onNodeWithTag("toBookingButton_Lottery 1")
            .performClick()
        // verify ____ screen
        //composeTestRule
        //    .onNodeWithText("Reservations 1")
        //    .assertExists()

        // back to home
        composeTestRule
            .onNodeWithTag("toBackButton")
            .performClick()

        composeTestRule
            .onNodeWithTag("toBackButton")
            .performClick()

        composeTestRule
            .onNodeWithTag("toBackButton")
            .performClick()

        // verify home screen
        //composeTestRule
        //    .onNodeWithText("ADMIN")
        //
    }

    @Test
    fun navigate_to_campsites_then_back_to_home() {
        // navigate to campsites screen
        composeTestRule
            .onNodeWithText("Campsite 1")
            .performClick()

        // verify ____ screen
        //composeTestRule
        //    .onNodeWithText("Reservations 1")
        //    .assertExists()

        // back to home
        composeTestRule
            .onNodeWithTag("toBackButton")
            .performClick()

        // verify home screen
        //composeTestRule
        //    .onNodeWithText("ADMIN")
        //
    }

}