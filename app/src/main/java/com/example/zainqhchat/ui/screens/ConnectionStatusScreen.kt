package com.example.zainqhchat.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifiStatusbarConnectedNoInternet4
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.core.network.ConnectionState
import com.example.zainqhchat.ui.theme.GoldChampagne
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.StatusErrorRed
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import kotlinx.coroutines.delay

/**
 * شاشة تفاعلية تظهر عند بدء تشغيل التطبيق إذا كان الإنترنت ضعيفًا أو
 * منقطعًا تمامًا. تُتيح للمستخدم إعادة المحاولة يدويًا، وتُعيد المحاولة
 * تلقائيًا كل بضع ثوانٍ، وتنتقل بنفسها بمجرد عودة الاتصال الجيد.
 *
 * [state] الحالة الحالية القادمة من NetworkMonitor.
 * [onRetryNow] يُستدعى عند ضغط المستخدم على زر "إعادة المحاولة".
 * [onConnected] يُستدعى تلقائيًا بمجرد أن تصبح الحالة CONNECTED.
 */
@Composable
fun ConnectionStatusScreen(
    state: ConnectionState,
    onRetryNow: () -> Unit,
    onConnected: () -> Unit
) {
    var isRetrying by remember { mutableStateOf(false) }
    var secondsUntilAutoRetry by remember { mutableIntStateOf(8) }

    // الانتقال التلقائي بمجرد عودة اتصال جيد.
    LaunchedEffect(state) {
        if (state == ConnectionState.CONNECTED) {
            onConnected()
        }
    }

    // عدّاد إعادة المحاولة التلقائية.
    LaunchedEffect(state) {
        if (state != ConnectionState.CONNECTED) {
            secondsUntilAutoRetry = 8
            while (secondsUntilAutoRetry > 0) {
                delay(1000)
                secondsUntilAutoRetry -= 1
            }
            isRetrying = true
            onRetryNow()
            delay(600)
            isRetrying = false
        }
    }

    val isFullyOffline = state == ConnectionState.DISCONNECTED

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LuxuryBlackBg, Color(0xFF121218), LuxuryBlackBg)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .then(
                        Modifier.background(
                            color = (if (isFullyOffline) StatusErrorRed else GoldPrimary).copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isFullyOffline) 100.dp else (100 * pulseScale).dp)
                        .background(
                            color = (if (isFullyOffline) StatusErrorRed else GoldPrimary).copy(alpha = 0.16f),
                            shape = CircleShape
                        )
                )
                Icon(
                    imageVector = if (isFullyOffline) Icons.Filled.WifiOff else Icons.Filled.SignalWifiStatusbarConnectedNoInternet4,
                    contentDescription = null,
                    tint = if (isFullyOffline) StatusErrorRed else GoldPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = if (isFullyOffline) "لا يوجد اتصال بالإنترنت" else "اتصال الإنترنت ضعيف",
                color = TextPrimaryWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isFullyOffline)
                    "تأكد من تفعيل الواي فاي أو بيانات الجوال، ثم أعد المحاولة."
                else
                    "قد يستغرق التطبيق وقتًا أطول من المعتاد في التحميل حتى تتحسن الشبكة.",
                color = TextSecondaryMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            RetryButton(
                isRetrying = isRetrying,
                onClick = {
                    isRetrying = true
                    onRetryNow()
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "ستتم إعادة المحاولة تلقائيًا خلال $secondsUntilAutoRetry ث",
                color = TextSecondaryMuted.copy(alpha = 0.8f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            ConnectionTipsCard()
        }
    }
}

@Composable
private fun RetryButton(isRetrying: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(enabled = !isRetrying) { onClick() }
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(GoldPrimary, GoldPrimary.copy(alpha = 0.75f))
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 28.dp, vertical = 14.dp)
    ) {
        if (isRetrying) {
            CircularProgressIndicator(
                color = LuxuryBlackBg,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = LuxuryBlackBg,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isRetrying) "جارِ إعادة المحاولة..." else "إعادة المحاولة الآن",
            color = LuxuryBlackBg,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ConnectionTipsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = LuxurySurfaceCard, shape = RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = LuxuryBorderGold, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = GoldChampagne,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "نصائح سريعة",
                color = TextPrimaryWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        listOf(
            "فعّل وضع الطيران لمدة 5 ثوانٍ ثم أطفئه.",
            "اقترب أكثر من جهاز الراوتر إذا كنت تستخدم واي فاي.",
            "جرّب التبديل بين الواي فاي وبيانات الجوال."
        ).forEach { tip ->
            Text(
                text = "•  $tip",
                color = TextSecondaryMuted,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
