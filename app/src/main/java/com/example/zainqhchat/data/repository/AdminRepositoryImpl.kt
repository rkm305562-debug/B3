package com.example.zainqhchat.data.repository

import com.example.zainqhchat.data.remote.supabase.SupabaseDatabaseService
import com.example.zainqhchat.data.remote.supabase.SupabaseRealtimeService
import com.example.zainqhchat.data.remote.supabase.SupabaseStorageService
import com.example.zainqhchat.domain.model.AdminActionLogEntry
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.repository.AdminRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class AdminRepositoryImpl(
    private val databaseService: SupabaseDatabaseService,
    private val storageService: SupabaseStorageService,
    private val realtimeService: SupabaseRealtimeService
) : AdminRepository {

    override fun observePublicMessagesForModeration(): Flow<List<ChatMessage>> = callbackFlow {
        realtimeService.acquire()
        val current = LinkedHashMap<String, ChatMessage>()

        try {
            databaseService.fetchPublicMessages(limit = 200).forEach { current[it.id] = it }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
        trySend(current.values.sortedByDescending { it.timestamp })

        val job = launch {
            realtimeService.messageChanges.collect { change ->
                if (!change.message.isPublic) return@collect
                when (change.type) {
                    "DELETE" -> current.remove(change.message.id)
                    else -> current[change.message.id] = change.message
                }
                trySend(current.values.sortedByDescending { it.timestamp })
            }
        }

        awaitClose {
            job.cancel()
            realtimeService.release()
        }
    }

    override suspend fun searchUsers(query: String?): List<User> =
        databaseService.fetchAllUsersForAdmin(query)

    override suspend fun setUserBanStatus(targetUserId: String, banned: Boolean, reason: String?): Result<Unit> =
        databaseService.adminSetBanStatus(targetUserId, banned, reason)

    override suspend fun setUserTempBan(targetUserId: String, hours: Int, reason: String?): Result<Unit> =
        databaseService.adminSetTempBan(targetUserId, hours, reason)

    override suspend fun deleteUser(targetUserId: String, reason: String?): Result<Unit> =
        databaseService.adminDeleteUser(targetUserId, reason)

    override suspend fun deletePublicMessage(messageId: String, imageUrl: String?, reason: String?): Result<Unit> {
        val result = databaseService.adminDeleteMessage(messageId, reason)
        // حذف الملف الفعلي من Storage (إن وُجدت صورة) — لا يُفشِل العملية
        // الأساسية إن تعذّر (مثلاً كانت الصورة مستضافة خارجيًا).
        if (result.isSuccess && !imageUrl.isNullOrBlank()) {
            storageService.deleteObjectByPublicUrl(imageUrl)
        }
        return result
    }

    override suspend fun removeUserAvatar(targetUserId: String, avatarUrl: String?, reason: String?): Result<Unit> {
        val result = databaseService.adminRemoveAvatar(targetUserId, reason)
        if (result.isSuccess && !avatarUrl.isNullOrBlank()) {
            storageService.deleteObjectByPublicUrl(avatarUrl)
        }
        return result
    }

    override suspend fun adjustUserPoints(targetUserId: String, delta: Int, reason: String?): Result<Unit> =
        databaseService.adminAdjustPoints(targetUserId, delta, reason)

    override suspend fun adjustUserCurrency(
        targetUserId: String,
        coinsDelta: Long,
        diamondsDelta: Long,
        reason: String?
    ): Result<Unit> = databaseService.adminAdjustCurrency(targetUserId, coinsDelta, diamondsDelta, reason)

    override suspend fun broadcastNotification(title: String, body: String?): Result<Int> =
        databaseService.adminBroadcastNotification(title, body)

    override suspend fun fetchActionLog(): List<AdminActionLogEntry> =
        databaseService.fetchAdminActionLog()
}
