package com.example.zainqhchat.data.remote.supabase

import com.example.zainqhchat.domain.model.AdminActionLogEntry
import com.example.zainqhchat.domain.model.AdminActionType
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.CurrencyKind
import com.example.zainqhchat.domain.model.CurrencyTransaction
import com.example.zainqhchat.domain.model.CurrencyTransactionType
import com.example.zainqhchat.domain.model.NotificationItem
import com.example.zainqhchat.domain.model.NotificationType
import com.example.zainqhchat.domain.model.ReceivedGift
import com.example.zainqhchat.domain.model.ShopCar
import com.example.zainqhchat.domain.model.ShopGift
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.model.UserTier
import com.example.zainqhchat.domain.model.WealthLeaderboardEntry
import org.json.JSONObject

/**
 * دوال تحويل حقيقية من JSON الراجع من Supabase (PostgREST أو Realtime) إلى
 * نماذج الـ Domain — مركزية لتفادي ازدواجية منطق التحويل بين الخدمتين.
 */
internal object SupabaseMappers {

    fun userFromJson(json: JSONObject): User = User(
        id = json.getString("id"),
        username = json.getString("username"),
        name = json.getString("name"),
        age = json.optInt("age", 18),
        avatarUrl = json.optStringOrNull("avatar_url"),
        // ملاحظة مهمة: "النقاط" المعروضة للمستخدم أصبحت مطابقة تمامًا لعدد
        // "العملات" (coins) بناءً على طلب صريح — بدل عمود points المستقل
        // (الذي يزداد +1 لكل رسالة عبر تريجر قاعدة البيانات ويبقى مستخدَمًا
        // داخليًا في points_transactions/leaderboard القديم). هذا يوحّد
        // الرقم الظاهر في كل الشاشات (الملف الشخصي، شارات الفئة، لوحة
        // المتصدرين) مع رصيد العملات الفعلي دون أي حاجة لتعديل قاعدة
        // البيانات أو كسر نظام العملات القائم.
        points = json.optLong("coins", 0L).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
        followerCount = json.optInt("follower_count", 0),
        followingCount = json.optInt("following_count", 0),
        isOnline = json.optBoolean("is_online", false),
        lastActiveTimestamp = json.optLong("last_active_timestamp", System.currentTimeMillis()),
        createdAtTimestamp = json.optLong("created_at_timestamp", System.currentTimeMillis()),
        role = json.optString("role", "user"),
        isBanned = json.optBoolean("is_banned", false),
        coins = json.optLong("coins", 0L),
        diamonds = json.optLong("diamonds", 0L),
        selectedCarId = json.optStringOrNull("selected_car_id")
    )

    fun messageFromJson(json: JSONObject): ChatMessage {
        val tier = try {
            UserTier.valueOf(json.optString("sender_tier_name", UserTier.NEW.name))
        } catch (e: Exception) {
            UserTier.NEW
        }
        return ChatMessage(
            id = json.getString("id"),
            senderId = json.getString("sender_id"),
            senderName = json.getString("sender_name"),
            senderAvatarUrl = json.optStringOrNull("sender_avatar_url"),
            senderTier = tier,
            recipientId = json.optStringOrNull("recipient_id"),
            isPublic = json.optBoolean("is_public", true),
            text = json.optString("text", ""),
            imageUrl = json.optStringOrNull("image_url"),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            isRead = json.optBoolean("is_read", false),
            readTimestamp = if (json.isNull("read_timestamp")) null else json.optLong("read_timestamp"),
            mentionedUserIds = json.optJSONArray("mentioned_user_ids")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        )
    }

    fun notificationFromJson(json: JSONObject): NotificationItem {
        val type = when (json.optString("type", "system")) {
            "message" -> NotificationType.MESSAGE
            "follow" -> NotificationType.FOLLOW
            else -> NotificationType.SYSTEM
        }
        return NotificationItem(
            id = json.optLong("id", 0L),
            userId = json.getString("user_id"),
            type = type,
            title = json.optString("title", ""),
            body = json.optStringOrNull("body"),
            relatedId = json.optStringOrNull("related_id"),
            actorId = json.optStringOrNull("actor_id"),
            isRead = json.optBoolean("is_read", false),
            createdAtMillis = parseTimestampMillis(json.optString("created_at", null))
        )
    }

    fun adminActionLogFromJson(json: JSONObject): AdminActionLogEntry {
        val actionType = when (json.optString("action_type")) {
            "ban_user" -> AdminActionType.BAN_USER
            "unban_user" -> AdminActionType.UNBAN_USER
            "temp_ban_user" -> AdminActionType.TEMP_BAN_USER
            "delete_message" -> AdminActionType.DELETE_MESSAGE
            "remove_avatar" -> AdminActionType.REMOVE_AVATAR
            "adjust_currency" -> AdminActionType.ADJUST_CURRENCY
            "delete_user" -> AdminActionType.DELETE_USER
            "broadcast_notification" -> AdminActionType.BROADCAST_NOTIFICATION
            else -> AdminActionType.ADJUST_POINTS
        }
        return AdminActionLogEntry(
            id = json.optLong("id", 0L),
            adminId = json.optStringOrNull("admin_id"),
            targetUserId = json.optStringOrNull("target_user_id"),
            actionType = actionType,
            reason = json.optStringOrNull("reason"),
            pointsDelta = if (json.isNull("points_delta")) null else json.optInt("points_delta"),
            coinsDelta = if (json.isNull("coins_delta")) null else json.optLong("coins_delta"),
            diamondsDelta = if (json.isNull("diamonds_delta")) null else json.optLong("diamonds_delta"),
            relatedMessageId = json.optStringOrNull("related_message_id"),
            createdAtMillis = parseTimestampMillis(json.optString("created_at", null))
        )
    }

    private fun parseTimestampMillis(isoTimestamp: String?): Long {
        if (isoTimestamp.isNullOrBlank()) return System.currentTimeMillis()
        // ملاحظة: minSdk = 24 لذا لا يمكن استخدام java.time.Instant (يتطلب API 26+
        // بدون Core Library Desugaring، وهو غير مُفعَّل في هذا المشروع).
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

    // ============================ قسم العملات ==============================

    fun shopCarFromJson(json: JSONObject, ownedCarIds: Set<String>, selectedCarId: String?): ShopCar {
        val id = json.getString("id")
        return ShopCar(
            id = id,
            name = json.getString("name"),
            tier = json.optInt("tier", 1),
            nominalValueUsd = json.optLong("nominal_value_usd", 0L),
            priceDiamonds = json.optInt("price_diamonds", 0),
            icon = json.optString("icon", "directions_car"),
            isOwned = ownedCarIds.contains(id),
            isSelected = selectedCarId == id
        )
    }

    fun shopGiftFromJson(json: JSONObject): ShopGift = ShopGift(
        id = json.getString("id"),
        name = json.getString("name"),
        icon = json.optString("icon", "card_giftcard"),
        priceDiamonds = json.optInt("price_diamonds", 0),
        coinValue = json.optInt("coin_value", 0)
    )

    fun receivedGiftFromJson(json: JSONObject): ReceivedGift {
        val sender = json.optJSONObject("sender")
        val gift = json.optJSONObject("gift")
        return ReceivedGift(
            id = json.optLong("id", 0L),
            senderId = json.getString("sender_id"),
            senderName = sender?.optString("name") ?: "مستخدم",
            senderAvatarUrl = sender?.optStringOrNull("avatar_url"),
            giftId = json.getString("gift_id"),
            giftName = gift?.optString("name") ?: "",
            giftIcon = gift?.optString("icon") ?: "card_giftcard",
            coinValue = json.optInt("coin_value", 0),
            isClaimed = json.optString("status", "pending") == "claimed",
            createdAtTimestamp = parseTimestampMillis(json.optString("created_at", null))
        )
    }

    fun currencyTransactionFromJson(json: JSONObject): CurrencyTransaction = CurrencyTransaction(
        id = json.optLong("id", 0L),
        type = CurrencyTransactionType.fromRaw(json.optString("type", "admin_adjustment")),
        currency = if (json.optString("currency") == "diamonds") CurrencyKind.DIAMONDS else CurrencyKind.COINS,
        amount = json.optLong("amount", 0L),
        balanceAfter = json.optLong("balance_after", 0L),
        description = json.optString("description", ""),
        createdAtTimestamp = parseTimestampMillis(json.optString("created_at", null))
    )

    fun wealthLeaderboardEntryFromJson(json: JSONObject): WealthLeaderboardEntry = WealthLeaderboardEntry(
        id = json.getString("id"),
        username = json.getString("username"),
        name = json.getString("name"),
        avatarUrl = json.optStringOrNull("avatar_url"),
        coins = json.optLong("coins", 0L),
        diamonds = json.optLong("diamonds", 0L),
        carsValue = json.optLong("cars_value", 0L),
        wealthScore = json.optLong("wealth_score", 0L),
        selectedCarId = json.optStringOrNull("selected_car_id")
    )

    fun featureFlagFromJson(json: JSONObject) = com.example.zainqhchat.domain.model.FeatureFlag(
        sectionKey = json.optString("section_key"),
        isEnabled = json.optBoolean("is_enabled", true),
        disabledReason = json.optStringOrNull("disabled_reason")
    )

    // trim() هنا يحمي من أي مسافات/أسطر جديدة زائدة قد تُدرَج بالخطأ ضمن
    // القيمة من طرف عميل آخر غير هذا التطبيق (مثل عميل ويب HTML يتشارك
    // نفس قاعدة البيانات) — وهي مسافات لا تمنع المتصفح من عرض الصورة عادة
    // (يتسامح معها HTML)، لكنها قد تُفسد الرابط تمامًا عند تحميله مباشرة
    // عبر مكتبة صور أندرويد (Coil) التي تتطلب رابطًا نظيفًا بلا حشو.
    fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else getString(key).trim().ifBlank { null }
}
