package com.example.zainqhchat.data.remote.supabase

import android.util.Log
import com.example.zainqhchat.domain.model.ChatMessage
import com.example.zainqhchat.domain.model.NotificationItem
import com.example.zainqhchat.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

/** نوع تغيير حقيقي وصل عبر Supabase Realtime (Postgres Changes). */
data class MessageChange(val type: String, val message: ChatMessage)

/**
 * عميل Supabase Realtime حقيقي (بروتوكول Phoenix Channels عبر WebSocket) —
 * بدون أي بيانات وهمية وبدون Polling. اتصال WebSocket واحد فقط يُشارَك بين
 * كل شاشات التطبيق (Reference counting عبر [acquire]/[release]) بدلاً من فتح
 * اتصال أو قناة منفصلة لكل محادثة، ويُغلَق تلقائيًا عند عدم وجود أي مستخدم له.
 *
 * الاعتماد على RLS: القنوات مفتوحة على مستوى الجدول بالكامل (بدون فلتر عمود
 * واحد لأن postgres_changes لا يدعم شروط OR)، لكن Supabase Realtime يطبّق
 * سياسات RLS نفسها على كل عميل حسب الـ JWT المرسل عند الانضمام، لذلك كل عميل
 * يستقبل فقط التغييرات المصرَّح له برؤيتها فعليًا (نفس ضمان REST تمامًا).
 */
interface SupabaseRealtimeService {
    /** يزيد عداد المستخدمين النشطين للاتصال، ويفتح الـ WebSocket إن لم يكن مفتوحًا. */
    fun acquire()

    /** ينقص العداد، ويغلق الـ WebSocket فعليًا فقط عند وصوله للصفر (منع تسرّب الذاكرة). */
    fun release()

    val messageChanges: SharedFlow<MessageChange>
    val userChanges: SharedFlow<User>
    val notificationInserts: SharedFlow<NotificationItem>
}

class SupabaseRealtimeServiceImpl(
    private val authService: SupabaseAuthService
) : SupabaseRealtimeService {

    private val refCount = AtomicInteger(0)
    private var webSocket: WebSocket? = null
    private var supervisorJob: Job? = null
    private var scope: CoroutineScope? = null
    private var heartbeatRef = AtomicInteger(0)
    private var joinRef = AtomicInteger(0)
    private var reconnectAttempts = 0

    private val _messageChanges = MutableSharedFlow<MessageChange>(extraBufferCapacity = 64)
    override val messageChanges: SharedFlow<MessageChange> = _messageChanges.asSharedFlow()

    private val _userChanges = MutableSharedFlow<User>(extraBufferCapacity = 64)
    override val userChanges: SharedFlow<User> = _userChanges.asSharedFlow()

    private val _notificationInserts = MutableSharedFlow<NotificationItem>(extraBufferCapacity = 64)
    override val notificationInserts: SharedFlow<NotificationItem> = _notificationInserts.asSharedFlow()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    @Synchronized
    override fun acquire() {
        if (refCount.getAndIncrement() == 0) {
            connect()
        }
    }

    @Synchronized
    override fun release() {
        if (refCount.decrementAndGet() <= 0) {
            refCount.set(0)
            disconnect()
        }
    }

    @Synchronized
    private fun connect() {
        if (webSocket != null) return
        val job = SupervisorJob()
        supervisorJob = job
        val newScope = CoroutineScope(Dispatchers.IO + job)
        scope = newScope

        newScope.launch {
            val token = authService.currentValidAccessToken()
            openSocket(token)
        }
    }

    private fun openSocket(accessToken: String?) {
        val httpUrl = SupabaseConfig.getSupabaseUrl()
        val wsUrl = httpUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") +
            "/realtime/v1/websocket?apikey=${SupabaseConfig.getSupabaseAnonKey()}&vsn=1.0.0"

        val request = Request.Builder().url(wsUrl).build()
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                joinChannel(webSocket, "public:chat_messages", "chat_messages", accessToken)
                joinChannel(webSocket, "public:users", "users", accessToken)
                joinChannel(webSocket, "public:notifications", "notifications", accessToken)
                startHeartbeat(webSocket)
                startAuthRefresh(webSocket)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncoming(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Realtime socket failure: ${t.message}")
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (refCount.get() > 0) scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        val activeScope = scope ?: return
        if (refCount.get() <= 0) return
        reconnectAttempts++
        val delayMs = minOf(2000L * reconnectAttempts, 15_000L)
        webSocket = null
        activeScope.launch {
            delay(delayMs)
            if (refCount.get() > 0) {
                val token = authService.currentValidAccessToken()
                openSocket(token)
            }
        }
    }

    private fun joinChannel(socket: WebSocket, topicSuffix: String, table: String, accessToken: String?) {
        val ref = joinRef.incrementAndGet().toString()
        val changesConfig = JSONObject().put(
            "postgres_changes",
            org.json.JSONArray().put(
                JSONObject()
                    .put("event", "*")
                    .put("schema", "public")
                    .put("table", table)
            )
        )
        val payload = JSONObject().put("config", changesConfig)
        if (accessToken != null) payload.put("access_token", accessToken)

        val message = JSONObject()
            .put("topic", "realtime:$topicSuffix")
            .put("event", "phx_join")
            .put("payload", payload)
            .put("ref", ref)

        socket.send(message.toString())
    }

    /**
     * قنوات Supabase Realtime تتحقق من صلاحيات RLS باستخدام الـ JWT المُرسَل
     * عند الانضمام (phx_join) فقط — إن بقي الاتصال مفتوحًا لفترة طويلة (وهذا
     * متوقَّع لأنه اتصال مشترك واحد لكل التطبيق) وانتهت صلاحية ذلك التوكن،
     * تتوقف الأحداث اللحظية عن الوصول بصمت دون أي خطأ ظاهر، وهو على الأرجح
     * أحد أسباب "الرسائل لا تصل إلا بعد الخروج والعودة". لذلك نُعيد الانضمام
     * بتوكن جديد دوريًا لتحديث سياق RLS للقناة نفسها دون إعادة فتح الاتصال بالكامل.
     */
    private fun startAuthRefresh(socket: WebSocket) {
        val activeScope = scope ?: return
        activeScope.launch {
            while (refCount.get() > 0 && webSocket === socket) {
                delay(20 * 60_000L) // كل 20 دقيقة — أقل من عمر JWT الافتراضي (ساعة)
                val freshToken = authService.currentValidAccessToken()
                joinChannel(socket, "public:chat_messages", "chat_messages", freshToken)
                joinChannel(socket, "public:users", "users", freshToken)
                joinChannel(socket, "public:notifications", "notifications", freshToken)
            }
        }
    }

    private fun startHeartbeat(socket: WebSocket) {
        val activeScope = scope ?: return
        activeScope.launch {
            while (refCount.get() > 0 && webSocket === socket) {
                delay(25_000)
                val ref = heartbeatRef.incrementAndGet().toString()
                val heartbeat = JSONObject()
                    .put("topic", "phoenix")
                    .put("event", "heartbeat")
                    .put("payload", JSONObject())
                    .put("ref", ref)
                runCatching { socket.send(heartbeat.toString()) }
            }
        }
    }

    private fun handleIncoming(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event")

            if (event == "phx_reply") {
                val status = json.optJSONObject("payload")?.optString("status")
                if (status != "ok") {
                    Log.w(TAG, "Realtime join/reply not ok for topic=${json.optString("topic")}: $text")
                }
                return
            }

            if (event != "postgres_changes") return

            val payload = json.optJSONObject("payload") ?: return
            val data = payload.optJSONObject("data") ?: return
            val table = data.optString("table")
            // ملاحظة مهمة: الحقول الفعلية في حمولة Supabase Realtime هي
            // "eventType" و "new" و "old" (وليس "type"/"record"/"old_record"
            // كما في نسخة سابقة من هذا الملف) — هذا كان على الأرجح السبب
            // الجذري لعدم وصول أي تحديث لحظي إطلاقًا، لأن كل رسالة كانت
            // تُرفَض بصمت عند البحث عن حقل "record" غير الموجود. نتحقق هنا
            // من كلا التسميتين احتياطيًا لضمان العمل بغض النظر عن الإصدار.
            val type = data.optString("eventType", data.optString("type", ""))
            val record = data.optJSONObject("new") ?: data.optJSONObject("record") ?: return

            when (table) {
                "chat_messages" -> {
                    val message = SupabaseMappers.messageFromJson(record)
                    _messageChanges.tryEmit(MessageChange(type, message))
                }
                "users" -> {
                    val user = SupabaseMappers.userFromJson(record)
                    _userChanges.tryEmit(user)
                }
                "notifications" -> {
                    if (type.equals("INSERT", ignoreCase = true)) {
                        val notification = SupabaseMappers.notificationFromJson(record)
                        _notificationInserts.tryEmit(notification)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse realtime payload: ${e.message}")
        }
    }

    @Synchronized
    private fun disconnect() {
        webSocket?.close(1000, "no more subscribers")
        webSocket = null
        supervisorJob?.cancel()
        supervisorJob = null
        scope = null
    }

    companion object {
        private const val TAG = "SupabaseRealtime"
    }
}
