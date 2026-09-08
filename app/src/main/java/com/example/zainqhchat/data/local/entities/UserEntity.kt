package com.example.zainqhchat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zainqhchat.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val passwordHash: String,
    val name: String,
    val age: Int,
    val avatarUrl: String? = null,
    val points: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isOnline: Boolean = true,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val createdAtTimestamp: Long = System.currentTimeMillis()
) {
    fun toDomainModel(isFollowed: Boolean = false, isBlocked: Boolean = false): User {
        return User(
            id = id,
            username = username,
            name = name,
            age = age,
            avatarUrl = avatarUrl,
            points = points,
            followerCount = followerCount,
            followingCount = followingCount,
            isOnline = isOnline,
            lastActiveTimestamp = lastActiveTimestamp,
            createdAtTimestamp = createdAtTimestamp,
            isFollowedByCurrentUser = isFollowed,
            isBlockedByCurrentUser = isBlocked
        )
    }

    companion object {
        fun fromDomainModel(user: User, passwordHash: String = ""): UserEntity {
            return UserEntity(
                id = user.id,
                username = user.username,
                passwordHash = passwordHash,
                name = user.name,
                age = user.age,
                avatarUrl = user.avatarUrl,
                points = user.points,
                followerCount = user.followerCount,
                followingCount = user.followingCount,
                isOnline = user.isOnline,
                lastActiveTimestamp = user.lastActiveTimestamp,
                createdAtTimestamp = user.createdAtTimestamp
            )
        }
    }
}
