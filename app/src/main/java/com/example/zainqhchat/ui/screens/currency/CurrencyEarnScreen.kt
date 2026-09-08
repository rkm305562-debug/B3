package com.example.zainqhchat.ui.screens.currency

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.core.ads.RewardedAdManager
import com.example.zainqhchat.domain.model.ReceivedGift
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.ui.components.GoldButton
import com.example.zainqhchat.ui.components.UserAvatar
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.CurrencyActionState
import com.example.zainqhchat.ui.viewmodels.CurrencyUiState
import com.example.zainqhchat.ui.viewmodels.CurrencyViewModel

@Composable
fun CurrencyEarnScreen(
    currencyViewModel: CurrencyViewModel,
    currentUser: User,
    onBackClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val rewardedAdManager = remember { RewardedAdManager(context) }

    LaunchedEffect(Unit) {
        rewardedAdManager.preload()
        currencyViewModel.loadReceivedGifts()
        currencyViewModel.startAccumulatorTicker()
    }

    val dailyState by currencyViewModel.dailyRewardAction.collectAsState()
    val adState by currencyViewModel.adRewardAction.collectAsState()
    val accumulatorState by currencyViewModel.accumulatorAction.collectAsState()
    val giftClaimState by currencyViewModel.giftClaimAction.collectAsState()
    val pendingAccumulator by currencyViewModel.pendingAccumulatorAmount.collectAsState()
    val receivedGiftsState by currencyViewModel.receivedGiftsState.collectAsState()

    ActionToast(dailyState) { currencyViewModel.clearDailyRewardAction() }
    ActionToast(adState) { currencyViewModel.clearAdRewardAction() }
    ActionToast(accumulatorState) { currencyViewModel.clearAccumulatorAction() }
    ActionToast(giftClaimState) { currencyViewModel.clearGiftClaimAction() }

    Scaffold(
        topBar = {
            CurrencyTopBar(
                title = "زيادة العملات 🪙",
                userAvatarUrl = currentUser.avatarUrl,
                userName = currentUser.name,
                onBackClick = onBackClick,
                onNotificationsClick = onOpenNotifications,
                onAvatarClick = onOpenProfile
            )
        },
        containerColor = LuxuryBlackBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                EarnRow(
                    icon = Icons.Default.CalendarMonth,
                    title = "مكافأة يومية",
                    subtitle = "تزداد كل يوم متتالٍ — تعود من البداية عند الانقطاع",
                    buttonText = "استلام 🎁",
                    isLoading = dailyState is CurrencyActionState.Loading,
                    onClick = { currencyViewModel.claimDailyReward() }
                )
            }

            item {
                EarnRow(
                    icon = Icons.Default.PlayCircle,
                    title = "إعلان مكافأة",
                    subtitle = "15 عملة بعد مشاهدة إعلان كامل — 3 مرات كحد أقصى يوميًا",
                    buttonText = "مشاهدة 📺",
                    isLoading = adState is CurrencyActionState.Loading,
                    onClick = {
                        val activity = context as? Activity
                        if (activity == null) {
                            Toast.makeText(context, "تعذّر فتح الإعلان الآن", Toast.LENGTH_SHORT).show()
                        } else {
                            rewardedAdManager.show(
                                activity = activity,
                                onEarned = { currencyViewModel.claimAdReward() },
                                onUnavailable = {
                                    Toast.makeText(context, "الإعلان غير جاهز الآن، حاول لاحقًا", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                )
            }

            item {
                AccumulatorCard(
                    pendingAmount = pendingAccumulator,
                    isLoading = accumulatorState is CurrencyActionState.Loading,
                    onWithdraw = { currencyViewModel.claimAccumulatedCoins() }
                )
            }

            item {
                Text("الهدايا المستلمة 🎁", color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            item {
                CurrencyStateContent(state = receivedGiftsState, onRetry = { currencyViewModel.loadReceivedGifts() }) { gifts ->
                    if (gifts.isEmpty()) {
                        Text("لا توجد هدايا بعد", color = TextSecondaryMuted, fontSize = 12.5.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            gifts.forEach { gift ->
                                ReceivedGiftRow(
                                    gift = gift,
                                    onClaim = { currencyViewModel.claimReceivedGift(gift.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionToast(state: CurrencyActionState, onConsumed: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(state) {
        when (state) {
            is CurrencyActionState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                onConsumed()
            }
            is CurrencyActionState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                onConsumed()
            }
            else -> {}
        }
    }
}

@Composable
private fun EarnRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
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
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(GoldPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = TextSecondaryMuted, fontSize = 11.sp, maxLines = 2)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isLoading) {
            CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(28.dp))
        } else {
            GoldButton(text = buttonText, onClick = onClick, modifier = Modifier.width(110.dp))
        }
    }
}

@Composable
private fun AccumulatorCard(pendingAmount: Long, isLoading: Boolean, onWithdraw: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LuxurySurfaceCard.copy(alpha = 0.85f))
            .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(GoldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("عملات متراكمة", color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("تزداد كل ما بقيت في التطبيق — اسحبها متى شئت", color = TextSecondaryMuted, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("$pendingAmount", color = GoldPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Spacer(modifier = Modifier.weight(1f))
            if (isLoading) {
                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(28.dp))
            } else {
                GoldButton(
                    text = "سحب العملات",
                    onClick = onWithdraw,
                    enabled = pendingAmount > 0,
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

@Composable
private fun ReceivedGiftRow(gift: ReceivedGift, onClaim: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LuxurySurfaceCard.copy(alpha = 0.7f))
            .border(1.dp, LuxuryBorderGold, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(name = gift.senderName, avatarUrl = gift.senderAvatarUrl, size = 38.dp, showOnlineIndicator = false)
        Spacer(modifier = Modifier.width(10.dp))
        Icon(currencyIconFor(gift.giftIcon), contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${gift.senderName} أرسل ${gift.giftName}", color = TextPrimaryWhite, fontSize = 12.5.sp, maxLines = 1)
            Text("قيمتها ${gift.coinValue} عملة", color = TextSecondaryMuted, fontSize = 11.sp)
        }
        if (gift.isClaimed) {
            Text("تم الاستلام", color = TextSecondaryMuted, fontSize = 11.sp)
        } else {
            GoldButton(text = "استلام", onClick = onClaim, modifier = Modifier.width(90.dp))
        }
    }
}
