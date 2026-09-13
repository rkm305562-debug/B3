package com.example.zainqhchat.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * يضغط أي صورة يختارها المستخدم (صورة شخصية أو صورة دردشة) قبل رفعها إلى
 * الخادم: يُصغّرها لأقصى بُعد معقول، ويعيد ترميزها كـ JPEG بجودة جيدة —
 * بدل رفع الصورة الأصلية كما هي مهما كانت ضخمة (بعض كاميرات الهواتف
 * الحديثة تُنتج صورًا بعشرات الميغابايت). هذا يوفّر بيانات الجوّال، يُسرِّع
 * الرفع خصوصًا على اتصال ضعيف، ويقلّل احتمال أي بطء/تعليق مؤقت للواجهة على
 * الأجهزة الأضعف عند التعامل مع صور ضخمة.
 */
object ImageCompressor {

    /** إعداد جاهز لصور الدردشة (وضوح كافٍ للعرض الكامل على الشاشة). */
    const val MAX_DIMENSION_CHAT_IMAGE = 1280

    /** إعداد جاهز للصور الشخصية (لا تُعرض أبدًا بحجم أكبر من ذلك في التطبيق). */
    const val MAX_DIMENSION_AVATAR = 512

    /**
     * يعيد زوج (bytes مضغوطة, "image/jpeg") أو null إن تعذّرت قراءة/فك ترميز
     * الصورة (ملف تالف، صيغة غير مدعومة، إلخ) — في تلك الحالة على المستدعي
     * التعامل مع الفشل بأمان (مثل عدم تغيير الصورة الحالية) بدل الانهيار.
     */
    fun compress(context: Context, uri: Uri, maxDimension: Int, quality: Int = 85): Pair<ByteArray, String>? {
        return try {
            val original = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val scaled = if (original.width > maxDimension || original.height > maxDimension) {
                val ratio = minOf(
                    maxDimension.toFloat() / original.width,
                    maxDimension.toFloat() / original.height
                )
                val newWidth = (original.width * ratio).toInt().coerceAtLeast(1)
                val newHeight = (original.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(original, newWidth, newHeight, true).also {
                    if (it !== original) original.recycle()
                }
            } else {
                original
            }

            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            if (scaled !== original) scaled.recycle()

            outputStream.toByteArray() to "image/jpeg"
        } catch (e: Exception) {
            // ملف صورة تالف/غير مدعوم/نفاد ذاكرة نادر — نُعيد null بأمان بدل
            // إسقاط التطبيق أثناء اختيار صورة أو إرسالها.
            null
        }
    }
}
