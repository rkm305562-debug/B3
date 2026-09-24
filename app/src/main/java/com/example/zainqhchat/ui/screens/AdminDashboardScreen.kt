package com.example.zainqhchat.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.zainqhchat.domain.model.AdminActionLogEntry
import com.example.zainqhchat.domain.model.AdminActionType
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.model.isReallyOnline
import com.example.zainqhchat.ui.components.LuxuryCard
import com.example.zainqhchat.ui.components.UserAvatar
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceDark
import com.example.zainqhchat.ui.theme.StatusErrorRed
import com.example.zainqhchat.ui.theme.StatusOnlineGreen
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class AdminSection(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val adminSections = listOf(
    AdminSection(0, "الدردشة العامة", "مراقبة الرسائل وحذفها", Icons.Default.Forum),
    AdminSection(1, "المستخدمون", "إدارة الحسابات والحظر", Icons.Default.People),
    AdminSection(5, "الأجهزة المحظورة", "عرض الحظر النهائي وفكّه", Icons.Default.Block),
    AdminSection(2, "سجل الإجراءات", "كل ما نفّذه المدراء", Icons.Default.History),
    AdminSection(3, "بث إشعار", "إشعار لكل المستخدمين", Icons.Default.Notifications),
    AdminSection(4, "الأقسام", "إغلاق الأقسام مؤقتًا", Icons.Default.Settings)
)

@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    onOpenProfile: (String) -> Unit,
    onBackClick: () -> Unit
) {
    // null = قائمة البطاقات الرئيسية، وإلا رقم القسم المفتوح حاليًا.
    var selectedTab by remember { mutableStateOf<Int?>(null) }
    val statusMessage by adminViewModel.statusMessage.collectAsState()
    val openSection = adminSections.firstOrNull { it.id == selectedTab }

    LaunchedEffect(Unit) {
        adminViewModel.searchUsers(null)
        adminViewModel.loadActionLog()
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 5) adminViewModel.loadBannedDevices()
    }

    BackHandler(enabled = selectedTab != null) { selectedTab = null }

    Scaffold(
        topBar = {
            Surface(color = LuxurySurfaceDark, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (selectedTab != null) selectedTab = null else onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimaryWhite)
                    }
                    Text(
                        openSection?.title ?: "لوحة الإدارة 🛡️",
                        color = TextPrimaryWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        },
        containerColor = LuxuryBlackBg
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                null -> AdminSectionsGrid(onSelect = { selectedTab = it })
                0 -> AdminModerationTab(adminViewModel, onOpenProfile)
                1 -> AdminUsersTab(adminViewModel, onOpenProfile)
                2 -> AdminActionLogTab(adminViewModel)
                3 -> AdminBroadcastTab(adminViewModel)
                4 -> AdminFeatureFlagsTab(adminViewModel)
                5 -> AdminBannedDevicesTab(adminViewModel)
            }
        }
    }

    if (statusMessage != null) {
        AlertDialog(
            onDismissRequest = { adminViewModel.clearStatusMessage() },
            containerColor = LuxurySurfaceDark,
            title = { Text("نتيجة العملية", color = GoldPrimary) },
            text = { Text(statusMessage ?: "", color = TextPrimaryWhite) },
            confirmButton = {
                TextButton(onClick = { adminViewModel.clearStatusMessage() }) {
                    Text("حسنًا", color = GoldPrimary)
                }
            }
        )
    }
}

@Composable
private fun AdminSectionsGrid(onSelect: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        gridItems(adminSections, key = { it.id }) { section ->
            LuxuryCard(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                onClick = { onSelect(section.id) }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(section.icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(38.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        section.title,
                        color = TextPrimaryWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        section.subtitle,
                        color = TextSecondaryMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminBannedDevicesTab(adminViewModel: AdminViewModel) {
    val devices by adminViewModel.bannedDevices.collectAsState()
    val timeFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar"))
    var unbanTarget by remember { mutableStateOf<com.example.zainqhchat.domain.model.BannedDevice?>(null) }

    if (devices.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("لا توجد أجهزة محظورة نهائيًا", color = TextSecondaryMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices, key = { it.deviceId }) { device ->
                LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = StatusErrorRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                device.bannedUsername?.let { "@$it" } ?: "مستخدم محذوف",
                                color = TextPrimaryWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        val id = device.deviceId
                        val shortId = if (id.length > 10) id.take(6) + "…" + id.takeLast(4) else id
                        Text("معرّف الجهاز: $shortId", color = TextSecondaryMuted, fontSize = 12.sp)
                        if (!device.reason.isNullOrBlank()) {
                            Text("السبب: ${device.reason}", color = TextSecondaryMuted, fontSize = 12.sp)
                        }
                        Text(timeFormatter.format(Date(device.bannedAtMillis)), color = TextSecondaryMuted, fontSize = 11.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { unbanTarget = device }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusOnlineGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فك الحظر النهائي", color = StatusOnlineGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    val target = unbanTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { unbanTarget = null },
            containerColor = LuxurySurfaceDark,
            title = { Text("فك الحظر النهائي", color = GoldPrimary) },
            text = {
                Text(
                    "سيتمكن هذا الجهاز من إنشاء حساب جديد مجددًا. لا يمكن استعادة الحساب المحذوف.",
                    color = TextPrimaryWhite
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    adminViewModel.unbanDevice(target.deviceId)
                    unbanTarget = null
                }) { Text("فك الحظر", color = StatusOnlineGreen) }
            },
            dismissButton = {
                TextButton(onClick = { unbanTarget = null }) { Text("إلغاء", color = TextSecondaryMuted) }
            }
        )
    }
}

@Composable
private fun AdminModerationTab(adminViewModel: AdminViewModel, onOpenProfile: (String) -> Unit) {
    val messages by adminViewModel.moderationMessages.collectAsState()
    var pendingDelete by remember { mutableStateOf<ChatMessage?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                    UserAvatar(
                        name = message.senderName,
                        avatarUrl = message.senderAvatarUrl,
                        size = 40.dp,
                        showOnlineIndicator = false,
                        modifier = Modifier.clickable { onOpenProfile(message.senderId) }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(message.senderName, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (message.text.isNotBlank()) {
                            Text(message.text, color = TextSecondaryMuted, fontSize = 13.sp)
                        }
                        if (!message.imageUrl.isNullOrBlank()) {
                            AdminImageThumbnail(message.imageUrl!!)
                        }
                    }
                    IconButton(onClick = { pendingDelete = message }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = StatusErrorRed)
                    }
                }
            }
        }
    }

    val target = pendingDelete
    if (target != null) {
        var reason by remember(target.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = LuxurySurfaceDark,
            title = { Text("حذف الرسالة نهائيًا؟", color = StatusErrorRed, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("سيُحذف نص الرسالة والصورة المرفقة (إن وُجدت) نهائيًا.", color = TextSecondaryMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("سبب الحذف (اختياري)", color = TextSecondaryMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    adminViewModel.deleteMessage(target.id, target.imageUrl, reason.ifBlank { null })
                    pendingDelete = null
                }) {
                    Text("حذف نهائيًا", color = StatusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("إلغاء", color = TextSecondaryMuted) }
            }
        )
    }
}

@Composable
private fun AdminImageThumbnail(imageUrl: String) {
    Box(
        modifier = Modifier
            .padding(top = 6.dp)
            .size(90.dp)
            .clip(RoundedCornerShape(10.dp))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "صورة الرسالة",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AdminUsersTab(adminViewModel: AdminViewModel, onOpenProfile: (String) -> Unit) {
    val users by adminViewModel.users.collectAsState()
    var query by remember { mutableStateOf("") }
    var pointsTarget by remember { mutableStateOf<User?>(null) }
    var avatarRemovalTarget by remember { mutableStateOf<User?>(null) }
    var tempBanTarget by remember { mutableStateOf<User?>(null) }
    var banUserTarget by remember { mutableStateOf<User?>(null) }
    var currencyTarget by remember { mutableStateOf<User?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                adminViewModel.searchUsers(it.ifBlank { null })
            },
            placeholder = { Text("ابحث بالاسم أو اسم المستخدم...", color = TextSecondaryMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = LuxuryBorderGold,
                focusedTextColor = TextPrimaryWhite,
                unfocusedTextColor = TextPrimaryWhite
            ),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users, key = { it.id }) { user ->
                LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(
                                name = user.name,
                                avatarUrl = user.avatarUrl,
                                size = 44.dp,
                                isOnline = user.isReallyOnline(),
                                modifier = Modifier.clickable { onOpenProfile(user.id) }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("@${user.username} • ${user.points} نقطة", color = TextSecondaryMuted, fontSize = 11.sp)
                                if (user.isBanned) {
                                    Text("محظور 🚫", color = StatusErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { banUserTarget = user }) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    tint = StatusErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حظر وحذف نهائي", color = StatusErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = {
                                if (user.isBanned) {
                                    // إلغاء الحظر (المؤقت) إجراء غير مدمّر — فوري بلا نافذة تأكيد.
                                    adminViewModel.setBanStatus(user.id, false, null)
                                } else {
                                    tempBanTarget = user
                                }
                            }) {
                                Text(
                                    if (user.isBanned) "إلغاء الحظر" else "حظر مؤقت",
                                    color = if (user.isBanned) StatusOnlineGreen else StatusErrorRed,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = { pointsTarget = user }) {
                                Text("النقاط", color = GoldPrimary, fontSize = 12.sp)
                            }
                            TextButton(onClick = { currencyTarget = user }) {
                                Text("العملات/الألماس", color = GoldPrimary, fontSize = 12.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!user.avatarUrl.isNullOrBlank()) {
                                TextButton(onClick = { avatarRemovalTarget = user }) {
                                    Icon(Icons.Default.PersonOff, contentDescription = null, tint = StatusErrorRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إزالة الصورة", color = StatusErrorRed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val pTarget = pointsTarget
    if (pTarget != null) {
        var amountText by remember(pTarget.id) { mutableStateOf("") }
        var reason by remember(pTarget.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pointsTarget = null },
            containerColor = LuxurySurfaceDark,
            title = { Text("تعديل نقاط ${pTarget.name}", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("رصيد حالي: ${pTarget.points} نقطة", color = TextSecondaryMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("عدد النقاط (سالب للخصم، موجب للإضافة)", color = TextSecondaryMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("السبب (مثال: مكافأة نشاط)", color = TextSecondaryMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val delta = amountText.toIntOrNull()
                    if (delta != null && delta != 0) {
                        adminViewModel.adjustPoints(pTarget.id, delta, reason.ifBlank { null })
                    }
                    pointsTarget = null
                }) {
                    Text("تطبيق", color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pointsTarget = null }) { Text("إلغاء", color = TextSecondaryMuted) }
            }
        )
    }

    val aTarget = avatarRemovalTarget
    if (aTarget != null) {
        AlertDialog(
            onDismissRequest = { avatarRemovalTarget = null },
            containerColor = LuxurySurfaceDark,
            title = { Text("إزالة صورة ${aTarget.name}؟", color = StatusErrorRed, fontWeight = FontWeight.Bold) },
            text = { Text("سيعود المستخدم للصورة الافتراضية.", color = TextSecondaryMuted, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    adminViewModel.removeAvatar(aTarget.id, aTarget.avatarUrl, null)
                    avatarRemovalTarget = null
                }) {
                    Text("إزالة", color = StatusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { avatarRemovalTarget = null }) { Text("إلغاء", color = TextSecondaryMuted) }
            }
        )
    }

    val tbTarget = tempBanTarget
    if (tbTarget != null) {
        var hoursText by remember(tbTarget.id) { mutableStateOf("24") }
        var reason by remember(tbTarget.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { tempBanTarget = null },
            containerColor = LuxurySurfaceDark,
            title = { Text("حظر مؤقت لـ ${tbTarget.name}", color = StatusErrorRed, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it },
                        label = { Text("عدد الساعات", color = TextSecondaryMuted) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("السبب (اختياري)", color = TextSecondaryMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val hours = hoursText.toIntOrNull()
                    if (hours != null && hours > 0) {
                        adminViewModel.setTempBan(tbTarget.id, hours, reason.ifBlank { null })
                    }
                    tempBanTarget = null
                }) {
                    Text("تطبيق", color = StatusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tempBanTarget = null }) { Text("إلغاء", color = TextSecondaryMuted) }
            }
        )
    }

    val banTarget = banUserTarget
    if (banTarget != null) {
        AlertDialog(
            onDismissRequest = { banUserTarget = null },
            containerColor = LuxurySurfaceDark,
            title = { Text("حظر ${banTarget.name} نهائيًا؟", color = StatusErrorRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "سيُحذف الحساب وكل بياناته نهائيًا فور الحظر (تمامًا كالحذف الكامل)، " +
                        "كما لن يستطيع هذا الشخص إنشاء حساب جديد من نفس الجهاز مطلقًا حتى " +
                        "لو حذف التطبيق وأعاد تثبيته. لا يمكن التراجع عن هذا الإجراء.",
                    color = TextSecondaryMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    adminViewModel.setBanStatus(banTarget.id, true, null)
                    banUserTarget = null
                }) {
                    Text("حظر وحذف نهائيًا", color = StatusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { banUserTarget = null }) { Text("إلغاء", color = TextSecondaryMuted) }
            }
        )
    }

    val cTarget = currencyTarget
    if (cTarget != null) {
        var coinsText by remember(cTarget.id) { mutableStateOf("") }
        var diamondsText by remember(cTarget.id) { mutableStateOf("") }
        var reason by remember(cTarget.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { currencyTarget = null },
            containerColor = LuxurySurfaceDark,
            title = { Text("تعديل رصيد ${cTarget.name}", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "الرصيد الحالي: ${cTarget.coins} عملة، ${cTarget.diamonds} ألماسة",
                        color = TextSecondaryMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = coinsText,
                        onValueChange = { coinsText = it },
                        label = { Text("تغيير العملات (سالب للخصم)", color = TextSecondaryMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = diamondsText,
                        onValueChange = { diamondsText = it },
                        label = { Text("تغيير الألماس (سالب للخصم)", color = TextSecondaryMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("السبب (اختياري)", color = TextSecondaryMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val coinsDelta = coinsText.toLongOrNull() ?: 0L
                    val diamondsDelta = diamondsText.toLongOrNull() ?: 0L
                    if (coinsDelta != 0L || diamondsDelta != 0L) {
                        adminViewModel.adjustCurrency(cTarget.id, coinsDelta, diamondsDelta, reason.ifBlank { null })
                    }
                    currencyTarget = null
                }) {
                    Text("تطبيق", color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { currencyTarget = null }) { Text("إلغاء", color = TextSecondaryMuted) }
            }
        )
    }
}

@Composable
private fun AdminActionLogTab(adminViewModel: AdminViewModel) {
    val log by adminViewModel.actionLog.collectAsState()
    val timeFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar"))

    if (log.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.History, contentDescription = null, tint = TextSecondaryMuted)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(log, key = { it.id }) { entry ->
            LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(describeAction(entry), color = TextPrimaryWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (!entry.reason.isNullOrBlank()) {
                        Text("السبب: ${entry.reason}", color = TextSecondaryMuted, fontSize = 12.sp)
                    }
                    Text(timeFormatter.format(Date(entry.createdAtMillis)), color = TextSecondaryMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminBroadcastTab(adminViewModel: AdminViewModel) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "أرسل إشعارًا فوريًا حقيقيًا لجميع المستخدمين دفعة واحدة عبر نظام الإشعارات الحالي.",
            color = TextSecondaryMuted,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("عنوان الإشعار", color = TextSecondaryMuted) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = LuxuryBorderGold,
                focusedTextColor = TextPrimaryWhite,
                unfocusedTextColor = TextPrimaryWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("نص الإشعار (اختياري)", color = TextSecondaryMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = LuxuryBorderGold,
                focusedTextColor = TextPrimaryWhite,
                unfocusedTextColor = TextPrimaryWhite
            ),
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        com.example.zainqhchat.ui.components.GoldButton(
            text = "إرسال للجميع 📢",
            enabled = title.isNotBlank(),
            onClick = { showConfirm = true }
        )
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = LuxurySurfaceDark,
            title = { Text("تأكيد الإرسال الجماعي", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("سيصل هذا الإشعار لجميع مستخدمي التطبيق. متابعة؟", color = TextSecondaryMuted, fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = {
                    adminViewModel.broadcastNotification(title.trim(), body.trim().ifBlank { null })
                    showConfirm = false
                    title = ""
                    body = ""
                }) {
                    Text("إرسال", color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("إلغاء", color = TextSecondaryMuted) }
            }
        )
    }
}

/** أسماء ووصف عربي ودود لكل مفتاح قسم تقني — للعرض فقط داخل لوحة الإدارة. */
private fun sectionDisplayName(key: String): String = when (key) {
    "chats" -> "الدردشات"
    "currency" -> "العملات"
    "contact_admin" -> "تواصل مع المدير"
    "online_users" -> "المتصلون الآن"
    "public_chat_link" -> "الدردشة العامة (الرابط الخارجي)"
    "settings" -> "الإعدادات"
    else -> key
}

@Composable
private fun AdminFeatureFlagsTab(adminViewModel: AdminViewModel) {
    val flags by adminViewModel.featureFlags.collectAsState()
    var reasonDialogFor by remember { mutableStateOf<String?>(null) }
    var reasonText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { adminViewModel.loadFeatureFlags() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                "أغلق أي قسم مؤقتًا (لصيانة أو ظرف طارئ) — يظهر التغيير فورًا لكل " +
                    "المستخدمين، بمن فيهم الزوّار، عند فتحهم الصفحة الرئيسية التالية.",
                color = TextSecondaryMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        val knownKeys = listOf("chats", "currency", "contact_admin", "online_users", "public_chat_link", "settings")
        items(knownKeys) { key ->
            val flag = flags.find { it.sectionKey == key }
            val isEnabled = flag?.isEnabled ?: true

            Surface(
                color = LuxurySurfaceDark,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sectionDisplayName(key), color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (!isEnabled && !flag?.disabledReason.isNullOrBlank()) {
                                Text(
                                    "مغلق: ${flag?.disabledReason}",
                                    color = StatusErrorRed,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { newValue ->
                                if (newValue) {
                                    adminViewModel.setFeatureFlag(key, true, null)
                                } else {
                                    reasonText = ""
                                    reasonDialogFor = key
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = GoldPrimary)
                        )
                    }
                }
            }
        }
    }

    val closingKey = reasonDialogFor
    if (closingKey != null) {
        AlertDialog(
            onDismissRequest = { reasonDialogFor = null },
            containerColor = LuxurySurfaceDark,
            title = { Text("إغلاق \"${sectionDisplayName(closingKey)}\" مؤقتًا", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "سيظهر هذا السبب للمستخدمين عند محاولتهم فتح هذا القسم (اختياري).",
                        color = TextSecondaryMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("سبب الإغلاق (اختياري)", color = TextSecondaryMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    adminViewModel.setFeatureFlag(closingKey, false, reasonText.trim().ifBlank { null })
                    reasonDialogFor = null
                }) {
                    Text("إغلاق القسم", color = StatusErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reasonDialogFor = null }) { Text("إلغاء", color = TextSecondaryMuted) }
            }
        )
    }
}

private fun describeAction(entry: AdminActionLogEntry): String = when (entry.actionType) {
    AdminActionType.BAN_USER -> "حظر مستخدم (دائم)"
    AdminActionType.UNBAN_USER -> "فك حظر مستخدم"
    AdminActionType.TEMP_BAN_USER -> "حظر مؤقت — ${entry.reason ?: ""}"
    AdminActionType.DELETE_MESSAGE -> "حذف رسالة عامة"
    AdminActionType.REMOVE_AVATAR -> "إزالة صورة شخصية"
    AdminActionType.DELETE_USER -> "حذف مستخدم نهائيًا"
    AdminActionType.BROADCAST_NOTIFICATION -> "إشعار جماعي: ${entry.reason ?: ""}"
    AdminActionType.ADJUST_POINTS -> {
        val delta = entry.pointsDelta ?: 0
        if (delta >= 0) "إضافة $delta نقطة" else "خصم ${-delta} نقطة"
    }
    AdminActionType.ADJUST_CURRENCY -> {
        val coins = entry.coinsDelta ?: 0L
        val diamonds = entry.diamondsDelta ?: 0L
        buildList {
            if (coins != 0L) add(if (coins > 0) "+$coins عملة" else "$coins عملة")
            if (diamonds != 0L) add(if (diamonds > 0) "+$diamonds ألماسة" else "$diamonds ألماسة")
        }.joinToString(" ، ").ifBlank { "تعديل رصيد" }
    }
}
