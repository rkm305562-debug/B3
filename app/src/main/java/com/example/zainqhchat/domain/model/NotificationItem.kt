package com.example.zainqhchat.domain.model

enum class NotificationType { MESSAGE, FOLLOW, SYSTEM }

data class NotificationItem(
    val id: Long,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String?,
    val relatedId: String?,
    val actorId: String?,
    val isRead: Boolean,
    val createdAtMillis: Long
)
