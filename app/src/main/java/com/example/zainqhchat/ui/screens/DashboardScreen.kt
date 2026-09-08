package com.example.zainqhchat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.model.isReallyOnline
import com.example.zainqhchat.ui.components.UserAvatar
import com.example.zainqhchat.ui.theme.GoldDark
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.LuxurySurfaceDark
import com.example.zainqhchat.ui.theme.StatusOnlineGreen
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.ChatViewModel
import com.example.zainqhchat.ui.viewmodels.NotificationViewModel
import com.example.zainqhchat.ui.viewmodels.UserViewModel

/**
 * الصفحة الرئيسية الجديدة — بدون أي قائمة سفلية. Dashboard بسيط: Header
 * ترحيبي + ثلاث بطاقات كبيرة تفتح كل واحدة شاشتها الخاصة الكاملة.
 */
@Composable
fun DashboardScreen(
    currentUser: User,
    chatViewModel: ChatViewModel,
    userViewModel: UserViewModel,
    notificationViewModel: NotificationViewModel,
    onOpenChats: () -> Unit,
    onOpenOnlineUsers: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCurrency: () -> Unit,
    onContactAdmin: () -> Unit
) {
    LaunchedEffect(currentUser.id) {
        chatViewModel.setUsers(currentUser.id)
        userViewModel.setCurrentUserId(currentUser.id)
    }

    val chatPreviews by chatViewModel.chatPreviews.collectAsState()
    val usersGrouped by userViewModel.usersGroupedByOnlineStatus.collectAsState()
    val unreadNotificationCount by notificationViewModel.unreadCount.collectAsState()

    val totalUnreadMessages = chatPreviews.sumOf { it.unreadCount }
    val recentChatAvatars = chatPreviews.filter { !it.isPublic }.take(4)
    val onlineUsers = usersGrouped.values.flatten()
    val onlineNowAvatars = onlineUsers.filter { it.isReallyOnline() }.take(4)
    val onlineCount = onlineUsers.count { it.isReallyOnline() }

    var cardsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cardsVisible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlackBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                DashboardHeader(
                    currentUser = currentUser,
                    unreadNotificationCount = unreadNotificationCount,
                    onOpenProfile = onOpenProfile,
                    onOpenNotifications = onOpenNotifications
                )
            }

            item {
                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }
                ) {
                    DashboardCard(
                        title = "الدردشات",
                        subtitle = "الدردشة العامة والمحادثات الخاصة",
                        icon = Icons.Default.ChatBubble,
                        iconGradient = listOf(GoldPrimary, GoldDark),
                        statCount = totalUnreadMessages,
                        statLabel = if (totalUnreadMessages > 0) "رسالة جديدة" else null,
                        avatars = recentChatAvatars.map { it.avatarUrl to it.title },
                        onClick = onOpenChats,
                        testTag = "dashboard_card_chats"
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 }
                ) {
                    DashboardCard(
                        title = "المتصلون الآن",
                        subtitle = "اكتشف من هو متصل الآن وتواصل معه",
                        icon = Icons.Default.People,
                        iconGradient = listOf(StatusOnlineGreen, Color(0xFF16A34A)),
                        statCount = onlineCount,
                        statLabel = "متصل الآن",
                        avatars = onlineNowAvatars.map { it.avatarUrl to it.name },
                        onClick = onOpenOnlineUsers,
                        testTag = "dashboard_card_online"
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = fadeIn(tween(550)) + slideInVertically(tween(550)) { it / 3 }
                ) {
                    DashboardCard(
                        title = "العملات",
                        subtitle = "رصيدك، المكافآت، والمتجر",
                        icon = Icons.Default.MonetizationOn,
                        iconGradient = listOf(GoldPrimary, GoldDark),
                        statCount = currentUser.coins.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                        statLabel = "عملة",
                        avatars = listOf(currentUser.avatarUrl to currentUser.name),
                        onClick = onOpenCurrency,
                        testTag = "dashboard_card_currency"
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
                ) {
                    DashboardCard(
                        title = "تواصل مع المدير",
                        subtitle = "محادثة مباشرة مع إدارة التطبيق",
                        icon = Icons.Default.SupportAgent,
                        iconGradient = listOf(GoldPrimary, Color(0xFF0EA5E9)),
                        statCount = 0,
                        statLabel = null,
                        avatars = emptyList(),
                        onClick = onContactAdmin,
                        testTag = "dashboard_card_contact_admin"
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = cardsVisible,
                    enter = fadeIn(tween(650)) + slideInVertically(tween(650)) { it / 3 }
                ) {
                    DashboardCard(
                        title = "الإعدادات",
                        subtitle = "ملفك الشخصي، النقاط، والخصوصية",
                        icon = Icons.Default.Settings,
                        iconGradient = listOf(Color(0xFF64748B), Color(0xFF475569)),
                        statCount = currentUser.points,
                        statLabel = "نقطة",
                        avatars = listOf(currentUser.avatarUrl to currentUser.name),
                        onClick = onOpenSettings,
                        testTag = "dashboard_card_settings"
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    currentUser: User,
    unreadNotificationCount: Int,
    onOpenProfile: (String) -> Unit,
    onOpenNotifications: () -> Unit
) {
    Surface(
        color = LuxurySurfaceDark,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_launcher_photo),
                            contentDescription = "شعار دردشة توتة",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "دردشة توتة",
                        color = TextPrimaryWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(containerColor = GoldPrimary, contentColor = Color.White) {
                                    Text(unreadNotificationCount.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        modifier = Modifier
                            .clickable { onOpenNotifications() }
                            .testTag("notifications_bell_btn")
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "الإشعارات", tint = GoldPrimary)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    UserAvatar(
                        name = currentUser.name,
                        avatarUrl = currentUser.avatarUrl,
                        size = 40.dp,
                        isOnline = true,
                        tier = currentUser.tier,
                        modifier = Modifier
                            .clickable { onOpenProfile(currentUser.id) }
                            .testTag("my_profile_header_btn")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "مرحبًا ${currentUser.name} 👋",
                color = TextPrimaryWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
            Text(
                text = "نتمنى لك محادثات ممتعة.",
                color = TextSecondaryMuted,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconGradient: List<Color>,
    statCount: Int,
    statLabel: String?,
    avatars: List<Pair<String?, String>>,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.tween(120),
        label = "cardScale"
    )

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = LuxurySurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .scale(scale)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // أيقونة دائرية بتدرّج لوني يعطي إحساسًا بالعمق (شبه ثلاثي الأبعاد)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(iconGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = TextSecondaryMuted, fontSize = 12.sp, maxLines = 2)

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (statLabel != null) {
                        Surface(
                            color = LuxuryBorderGold.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "$statCount $statLabel",
                                color = GoldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // صور آخر المستخدمين (Stack متراكب بسيط)
                    Box {
                        avatars.take(4).forEachIndexed { index, (avatarUrl, name) ->
                            Box(modifier = Modifier.offset(x = (index * 16).dp)) {
                                UserAvatar(
                                    name = name,
                                    avatarUrl = avatarUrl,
                                    size = 22.dp,
                                    showOnlineIndicator = false
                                )
                            }
                        }
                    }
                }
            }

            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = TextSecondaryMuted
            )
        }
    }
}
