/**
 * Purpose: Dialog for composing and sending an in-app message.
 * Role / Design Pattern: View layer dialog component shared by entrant and owner flows.
 * Outstanding Issues: None.
 */
package com.example.campsitelottery

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Lets the user type a message and send it through [messageRepository].
 *
 * @param title dialog title (e.g. "Message owner").
 * @param fromUserId current user's id.
 * @param toUserId recipient id.
 * @param campsiteId optional campsite context.
 * @param reservationId optional reservation context.
 * @param messageRepository where the message is stored.
 * @param onSent called after a successful send.
 * @param onDismiss called when the dialog is closed without sending, or after send.
 */
@Composable
fun SendMessageDialog(
    title: String,
    fromUserId: String,
    toUserId: String,
    campsiteId: String = "",
    reservationId: String = "",
    messageRepository: MessageRepository,
    onSent: () -> Unit,
    onDismiss: () -> Unit,
) {
    var body by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = body,
                onValueChange = {
                    body = it
                    error = null
                },
                label = { Text("Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("messageBodyField"),
                enabled = !isSending,
                minLines = 3,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isSending) return@TextButton
                    isSending = true
                    error = null
                    messageRepository.sendMessage(
                        fromUserId = fromUserId,
                        toUserId = toUserId,
                        body = body,
                        campsiteId = campsiteId,
                        reservationId = reservationId,
                        onSuccess = {
                            isSending = false
                            onSent()
                            onDismiss()
                        },
                        onFailure = {
                            isSending = false
                            error = it.message ?: "Could not send message"
                        },
                    )
                },
                enabled = !isSending && body.isNotBlank(),
                modifier = Modifier.testTag("sendMessageButton"),
            ) {
                Text(if (isSending) "Sending..." else "Send")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSending,
                modifier = Modifier.testTag("cancelMessageButton"),
            ) {
                Text("Cancel")
            }
        },
    )
}
