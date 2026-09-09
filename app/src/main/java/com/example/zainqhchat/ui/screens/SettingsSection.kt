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
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. بطاقة الملف الشخصي وتعديله
        item {
            LuxuryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_profile_card"),
                onClick = { showEditProfileDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        name = currentUser?.name ?: "Zain",
                        avatarUrl = currentUser?.avatarUrl,
                        size = 58.dp,
                        isOnline = true,
                        tier = currentUser?.tier
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.name ?: "مستخدم قروب بنات و شباب",
                            color = TextPrimaryWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "@${currentUser?.username ?: "username"}",
                            color = TextSecondaryMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اضغط لتعديل الملف الشخصي ✏️",
                            color = GoldPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 1أ. الأمان — إضافة/تغيير كلمة المرور
        item {
            LuxuryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security_card"),
                onClick = { showSetPasswordDialog = true }
            ) {
                SettingsRowItem(
                    icon = Icons.Default.Lock,
                    title = "الأمان — إضافة/تغيير كلمة المرور",
                    onClick = { showSetPasswordDialog = true },
                    testTag = "set_password_btn"
                )
            }
        }

        // 1ب. تواصل مع المدير — محادثة مباشرة، متاحة لأي مستخدم من هنا.
        item {
            LuxuryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_admin_card"),
                onClick = onContactAdmin
            ) {
                SettingsRowItem(
                    icon = Icons.Default.SupportAgent,
                    title = "تواصل مع المدير 💬",
                    onClick = onContactAdmin,
                    testTag = "contact_admin_btn"
                )
            }
        }

        // 2. إعدادات المظهر والوضع الداكن/الفاتح
        item {
            Text(
                text = "المظهر والألوان 🎨",
                color = GoldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // الوضع الداكن
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("الوضع الداكن الفاخر", color = TextPrimaryWhite, fontSize = 15.sp)
                        }
                        Switch(
                            checked = settingsState.isDarkMode,
                            onCheckedChange = { settingsViewModel.toggleDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LuxuryBlackBg,
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
            Text(
                text = "المعلومات والسياسات 📜",
                color = GoldPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SettingsRowItem(
                        icon = Icons.Default.PrivacyTip,
                        title = "سياسة الخصوصية",
                        onClick = { showPrivacyPolicyDialog = true },
                        testTag = "privacy_policy_btn"
                    )

                    Divider(color = LuxuryBorderGold.copy(alpha = 0.3f))

                    SettingsRowItem(
                        icon = Icons.Default.Description,
                        title = "الشروط والأحكام",
                        onClick = { showTermsDialog = true },
                        testTag = "terms_conditions_btn"
                    )

                    Divider(color = LuxuryBorderGold.copy(alpha = 0.3f))

                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "عن تطبيق قروب بنات و شباب",
                        onClick = { showAboutDialog = true },
                        testTag = "about_app_btn"
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
                Spacer(modifier = Modifier.height(4.dp))
                LuxuryCard(
                    modifier = Modifier.fillMaxWidth().testTag("open_admin_btn"),
                    onClick = onOpenAdmin
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛡️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("لوحة الإدارة", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("إدارة المستخدمين ومراقبة الدردشة العامة", color = TextSecondaryMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 4. تسجيل الخروج
        item {
            Spacer(modifier = Modifier.height(8.dp))
            GoldButton(
                text = "تسجيل الخروج 🚪",
                onClick = onLogout,
                testTag = "logout_btn"
            )
        }

        // 5. حذف الحساب نهائيًا (إجراء مدمِّر — منفصل بوضوح عن تسجيل الخروج)
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = LuxuryBorderGold.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "منطقة الخطر",
                color = Color(0xFFFF5252),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDeleteAccountDialog = true },
                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("delete_account_btn")
            ) {
                Text("حذف الحساب نهائيًا 🗑️", fontWeight = FontWeight.Bold)
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
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = GoldPrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, color = TextPrimaryWhite, fontSize = 15.sp)
        }
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
                                        val bytes = context.contentResolver
                                            .openInputStream(uriToUpload)?.use { it.readBytes() }
                                        val mimeType = context.contentResolver.getType(uriToUpload) ?: "image/jpeg"
                                        bytes?.let {
                                            com.example.zainqhchat.domain.model.NewAvatar(it, mimeType)
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

                        "٨) عدم مشاركة البيانات\n" +
                        "لا نبيع بياناتك ولا نشاركها مع أي طرف ثالث لأغراض تسويقية.\n\n" +

                        "للتواصل بخصوص خصوصيتك أو طلب حذف بياناتك، استخدم خيار حذف الحساب " +
                        "داخل التطبيق مباشرة.",
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
                        "يجب أن يكون عمرك 18 سنة فأكثر لاستخدام هذا التطبيق. التطبيق يتضمن " +
                        "ميزات تعارف وتواصل بين المستخدمين، ولا يُسمح إطلاقًا لمن هم دون 18 " +
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
