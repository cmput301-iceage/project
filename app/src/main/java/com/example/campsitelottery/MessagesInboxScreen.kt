/**
 * Purpose: Inbox of in-app messages for the current entrant or owner.
 * Role / Design Pattern: View layer Composable listing messages for one user.
 * Outstanding Issues: None.
 */
package com.example.campsitelottery

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Shows messages sent or received by [userId].
 */
@Composable
fun MessagesInboxScreen(
    navController: NavController,
    userId: String,
    messageRepository: MessageRepository = remember { FirestoreMessageRepository() },
) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    DisposableEffect(userId, messageRepository) {
        val listener = messageRepository.listenMessagesForUser(
            userId = userId,
            onSuccess = {
                messages = it
                isLoading = false
                errorMessage = null
            },
            onFailure = {
                errorMessage = it.message
                isLoading = false
            },
        )
        onDispose { listener.remove() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Absolute.Left,
        ) {
            Button(
                onClick = { navController.popBackStack() },
                shape = RectangleShape,
                modifier = Modifier.testTag("toBackButton"),
            ) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Messages", fontSize = 24.sp, modifier = Modifier.testTag("messagesInboxHeader"))
        Spacer(modifier = Modifier.height(8.dp))

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            messages.isEmpty() -> {
                Text("No messages yet.", modifier = Modifier.testTag("messagesInboxEmpty"))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("messagesInboxList"),
                ) {
                    items(
                        messages,
                        key = { it.messageId.ifBlank { it.hashCode().toString() } },
                    ) { message ->
                        val direction = if (message.fromUserId == userId) "Sent to" else "From"
                        val other = if (message.fromUserId == userId) {
                            message.toUserId
                        } else {
                            message.fromUserId
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline)
                                .padding(12.dp)
                                .testTag("inboxMessage_${message.messageId}"),
                        ) {
                            Text(dateFormat.format(message.createdAt), fontSize = 12.sp)
                            Text("$direction: $other", fontSize = 13.sp)
                            if (message.campsiteId.isNotBlank()) {
                                Text("Campsite: ${message.campsiteId}", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(message.body, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessagesInboxScreenPreview() {
    MessagesInboxScreen(
        rememberNavController(),
        userId = "preview-user",
        messageRepository = remember { InMemoryMessageRepository() },
    )
}
