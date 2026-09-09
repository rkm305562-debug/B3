package com.example.zainqhchat.domain.model

/**
 * صورة شخصية جديدة تم اختيارها من الجهاز، جاهزة للرفع إلى Supabase Storage.
 * (نوع من طبقة Domain نفسها، بدون أي اعتماد على Android/Uri، حفاظًا على نظافة المعمارية)
 */
data class NewAvatar(
    val bytes: ByteArray,
    val mimeType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NewAvatar) return false
        return bytes.contentEquals(other.bytes) && mimeType == other.mimeType
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + mimeType.hashCode()
}

/**
 * الحالات الممكنة عند تحديث صورة الملف الشخصي.
 */
sealed interface AvatarUpdate {
    /** لم يغيّر المستخدم الصورة. */
    data object Unchanged : AvatarUpdate

    /** طلب المستخدم إزالة الصورة الحالية. */
    data object Removed : AvatarUpdate

    /** اختار المستخدم صورة جديدة يجب رفعها. */
    data class New(val avatar: NewAvatar) : AvatarUpdate
}
