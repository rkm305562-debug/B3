package com.example.zainqhchat.domain.repository

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

interface CurrencyRepository {
    suspend fun fetchShopCars(userId: String, selectedCarId: String?): List<ShopCar>
    suspend fun fetchShopGifts(): List<ShopGift>
    suspend fun fetchReceivedGifts(userId: String): List<ReceivedGift>
    suspend fun fetchTransactions(userId: String): List<CurrencyTransaction>
    suspend fun fetchWealthLeaderboard(): List<WealthLeaderboardEntry>

    suspend fun claimDailyReward(): Result<DailyRewardResult>
    suspend fun claimAdReward(): Result<AdRewardResult>
    suspend fun getAccumulatorStatus(): Result<AccumulatorStatus>
    suspend fun claimAccumulatedCoins(): Result<AccumulatorClaimResult>
    suspend fun purchaseCar(carId: String): Result<Unit>
    suspend fun selectOwnedCar(carId: String?): Result<Unit>
    suspend fun sendGift(recipientId: String, giftId: String): Result<Unit>
    suspend fun claimReceivedGift(giftRecordId: Long): Result<Long>
    suspend fun exchangeCoinsForDiamonds(diamonds: Int): Result<CoinsToDiamondsExchangeResult>
}
