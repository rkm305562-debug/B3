package com.example.zainqhchat.domain.model

/**
 * نموذج بيانات المستخدم
 */
data class User(
    val id: String,
    val username: String,
    val name: String,
    val age: Int,
    val avatarUrl: String? = null,
    val points: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val isOnline: Boolean = true,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val isFollowedByCurrentUser: Boolean = false,
    val isBlockedByCurrentUser: Boolean = false,
    val role: String = "user",
    val isBanned: Boolean = false,
    // قسم العملات — راجع domain/model/CurrencyModels.kt
    val coins: Long = 0,
    val diamonds: Long = 0,
    val selectedCarId: String? = null
) {
    val tier: UserTier
        get() = UserTier.fromPoints(points)

    /**
     * ملاحظة أمان مهمة: لا نثق بعمود is_online وحده أبدًا هنا. عند إغلاق
     * التطبيق فجأة (تصغيره أو قتل النظام له) قد لا يكتمل طلب "تعليم غير
     * متصل" (setOnlineStatus(false) في onStop بـ MainActivity) قبل تعليق
     * العملية، فيبقى is_online=true خطأً لفترة طويلة. لذلك نعتمد دائمًا على
     * حداثة last_active_timestamp الفعلية (نفس منطق isReallyOnline في
     * PresenceUtils.kt) بدل الوثوق بالعلم وحده — وهذا ما كان يجعل أي مستخدم
     * يظهر "متصل الآن" إلى الأبد فور أول مرة يتصل فيها.
     */
    val onlineCategory: OnlineStatusCategory
        get() = if (isReallyOnline()) OnlineStatusCategory.ONLINE_NOW else OnlineStatusCategory.fromLastActive(lastActiveTimestamp)

    val isAdmin: Boolean
        get() = role == "admin"
}
