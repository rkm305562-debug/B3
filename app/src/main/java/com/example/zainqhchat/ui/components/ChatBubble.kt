package com.example.zainqhchat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.ui.theme.GoldDark
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.LuxurySurfaceElevated
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ChatBubble(
    message: ChatMessage,
    isFromCurrentUser: Boolean,
    onAvatarClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale("ar"))
    val formattedTime = timeFormatter.format(Date(message.timestamp))
    var showFullScreenImage by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isFromCurrentUser) {
            UserAvatar(
                name = message.senderName,
                avatarUrl = message.senderAvatarUrl,
                size = 36.dp,
                showOnlineIndicator = false,
                tier = message.senderTier,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .then(
                        if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick)
                        else Modifier
                    )
            )
        }

        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // اسم المرسل والرمز للفئة في الدردشة العامة
            if (!isFromCurrentUser && message.isPublic) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 2.dp)
                        .then(
                            if (onAvatarClick != null) Modifier.clickable(onClick = onAvatarClick)
                            else Modifier
                        )
                ) {
                    Text(
                        text = message.senderName,
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (message.senderTier.badgeSymbol.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = message.senderTier.badgeSymbol,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // فقاعة الرسالة
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isFromCurrentUser) 18.dp else 4.dp,
                            bottomEnd = if (isFromCurrentUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isFromCurrentUser) GoldDark else LuxurySurfaceElevated
                    )
                    .border(
                        width = 0.8.dp,
                        color = if (isFromCurrentUser) GoldPrimary else LuxuryBorderGold,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isFromCurrentUser) 18.dp else 4.dp,
                            bottomEnd = if (isFromCurrentUser) 4.dp else 18.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .then(
                        if (onLongPress != null) {
                            Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)
                        } else {
                            Modifier
                        }
                    )
            ) {
                Column {
                    // إذا كانت الرسالة تحتوي على صورة — تُعرض كاملة بأبعادها
                    // الأصلية (بدون قص)، مع إمكانية الضغط لفتحها بالحجم الكامل.
                    if (!message.imageUrl.isNull_or_Blank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(message.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "صورة مَرفقة",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showFullScreenImage = true }
                                .padding(bottom = 6.dp)
                        )
                    }

                    if (message.text.isNotEmpty()) {
                        Text(
                            text = message.text,
                            color = if (isFromCurrentUser) LuxuryBlackBg else TextPrimaryWhite,
                            fontSize = 15.sp,
                            fontWeight = if (isFromCurrentUser) FontWeight.SemiBold else FontWeight.Normal,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            color = if (isFromCurrentUser) LuxuryBlackBg.copy(alpha = 0.7f) else TextSecondaryMuted,
                            fontSize = 10.sp
                        )
                        if (isFromCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = if (message.isRead) "تمت القراءة" else "تم الإرسال",
                                tint = if (message.isRead) Color(0xFF00E676) else LuxuryBlackBg.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFullScreenImage && !message.imageUrl.isNull_or_Blank()) {
        FullScreenImageViewer(
            imageUrl = message.imageUrl!!,
            onDismiss = { showFullScreenImage = false }
        )
    }
}

/**
 * عارض صور بالحجم الكامل — يفتح فوق الشاشة بالكامل عند الضغط على أي صورة
 * داخل الدردشة، مع الحفاظ على الأبعاد الأصلية للصورة (بدون قص).
 */
@Composable
private fun FullScreenImageViewer(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "صورة بالحجم الكامل",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    tint = Color.White
                )
            }
        }
    }
}

private fun String?.isNull_or_Blank(): Boolean = this == null || this.isBlank()
