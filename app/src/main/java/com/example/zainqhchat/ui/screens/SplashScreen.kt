package com.example.zainqhchat.ui.screens

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.zainqhchat.ui.theme.GoldMetallic
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import kotlinx.coroutines.delay

/**
 * شاشة البداية (Splash) — بطاقة زجاجية (Glassmorphism) في وسط الشاشة فوق
 * خلفية داكنة متدرّجة، بنفس روح التصميم المرجعي (HTML) الذي أرفقه المستخدم،
 * لكن بألوان التطبيق نفسها (الأزرق السماوي GoldPrimary/GoldMetallic بدل
 * الألوان الأصلية في المرجع). ثلاث نقاط نابضة بدل شريط تحميل، ونص
 * "جاري التحميل..." أسفلها — تمامًا كالمرجع.
 *
 * السلوك الوظيفي كما هو: تنتظر [minDurationMillis] كحد أدنى ثم تستدعي
 * [onSplashFinished] بحالة تسجيل الدخول.
 */
@Composable
fun SplashScreen(
    isLoggedIn: Boolean,
    onSplashFinished: (isLoggedIn: Boolean) -> Unit,
    minDurationMillis: Long = 2000
) {
    var cardVisible by remember { mutableStateOf(false) }

    val cardAlpha by animateFloatAsState(
        targetValue = if (cardVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "card_alpha"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (cardVisible) 1f else 0.92f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "card_scale"
    )

    LaunchedEffect(Unit) {
        cardVisible = true
        delay(minDurationMillis)
        onSplashFinished(isLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1526), LuxuryBlackBg, Color(0xFF05070C))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .scale(cardScale)
                .clip(RoundedCornerShape(32.dp))
                .background(TextPrimaryWhite.copy(alpha = 0.05f))
                .border(1.dp, TextPrimaryWhite.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                .padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "شعار دردشة توتة",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(150.dp)
                    .alpha(cardAlpha)
                    .clip(RoundedCornerShape(28.dp))
                    .border(2.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "دردشة توتة",
                color = GoldPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.alpha(cardAlpha)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PulsingDot(color = GoldMetallic, delayMillis = 0)
                PulsingDot(color = TextPrimaryWhite, delayMillis = 200)
                PulsingDot(color = GoldMetallic, delayMillis = 400)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "جاري التحميل...",
                color = TextSecondaryMuted,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(cardAlpha * 0.85f)
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color, delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis, StartOffsetType.Delay)
        ),
        label = "dot_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis, StartOffsetType.Delay)
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = Modifier
            .size(14.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}
