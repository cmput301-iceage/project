package com.example.campsitelottery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Dialog for filtering campsites by location, number of guests and date (US 1.2).
 *
 * The search text in [initial] is kept as-is; only the filter fields are edited here.
 *
 * @param initial the filters currently applied, used to prefill the fields.
 * @param onApply receives the new filters when the user taps Apply or Clear.
 * @param onDismiss called when the dialog is closed without applying.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampsiteFilterDialog(
    initial: CampsiteFilter,
    onApply: (CampsiteFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var location by rememberSaveable { mutableStateOf(initial.location) }
    var guestsText by rememberSaveable { mutableStateOf(initial.guests?.toString().orEmpty()) }
    var dateMillis by rememberSaveable { mutableStateOf(initial.dateMillis) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val guestsValid = CampsiteSearch.isValidGuestInput(guestsText)

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateMillis = pickerState.selectedDateMillis
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("confirmDateButton"),
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter campsites") },
        text = {
            Column {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    placeholder = { Text("e.g. Jasper") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("filterLocationField"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = guestsText,
                    onValueChange = { guestsText = it },
                    label = { Text("Number of guests") },
                    isError = !guestsValid,
                    supportingText = if (guestsValid) null else {
                        { Text("Enter a whole number above 0") }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("filterGuestsField"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.testTag("filterDateButton"),
                    ) {
                        Text(dateMillis?.let { CampsiteSearch.formatDate(it) } ?: "Any date")
                    }
                    if (dateMillis != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { dateMillis = null },
                            modifier = Modifier.testTag("clearDateButton"),
                        ) { Text("Clear date") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        initial.copy(
                            location = location.trim(),
                            guests = CampsiteSearch.parseGuests(guestsText),
                            dateMillis = dateMillis,
                        )
                    )
                },
                enabled = guestsValid,
                modifier = Modifier.testTag("applyFiltersButton"),
            ) { Text("Apply") }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { onApply(initial.copy(location = "", guests = null, dateMillis = null)) },
                    modifier = Modifier.testTag("clearFiltersButton"),
                ) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
