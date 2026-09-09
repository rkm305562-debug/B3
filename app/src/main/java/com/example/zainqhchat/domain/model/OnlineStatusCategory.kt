package com.example.zainqhchat.domain.model

/**
 * فئات حالة الاتصال المرتبة حسب الوقت
 */
enum class OnlineStatusCategory(
    val titleArabic: String,
    val maxMinutesAgo: Long
) {
    ONLINE_NOW("متصل الآن", 2),
    TWO_MINUTES_AGO("متصل منذ دقيقتين", 15),
    ONE_HOUR_AGO("متصل منذ ساعة", 120),
    SIX_HOURS_AGO("متصل منذ 6 ساعات", 720),
    ONE_DAY_AGO("متصل منذ يوم", 2880),
    SEVERAL_DAYS_AGO("متصل منذ عدة أيام", Long.MAX_VALUE);

    companion object {
        fun fromLastActive(lastActiveTimestamp: Long): OnlineStatusCategory {
            val diffMinutes = (System.currentTimeMillis() - lastActiveTimestamp) / (1000 * 60)
            return when {
                diffMinutes <= 2 -> ONLINE_NOW
                diffMinutes <= 15 -> TWO_MINUTES_AGO
                diffMinutes <= 120 -> ONE_HOUR_AGO
                diffMinutes <= 720 -> SIX_HOURS_AGO
                diffMinutes <= 2880 -> ONE_DAY_AGO
                else -> SEVERAL_DAYS_AGO
            }
        }
    }
}
