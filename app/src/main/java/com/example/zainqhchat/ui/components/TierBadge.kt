package com.example.zainqhchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.UserTier

@Composable
fun TierBadge(
    tier: UserTier,
    modifier: Modifier = Modifier,
    showPoints: Boolean = false,
    points: Int = 0
) {
    val badgeColor = Color(tier.hexColor)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(badgeColor.copy(alpha = 0.18f))
            .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (tier.badgeSymbol.isNotBlank()) {
            Text(
                text = tier.badgeSymbol,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = tier.titleArabic,
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        if (showPoints) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($points ن)",
                color = badgeColor.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
