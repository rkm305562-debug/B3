package com.example.zainqhchat.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.zainqhchat.domain.model.UserSettings
import com.example.zainqhchat.ui.theme.setAppDarkMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * إعدادات حقيقية محفوظة فعليًا (SharedPreferences) — الوضع الليلي/الفاتح
 * كان سابقًا في الذاكرة فقط بلا أي حفظ، وبلا أي تأثير بصري فعلي على الشاشات
 * (كانت ألوان التطبيق ثابتة). الآن: يُحفَظ اختيار المستخدم فعليًا، ويُطبَّق
 * فورًا على كل الشاشات دون إعادة تشغيل، عبر [com.example.zainqhchat.ui.theme.setAppDarkMode]
 * التي تُحدِّث حالة عامة قابلة للمراقبة يقرأها كل لون في التطبيق مباشرة.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(loadSavedSettings())
    val settingsState: StateFlow<UserSettings> = _settingsState.asStateFlow()

    init {
        // تطبيق الوضع المحفوظ فورًا عند بدء التطبيق (قبل أول رسم للواجهة)
        setAppDarkMode(_settingsState.value.isDarkMode)
    }

    fun toggleDarkMode(isDark: Boolean) {
        _settingsState.value = _settingsState.value.copy(isDarkMode = isDark)
        setAppDarkMode(isDark)
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun setPrimaryColor(colorHex: Long) {
        _settingsState.value = _settingsState.value.copy(primaryColorHex = colorHex)
    }

    fun toggleNotifications(enabled: Boolean) {
        _settingsState.value = _settingsState.value.copy(notificationsEnabled = enabled)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    private fun loadSavedSettings(): UserSettings {
        val defaults = UserSettings()
        return defaults.copy(
            isDarkMode = prefs.getBoolean(KEY_DARK_MODE, defaults.isDarkMode),
            notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, defaults.notificationsEnabled)
        )
    }

    companion object {
        private const val PREFS_NAME = "zainqh_app_settings"
        private const val KEY_DARK_MODE = "is_dark_mode"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
    }
}
