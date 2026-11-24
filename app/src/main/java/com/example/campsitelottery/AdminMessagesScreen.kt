/**
 * Purpose: Admin screen to review all in-app messages between owners and entrants (US 03.12.01).
 * Role / Design Pattern: View layer Composable for message moderation/audit.
 * Outstanding Issues: None.
 */
package com.example.campsitelottery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Live browse of every message in the `messages` collection (US 03.12.01).
 */
@Composable
fun AdminMessagesScreen(
    navController: NavController,
    messageRepository: MessageRepository = remember { FirestoreMessageRepository() },
) {
    val backgroundColor = Color(0xFFFEFAE0)
    val buttonColor = Color(0xFF283618)

    var searchText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Message>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    DisposableEffect(messageRepository) {
        val listener = messageRepository.listenAllMessages(
            onSuccess = {
                messages.clear()
                messages.addAll(it)
                errorMessage = null
            },
            onFailure = { errorMessage = it.message },
        )
        onDispose { listener.remove() }
    }

    val filtered = messages.filter { message ->
        val q = searchText.trim()
        if (q.isEmpty()) return@filter true
        message.body.contains(q, ignoreCase = true) ||
            message.fromUserId.contains(q, ignoreCase = true) ||
            message.toUserId.contains(q, ignoreCase = true) ||
            message.campsiteId.contains(q, ignoreCase = true) ||
            message.reservationId.contains(q, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { navController.popBackStack() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = Color.White),
                modifier = Modifier.testTag("backButton"),
            ) {
                Text("< Back")
            }
            Text(
                text = "Messages",
                fontSize = 28.sp,
                modifier = Modifier.testTag("adminMessagesHeader"),
            )
            Spacer(modifier = Modifier.height(1.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        BasicTextField(
            value = searchText,
            onValueChange = { searchText = it },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = buttonColor,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .border(2.dp, buttonColor)
                .padding(8.dp)
                .wrapContentHeight(Alignment.CenterVertically)
                .testTag("adminMessagesSearch"),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (searchText.isEmpty()) {
                        Text("Search...", color = buttonColor, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))
        errorMessage?.let {
            Text(it, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filtered.isEmpty()) {
            Text(
                "No messages yet.",
                modifier = Modifier.testTag("adminMessagesEmpty"),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("adminMessagesList"),
        ) {
            items(filtered, key = { it.messageId.ifBlank { it.hashCode().toString() } }) { message ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.dp, buttonColor)
                        .padding(12.dp)
                        .testTag("adminMessage_${message.messageId}"),
                ) {
                    Text(
                        dateFormat.format(message.createdAt),
                        fontSize = 12.sp,
                        color = buttonColor,
                    )
                    Text("From: ${message.fromUserId}", fontSize = 13.sp)
                    Text("To: ${message.toUserId}", fontSize = 13.sp)
                    if (message.campsiteId.isNotBlank()) {
                        Text("Campsite: ${message.campsiteId}", fontSize = 12.sp)
                    }
                    if (message.reservationId.isNotBlank()) {
                        Text("Reservation: ${message.reservationId}", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(message.body, fontSize = 15.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminMessagesScreenPreview() {
    AdminMessagesScreen(
        rememberNavController(),
        messageRepository = remember {
            InMemoryMessageRepository(
                listOf(
                    Message(
                        messageId = "1",
                        fromUserId = "entrant-1",
                        toUserId = "owner-1",
                        body = "Hi, is water available?",
                        campsiteId = "camp-1",
                    ),
                ),
            )
        },
    )
}
