package com.example.campsitelottery

import androidx.compose.ui.input.key.Key
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
     * Tests US 02.01.01 & US 02.01.02:
     * Verifies that the RegisterCampsiteScreen correctly displays all required input fields
     * (name, description, address, occupancy, pricing, rules, policies, and tags) and
     * allows a user to input text into them.
     */
    @Test
    fun testRegisterCampsiteFormInputs() {
        // Given the RegisterCampsiteScreen is loaded
        composeTestRule.setContent {
            val navController = rememberNavController()
            RegisterCampsiteScreen(navController,null , "placeholder_id")
        }

        // Then verify all form fields and action buttons exist
        composeTestRule.onNodeWithText("Register New Campsite").assertExists()
        composeTestRule.onNodeWithText("Campsite Name").assertExists()
        composeTestRule.onNodeWithText("Description & Amenities").assertExists()
        composeTestRule.onNodeWithText("Location / Address").assertExists()
        composeTestRule.onNodeWithText("Max Occupancy").assertExists()
        composeTestRule.onNodeWithText("Price Per Night ($)").assertExists()
        composeTestRule.onNodeWithText("Campsite Rules").assertExists()
        composeTestRule.onNodeWithText("Cancellation Policy").assertExists()
        composeTestRule.onNodeWithText("Tags (comma separated)").assertExists()
        composeTestRule.onNodeWithText("Publish Campsite").assertExists()

        // When the user types information into the fields
        composeTestRule.onNodeWithText("Campsite Name").performTextInput("Jasper Pines")
        composeTestRule.onNodeWithText("Max Occupancy").performClick()
        composeTestRule.onNodeWithText("Max Occupancy").performKeyInput { pressKey(Key.Backspace) }
        composeTestRule.onNodeWithText("Max Occupancy").performTextInput("6")
        composeTestRule.onNodeWithText("Price Per Night ($)").performTextInput("75.0")
        composeTestRule.onNodeWithText("Tags (comma separated)").performTextInput("Jasper, Hiking, Scenic")

        // Then verify the inputs were successfully accepted and displayed
        composeTestRule.onNodeWithText("Jasper Pines").assertExists()
        composeTestRule.onNodeWithText("6").assertExists()
    }

    /**
     * Tests US 02.01.04 & US 02.03.01:
     * Verifies that the OwnerDashboardScreen properly displays header elements,
     * the navigation back button, and the register trigger.
     */
    @Test
    fun testOwnerDashboardLayout() {
        // Given the OwnerDashboardScreen is loaded
        composeTestRule.setContent {
            val navController = rememberNavController()
            OwnerDashboardScreen(navController, "placeholderId")
        }

        // Then verify dashboard structural elements exist
        composeTestRule.onNodeWithText("My Campsites").assertExists()
        composeTestRule.onNodeWithText("+ Register New Campsite").assertExists()
        composeTestRule.onNodeWithText("Back").assertExists()
    }
}