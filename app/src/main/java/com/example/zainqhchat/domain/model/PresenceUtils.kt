package com.example.zainqhchat.domain.model

/**
 * "متصل الآن" الحقيقي لا يعتمد فقط على عمود is_online (الذي قد يبقى صحيحًا
 * خطأً إذا أُغلق التطبيق بالقوة قبل أن تُرسَل إشارة "غير متصل" — مثلاً عبر
 * قتل النظام للعملية مباشرة بدون استدعاء onStop). لذلك نعتبر المستخدم
 * متصلاً فعليًا فقط إذا كان is_online = true **و** آخر نبضة نشاط (heartbeat)
 * حديثة (خلال [ONLINE_STALE_THRESHOLD_MS]) — راجع MainActivity.kt لآلية
 * تحديث last_active_timestamp كل دقيقة أثناء تواجد التطبيق في المقدمة.
 */
const val ONLINE_STALE_THRESHOLD_MS = 90_000L // 90 ثانية (أكبر من فترة النبضة 60 ثانية بهامش أمان)

fun User.isReallyOnline(nowMillis: Long = System.currentTimeMillis()): Boolean =
    isOnline && (nowMillis - lastActiveTimestamp) < ONLINE_STALE_THRESHOLD_MS

/** نص "آخر ظهور" عربي مبني على last_active_timestamp الحقيقي (وليس نصًا ثابتًا). */
fun formatLastSeen(lastActiveTimestamp: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diffMs = (nowMillis - lastActiveTimestamp).coerceAtLeast(0)
    val minutes = diffMs / 60_000
    val hours = diffMs / 3_600_000
    val days = diffMs / 86_400_000

    return when {
        minutes < 1 -> "آخر ظهور: الآن"
        minutes < 60 -> "آخر ظهور: منذ ${minutes} د"
        hours < 24 -> "آخر ظهور: منذ ${hours} س"
        days < 7 -> "آخر ظهور: منذ ${days} يوم"
        else -> "آخر ظهور: منذ فترة طويلة"
    }
}
