package com.example.zainqhchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zainqhchat.domain.model.NewAvatar
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Authenticated(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

/** حالة عملية إضافة/تغيير كلمة المرور من الإعدادات — منفصلة عن [AuthUiState]
 * الرئيسية كي لا تتعارض مع حالة تسجيل الدخول/الخروج المعروضة في شاشة الدخول. */
sealed interface SetPasswordUiState {
    object Idle : SetPasswordUiState
    object Loading : SetPasswordUiState
    object Success : SetPasswordUiState
    data class Error(val message: String) : SetPasswordUiState
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _setPasswordState = MutableStateFlow<SetPasswordUiState>(SetPasswordUiState.Idle)
    val setPasswordState: StateFlow<SetPasswordUiState> = _setPasswordState.asStateFlow()

    val currentUser = authRepository.currentUserFlow

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.value = AuthUiState.Authenticated(user)
            } else {
                _uiState.value = AuthUiState.Idle
            }
        }
    }

    /**
     * تسجيل حساب جديد — يتطلب اسمًا فقط من المستخدم. تُولَّد كلمة مرور عشوائية
     * قوية داخليًا (لا يراها المستخدم ولا يحتاج لتذكّرها) حتى يستوفي الحساب
     * متطلب Supabase Auth بوجود كلمة مرور. هذا يعني أن الجلسة تبقى فعّالة على
     * هذا الجهاز فقط ما لم يُضِف المستخدم كلمة مرور حقيقية لاحقًا من
     * الإعدادات (setPassword) — عندها فقط يمكنه الدخول من جهاز آخر أو بعد
     * حذف التطبيق وإعادة تثبيته.
     */
    fun register(username: String, avatar: NewAvatar? = null) {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال اسم")
            return
        }

        // يدعم اسم المستخدم أي لغة بالكامل (عربي، إنجليزي، أرقام عربية/إنجليزية،
        // مسافات بين الكلمات) — التحقق الوحيد هو منع الفراغ التام أو المسافات
        // فقط، وحد أقصى معقول للطول. لا يوجد أي تصفية لحروف بعينها (Unicode-safe
        // بالكامل)؛ آلية بناء بريد الدخول الداخلي في SupabaseAuthService تدعم أي
        // نص عبر تجزئة حتمية، فلا حاجة لتقييد الأحرف هنا إطلاقًا.
        if (trimmedUsername.length > 40) {
            _uiState.value = AuthUiState.Error("اسم المستخدم طويل جدًا (الحد الأقصى 40 حرفًا)")
            return
        }

        // العمر لم يعد يُطلب عند التسجيل — يأخذ القيمة الافتراضية نفسها
        // المعتمدة في قاعدة البيانات (18)، ويمكن للمستخدم تعديله لاحقًا من
        // "تعديل الملف الشخصي" في الإعدادات.
        val defaultAge = 18
        val generatedPassword = generateRandomPassword()

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.registerUser(trimmedUsername, generatedPassword, trimmedUsername, defaultAge, avatar)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "فشل إنشاء الحساب")
                }
        }
    }

    /** يولّد كلمة مرور عشوائية قوية (16 محرفًا: حروف كبيرة/صغيرة وأرقام ورموز). */
    private fun generateRandomPassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%*"
        return (1..16).map { chars.random() }.joinToString("")
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("يرجى إدخال اسم المستخدم وكلمة المرور")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.loginUser(username, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "فشل تسجيل الدخول")
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    /**
     * حذف نهائي للحساب. [onResult] يُستدعى بـ true عند النجاح الفعلي (بعد
     * تأكد الحذف من Supabase) كي تنتقل الواجهة لشاشة الدخول فقط عند التأكد،
     * أو false مع رسالة خطأ حقيقية معروضة عبر uiState عند الفشل.
     */
    fun deleteAccount(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            authRepository.deleteAccount()
                .onSuccess {
                    _uiState.value = AuthUiState.Idle
                    onResult(true)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "تعذر حذف الحساب")
                    onResult(false)
                }
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    /** يضيف/يغيّر كلمة مرور حقيقية للحساب الحالي من الإعدادات. */
    fun setPassword(newPassword: String) {
        if (newPassword.length < 6) {
            _setPasswordState.value = SetPasswordUiState.Error("كلمة المرور يجب أن تكون 6 خانات على الأقل")
            return
        }
        viewModelScope.launch {
            _setPasswordState.value = SetPasswordUiState.Loading
            authRepository.setPassword(newPassword)
                .onSuccess {
                    _setPasswordState.value = SetPasswordUiState.Success
                }
                .onFailure { error ->
                    _setPasswordState.value = SetPasswordUiState.Error(error.message ?: "تعذّر حفظ كلمة المرور")
                }
        }
    }

    fun clearSetPasswordState() {
        _setPasswordState.value = SetPasswordUiState.Idle
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
