package com.example.zainqhchat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.model.formatLastSeen
import com.example.zainqhchat.domain.model.isReallyOnline
import com.example.zainqhchat.ui.components.ChatBubble
import com.example.zainqhchat.ui.components.UserAvatar
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.StatusErrorRed
import com.example.zainqhchat.ui.theme.StatusOnlineGreen
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.ChatViewModel
import com.example.zainqhchat.ui.viewmodels.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * شاشة الدردشة — أُعيد تصميمها بالكامل بتخطيط Column بسيط (بدلاً من
 * Scaffold + bottomBar) كي يبقى مربع الكتابة دائمًا ملتصقًا بأسفل الشاشة
 * ويتحرك مع لوحة المفاتيح بشكل طبيعي ومتوقع، بلا أي منطق تخطيط إضافي معقّد
 * قد يتعارض مع IME. `Modifier.imePadding()` مطبَّق مرة واحدة فقط على أعلى
 * عنصر جذر، فيرتفع الشريط السفلي معه تلقائيًا كوحدة واحدة سلسة.
 */
@Composable
fun ChatDetailScreen(
    currentUser: User,
    targetUserId: String?, // null للدردشة العامة
    targetUserName: String?,
    chatViewModel: ChatViewModel,
    userViewModel: UserViewModel,
    onBackClick: () -> Unit,
    onOpenProfileClick: (userId: String) -> Unit
) {
    val isPublicChat = targetUserId == null
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var messageContextMenuTarget by remember { mutableStateOf<com.example.zainqhchat.domain.model.ChatMessage?>(null) }
    var messageToReport by remember { mutableStateOf<com.example.zainqhchat.domain.model.ChatMessage?>(null) }
    var reportReasonText by remember { mutableStateOf("") }

    val replyingTo by chatViewModel.replyingTo.collectAsState()
    val messageActionResult by chatViewModel.messageActionResult.collectAsState()
    LaunchedEffect(messageActionResult) {
        messageActionResult?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            chatViewModel.clearMessageActionResult()
        }
    }

    val isUploadingImage by chatViewModel.isUploadingImage.collectAsState()
    val imageUploadError by chatViewModel.imageUploadError.collectAsState()

    // منتقي صور حقيقي من معرض الجهاز عبر Photo Picker الرسمي لأندرويد —
    // لا يحتاج أي إذن وصول للتخزين إطلاقًا (توصية Google الحالية).
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) attachedImageUri = uri
    }

    // جلب الرسائل مع التذكر لمنع إعادة التحميل التكراري والوميض
    val messagesFlow = remember(isPublicChat, targetUserId, currentUser.id) {
        if (isPublicChat) {
            chatViewModel.publicMessages
        } else {
            chatViewModel.getDirectMessages(targetUserId!!)
        }
    }
    val messages by messagesFlow.collectAsState()

    // جلب معلومات الطرف الآخر في المحادثة الخاصة مع التذكر
    val targetUserFlow = remember(isPublicChat, targetUserId, currentUser.id) {
        if (!isPublicChat && targetUserId != null) {
            userViewModel.getUserProfile(targetUserId, currentUser.id)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }
    val targetUser by targetUserFlow.collectAsState(initial = null)

    val pointsNotification by chatViewModel.pointsAwardedNotification.collectAsState()

    // التمرير التلقائي للرسالة الأخيرة
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // إعادة التمرير عند فتح/إغلاق لوحة المفاتيح حتى لا يغطي مربع الكتابة آخر رسالة
    val imeBottomPx = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeBottomPx > 0) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // وضع علامة "تمت القراءة"
    LaunchedEffect(targetUserId) {
        if (!isPublicChat) {
            chatViewModel.markAsRead(currentUser.id, targetUserId!!)
        }
    }

    // التلاشي التلقائي لنص النقاط
    LaunchedEffect(pointsNotification) {
        if (pointsNotification != null) {
            delay(2500)
            chatViewModel.clearPointsNotification()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlackBg)
            .imePadding()
    ) {
        // ============================ الشريط العلوي ============================
        Surface(
            color = LuxurySurfaceCard,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("chat_detail_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = TextPrimaryWhite
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (isPublicChat) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "الدردشة العامة 🌐",
                            color = TextPrimaryWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "مجتمع قروب بنات و شباب الرسمي",
                            color = TextSecondaryMuted,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    val reallyOnline = targetUser?.isReallyOnline() ?: false
                    UserAvatar(
                        name = targetUserName ?: targetUser?.name ?: "مستخدم",
                        avatarUrl = targetUser?.avatarUrl,
                        size = 42.dp,
                        isOnline = reallyOnline,
                        tier = targetUser?.tier,
                        modifier = Modifier.clickable {
                            if (targetUserId != null) onOpenProfileClick(targetUserId)
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.clickable {
                            if (targetUserId != null) onOpenProfileClick(targetUserId)
                        }
                    ) {
                        Text(
                            text = targetUserName ?: targetUser?.name ?: "مستخدم",
                            color = TextPrimaryWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (reallyOnline) {
                                    "متصل الآن 🟢"
                                } else {
                                    targetUser?.let { formatLastSeen(it.lastActiveTimestamp) } ?: "غير متصل ⚪"
                                },
                                color = if (reallyOnline) StatusOnlineGreen else TextSecondaryMuted,
                                fontSize = 11.sp
                            )
                            if (targetUser?.tier != null && targetUser?.tier?.badgeSymbol?.isNotBlank() == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${targetUser?.tier?.titleArabic} ${targetUser?.tier?.badgeSymbol}",
                                    color = GoldPrimary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // إشعار النقاط المكتسبة
        AnimatedVisibility(
            visible = pointsNotification != null,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldPrimary)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pointsNotification ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // ============================ قائمة الرسائل ============================
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                val isFromCurrent = message.senderId == currentUser.id
                ChatBubble(
                    message = message,
                    isFromCurrentUser = isFromCurrent,
                    modifier = Modifier.animateItem(),
                    onAvatarClick = {
                        if (!isFromCurrent) {
                            onOpenProfileClick(message.senderId)
                        }
                    },
                    onLongPress = { messageContextMenuTarget = message }
                )
            }
        }

        // ============================ شريط الإدخال السفلي ============================
        Surface(
            color = LuxurySurfaceCard,
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column {
                // شريط معاينة "الرد على" — يظهر فقط عند اختيار "رد" من قائمة الرسالة
                if (replyingTo != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LuxuryBorderGold.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ردًا على ${replyingTo!!.senderName}", color = GoldPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                replyingTo!!.text.take(60),
                                color = TextSecondaryMuted,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = { chatViewModel.clearReplyTarget() }) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء الرد", tint = TextSecondaryMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // معاينة الصورة المرفقة الحقيقية قبل الإرسال
                if (attachedImageUri != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            coil.compose.AsyncImage(
                                model = attachedImageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isUploadingImage) "جارٍ رفع الصورة..." else "صورة جاهزة للإرسال",
                            color = TextSecondaryMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = GoldPrimary
                            )
                        } else {
                            IconButton(onClick = { attachedImageUri = null }) {
                                Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = StatusErrorRed)
                            }
                        }
                    }
                }

                if (imageUploadError != null) {
                    Text(
                        text = imageUploadError ?: "",
                        color = StatusErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        enabled = !isUploadingImage,
                        modifier = Modifier.testTag("attach_image_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "إرفاق صورة",
                            tint = GoldPrimary
                        )
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("اكتب رسالتك هنا...", color = TextSecondaryMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = LuxuryBorderGold,
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite,
                            focusedContainerColor = LuxuryBlackBg,
                            unfocusedContainerColor = LuxuryBlackBg
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = (messageText.isNotBlank() || attachedImageUri != null) && !isUploadingImage
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (canSend) GoldPrimary else LuxuryBorderGold)
                            .clickable(enabled = canSend) {
                                val uriToSend = attachedImageUri
                                if (uriToSend != null) {
                                    coroutineScope.launch {
                                        val (bytes, mimeType) = withContext(Dispatchers.IO) {
                                            val mime = context.contentResolver.getType(uriToSend) ?: "image/jpeg"
                                            val data = context.contentResolver.openInputStream(uriToSend)?.use { it.readBytes() }
                                            data to mime
                                        }
                                        if (bytes != null) {
                                            chatViewModel.sendImageMessage(
                                                currentUser = currentUser,
                                                recipientId = targetUserId,
                                                imageBytes = bytes,
                                                mimeType = mimeType
                                            )
                                            attachedImageUri = null
                                        }
                                    }
                                } else if (messageText.isNotBlank()) {
                                    chatViewModel.sendMessage(
                                        currentUser = currentUser,
                                        recipientId = targetUserId,
                                        text = messageText
                                    )
                                    messageText = ""
                                }
                            }
                            .testTag("send_message_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "إرسال",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // قائمة خيارات الرسالة (رد، نسخ، إبلاغ، حذف) — تعمل فعليًا وليست شكلية.
    val menuTarget = messageContextMenuTarget
    if (menuTarget != null) {
        val isOwnMessage = menuTarget.senderId == currentUser.id
        AlertDialog(
            onDismissRequest = { messageContextMenuTarget = null },
            containerColor = LuxurySurfaceCard,
            title = { Text("خيارات الرسالة", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            chatViewModel.setReplyTarget(menuTarget)
                            messageContextMenuTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("رد", color = TextPrimaryWhite)
                        }
                    }
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("رسالة", menuTarget.text))
                            android.widget.Toast.makeText(context, "تم نسخ الرسالة", android.widget.Toast.LENGTH_SHORT).show()
                            messageContextMenuTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("نسخ", color = TextPrimaryWhite)
                        }
                    }
                    if (!isOwnMessage) {
                        TextButton(
                            onClick = {
                                messageToReport = menuTarget
                                messageContextMenuTarget = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("إبلاغ", color = TextPrimaryWhite)
                            }
                        }
                    }
                    if (isOwnMessage) {
                        TextButton(
                            onClick = {
                                chatViewModel.deleteMessage(menuTarget.id)
                                messageContextMenuTarget = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("حذف", color = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { messageContextMenuTarget = null }) { Text("إغلاق", color = TextSecondaryMuted) }
            }
        )
    }

    // نافذة كتابة سبب الإبلاغ
    val reportTarget = messageToReport
    if (reportTarget != null) {
        AlertDialog(
            onDismissRequest = { messageToReport = null; reportReasonText = "" },
            containerColor = LuxurySurfaceCard,
            title = { Text("الإبلاغ عن الرسالة", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("سيصل بلاغك مباشرة لإدارة التطبيق مع نص الرسالة.", color = TextSecondaryMuted, fontSize = 12.5.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reportReasonText,
                        onValueChange = { reportReasonText = it },
                        label = { Text("سبب الإبلاغ", color = TextSecondaryMuted) },
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
                    val reason = reportReasonText.ifBlank { "محتوى غير لائق" }
                    chatViewModel.reportMessage(currentUser.id, reportTarget, reason)
                    messageToReport = null
                    reportReasonText = ""
                }) {
                    Text("إرسال البلاغ", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { messageToReport = null; reportReasonText = "" }) {
                    Text("إلغاء", color = TextSecondaryMuted)
                }
            }
        )
    }
}
