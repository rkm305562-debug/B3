package com.example.zainqhchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.zainqhchat.domain.model.UserTier
import com.example.zainqhchat.ui.theme.GoldGradientEnd
import com.example.zainqhchat.ui.theme.GoldGradientStart
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.StatusOnlineGreen

@Composable
fun UserAvatar(
    name: String,
    avatarUrl: String? = null,
    size: Dp = 56.dp,
    isOnline: Boolean = false,
    showOnlineIndicator: Boolean = true,
    tier: UserTier? = null,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().firstOrNull()?.toString()?.uppercase() ?: "Z"
    val goldBorderBrush = Brush.sweepGradient(
        listOf(GoldGradientStart, GoldPrimary, GoldGradientEnd, GoldGradientStart)
    )

    Box(
        modifier = modifier.size(size + 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // الإطار الذهبي المحيط بالصورة الشخصية
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(2.dp, goldBorderBrush, CircleShape)
                .background(LuxuryBlackBg),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNull_or_Empty_or_CustomFallback()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2A2A38), Color(0xFF161622))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = name,
                        tint = GoldPrimary,
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }
        }

        // مؤشر متصل الآن
        if (showOnlineIndicator && isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(LuxuryBlackBg)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(StatusOnlineGreen)
            )
        }

        // شارة الفئة (رمز التاج/الشارة) — لا تُعرض إطلاقًا إن كانت الفئة بلا رمز
        if (tier != null && tier.badgeSymbol.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(Color(tier.hexColor))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(
                    text = tier.badgeSymbol,
                    fontSize = (size.value * 0.24f).sp
                )
            }
        }
    }
}

private fun String?.isNull_or_Empty_or_CustomFallback(): Boolean {
    return this == null || this.isBlank() || this.startsWith("default_")
}
