package com.example.zainqhchat.data.repository

import com.example.zainqhchat.data.remote.supabase.SupabaseCurrencyService
import com.example.zainqhchat.domain.model.AccumulatorClaimResult
import com.example.zainqhchat.domain.model.AccumulatorStatus
import com.example.zainqhchat.domain.model.AdRewardResult
import com.example.zainqhchat.domain.model.CoinsToDiamondsExchangeResult
import com.example.zainqhchat.domain.model.CurrencyTransaction
import com.example.zainqhchat.domain.model.DailyRewardResult
import com.example.zainqhchat.domain.model.ReceivedGift
import com.example.zainqhchat.domain.model.ShopCar
import com.example.zainqhchat.domain.model.ShopGift
import com.example.zainqhchat.domain.model.WealthLeaderboardEntry
import com.example.zainqhchat.domain.repository.CurrencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CurrencyRepositoryImpl(
    private val currencyService: SupabaseCurrencyService
) : CurrencyRepository {

    override suspend fun fetchShopCars(userId: String, selectedCarId: String?): List<ShopCar> =
        withContext(Dispatchers.IO) {
            val owned = currencyService.fetchOwnedCarIds(userId)
            currencyService.fetchShopCars(owned, selectedCarId)
        }

    override suspend fun fetchShopGifts(): List<ShopGift> = withContext(Dispatchers.IO) {
        currencyService.fetchShopGifts()
    }

    override suspend fun fetchReceivedGifts(userId: String): List<ReceivedGift> = withContext(Dispatchers.IO) {
        currencyService.fetchReceivedGifts(userId)
    }

    override suspend fun fetchTransactions(userId: String): List<CurrencyTransaction> = withContext(Dispatchers.IO) {
        currencyService.fetchTransactions(userId)
    }

    override suspend fun fetchWealthLeaderboard(): List<WealthLeaderboardEntry> = withContext(Dispatchers.IO) {
        currencyService.fetchWealthLeaderboard()
    }

    override suspend fun claimDailyReward(): Result<DailyRewardResult> = withContext(Dispatchers.IO) {
        currencyService.claimDailyReward()
    }

    override suspend fun claimAdReward(): Result<AdRewardResult> = withContext(Dispatchers.IO) {
        currencyService.claimAdReward()
    }

    override suspend fun getAccumulatorStatus(): Result<AccumulatorStatus> = withContext(Dispatchers.IO) {
        currencyService.getAccumulatorStatus()
    }

    override suspend fun claimAccumulatedCoins(): Result<AccumulatorClaimResult> = withContext(Dispatchers.IO) {
        currencyService.claimAccumulatedCoins()
    }

    override suspend fun purchaseCar(carId: String): Result<Unit> = withContext(Dispatchers.IO) {
        currencyService.purchaseCar(carId)
    }

    override suspend fun selectOwnedCar(carId: String?): Result<Unit> = withContext(Dispatchers.IO) {
        currencyService.selectOwnedCar(carId)
    }

    override suspend fun sendGift(recipientId: String, giftId: String): Result<Unit> = withContext(Dispatchers.IO) {
        currencyService.sendGift(recipientId, giftId)
    }

    override suspend fun claimReceivedGift(giftRecordId: Long): Result<Long> = withContext(Dispatchers.IO) {
        currencyService.claimReceivedGift(giftRecordId)
    }

    override suspend fun exchangeCoinsForDiamonds(diamonds: Int): Result<CoinsToDiamondsExchangeResult> =
        withContext(Dispatchers.IO) {
            currencyService.exchangeCoinsForDiamonds(diamonds)
        }
}
