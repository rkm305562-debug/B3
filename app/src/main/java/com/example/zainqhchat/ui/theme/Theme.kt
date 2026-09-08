package com.example.zainqhchat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ملاحظة: تُستخدم قيم ثابتة هنا (وليس الخصائص الحيّة في Color.kt) عمدًا،
// لأن كائنات ColorScheme هذه تُهيَّأ مرة واحدة فقط عند تحميل الملف؛ استخدام
// الخصائص الحيّة هنا كان سيُجمِّد قيمها بشكل غير صحيح. التلوين الفعلي الحيّ
// للشاشات يتم عبر ثوابت Color.kt نفسها في كل شاشة (وهي تعمل بشكل صحيح تمامًا
// لأنها تُقرأ في وقت الرسم الفعلي)، وليس عبر MaterialTheme.colorScheme.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF0EA5E9),
    onPrimaryContainer = Color(0xFF0C2233),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0B1220),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111A2C),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF111A2C),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF223354)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0EA5E9),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFFF6F8FB),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color(0xFFF6F8FB),
    background = Color(0xFFF6F8FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEFF3F8),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFF38BDF8)
)

@Composable
fun ZainQHChatTheme(
    darkTheme: Boolean = false, // الوضع الفاتح الحديث هو الافتراضي الآن
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            // أيقونات داكنة على خلفية فاتحة، وأيقونات فاتحة على خلفية داكنة
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
