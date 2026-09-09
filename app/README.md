# ZainQH Chat 👑📱 - Luxury Gold & Obsidian Android Chat Application

تطبيق **ZainQH Chat** هو تطبيق دردشة احترافي فاخر مصمم بأحدث تقنيات **Android (Kotlin & Jetpack Compose)** مع واجهة مستخدم ذهبية سوداء فاخرة (Gold & Obsidian Theme) وهيكلية نظيفة (Clean Architecture / MVVM).

---

## 🌟 أبرز الميزات (Features)

- 👑 **تصميم ملكي فاخر (Luxury Design)**: واجهة مستخدم باللون الأسود الملكي والأصفر الذهبي مع تأثيرات التدرج واللمعان (Gold Shimmer).
- 💬 **دردشة عامة وخاصة (Public & Private Chat)**: دعم الغرف العامة والمحادثات الثنائية المباشرة مع مؤشرات قراءة ورسائل صوتية وصور.
- 🟢 **حالة الاتصال والترتيب (Online Status & Hierarchy)**:
  - متصل الآن 🟢 / مشغول 🔴 / بعيد 🟡 / غير متصل ⚪.
  - رتب مستخدمين برونزية وفضية وذهبية وماسية وملكية (Bronze, Silver, Gold, Diamond, Royal 👑).
  - نظام نقاط تفاعلي ورتب تلقائية.
- 🔒 **حماية وأمان وسيرة احترافية (Security & Profile)**:
  - خيارات المتابعة، الإبلاغ عن المحتوى، والحظر (Follow, Report, Block).
  - تسجيل واثق باستخدام اسم المستخدم وكلمة المرور.
- 🛡️ **دعم منطقة الأمان (Safe Area / WindowInsets)**: توافق كامل مع كافة الهواتف وقطع الشاشة (Notch / Dynamic Island / Status Bars).
- ⚡ **جاهز للربط مع Supabase (Supabase Integration Ready)**:
  - Supabase Auth.
  - Supabase Database (PostgreSQL).
  - Supabase Storage (`avatars` & `chat-images`).
  - Supabase Realtime (WebSockets / Live Chat).

---

## 🛠️ التقنيات المستخدمة (Tech Stack)

- **اللغة**: Kotlin
- **الواجهات**: Jetpack Compose (Material Design 3)
- **المعمارية**: Clean Architecture (Data / Domain / UI Layers) + MVVM
- **حفظ البيانات المحلية**: Room Database + Flow
- **الشبكة والتخزين السحابي**: Supabase (Auth, PostgREST, Storage, Realtime)
- **إدارة التبعيات**: Gradle Kotlin DSL (`build.gradle.kts`)

---

## 🚀 كيفية تشغيل المشروع وتوليد ملف APK

### 1. المتطلبات الأساسية
- **Android Studio** (إصدار Giraffe أو أحدث).
- **JDK 17** أو أحدث.

### 2. البناء والتجميع عبر السطر الأوامري
```bash
# تنظيف وتجهيز المشروع
./gradlew clean

# بناء النسخة المخصصة للتطوير (Debug APK)
./gradlew assembleDebug

# إجراء الفحوصات والتأكد من جودة الكود
./gradlew check
```

سيكون ملف ה-APK الجاهز متاحاً في المسار:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 دليل التجهيز الشامل
للحصول على شرح تفصيلي لهيكلية الملفات وكيفية إعداد جداول Supabase وسياسات RLS، يرجى مراجعة ملف:
👉 [`CLAUDE_SETUP.md`](./CLAUDE_SETUP.md)

---

## 📜 الترخيص (License)
جميع الحقوق محفوظة لمشروع **ZainQH Chat** © 2026.
