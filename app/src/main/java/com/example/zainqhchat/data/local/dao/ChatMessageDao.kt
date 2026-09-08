package com.example.zainqhchat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.zainqhchat.data.local.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE isPublic = 1 ORDER BY timestamp ASC")
    fun getPublicMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE isPublic = 0 AND ((senderId = :userId1 AND recipientId = :userId2) OR (senderId = :userId2 AND recipientId = :userId1)) ORDER BY timestamp ASC")
    fun getDirectMessagesFlow(userId1: String, userId2: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE isPublic = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPublicMessage(): ChatMessageEntity?

    @Query("SELECT * FROM chat_messages WHERE isPublic = 0 AND ((senderId = :userId1 AND recipientId = :userId2) OR (senderId = :userId2 AND recipientId = :userId1)) ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastDirectMessage(userId1: String, userId2: String): ChatMessageEntity?

    @Query("SELECT COUNT(*) FROM chat_messages WHERE recipientId = :currentUserId AND senderId = :otherUserId AND isRead = 0")
    fun getUnreadCountFlow(currentUserId: String, otherUserId: String): Flow<Int>

    @Query("UPDATE chat_messages SET isRead = 1, readTimestamp = :readTime WHERE recipientId = :currentUserId AND senderId = :otherUserId AND isRead = 0")
    suspend fun markMessagesAsRead(currentUserId: String, otherUserId: String, readTime: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
}
