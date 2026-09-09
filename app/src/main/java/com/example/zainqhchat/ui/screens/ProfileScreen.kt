package com.example.zainqhchat.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.model.UserTier
import com.example.zainqhchat.domain.model.isReallyOnline
import com.example.zainqhchat.ui.components.GoldButton
import com.example.zainqhchat.ui.components.GoldOutlinedButton
import com.example.zainqhchat.ui.components.LuxuryCard
import com.example.zainqhchat.ui.components.TierBadge
import com.example.zainqhchat.ui.components.UserAvatar
import com.example.zainqhchat.ui.theme.GoldDark
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.UserViewModel

@Composable
fun ProfileScreen(
    targetUserId: String,
    currentUserId: String,
    userViewModel: UserViewModel,
    onBackClick: () -> Unit,
    onStartPrivateChat: (userId: String, userName: String) -> Unit
) {
    val userProfile by userViewModel.getUserProfile(targetUserId, currentUserId).collectAsState(initial = null)
    val actionMessage by userViewModel.actionMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var showBlockDialog by remember { mutableStateOf(false) }

    val isSelf = targetUserId == currentUserId

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            userViewModel.clearActionMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LuxuryBlackBg
    ) { padding ->
        if (userProfile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        } else {
            val user = userProfile!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                // شريط العودة والخيارات العلوي
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = GoldPrimary
                        )
                    }

                    Text(
                        text = if (isSelf) "ملفي الشخصي 👑" else "الملف الشخصي 👤",
                        color = GoldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    if (!isSelf) {
                        Row {
                            IconButton(onClick = { showReportDialog = true }) {
                                Icon(Icons.Default.Flag, contentDescription = "إبلاغ", tint = Color(0xFFFF5252))
                            }
                            IconButton(onClick = { showBlockDialog = true }) {
                                Icon(Icons.Default.Block, contentDescription = "حظر", tint = TextSecondaryMuted)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                // بطاقة صورة ومعلومات المستخدم الرئيسية
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(
                        name = user.name,
                        avatarUrl = user.avatarUrl,
                        size = 110.dp,
                        isOnline = user.isReallyOnline(),
                        tier = user.tier
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = user.name,
                        color = TextPrimaryWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "@${user.username}",
                        color = TextSecondaryMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TierBadge(
                        tier = user.tier,
                        showPoints = true,
                        points = user.points
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // إحصائيات المتابعين والذين يتابعهم والنقاط
                    LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileStatItem(count = user.followerCount.toString(), label = "المتابعين")
                            Box(modifier = Modifier.size(1.dp, 30.dp).background(LuxuryBorderGold))
                            ProfileStatItem(count = user.followingCount.toString(), label = "يتابعهم")
                            Box(modifier = Modifier.size(1.dp, 30.dp).background(LuxuryBorderGold))
                            ProfileStatItem(count = user.points.toString(), label = "النقاط 🌟")
                        }
                    }

                    // شارة السيارة المختارة (قسم العملات) — تظهر فقط إن كان المستخدم قد اشترى واختار سيارة.
                    val ownedCarInfo = com.example.zainqhchat.ui.screens.currency.CurrencyStaticCatalog
                        .carInfo(user.selectedCarId)
                    if (ownedCarInfo != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = ownedCarInfo.imageRes),
                                    contentDescription = ownedCarInfo.name,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("السيارة 🚗", color = TextSecondaryMuted, fontSize = 11.sp)
                                    Text(ownedCarInfo.name, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // شريط التقدم نحو الفئة القادمة
                    val nextTier = getNextTier(user.tier)
                    if (nextTier != null) {
                        LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "التقدم نحو فئة: ${nextTier.titleArabic} ${nextTier.badgeSymbol}",
                                        color = TextPrimaryWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${user.points} / ${nextTier.minPoints}",
                                        color = GoldPrimary,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val progress = (user.points.toFloat() / nextTier.minPoints.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = GoldPrimary,
                                    trackColor = LuxuryBlackBg
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // أزرار التفاعل والمتابعة / إرسال رسالة
                    if (!isSelf) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (user.isFollowedByCurrentUser) {
                                GoldOutlinedButton(
                                    text = "إلغاء المتابعة ✖",
                                    onClick = {
                                        userViewModel.toggleFollow(currentUserId, user.id, true)
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "unfollow_btn"
                                )
                            } else {
                                GoldButton(
                                    text = "متابعة +",
                                    onClick = {
                                        userViewModel.toggleFollow(currentUserId, user.id, false)
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "follow_btn"
                                )
                            }

                            GoldButton(
                                text = "محادثة 💬",
                                onClick = {
                                    onStartPrivateChat(user.id, user.name)
                                },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Chat,
                                testTag = "start_private_chat_profile_btn"
                            )
                        }
                    }
                }
            }
        }
    }

    // نافذة البلاغ
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = LuxurySurfaceCard,
            title = { Text("رفع بلاغ على المستخدم 🚩", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("اكتب سبب البلاغ ليتم مراجعته من الإدارة:", color = TextSecondaryMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("السبب") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("report_reason_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reportReason.isNotBlank()) {
                            userViewModel.reportUser(currentUserId, targetUserId, reportReason)
                            showReportDialog = false
                            reportReason = ""
                        }
                    }
                ) {
                    Text("إرسال البلاغ 🚀", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("إلغاء", color = TextSecondaryMuted)
                }
            }
        )
    }

    // نافذة الحظر
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            containerColor = LuxurySurfaceCard,
            title = { Text("حظر المستخدم 🚫", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت تأكد من رغبتك في حظر هذا المستخدم؟ لن تتمكن من رؤية رسائله بعد الآن.", color = TextPrimaryWhite, fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userViewModel.blockUser(currentUserId, targetUserId)
                        showBlockDialog = false
                        onBackClick()
                    }
                ) {
                    Text("تأكيد الحظر", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("إلغاء", color = TextSecondaryMuted)
                }
            }
        )
    }
}

@Composable
fun ProfileStatItem(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            color = GoldPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            color = TextSecondaryMuted,
            fontSize = 12.sp
        )
    }
}

private fun getNextTier(currentTier: UserTier): UserTier? {
    return when (currentTier) {
        UserTier.NEW -> UserTier.MEMBER
        UserTier.MEMBER -> UserTier.SENIOR_MEMBER
        UserTier.SENIOR_MEMBER -> UserTier.SPECIAL
        UserTier.SPECIAL -> UserTier.VIP
        UserTier.VIP -> UserTier.LEGEND
        UserTier.LEGEND -> null
    }
}
