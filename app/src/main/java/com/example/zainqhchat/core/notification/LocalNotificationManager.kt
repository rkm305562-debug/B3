package com.example.zainqhchat.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.zainqhchat.domain.model.ChatMessage

/**
 * مدير إشعارات محلي حقيقي (Android `NotificationManager` القياسي) — بدون
 * Firebase وبدون أي خدمة Push خارجية. يعرض إشعار نظام فعلي فورًا عند وصول
 * حدث حقيقي عبر Supabase Realtime (رسالة جديدة / متابعة جديدة) بينما تطبيق
 * ما يزال قيد التشغيل (في المقدمة أو الخلفية).
 *
 * ⚠️ حدّ حقيقي في نظام أندرويد نفسه (وليس قيدًا في الكود): لا يمكن لأي كود
 * أن "يوقظ" تطبيقًا مُغلَقًا بالكامل (Force-stopped / تمت إزالته من التطبيقات
 * الأخيرة) لعرض إشعار لحظي بدون خدمة Push حقيقية على مستوى نظام التشغيل
 * (FCM هو الخيار القياسي المجاني على أندرويد). هذا صحيح لأي تطبيق يعتمد على
 * WebSocket فقط (بما فيها Supabase Realtime)، وليس قصورًا في هذا التطبيق
 * تحديدًا.
 */
class LocalNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    MESSAGES_CHANNEL_ID,
                    "إشعارات الرسائل - دردشة توتة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "قناة إشعارات الرسائل الجديدة والدردشات الخاصة"
                    enableVibration(true)
                }
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    SOCIAL_CHANNEL_ID,
                    "إشعارات المتابعة - دردشة توتة",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "قناة إشعارات المتابعين الجدد"
                }
            )
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showMessageNotification(message: ChatMessage) {
        if (!hasNotificationPermission()) return

        val builder = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(message.senderName)
            .setContentText(message.text.ifEmpty { "📷 صورة جديدة" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(message.id.hashCode(), builder.build())
    }

    fun showFollowNotification(actorName: String, notificationId: Long) {
        if (!hasNotificationPermission()) return

        val builder = NotificationCompat.Builder(context, SOCIAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(actorName)
            .setContentText("بدأ بمتابعتك 👤")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify("follow_$notificationId".hashCode(), builder.build())
    }

    companion object {
        const val MESSAGES_CHANNEL_ID = "zainqh_chat_messages"
        const val SOCIAL_CHANNEL_ID = "zainqh_chat_social"
    }
}
