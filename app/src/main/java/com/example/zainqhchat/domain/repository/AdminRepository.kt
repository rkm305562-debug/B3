package com.example.zainqhchat.domain.repository

import com.example.zainqhchat.domain.model.AdminActionLogEntry
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * واجهة عمليات المدير — كل عملية حساسة هنا محمية فعليًا من جهة قاعدة
 * البيانات (دوال RPC تتحقق من role='admin' على الخادم)، وليست حماية واجهة
 * فقط. راجع supabase/schema.sql (قسم 8ب) للتفاصيل الكاملة.
 */
interface AdminRepository {
    /** الرسائل الحية للدردشة العامة لمراقبتها (لحظيًا عبر نفس آلية Realtime المستخدمة أصلاً). */
    fun observePublicMessagesForModeration(): Flow<List<ChatMessage>>

    suspend fun searchUsers(query: String?): List<User>
    suspend fun setUserBanStatus(targetUserId: String, banned: Boolean, reason: String?): Result<Unit>
    suspend fun setUserTempBan(targetUserId: String, hours: Int, reason: String?): Result<Unit>
    suspend fun deleteUser(targetUserId: String, reason: String?): Result<Unit>
    suspend fun deletePublicMessage(messageId: String, imageUrl: String?, reason: String?): Result<Unit>
    suspend fun removeUserAvatar(targetUserId: String, avatarUrl: String?, reason: String?): Result<Unit>
    suspend fun adjustUserPoints(targetUserId: String, delta: Int, reason: String?): Result<Unit>
    suspend fun adjustUserCurrency(targetUserId: String, coinsDelta: Long, diamondsDelta: Long, reason: String?): Result<Unit>
    suspend fun broadcastNotification(title: String, body: String?): Result<Int>
    suspend fun fetchActionLog(): List<AdminActionLogEntry>
}
