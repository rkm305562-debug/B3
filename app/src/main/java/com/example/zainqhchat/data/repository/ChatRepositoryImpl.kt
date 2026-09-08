package com.example.zainqhchat.data.repository

import com.example.zainqhchat.data.remote.supabase.SupabaseDatabaseService
import com.example.zainqhchat.data.remote.supabase.SupabaseRealtimeService
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.ChatPreview
import com.example.zainqhchat.domain.model.UserTier
import com.example.zainqhchat.domain.repository.ChatRepository
import com.example.zainqhchat.domain.model.isReallyOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * تنفيذ حقيقي للدردشة (عامة وخاصة) مباشرة عبر Supabase — لا Room إطلاقًا هنا.
 * التحديث اللحظي (بدون Polling وبدون إعادة تحميل الشاشة) عبر [SupabaseRealtimeService]
 * المشترك؛ كل Flow يستدعي acquire() عند بدء الجمع (Collect) و release() عند
 * توقفه (عبر awaitClose)، بحيث يُغلَق اتصال الـ WebSocket تلقائيًا عندما لا
 * تعود أي شاشة بحاجته — بدون أي اشتراك مكرر لنفس الجدول.
 */
class ChatRepositoryImpl(
    private val databaseService: SupabaseDatabaseService,
    private val realtimeService: SupabaseRealtimeService
) : ChatRepository {

    override fun getPublicMessagesFlow(): Flow<List<ChatMessage>> = callbackFlow {
        realtimeService.acquire()
        val current = LinkedHashMap<String, ChatMessage>()

        try {
            databaseService.fetchPublicMessages().forEach { current[it.id] = it }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
        trySend(current.values.sortedBy { it.timestamp })

        val job = launch {
            realtimeService.messageChanges.collect { change ->
                if (!change.message.isPublic) return@collect
                when (change.type) {
                    "DELETE" -> current.remove(change.message.id)
                    else -> current[change.message.id] = change.message
                }
                trySend(current.values.sortedBy { it.timestamp })
            }
        }

        // شبكة أمان: حتى لو تأخر أو ضاع حدث Realtime لأي سبب (توكن منتهي على
        // القناة، انقطاع لحظي، إلخ)، تضمن هذه الجولة الدورية وصول أي رسالة
        // خلال ثوانٍ معدودة بدل الحاجة للخروج من الشاشة والعودة إليها يدويًا.
        val pollJob = launch {
            while (true) {
                delay(12_000)
                try {
                    var changed = false
                    databaseService.fetchPublicMessages().forEach { m ->
                        if (current.put(m.id, m) == null) changed = true
                    }
                    if (changed) trySend(current.values.sortedBy { it.timestamp })
                } catch (e: Exception) {
                    // فشل مؤقت في الشبكة — Realtime سيحاول لوحده، لا داعي لإغلاق الـ Flow.
                }
            }
        }

        awaitClose {
            job.cancel()
            pollJob.cancel()
            realtimeService.release()
        }
    }

    override fun getDirectMessagesFlow(currentUserId: String, otherUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        realtimeService.acquire()
        val current = LinkedHashMap<String, ChatMessage>()

        fun belongsToThisConversation(m: ChatMessage): Boolean =
            !m.isPublic && (
                (m.senderId == currentUserId && m.recipientId == otherUserId) ||
                    (m.senderId == otherUserId && m.recipientId == currentUserId)
                )

        try {
            databaseService.fetchPrivateMessages(currentUserId, otherUserId).forEach { current[it.id] = it }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
        trySend(current.values.sortedBy { it.timestamp })

        val job = launch {
            realtimeService.messageChanges.collect { change ->
                if (!belongsToThisConversation(change.message)) return@collect
                when (change.type) {
                    "DELETE" -> current.remove(change.message.id)
                    else -> current[change.message.id] = change.message
                }
                trySend(current.values.sortedBy { it.timestamp })
            }
        }

        // شبكة أمان مطابقة لنفس السبب في getPublicMessagesFlow أعلاه.
        val pollJob = launch {
            while (true) {
                delay(12_000)
                try {
                    var changed = false
                    databaseService.fetchPrivateMessages(currentUserId, otherUserId).forEach { m ->
                        if (current.put(m.id, m) == null) changed = true
                    }
                    if (changed) trySend(current.values.sortedBy { it.timestamp })
                } catch (e: Exception) {
                    // فشل مؤقت في الشبكة — Realtime سيحاول لوحده، لا داعي لإغلاق الـ Flow.
                }
            }
        }

        awaitClose {
            job.cancel()
            pollJob.cancel()
            realtimeService.release()
        }
    }

    override fun getChatPreviewsFlow(currentUserId: String): Flow<List<ChatPreview>> = callbackFlow {
        realtimeService.acquire()

        var users: List<com.example.zainqhchat.domain.model.User>
        var publicLast: ChatMessage?
        val lastDirectByOtherUser = HashMap<String, ChatMessage>()

        try {
            users = databaseService.fetchUsersExcept(currentUserId)
            publicLast = databaseService.fetchPublicMessages(limit = 1).lastOrNull()
            databaseService.fetchPrivateMessagesForUser(currentUserId, limit = 300).forEach { m ->
                val otherId = if (m.senderId == currentUserId) m.recipientId else m.senderId
                if (otherId != null) {
                    val existing = lastDirectByOtherUser[otherId]
                    if (existing == null || m.timestamp > existing.timestamp) {
                        lastDirectByOtherUser[otherId] = m
                    }
                }
            }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        fun buildPreviews(): List<ChatPreview> {
            val list = mutableListOf<ChatPreview>()
            list.add(
                ChatPreview(
                    id = "public_room",
                    title = "الدردشة العامة 🌐",
                    isPublic = true,
                    avatarUrl = null,
                    lastMessageText = publicLast?.text ?: "مرحباً بكم في مجتمع دردشة توتة!",
                    lastMessageTimestamp = publicLast?.timestamp ?: System.currentTimeMillis(),
                    unreadCount = 0,
                    targetUserId = null,
                    isOnline = true,
                    userTier = UserTier.LEGEND
                )
            )
            for (user in users) {
                val last = lastDirectByOtherUser[user.id] ?: continue // لا محادثة فعلية بعد -> لا يظهر في قائمة المحادثات
                val unread = if (last.recipientId == currentUserId && !last.isRead) 1 else 0
                list.add(
                    ChatPreview(
                        id = "chat_${user.id}",
                        title = user.name,
                        isPublic = false,
                        avatarUrl = user.avatarUrl,
                        lastMessageText = last.text,
                        lastMessageTimestamp = last.timestamp,
                        unreadCount = unread,
                        targetUserId = user.id,
                        isOnline = user.isReallyOnline(),
                        userTier = UserTier.fromPoints(user.points)
                    )
                )
            }
            return list.sortedByDescending { it.lastMessageTimestamp }
        }

        trySend(buildPreviews())

        val messagesJob = launch {
            realtimeService.messageChanges.collect { change ->
                val m = change.message
                if (m.isPublic) {
                    publicLast = m
                } else {
                    val otherId = when {
                        m.senderId == currentUserId -> m.recipientId
                        m.recipientId == currentUserId -> m.senderId
                        else -> null
                    } ?: return@collect
                    val existing = lastDirectByOtherUser[otherId]
                    if (existing == null || m.timestamp >= existing.timestamp) {
                        lastDirectByOtherUser[otherId] = m
                    }
                }
                trySend(buildPreviews())
            }
        }

        val usersJob = launch {
            realtimeService.userChanges.collect { updatedUser ->
                if (updatedUser.id == currentUserId) return@collect
                users = users.map { if (it.id == updatedUser.id) updatedUser else it }
                trySend(buildPreviews())
            }
        }

        // شبكة أمان: إعادة بناء قائمة المحادثات دوريًا من REST حتى لو ضاع أي
        // حدث Realtime — هذا بالضبط ما كان يجعل الرسائل "لا تصل إلا عند
        // الخروج والعودة للتبويب"، لأن العودة كانت تُجبر إعادة الجلب يدويًا.
        val pollJob = launch {
            while (true) {
                delay(12_000)
                try {
                    users = databaseService.fetchUsersExcept(currentUserId)
                    publicLast = databaseService.fetchPublicMessages(limit = 1).lastOrNull() ?: publicLast
                    databaseService.fetchPrivateMessagesForUser(currentUserId, limit = 300).forEach { m ->
                        val otherId = if (m.senderId == currentUserId) m.recipientId else m.senderId
                        if (otherId != null) {
                            val existing = lastDirectByOtherUser[otherId]
                            if (existing == null || m.timestamp > existing.timestamp) {
                                lastDirectByOtherUser[otherId] = m
                            }
                        }
                    }
                    trySend(buildPreviews())
                } catch (e: Exception) {
                    // فشل مؤقت في الشبكة — Realtime سيحاول لوحده، لا داعي لإغلاق الـ Flow.
                }
            }
        }

        awaitClose {
            messagesJob.cancel()
            usersJob.cancel()
            pollJob.cancel()
            realtimeService.release()
        }
    }

    override suspend fun sendMessage(
        senderId: String,
        senderName: String,
        senderAvatarUrl: String?,
        senderTier: UserTier,
        recipientId: String?,
        text: String,
        imageUrl: String?,
        mentionedUserIds: List<String>
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty() && imageUrl.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("لا يمكن إرسال رسالة فارغة"))
        }

        val message = ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().replace("-", "").take(20),
            senderId = senderId,
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl,
            senderTier = senderTier,
            recipientId = recipientId,
            isPublic = recipientId == null,
            text = trimmedText,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            mentionedUserIds = mentionedUserIds
        )

        databaseService.sendMessage(message)
        // ملاحظة: منح +1 نقطة وإشعارات الرسائل الخاصة/الإشارات (@) في
        // الدردشة العامة تتم فعليًا داخل قاعدة البيانات عبر تريجر
        // handle_new_message (SECURITY DEFINER) في supabase/schema.sql
        // ومigrations/003، وليس من العميل. تمرير senderTier جاهزًا هنا
        // (بدل جلبه بطلب شبكة إضافي) يزيل جولة شبكة كاملة عن كل رسالة.
    }

    override suspend fun markMessagesAsRead(currentUserId: String, senderId: String) =
        withContext(Dispatchers.IO) {
            databaseService.markMessagesRead(currentUserId, senderId)
            Unit
        }

    override suspend fun deleteOwnMessage(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        databaseService.deleteOwnMessage(messageId)
    }
}
