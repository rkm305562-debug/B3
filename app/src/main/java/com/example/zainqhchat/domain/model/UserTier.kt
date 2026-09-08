package com.example.zainqhchat.domain.model

import androidx.compose.ui.graphics.Color

/**
 * فئات المستخدمين حسب النقاط المكتسبة داخل التطبيق
 */
enum class UserTier(
    val titleArabic: String,
    val minPoints: Int,
    val badgeSymbol: String,
    val hexColor: Long
) {
    NEW("جديد", 0, "", 0xFF8E8E93),
    MEMBER("عضو", 100, "⭐", 0xFF4CD964),
    SENIOR_MEMBER("عضو قديم", 300, "🛡️", 0xFF007AFF),
    SPECIAL("مميز", 600, "💎", 0xFF5856D6),
    VIP("VIP", 1000, "👑", 0xFFFFD700),
    LEGEND("أسطورة", 2500, "🔥", 0xFFFF3B30);

    companion object {
        fun fromPoints(points: Int): UserTier {
            return entries.sortedByDescending { it.minPoints }
                .firstOrNull { points >= it.minPoints } ?: NEW
        }
    }
}
