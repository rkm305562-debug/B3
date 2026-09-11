package com.example.zainqhchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zainqhchat.domain.model.AvatarUpdate
import com.example.zainqhchat.domain.model.OnlineStatusCategory
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _currentUserIdState = MutableStateFlow<String?>(null)

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun setCurrentUserId(currentUserId: String) {
        _currentUserIdState.value = currentUserId
    }

    val allUsers: StateFlow<List<User>> = _currentUserIdState.flatMapLatest { currentUserId ->
        if (currentUserId == null) flowOf(emptyList())
        else userRepository.getAllUsersFlow(currentUserId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ترتيب المستخدمين حسب فئات الاتصال المطلوبة بالضبط. ملاحظة مهمة: فئة كل
    // مستخدم تعتمد على "منذ متى" وقتها last_active_timestamp — إن اعتمدنا
    // فقط على map() فوق allUsers فلن يُعاد تصنيف مستخدم غادر التطبيق إلا إذا
    // وصل تحديث Realtime جديد لأي مستخدم (وهذا كان يجعل من يترك التطبيق
    // يظهر "متصل الآن" إلى الأبد إن لم يتغيّر شيء آخر في القائمة). لذلك
    // نجمعها مع نبضة دورية (كل 20 ثانية) تُعيد التصنيف بالوقت الحالي حتى بلا
    // أي بيانات جديدة من الخادم.
    private val presenceTicker = flow {
        while (true) {
            emit(Unit)
            delay(20_000)
        }
    }

    val usersGroupedByOnlineStatus: StateFlow<Map<OnlineStatusCategory, List<User>>> =
        combine(allUsers, presenceTicker) { userList, _ ->
            OnlineStatusCategory.entries.associateWith { category ->
                userList.filter { it.onlineCategory == category }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val userProfileCache = java.util.concurrent.ConcurrentHashMap<String, StateFlow<User?>>()

    fun getUserProfile(userId: String, currentUserId: String): StateFlow<User?> {
        val key = "${currentUserId}_$userId"
        return userProfileCache.getOrPut(key) {
            userRepository.getUserByIdFlow(userId, currentUserId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null
                )
        }
    }

    fun toggleFollow(currentUserId: String, targetUserId: String, isCurrentlyFollowing: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyFollowing) {
                userRepository.unfollowUser(currentUserId, targetUserId)
                    .onSuccess { _actionMessage.value = "تمت إلغاء المتابعة" }
            } else {
                userRepository.followUser(currentUserId, targetUserId)
                    .onSuccess { _actionMessage.value = "تمت المتابعة وحصلت على +5 نقاط! 🎉" }
            }
        }
    }

    fun blockUser(currentUserId: String, targetUserId: String, reason: String = "") {
        viewModelScope.launch {
            userRepository.blockUser(currentUserId, targetUserId, reason)
                .onSuccess { _actionMessage.value = "تم حظر المستخدم بنجاح" }
        }
    }

    fun reportUser(currentUserId: String, targetUserId: String, reason: String, messageId: String? = null) {
        viewModelScope.launch {
            userRepository.reportUser(currentUserId, targetUserId, reason, messageId)
                .onSuccess { _actionMessage.value = "تم رفع البلاغ للمراجعة وسيتم النظر فيه" }
                .onFailure { _actionMessage.value = it.message ?: "تعذّر إرسال البلاغ، حاول مرة أخرى" }
        }
    }

    fun updateProfile(userId: String, name: String, age: Int, avatar: AvatarUpdate) {
        viewModelScope.launch {
            userRepository.updateProfile(userId, name, age, avatar)
                .onSuccess { _actionMessage.value = "تم تحديث الملف الشخصي بنجاح" }
                .onFailure { _actionMessage.value = it.message ?: "فشل تحديث الملف الشخصي" }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    /** يجلب حساب المدير لفتح محادثة "تواصل مع المدير" — متاحة من أي مكان في التطبيق. */
    fun contactAdmin(onResult: (User?) -> Unit) {
        viewModelScope.launch {
            val admin = try {
                userRepository.fetchAdminUser()
            } catch (e: Exception) {
                null
            }
            onResult(admin)
        }
    }

    private val _featureFlags = MutableStateFlow<Map<String, com.example.zainqhchat.domain.model.FeatureFlag>>(emptyMap())
    /** حالة كل أقسام التطبيق — خريطة مفتاحها section_key. القسم يُعتبر
     *  مفعّلاً افتراضيًا إن لم يصل أي بيانات بعد (فشل شبكة مثلاً)، حتى لا
     *  يُحرَم المستخدمون من التطبيق كاملاً بسبب عطل مؤقت في هذا الفحص وحده. */
    val featureFlags: StateFlow<Map<String, com.example.zainqhchat.domain.model.FeatureFlag>> = _featureFlags.asStateFlow()

    fun loadFeatureFlags() {
        viewModelScope.launch {
            try {
                _featureFlags.value = userRepository.fetchFeatureFlags().associateBy { it.sectionKey }
            } catch (e: Exception) {
                // تجاهل بصمت — الافتراض الآمن (مفعّل) يبقى ساريًا تلقائيًا.
            }
        }
    }

    /** true إن كان القسم مغلقًا صراحةً؛ false افتراضيًا (بما فيها حالة عدم
     *  وصول أي بيانات بعد) — حتى لا نمنع مستخدمًا عن طريق الخطأ. */
    fun isSectionDisabled(sectionKey: String): Boolean =
        _featureFlags.value[sectionKey]?.isEnabled == false

    fun disabledReasonFor(sectionKey: String): String? =
        _featureFlags.value[sectionKey]?.disabledReason

    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserViewModel(userRepository) as T
        }
    }
}
