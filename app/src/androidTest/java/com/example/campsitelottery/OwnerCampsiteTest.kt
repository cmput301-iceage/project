package com.example.campsitelottery

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnerCampsiteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Tests Register campsite and US Provide campsite details:
     * Verifies that a user can navigate from the Home screen to the Owner Dashboard,
     * open the registration form, input campsite details, and view publishing controls.
     */
    @Test
    fun testRegisterCampsiteWorkflow() {
        // Given the full app starting at the home screen
        composeTestRule.setContent {
            CampsiteLotteryApp()
        }

        // When the user clicks the "Owner" button on HomeScreen
        composeTestRule.onNodeWithText("Owner").performClick()

        // Then verify we are on the Owner Dashboard and click to register a new campsite
        composeTestRule.onNodeWithText("My Campsites").assertExists()
        composeTestRule.onNodeWithText("+ Register New Campsite").performClick()

        // Then verify all input fields specified in US 02.01.02 are present
        composeTestRule.onNodeWithText("Campsite Name").assertExists()
        composeTestRule.onNodeWithText("Description & Amenities").assertExists()
        composeTestRule.onNodeWithText("Location / Address").assertExists()
        composeTestRule.onNodeWithText("Max Occupancy").assertExists()
        composeTestRule.onNodeWithText("Price Per Night ($)").assertExists()
        composeTestRule.onNodeWithText("Campsite Rules").assertExists()
        composeTestRule.onNodeWithText("Cancellation Policy").assertExists()
        composeTestRule.onNodeWithText("Tags (comma separated)").assertExists()

        // When the user inputs campsite details into the form
        composeTestRule.onNodeWithText("Campsite Name").performTextInput("Jasper Pine Grove")
        composeTestRule.onNodeWithText("Description & Amenities").performTextInput("Cookhouse and fire pit included")
        composeTestRule.onNodeWithText("Location / Address").assertExists() // or standard text entry
        composeTestRule.onNodeWithText("Max Occupancy").performTextInput("6")
        composeTestRule.onNodeWithText("Price Per Night ($)").performTextInput("75.0")
        composeTestRule.onNodeWithText("Tags (comma separated)").performTextInput("Jasper, Hiking, Scenic")

        // Then verify the publish action button is ready
        composeTestRule.onNodeWithText("Publish Campsite").assertExists()
        composeTestRule.onNodeWithText("Publish Campsite").assertIsEnabled()
    }

    /**
     * Tests 2.01.04 / US 02.03.01 View own campsite details and list:
     * Verifies that the Owner Dashboard properly displays the list view layout for management.
     */
    @Test
    fun testOwnerDashboardDisplaysCampsiteList() {
        // Given the OwnerDashboardScreen is loaded directly with a test NavController
        composeTestRule.setContent {
            val navController = rememberNavController()
            OwnerDashboardScreen(navController)
        }

        // Then verify the dashboard structural elements for viewing managed listings exist
        composeTestRule.onNodeWithText("My Campsites").assertExists()
        composeTestRule.onNodeWithText("+ Register New Campsite").assertExists()
        composeTestRule.onNodeWithText("Back").assertExists()
    }
}