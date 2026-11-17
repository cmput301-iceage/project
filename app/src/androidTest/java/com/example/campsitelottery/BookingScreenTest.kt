package com.example.campsitelottery

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/** No live Firebase access: these screens use an injected repository. */
@RunWith(AndroidJUnit4::class)
class BookingScreenTest {
    @get:Rule val compose = createComposeRule()
    private fun date(text: String) = BookingPolicy.parse(text)!!
    private val repository = FakeBookings()

    private fun show(waitlistId: String? = "lottery") {
        compose.setContent {
            MaterialTheme { BookingScreen(rememberNavController(), waitlistId, "winner", repository) }
        }
    }

    private fun enterDates() {
        compose.onNodeWithTag("bookingCheckIn").performScrollTo().performTextInput("2030-01-02")
        compose.onNodeWithTag("bookingCheckOut").performScrollTo().performTextInput("2030-01-04")
    }

    private fun consent() {
        compose.onNodeWithTag("bookingConsent").performScrollTo().performClick()
    }

    @Test fun explicitConsentIsRequiredAndConfirmationShowsSavedDetails() {
        show()
        enterDates()
        compose.onNodeWithTag("confirmBooking").performScrollTo().assertIsNotEnabled()
        consent()
        compose.onNodeWithTag("confirmBooking").performScrollTo().performClick()
        compose.onNodeWithText("Reservation confirmed").assertIsDisplayed()
        compose.onNodeWithText("Check-in: 2030-01-02").assertExists()
        compose.onNodeWithText("Check-out: 2030-01-04").assertExists()
        assertEquals(1, repository.confirmations)
        assertEquals("Quiet after 10", repository.lastRequest!!.acceptedRules)
        assertEquals("Seven days notice", repository.lastRequest!!.acceptedCancellationPolicy)
        assertEquals("winner", repository.lastRequest!!.entrantId)
        assertEquals("lottery", repository.lastRequest!!.waitlistId)
    }

    @Test fun switchingCampsiteClearsConsentAndControlsHaveLabels() {
        show()
        enterDates()
        consent()
        compose.onNodeWithTag("chooseCampsite_other").performScrollTo()
            .assert(hasText("Other campsite", substring = true)).performClick()
        compose.onNodeWithTag("bookingConsent").performScrollTo()
            .assertIsOff().assert(hasText("I have read and accept", substring = true))
        compose.onNodeWithTag("confirmBooking").performScrollTo().assertIsNotEnabled()
    }

    @Test fun unavailableNightsAreShownAndCannotBeConfirmed() {
        repository.options = repository.options.copy(
            unavailableNights = mapOf("site" to setOf(date("2030-01-03").time)),
        )
        show()
        compose.onNodeWithTag("unavailableNights").performScrollTo()
            .assert(hasText("2030-01-03", substring = true))
        enterDates()
        consent()
        compose.onNodeWithTag("confirmBooking").performScrollTo().performClick()
        compose.onNodeWithTag("bookingError").assert(hasText("no longer available", substring = true))
        assertEquals(0, repository.confirmations)
    }

    @Test fun saveFailureIsVisibleAndCanBeRetried() {
        repository.failure = "Connection unavailable"
        show()
        enterDates()
        consent()
        compose.onNodeWithTag("confirmBooking").performScrollTo().performClick()
        compose.onNodeWithTag("bookingError").assertTextEquals("Connection unavailable")
        repository.failure = null
        compose.onNodeWithTag("confirmBooking").performScrollTo().performClick()
        compose.onNodeWithText("Reservation confirmed").assertIsDisplayed()
    }

    @Test fun declineRequiresConfirmationAndDoesNotCreateReservation() {
        show()
        compose.onNodeWithTag("declineBooking").performScrollTo().performClick()
        assertEquals(0, repository.declines)
        compose.onNodeWithTag("confirmDecline").performClick()
        compose.onNodeWithTag("declineConfirmation").assertTextEquals("Invitation declined")
        assertEquals(1, repository.declines)
        assertEquals(0, repository.confirmations)
    }

    @Test fun reopeningSavedReservationDoesNotOfferAnotherBooking() {
        repository.options = repository.options.copy(reservation = Reservation(
            reservationId = "saved-id", waitlistId = "lottery", entrantId = "winner",
            campsiteId = "site", campsiteName = "Saved campsite name", status = ReservationStatus.CONFIRMED,
            checkInDate = date("2030-01-02"), checkOutDate = date("2030-01-04"),
        ), campsites = emptyList())
        show()
        compose.onNodeWithText("Reservation confirmed").assertIsDisplayed()
        compose.onNodeWithText("Confirmation ID: saved-id").assertExists()
        compose.onNodeWithText("Campsite: Saved campsite name").assertExists()
        compose.onNodeWithTag("confirmBooking").assertDoesNotExist()
        compose.onNodeWithTag("declineBooking").assertDoesNotExist()
    }

    @Test fun missingInvitationCannotWriteAnything() {
        show(waitlistId = null)
        compose.onNodeWithTag("bookingError").assert(hasText("My lotteries", substring = true))
        assertEquals(0, repository.loads)
        assertEquals(0, repository.confirmations)
    }

    @Test fun routeCarriesAndEncodesLotteryIdentity() {
        assertEquals("booking/lottery%201", bookingRoute("lottery 1"))
    }

    private inner class FakeBookings : BookingRepository {
        var loads = 0
        var confirmations = 0
        var declines = 0
        var failure: String? = null
        var lastRequest: BookingRequest? = null
        var options = BookingOptions(
            waitlist = Waitlist(
                waitlistId = "lottery", lotteryName = "Test lottery",
                ownerId = "owner", campsiteIds = listOf("site", "other"),
                isDrawn = true, selectedWinnerIds = mutableListOf("winner"),
                bookingStartDate = date("2030-01-01"), bookingEndDate = date("2030-01-10"),
            ),
            campsites = listOf(
                Campsite(campsiteId = "site", ownerId = "owner", name = "First campsite", pricePerNight = 25.0,
                    rules = "Quiet after 10", cancellationPolicy = "Seven days notice"),
                Campsite(campsiteId = "other", ownerId = "owner", name = "Other campsite", pricePerNight = 30.0,
                    rules = "Other rules", cancellationPolicy = "No refunds"),
            ),
            unavailableNights = emptyMap(),
        )

        override fun load(waitlistId: String, entrantId: String, onSuccess: (BookingOptions) -> Unit, onFailure: (Exception) -> Unit) {
            loads++
            onSuccess(options)
        }

        override fun confirm(request: BookingRequest, onSuccess: (Reservation) -> Unit, onFailure: (Exception) -> Unit) {
            confirmations++
            lastRequest = request
            failure?.let { onFailure(IllegalStateException(it)); return }
            onSuccess(Reservation(
                reservationId = "saved-id", waitlistId = request.waitlistId,
                entrantId = request.entrantId, campsiteId = request.campsiteId,
                checkInDate = request.checkIn, checkOutDate = request.checkOut,
                status = ReservationStatus.CONFIRMED, rulesAccepted = request.rulesAccepted,
                totalPrice = 50.0, createdAt = Date(), updatedAt = Date(),
            ))
        }

        override fun decline(waitlistId: String, entrantId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
            declines++
            onSuccess()
        }
    }
}
