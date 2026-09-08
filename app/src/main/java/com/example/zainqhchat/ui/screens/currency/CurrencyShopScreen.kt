package com.example.zainqhchat.ui.screens.currency

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.domain.model.ShopCar
import com.example.zainqhchat.domain.model.ShopGift
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.ui.components.GoldButton
import com.example.zainqhchat.ui.components.GoldOutlinedButton
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.LuxurySurfaceDark
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.CurrencyActionState
import com.example.zainqhchat.ui.viewmodels.CurrencyViewModel

@Composable
fun CurrencyShopScreen(
    currencyViewModel: CurrencyViewModel,
    currentUser: User,
    onBackClick: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        currencyViewModel.loadShopCars()
        currencyViewModel.loadShopGifts()
    }

    val carsState by currencyViewModel.shopCarsState.collectAsState()
    val giftsState by currencyViewModel.shopGiftsState.collectAsState()
    val purchaseState by currencyViewModel.carPurchaseAction.collectAsState()

    LaunchedEffect(purchaseState) {
        when (val state = purchaseState) {
            is CurrencyActionState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                currencyViewModel.clearCarPurchaseAction()
            }
            is CurrencyActionState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                currencyViewModel.clearCarPurchaseAction()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CurrencyTopBar(
                title = "الشراء 🛒",
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = LuxurySurfaceDark,
                contentColor = GoldPrimary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("السيارات 🚗") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("الهدايا 🎁") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("الألماس 💎") })
            }

            when (selectedTab) {
                0 -> CurrencyStateContent(state = carsState, onRetry = { currencyViewModel.loadShopCars() }) { cars ->
                    CarsGrid(
                        cars = cars,
                        isBusy = purchaseState is CurrencyActionState.Loading,
                        onPurchase = { currencyViewModel.purchaseCar(it) },
                        onSelect = { currencyViewModel.selectCar(it) }
                    )
                }
                1 -> CurrencyStateContent(state = giftsState, onRetry = { currencyViewModel.loadShopGifts() }) { gifts ->
                    GiftsTab(gifts = gifts, currencyViewModel = currencyViewModel)
                }
                else -> DiamondsExchangeTab(currencyViewModel)
            }
        }
    }
}

@Composable
private fun CarsGrid(
    cars: List<ShopCar>,
    isBusy: Boolean,
    onPurchase: (String) -> Unit,
    onSelect: (String?) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(cars.size) { index ->
            val car = cars[index]
            CarCard(car = car, isBusy = isBusy, onPurchase = { onPurchase(car.id) }, onSelect = {
                onSelect(if (car.isSelected) null else car.id)
            })
        }
    }
}

@Composable
private fun CarCard(car: ShopCar, isBusy: Boolean, onPurchase: () -> Unit, onSelect: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LuxurySurfaceCard.copy(alpha = 0.85f))
            .border(
                1.5.dp,
                if (car.isSelected) GoldPrimary else GoldPrimary.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageRes = CurrencyStaticCatalog.carInfo(car.id)?.imageRes
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = car.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(GoldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(currencyIconFor(car.icon), contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(car.name, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Diamond, contentDescription = null, tint = TextPrimaryWhite, modifier = Modifier.size(14.dp))
            Text(" ${car.priceDiamonds}", color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            isBusy -> CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(24.dp))
            car.isOwned && car.isSelected -> GoldOutlinedButton(text = "مُختارة ✓", onClick = onSelect, modifier = Modifier.height(38.dp))
            car.isOwned -> GoldButton(text = "اختيار", onClick = onSelect, modifier = Modifier.height(38.dp))
            else -> GoldButton(text = "شراء الآن", onClick = onPurchase, icon = Icons.Default.Diamond, modifier = Modifier.height(38.dp))
        }
    }
}

@Composable
private fun GiftsTab(gifts: List<ShopGift>, currencyViewModel: CurrencyViewModel) {
    var selectedGift by remember { mutableIntStateOf(-1) }
    var recipientId by remember { androidx.compose.runtime.mutableStateOf("") }
    val sendState by currencyViewModel.giftSendAction.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(sendState) {
        when (val state = sendState) {
            is CurrencyActionState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                currencyViewModel.clearGiftSendAction()
            }
            is CurrencyActionState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                currencyViewModel.clearGiftSendAction()
            }
            else -> {}
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "اختر هدية، ثم اكتب معرّف المستخدم (User ID) المستلم من ملفه الشخصي وأرسلها",
                color = TextSecondaryMuted,
                fontSize = 11.5.sp
            )
        }

        item {
            androidx.compose.material3.OutlinedTextField(
                value = recipientId,
                onValueChange = { recipientId = it },
                label = { Text("معرّف المستلم", color = TextSecondaryMuted) },
                singleLine = true,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = LuxuryBorderGold,
                    focusedTextColor = TextPrimaryWhite,
                    unfocusedTextColor = TextPrimaryWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(gifts.size) { index ->
            val gift = gifts[index]
            val isSelected = selectedGift == index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LuxurySurfaceCard.copy(alpha = 0.85f))
                    .border(
                        1.5.dp,
                        if (isSelected) GoldPrimary else GoldPrimary.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
                    .then(Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(currencyIconFor(gift.icon), contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(gift.name, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("قيمتها للمستلم: ${gift.coinValue} عملة", color = TextSecondaryMuted, fontSize = 10.5.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Diamond, contentDescription = null, tint = TextPrimaryWhite, modifier = Modifier.size(14.dp))
                    Text(" ${gift.priceDiamonds}", color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldPrimary)
                } else {
                    androidx.compose.material3.TextButton(onClick = { selectedGift = index }) {
                        Text("اختيار", color = GoldPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            val isLoading = sendState is CurrencyActionState.Loading
            GoldButton(
                text = "إرسال الهدية 🎁",
                enabled = selectedGift >= 0 && recipientId.isNotBlank() && !isLoading,
                onClick = { currencyViewModel.sendGift(recipientId.trim(), gifts[selectedGift].id) }
            )
        }
    }
}

@Composable
private fun DiamondsExchangeTab(currencyViewModel: CurrencyViewModel) {
    val context = LocalContext.current
    val liveUser by currencyViewModel.liveUser.collectAsState()
    val exchangeState by currencyViewModel.diamondExchangeAction.collectAsState()
    val coinsPerDiamond = 1000

    LaunchedEffect(exchangeState) {
        when (val state = exchangeState) {
            is CurrencyActionState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                currencyViewModel.clearDiamondExchangeAction()
            }
            is CurrencyActionState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                currencyViewModel.clearDiamondExchangeAction()
            }
            else -> {}
        }
    }

    val isLoading = exchangeState is CurrencyActionState.Loading
    val currentCoins = liveUser?.coins ?: 0L
    val packages = listOf(1, 5, 10, 50, 100)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "بدّل عملاتك داخل التطبيق مقابل ألماس — لا يوجد أي دفع بأموال حقيقية إطلاقًا. كل 1000 عملة = 1 ألماسة 💎",
                color = TextSecondaryMuted,
                fontSize = 12.sp
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Text(" رصيدك: ${"%,d".format(currentCoins)} عملة", color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        items(packages.size) { index ->
            val diamonds = packages[index]
            val cost = diamonds.toLong() * coinsPerDiamond
            val canAfford = currentCoins >= cost

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LuxurySurfaceCard.copy(alpha = 0.85f))
                    .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Diamond, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("$diamonds ألماسة", color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("مقابل ${"%,d".format(cost)} عملة", color = TextSecondaryMuted, fontSize = 11.sp)
                }
                if (isLoading) {
                    CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(24.dp))
                } else {
                    GoldButton(
                        text = "تبديل",
                        enabled = canAfford,
                        onClick = { currencyViewModel.exchangeCoinsForDiamonds(diamonds) },
                        modifier = Modifier.width(90.dp)
                    )
                }
            }
        }
    }
}
