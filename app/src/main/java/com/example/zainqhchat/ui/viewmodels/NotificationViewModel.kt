package com.example.zainqhchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zainqhchat.core.notification.LocalNotificationManager
import com.example.zainqhchat.domain.model.NotificationItem
import com.example.zainqhchat.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val notificationRepository: NotificationRepository,
    private val localNotificationManager: LocalNotificationManager
) : ViewModel() {

    private val _currentUserIdState = MutableStateFlow<String?>(null)

    fun setCurrentUserId(userId: String) {
        if (_currentUserIdState.value == userId) return
        _currentUserIdState.value = userId
        listenForIncomingMessageNotifications(userId)
    }

    private var messageListenerStarted = false

    private fun listenForIncomingMessageNotifications(userId: String) {
        // يُشغَّل مرة واحدة فقط لكل جلسة تسجيل دخول (ViewModel هذا على مستوى
        // التطبيق كاملاً، وليس مربوطًا بشاشة معيّنة) — يستمر بالاستماع بغض
        // النظر عن الشاشة المفتوحة حاليًا طالما التطبيق يعمل.
        if (messageListenerStarted) return
        messageListenerStarted = true
        viewModelScope.launch {
            notificationRepository.observeIncomingPrivateMessages(userId).distinctUntilChanged { old, new -> old.id == new.id }
                .collect { message ->
                    localNotificationManager.showMessageNotification(message)
                }
        }
    }

    val notifications: StateFlow<List<NotificationItem>> = _currentUserIdState.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList())
        else notificationRepository.getNotificationsFlow(userId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unreadCount: StateFlow<Int> = notifications.map { list -> list.count { !it.isRead } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    class Factory(
        private val notificationRepository: NotificationRepository,
        private val localNotificationManager: LocalNotificationManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationViewModel(notificationRepository, localNotificationManager) as T
        }
    }
}
