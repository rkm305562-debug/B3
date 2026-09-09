package com.example.zainqhchat.domain.repository

import com.example.zainqhchat.domain.model.AvatarUpdate
import com.example.zainqhchat.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * واجهة إدارة المستخدمين والملفات الشخصية والتواصل الاجتماعي والنقاط
 * مربوطة فعليًا بجداول Supabase (users, follows, blocks, reports).
 */
interface UserRepository {
    fun getUserByIdFlow(userId: String, currentUserId: String): Flow<User?>

    suspend fun getUserById(userId: String, currentUserId: String): User?

    fun getAllUsersFlow(currentUserId: String): Flow<List<User>>

    suspend fun updateProfile(userId: String, name: String, age: Int, avatar: AvatarUpdate): Result<Unit>

    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit>

    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit>

    fun isFollowingFlow(currentUserId: String, targetUserId: String): Flow<Boolean>

    suspend fun blockUser(currentUserId: String, targetUserId: String, reason: String = ""): Result<Unit>

    suspend fun reportUser(currentUserId: String, targetUserId: String, reason: String, messageId: String? = null): Result<Unit>

    suspend fun awardPoints(userId: String, points: Int): Result<Int>

    /** يجلب حساب المدير — لفتح محادثة "تواصل مع المدير" من أي مكان في التطبيق. */
    suspend fun fetchAdminUser(): User?
}
