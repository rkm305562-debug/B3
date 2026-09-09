package com.example.zainqhchat.data.local.entities

import androidx.room.Entity

@Entity(tableName = "follows", primaryKeys = ["followerId", "followingId"])
data class FollowEntity(
    val followerId: String,
    val followingId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_users", primaryKeys = ["blockerId", "blockedUserId"])
data class BlockedUserEntity(
    val blockerId: String,
    val blockedUserId: String,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
