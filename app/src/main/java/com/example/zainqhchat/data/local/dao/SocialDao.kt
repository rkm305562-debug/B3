package com.example.zainqhchat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zainqhchat.data.local.entities.BlockedUserEntity
import com.example.zainqhchat.data.local.entities.FollowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialDao {

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerId = :followerId AND followingId = :followingId)")
    fun isFollowingFlow(followerId: String, followingId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerId = :followerId AND followingId = :followingId)")
    suspend fun isFollowing(followerId: String, followingId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun followUser(follow: FollowEntity)

    @Query("DELETE FROM follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun unfollowUser(followerId: String, followingId: String)

    @Query("SELECT COUNT(*) FROM follows WHERE followingId = :userId")
    fun getFollowersCountFlow(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM follows WHERE followerId = :userId")
    fun getFollowingCountFlow(userId: String): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_users WHERE blockerId = :blockerId AND blockedUserId = :blockedUserId)")
    fun isBlockedFlow(blockerId: String, blockedUserId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockUser(blocked: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE blockerId = :blockerId AND blockedUserId = :blockedUserId")
    suspend fun unblockUser(blockerId: String, blockedUserId: String)
}
