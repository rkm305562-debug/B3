package com.example.zainqhchat.domain.model

enum class AdminActionType {
    BAN_USER, UNBAN_USER, TEMP_BAN_USER, DELETE_MESSAGE, REMOVE_AVATAR,
    ADJUST_POINTS, ADJUST_CURRENCY, DELETE_USER, BROADCAST_NOTIFICATION
}

data class AdminActionLogEntry(
    val id: Long,
    val adminId: String?,
    val targetUserId: String?,
    val actionType: AdminActionType,
    val reason: String?,
    val pointsDelta: Int?,
    val coinsDelta: Long? = null,
    val diamondsDelta: Long? = null,
    val relatedMessageId: String?,
    val createdAtMillis: Long
)
