package com.example.campsitelottery

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

/** A winning invitation must be carried through navigation, not inferred from a global user. */
fun bookingRoute(waitlistId: String): String = "booking/${Uri.encode(waitlistId)}"

/**
 * Winner booking: choose an included campsite and available nights, review its
 * rules/policy, then confirm or explicitly decline the invitation.
 *
 * The injectable repository keeps UI tests and previews independent of live data.
 */
@Composable
fun BookingScreen(
    navController: NavController,
    waitlistId: String? = null,
    entrantId: String? = null,
    repository: BookingRepository = remember { FirestoreBookingRepository() },
) {
    val context = LocalContext.current
    val userId = entrantId ?: remember(context) { DeviceIdentity.getDeviceId(context) }
    var options by remember(waitlistId, userId, repository) { mutableStateOf<BookingOptions?>(null) }
    var loading by remember(waitlistId, userId, repository) { mutableStateOf(true) }
    var busy by remember(waitlistId, userId, repository) { mutableStateOf(false) }
    var error by remember(waitlistId, userId, repository) { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var selectedId by remember(waitlistId, userId) { mutableStateOf<String?>(null) }
    var checkInText by remember(waitlistId, userId) { mutableStateOf("") }
    var checkOutText by remember(waitlistId, userId) { mutableStateOf("") }
    var declined by remember(waitlistId, userId) { mutableStateOf(false) }
    var askDecline by remember { mutableStateOf(false) }
    val active = remember(waitlistId, userId, repository) { AtomicBoolean(true) }

    DisposableEffect(waitlistId, userId, repository, refresh) {
        var currentLoad = true
        active.set(true)
        loading = true
        error = null
        options = null
        if (waitlistId.isNullOrBlank()) {
            loading = false
            error = "Open a winning invitation from My lotteries to choose a reservation."
        } else {
            repository.load(waitlistId, userId, { result ->
                if (currentLoad) {
                    options = result
                    selectedId = selectedId?.takeIf { id -> result.campsites.any { it.campsiteId == id } }
                        ?: result.campsites.firstOrNull()?.campsiteId
                    loading = false
                }
            }, { failure ->
                if (currentLoad) {
                    error = failure.message ?: "Could not load booking options."
                    loading = false
                }
            })
        }
        onDispose { currentLoad = false; active.set(false) }
    }

    val selected = options?.campsites?.firstOrNull { it.campsiteId == selectedId }
    var consent by remember(selectedId, selected?.rules, selected?.cancellationPolicy, selected?.pricePerNight) {
        mutableStateOf(false)
    }
    val checkIn = BookingPolicy.parse(checkInText)
    val checkOut = BookingPolicy.parse(checkOutText)
    val nights = if (checkIn != null && checkOut != null)
        runCatching { BookingPolicy.nights(checkIn, checkOut) }.getOrNull() else null
    val existing = options?.reservation

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.testTag("toBackButton"),
        ) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text("Book your winning invitation", fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("bookingError")) }

        when {
            loading -> CircularProgressIndicator(Modifier.testTag("bookingLoading"))
            declined -> {
                Text("Invitation declined", modifier = Modifier.testTag("declineConfirmation"))
                Text("You will not reserve a campsite through this invitation.")
            }
            existing != null -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("bookingConfirmation"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Text(
                            if (existing.status == ReservationStatus.CONFIRMED) "Reservation confirmed"
                            else "Reservation: ${existing.status}",
                            fontSize = 22.sp,
                        )
                        Text("Confirmation ID: ${existing.reservationId}")
                        Text("Campsite: ${existing.campsiteName.ifBlank {
                            options?.campsites?.firstOrNull { it.campsiteId == existing.campsiteId }?.name
                                ?: existing.campsiteId
                        }}")
                        Text("Check-in: ${existing.checkInDate?.let(BookingPolicy::format) ?: "—"}")
                        Text("Check-out: ${existing.checkOutDate?.let(BookingPolicy::format) ?: "—"}")
                        Text("Total: $${"%.2f".format(existing.totalPrice)}")
                        Text("This invitation has been used. Cancellation does not create a second invitation.")
                    }
                }
                Button(onClick = { navController.navigate("reservation") }) { Text("View my reservations") }
            }
            options != null -> {
                val current = options!!
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("bookingForm"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(current.waitlist.lotteryName, fontSize = 20.sp)
                        Text("Choose a campsite")
                    }
                    items(current.campsites, key = { it.campsiteId }) { site ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .testTag("chooseCampsite_${site.campsiteId}")
                                .selectable(
                                    selected = selectedId == site.campsiteId, enabled = !busy,
                                    role = Role.RadioButton, onClick = { selectedId = site.campsiteId },
                                ),
                        ) {
                            RadioButton(
                                selected = selectedId == site.campsiteId,
                                enabled = !busy,
                                onClick = null,
                            )
                            Text("${site.name} — $${"%.2f".format(site.pricePerNight)}/night")
                        }
                    }
                    if (selected != null) {
                        val unavailable = current.unavailableNights[selected.campsiteId].orEmpty()
                        val first = current.waitlist.bookingStartDate?.let(BookingPolicy::day)
                        val last = current.waitlist.bookingEndDate?.let(BookingPolicy::day)
                        item {
                            Text("Stay dates are calendar dates, not times. Checkout does not occupy a night.")
                            if (first != null && last != null && first <= last) {
                                Text("Available stay period: ${BookingPolicy.format(Date(first))} through ${BookingPolicy.format(Date(last))} (inclusive nights)")
                                Text("Latest checkout: ${BookingPolicy.format(Date(last + BookingPolicy.DAY_MILLIS))}")
                            } else Text("This lottery has no valid stay period.")
                            val blocked = unavailable.sorted().filter {
                                first != null && last != null && it in first..last
                            }
                            Text(
                                if (blocked.isEmpty()) "No reserved nights in this period."
                                else "Unavailable nights: ${blocked.joinToString { BookingPolicy.format(Date(it)) }}",
                                modifier = Modifier.testTag("unavailableNights"),
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = checkInText, onValueChange = { checkInText = it },
                                label = { Text("Check-in (yyyy-MM-dd)") }, singleLine = true,
                                enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("bookingCheckIn"),
                            )
                            OutlinedTextField(
                                value = checkOutText, onValueChange = { checkOutText = it },
                                label = { Text("Check-out (yyyy-MM-dd)") }, singleLine = true,
                                enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("bookingCheckOut"),
                            )
                            if (nights != null) Text("${nights.size} nights — total $${"%.2f".format(nights.size * selected.pricePerNight)}")
                        }
                        item {
                            Text("Campsite rules", fontSize = 20.sp)
                            Text(selected.rules.ifBlank { "No additional campsite rules." }, Modifier.testTag("bookingRules"))
                            Text("Cancellation policy", fontSize = 20.sp)
                            Text(selected.cancellationPolicy.ifBlank { "No additional cancellation policy." }, Modifier.testTag("bookingPolicy"))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().testTag("bookingConsent")
                                    .toggleable(
                                        value = consent, enabled = !busy, role = Role.Checkbox,
                                        onValueChange = { consent = it },
                                    ),
                            ) {
                                Checkbox(
                                    checked = consent, onCheckedChange = null, enabled = !busy,
                                )
                                Text("I have read and accept the campsite rules and cancellation policy.")
                            }
                        }
                        item {
                            Button(
                                enabled = !busy && consent && nights != null,
                                modifier = Modifier.fillMaxWidth().testTag("confirmBooking"),
                                onClick = {
                                    val request = BookingRequest(
                                        waitlistId = waitlistId!!, entrantId = userId,
                                        campsiteId = selected.campsiteId, checkIn = checkIn!!, checkOut = checkOut!!,
                                        rulesAccepted = consent, acceptedRules = selected.rules,
                                        acceptedCancellationPolicy = selected.cancellationPolicy,
                                        pricePerNight = selected.pricePerNight,
                                    )
                                    val invalid = runCatching {
                                        BookingPolicy.validate(request, current.waitlist, selected, unavailable, Date())
                                    }.exceptionOrNull()
                                    if (invalid != null) error = invalid.message
                                    else {
                                        busy = true
                                        error = null
                                        repository.confirm(request, { saved ->
                                            if (active.get()) { options = current.copy(reservation = saved); busy = false }
                                        }, { failure ->
                                            if (active.get()) { error = failure.message ?: "Could not confirm reservation."; busy = false }
                                        })
                                    }
                                },
                            ) { Text(if (busy) "Saving..." else "Confirm reservation") }
                        }
                    } else item { Text("No campsites are available for this invitation.") }
                    item {
                        TextButton(enabled = !busy, onClick = { refresh++ }, modifier = Modifier.testTag("refreshBooking")) {
                            Text("Refresh availability")
                        }
                        Button(
                            enabled = !busy, onClick = { askDecline = true },
                            modifier = Modifier.fillMaxWidth().testTag("declineBooking"),
                        ) { Text("Decline invitation") }
                    }
                }
            }
            !waitlistId.isNullOrBlank() -> Button(onClick = { refresh++ }) { Text("Retry") }
        }
    }
    if (askDecline) {
        AlertDialog(
            onDismissRequest = { if (!busy) askDecline = false },
            title = { Text("Decline this invitation?") },
            text = { Text("You will no longer be able to book through this lottery win.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    repository.decline(waitlistId!!, userId, {
                        if (active.get()) { declined = true; busy = false; askDecline = false; error = null }
                    }, { failure ->
                        if (active.get()) { busy = false; askDecline = false; error = failure.message ?: "Could not decline invitation." }
                    })
                }, modifier = Modifier.testTag("confirmDecline")) { Text("Decline") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { askDecline = false }) { Text("Keep invitation") } },
        )
    }
}
