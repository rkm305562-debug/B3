package com.example.zainqhchat.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.zainqhchat.ui.theme.GoldChampagne
import com.example.zainqhchat.ui.theme.GoldDark
import com.example.zainqhchat.ui.theme.GoldGradientEnd
import com.example.zainqhchat.ui.theme.GoldGradientStart
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import kotlinx.coroutines.delay

/**
 * شاشة البداية (Splash) — بتصميم "حلقة تحميل" (خلفية متدرّجة فاتحة + دائرة
 * منقّطة ثابتة + قوس متدرّج دوّار فوقها وفقاعة دردشة في المنتصف)، مطابقة
 * لروح الصورة المرجعية التي أرفقها المستخدم، لكن بألوان التطبيق نفسها
 * وباسمه الحالي "قروب بنات و شباب".
 *
 * السلوك الوظيفي كما هو تمامًا: تنتظر [minDurationMillis] كحد أدنى ثم تستدعي
 * [onSplashFinished] بحالة تسجيل الدخول — المتصفح (NavHost في MainActivity)
 * هو من يقرر لاحقًا إلى أين ينتقل بناءً على هذه القيمة.
 */
@Composable
fun SplashScreen(
    isLoggedIn: Boolean,
    onSplashFinished: (isLoggedIn: Boolean) -> Unit,
    minDurationMillis: Long = 2000
) {
    var contentVisible by remember { mutableStateOf(false) }

    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "content_alpha"
    )

    LaunchedEffect(Unit) {
        contentVisible = true
        delay(minDurationMillis)
        onSplashFinished(isLoggedIn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GoldChampagne, LuxuryBlackBg, LuxuryBlackBg)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoadingRingWithIcon()

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.app_name),
                color = GoldDark,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.splash_subtitle),
                color = TextSecondaryMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            LoadingProgressBar()

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.splash_loading_text),
                color = GoldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * الحلقة الدائرية: دائرة منقّطة ثابتة فاتحة اللون تمثل "المسار"، وفوقها قوس
 * متدرّج (أزرق) يدور باستمرار ليعطي إحساس التحميل الحي، وفي المنتصف أيقونة
 * فقاعة دردشة — تمامًا كبنية الصورة المرجعية.
 */
@Composable
private fun LoadingRingWithIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation_value"
    )

    Box(
        modifier = Modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // الدائرة المنقّطة الثابتة (المسار الخلفي)
        Canvas(modifier = Modifier.size(150.dp)) {
            drawCircle(
                color = LuxuryBorderGold,
                radius = size.minDimension / 2 - 6.dp.toPx(),
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }

        // القوس المتدرّج الدوّار (مؤشر التحميل الفعلي)
        Canvas(modifier = Modifier.size(150.dp)) {
            rotate(degrees = rotation) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(GoldGradientStart, GoldGradientEnd, Color.Transparent)),
                    startAngle = 0f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                    size = Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // الدائرة الداخلية + فقاعة الدردشة
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(TextPrimaryWhite.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubble,
                contentDescription = stringResource(R.string.content_desc_app_logo),
                tint = GoldPrimary,
                modifier = Modifier.size(46.dp)
            )
        }
    }
}

/** شريط تقدّم رفيع بحواف دائرية، جزء منه ملوّن (أزرق) والباقي فاتح — كالمرجع. */
@Composable
private fun LoadingProgressBar() {
    val infiniteTransition = rememberInfiniteTransition(label = "progress_bar")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress_value"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(LuxuryBorderGold)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(GoldGradientStart, GoldGradientEnd)))
        )
    }
}
