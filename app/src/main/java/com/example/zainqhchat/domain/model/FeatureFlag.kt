package com.example.zainqhchat.domain.model

/**
 * حالة قسم من أقسام التطبيق (مفعّل/مغلق مؤقتًا من طرف المدير). القراءة
 * متاحة للجميع (حتى الزوّار قبل تسجيل الدخول) لإظهار حالة الأقسام على
 * الصفحة الرئيسية فورًا؛ التعديل محصور بالمدير عبر admin_set_feature_flag.
 */
data class FeatureFlag(
    val sectionKey: String,
    val isEnabled: Boolean,
    val disabledReason: String?
)

/** مفاتيح الأقسام المعروفة — يجب أن تطابق بالضبط القيم المُدخلة في
 *  supabase/migrations/008_section_toggles_profanity_filter_rate_limit.sql */
object FeatureSections {
    const val CHATS = "chats"
    const val CURRENCY = "currency"
    const val CONTACT_ADMIN = "contact_admin"
    const val ONLINE_USERS = "online_users"
    const val PUBLIC_CHAT_LINK = "public_chat_link"
    const val SETTINGS = "settings"
}
