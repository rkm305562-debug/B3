package com.example.zainqhchat.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

// =====================================================================
// نظام ألوان حيّ وحقيقي (فاتح/داكن) — 2026.
// كل لون أدناه هو خاصية محسوبة (get()) تعتمد على حالة Compose عامة قابلة
// للمراقبة (mutableStateOf). أي Composable يقرأ أحد هذه الألوان أثناء الرسم
// يُسجَّل تلقائيًا في نظام Snapshot الخاص بـ Compose، فيُعاد رسمه فورًا عند
// تغيير الوضع عبر [setAppDarkMode] — دون الحاجة لإعادة تشغيل التطبيق ودون
// الحاجة لتعديل أي شاشة من الشاشات التي تستخدم هذه الأسماء (لأن قراءة خاصية
// Kotlin تبدو مطابقة تمامًا لقراءة ثابت عادي من ناحية بناء الجملة).
// =====================================================================

private val isDarkModeState = mutableStateOf(false)

/** يُستدعى من SettingsViewModel عند تبديل الوضع أو عند بدء التطبيق. */
fun setAppDarkMode(isDark: Boolean) {
    isDarkModeState.value = isDark
}

fun isAppDarkMode(): Boolean = isDarkModeState.value

private val darkMode: Boolean get() = isDarkModeState.value

// اللون الرئيسي (أزرق سماوي)
val GoldPrimary: Color get() = if (darkMode) Color(0xFF38BDF8) else Color(0xFF0EA5E9)
val GoldDark: Color get() = if (darkMode) Color(0xFF0EA5E9) else Color(0xFF0284C7)
val GoldMetallic: Color get() = Color(0xFF38BDF8)
val GoldChampagne: Color get() = if (darkMode) Color(0xFF0C2233) else Color(0xFFE0F2FE)
val GoldGradientStart: Color get() = Color(0xFF38BDF8)
val GoldGradientEnd: Color get() = if (darkMode) Color(0xFF0284C7) else Color(0xFF0EA5E9)

// خلفيات وأسطح
val LuxuryBlackBg: Color get() = if (darkMode) Color(0xFF0B1220) else Color(0xFFF6F8FB)
val LuxurySurfaceDark: Color get() = if (darkMode) Color(0xFF111A2C) else Color(0xFFFFFFFF)
val LuxurySurfaceCard: Color get() = if (darkMode) Color(0xFF111A2C) else Color(0xFFFFFFFF)
val LuxurySurfaceElevated: Color get() = if (darkMode) Color(0xFF16213A) else Color(0xFFEFF3F8)
val LuxuryBorderGold: Color get() = if (darkMode) Color(0xFF223354) else Color(0xFFDCE3EC)

// النص والأيقونات
val TextPrimaryWhite: Color get() = if (darkMode) Color(0xFFF1F5F9) else Color(0xFF0F172A)
val TextSecondaryMuted: Color get() = if (darkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
val TextGold: Color get() = GoldPrimary

// الحالة والاتصال — عالمية، لا تتغير بين الوضعين
val StatusOnlineGreen = Color(0xFF22C55E)
val StatusOfflineGray = Color(0xFF94A3B8)
val StatusErrorRed = Color(0xFFEF4444)
