package com.example.zainqhchat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.zainqhchat.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id != :currentUserId ORDER BY isOnline DESC, lastActiveTimestamp DESC")
    fun getAllUsersExceptFlow(currentUserId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id != :currentUserId")
    suspend fun getAllUsersExcept(currentUserId: String): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET points = points + :addPoints WHERE id = :userId")
    suspend fun addPoints(userId: String, addPoints: Int)

    @Query("UPDATE users SET isOnline = :isOnline, lastActiveTimestamp = :timestamp WHERE id = :userId")
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean, timestamp: Long)

    @Query("UPDATE users SET name = :name, age = :age, avatarUrl = :avatarUrl WHERE id = :userId")
    suspend fun updateProfile(userId: String, name: String, age: Int, avatarUrl: String?)
}
