package com.example.zainqhchat.domain.model

/**
 * إعدادات التطبيق والمظهر
 */
data class UserSettings(
    val isDarkMode: Boolean = false,
    val primaryColorHex: Long = 0xFF0EA5E9, // الأزرق السماوي الحديث
    val notificationsEnabled: Boolean = true
)
