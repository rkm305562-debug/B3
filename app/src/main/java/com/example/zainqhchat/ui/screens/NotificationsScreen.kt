package com.example.zainqhchat.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.NotificationItem
import com.example.zainqhchat.domain.model.NotificationType
import com.example.zainqhchat.ui.components.LuxuryCard
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceDark
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.NotificationViewModel

@Composable
fun NotificationsScreen(
    notificationViewModel: NotificationViewModel,
    onBackClick: () -> Unit,
    onOpenProfileClick: (String) -> Unit
) {
    val notifications by notificationViewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            Surface(
                color = LuxurySurfaceDark,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, LuxuryBorderGold),
                modifier = Modifier.statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = GoldPrimary)
                    }
                    Text("الإشعارات 🔔", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        },
        containerColor = LuxuryBlackBg
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = TextSecondaryMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("لا توجد إشعارات بعد", color = TextSecondaryMuted, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationRow(
                        notification = notification,
                        onClick = {
                            if (!notification.isRead) notificationViewModel.markAsRead(notification.id)
                            notification.actorId?.let { onOpenProfileClick(it) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationItem, onClick: () -> Unit) {
    LuxuryCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (notification.type) {
                    NotificationType.MESSAGE -> Icons.Default.ChatBubble
                    NotificationType.FOLLOW -> Icons.Default.PersonAdd
                    NotificationType.SYSTEM -> Icons.Default.NotificationsNone
                },
                contentDescription = null,
                tint = if (notification.isRead) TextSecondaryMuted else GoldPrimary
            )

            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = notification.title,
                    color = TextPrimaryWhite,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (!notification.body.isNullOrBlank()) {
                    Text(
                        text = notification.body,
                        color = TextSecondaryMuted,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                }
            }

            if (!notification.isRead) {
                Canvas(modifier = Modifier.padding(start = 8.dp).size(8.dp)) {
                    drawCircle(color = Color(0xFFFFD700))
                }
            }
        }
    }
}
