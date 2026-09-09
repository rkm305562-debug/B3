package com.example.zainqhchat.data.repository

import com.example.zainqhchat.core.notification.LocalNotificationManager
import com.example.zainqhchat.data.remote.supabase.SupabaseDatabaseService
import com.example.zainqhchat.data.remote.supabase.SupabaseRealtimeService
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.NotificationItem
import com.example.zainqhchat.domain.model.NotificationType
import com.example.zainqhchat.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * تنفيذ حقيقي للإشعارات عبر Supabase + Realtime، مع عرض إشعار نظام أندرويد
 * فعلي فورًا (عبر [LocalNotificationManager]) عند وصول أي إشعار جديد بينما
 * التطبيق قيد التشغيل — دون أي Polling ودون Firebase.
 */
class NotificationRepositoryImpl(
    private val databaseService: SupabaseDatabaseService,
    private val realtimeService: SupabaseRealtimeService,
    private val localNotificationManager: LocalNotificationManager
) : NotificationRepository {

    override fun getNotificationsFlow(userId: String): Flow<List<NotificationItem>> = callbackFlow {
        realtimeService.acquire()
        val current = LinkedHashMap<Long, NotificationItem>()

        try {
            databaseService.fetchNotifications(userId).forEach { current[it.id] = it }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
        trySend(current.values.sortedByDescending { it.createdAtMillis })

        val job = launch {
            realtimeService.notificationInserts.collect { notification ->
                if (notification.userId != userId) return@collect
                val isNew = !current.containsKey(notification.id)
                current[notification.id] = notification
                trySend(current.values.sortedByDescending { it.createdAtMillis })

                // إشعار نظام فعلي فورًا — فقط لإشعار جديد فعلاً (وليس تكرارًا
                // من إعادة محاولة اتصال الـ Realtime).
                if (isNew && notification.type == NotificationType.FOLLOW) {
                    localNotificationManager.showFollowNotification(notification.title, notification.id)
                }
            }
        }

        awaitClose {
            job.cancel()
            realtimeService.release()
        }
    }

    override suspend fun markAsRead(notificationId: Long): Result<Unit> =
        databaseService.markNotificationRead(notificationId)

    override fun observeIncomingPrivateMessages(currentUserId: String): Flow<ChatMessage> = callbackFlow {
        realtimeService.acquire()
        val job = launch {
            realtimeService.messageChanges.collect { change ->
                val m = change.message
                if (change.type == "INSERT" &&
                    !m.isPublic &&
                    m.recipientId == currentUserId &&
                    m.senderId != currentUserId
                ) {
                    trySend(m)
                }
            }
        }
        awaitClose {
            job.cancel()
            realtimeService.release()
        }
    }
}
