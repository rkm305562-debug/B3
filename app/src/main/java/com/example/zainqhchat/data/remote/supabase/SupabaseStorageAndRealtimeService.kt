package com.example.zainqhchat.data.remote.supabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * إدارة رفع الصور الحقيقي إلى حافلات (Buckets) Supabase Storage: avatars, chat-images.
 * الرفع يتم عبر REST API الحقيقي لـ Supabase Storage باستخدام Access Token
 * الخاص بالمستخدم المسجل دخوله (وليس service_role)، لتُطبَّق سياسات
 * storage.objects المعرّفة في supabase/schema.sql.
 */
interface SupabaseStorageService {
    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray, mimeType: String): Result<String>
    suspend fun uploadChatImage(chatId: String, imageBytes: ByteArray, mimeType: String): Result<String>
    fun getPublicUrl(bucket: String, path: String): String

    /**
     * حذف حقيقي لملف من Storage عبر رابطه العام الكامل (يُستخرَج منه اسم
     * الحافلة والمسار تلقائيًا). يعتمد على سياسات storage.objects في
     * supabase/schema.sql التي تسمح للمالك أو لمستخدم role='admin' بالحذف.
     * فشل الحذف هنا (مثلاً رابط خارجي غير تابع لمساحة التخزين) لا يُعتبر
     * خطأ فادحًا من طرف الاستدعاء الإداري — يُترك القرار للمستدعي.
     */
    suspend fun deleteObjectByPublicUrl(publicUrl: String): Result<Unit>
}

class SupabaseStorageServiceImpl(
    private val authService: SupabaseAuthService
) : SupabaseStorageService {

    private val storageBaseUrl get() = "${SupabaseConfig.getSupabaseUrl()}/storage/v1"

    override suspend fun uploadAvatar(userId: String, imageBytes: ByteArray, mimeType: String): Result<String> {
        val extension = extensionFor(mimeType)
        val path = "$userId/avatar_${System.currentTimeMillis()}.$extension"
        return upload(SupabaseConfig.Buckets.AVATARS, path, imageBytes, mimeType)
    }

    override suspend fun uploadChatImage(chatId: String, imageBytes: ByteArray, mimeType: String): Result<String> {
        val extension = extensionFor(mimeType)
        val path = "$chatId/img_${System.currentTimeMillis()}.$extension"
        return upload(SupabaseConfig.Buckets.CHAT_IMAGES, path, imageBytes, mimeType)
    }

    override fun getPublicUrl(bucket: String, path: String): String =
        "${SupabaseConfig.getSupabaseUrl()}/storage/v1/object/public/$bucket/$path"

    override suspend fun deleteObjectByPublicUrl(publicUrl: String): Result<Unit> {
        return try {
            val marker = "/storage/v1/object/public/"
            val idx = publicUrl.indexOf(marker)
            if (idx == -1) return Result.failure(IllegalArgumentException("رابط غير تابع لمساحة تخزين المشروع"))

            val bucketAndPath = publicUrl.substring(idx + marker.length)
            val token = authService.currentValidAccessToken()
                ?: return Result.failure(IllegalStateException("لا توجد جلسة صالحة لحذف الملف"))

            val request = Request.Builder()
                .url("$storageBaseUrl/object/$bucketAndPath")
                .headers(SupabaseHttp.baseHeaders(token).build())
                .delete()
                .build()

            withContext(Dispatchers.IO) { SupabaseHttp.execute(request) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun upload(bucket: String, path: String, bytes: ByteArray, mimeType: String): Result<String> {
        return try {
            val token = authService.currentValidAccessToken()
                ?: return Result.failure(IllegalStateException("لا توجد جلسة صالحة لرفع الصورة"))

            val request = Request.Builder()
                .url("$storageBaseUrl/object/$bucket/$path")
                .headers(
                    SupabaseHttp.baseHeaders(token)
                        .add("x-upsert", "true")
                        .build()
                )
                .post(bytes.toRequestBody(mimeType.toMediaType()))
                .build()

            withContext(Dispatchers.IO) { SupabaseHttp.execute(request) }
            Result.success(getPublicUrl(bucket, path))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extensionFor(mimeType: String): String = when (mimeType.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
}
