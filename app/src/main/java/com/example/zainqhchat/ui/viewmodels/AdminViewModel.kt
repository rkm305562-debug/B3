package com.example.zainqhchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zainqhchat.domain.model.AdminActionLogEntry
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(private val adminRepository: AdminRepository) : ViewModel() {

    val moderationMessages: StateFlow<List<ChatMessage>> =
        adminRepository.observePublicMessagesForModeration().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _actionLog = MutableStateFlow<List<AdminActionLogEntry>>(emptyList())
    val actionLog: StateFlow<List<AdminActionLogEntry>> = _actionLog

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private val _featureFlags = MutableStateFlow<List<com.example.zainqhchat.domain.model.FeatureFlag>>(emptyList())
    val featureFlags: StateFlow<List<com.example.zainqhchat.domain.model.FeatureFlag>> = _featureFlags

    fun loadFeatureFlags() {
        viewModelScope.launch {
            try {
                _featureFlags.value = adminRepository.fetchFeatureFlags()
            } catch (e: Exception) {
                _statusMessage.value = "تعذّر تحميل حالة الأقسام"
            }
        }
    }

    fun setFeatureFlag(sectionKey: String, enabled: Boolean, reason: String?) {
        viewModelScope.launch {
            adminRepository.setFeatureFlag(sectionKey, enabled, reason)
                .onSuccess {
                    _statusMessage.value = if (enabled) "تم تفعيل القسم" else "تم إغلاق القسم مؤقتًا"
                    loadFeatureFlags()
                }
                .onFailure { _statusMessage.value = it.message ?: "فشلت العملية" }
        }
    }

    fun searchUsers(query: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _users.value = adminRepository.searchUsers(query)
            } catch (e: Exception) {
                _statusMessage.value = e.message ?: "تعذر جلب المستخدمين"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadActionLog() {
        viewModelScope.launch {
            try {
                _actionLog.value = adminRepository.fetchActionLog()
            } catch (e: Exception) {
                _statusMessage.value = e.message ?: "تعذر جلب سجل الإجراءات"
            }
        }
    }

    fun setBanStatus(targetUserId: String, banned: Boolean, reason: String?) {
        viewModelScope.launch {
            adminRepository.setUserBanStatus(targetUserId, banned, reason)
                .onSuccess {
                    _statusMessage.value = if (banned) "تم حظر المستخدم" else "تم فك الحظر عن المستخدم"
                    searchUsers(null)
                }
                .onFailure { _statusMessage.value = it.message ?: "فشلت العملية" }
        }
    }

    fun setTempBan(targetUserId: String, hours: Int, reason: String?) {
        viewModelScope.launch {
            adminRepository.setUserTempBan(targetUserId, hours, reason)
                .onSuccess {
                    _statusMessage.value = "تم تطبيق حظر مؤقت لمدة $hours ساعة"
                    searchUsers(null)
                }
                .onFailure { _statusMessage.value = it.message ?: "فشل تطبيق الحظر المؤقت" }
        }
    }

    fun deleteUser(targetUserId: String, reason: String?) {
        viewModelScope.launch {
            adminRepository.deleteUser(targetUserId, reason)
                .onSuccess {
                    _statusMessage.value = "تم حذف المستخدم نهائيًا"
                    searchUsers(null)
                }
                .onFailure { _statusMessage.value = it.message ?: "فشل حذف المستخدم" }
        }
    }

    fun deleteMessage(messageId: String, imageUrl: String?, reason: String?) {
        viewModelScope.launch {
            adminRepository.deletePublicMessage(messageId, imageUrl, reason)
                .onSuccess { _statusMessage.value = "تم حذف الرسالة" }
                .onFailure { _statusMessage.value = it.message ?: "فشل حذف الرسالة" }
        }
    }

    fun removeAvatar(targetUserId: String, avatarUrl: String?, reason: String?) {
        viewModelScope.launch {
            adminRepository.removeUserAvatar(targetUserId, avatarUrl, reason)
                .onSuccess {
                    _statusMessage.value = "تمت إزالة الصورة الشخصية"
                    searchUsers(null)
                }
                .onFailure { _statusMessage.value = it.message ?: "فشلت إزالة الصورة" }
        }
    }

    fun adjustPoints(targetUserId: String, delta: Int, reason: String?) {
        viewModelScope.launch {
            adminRepository.adjustUserPoints(targetUserId, delta, reason)
                .onSuccess {
                    _statusMessage.value = "تم تعديل النقاط بنجاح"
                    searchUsers(null)
                }
                .onFailure { _statusMessage.value = it.message ?: "فشل تعديل النقاط" }
        }
    }

    fun adjustCurrency(targetUserId: String, coinsDelta: Long, diamondsDelta: Long, reason: String?) {
        viewModelScope.launch {
            adminRepository.adjustUserCurrency(targetUserId, coinsDelta, diamondsDelta, reason)
                .onSuccess {
                    _statusMessage.value = "تم تعديل رصيد المستخدم بنجاح"
                    searchUsers(null)
                }
                .onFailure { _statusMessage.value = it.message ?: "فشل تعديل الرصيد" }
        }
    }

    fun broadcastNotification(title: String, body: String?) {
        viewModelScope.launch {
            adminRepository.broadcastNotification(title, body)
                .onSuccess { count -> _statusMessage.value = "تم إرسال الإشعار إلى $count مستخدم" }
                .onFailure { _statusMessage.value = it.message ?: "فشل إرسال الإشعار الجماعي" }
        }
    }

    class Factory(private val adminRepository: AdminRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminViewModel(adminRepository) as T
        }
    }
}
