package com.example.zainqhchat.domain.repository

import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.ChatPreview
import com.example.zainqhchat.domain.model.UserTier
import kotlinx.coroutines.flow.Flow

/**
 * واجهة المحادثات والدردشة العامة والخاصة
 */
interface ChatRepository {
    fun getPublicMessagesFlow(): Flow<List<ChatMessage>>

    fun getDirectMessagesFlow(currentUserId: String, otherUserId: String): Flow<List<ChatMessage>>

    fun getChatPreviewsFlow(currentUserId: String): Flow<List<ChatPreview>>

    suspend fun sendMessage(
        senderId: String,
        senderName: String,
        senderAvatarUrl: String?,
        senderTier: UserTier,
        recipientId: String?, // null للدردشة العامة
        text: String,
        imageUrl: String? = null,
        mentionedUserIds: List<String> = emptyList()
    ): Result<ChatMessage>

    suspend fun markMessagesAsRead(currentUserId: String, senderId: String)

    /** يحذف رسالة يملكها المستخدم الحالي (يعمل في الدردشة العامة والخاصة على حدٍّ سواء). */
    suspend fun deleteOwnMessage(messageId: String): Result<Unit>
}
