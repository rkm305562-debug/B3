package com.example.zainqhchat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.UserTier

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val senderTierName: String = UserTier.NEW.name,
    val recipientId: String? = null,
    val isPublic: Boolean = true,
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readTimestamp: Long? = null
) {
    fun toDomainModel(): ChatMessage {
        val tier = try {
            UserTier.valueOf(senderTierName)
        } catch (e: Exception) {
            UserTier.NEW
        }
        return ChatMessage(
            id = id,
            senderId = senderId,
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl,
            senderTier = tier,
            recipientId = recipientId,
            isPublic = isPublic,
            text = text,
            imageUrl = imageUrl,
            timestamp = timestamp,
            isRead = isRead,
            readTimestamp = readTimestamp
        )
    }

    companion object {
        fun fromDomainModel(message: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = message.id,
                senderId = message.senderId,
                senderName = message.senderName,
                senderAvatarUrl = message.senderAvatarUrl,
                senderTierName = message.senderTier.name,
                recipientId = message.recipientId,
                isPublic = message.isPublic,
                text = message.text,
                imageUrl = message.imageUrl,
                timestamp = message.timestamp,
                isRead = message.isRead,
                readTimestamp = message.readTimestamp
            )
        }
    }
}
