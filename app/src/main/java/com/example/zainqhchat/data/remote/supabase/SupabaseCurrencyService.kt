package com.example.zainqhchat.data.remote.supabase

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
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * كل عمليات قسم العملات الحقيقية عبر Supabase PostgREST/RPC — بدون أي
 * منطق مالي على العميل: كل تعديل رصيد (عملات/ألماس/ملكية) يمرّ عبر دالة
 * RPC واحدة ذرّية على الخادم (راجع supabase/migrations/002_currency_system.sql).
 * هذا الملف يقرأ فقط ويستدعي RPC، ولا يكتب لأي عمود رصيد مباشرة إطلاقًا.
 */
interface SupabaseCurrencyService {
    suspend fun fetchShopCars(ownedCarIds: Set<String>, selectedCarId: String?): List<ShopCar>
    suspend fun fetchOwnedCarIds(userId: String): Set<String>
    suspend fun fetchShopGifts(): List<ShopGift>
    suspend fun fetchReceivedGifts(userId: String, limit: Int = 100): List<ReceivedGift>
    suspend fun fetchTransactions(userId: String, limit: Int = 200): List<CurrencyTransaction>
    suspend fun fetchWealthLeaderboard(limit: Int = 3): List<WealthLeaderboardEntry>

    suspend fun claimDailyReward(): Result<DailyRewardResult>
    suspend fun claimAdReward(): Result<AdRewardResult>
    suspend fun getAccumulatorStatus(): Result<AccumulatorStatus>
    suspend fun claimAccumulatedCoins(): Result<AccumulatorClaimResult>
    suspend fun purchaseCar(carId: String): Result<Unit>
    suspend fun selectOwnedCar(carId: String?): Result<Unit>
    suspend fun sendGift(recipientId: String, giftId: String): Result<Unit>
    suspend fun claimReceivedGift(giftRecordId: Long): Result<Long>
    /** يبدّل عملات داخل التطبيق مقابل ألماس (1000 عملة = 1 ألماسة) — لا أموال حقيقية إطلاقًا. */
    suspend fun exchangeCoinsForDiamonds(diamonds: Int): Result<CoinsToDiamondsExchangeResult>
}

class SupabaseCurrencyServiceImpl(
    private val authService: SupabaseAuthService
) : SupabaseCurrencyService {

    private val restBaseUrl get() = "${SupabaseConfig.getSupabaseUrl()}/rest/v1"

    private suspend fun authHeaders() =
        SupabaseHttp.baseHeaders(authService.currentValidAccessToken())

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    override suspend fun fetchOwnedCarIds(userId: String): Set<String> {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.USER_CARS}?user_id=eq.${enc(userId)}&select=car_id"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val array = JSONArray(SupabaseHttp.execute(request))
        return (0 until array.length()).map { array.getJSONObject(it).getString("car_id") }.toSet()
    }

    override suspend fun fetchShopCars(ownedCarIds: Set<String>, selectedCarId: String?): List<ShopCar> {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.SHOP_CARS}?select=*&order=sort_order.asc"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val array = JSONArray(SupabaseHttp.execute(request))
        return (0 until array.length()).map {
            SupabaseMappers.shopCarFromJson(array.getJSONObject(it), ownedCarIds, selectedCarId)
        }
    }

    override suspend fun fetchShopGifts(): List<ShopGift> {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.SHOP_GIFTS}?select=*&order=sort_order.asc"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val array = JSONArray(SupabaseHttp.execute(request))
        return (0 until array.length()).map { SupabaseMappers.shopGiftFromJson(array.getJSONObject(it)) }
    }

    override suspend fun fetchReceivedGifts(userId: String, limit: Int): List<ReceivedGift> {
        val select = "select=*,sender:users!sender_id(name,avatar_url),gift:shop_gifts!gift_id(name,icon)"
        val url = "$restBaseUrl/${SupabaseConfig.Tables.USER_GIFTS_RECEIVED}" +
            "?recipient_id=eq.${enc(userId)}&$select&order=created_at.desc&limit=$limit"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val array = JSONArray(SupabaseHttp.execute(request))
        return (0 until array.length()).map { SupabaseMappers.receivedGiftFromJson(array.getJSONObject(it)) }
    }

    override suspend fun fetchTransactions(userId: String, limit: Int): List<CurrencyTransaction> {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.CURRENCY_TRANSACTIONS}" +
            "?user_id=eq.${enc(userId)}&select=*&order=created_at.desc&limit=$limit"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val array = JSONArray(SupabaseHttp.execute(request))
        return (0 until array.length()).map { SupabaseMappers.currencyTransactionFromJson(array.getJSONObject(it)) }
    }

    override suspend fun fetchWealthLeaderboard(limit: Int): List<WealthLeaderboardEntry> {
        val url = "$restBaseUrl/${SupabaseConfig.Views.CURRENCY_LEADERBOARD}?select=*&limit=$limit"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val array = JSONArray(SupabaseHttp.execute(request))
        return (0 until array.length()).map { SupabaseMappers.wealthLeaderboardEntryFromJson(array.getJSONObject(it)) }
    }

    // ============================== RPC ====================================

    override suspend fun claimDailyReward(): Result<DailyRewardResult> = runCatchingRpc("claim_daily_reward") { json ->
        DailyRewardResult(
            reward = json.getInt("reward"),
            streak = json.getInt("streak"),
            newCoinsBalance = json.getLong("coins")
        )
    }

    override suspend fun claimAdReward(): Result<AdRewardResult> = runCatchingRpc("claim_ad_reward") { json ->
        AdRewardResult(
            reward = json.getInt("reward"),
            remainingToday = json.getInt("remaining_today"),
            newCoinsBalance = json.getLong("coins")
        )
    }

    override suspend fun getAccumulatorStatus(): Result<AccumulatorStatus> =
        runCatchingRpc("get_accumulator_status") { json ->
            AccumulatorStatus(
                startedAtEpochMillis = parseIsoToMillis(json.optString("started_at", null)),
                serverNowEpochMillis = parseIsoToMillis(json.optString("server_now", null))
            )
        }

    override suspend fun claimAccumulatedCoins(): Result<AccumulatorClaimResult> =
        runCatchingRpc("claim_accumulated_coins") { json ->
            AccumulatorClaimResult(
                reward = json.getLong("reward"),
                ticks = json.getInt("ticks"),
                newCoinsBalance = json.getLong("coins")
            )
        }

    override suspend fun purchaseCar(carId: String): Result<Unit> =
        callRpcWithPayload("purchase_car", JSONObject().put("p_car_id", carId))

    override suspend fun selectOwnedCar(carId: String?): Result<Unit> =
        callRpcWithPayload("select_owned_car", JSONObject().put("p_car_id", carId ?: JSONObject.NULL))

    override suspend fun sendGift(recipientId: String, giftId: String): Result<Unit> =
        callRpcWithPayload(
            "send_gift",
            JSONObject().put("p_recipient_id", recipientId).put("p_gift_id", giftId)
        )

    override suspend fun claimReceivedGift(giftRecordId: Long): Result<Long> =
        runCatchingRpc(
            "claim_received_gift",
            JSONObject().put("p_gift_record_id", giftRecordId)
        ) { json -> json.getLong("reward") }

    override suspend fun exchangeCoinsForDiamonds(diamonds: Int): Result<CoinsToDiamondsExchangeResult> =
        runCatchingRpc(
            "exchange_coins_for_diamonds",
            JSONObject().put("p_diamonds", diamonds)
        ) { json ->
            CoinsToDiamondsExchangeResult(
                newCoinsBalance = json.getLong("coins"),
                newDiamondsBalance = json.getLong("diamonds"),
                coinsSpent = json.getLong("spent")
            )
        }

    private suspend fun <T> runCatchingRpc(
        functionName: String,
        payload: JSONObject = JSONObject(),
        parse: (JSONObject) -> T
    ): Result<T> = try {
        val url = "$restBaseUrl/rpc/$functionName"
        val request = Request.Builder()
            .url(url)
            .headers(authHeaders().build())
            .post(SupabaseHttp.jsonBody(payload))
            .build()
        val body = SupabaseHttp.execute(request)
        Result.success(parse(JSONObject(body)))
    } catch (e: Exception) {
        Result.failure(mapCurrencyError(e))
    }

    private suspend fun callRpcWithPayload(functionName: String, payload: JSONObject): Result<Unit> = try {
        val url = "$restBaseUrl/rpc/$functionName"
        val request = Request.Builder()
            .url(url)
            .headers(authHeaders().add("Prefer", "return=minimal").build())
            .post(SupabaseHttp.jsonBody(payload))
            .build()
        SupabaseHttp.execute(request)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(mapCurrencyError(e))
    }

    /**
     * يحوّل أكواد أخطاء RPC الخام (مثل insufficient_diamonds) إلى استثناء
     * برسالة عربية واضحة — تُعرَض في الواجهة مباشرة بدل رسائل تقنية
     * (permission denied / RPC error / undefined).
     */
    private fun mapCurrencyError(e: Exception): Exception {
        val raw = e.message ?: ""
        val friendly = when {
            raw.contains("insufficient_diamonds") -> "رصيدك من الألماس غير كافٍ لإتمام هذه العملية"
            raw.contains("insufficient_coins") -> "رصيدك من العملات غير كافٍ لإتمام هذا التبديل"
            raw.contains("invalid_amount") -> "الكمية المطلوبة غير صحيحة"
            raw.contains("daily_reward_already_claimed") -> "لقد استلمت مكافأتك اليومية بالفعل، عد لاحقًا"
            raw.contains("ad_reward_limit_reached") -> "وصلت للحد الأقصى من مكافآت الإعلانات (3 يوميًا)، عد غدًا"
            raw.contains("accumulator_not_ready") -> "لم يمر وقت كافٍ بعد لسحب العملات المتراكمة"
            raw.contains("car_already_owned") -> "أنت تمتلك هذه السيارة بالفعل"
            raw.contains("car_not_owned") -> "لا يمكنك اختيار سيارة لا تملكها"
            raw.contains("car_not_found") -> "هذه السيارة غير متاحة"
            raw.contains("gift_not_found") -> "هذه الهدية غير متاحة"
            raw.contains("gift_already_claimed") -> "تم استلام هذه الهدية من قبل"
            raw.contains("recipient_not_found") -> "المستخدم المستلم غير موجود"
            raw.contains("cannot_gift_self") -> "لا يمكنك إرسال هدية لنفسك"
            raw.contains("not_authenticated") || raw.contains("not_authorized") ->
                "يرجى تسجيل الدخول مرة أخرى للمتابعة"
            e is SupabaseApiException -> "تعذّر إتمام العملية، حاول مرة أخرى"
            else -> "تعذّر الاتصال بالخادم، تحقق من الإنترنت وحاول مرة أخرى"
        }
        return IllegalStateException(friendly, e)
    }

    private fun parseIsoToMillis(isoTimestamp: String?): Long {
        if (isoTimestamp.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            var normalized = isoTimestamp.substringBefore("+").trimEnd('Z')
            val dotIndex = normalized.indexOf('.')
            if (dotIndex != -1) {
                val fraction = normalized.substring(dotIndex + 1).take(3).padEnd(3, '0')
                normalized = normalized.substring(0, dotIndex) + "." + fraction
            }
            val pattern = if (dotIndex != -1) "yyyy-MM-dd'T'HH:mm:ss.SSS" else "yyyy-MM-dd'T'HH:mm:ss"
            val format = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            format.parse(normalized)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
