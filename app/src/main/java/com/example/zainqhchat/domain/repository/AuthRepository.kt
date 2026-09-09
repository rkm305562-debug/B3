package com.example.zainqhchat.domain.repository

import com.example.zainqhchat.domain.model.NewAvatar
import com.example.zainqhchat.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * واجهة المصادقة - مربوطة فعليًا بـ Supabase Auth (GoTrue)
 */
interface AuthRepository {
    val currentUserFlow: Flow<User?>

    suspend fun getCurrentUser(): User?

    /**
     * تسجيل حساب جديد باسم مستخدم وكلمة مرور ورقم عمر وصورة شخصية اختيارية
     * (تُرفع فعليًا إلى Supabase Storage بعد إنشاء الجلسة).
     */
    suspend fun registerUser(
        username: String,
        password: String,
        name: String,
        age: Int,
        avatar: NewAvatar?
    ): Result<User>

    /**
     * تسجيل الدخول باسم المستخدم وكلمة المرور
     */
    suspend fun loginUser(
        username: String,
        password: String
    ): Result<User>

    suspend fun logout()

    /**
     * يضيف/يغيّر كلمة مرور حقيقية للحساب الحالي — يُستخدم من الإعدادات كي
     * يستطيع من سجّل باسم فقط (بدون كلمة مرور اختارها هو) أن يضمن إمكانية
     * الدخول لاحقًا من جهاز آخر أو بعد حذف التطبيق.
     */
    suspend fun setPassword(newPassword: String): Result<Unit>

    suspend fun isLoggedIn(): Boolean

    /**
     * يُحدّث حالة الاتصال (is_online) وطابع آخر نشاط (last_active_timestamp)
     * للمستخدم الحالي فورًا — يُستدعى من دورة حياة الشاشة الرئيسية
     * (onStart/onStop) ومن نبضة دورية أثناء بقاء التطبيق في المقدمة، بحيث
     * يعكس "متصل الآن" و"آخر ظهور" الواقع الفعلي بدل الاعتماد فقط على
     * لحظة تسجيل الدخول. راجع MainActivity.kt.
     */
    suspend fun setOnlineStatus(isOnline: Boolean)

    /**
     * حذف نهائي وحقيقي للحساب (Supabase Auth + كل بيانات الملف الشخصي
     * والدردشات والمتابعات المرتبطة به). إجراء لا يمكن التراجع عنه.
     */
    suspend fun deleteAccount(): Result<Unit>
}
