package com.example.zainqhchat.data.remote.supabase

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * استثناء حقيقي يحمل تفاصيل خطأ Supabase (HTTP status + رسالة الخادم) بدلاً
 * من ابتلاع الأخطاء أو إرجاع نتائج وهمية.
 */
class SupabaseApiException(
    val statusCode: Int,
    val errorCode: String? = null,
    message: String
) : IOException(message)

/**
 * أدوات HTTP منخفضة المستوى مشتركة بين خدمات Supabase (Auth / PostgREST / Storage).
 * تستخدم OkHttp الحقيقي (موجود بالفعل ضمن تبعيات المشروع) لإجراء طلبات شبكة فعلية،
 * دون أي بيانات وهمية أو Stub.
 */
internal object SupabaseHttp {

    val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()
    }

    fun baseHeaders(accessToken: String?): Headers.Builder {
        val builder = Headers.Builder()
            .add("apikey", SupabaseConfig.getSupabaseAnonKey())
            .add("Authorization", "Bearer ${accessToken ?: SupabaseConfig.getSupabaseAnonKey()}")
        return builder
    }

    fun jsonBody(json: JSONObject): RequestBody = json.toString().toRequestBody(JSON_MEDIA_TYPE)

    fun jsonBody(json: JSONArray): RequestBody = json.toString().toRequestBody(JSON_MEDIA_TYPE)

    /**
     * ينفّذ الطلب فعليًا عبر OkHttp على Dispatchers.IO، ويرمي [SupabaseApiException]
     * الحقيقي عند فشل الاستجابة بدل إخفاء الخطأ.
     */
    suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val (code, message) = parseError(bodyString, response.message)
                throw SupabaseApiException(response.code, code, message)
            }
            bodyString
        }
    }

    private fun parseError(body: String, fallback: String): Pair<String?, String> {
        return try {
            val json = JSONObject(body)
            val message = json.optString("msg", json.optString("message", json.optString("error_description", fallback)))
            val code = json.optString("error_code", json.optString("code", null))
            code to message
        } catch (e: Exception) {
            null to (body.ifBlank { fallback })
        }
    }
}
