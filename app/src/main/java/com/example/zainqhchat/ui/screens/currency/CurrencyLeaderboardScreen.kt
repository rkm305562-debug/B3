package com.example.zainqhchat.ui.screens.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.model.WealthLeaderboardEntry
import com.example.zainqhchat.ui.components.UserAvatar
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.CurrencyViewModel

@Composable
fun CurrencyLeaderboardScreen(
    currencyViewModel: CurrencyViewModel,
    currentUser: User,
    onBackClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    LaunchedEffect(Unit) { currencyViewModel.loadLeaderboard() }
    val state by currencyViewModel.leaderboardState.collectAsState()

    Scaffold(
        topBar = {
            CurrencyTopBar(
                title = "المتصدرون 🏆",
                userAvatarUrl = currentUser.avatarUrl,
                userName = currentUser.name,
                onBackClick = onBackClick,
                onNotificationsClick = onOpenNotifications,
                onAvatarClick = onOpenProfile
            )
        },
        containerColor = LuxuryBlackBg
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Text(
                "أفضل 3 مستخدمين حسب مجموع العملات والألماس وقيمة السيارات المملوكة",
                color = TextSecondaryMuted,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(16.dp)
            )

            CurrencyStateContent(state = state, onRetry = { currencyViewModel.loadLeaderboard() }) { entries ->
                if (entries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا يوجد مستخدمون بعد", color = TextSecondaryMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(entries.take(3).withIndex().toList()) { (index, entry) ->
                            LeaderboardRow(rank = index + 1, entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: WealthLeaderboardEntry) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LuxurySurfaceCard.copy(alpha = 0.85f))
            .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(rankColor),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        UserAvatar(name = entry.name, avatarUrl = entry.avatarUrl, size = 46.dp, showOnlineIndicator = false)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                Text(" ${entry.coins}", color = TextSecondaryMuted, fontSize = 11.5.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Diamond, contentDescription = null, tint = TextPrimaryWhite, modifier = Modifier.size(13.dp))
                Text(" ${entry.diamonds}", color = TextSecondaryMuted, fontSize = 11.5.sp)
            }
            val carInfo = CurrencyStaticCatalog.carInfo(entry.selectedCarId)
            if (carInfo != null) {
                Text("🚗 ${carInfo.name}", color = GoldPrimary, fontSize = 10.5.sp)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("الثروة", color = TextSecondaryMuted, fontSize = 10.sp)
            Text("${entry.wealthScore}", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
