package com.example.zainqhchat.data.remote.supabase

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

/**
 * جلسة مصادقة حقيقية راجعة من Supabase Auth (GoTrue).
 */
data class SupabaseAuthSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long
)

/**
 * خدمة المصادقة الحقيقية عبر Supabase Auth REST API (GoTrue) — بدون أي Mock.
 * التطبيق يستخدم اسم مستخدم فقط (بدون بريد إلكتروني)، لذا يُشتق بريد داخلي
 * ثابت وحتمي من اسم المستخدم (username@zainqhchat.com) يُستخدم فقط كمعرّف
 * تسجيل دخول لدى Supabase Auth ولا يُستخدم للتواصل الفعلي.
 *
 * ملاحظة تشغيلية مهمة: يجب تعطيل خيار "Confirm email" في إعدادات
 * Supabase Auth (Authentication -> Providers -> Email) لأن التطبيق لا يملك
 * صندوق بريد حقيقي لتأكيد الحساب. هذا إعداد يتم على لوحة تحكم Supabase
 * ولا يمكن ضبطه من كود التطبيق أو من هذا الملف.
 */
interface SupabaseAuthService {
    suspend fun signUp(username: String, password: String, name: String, age: Int): Result<SupabaseAuthSession>
    suspend fun signIn(username: String, password: String): Result<SupabaseAuthSession>
    suspend fun signOut(): Result<Unit>

    /**
     * يغيّر كلمة مرور المستخدم الحالي (يتطلب جلسة صالحة). يُستخدم من شاشة
     * الإعدادات كي يستطيع من سجّل بدون كلمة مرور (تسجيل باسم فقط) أن يضيف
     * واحدة لاحقًا للاحتفاظ بإمكانية الدخول من جهاز آخر أو بعد حذف التطبيق.
     */
    suspend fun updatePassword(newPassword: String): Result<Unit>

    /** يعيد Access Token صالحًا (ويجدده تلقائيًا عبر refresh_token عند الحاجة) أو null إن لم توجد جلسة. */
    suspend fun currentValidAccessToken(): String?

    fun currentUserId(): String?
    fun hasSession(): Boolean
}

class SupabaseAuthServiceImpl(context: Context) : SupabaseAuthService {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val authBaseUrl get() = "${SupabaseConfig.getSupabaseUrl()}/auth/v1"

    override suspend fun signUp(
        username: String,
        password: String,
        name: String,
        age: Int
    ): Result<SupabaseAuthSession> {
        val email = usernameToInternalEmail(username)
        return try {
            val metadata = JSONObject()
                .put("username", username)
                .put("name", name)
                .put("age", age)

            val body = JSONObject()
                .put("email", email)
                .put("password", password)
                .put("data", metadata)

            val request = Request.Builder()
                .url("$authBaseUrl/signup")
                .headers(SupabaseHttp.baseHeaders(null).build())
                .post(SupabaseHttp.jsonBody(body))
                .build()

            val responseBody = SupabaseHttp.execute(request)
            val json = JSONObject(responseBody)

            val session = extractSession(json)
                ?: run {
                    // بعض إعدادات Supabase تتطلب تأكيد البريد قبل إرجاع جلسة.
                    // بما أن التطبيق يعتمد اسم مستخدم فقط، نحاول تسجيل الدخول
                    // مباشرة بنفس البيانات كخطوة ثانية حقيقية (وليست بديلاً وهميًا).
                    return signIn(username, password)
                }

            persistSession(session)
            Result.success(session)
        } catch (e: SupabaseApiException) {
            Result.failure(mapSignUpError(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(username: String, password: String): Result<SupabaseAuthSession> {
        val email = usernameToInternalEmail(username)
        return try {
            val body = JSONObject()
                .put("email", email)
                .put("password", password)

            val request = Request.Builder()
                .url("$authBaseUrl/token?grant_type=password")
                .headers(SupabaseHttp.baseHeaders(null).build())
                .post(SupabaseHttp.jsonBody(body))
                .build()

            val responseBody = SupabaseHttp.execute(request)
            val json = JSONObject(responseBody)
            val session = extractSession(json)
                ?: return Result.failure(IllegalStateException("تعذر إنشاء الجلسة من استجابة Supabase"))

            persistSession(session)
            Result.success(session)
        } catch (e: SupabaseApiException) {
            Result.failure(mapSignInError(e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        return try {
            if (token != null) {
                val request = Request.Builder()
                    .url("$authBaseUrl/logout")
                    .headers(SupabaseHttp.baseHeaders(token).build())
                    .post("".toRequestBody(null))
                    .build()
                runCatching { SupabaseHttp.execute(request) }
            }
            Result.success(Unit)
        } finally {
            clearSession()
        }
    }

    /**
     * يغيّر كلمة مرور المستخدم الحالي عبر `PUT /auth/v1/user` باستخدام Access
     * Token الحالي — هذا هو المسار الرسمي في GoTrue لتحديث بيانات المستخدم
     * المصادَق عليه نفسه (وليس مسار إعادة تعيين عبر بريد إلكتروني، لأن
     * التطبيق لا يملك بريدًا حقيقيًا للمستخدم أصلاً).
     */
    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        val token = currentValidAccessToken()
            ?: return Result.failure(IllegalStateException("الجلسة غير صالحة، يرجى تسجيل الدخول من جديد"))
        return try {
            val body = JSONObject().put("password", newPassword)
            val request = Request.Builder()
                .url("$authBaseUrl/user")
                .headers(SupabaseHttp.baseHeaders(token).build())
                .put(SupabaseHttp.jsonBody(body))
                .build()
            SupabaseHttp.execute(request)
            Result.success(Unit)
        } catch (e: SupabaseApiException) {
            Result.failure(
                if (e.message?.contains("password", true) == true && e.message?.contains("6", true) == true) {
                    IllegalStateException("كلمة المرور يجب أن تكون 6 خانات على الأقل")
                } else {
                    e
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun currentValidAccessToken(): String? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)

        val stillValid = System.currentTimeMillis() < (expiresAt - EXPIRY_SAFETY_MARGIN_MS)
        if (stillValid) return accessToken
        if (refreshToken == null) return null

        return try {
            val body = JSONObject().put("refresh_token", refreshToken)
            val request = Request.Builder()
                .url("$authBaseUrl/token?grant_type=refresh_token")
                .headers(SupabaseHttp.baseHeaders(null).build())
                .post(SupabaseHttp.jsonBody(body))
                .build()

            val responseBody = SupabaseHttp.execute(request)
            val json = JSONObject(responseBody)
            val session = extractSession(json) ?: return null
            persistSession(session)
            session.accessToken
        } catch (e: Exception) {
            // فشل تجديد الجلسة (مثلاً refresh token منتهي) -> يجب تسجيل الدخول من جديد.
            clearSession()
            null
        }
    }

    override fun currentUserId(): String? = prefs.getString(KEY_USER_ID, null)

    override fun hasSession(): Boolean = prefs.getString(KEY_ACCESS_TOKEN, null) != null

    private fun extractSession(json: JSONObject): SupabaseAuthSession? {
        val accessToken = if (json.has("access_token")) json.optString("access_token", null) else null
        accessToken ?: return null
        val refreshToken = json.optString("refresh_token", "")
        val expiresIn = json.optLong("expires_in", 3600L)
        val userId = json.optJSONObject("user")?.optString("id")
        userId ?: return null

        return SupabaseAuthSession(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = System.currentTimeMillis() + expiresIn * 1000L
        )
    }

    private fun persistSession(session: SupabaseAuthSession) {
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAtMillis)
            .apply()
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun mapSignUpError(e: SupabaseApiException): Exception {
        val msg = e.message.orEmpty()
        return when {
            e.statusCode == 422 || e.errorCode == "user_already_exists" ->
                IllegalStateException("اسم المستخدم مستخدم بالفعل")

            e.errorCode == "email_address_invalid" || msg.contains("invalid", true) ->
                IllegalStateException(
                    "اسم المستخدم يحتوي على رموز غير مقبولة، أو أن مشروع Supabase " +
                        "لديك مفعّل عليه قيد \"Allowed email domains\" في Authentication " +
                        "يمنع النطاق الداخلي المستخدم. جرّب اسم مستخدم بحروف/أرقام إنجليزية " +
                        "فقط، وإن استمرت المشكلة تحقق من هذا القيد في لوحة تحكم Supabase."
                )

            e.errorCode == "over_email_send_rate_limit" || msg.contains("rate limit", true) ->
                IllegalStateException(
                    "تم تجاوز الحد المسموح من رسائل التأكيد لأن خيار \"Confirm email\" " +
                        "ما زال مُفعَّلاً في مشروع Supabase (Authentication → Sign In / " +
                        "Providers → Email). يجب تعطيله كي لا يحاول Supabase إرسال أي بريد " +
                        "إطلاقًا عند التسجيل — هذا هو الحل الوحيد الدائم؛ لا يوجد حل من كود " +
                        "التطبيق لتجاوز حد إرسال البريد المفروض من خادم Supabase نفسه."
                )

            else -> e
        }
    }

    private fun mapSignInError(e: SupabaseApiException): Exception {
        val msg = e.message.orEmpty()
        return when {
            e.statusCode == 400 && msg.contains("Invalid login credentials", true) ->
                IllegalStateException("اسم المستخدم أو كلمة المرور غير صحيحة")

            msg.contains("Email not confirmed", true) ->
                IllegalStateException(
                    "الحساب بانتظار تأكيد البريد على مستوى مشروع Supabase. " +
                        "يجب تعطيل \"Confirm email\" من إعدادات Authentication في لوحة تحكم Supabase " +
                        "لأن التطبيق يعتمد اسم مستخدم فقط بدون بريد حقيقي."
                )

            e.errorCode == "over_email_send_rate_limit" || msg.contains("rate limit", true) ->
                IllegalStateException(
                    "تم تجاوز حد إرسال البريد من Supabase. عطّل \"Confirm email\" من " +
                        "لوحة تحكم Supabase لمنع إرسال أي بريد أصلاً عند تسجيل الدخول/التسجيل."
                )

            else -> e
        }
    }

    /**
     * يبني بريدًا داخليًا حتميًا (Deterministic) من اسم المستخدم عبر تجزئة
     * SHA-256 بدل استخدام الأحرف مباشرة — هذا يدعم أي لغة كاملة (عربي، إنجليزي،
     * أرقام، مسافات) في اسم المستخدم دون أي تصفية للأحرف، لأن ناتج البريد
     * دائمًا ASCII صرف بغض النظر عن الأحرف الأصلية. نفس اسم المستخدم يُنتج
     * دائمًا نفس البريد (بعد تطبيع Unicode وتوحيد حالة الأحرف اللاتينية)، لذا
     * يعمل تسجيل الدخول بشكل صحيح لاحقًا بإدخال نفس الاسم مجددًا.
     *
     * ⚠️ إصلاح مهم: NFKC وحدها لا توحّد بين حروف عربية/فارسية متشابهة الشكل
     * لكنها Unicode codepoints مختلفة فعليًا — أشهرها الياء الفارسية ی
     * (U+06CC) مقابل الياء العربية ي (U+064A)، والكاف الفارسية ک (U+06A9)
     * مقابل الكاف العربية ك (U+0643)، بالإضافة للأرقام الهندية الشرقية
     * ٠١٢٣ مقابل الأرقام اللاتينية. لوحات مفاتيح أندرويد (خصوصًا Gboard مع
     * التصحيح التلقائي) قد تُدخل نسخة مختلفة من نفس "الحرف الظاهر" بين مرة
     * التسجيل ومرة الدخول لاحقًا، فيختلف البريد الداخلي الناتج ويفشل الدخول
     * برسالة "اسم المستخدم أو كلمة المرور غير صحيحة" رغم أن المستخدم كتب
     * نفس الاسم تمامًا من ناحيته. لذلك نوحّد هذه المتغيرات يدويًا، ونزيل أي
     * محارف خفية (Zero-width / Bidi marks) قد تُدرَج بصمت من بعض لوحات
     * المفاتيح، قبل التجزئة.
     */
    private fun usernameToInternalEmail(username: String): String {
        val normalized = java.text.Normalizer
            .normalize(username.trim(), java.text.Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .let(::canonicalizeArabicVariants)
            .replace(INVISIBLE_CHARS_REGEX, "")
            .replace(Regex("\\s+"), " ")
            .trim()
        val digestBytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(normalized.toByteArray(Charsets.UTF_8))
        val hex = digestBytes.joinToString("") { "%02x".format(it) }.take(24)
        return "u$hex@zainqhchat.com"
    }

    /** يوحّد المتغيرات الفارسية/الهندية الشرقية الشائعة إلى مقابلها العربي/اللاتيني القياسي. */
    private fun canonicalizeArabicVariants(text: String): String {
        val builder = StringBuilder(text.length)
        for (ch in text) {
            builder.append(
                when (ch) {
                    '\u06CC', '\u0649' -> '\u064A' // ی / ى الفارسية أو المقصورة → ي العربية القياسية
                    '\u06A9' -> '\u0643' // ک الفارسية → ك العربية
                    '\u06AF' -> '\u0643' // گ (كاف فارسية بثلاث نقاط) → ك
                    '\u0654', '\u0655' -> ' ' // علامات همزة منفصلة نادرة — تُهمَل بدل التسبب بفرق
                    '\u0660' -> '0'
                    '\u0661' -> '1'
                    '\u0662' -> '2'
                    '\u0663' -> '3'
                    '\u0664' -> '4'
                    '\u0665' -> '5'
                    '\u0666' -> '6'
                    '\u0667' -> '7'
                    '\u0668' -> '8'
                    '\u0669' -> '9'
                    '\u06F0' -> '0'
                    '\u06F1' -> '1'
                    '\u06F2' -> '2'
                    '\u06F3' -> '3'
                    '\u06F4' -> '4'
                    '\u06F5' -> '5'
                    '\u06F6' -> '6'
                    '\u06F7' -> '7'
                    '\u06F8' -> '8'
                    '\u06F9' -> '9'
                    else -> ch
                }
            )
        }
        return builder.toString()
    }

    companion object {
        private const val PREFS_NAME = "zainqh_supabase_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val EXPIRY_SAFETY_MARGIN_MS = 60_000L

        // محارف خفية قد تُدرجها بعض لوحات المفاتيح بصمت (لا تظهر للعين إطلاقًا)
        // بين مرة كتابة وأخرى: علامات اتجاه النص (LRM/RLM/ALM)، فواصل عرض
        // الكلمة الصفرية (ZWJ/ZWNJ)، والمسافة ذات العرض الصفري (ZWSP).
        private val INVISIBLE_CHARS_REGEX = Regex("[\u200B\u200C\u200D\u200E\u200F\u061C\uFEFF]")
    }
}
