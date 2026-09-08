package com.example.zainqhchat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.OnlineStatusCategory
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.model.isReallyOnline
import com.example.zainqhchat.ui.components.LuxuryCard
import com.example.zainqhchat.ui.components.TierBadge
import com.example.zainqhchat.ui.components.UserAvatar
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted

@Composable
fun OnlineSection(
    usersGrouped: Map<OnlineStatusCategory, List<User>>,
    onOpenProfile: (userId: String) -> Unit,
    onStartPrivateChat: (userId: String, userName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OnlineStatusCategory.entries.forEach { category ->
            val usersInCategory = usersGrouped[category] ?: emptyList()

            if (usersInCategory.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = category.titleArabic,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${usersInCategory.size})",
                                color = TextSecondaryMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                items(usersInCategory, key = { it.id }) { user ->
                    OnlineUserRow(
                        user = user,
                        onProfileClick = { onOpenProfile(user.id) },
                        onChatClick = { onStartPrivateChat(user.id, user.name) }
                    )
                }
            }
        }
    }
}

@Composable
fun OnlineUserRow(
    user: User,
    onProfileClick: () -> Unit,
    onChatClick: () -> Unit
) {
    LuxuryCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("online_user_card_${user.id}"),
        onClick = onProfileClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = user.name,
                avatarUrl = user.avatarUrl,
                size = 50.dp,
                isOnline = user.isReallyOnline(),
                tier = user.tier
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = user.name,
                        color = TextPrimaryWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TierBadge(
                    tier = user.tier,
                    showPoints = true,
                    points = user.points
                )
            }

            IconButton(
                onClick = onChatClick,
                modifier = Modifier.testTag("start_chat_btn_${user.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "محادثة خاصة",
                    tint = GoldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
