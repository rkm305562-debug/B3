package com.example.zainqhchat.ui.screens.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.ui.components.GoldButton
import com.example.zainqhchat.ui.components.UserAvatar
import com.example.zainqhchat.ui.theme.GoldDark
import com.example.zainqhchat.ui.theme.GoldGradientEnd
import com.example.zainqhchat.ui.theme.GoldGradientStart
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.LuxurySurfaceDark
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.CurrencyUiState

/** يحوّل مفتاح الأيقونة النصي المخزَّن في قاعدة البيانات إلى ImageVector فعلي. */
fun currencyIconFor(key: String): ImageVector = when (key) {
    "directions_car" -> Icons.Default.DirectionsCar
    "sports_score" -> Icons.Default.SportsScore
    "bolt" -> Icons.Default.Bolt
    "local_florist" -> Icons.Default.LocalFlorist
    "favorite" -> Icons.Default.Favorite
    "workspace_premium" -> Icons.Default.WorkspacePremium
    "diamond" -> Icons.Default.Diamond
    else -> Icons.Default.CardGiftcard
}

/** شريط علوي موحّد لكل شاشات قسم العملات: رجوع + عنوان + صورة المستخدم + الإشعارات. */
@Composable
fun CurrencyTopBar(
    title: String,
    userAvatarUrl: String?,
    userName: String,
    onBackClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Surface(color = LuxurySurfaceDark, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Default.Notifications, contentDescription = "الإشعارات", tint = GoldPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.clickable(onClick = onAvatarClick)) {
                    UserAvatar(name = userName, avatarUrl = userAvatarUrl, size = 34.dp, showOnlineIndicator = false)
                }
            }

            Text(title, color = TextPrimaryWhite, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)

            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = GoldPrimary)
            }
        }
    }
}

/** بطاقة مربّعة لشبكة 2×2 — أيقونة داخل مربع أنيق بإضاءة زرقاء + عنوان + وصف قصير. */
@Composable
fun CurrencyGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(LuxurySurfaceCard.copy(alpha = 0.85f))
            .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(GoldGradientStart, GoldDark))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            title,
            color = TextPrimaryWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            subtitle,
            color = TextSecondaryMuted,
            fontSize = 10.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * غلاف عام لحالات التحميل/الخطأ/النجاح لأي قائمة في قسم العملات — رسائل
 * عربية واضحة وزر إعادة محاولة دائمًا بدل أي نص تقني.
 */
@Composable
fun <T> CurrencyStateContent(
    state: CurrencyUiState<T>,
    onRetry: () -> Unit,
    content: @Composable (T) -> Unit
) {
    when (state) {
        is CurrencyUiState.Loading -> {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        }
        is CurrencyUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.message, color = TextSecondaryMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(14.dp))
                GoldButton(text = "إعادة المحاولة 🔄", onClick = onRetry, modifier = Modifier.fillMaxWidth(0.6f))
            }
        }
        is CurrencyUiState.Success -> content(state.data)
    }
}
