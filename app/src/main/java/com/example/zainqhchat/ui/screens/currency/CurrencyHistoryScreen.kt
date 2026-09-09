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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.CurrencyKind
import com.example.zainqhchat.domain.model.CurrencyTransaction
import com.example.zainqhchat.domain.model.CurrencyTransactionType
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.CurrencyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrencyHistoryScreen(
    currencyViewModel: CurrencyViewModel,
    currentUser: User,
    onBackClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    LaunchedEffect(Unit) { currencyViewModel.loadTransactions() }
    val state by currencyViewModel.transactionsState.collectAsState()

    Scaffold(
        topBar = {
            CurrencyTopBar(
                title = "سجل العمليات 📜",
                userAvatarUrl = currentUser.avatarUrl,
                userName = currentUser.name,
                onBackClick = onBackClick,
                onNotificationsClick = onOpenNotifications,
                onAvatarClick = onOpenProfile
            )
        },
        containerColor = LuxuryBlackBg
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            CurrencyStateContent(state = state, onRetry = { currencyViewModel.loadTransactions() }) { transactions ->
                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد عمليات بعد", color = TextSecondaryMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transactions.size) { index -> TransactionRow(transactions[index]) }
                    }
                }
            }
        }
    }
}

private fun iconFor(type: CurrencyTransactionType): ImageVector = when (type) {
    CurrencyTransactionType.DAILY_REWARD -> Icons.Default.CalendarMonth
    CurrencyTransactionType.AD_REWARD -> Icons.Default.PlayCircle
    CurrencyTransactionType.ACCUMULATOR_COLLECT -> Icons.Default.Timer
    CurrencyTransactionType.GIFT_SENT, CurrencyTransactionType.GIFT_RECEIVED -> Icons.Default.CardGiftcard
    CurrencyTransactionType.CAR_PURCHASE -> Icons.Default.DirectionsCar
    CurrencyTransactionType.DIAMOND_PURCHASE -> Icons.Default.Diamond
    CurrencyTransactionType.ADMIN_ADJUSTMENT -> Icons.Default.TrendingUp
}

@Composable
private fun TransactionRow(tx: CurrencyTransaction) {
    val isPositive = tx.amount >= 0
    val amountColor = if (isPositive) Color(0xFF22C55E) else Color(0xFFEF4444)
    val currencySymbol = if (tx.currency == CurrencyKind.DIAMONDS) "💎" else "🪙"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LuxurySurfaceCard.copy(alpha = 0.8f))
            .border(1.dp, LuxuryBorderGold, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(GoldPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = amountColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.description, color = TextPrimaryWhite, fontSize = 12.5.sp, maxLines = 2)
            Text(formatDateTime(tx.createdAtTimestamp), color = TextSecondaryMuted, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${if (isPositive) "+" else ""}${tx.amount} $currencySymbol",
            color = amountColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

private fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("ar")).format(Date(epochMillis))
