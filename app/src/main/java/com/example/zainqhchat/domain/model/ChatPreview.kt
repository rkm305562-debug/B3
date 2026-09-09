package com.example.zainqhchat.domain.model

/**
 * معاينة قائمة الدردشات في الصفحة الرئيسية
 */
data class ChatPreview(
    val id: String,
    val title: String,
    val isPublic: Boolean,
    val avatarUrl: String? = null,
    val lastMessageText: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int = 0,
    val targetUserId: String? = null,
    val isOnline: Boolean = false,
    val userTier: UserTier = UserTier.NEW
)
