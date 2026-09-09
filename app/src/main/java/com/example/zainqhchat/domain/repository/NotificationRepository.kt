package com.example.zainqhchat.domain.repository

import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.NotificationItem
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الإشعارات — مربوطة فعليًا بجدول notifications في Supabase،
 * مع تحديث لحظي عبر Realtime عند وصول إشعار جديد (بدون Polling).
 */
interface NotificationRepository {
    fun getNotificationsFlow(userId: String): Flow<List<NotificationItem>>
    suspend fun markAsRead(notificationId: Long): Result<Unit>

    /**
     * بث حيّ لأي رسالة خاصة جديدة موجَّهة للمستخدم الحالي من أي محادثة كانت،
     * بغض النظر عن الشاشة المفتوحة حاليًا — يُستخدم لعرض إشعار نظام فعلي.
     */
    fun observeIncomingPrivateMessages(currentUserId: String): Flow<ChatMessage>
}
