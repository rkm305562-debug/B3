# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# =====================================================================
# قواعد إضافية خاصة بهذا المشروع — لتفعيل R8/التصغير بأمان (حماية أساسية
# من فك الحزم بسهولة/الاستنساخ المباشر) دون كسر أي مكتبة تعتمد جزئيًا على
# الانعكاس (Reflection) وقت التشغيل.
# =====================================================================

# Room: تصنيفات الكيانات وقواعد البيانات المحلية (التخزين المؤقت فقط هنا)
-keep class com.example.zainqhchat.data.local.entities.** { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# نماذج المجال (Domain models) — تُبنى يدويًا من JSON خام (org.json) وليس
# عبر مكتبة تسلسل بالانعكاس، لكن نُبقيها كاملة الأسماء احتياطًا لأي كود
# مستقبلي قد يعتمد على أسماء الحقول (مثل تصحيح أخطاء عبر Logcat).
-keep class com.example.zainqhchat.domain.model.** { *; }

# OkHttp / Okio (تحذيرات شائعة غير ضارة عند التصغير)
-dontwarn okhttp3.**
-dontwarn okio.**

# Jetpack Compose يُولَّد وقت الترجمة (لا انعكاس وقت التشغيل) — قواعده
# الافتراضية المرفقة مع المكتبة كافية تلقائيًا؛ لا حاجة لقواعد إضافية هنا.
