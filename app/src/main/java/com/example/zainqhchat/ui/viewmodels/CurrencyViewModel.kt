package com.example.zainqhchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zainqhchat.domain.model.AccumulatorStatus
import com.example.zainqhchat.domain.model.CurrencyTransaction
import com.example.zainqhchat.domain.model.ReceivedGift
import com.example.zainqhchat.domain.model.ShopCar
import com.example.zainqhchat.domain.model.ShopGift
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.model.WealthLeaderboardEntry
import com.example.zainqhchat.domain.repository.CurrencyRepository
import com.example.zainqhchat.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** حالة عامة قابلة لإعادة الاستخدام لأي شاشة فرعية في قسم العملات: تحميل/نجاح/خطأ. */
sealed interface CurrencyUiState<out T> {
    object Loading : CurrencyUiState<Nothing>
    data class Success<T>(val data: T) : CurrencyUiState<T>
    data class Error(val message: String) : CurrencyUiState<Nothing>
}

/** حالة إجراء لحظي (استلام مكافأة، شراء، إرسال هدية...) منفصلة عن حالة تحميل القوائم. */
sealed interface CurrencyActionState {
    object Idle : CurrencyActionState
    object Loading : CurrencyActionState
    data class Success(val message: String) : CurrencyActionState
    data class Error(val message: String) : CurrencyActionState
}

class CurrencyViewModel(
    private val currencyRepository: CurrencyRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentUserIdState = MutableStateFlow<String?>(null)

    /** يجب استدعاؤها مرة عند دخول قسم العملات (بنفس نمط UserViewModel.setCurrentUserId). */
    fun setCurrentUserId(userId: String) {
        if (_currentUserIdState.value == userId) return
        _currentUserIdState.value = userId
    }

    private fun requireUserId(): String =
        _currentUserIdState.value ?: throw IllegalStateException("لم يتم تحديد المستخدم الحالي بعد")

    /** رصيد المستخدم الحالي — يتحدث فورًا عبر نفس آلية Realtime المستخدمة أصلًا لبقية التطبيق. */
    val liveUser: StateFlow<User?> = _currentUserIdState.flatMapLatest { userId ->
        if (userId == null) flowOf(null) else userRepository.getUserByIdFlow(userId, userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _shopCarsState = MutableStateFlow<CurrencyUiState<List<ShopCar>>>(CurrencyUiState.Loading)
    val shopCarsState: StateFlow<CurrencyUiState<List<ShopCar>>> = _shopCarsState.asStateFlow()

    private val _shopGiftsState = MutableStateFlow<CurrencyUiState<List<ShopGift>>>(CurrencyUiState.Loading)
    val shopGiftsState: StateFlow<CurrencyUiState<List<ShopGift>>> = _shopGiftsState.asStateFlow()

    private val _receivedGiftsState = MutableStateFlow<CurrencyUiState<List<ReceivedGift>>>(CurrencyUiState.Loading)
    val receivedGiftsState: StateFlow<CurrencyUiState<List<ReceivedGift>>> = _receivedGiftsState.asStateFlow()

    private val _transactionsState = MutableStateFlow<CurrencyUiState<List<CurrencyTransaction>>>(CurrencyUiState.Loading)
    val transactionsState: StateFlow<CurrencyUiState<List<CurrencyTransaction>>> = _transactionsState.asStateFlow()

    private val _leaderboardState = MutableStateFlow<CurrencyUiState<List<WealthLeaderboardEntry>>>(CurrencyUiState.Loading)
    val leaderboardState: StateFlow<CurrencyUiState<List<WealthLeaderboardEntry>>> = _leaderboardState.asStateFlow()

    private val _dailyRewardAction = MutableStateFlow<CurrencyActionState>(CurrencyActionState.Idle)
    val dailyRewardAction: StateFlow<CurrencyActionState> = _dailyRewardAction.asStateFlow()

    private val _adRewardAction = MutableStateFlow<CurrencyActionState>(CurrencyActionState.Idle)
    val adRewardAction: StateFlow<CurrencyActionState> = _adRewardAction.asStateFlow()

    private val _accumulatorAction = MutableStateFlow<CurrencyActionState>(CurrencyActionState.Idle)
    val accumulatorAction: StateFlow<CurrencyActionState> = _accumulatorAction.asStateFlow()

    private val _giftClaimAction = MutableStateFlow<CurrencyActionState>(CurrencyActionState.Idle)
    val giftClaimAction: StateFlow<CurrencyActionState> = _giftClaimAction.asStateFlow()

    private val _giftSendAction = MutableStateFlow<CurrencyActionState>(CurrencyActionState.Idle)
    val giftSendAction: StateFlow<CurrencyActionState> = _giftSendAction.asStateFlow()

    private val _carPurchaseAction = MutableStateFlow<CurrencyActionState>(CurrencyActionState.Idle)
    val carPurchaseAction: StateFlow<CurrencyActionState> = _carPurchaseAction.asStateFlow()

    private val _diamondExchangeAction = MutableStateFlow<CurrencyActionState>(CurrencyActionState.Idle)
    val diamondExchangeAction: StateFlow<CurrencyActionState> = _diamondExchangeAction.asStateFlow()

    /** المبلغ التراكمي "الحي" المعروض للمستخدم (يُحتسَب محليًا من ساعة الخادم، ولا يُعتمَد عليه للمنح الفعلي). */
    private val _pendingAccumulatorAmount = MutableStateFlow(0L)
    val pendingAccumulatorAmount: StateFlow<Long> = _pendingAccumulatorAmount.asStateFlow()

    private var accumulatorStartedAtMillis: Long? = null
    private var accumulatorClockOffsetMillis: Long = 0L
    private var accumulatorTickerJob: kotlinx.coroutines.Job? = null

    fun loadShopCars() {
        viewModelScope.launch {
            _shopCarsState.value = CurrencyUiState.Loading
            try {
                val cars = currencyRepository.fetchShopCars(requireUserId(), liveUser.value?.selectedCarId)
                _shopCarsState.value = CurrencyUiState.Success(cars)
            } catch (e: Exception) {
                _shopCarsState.value = CurrencyUiState.Error(friendlyLoadError())
            }
        }
    }

    fun loadShopGifts() {
        viewModelScope.launch {
            _shopGiftsState.value = CurrencyUiState.Loading
            try {
                _shopGiftsState.value = CurrencyUiState.Success(currencyRepository.fetchShopGifts())
            } catch (e: Exception) {
                _shopGiftsState.value = CurrencyUiState.Error(friendlyLoadError())
            }
        }
    }

    fun loadReceivedGifts() {
        viewModelScope.launch {
            _receivedGiftsState.value = CurrencyUiState.Loading
            try {
                _receivedGiftsState.value = CurrencyUiState.Success(currencyRepository.fetchReceivedGifts(requireUserId()))
            } catch (e: Exception) {
                _receivedGiftsState.value = CurrencyUiState.Error(friendlyLoadError())
            }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _transactionsState.value = CurrencyUiState.Loading
            try {
                _transactionsState.value = CurrencyUiState.Success(currencyRepository.fetchTransactions(requireUserId()))
            } catch (e: Exception) {
                _transactionsState.value = CurrencyUiState.Error(friendlyLoadError())
            }
        }
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _leaderboardState.value = CurrencyUiState.Loading
            try {
                _leaderboardState.value = CurrencyUiState.Success(currencyRepository.fetchWealthLeaderboard())
            } catch (e: Exception) {
                _leaderboardState.value = CurrencyUiState.Error(friendlyLoadError())
            }
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            _dailyRewardAction.value = CurrencyActionState.Loading
            currencyRepository.claimDailyReward()
                .onSuccess {
                    _dailyRewardAction.value = CurrencyActionState.Success(
                        "🎉 حصلت على ${it.reward} عملة! (اليوم ${it.streak} من المتتالية)"
                    )
                }
                .onFailure { e -> _dailyRewardAction.value = CurrencyActionState.Error(friendlyActionError(e)) }
        }
    }

    fun claimAdReward() {
        viewModelScope.launch {
            _adRewardAction.value = CurrencyActionState.Loading
            currencyRepository.claimAdReward()
                .onSuccess {
                    _adRewardAction.value = CurrencyActionState.Success(
                        "🎉 حصلت على ${it.reward} عملة! متبقٍ ${it.remainingToday} مكافآت اليوم"
                    )
                }
                .onFailure { e -> _adRewardAction.value = CurrencyActionState.Error(friendlyActionError(e)) }
        }
    }

    /** يبدأ عدّادًا محليًا حيًا (كل ثانية) لعرض المبلغ التراكمي المستحق، بالاعتماد على ساعة الخادم فقط. */
    fun startAccumulatorTicker() {
        if (accumulatorTickerJob?.isActive == true) return
        accumulatorTickerJob = viewModelScope.launch {
            currencyRepository.getAccumulatorStatus()
                .onSuccess { status: AccumulatorStatus ->
                    accumulatorStartedAtMillis = status.startedAtEpochMillis
                    accumulatorClockOffsetMillis = status.serverNowEpochMillis - System.currentTimeMillis()
                    while (true) {
                        val startedAt = accumulatorStartedAtMillis ?: break
                        val serverNow = System.currentTimeMillis() + accumulatorClockOffsetMillis
                        val minutes = (serverNow - startedAt) / 60000.0
                        val ticks = minOf((minutes / 2.0).toInt(), 30)
                        _pendingAccumulatorAmount.value = (ticks.toLong() * (ticks + 1)) / 2
                        delay(1000)
                    }
                }
        }
    }

    fun claimAccumulatedCoins() {
        viewModelScope.launch {
            _accumulatorAction.value = CurrencyActionState.Loading
            currencyRepository.claimAccumulatedCoins()
                .onSuccess {
                    _accumulatorAction.value = CurrencyActionState.Success("🎉 تم سحب ${it.reward} عملة إلى رصيدك")
                    accumulatorStartedAtMillis = System.currentTimeMillis() + accumulatorClockOffsetMillis
                    _pendingAccumulatorAmount.value = 0L
                }
                .onFailure { e -> _accumulatorAction.value = CurrencyActionState.Error(friendlyActionError(e)) }
        }
    }

    fun claimReceivedGift(giftRecordId: Long) {
        viewModelScope.launch {
            _giftClaimAction.value = CurrencyActionState.Loading
            currencyRepository.claimReceivedGift(giftRecordId)
                .onSuccess {
                    _giftClaimAction.value = CurrencyActionState.Success("🎁 تم تحويل الهدية إلى $it عملة")
                    loadReceivedGifts()
                }
                .onFailure { e -> _giftClaimAction.value = CurrencyActionState.Error(friendlyActionError(e)) }
        }
    }

    fun sendGift(recipientId: String, giftId: String) {
        viewModelScope.launch {
            _giftSendAction.value = CurrencyActionState.Loading
            currencyRepository.sendGift(recipientId, giftId)
                .onSuccess { _giftSendAction.value = CurrencyActionState.Success("🎁 تم إرسال الهدية بنجاح") }
                .onFailure { e -> _giftSendAction.value = CurrencyActionState.Error(friendlyActionError(e)) }
        }
    }

    fun purchaseCar(carId: String) {
        viewModelScope.launch {
            _carPurchaseAction.value = CurrencyActionState.Loading
            currencyRepository.purchaseCar(carId)
                .onSuccess {
                    _carPurchaseAction.value = CurrencyActionState.Success("🚗 تم شراء السيارة بنجاح")
                    loadShopCars()
                }
                .onFailure { e -> _carPurchaseAction.value = CurrencyActionState.Error(friendlyActionError(e)) }
        }
    }

    fun selectCar(carId: String?) {
        viewModelScope.launch {
            currencyRepository.selectOwnedCar(carId)
                .onSuccess { loadShopCars() }
                .onFailure { e -> _carPurchaseAction.value = CurrencyActionState.Error(friendlyActionError(e)) }
        }
    }

    /** يبدّل عملات مقابل ألماس داخل التطبيق (1000 عملة = 1 ألماسة) — لا أموال حقيقية. */
    fun exchangeCoinsForDiamonds(diamonds: Int) {
        viewModelScope.launch {
            _diamondExchangeAction.value = CurrencyActionState.Loading
            currencyRepository.exchangeCoinsForDiamonds(diamonds)
                .onSuccess {
                    _diamondExchangeAction.value = CurrencyActionState.Success(
                        "💎 تم تبديل ${it.coinsSpent} عملة مقابل $diamonds ألماسة"
                    )
                }
                .onFailure { e -> _diamondExchangeAction.value = CurrencyActionState.Error(friendlyActionError(e)) }
        }
    }

    fun clearDiamondExchangeAction() { _diamondExchangeAction.value = CurrencyActionState.Idle }

    fun clearDailyRewardAction() { _dailyRewardAction.value = CurrencyActionState.Idle }
    fun clearAdRewardAction() { _adRewardAction.value = CurrencyActionState.Idle }
    fun clearAccumulatorAction() { _accumulatorAction.value = CurrencyActionState.Idle }
    fun clearGiftClaimAction() { _giftClaimAction.value = CurrencyActionState.Idle }
    fun clearGiftSendAction() { _giftSendAction.value = CurrencyActionState.Idle }
    fun clearCarPurchaseAction() { _carPurchaseAction.value = CurrencyActionState.Idle }

    /** رسائل عربية واضحة فقط — لا تُعرض أي نصوص تقنية (permission denied / RPC error / undefined). */
    private fun friendlyActionError(e: Throwable): String =
        e.message?.takeIf { it.isNotBlank() && !looksTechnical(it) }
            ?: "تعذّر إتمام العملية، تحقق من اتصالك بالإنترنت وحاول مرة أخرى"

    private fun friendlyLoadError(): String = "تعذّر تحميل البيانات، تحقق من اتصالك بالإنترنت وحاول مرة أخرى"

    private fun looksTechnical(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("permission denied") || lower.contains("rpc") ||
            lower.contains("undefined") || lower.contains("typeerror") || lower.contains("exception")
    }

    class Factory(
        private val currencyRepository: CurrencyRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CurrencyViewModel(currencyRepository, userRepository) as T
        }
    }
}
