package com.example.campsitelottery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminScreenUITest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>() // access MainActivity

    //TODO: set database starting state

    @Test
    fun navigateToAdminDash_thenToProfiles_andBack()
    {
        // navigate to the admin dash
        composeTestRule
            .onNodeWithTag("adminButton")
            .performClick()

        // verify we are in admin dashboard
        composeTestRule
            .onNodeWithText("Admin")
            .assertExists()

        // navigate to the admin profiles list
        composeTestRule
            .onNodeWithTag("toProfilesButton")
            .performClick()


        // verify we are in the admin profiles list screen
        composeTestRule
            .onNodeWithText("Profiles")
            .assertExists()


        // TODO, test delete user

        // go back to the admin dash
        composeTestRule
            .onNodeWithTag("backButton")
            .performClick()


        // verify we are in admin dash
        composeTestRule
            .onNodeWithText("Admin")
            .assertExists()



        // go back home
        composeTestRule
            .onNodeWithTag("backButton")
            .performClick()


        // verify we are at home screen
        composeTestRule
            .onNodeWithTag("adminButton")
            .assertExists()





    }


}