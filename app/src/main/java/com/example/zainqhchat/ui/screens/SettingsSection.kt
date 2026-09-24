package com.example.zainqhchat.ui.screens

import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.ui.components.GoldButton
import com.example.zainqhchat.ui.components.LuxuryCard
import com.example.zainqhchat.ui.components.UserAvatar
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.AuthViewModel
import com.example.zainqhchat.ui.viewmodels.SetPasswordUiState
import com.example.zainqhchat.ui.viewmodels.SettingsViewModel
import com.example.zainqhchat.ui.viewmodels.UserViewModel

@Composable
fun SettingsSection(
    currentUser: User?,
    settingsViewModel: SettingsViewModel,
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onDeleteAccount: (onResult: (Boolean) -> Unit) -> Unit,
    onOpenAdmin: () -> Unit,
    onContactAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsState by settingsViewModel.settingsState.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. بطاقة الملف الشخصي وتعديله
        item {
            SettingsProfileHero(
                currentUser = currentUser,
                onClick = { showEditProfileDialog = true }
            )
        }

        // 1أ + 1ب. الأمان (كلمة المرور) ثم التواصل مع المدير — مجموعة واحدة بنفس الترتيب.
        item {
            SettingsGroupSurface {
                Box(modifier = Modifier.testTag("security_card")) {
                    SettingsRowItem(
                        icon = Icons.Default.Lock,
                        title = "الأمان — إضافة/تغيير كلمة المرور",
                        onClick = { showSetPasswordDialog = true },
                        testTag = "set_password_btn",
                        tint = Color(0xFF8B5CF6)
                    )
                }
                SettingsRowDivider()
                Box(modifier = Modifier.testTag("contact_admin_card")) {
                    SettingsRowItem(
                        icon = Icons.Default.SupportAgent,
                        title = "تواصل مع المدير 💬",
                        onClick = onContactAdmin,
                        testTag = "contact_admin_btn",
                        tint = Color(0xFF22C55E)
                    )
                }
            }
        }

        // 2. إعدادات المظهر والوضع الداكن/الفاتح
        item {
            Column {
            SettingsGroupLabel("المظهر والألوان 🎨")
            SettingsGroupSurface {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconTile(icon = Icons.Default.DarkMode, tint = Color(0xFF6366F1))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        "الوضع الداكن الفاخر",
                        color = TextPrimaryWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settingsState.isDarkMode,
                        onCheckedChange = { settingsViewModel.toggleDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GoldPrimary
                        ),
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
            }
        }

        // 3. المعلومات وسياسة الخصوصية
        item {
            Column {
            SettingsGroupLabel("المعلومات والسياسات 📜")
            SettingsGroupSurface {
                SettingsRowItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "سياسة الخصوصية",
                    onClick = { showPrivacyPolicyDialog = true },
                    testTag = "privacy_policy_btn",
                    tint = Color(0xFF0EA5E9)
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.Description,
                    title = "الشروط والأحكام",
                    onClick = { showTermsDialog = true },
                    testTag = "terms_conditions_btn",
                    tint = Color(0xFFF59E0B)
                )
                SettingsRowDivider()
                SettingsRowItem(
                    icon = Icons.Default.Info,
                    title = "عن تطبيق قروب بنات و شباب",
                    onClick = { showAboutDialog = true },
                    testTag = "about_app_btn",
                    tint = Color(0xFF14B8A6)
                )
            }
            }
        }

        // 3ب. لوحة الإدارة — تظهر فقط إن كان المستخدم الحالي مديرًا فعليًا.
        // هذا الشرط للواجهة فقط؛ الحماية الحقيقية من محاولة أي مستخدم آخر
        // استدعاء عمليات الإدارة مباشرة تتم من جهة قاعدة البيانات (RLS/RPC)
        // بغض النظر عن ظهور هذا الزر أو عدمه.
        if (currentUser?.isAdmin == true) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(GoldPrimary.copy(alpha = 0.10f))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                        .clickable(onClick = onOpenAdmin)
                        .testTag("open_admin_btn")
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GoldPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛡️", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("لوحة الإدارة", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("إدارة المستخدمين ومراقبة الدردشة العامة", color = TextSecondaryMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = GoldPrimary)
                }
            }
        }

        // 4. تسجيل الخروج
        item {
            GoldButton(
                text = "تسجيل الخروج 🚪",
                onClick = onLogout,
                testTag = "logout_btn"
            )
        }

        // 5. حذف الحساب نهائيًا (إجراء مدمِّر — منفصل بوضوح عن تسجيل الخروج)
        item {
            val danger = Color(0xFFFF5252)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(danger.copy(alpha = 0.07f))
                    .border(1.dp, danger.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = danger, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "منطقة الخطر",
                        color = danger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "حذف الحساب إجراء نهائي ولا يمكن التراجع عنه.",
                    color = TextSecondaryMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showDeleteAccountDialog = true },
                    border = BorderStroke(1.dp, danger),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = danger),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("delete_account_btn")
                ) {
                    Text("حذف الحساب نهائيًا 🗑️", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // نافذة تأكيد حذف الحساب
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
            containerColor = LuxurySurfaceCard,
            title = {
                Text("حذف الحساب نهائيًا ⚠️", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "سيتم حذف حسابك وكل بياناتك نهائيًا: الملف الشخصي، الرسائل، " +
                            "المتابعات، النقاط، والإشعارات. لا يمكن التراجع عن هذا الإجراء إطلاقًا.",
                        color = TextSecondaryMuted,
                        fontSize = 13.sp
                    )
                    if (deleteAccountError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(deleteAccountError!!, color = Color(0xFFFF5252), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = {
                        isDeletingAccount = true
                        deleteAccountError = null
                        onDeleteAccount { success ->
                            isDeletingAccount = false
                            if (success) {
                                showDeleteAccountDialog = false
                            } else {
                                deleteAccountError = "تعذر حذف الحساب. تحقق من الاتصال بالإنترنت وحاول مجددًا."
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_account_btn")
                ) {
                    Text(
                        if (isDeletingAccount) "جارٍ الحذف..." else "نعم، احذف حسابي نهائيًا",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeletingAccount,
                    onClick = { showDeleteAccountDialog = false }
                ) {
                    Text("إلغاء", color = TextSecondaryMuted)
                }
            }
        )
    }

    // نافذة تعديل الملف الشخصي
    if (showEditProfileDialog && currentUser != null) {
        EditProfileDialog(
            currentUser = currentUser,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName, newAge, newAvatar ->
                userViewModel.updateProfile(currentUser.id, newName, newAge, newAvatar)
                showEditProfileDialog = false
            }
        )
    }

    // نافذة إضافة/تغيير كلمة المرور
    if (showSetPasswordDialog) {
        SetPasswordDialog(
            authViewModel = authViewModel,
            onDismiss = { showSetPasswordDialog = false }
        )
    }

    // نافذة سياسة الخصوصية
    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }

    if (showTermsDialog) {
        TermsAndConditionsDialog(onDismiss = { showTermsDialog = false })
    }

    // نافذة عن التطبيق
    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun SettingsProfileHero(currentUser: User?, onClick: () -> Unit) {
    val heroShape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(heroShape)
            .background(
                Brush.linearGradient(listOf(Color(0xFF0369A1), Color(0xFF0EA5E9)))
            )
            .drawBehind {
                // زخارف دائرية شفافة خلف المحتوى
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = 75.dp.toPx(),
                    center = Offset(x = size.width * 0.12f, y = 0f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = 55.dp.toPx(),
                    center = Offset(x = size.width * 0.92f, y = size.height)
                )
            }
            .clickable(onClick = onClick)
            .testTag("edit_profile_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = currentUser?.name ?: "Zain",
                avatarUrl = currentUser?.avatarUrl,
                size = 68.dp,
                isOnline = true,
                tier = currentUser?.tier
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentUser?.name ?: "مستخدم قروب بنات و شباب",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@${currentUser?.username ?: "username"}",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "اضغط لتعديل الملف الشخصي ✏️",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Row(
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GoldPrimary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = TextPrimaryWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun SettingsGroupSurface(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LuxurySurfaceCard)
            .border(1.dp, LuxuryBorderGold, shape),
        content = content
    )
}

@Composable
private fun SettingsIconTile(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SettingsRowDivider() {
    Divider(
        modifier = Modifier.padding(start = 68.dp),
        color = LuxuryBorderGold.copy(alpha = 0.6f)
    )
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    testTag: String,
    tint: Color = GoldPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconTile(icon = icon, tint = tint)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = TextPrimaryWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = TextSecondaryMuted)
    }
}

@Composable
fun EditProfileDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onSave: (newName: String, newAge: Int, avatar: com.example.zainqhchat.domain.model.AvatarUpdate) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var name by remember { mutableStateOf(currentUser.name) }

    // حالات ثلاث حقيقية للصورة: بلا تغيير / محذوفة / صورة جديدة تنتظر الرفع.
    var pickedAvatarUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var avatarRemoved by remember { mutableStateOf(false) }
    val previewAvatarUrl: String? = when {
        avatarRemoved -> null
        pickedAvatarUri != null -> pickedAvatarUri.toString()
        else -> currentUser.avatarUrl
    }

    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            pickedAvatarUri = uri
            avatarRemoved = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuxurySurfaceCard,
        title = {
            Text("تعديل الملف الشخصي ✏️", color = GoldPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الصورة الشخصية:", color = TextSecondaryMuted, fontSize = 12.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    UserAvatar(
                        name = name.ifEmpty { "U" },
                        avatarUrl = previewAvatarUrl,
                        size = 52.dp,
                        showOnlineIndicator = false
                    )

                    Column {
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            border = BorderStroke(1.dp, GoldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                        ) {
                            Text("تغيير الصورة 📷", fontSize = 12.sp)
                        }

                        if (previewAvatarUrl != null) {
                            TextButton(onClick = {
                                pickedAvatarUri = null
                                avatarRemoved = true
                            }) {
                                Text("إزالة 🗑️", color = Color(0xFFFF5252), fontSize = 11.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل", color = TextSecondaryMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = LuxuryBorderGold,
                        focusedTextColor = TextPrimaryWhite,
                        unfocusedTextColor = TextPrimaryWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // العمر لم يعد قابلاً للتعديل من الواجهة إطلاقًا — يبقى كما
                    // سُجِّل (18 افتراضيًا)، إذ أُلغي عرضه وتعديله بالكامل بناءً
                    // على طلب صريح. يظل موجودًا داخليًا فقط للامتثال لسياسة
                    // الحد الأدنى للعمر (راجع migration 005).
                    val age = currentUser.age
                    val uriToUpload = pickedAvatarUri
                    coroutineScope.launch {
                        val avatarUpdate = when {
                            uriToUpload != null -> {
                                val newAvatar = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val compressed = com.example.zainqhchat.core.util.ImageCompressor.compress(
                                            context,
                                            uriToUpload,
                                            com.example.zainqhchat.core.util.ImageCompressor.MAX_DIMENSION_AVATAR
                                        )
                                        if (compressed != null) {
                                            val (bytes, mimeType) = compressed
                                            com.example.zainqhchat.domain.model.NewAvatar(bytes, mimeType)
                                        } else {
                                            // فشل الضغط (نادر) -> نعود للطريقة القديمة (رفع الصورة كما هي).
                                            val bytes = context.contentResolver
                                                .openInputStream(uriToUpload)?.use { it.readBytes() }
                                            val mimeType = context.contentResolver.getType(uriToUpload) ?: "image/jpeg"
                                            bytes?.let {
                                                com.example.zainqhchat.domain.model.NewAvatar(it, mimeType)
                                            }
                                        }
                                    }.getOrNull()
                                }
                                if (newAvatar != null) {
                                    com.example.zainqhchat.domain.model.AvatarUpdate.New(newAvatar)
                                } else {
                                    com.example.zainqhchat.domain.model.AvatarUpdate.Unchanged
                                }
                            }
                            avatarRemoved -> com.example.zainqhchat.domain.model.AvatarUpdate.Removed
                            else -> com.example.zainqhchat.domain.model.AvatarUpdate.Unchanged
                        }
                        onSave(name, age, avatarUpdate)
                    }
                },
                modifier = Modifier.testTag("save_profile_btn")
            ) {
                Text("حفظ التغييرات 💾", color = GoldPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondaryMuted)
            }
        }
    )
}

@Composable
fun SetPasswordDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    val setPasswordState by authViewModel.setPasswordState.collectAsState()

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // إعادة الحالة لحالتها الأولية عند إغلاق النافذة كي لا تظهر رسالة نجاح
    // قديمة في المرة القادمة.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { authViewModel.clearSetPasswordState() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuxurySurfaceCard,
        title = {
            Text("إضافة كلمة مرور 🔒", color = GoldPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (setPasswordState is SetPasswordUiState.Success) {
                    Text(
                        text = "تم حفظ كلمة المرور بنجاح ✅ يمكنك الآن الدخول لحسابك من أي جهاز بهذا الاسم وكلمة المرور.",
                        color = TextPrimaryWhite,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                } else {
                    Text(
                        text = "أضف كلمة مرور لحسابك حتى تستطيع الدخول إليه لاحقًا من جهاز آخر " +
                            "أو بعد حذف التطبيق. بدون كلمة مرور، يبقى حسابك متاحًا على هذا " +
                            "الجهاز فقط.",
                        color = TextSecondaryMuted,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            validationError = null
                        },
                        label = { Text("كلمة المرور الجديدة", color = TextSecondaryMuted) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            androidx.compose.material3.IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = "تبديل الرؤية",
                                    tint = TextSecondaryMuted
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("new_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            validationError = null
                        },
                        label = { Text("تأكيد كلمة المرور", color = TextSecondaryMuted) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            androidx.compose.ui.text.input.PasswordVisualTransformation()
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_password_input")
                    )

                    val errorToShow = validationError
                        ?: (setPasswordState as? SetPasswordUiState.Error)?.message
                    if (errorToShow != null) {
                        Text(errorToShow, color = Color(0xFFFF5252), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (setPasswordState is SetPasswordUiState.Success) {
                TextButton(onClick = onDismiss) {
                    Text("تم", color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    onClick = {
                        when {
                            newPassword.length < 6 ->
                                validationError = "كلمة المرور يجب أن تكون 6 خانات على الأقل"
                            newPassword != confirmPassword ->
                                validationError = "كلمتا المرور غير متطابقتين"
                            else -> authViewModel.setPassword(newPassword)
                        }
                    },
                    enabled = setPasswordState !is SetPasswordUiState.Loading,
                    modifier = Modifier.testTag("confirm_set_password_btn")
                ) {
                    if (setPasswordState is SetPasswordUiState.Loading) {
                        CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(18.dp))
                    } else {
                        Text("حفظ", color = GoldPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (setPasswordState !is SetPasswordUiState.Success) {
                TextButton(onClick = onDismiss) {
                    Text("إلغاء", color = TextSecondaryMuted)
                }
            }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuxurySurfaceCard,
        title = {
            Text("سياسة الخصوصية 🛡️", color = GoldPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "سياسة الخصوصية — قروب بنات و شباب\n\n" +
                        "آخر تحديث: 2026\n\n" +
                        "يوضّح هذا المستند كيف يتعامل تطبيق \"قروب بنات و شباب\" مع بياناتك. " +
                        "التطبيق يعتمد بالكامل على منصة Supabase (خدمة بنية تحتية مستقلة " +
                        "تقدّم قاعدة بيانات ومصادقة وتخزين ملفات)، ولا يستخدم Firebase أو أي " +
                        "خدمة إعلانات/تحليلات من Google للبيانات الشخصية. لا توجد أي " +
                        "إعلانات في هذا التطبيق إطلاقًا.\n\n" +

                        "١) تسجيل الدخول وإنشاء الحساب\n" +
                        "يتم إنشاء حسابك وإدارة تسجيل دخولك عبر Supabase Auth باستخدام اسم " +
                        "مستخدم وكلمة مرور. تُخزَّن كلمة المرور بشكل مشفّر بالكامل من طرف " +
                        "Supabase ولا يمكن لأي طرف — بما فيه فريق التطبيق — الاطلاع عليها " +
                        "كنص صريح.\n\n" +

                        "٢) البيانات التي نجمعها\n" +
                        "الاسم الكامل، اسم المستخدم، العمر، الصورة الشخصية (اختيارية)، " +
                        "الرسائل التي ترسلها (نصية أو صور)، قائمة من تتابعهم/يتابعونك، " +
                        "رصيد نقاطك داخل التطبيق، وحالة اتصالك (متصل الآن / آخر ظهور).\n\n" +

                        "٣) الرسائل والصور\n" +
                        "الرسائل الخاصة تُخزَّن بشكل يسمح لطرفي المحادثة فقط بقراءتها " +
                        "(محكومة بسياسات وصول صارمة على مستوى قاعدة البيانات). الدردشة " +
                        "العامة مرئية لجميع المستخدمين المسجَّلين. الصور الشخصية وصور " +
                        "المحادثات تُرفَع وتُخزَّن في مساحة تخزين ملفات (Supabase Storage) " +
                        "مخصصة لهذا الغرض.\n\n" +

                        "٤) طريقة تخزين البيانات\n" +
                        "تُخزَّن جميع بياناتك على خوادم Supabase (بنية تحتية سحابية مستقلة " +
                        "عن Google/Firebase)، مع تفعيل التشفير أثناء النقل بين التطبيق " +
                        "والخادم (HTTPS/WSS)، وسياسات وصول تمنع أي مستخدم من الاطلاع على " +
                        "بيانات مستخدم آخر لا يجب أن يراها.\n\n" +

                        "٥) الحد الأدنى للعمر\n" +
                        "هذا التطبيق مخصص فقط لمن هم 18 عامًا فأكثر. لا نجمع بيانات عن " +
                        "قصد من أي شخص دون هذا السن، وإن اكتشفنا حسابًا لمستخدم دون 18 " +
                        "عامًا فسيُحذف فورًا مع كل بياناته المرتبطة.\n\n" +

                        "٦) حقوقك\n" +
                        "يحق لك في أي وقت: تعديل ملفك الشخصي (الاسم، الصورة)، " +
                        "حظر أو الإبلاغ عن أي مستخدم، إلغاء متابعة أي شخص، وتسجيل الخروج.\n\n" +

                        "٧) حذف الحساب\n" +
                        "يمكنك حذف حسابك نهائيًا من داخل التطبيق (الإعدادات ← منطقة الخطر). " +
                        "هذا يحذف ملفك الشخصي وكل بياناته المرتبطة (رسائلك، متابعاتك، " +
                        "إشعاراتك، رصيد نقاطك) نهائيًا من قاعدة البيانات، ولا يمكن التراجع " +
                        "عنه.\n\n" +
                        "إذا لم يعد التطبيق مثبَّتًا على جهازك ولم تستطع الدخول لطلب الحذف من " +
                        "داخله، يمكنك طلب حذف حسابك وبياناتك من خارج التطبيق عبر هذه الصفحة:\n" +
                        "https://tabark17.blogspot.com/p/blog-page_10.html?m=1\n\n" +

                        "٨) عدم مشاركة البيانات\n" +
                        "لا نبيع بياناتك ولا نشاركها مع أي طرف ثالث لأغراض تسويقية.\n\n" +

                        "للتواصل بخصوص خصوصيتك أو طلب حذف بياناتك، استخدم خيار حذف الحساب " +
                        "داخل التطبيق مباشرة، أو رابط الطلب الخارجي أعلاه إذا لم يعد التطبيق " +
                        "مثبَّتًا لديك.",
                    color = TextPrimaryWhite,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("فهمت وموافق 👍", color = GoldPrimary)
            }
        }
    )
}

@Composable
fun TermsAndConditionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuxurySurfaceCard,
        title = {
            Text("الشروط والأحكام 📜", color = GoldPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "الشروط والأحكام — قروب بنات و شباب\n\n" +
                        "باستخدامك تطبيق \"قروب بنات و شباب\" أو إنشائك حسابًا فيه، فإنك تقرّ بأنك " +
                        "قرأت هذه الشروط ووافقت عليها بالكامل. إن كنت لا توافق على أي بند منها، " +
                        "يرجى عدم استخدام التطبيق.\n\n" +

                        "١) الحد الأدنى للعمر\n" +
                        "يجب أن يكون عمرك 18 عامًا فأكثر لإنشاء حساب واستخدام هذا التطبيق. " +
                        "التطبيق يتضمن ميزات تعارف وتواصل بين المستخدمين، ولا يُسمح إطلاقًا لمن هم دون 18 " +
                        "سنة بإنشاء حساب أو استخدام الخدمة. عند اكتشاف أي حساب لمستخدم دون " +
                        "هذا السن، يحق لإدارة التطبيق حذف الحساب فورًا ودون إشعار مسبق.\n\n" +

                        "٢) حذف الحساب\n" +
                        "يمكنك حذف حسابك نهائيًا في أي وقت من داخل التطبيق عبر الإعدادات " +
                        "(منطقة الخطر في أسفل صفحة الإعدادات)، وهو ما يوضحه أيضًا مستند " +
                        "سياسة الخصوصية. حذف الحساب نهائي ولا يمكن التراجع عنه، ويشمل جميع " +
                        "بياناتك ورسائلك ومتابعاتك ورصيدك داخل التطبيق.\n\n" +

                        "٣) الإبلاغ عن مستخدم أو محتوى مخالف\n" +
                        "إن واجهت مستخدمًا أو محتوى مخالفًا لهذه الشروط، استخدم زر \"إبلاغ\" " +
                        "المتوفر مباشرة في الملف الشخصي وفي كل رسالة دردشة، أو تواصل معنا " +
                        "مباشرة عبر قسم \"تواصل مع المدير\" المتاح من الشاشة الرئيسية " +
                        "والإعدادات. نراجع كل بلاغ ونتخذ الإجراء المناسب (تحذير، حظر مؤقت، " +
                        "حظر دائم، أو حذف الحساب) حسب خطورة المخالفة.\n\n" +

                        "٤) قواعد السلوك داخل التطبيق\n" +
                        "يُمنع منعًا باتًا:\n" +
                        "• التحرش أو التنمر أو الإساءة اللفظية لأي مستخدم آخر.\n" +
                        "• نشر أو إرسال أي محتوى إباحي أو جنسي صريح، أو محتوى يستغل أو " +
                        "يعرّض القُصَّر للخطر بأي شكل — وهذا يشمل حظرًا فوريًا ودائمًا وإبلاغ " +
                        "الجهات المختصة عند الاقتضاء.\n" +
                        "• الخطاب العنصري أو الطائفي أو خطاب الكراهية بجميع أشكاله.\n" +
                        "• انتحال شخصية شخص آخر أو إنشاء حسابات وهمية مضللة.\n" +
                        "• الاحتيال، النصب، أو طلب أموال/بيانات مالية من مستخدمين آخرين.\n" +
                        "• الترويج لمنتجات أو خدمات (سبام) دون إذن من إدارة التطبيق.\n" +
                        "• نشر معلومات خاصة بمستخدم آخر دون إذنه (Doxxing).\n" +
                        "• أي محتوى يخالف الآداب العامة أو القيم التي يقوم عليها التطبيق.\n" +
                        "• محاولة اختراق التطبيق أو التلاعب بأرصدة العملات/الألماس أو أي " +
                        "نظام داخلي بوسائل غير مشروعة.\n\n" +

                        "٥) صلاحيات الإشراف والإدارة\n" +
                        "تحتفظ إدارة التطبيق بالحق في مراجعة المحتوى المُبلَّغ عنه، وحذف أي " +
                        "رسالة أو صورة مخالفة، وإزالة الصور الشخصية غير اللائقة، وتطبيق حظر " +
                        "مؤقت أو دائم، أو حذف أي حساب يخالف هذه الشروط — كل ذلك دون الحاجة " +
                        "لإشعار مسبق في حالات المخالفات الجسيمة.\n\n" +

                        "٦) المحتوى الذي تنشره\n" +
                        "أنت المسؤول الوحيد عن أي محتوى (نص أو صورة) ترسله داخل التطبيق. " +
                        "بإرسالك محتوى عبر الدردشة العامة، فإنك تقرّ بأنه مناسب للعرض العام " +
                        "لجميع مستخدمي التطبيق.\n\n" +

                        "٧) نظام العملات والألماس\n" +
                        "العملات والألماس داخل التطبيق ذات قيمة افتراضية داخل التطبيق فقط، " +
                        "ولا تمثّل أي قيمة نقدية حقيقية، ولا يمكن استبدالها بأموال حقيقية أو " +
                        "سحبها خارج التطبيق.\n\n" +

                        "٨) التعديلات على الخدمة والشروط\n" +
                        "يحق لإدارة التطبيق تعديل هذه الشروط أو أي ميزة في الخدمة في أي وقت. " +
                        "استمرارك في استخدام التطبيق بعد أي تعديل يُعد موافقة ضمنية على " +
                        "الشروط المُحدَّثة.\n\n" +

                        "٩) إخلاء المسؤولية\n" +
                        "التطبيق منصة تواصل بين مستخدمين، ولا تتحمل إدارته مسؤولية تصرفات " +
                        "أي مستخدم خارج التطبيق أو أي اتفاق يتم التوصل إليه بين المستخدمين " +
                        "خارج نطاق الخدمة.\n\n" +

                        "للاطلاع على كيفية التعامل مع بياناتك، راجع سياسة الخصوصية.",
                    color = TextPrimaryWhite,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("فهمت 👍", color = GoldPrimary)
            }
        }
    )
}

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LuxurySurfaceCard,
        title = {
            Text("عن التطبيق ℹ️", color = GoldPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("قروب بنات و شباب", color = GoldPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("الإصدار: v1.0.0", color = TextSecondaryMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "قروب بنات و شباب هو تطبيق دردشة إسلامي عربي، صُمم ليكون مساحة تعارف " +
                        "وترفيه هادئة وآمنة تراعي القيم والآداب الإسلامية في التواصل. " +
                        "يتيح لك التطبيق التعرف على أشخاص جدد، والدردشة العامة مع " +
                        "المجتمع، وفتح محادثات خاصة، ومتابعة المستخدمين، مع الحفاظ على " +
                        "أجواء محترمة وخالية من أي محتوى غير لائق بفضل أدوات الإشراف " +
                        "المتوفرة في لوحة الإدارة. كما يوفر نظام نقاط وإنجازات لتحفيز " +
                        "التفاعل — كل ذلك ضمن تصميم عصري أنيق.",
                    color = TextPrimaryWhite,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = GoldPrimary)
            }
        }
    )
}
