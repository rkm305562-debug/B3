package com.example.zainqhchat.data.remote.supabase

import com.example.zainqhchat.domain.model.AdminActionLogEntry
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.NotificationItem
import com.example.zainqhchat.domain.model.User
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * واجهة الاستعلامات والعمليات على قاعدة بيانات PostgreSQL في Supabase (PostgREST).
 * تدير جداول: users, user_settings, chat_messages, follows, reports, blocks.
 * كل استعلام يمرّ عبر REST API الحقيقي لـ Supabase، ويخضع بالكامل لسياسات RLS
 * المعرّفة في supabase/schema.sql — لا يُستخدم مفتاح service_role إطلاقًا هنا،
 * فقط anon/publishable key + Authorization Bearer الخاص بالمستخدم المسجل دخوله.
 */
interface SupabaseDatabaseService {
    // Users Table
    suspend fun fetchUsersExcept(currentUserId: String): List<User>
    /** يجلب أول حساب مدير (role='admin') — لميزة "تواصل مع المدير". */
    suspend fun fetchAdminUser(): User?
    suspend fun fetchUserById(userId: String): User?
    suspend fun updateUserProfile(userId: String, name: String, age: Int, avatarUrl: String?): Result<Unit>
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit>

    /**
     * إدراج احتياطي (Self-heal) لصف المستخدم في جدول users إن لم يكن موجودًا بعد
     * (مثلاً بسبب تأخر التريجر `handle_new_auth_user` أو أي تعارض توقيت). آمن
     * تمامًا للاستدعاء حتى لو كان الصف موجودًا فعلاً: يستخدم "ignore-duplicates"
     * فلا يفشل ولا يكرر الصف عند وجود تعارض على المفتاح الأساسي (id).
     */
    suspend fun ensureUserProfileExists(userId: String, username: String, name: String, age: Int): Result<Unit>

    // Chat Messages Table
    suspend fun fetchPublicMessages(limit: Int = 100): List<ChatMessage>
    suspend fun fetchPrivateMessages(user1Id: String, user2Id: String, limit: Int = 100): List<ChatMessage>
    suspend fun sendMessage(message: ChatMessage): Result<ChatMessage>

    // Follows, Reports, Blocks Tables
    suspend fun followUser(followerId: String, followingId: String): Result<Unit>
    suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit>
    suspend fun isFollowing(followerId: String, followingId: String): Boolean
    suspend fun followingIds(followerId: String): Set<String>
    suspend fun reportUser(reporterId: String, reportedId: String, reason: String, messageId: String? = null): Result<Unit>
    suspend fun blockUser(blockerId: String, blockedUserId: String, reason: String): Result<Unit>
    suspend fun isBlocked(blockerId: String, blockedUserId: String): Boolean
    suspend fun blockedIds(blockerId: String): Set<String>

    // Batch fetch of all private messages involving a user (used to build chat previews)
    suspend fun fetchPrivateMessagesForUser(userId: String, limit: Int = 200): List<ChatMessage>
    suspend fun markMessagesRead(currentUserId: String, senderId: String): Result<Unit>

    // Notifications Table
    suspend fun fetchNotifications(userId: String, limit: Int = 50): List<NotificationItem>
    suspend fun markNotificationRead(notificationId: Long): Result<Unit>

    // ==================== نظام المدير (Admin) ====================
    // كل دالة هنا تستدعي دالة RPC حقيقية في قاعدة البيانات (SECURITY DEFINER)
    // تتحقق من صلاحية role='admin' للمستخدم الحالي من طرف الخادم في كل مرة —
    // وليست مجرد إخفاء أزرار في الواجهة. راجع supabase/schema.sql (قسم 8ب).

    /** كل المستخدمين (بلا استثناء المستخدم الحالي) مع بحث اختياري بالاسم/اسم المستخدم — لوحة إدارة المستخدمين. */
    suspend fun fetchAllUsersForAdmin(searchQuery: String? = null, limit: Int = 100): List<User>

    suspend fun adminSetBanStatus(targetUserId: String, banned: Boolean, reason: String?): Result<Unit>
    suspend fun adminSetTempBan(targetUserId: String, hours: Int, reason: String?): Result<Unit>
    suspend fun adminDeleteUser(targetUserId: String, reason: String?): Result<Unit>
    suspend fun adminDeleteMessage(messageId: String, reason: String?): Result<Unit>
    suspend fun adminRemoveAvatar(targetUserId: String, reason: String?): Result<Unit>
    suspend fun adminAdjustPoints(targetUserId: String, delta: Int, reason: String?): Result<Unit>
    suspend fun adminAdjustCurrency(targetUserId: String, coinsDelta: Long, diamondsDelta: Long, reason: String?): Result<Unit>
    suspend fun adminBroadcastNotification(title: String, body: String?): Result<Int>
    suspend fun fetchAdminActionLog(limit: Int = 100): List<AdminActionLogEntry>

    /**
     * حذف نهائي وحقيقي لحساب المستخدم الحالي عبر RPC حقيقي (`delete_own_account`
     * في supabase/schema.sql)، يعمل بصلاحيات المستخدم نفسه فقط (auth.uid())
     * بلا حاجة لمفتاح service_role إطلاقًا. يحذف صف public.users (وكل ما
     * يعتمد عليه عبر CASCADE: المتابعات، الحظر، البلاغات، الرسائل،
     * الإشعارات، سجل النقاط) ثم صف auth.users نفسه.
     */
    suspend fun deleteOwnAccount(): Result<Unit>
    /** يحذف رسالة يملكها المستخدم الحالي فعليًا (RLS تتحقق من sender_id = المستخدم الحالي). */
    suspend fun deleteOwnMessage(messageId: String): Result<Unit>
}

/**
 * تنفيذ حقيقي للربط المباشر مع Supabase PostgREST API عبر OkHttp.
 * [authService] يوفّر Access Token الحالي (ويجدده تلقائيًا) لإرفاقه بكل طلب
 * كي تُطبَّق سياسات RLS الصحيحة الخاصة بالمستخدم المسجل دخوله.
 */
class SupabaseDatabaseServiceImpl(
    private val authService: SupabaseAuthService
) : SupabaseDatabaseService {

    private val restBaseUrl get() = "${SupabaseConfig.getSupabaseUrl()}/rest/v1"

    private suspend fun authHeaders() =
        SupabaseHttp.baseHeaders(authService.currentValidAccessToken())

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    // ============================= USERS ==================================

    override suspend fun fetchUsersExcept(currentUserId: String): List<User> {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.USERS}?id=neq.${enc(currentUserId)}&select=*&order=is_online.desc,last_active_timestamp.desc"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        val array = JSONArray(body)
        return (0 until array.length()).map { SupabaseMappers.userFromJson(array.getJSONObject(it)) }
    }

    override suspend fun fetchAdminUser(): User? {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.USERS}?role=eq.admin&select=*&limit=1"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val array = JSONArray(SupabaseHttp.execute(request))
        if (array.length() == 0) return null
        return SupabaseMappers.userFromJson(array.getJSONObject(0))
    }

    override suspend fun fetchUserById(userId: String): User? {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.USERS}?id=eq.${enc(userId)}&select=*&limit=1"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        val array = JSONArray(body)
        if (array.length() == 0) return null
        return SupabaseMappers.userFromJson(array.getJSONObject(0))
    }

    override suspend fun updateUserProfile(userId: String, name: String, age: Int, avatarUrl: String?): Result<Unit> {
        return try {
            val payload = JSONObject().put("name", name).put("age", age)
            if (avatarUrl != null) payload.put("avatar_url", avatarUrl) else payload.put("avatar_url", JSONObject.NULL)

            val url = "$restBaseUrl/${SupabaseConfig.Tables.USERS}?id=eq.${enc(userId)}"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal").build())
                .patch(SupabaseHttp.jsonBody(payload))
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Result<Unit> {
        return try {
            val payload = JSONObject()
                .put("is_online", isOnline)
                .put("last_active_timestamp", System.currentTimeMillis())

            val url = "$restBaseUrl/${SupabaseConfig.Tables.USERS}?id=eq.${enc(userId)}"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal").build())
                .patch(SupabaseHttp.jsonBody(payload))
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ensureUserProfileExists(userId: String, username: String, name: String, age: Int): Result<Unit> {
        return try {
            val payload = JSONObject()
                .put("id", userId)
                .put("username", username)
                .put("name", name)
                .put("age", age)

            val url = "$restBaseUrl/${SupabaseConfig.Tables.USERS}"
            val request = Request.Builder()
                .url(url)
                // ignore-duplicates = ON CONFLICT (id) DO NOTHING؛ لا يحتاج صلاحية
                // UPDATE على أي عمود، وآمن للاستدعاء حتى لو أنشأ التريجر الصف بالفعل.
                .headers(authHeaders().add("Prefer", "return=minimal,resolution=ignore-duplicates").build())
                .post(SupabaseHttp.jsonBody(payload))
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================= CHAT MESSAGES ===============================

    override suspend fun fetchPublicMessages(limit: Int): List<ChatMessage> {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.CHAT_MESSAGES}" +
            "?is_public=eq.true&select=*&order=timestamp.desc&limit=$limit"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        val array = JSONArray(body)
        return (0 until array.length()).map { SupabaseMappers.messageFromJson(array.getJSONObject(it)) }.reversed()
    }

    override suspend fun fetchPrivateMessages(user1Id: String, user2Id: String, limit: Int): List<ChatMessage> {
        val filter = "or=(and(sender_id.eq.${enc(user1Id)},recipient_id.eq.${enc(user2Id)})," +
            "and(sender_id.eq.${enc(user2Id)},recipient_id.eq.${enc(user1Id)}))"
        val url = "$restBaseUrl/${SupabaseConfig.Tables.CHAT_MESSAGES}?is_public=eq.false&$filter&select=*&order=timestamp.desc&limit=$limit"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        val array = JSONArray(body)
        return (0 until array.length()).map { SupabaseMappers.messageFromJson(array.getJSONObject(it)) }.reversed()
    }

    override suspend fun sendMessage(message: ChatMessage): Result<ChatMessage> {
        return try {
            val payload = JSONObject()
                .put("id", message.id)
                .put("sender_id", message.senderId)
                .put("sender_name", message.senderName)
                .put("sender_avatar_url", message.senderAvatarUrl ?: JSONObject.NULL)
                .put("sender_tier_name", message.senderTier.name)
                .put("recipient_id", message.recipientId ?: JSONObject.NULL)
                .put("is_public", message.isPublic)
                .put("text", message.text)
                .put("image_url", message.imageUrl ?: JSONObject.NULL)
                .put("timestamp", message.timestamp)
                .put("is_read", message.isRead)
                .put("mentioned_user_ids", org.json.JSONArray(message.mentionedUserIds))

            val url = "$restBaseUrl/${SupabaseConfig.Tables.CHAT_MESSAGES}"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=representation").build())
                .post(SupabaseHttp.jsonBody(payload))
                .build()
            val body = SupabaseHttp.execute(request)
            val array = JSONArray(body)
            Result.success(if (array.length() > 0) SupabaseMappers.messageFromJson(array.getJSONObject(0)) else message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================ FOLLOWS ==================================

    override suspend fun followUser(followerId: String, followingId: String): Result<Unit> {
        return try {
            val payload = JSONObject()
                .put("follower_id", followerId)
                .put("following_id", followingId)
            val url = "$restBaseUrl/${SupabaseConfig.Tables.FOLLOWS}"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal,resolution=ignore-duplicates").build())
                .post(SupabaseHttp.jsonBody(payload))
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> {
        return try {
            val url = "$restBaseUrl/${SupabaseConfig.Tables.FOLLOWS}?follower_id=eq.${enc(followerId)}&following_id=eq.${enc(followingId)}"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal").build())
                .delete()
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFollowing(followerId: String, followingId: String): Boolean {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.FOLLOWS}?follower_id=eq.${enc(followerId)}&following_id=eq.${enc(followingId)}&select=follower_id&limit=1"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        return JSONArray(body).length() > 0
    }

    override suspend fun followingIds(followerId: String): Set<String> {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.FOLLOWS}?follower_id=eq.${enc(followerId)}&select=following_id"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        val array = JSONArray(body)
        return (0 until array.length()).map { array.getJSONObject(it).getString("following_id") }.toSet()
    }

    // ============================ BLOCKS ===================================

    override suspend fun blockUser(blockerId: String, blockedUserId: String, reason: String): Result<Unit> {
        return try {
            val payload = JSONObject()
                .put("blocker_id", blockerId)
                .put("blocked_user_id", blockedUserId)
                .put("reason", reason)
            val url = "$restBaseUrl/${SupabaseConfig.Tables.BLOCKS}"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal,resolution=merge-duplicates").build())
                .post(SupabaseHttp.jsonBody(payload))
                .build()
            SupabaseHttp.execute(request)
            // الحظر يُلغي المتابعة تلقائيًا (نفس السلوك المطلوب سابقًا مع Room)
            unfollowUser(blockerId, blockedUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isBlocked(blockerId: String, blockedUserId: String): Boolean {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.BLOCKS}?blocker_id=eq.${enc(blockerId)}&blocked_user_id=eq.${enc(blockedUserId)}&select=blocker_id&limit=1"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        return JSONArray(body).length() > 0
    }

    override suspend fun blockedIds(blockerId: String): Set<String> {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.BLOCKS}?blocker_id=eq.${enc(blockerId)}&select=blocked_user_id"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        val array = JSONArray(body)
        return (0 until array.length()).map { array.getJSONObject(it).getString("blocked_user_id") }.toSet()
    }

    // ============================ REPORTS ==================================

    override suspend fun reportUser(reporterId: String, reportedId: String, reason: String, messageId: String?): Result<Unit> {
        return try {
            val payload = JSONObject()
                .put("reporter_id", reporterId)
                .put("reported_id", reportedId)
                .put("reason", reason)
            if (messageId != null) payload.put("message_id", messageId)
            val url = "$restBaseUrl/${SupabaseConfig.Tables.REPORTS}"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal").build())
                .post(SupabaseHttp.jsonBody(payload))
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ====================== PRIVATE CONVERSATIONS (BATCH) ==================

    override suspend fun fetchPrivateMessagesForUser(userId: String, limit: Int): List<ChatMessage> {
        val filter = "or=(sender_id.eq.${enc(userId)},recipient_id.eq.${enc(userId)})"
        val url = "$restBaseUrl/${SupabaseConfig.Tables.CHAT_MESSAGES}?is_public=eq.false&$filter&select=*&order=timestamp.desc&limit=$limit"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        val array = JSONArray(body)
        return (0 until array.length()).map { SupabaseMappers.messageFromJson(array.getJSONObject(it)) }
    }

    override suspend fun markMessagesRead(currentUserId: String, senderId: String): Result<Unit> {
        return try {
            val payload = JSONObject().put("is_read", true).put("read_timestamp", System.currentTimeMillis())
            val url = "$restBaseUrl/${SupabaseConfig.Tables.CHAT_MESSAGES}" +
                "?recipient_id=eq.${enc(currentUserId)}&sender_id=eq.${enc(senderId)}&is_read=eq.false"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal").build())
                .patch(SupabaseHttp.jsonBody(payload))
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================ NOTIFICATIONS =============================

    override suspend fun fetchNotifications(userId: String, limit: Int): List<NotificationItem> {
        val url = "$restBaseUrl/${SupabaseConfig.Tables.NOTIFICATIONS}?user_id=eq.${enc(userId)}&select=*&order=created_at.desc&limit=$limit"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        val array = JSONArray(body)
        return (0 until array.length()).map { SupabaseMappers.notificationFromJson(array.getJSONObject(it)) }
    }

    override suspend fun markNotificationRead(notificationId: Long): Result<Unit> {
        return try {
            val payload = JSONObject().put("is_read", true)
            val url = "$restBaseUrl/${SupabaseConfig.Tables.NOTIFICATIONS}?id=eq.$notificationId"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal").build())
                .patch(SupabaseHttp.jsonBody(payload))
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteOwnAccount(): Result<Unit> {
        return try {
            val url = "$restBaseUrl/rpc/delete_own_account"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal").build())
                .post(SupabaseHttp.jsonBody(JSONObject()))
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteOwnMessage(messageId: String): Result<Unit> {
        return try {
            val url = "$restBaseUrl/${SupabaseConfig.Tables.CHAT_MESSAGES}?id=eq.${enc(messageId)}"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal").build())
                .delete()
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== نظام المدير (Admin) ====================

    override suspend fun fetchAllUsersForAdmin(searchQuery: String?, limit: Int): List<User> {
        val filter = if (!searchQuery.isNullOrBlank()) {
            "&or=(name.ilike.*${enc(searchQuery.trim())}*,username.ilike.*${enc(searchQuery.trim())}*)"
        } else ""
        val url = "$restBaseUrl/${SupabaseConfig.Tables.USERS}?select=*$filter&order=created_at_timestamp.desc&limit=$limit"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        val array = JSONArray(body)
        return (0 until array.length()).map { SupabaseMappers.userFromJson(array.getJSONObject(it)) }
    }

    override suspend fun adminSetBanStatus(targetUserId: String, banned: Boolean, reason: String?): Result<Unit> =
        callAdminRpc(
            "admin_set_ban_status",
            JSONObject()
                .put("p_target_user_id", targetUserId)
                .put("p_banned", banned)
                .put("p_reason", reason ?: JSONObject.NULL)
        )

    override suspend fun adminSetTempBan(targetUserId: String, hours: Int, reason: String?): Result<Unit> =
        callAdminRpc(
            "admin_set_temp_ban",
            JSONObject()
                .put("p_target_user_id", targetUserId)
                .put("p_hours", hours)
                .put("p_reason", reason ?: JSONObject.NULL)
        )

    override suspend fun adminDeleteUser(targetUserId: String, reason: String?): Result<Unit> =
        callAdminRpc(
            "admin_delete_user",
            JSONObject()
                .put("p_target_user_id", targetUserId)
                .put("p_reason", reason ?: JSONObject.NULL)
        )

    override suspend fun adminDeleteMessage(messageId: String, reason: String?): Result<Unit> =
        callAdminRpc(
            "admin_delete_message",
            JSONObject()
                .put("p_message_id", messageId)
                .put("p_reason", reason ?: JSONObject.NULL)
        )

    override suspend fun adminRemoveAvatar(targetUserId: String, reason: String?): Result<Unit> =
        callAdminRpc(
            "admin_remove_avatar",
            JSONObject()
                .put("p_target_user_id", targetUserId)
                .put("p_reason", reason ?: JSONObject.NULL)
        )

    override suspend fun adminAdjustPoints(targetUserId: String, delta: Int, reason: String?): Result<Unit> =
        callAdminRpc(
            "admin_adjust_points",
            JSONObject()
                .put("p_target_user_id", targetUserId)
                .put("p_delta", delta)
                .put("p_reason", reason ?: JSONObject.NULL)
        )

    override suspend fun adminAdjustCurrency(
        targetUserId: String,
        coinsDelta: Long,
        diamondsDelta: Long,
        reason: String?
    ): Result<Unit> =
        callAdminRpc(
            "admin_adjust_currency",
            JSONObject()
                .put("p_target_user_id", targetUserId)
                .put("p_coins_delta", coinsDelta)
                .put("p_diamonds_delta", diamondsDelta)
                .put("p_reason", reason ?: JSONObject.NULL)
        )

    override suspend fun adminBroadcastNotification(title: String, body: String?): Result<Int> {
        return try {
            val url = "$restBaseUrl/rpc/admin_broadcast_notification"
            val payload = JSONObject()
                .put("p_title", title)
                .put("p_body", body ?: JSONObject.NULL)
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().build())
                .post(SupabaseHttp.jsonBody(payload))
                .build()
            val responseBody = SupabaseHttp.execute(request)
            val count = responseBody.trim().toIntOrNull() ?: 0
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchAdminActionLog(limit: Int): List<AdminActionLogEntry> {
        val url = "$restBaseUrl/admin_action_log?select=*&order=created_at.desc&limit=$limit"
        val request = Request.Builder().url(url).headers(authHeaders().build()).get().build()
        val body = SupabaseHttp.execute(request)
        val array = JSONArray(body)
        return (0 until array.length()).map { SupabaseMappers.adminActionLogFromJson(array.getJSONObject(it)) }
    }

    /**
     * كل دوال RPC الإدارية تُرجع خطأ حقيقي (رسالة "not authorized...") إن لم
     * يكن المستخدم الحالي فعليًا role='admin' في قاعدة البيانات — يتم التحقق
     * من ذلك داخل الدالة نفسها على الخادم (SECURITY DEFINER)، وليس هنا في
     * العميل، فلا يمكن لأي مستخدم عادي تجاوز هذا التحقق مهما فعل في التطبيق.
     */
    private suspend fun callAdminRpc(functionName: String, payload: JSONObject): Result<Unit> {
        return try {
            val url = "$restBaseUrl/rpc/$functionName"
            val request = Request.Builder()
                .url(url)
                .headers(authHeaders().add("Prefer", "return=minimal").build())
                .post(SupabaseHttp.jsonBody(payload))
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
