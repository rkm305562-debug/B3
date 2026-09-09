package com.example.zainqhchat.domain.model

/**
 * نموذج رسالة الدردشة (الدردشة العامة والرسائل الخاصة)
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val senderTier: UserTier = UserTier.NEW,
    val recipientId: String? = null, // null للدردشة العامة
    val isPublic: Boolean = true,
    val text: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readTimestamp: Long? = null,
    /** معرّفات المستخدمين المُشار إليهم (@) في هذه الرسالة — للإشعارات الحقيقية في الدردشة العامة. */
    val mentionedUserIds: List<String> = emptyList()
)
