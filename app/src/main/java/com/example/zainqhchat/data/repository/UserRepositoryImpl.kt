package com.example.zainqhchat.data.repository

import com.example.zainqhchat.data.local.LocalUserCache
import com.example.zainqhchat.data.remote.supabase.SupabaseDatabaseService
import com.example.zainqhchat.data.remote.supabase.SupabaseRealtimeService
import com.example.zainqhchat.data.remote.supabase.SupabaseStorageService
import com.example.zainqhchat.domain.model.AvatarUpdate
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * تنفيذ حقيقي لإدارة الملفات الشخصية والتواصل الاجتماعي (متابعة/حظر/بلاغ)
 * مباشرة عبر Supabase PostgREST + Realtime — بدون Room وبدون أي بيانات وهمية.
 *
 * `getAllUsersFlow` و `getUserByIdFlow` تُحدَّثان لحظيًا (بدون Polling) عبر
 * [SupabaseRealtimeService.userChanges] عند تغيّر أي عمود في صف مستخدم آخر —
 * بما في ذلك `is_online` و `last_active_timestamp` (قسم "المتصلون الآن" وآخر ظهور).
 */
class UserRepositoryImpl(
    private val databaseService: SupabaseDatabaseService,
    private val storageService: SupabaseStorageService,
    private val realtimeService: SupabaseRealtimeService,
    private val localUserCache: LocalUserCache
) : UserRepository {

    override fun getUserByIdFlow(userId: String, currentUserId: String): Flow<User?> = callbackFlow {
        realtimeService.acquire()
        var current: User? = try {
            getUserById(userId, currentUserId)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
        trySend(current)

        val job = launch {
            realtimeService.userChanges.collect { updated ->
                if (updated.id != userId) return@collect
                current = updated.copy(
                    isFollowedByCurrentUser = current?.isFollowedByCurrentUser ?: false,
                    isBlockedByCurrentUser = current?.isBlockedByCurrentUser ?: false
                )
                trySend(current)
            }
        }

        awaitClose {
            job.cancel()
            realtimeService.release()
        }
    }

    override suspend fun getUserById(userId: String, currentUserId: String): User? =
        withContext(Dispatchers.IO) {
            val user = databaseService.fetchUserById(userId) ?: return@withContext null
            val isFollowing = databaseService.isFollowing(currentUserId, userId)
            val isBlocked = databaseService.isBlocked(currentUserId, userId)
            user.copy(isFollowedByCurrentUser = isFollowing, isBlockedByCurrentUser = isBlocked)
        }

    override fun getAllUsersFlow(currentUserId: String): Flow<List<User>> = callbackFlow {
        realtimeService.acquire()

        var followingIds: Set<String>
        var blockedIds: Set<String>
        var users: List<User>
        try {
            users = databaseService.fetchUsersExcept(currentUserId)
            followingIds = databaseService.followingIds(currentUserId)
            blockedIds = databaseService.blockedIds(currentUserId)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        fun decorate() = users.map { user ->
            user.copy(
                isFollowedByCurrentUser = user.id in followingIds,
                isBlockedByCurrentUser = user.id in blockedIds
            )
        }

        trySend(decorate())

        val job = launch {
            realtimeService.userChanges.collect { updated ->
                if (updated.id == currentUserId) return@collect
                users = if (users.any { it.id == updated.id }) {
                    users.map { if (it.id == updated.id) updated else it }
                } else {
                    users + updated
                }
                trySend(decorate())
            }
        }

        awaitClose {
            job.cancel()
            realtimeService.release()
        }
    }

    override suspend fun updateProfile(
        userId: String,
        name: String,
        age: Int,
        avatar: AvatarUpdate
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val avatarUrl: String? = when (avatar) {
                is AvatarUpdate.Unchanged -> databaseService.fetchUserById(userId)?.avatarUrl
                is AvatarUpdate.Removed -> null
                is AvatarUpdate.New -> {
                    val uploaded = storageService.uploadAvatar(
                        userId,
                        avatar.avatar.bytes,
                        avatar.avatar.mimeType
                    ).getOrElse { return@withContext Result.failure(it) }
                    uploaded
                }
            }

            databaseService.updateUserProfile(userId, name.trim(), age, avatarUrl)
                .onSuccess {
                    databaseService.fetchUserById(userId)?.let { localUserCache.upsert(it) }
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            databaseService.followUser(currentUserId, targetUserId)
            // منح +5 نقاط لتشجيع التفاعل الاجتماعي، مطابقة لتريجر handle_follow_change في schema.sql
        }

    override suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            databaseService.unfollowUser(currentUserId, targetUserId)
        }

    override fun isFollowingFlow(currentUserId: String, targetUserId: String): Flow<Boolean> = flow {
        emit(databaseService.isFollowing(currentUserId, targetUserId))
    }

    override suspend fun blockUser(
        currentUserId: String,
        targetUserId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        databaseService.blockUser(currentUserId, targetUserId, reason)
    }

    override suspend fun reportUser(
        currentUserId: String,
        targetUserId: String,
        reason: String,
        messageId: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        databaseService.reportUser(currentUserId, targetUserId, reason, messageId)
    }

    /**
     * النقاط رقم "لا يُكتب من العميل" بتصميم متعمّد: تريجرات SECURITY DEFINER في
     * supabase/schema.sql (handle_new_message / handle_follow_change) هي الوحيدة
     * المخوّلة بتعديل عمود points (عبر صلاحيات GRANT UPDATE المقيّدة بالأعمدة على
     * جدول users). لذلك هذه الدالة للقراءة فقط وتُرجع الرصيد الفعلي الحالي، ولا
     * تحاول أي كتابة مباشرة كي لا تخالف نموذج الأمان.
     */
    override suspend fun awardPoints(userId: String, points: Int): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val user = databaseService.fetchUserById(userId)
                    ?: return@withContext Result.failure(IllegalStateException("المستخدم غير موجود"))
                Result.success(user.points)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun fetchAdminUser(): User? = withContext(Dispatchers.IO) {
        try {
            databaseService.fetchAdminUser()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun fetchFeatureFlags(): List<com.example.zainqhchat.domain.model.FeatureFlag> =
        withContext(Dispatchers.IO) {
            try {
                databaseService.fetchFeatureFlags()
            } catch (e: Exception) {
                emptyList()
            }
        }
}
