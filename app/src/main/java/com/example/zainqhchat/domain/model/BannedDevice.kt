package com.example.zainqhchat.domain.model

/** جهاز محظور نهائيًا من التسجيل (صف من جدول banned_devices). */
data class BannedDevice(
    val deviceId: String,
    val bannedUserId: String?,
    val bannedUsername: String?,
    val reason: String?,
    val bannedAtMillis: Long
)
