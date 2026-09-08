package com.example.zainqhchat.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zainqhchat.data.remote.supabase.SupabaseStorageService
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.ChatPreview
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.model.UserTier
import com.example.zainqhchat.domain.repository.ChatRepository
import com.example.zainqhchat.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val storageService: SupabaseStorageService
) : ViewModel() {

    private val _currentUserIdState = MutableStateFlow<String?>(null)
    private val _activeTargetUserIdState = MutableStateFlow<String?>(null)

    // إشعار النقاط المكتسبة
    private val _pointsAwardedNotification = MutableStateFlow<String?>(null)
    val pointsAwardedNotification: StateFlow<String?> = _pointsAwardedNotification.asStateFlow()

    // رفع صورة الدردشة الحقيقي (حالة تحميل + رسالة خطأ حقيقية إن فشل)
    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _imageUploadError = MutableStateFlow<String?>(null)
    val imageUploadError: StateFlow<String?> = _imageUploadError.asStateFlow()

    fun clearImageUploadError() {
        _imageUploadError.value = null
    }

    fun setUsers(currentUserId: String) {
        _currentUserIdState.value = currentUserId
    }

    val chatPreviews: StateFlow<List<ChatPreview>> = _currentUserIdState.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList())
        else chatRepository.getChatPreviewsFlow(userId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val publicMessages: StateFlow<List<ChatMessage>> = chatRepository.getPublicMessagesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** لقطة حيّة بكل المستخدمين — تُستخدم فقط لاكتشاف الإشارات (@) عند الإرسال. */
    private val allUsers: StateFlow<List<User>> = _currentUserIdState.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else userRepository.getAllUsersFlow(userId)
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    private val directMessagesCache = java.util.concurrent.ConcurrentHashMap<String, StateFlow<List<ChatMessage>>>()

    fun getDirectMessages(otherUserId: String): StateFlow<List<ChatMessage>> {
        val currentUserId = _currentUserIdState.value ?: ""
        val key = "${currentUserId}_$otherUserId"
        return directMessagesCache.getOrPut(key) {
            chatRepository.getDirectMessagesFlow(currentUserId, otherUserId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
        }
    }

    fun sendMessage(
        currentUser: User,
        recipientId: String?, // null للدردشة العامة
        text: String,
        imageUrl: String? = null
    ) {
        if (text.isBlank() && imageUrl.isNull_or_Blank()) return

        // إن كان المستخدم يردّ على رسالة، نُدرج اقتباسًا قصيرًا منها داخل نص
        // الرسالة نفسها (بدل إضافة عمود جديد لقاعدة البيانات) — حل بسيط
        // وفعّال يعمل فورًا في الدردشة العامة والخاصة على حدٍّ سواء.
        val replyTarget = _replyingTo.value
        val finalText = if (replyTarget != null) {
            val snippet = replyTarget.text.take(60).let { if (replyTarget.text.length > 60) "$it…" else it }
            "↩️ ردًا على ${replyTarget.senderName}: «$snippet»\n$text"
        } else {
            text
        }
        _replyingTo.value = null

        // حساب الفئة (Tier) محليًا من نقاط المستخدم الحالي المعروفة أصلًا —
        // بدل طلب شبكة إضافي لجلبها، ما كان يُبطئ كل رسالة بجولة اتصال كاملة.
        val senderTier = UserTier.fromPoints(currentUser.points)
        val mentionedUserIds = if (recipientId == null) {
            detectMentions(finalText, allUsers.value, currentUser.id)
        } else {
            emptyList()
        }

        viewModelScope.launch {
            chatRepository.sendMessage(
                senderId = currentUser.id,
                senderName = currentUser.name,
                senderAvatarUrl = currentUser.avatarUrl,
                senderTier = senderTier,
                recipientId = recipientId,
                text = finalText,
                imageUrl = imageUrl,
                mentionedUserIds = mentionedUserIds
            ).onSuccess {
                _pointsAwardedNotification.value = "+1 نقطة! 🌟"
            }
        }
    }

    fun clearPointsNotification() {
        _pointsAwardedNotification.value = null
    }

    /**
     * رفع صورة حقيقية (بايتات من معرض الجهاز) إلى Supabase Storage ثم إرسالها
     * كرسالة دردشة فعلية — بدون أي صور تجريبية أو Mock.
     */
    fun sendImageMessage(
        currentUser: User,
        recipientId: String?,
        imageBytes: ByteArray,
        mimeType: String
    ) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            _imageUploadError.value = null

            val senderTier = UserTier.fromPoints(currentUser.points)
            val chatId = recipientId ?: "public"
            storageService.uploadChatImage(chatId, imageBytes, mimeType)
                .onSuccess { imageUrl ->
                    chatRepository.sendMessage(
                        senderId = currentUser.id,
                        senderName = currentUser.name,
                        senderAvatarUrl = currentUser.avatarUrl,
                        senderTier = senderTier,
                        recipientId = recipientId,
                        text = "",
                        imageUrl = imageUrl
                    ).onSuccess {
                        _pointsAwardedNotification.value = "+1 نقطة! 🌟"
                    }.onFailure { error ->
                        _imageUploadError.value = error.message ?: "تعذر إرسال الصورة"
                    }
                }
                .onFailure { error ->
                    _imageUploadError.value = error.message ?: "تعذر رفع الصورة"
                }

            _isUploadingImage.value = false
        }
    }

    fun markAsRead(currentUserId: String, otherUserId: String) {
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(currentUserId, otherUserId)
        }
    }

    /** الرسالة التي يردّ عليها المستخدم حاليًا (تظهر كشريط معاينة فوق حقل الكتابة). */
    private val _replyingTo = MutableStateFlow<ChatMessage?>(null)
    val replyingTo: StateFlow<ChatMessage?> = _replyingTo.asStateFlow()

    fun setReplyTarget(message: ChatMessage?) { _replyingTo.value = message }
    fun clearReplyTarget() { _replyingTo.value = null }

    private val _messageActionResult = MutableStateFlow<String?>(null)
    val messageActionResult: StateFlow<String?> = _messageActionResult.asStateFlow()
    fun clearMessageActionResult() { _messageActionResult.value = null }

    /** يحذف رسالة يملكها المستخدم الحالي — تعمل في الدردشة العامة والخاصة. */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteOwnMessage(messageId)
                .onSuccess { _messageActionResult.value = "تم حذف الرسالة" }
                .onFailure { _messageActionResult.value = it.message ?: "تعذّر حذف الرسالة" }
        }
    }

    /** يُبلغ عن رسالة/مرسلها — يظهر فورًا في لوحة الإدارة. */
    fun reportMessage(currentUserId: String, message: ChatMessage, reason: String) {
        viewModelScope.launch {
            userRepository.reportUser(currentUserId, message.senderId, reason, message.id)
                .onSuccess { _messageActionResult.value = "تم إرسال البلاغ، شكرًا لك" }
                .onFailure { _messageActionResult.value = it.message ?: "تعذّر إرسال البلاغ" }
        }
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val userRepository: UserRepository,
        private val storageService: SupabaseStorageService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(chatRepository, userRepository, storageService) as T
        }
    }
}

private fun String?.isNull_or_Blank(): Boolean = this == null || this.isBlank()

/**
 * يكتشف الإشارات (@اسم) في نص رسالة الدردشة العامة بمطابقتها مع أسماء
 * المستخدمين/أسماء المستخدمين المعروفة (الأطول أولًا لتفادي تطابق جزئي
 * خاطئ)، ويُعيد معرّفات من أُشير إليهم فعليًا (بدون المرسل نفسه). هذا ما
 * يُنشئ إشعار "أشار إليك" الحقيقي في الدردشة العامة عبر تريجر قاعدة
 * البيانات (راجع supabase/migrations/003_diamond_exchange_and_mentions.sql).
 */
private fun detectMentions(text: String, users: List<User>, currentUserId: String): List<String> {
    if (!text.contains('@')) return emptyList()

    val candidates = users
        .filter { it.id != currentUserId }
        .flatMap { user -> listOf(user.username to user.id, user.name to user.id) }
        .filter { it.first.isNotBlank() }
        .sortedByDescending { it.first.length }

    if (candidates.isEmpty()) return emptyList()

    val matched = LinkedHashSet<String>()
    text.indices.filter { text[it] == '@' }.forEach { atIndex ->
        val remainder = text.substring(atIndex + 1)
        candidates.firstOrNull { (label, _) -> remainder.startsWith(label) }?.let { (_, userId) ->
            matched.add(userId)
        }
    }
    return matched.toList()
}
