package com.example.campsitelottery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Data class representing grouped notification items for a single lottery waitlist.
 */
data class LotteryNotificationGroup(
    val waitlistId: String,
    val lotteryName: String,
    val notifications: List<NotificationItem>
)

/**
 * Screen for campsite owners to audit all notifications sent for their lotteries.
 * Compacts notifications by lottery and displays detailed entrant notification history.
 */
@Composable
fun OwnerViewNotificationsScreen(
    navController: NavController,
    ownerId: String
) {
    val buttonColor = Color(0xFF283618)
    val cardBg = Color(0xFFFEFAE0)

    var groups by remember { mutableStateOf<List<LotteryNotificationGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val expandedWaitlists = remember { mutableStateMapOf<String, Boolean>() }
    val userNames = remember { mutableStateMapOf<String, String>() }
    val scope = rememberCoroutineScope()

    DisposableEffect(ownerId) {
        val db = FirebaseFirestore.getInstance()

        // Fetch owner waitlists to resolve waitlist names & IDs
        val waitlistListener = db.collection("waitlists")
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { waitlistSnapshot, _ ->
                val waitlistMap = waitlistSnapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Waitlist::class.java)
                }?.associate { it.waitlistId to it.lotteryName } ?: emptyMap()

                val ownerWaitlistIds = waitlistMap.keys

                // Fetch all notifications sent for this owner's lotteries
                db.collection("notifications")
                    .addSnapshotListener { notifSnapshot, notifError ->
                        if (notifError != null) {
                            isLoading = false
                            return@addSnapshotListener
                        }

                        val allNotifs = notifSnapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(NotificationItem::class.java)
                        } ?: emptyList()

                        // Filter by ownerId matching or waitlistId belonging to owner
                        val filteredNotifs = allNotifs.filter { notif ->
                            (notif.ownerId == ownerId || notif.waitlistId in ownerWaitlistIds) &&
                                    notif.waitlistId.isNotBlank()
                        }

                        // Group by waitlistId
                        val groupedMap = filteredNotifs.groupBy { it.waitlistId }

                        val groupList = groupedMap.map { (wId, list) ->
                            val name = waitlistMap[wId] ?: list.firstOrNull()?.title?.replace("Lottery Joined! ⛺", "")?.replace("Waitlist Joined! ⛺", "")?.trim()?.ifBlank { null } ?: wId
                            LotteryNotificationGroup(
                                waitlistId = wId,
                                lotteryName = name,
                                notifications = list.sortedByDescending { it.timestamp }
                            )
                        }.sortedByDescending { group -> group.notifications.maxOfOrNull { it.timestamp } ?: java.util.Date(0) }

                        groups = groupList
                        isLoading = false

                        // Resolve user names asynchronously for recipient user IDs
                        val distinctUserIds = filteredNotifs.map { it.userId }.filter { it.isNotBlank() }.distinct()
                        scope.launch {
                            for (uId in distinctUserIds) {
                                if (!userNames.containsKey(uId)) {
                                    try {
                                        val uDoc = db.collection("users").document(uId).get().await()
                                        val name = uDoc.getString("name")?.ifBlank { null } ?: uId
                                        userNames[uId] = name
                                    } catch (e: Exception) {
                                        userNames[uId] = uId
                                    }
                                }
                            }
                        }
                    }
            }

        onDispose {
            waitlistListener.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Absolute.Left
        ) {
            Button(
                onClick = { navController.popBackStack() },
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
            ) {
                Text("< Back")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Sent Notifications", fontSize = 24.sp, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            groups.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No notifications sent for your lotteries yet.",
                        fontSize = 16.sp,
                        modifier = Modifier.testTag("emptyOwnerNotifications")
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("ownerNotificationList")
                ) {
                    items(groups, key = { it.waitlistId }) { group ->
                        val isExpanded = expandedWaitlists[group.waitlistId] ?: false
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    expandedWaitlists[group.waitlistId] = !isExpanded
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = buttonColor,
                                contentColor = cardBg
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${group.lotteryName} notifications",
                                        fontSize = 18.sp,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = cardBg,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (isExpanded) "▲ Hide" else "▼ View (${group.notifications.size})",
                                        fontSize = 14.sp,
                                        color = cardBg.copy(alpha = 0.9f)
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                    ) {
                                        Text(
                                            text = "Notification History:",
                                            fontSize = 14.sp,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = cardBg.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        group.notifications.forEach { notif ->
                                            val recipientName = userNames[notif.userId] ?: notif.userId
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = cardBg,
                                                    contentColor = buttonColor
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(
                                                        text = recipientName,
                                                        fontSize = 16.sp,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    Text(
                                                        text = dateLongUIFormat(notif.timestamp),
                                                        fontSize = 12.sp,
                                                        color = buttonColor.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
