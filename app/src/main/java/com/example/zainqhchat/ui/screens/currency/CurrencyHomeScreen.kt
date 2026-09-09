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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.CurrencyViewModel

/**
 * الصفحة الرئيسية لقسم العملات: رصيد حقيقي يتحدث فورًا (Realtime) + شبكة
 * 2×2 من البطاقات تفتح كل واحدة شاشتها الخاصة. Mobile-First: بدون أي
 * تمرير أفقي، كل شيء يظهر ضمن عرض الشاشة.
 */
@Composable
fun CurrencyHomeScreen(
    currencyViewModel: CurrencyViewModel,
    fallbackUser: User,
    onBackClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenEarnCoins: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val liveUser by currencyViewModel.liveUser.collectAsState()
    val user = liveUser ?: fallbackUser

    Scaffold(
        topBar = {
            CurrencyTopBar(
                title = "العملات",
                userAvatarUrl = user.avatarUrl,
                userName = user.name,
                onBackClick = onBackClick,
                onNotificationsClick = onOpenNotifications,
                onAvatarClick = onOpenProfile
            )
        },
        containerColor = LuxuryBlackBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "إدارة رصيدك ومكافآتك ومشترياتك",
                    color = TextSecondaryMuted,
                    fontSize = 13.sp
                )
            }

            item {
                BalanceCard(coins = user.coins, diamonds = user.diamonds)
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    userScrollEnabled = false
                ) {
                    item {
                        CurrencyGridCard(
                            title = "المتصدرون",
                            subtitle = "أفضل 3 حسب الثروة",
                            icon = Icons.Default.EmojiEvents,
                            onClick = onOpenLeaderboard
                        )
                    }
                    item {
                        CurrencyGridCard(
                            title = "زيادة العملات",
                            subtitle = "مكافآت وهدايا وتواجد",
                            icon = Icons.Default.MonetizationOn,
                            onClick = onOpenEarnCoins
                        )
                    }
                    item {
                        CurrencyGridCard(
                            title = "الشراء",
                            subtitle = "الألماس والسيارات",
                            icon = Icons.Default.Storefront,
                            onClick = onOpenShop
                        )
                    }
                    item {
                        CurrencyGridCard(
                            title = "سجل العمليات",
                            subtitle = "كل حركة مالية بالتفصيل",
                            icon = Icons.Default.History,
                            onClick = onOpenHistory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(coins: Long, diamonds: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(listOf(LuxurySurfaceCard, LuxurySurfaceCard.copy(alpha = 0.7f)))
            )
            .border(1.2.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Text("رصيدك الحالي", color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatAmount(coins),
                color = GoldPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp
            )
            Text(" عملة", color = TextSecondaryMuted, fontSize = 13.sp)

            Spacer(modifier = Modifier.width(20.dp))

            Icon(Icons.Default.Diamond, contentDescription = null, tint = TextPrimaryWhite, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formatAmount(diamonds),
                color = TextPrimaryWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LuxuryBorderGold))
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "رصيدك يتحدث تلقائيًا بعد كل عملية ناجحة",
            color = TextSecondaryMuted,
            fontSize = 11.5.sp
        )
    }
}

private fun formatAmount(value: Long): String =
    "%,d".format(value)
