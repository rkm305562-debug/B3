package com.example.zainqhchat.domain.model

/** سيارة من كتالوج المتجر (3 فقط — تصاميم خيالية أصلية، ليست علامات حقيقية). */
data class ShopCar(
    val id: String,
    val name: String,
    val tier: Int,
    val nominalValueUsd: Long,
    val priceDiamonds: Int,
    val icon: String,
    val isOwned: Boolean = false,
    val isSelected: Boolean = false
)

/** هدية من كتالوج الهدايا القابلة للشراء والإرسال. */
data class ShopGift(
    val id: String,
    val name: String,
    val icon: String,
    val priceDiamonds: Int,
    val coinValue: Int
)

/** هدية وردت لهذا المستخدم من شخص آخر (بانتظار الاستلام أو مُستلَمة). */
data class ReceivedGift(
    val id: Long,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String?,
    val giftId: String,
    val giftName: String,
    val giftIcon: String,
    val coinValue: Int,
    val isClaimed: Boolean,
    val createdAtTimestamp: Long
)

/** نوع العملية في سجل العمليات المالية. */
enum class CurrencyTransactionType {
    DAILY_REWARD, AD_REWARD, ACCUMULATOR_COLLECT, GIFT_SENT, GIFT_RECEIVED,
    DIAMOND_PURCHASE, CAR_PURCHASE, ADMIN_ADJUSTMENT;

    companion object {
        fun fromRaw(raw: String): CurrencyTransactionType = when (raw) {
            "daily_reward" -> DAILY_REWARD
            "ad_reward" -> AD_REWARD
            "accumulator_collect" -> ACCUMULATOR_COLLECT
            "gift_sent" -> GIFT_SENT
            "gift_received" -> GIFT_RECEIVED
            "diamond_purchase" -> DIAMOND_PURCHASE
            "car_purchase" -> CAR_PURCHASE
            else -> ADMIN_ADJUSTMENT
        }
    }
}

enum class CurrencyKind { COINS, DIAMONDS }

/** سطر واحد في سجل العمليات المالية (audit log حقيقي من currency_transactions). */
data class CurrencyTransaction(
    val id: Long,
    val type: CurrencyTransactionType,
    val currency: CurrencyKind,
    val amount: Long,
    val balanceAfter: Long,
    val description: String,
    val createdAtTimestamp: Long
)

/** نتيجة تبديل عملات مقابل ألماس داخل التطبيق (1000 عملة = 1 ألماسة — لا أموال حقيقية). */
data class CoinsToDiamondsExchangeResult(val newCoinsBalance: Long, val newDiamondsBalance: Long, val coinsSpent: Long)

/** نتيجة استلام المكافأة اليومية. */
data class DailyRewardResult(val reward: Int, val streak: Int, val newCoinsBalance: Long)

/** نتيجة استلام مكافأة الإعلان. */
data class AdRewardResult(val reward: Int, val remainingToday: Int, val newCoinsBalance: Long)

/** حالة العدّاد التراكمي كما يراها الخادم (لعرض حي دقيق بلا اعتماد على ساعة الجهاز). */
data class AccumulatorStatus(val startedAtEpochMillis: Long, val serverNowEpochMillis: Long)

/** نتيجة سحب العملات المتراكمة. */
data class AccumulatorClaimResult(val reward: Long, val ticks: Int, val newCoinsBalance: Long)

/** صف في المتصدرين حسب الثروة (عملات + ألماس + قيمة السيارات) — منفصل عن نظام النقاط points. */
data class WealthLeaderboardEntry(
    val id: String,
    val username: String,
    val name: String,
    val avatarUrl: String?,
    val coins: Long,
    val diamonds: Long,
    val carsValue: Long,
    val wealthScore: Long,
    val selectedCarId: String?
)
