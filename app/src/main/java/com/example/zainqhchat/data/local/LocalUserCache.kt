package com.example.zainqhchat.data.local

import com.example.zainqhchat.data.local.dao.UserDao
import com.example.zainqhchat.data.local.entities.UserEntity
import com.example.zainqhchat.domain.model.User

/**
 * طبقة تخزين مؤقت محلي (Cache) فوق Room — وليست مصدر الحقيقة بعد الآن.
 *
 * بعد ربط المصادقة والملف الشخصي فعليًا بـ Supabase، أصبح Supabase هو مصدر
 * الحقيقة الوحيد لبيانات المستخدم وكلمة المرور والجلسة. هذا الكائن يبقي فقط
 * نسخة محلية محدَّثة من المستخدم الحالي داخل جدول `users` في Room، لأن شاشات
 * الدردشة (`ChatRepositoryImpl`) لم تُهاجَر بعد لـ Supabase (سيتم ذلك في
 * المرحلة القادمة) وما زالت تعتمد على هذا الجدول لعرض بيانات المرسل.
 *
 * لا تُقرأ كلمة المرور من هنا أبدًا ولا تُستخدم هذه النسخة لاتخاذ أي قرار
 * متعلق بالمصادقة (تسجيل الدخول/الخروج بالكامل عبر SupabaseAuthService).
 */
class LocalUserCache(private val userDao: UserDao) {

    suspend fun upsert(user: User) {
        userDao.insertUser(
            UserEntity(
                id = user.id,
                username = user.username,
                passwordHash = "", // غير مستخدمة بعد الآن؛ المصادقة عبر Supabase فقط
                name = user.name,
                age = user.age,
                avatarUrl = user.avatarUrl,
                points = user.points,
                followerCount = user.followerCount,
                followingCount = user.followingCount,
                isOnline = user.isOnline,
                lastActiveTimestamp = user.lastActiveTimestamp,
                createdAtTimestamp = user.createdAtTimestamp
            )
        )
    }
}
