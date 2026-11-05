/**
 * Test file developed by Gemini: Can you make a test file that tests this feature, by making a campsite, creating a lottery for it, a couple users, adding the users to the lottery, having the user drawn at random, then deleting the test datat you created. Across like 10 or so test funcitons required to test this feature
 *
 * File Name: LotteryLifecycleIntegrationTest.kt
 * Purpose: Provides an end-to-end integration test suite verifying the complete campsite and lottery lifecycle,
 * including creation, entrant waitlist participation, automated/random draws, updates, and database cleanup.
 * Architectural Role: Instrumented test suite interacting with Firebase Firestore.
 * Outstanding Issues: Requires active Firestore connection or emulator during test execution.
 */
package com.example.campsitelottery

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class LotteryLifecycleIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val db = FirebaseFirestore.getInstance()
    private val testCampsiteId = "test_campsite_${System.currentTimeMillis()}"
    private val testWaitlistId = "test_waitlist_${System.currentTimeMillis()}"
    private val testOwnerId = "test_owner_device_123"
    private val testEntrant1 = "test_entrant_abc"
    private val testEntrant2 = "test_entrant_xyz"

    @Before
    fun setupTestFixtures() {
        // Clean setup before tests if needed
    }

    @After
    fun cleanupTestData() {
        // Test 10: Clean up and delete all test data created during test execution
        try {
            Tasks.await(db.collection("campsites").document(testCampsiteId).delete())
            Tasks.await(db.collection("waitlists").document(testWaitlistId).delete())
        } catch (e: Exception) {
            // Ignore if already deleted
        }
    }

    /**
     * Test 1: Verifies that a campsite owner can successfully create and persist a test campsite document.
     */
    @Test
    fun test1_CreateAndVerifyCampsite() {
        val campsite = Campsite(
            campsiteId = testCampsiteId,
            ownerId = testOwnerId,
            name = "Integration Test Camp",
            address = "123 Wilderness Way",
            maxOccupancy = 4,
            pricePerNight = 50.0
        )

        val task = db.collection("campsites").document(testCampsiteId).set(campsite)
        Tasks.await(task)
        assertTrue(task.isSuccessful)

        val fetchTask = db.collection("campsites").document(testCampsiteId).get()
        val doc = Tasks.await(fetchTask)
        assertTrue(doc.exists())
        assertEquals("Integration Test Camp", doc.getString("name"))
    }

    /**
     * Test 2: Verifies that an owner can create a lottery waitlist linked to the test campsite.
     */
    @Test
    fun test2_CreateLotteryForCampsite() {
        val waitlist = Waitlist(
            waitlistId = testWaitlistId,
            ownerId = testOwnerId,
            lotteryName = "Integration Test Lottery",
            campsiteIds = listOf(testCampsiteId),
            numberOfWinnersToDraw = 1,
            isDrawn = false
        )

        val task = db.collection("waitlists").document(testWaitlistId).set(waitlist)
        Tasks.await(task)
        assertTrue(task.isSuccessful)

        val doc = Tasks.await(db.collection("waitlists").document(testWaitlistId).get())
        assertTrue(doc.exists())
        assertEquals("Integration Test Lottery", doc.getString("lotteryName"))
        assertEquals(false, doc.getBoolean("drawn"))
    }

    /**
     * Test 3: Verifies that multiple users can successfully join the active lottery waitlist.
     */
    @Test
    fun test3_AddUsersToLotteryWaitlist() {
        // Initialize waitlist first
        val waitlist = Waitlist(
            waitlistId = testWaitlistId,
            ownerId = testOwnerId,
            lotteryName = "Integration Test Lottery",
            campsiteIds = listOf(testCampsiteId),
            numberOfWinnersToDraw = 1,
            joinedEntrantIds = mutableListOf(testEntrant1),
            isDrawn = false
        )
        Tasks.await(db.collection("waitlists").document(testWaitlistId).set(waitlist))

        // Entrant 2 joins
        val updateTask = db.collection("waitlists").document(testWaitlistId)
            .update("joinedEntrantIds", listOf(testEntrant1, testEntrant2))
        Tasks.await(updateTask)

        val doc = Tasks.await(db.collection("waitlists").document(testWaitlistId).get())
        @Suppress("UNCHECKED_CAST")
        val entrants = doc.get("joinedEntrantIds") as List<String>
        assertEquals(2, entrants.size)
        assertTrue(entrants.contains(testEntrant1))
        assertTrue(entrants.contains(testEntrant2))
    }

    /**
     * Test 4: Verifies that the owner can query and retrieve the correct list of joined entrants.
     */
    @Test
    fun test4_OwnerCanViewEntrantsList() {
        val waitlist = Waitlist(
            waitlistId = testWaitlistId,
            ownerId = testOwnerId,
            campsiteIds = listOf(testCampsiteId),
            joinedEntrantIds = mutableListOf(testEntrant1, testEntrant2)
        )
        Tasks.await(db.collection("waitlists").document(testWaitlistId).set(waitlist))

        val doc = Tasks.await(db.collection("waitlists").document(testWaitlistId).get())
        val fetchedWaitlist = doc.toObject(Waitlist::class.java)

        assertNotNull(fetchedWaitlist)
        assertEquals(2, fetchedWaitlist?.joinedEntrantIds?.size)
    }

    /**
     * Test 5: Verifies that an entrant can remove themselves from the waitlist prior to the draw.
     */
    @Test
    fun test5_EntrantCanLeaveWaitlistBeforeDraw() {
        val waitlist = Waitlist(
            waitlistId = testWaitlistId,
            ownerId = testOwnerId,
            campsiteIds = listOf(testCampsiteId),
            joinedEntrantIds = mutableListOf(testEntrant1, testEntrant2),
            isDrawn = false
        )
        Tasks.await(db.collection("waitlists").document(testWaitlistId).set(waitlist))

        // Entrant 1 leaves
        val updatedEntrants = listOf(testEntrant2)
        Tasks.await(db.collection("waitlists").document(testWaitlistId).update("joinedEntrantIds", updatedEntrants))

        val doc = Tasks.await(db.collection("waitlists").document(testWaitlistId).get())
        @Suppress("UNCHECKED_CAST")
        val currentEntrants = doc.get("joinedEntrantIds") as List<String>
        assertEquals(1, currentEntrants.size)
        assertFalse(currentEntrants.contains(testEntrant1))
    }

    /**
     * Test 6: Verifies that the owner can update lottery configuration settings (e.g., winner count or name).
     */
    @Test
    fun test6_OwnerCanUpdateLotteryListing() {
        val waitlist = Waitlist(
            waitlistId = testWaitlistId,
            ownerId = testOwnerId,
            lotteryName = "Original Name",
            numberOfWinnersToDraw = 1
        )
        Tasks.await(db.collection("waitlists").document(testWaitlistId).set(waitlist))

        // Update lottery parameters
        Tasks.await(
            db.collection("waitlists").document(testWaitlistId)
                .update(mapOf("lotteryName" to "Updated Lottery Title", "numberOfWinnersToDraw" to 2))
        )

        val doc = Tasks.await(db.collection("waitlists").document(testWaitlistId).get())
        assertEquals("Updated Lottery Title", doc.getString("lotteryName"))
        assertEquals(2L, doc.getLong("numberOfWinnersToDraw"))
    }

    /**
     * Test 7: Verifies that executing the random draw selects winners and marks the lottery as drawn.
     */
    @Test
    fun test7_ExecuteRandomWinnerDraw() {
        val waitlist = Waitlist(
            waitlistId = testWaitlistId,
            ownerId = testOwnerId,
            campsiteIds = listOf(testCampsiteId),
            joinedEntrantIds = mutableListOf(testEntrant1, testEntrant2),
            numberOfWinnersToDraw = 1,
            isDrawn = false
        )
        Tasks.await(db.collection("waitlists").document(testWaitlistId).set(waitlist))

        // Simulate draw logic
        val entrants = waitlist.joinedEntrantIds
        val winners = entrants.shuffled().take(waitlist.numberOfWinnersToDraw)

        Tasks.await(
            db.collection("waitlists").document(testWaitlistId)
                .update(mapOf("selectedWinnerIds" to winners, "isDrawn" to true))
        )

        val doc = Tasks.await(db.collection("waitlists").document(testWaitlistId).get())
        assertEquals(true, doc.getBoolean("isDrawn"))
        @Suppress("UNCHECKED_CAST")
        val selectedWinners = doc.get("selectedWinnerIds") as List<String>
        assertEquals(1, selectedWinners.size)
        assertTrue(entrants.contains(selectedWinners[0]))
    }

    /**
     * Test 8: Verifies that a drawn winner can correctly recognize their winning status.
     */
    @Test
    fun test8_EntrantSeesWinningStatus() {
        val waitlist = Waitlist(
            waitlistId = testWaitlistId,
            ownerId = testOwnerId,
            joinedEntrantIds = mutableListOf(testEntrant1),
            selectedWinnerIds = mutableListOf(testEntrant1),
            isDrawn = true
        )
        Tasks.await(db.collection("waitlists").document(testWaitlistId).set(waitlist))

        val doc = Tasks.await(db.collection("waitlists").document(testWaitlistId).get())
        val fetched = doc.toObject(Waitlist::class.java)

        assertNotNull(fetched)
        assertTrue(fetched!!.isDrawn)
        assertTrue(fetched.selectedWinnerIds.contains(testEntrant1))
    }

    /**
     * Test 9: Verifies query filtering for managed lotteries vs joined lotteries.
     */
    @Test
    fun test9_QueryManagedAndJoinedLotteries() {
        val waitlist = Waitlist(
            waitlistId = testWaitlistId,
            ownerId = testOwnerId,
            joinedEntrantIds = mutableListOf(testEntrant1)
        )
        Tasks.await(db.collection("waitlists").document(testWaitlistId).set(waitlist))

        // Query owner filter
        val ownerQuery = Tasks.await(db.collection("waitlists").whereEqualTo("ownerId", testOwnerId).get())
        assertFalse(ownerQuery.isEmpty)

        // Query entrant filter
        val entrantQuery = Tasks.await(db.collection("waitlists").whereArrayContains("joinedEntrantIds", testEntrant1).get())
        assertFalse(entrantQuery.isEmpty)
    }

    /**
     * Test 10: Verifies that test cleanup successfully purges temporary fixtures from Firestore.
     */
    @Test
    fun test10_CleanupAndDeleteTestData() {
        // Create test data specifically for this check
        val campsite = Campsite(campsiteId = testCampsiteId, ownerId = testOwnerId)
        Tasks.await(db.collection("campsites").document(testCampsiteId).set(campsite))

        // Execute deletion
        Tasks.await(db.collection("campsites").document(testCampsiteId).delete())

        val doc = Tasks.await(db.collection("campsites").document(testCampsiteId).get())
        assertFalse(doc.exists())
    }
}