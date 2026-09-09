# دليل التطوير التقني الشامل وإرشادات Claude (CLAUDE_INSTRUCTIONS.md)

هذا المستند هو الدليل التقني المرجعي الشامل لتطوير وصيانة مشروع **ZainQH Chat** لضمان التوافق التام مع **Supabase**، **GitHub Actions**، نظام **Gradle**، وبيئة **Android Jetpack Compose**.

---

## 1. هيكل المشروع (Project Architecture & Structure)

المشروع مبني وفق معمارية **Clean Architecture + MVVM** باستخدام **Kotlin** و **Jetpack Compose**.

```text
app/src/main/java/com/example/zainqhchat/
├── core/                           # الخدمات المساعدة والوظائف الأساسية
│   └── notification/
│       └── FCMNotificationManager.kt   # إدارة إشعارات الدفع (Push Notifications)
├── data/                           # طبقة البيانات والاتصالات
│   ├── local/                      # التخزين المحلي باستخدام Room Database
│   │   ├── AppDatabase.kt          # قاعدة بيانات Room المحلية
│   │   ├── dao/                    # كائنات الوصول للبيانات (UserDao, ChatMessageDao, SocialDao)
│   │   └── entities/               # كيانات التخزين المحلي (UserEntity, ChatMessageEntity, SocialEntities)
│   ├── remote/                     # واجهات الاتصال الخارجية والـ Remote APIs
│   │   └── supabase/               # خدمات وقواعد بيانات Supabase
│   │       ├── SupabaseConfig.kt                   # إعدادات الربط وعناوين Supabase
│   │       ├── SupabaseAuthService.kt             # إدارة المصادقة والتسجيل
│   │       ├── SupabaseDatabaseService.kt         # عمليات قاعدة البيانات والرسائل
│   │       └── SupabaseStorageAndRealtimeService.kt # رفع الصور واستقبال التحديثات الحية
│   └── repository/                 # تنفيذ واجهات Repositories
│       ├── AuthRepositoryImpl.kt
│       ├── ChatRepositoryImpl.kt
│       └── UserRepositoryImpl.kt
├── domain/                         # طبقة منطق العمل والمجال (Domain Layer)
│   ├── model/                      # نماذج البيانات (User, ChatMessage, UserTier, UserSettings, ChatPreview)
│   └── repository/                 # واجهات العقود (AuthRepository, ChatRepository, UserRepository)
├── ui/                             # طبقة الواجهات والـ ViewModel (Jetpack Compose)
│   ├── components/                 # العناصر المكررة (ChatBubble, UserAvatar, TierBadge, GoldComponents)
│   ├── screens/                    # الشاشات الرئيسية
│   │   ├── HomeScreen.kt           # الشاشة الرئيسية والتبويبات
│   │   ├── ChatDetailScreen.kt     # شاشة الدردشة العامة والخاصة (IME & Keyboard Insets)
│   │   ├── AuthScreen.kt           # شاشة تسجيل الدخول وإنشاء الحساب
│   │   ├── ProfileScreen.kt        # شاشة الملف الشخصي والمستويات
│   │   ├── ChatListSection.kt      # قائمة المحادثات النشطة
│   │   ├── OnlineSection.kt        # قائمة المتواجدين الآن
│   │   ├── SettingsSection.kt      # شاشة الإعدادات وتعديل الحساب
│   │   └── SplashScreen.kt         # شاشة الترحيب
│   ├── theme/                      # الألوان والأنماط (Theme, Color, Type - Luxury Dark Gold Palette)
│   └── viewmodels/                 # حالات الواجهة (AuthViewModel, ChatViewModel, UserViewModel, SettingsViewModel)
└── MainActivity.kt                 # النقطة الرئيسية للتطبيق وإدارة حالة التنقل
```

### تصنيف الملفات والحساسية:
- **ملفات حساسة جدًا (يمنع تعديلها بدون سبب تقني حرج):**
  - `.github/workflows/android-build.yml`
  - `debug.keystore.base64` & `debug.keystore`
  - `gradle/wrapper/gradle-wrapper.jar`
  - `build.gradle.kts` & `app/build.gradle.kts`
  - `AndroidManifest.xml`
  - `metadata.json` (الخاص بـ Google AI Studio)
- **ملفات آمنة للتعديل والتطوير:**
  - جميع ملفات المجلد `app/src/main/java/com/example/zainqhchat/` (الـ UI، ViewModels، Repositories، Models، Supabase Services).

---

## 2. ربط Supabase وإدارة الأسرار (Supabase & Secrets Management)

1. **متغيرات البيئة (`.env`):**
   - يتم تخزين مفاتيح Supabase داخل ملف `.env` بالشكل التالي:
     ```env
     SUPABASE_URL=https://your-supabase-project.supabase.co
     SUPABASE_ANON_KEY=your-supabase-anon-key
     ```
2. **الوصول عبر `BuildConfig`:**
   - يقرأ مشروع Gradle المتغيرات تلقائيًا عبر التكوين في `app/build.gradle.kts` ويُنشئ:
     ```kotlin
     val url = BuildConfig.SUPABASE_URL
     val key = BuildConfig.SUPABASE_ANON_KEY
     ```
3. **قواعد الأمان:**
   - **يمنع تمامًا** كتابة المفاتيح مباشرة داخل الكود المصدري (Hardcoding).
   - **يمنع رفع** ملف `.env` إلى GitHub (مضمّن بالفعل في `.gitignore`).
   - يجب توفير نموذج تجريبي دائماً في `.env.example`.

---

## 3. قاعدة البيانات ومخطط Supabase (Database Schema & RLS)

### الجداول وعلاقاتها:

1. **جدول المستخدمين (`users`):**
   - `id` (text, Primary Key)
   - `username` (text, Unique, Index) - اسم المستخدم الفريد.
   - `display_name` (text) - اسم العرض.
   - `age` (integer) - العمر.
   - `avatar_url` (text, Nullable) - رابط الصورة الشخصية المرفوعة.
   - `points` (integer, Default: 0) - نقاط التفاعل.
   - `is_online` (boolean, Default: false)
   - `last_seen` (bigint)

2. **جدول الرسائل (`chat_messages`):**
   - `id` (text, Primary Key)
   - `sender_id` (text, Foreign Key -> `users.id`)
   - `receiver_id` (text, Nullable, Foreign Key -> `users.id`) - يكون `null` للرسائل العامة.
   - `text` (text)
   - `image_url` (text, Nullable)
   - `timestamp` (bigint)
   - `is_read` (boolean, Default: false)

3. **جدول المتابعات (`follows`):**
   - `follower_id` (text, Foreign Key -> `users.id`)
   - `following_id` (text, Foreign Key -> `users.id`)
   - Composite Primary Key (`follower_id`, `following_id`)

4. **جدول إعدادات المستخدم (`user_settings`):**
   - `user_id` (text, Primary Key, Foreign Key -> `users.id`)
   - `notifications_enabled` (boolean, Default: true)
   - `sound_enabled` (boolean, Default: true)
   - `dark_theme` (boolean, Default: true)

### حاويات التخزين (Supabase Storage Buckets):
- **`avatars`**: تخزين الصور الشخصية للمستخدمين المرفوعة من معرض الجهاز (Public Access للقراءة، Authenticated للرفع).
- **`chat-images`**: تخزين الصور المرفقة داخل المحادثات.

### سياسات RLS (Row Level Security):
- يجب تفعيل RLS على جميع الجداول، والسموح للمستخدمين المسجلين بقراءة البيانات وإضافة رسائلهم وتحديث بياناتهم الشخصية فقط.

---

## 4. تعليمات خاصة بمشكلة مربع الكتابة ولواحق الشاشة (IME & Keyboard Insets)

### التحليل الحقيقي للمشكلة:
ارتفاع مربع الكتابة إلى منتصف الشاشة أو قفزه للأعلى ينجم عن تعارض بين `Scaffold` المزدوج وتمرير `WindowInsets` أكثر من مرة أو تطبيق `imePadding()` في مستويات غير صحيحة داخل الهيكل.

### الحل النهائي المعتمد في `ChatDetailScreen.kt`:
1. **استخدام `bottomBar` في `Scaffold` لثبات مربع الإدخال:**
   ضع شريط إدخال الرسالة داخل خاصية `bottomBar` الخاصة بـ `Scaffold` بدلاً من وضعه كعنصر أخير داخل `Column` بـ `weight(1f)`.
2. **استخدام `windowInsetsPadding` بدقة:**
   ```kotlin
   Surface(
       color = LuxurySurfaceCard,
       tonalElevation = 8.dp,
       modifier = Modifier
           .fillMaxWidth()
           .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
   ) {
       // شريط كتابة الرسالة (TextField + زر الإرسال)
   }
   ```
3. **تجنب إعادة بناء الشاشة (Flicker Prevention):**
   تأكد من تذكر تدفق البيانات (Flows) باستخدام `remember(isPublicChat, targetUserId, currentUser.id)` لمنع إعادة إنشاء الـ StateFlows ومراقبين البيانات عند كل تحديث للواجهة.

---

## 5. قواعد نظام النقاط والصورة الشخصية

1. **قواعد النقاط (Points Rules):**
   - إرسال رسالة = **+1 نقطة** فقط.
   - متابعة مستخدم = **+5 نقاط** فقط.
   - لا تمنح نقاطًا مجانية عند إنشاء الحساب أو تسجيل الدخول اليومي أو تغيير الصورة لضمان قيمة التنافس.
2. **الصورة الشخصية (User Avatars):**
   - تم إلغاء جميع الصور الرمزية الافتراضية المجهزة مسبقًا.
   - عند التسجيل أو تعديل الحساب، يختار المستخدم صورة من جهازه باستخدام `ActivityResultContracts.GetContent()`.
   - في حال عدم اختيار صورة، تظهر أيقونة مستخدم بسيطة vector icon (`Icons.Default.Person`) خلف خلفية ذهبية فاخرة.

---

## 6. سير العمل في GitHub Actions ونظام Gradle

### آليات صيانة الـ CI/CD Pipeline:
1. **إعادة بناء مفاتيح التوقيع (`Restore or Generate Keystores`):**
   يقرأ الـ Workflow ملف `debug.keystore.base64` ويسترجعه تلقائيًا لمنع خطأ `Keystore file not found for signing config 'debugConfig'`.
2. **إذونات وإصلاح `gradlew`:**
   تضمين خطوة `chmod +x gradlew` وإعادة بناء الـ wrapper عبر `gradle wrapper` عند الحاجة، مع التأكد من عدم حذف `gradle-wrapper.jar`.
3. **التحقق المستمر قبل الـ Commit:**
   يجب أن تنجح الأوامر التالية محليًا وفي الخادم:
   - `./gradlew testDebugUnitTest --no-daemon`
   - `compile_applet` (لتجميع التطبيق والتأكد من خلوه من الأخطاء)

---

## 7. نموذج تقرير التعديلات الملتزم (CLAUDE_REPORT.md)

عند إجراء أي مجموعة تعديلات على المشروع، يجب إنشاء أو تحديث الملف `CLAUDE_REPORT.md` بالنموذج التالي:

```markdown
# تقرير التعديلات المنجزة (CLAUDE_REPORT.md)

## 1. ملخص التعديلات
- [وصف مختصر للتعديل]

## 2. قائمة الملفات المعدلة
- `مسار/الملف/المعدل.kt`: [سبب التعديل والتغييرات المنجزة]

## 3. الملفات التي لم يتم لمسها (للحفاظ على الاستقرار)
- `build.gradle.kts`
- `.github/workflows/android-build.yml`
- `debug.keystore.base64`

## 4. فحوصات الاستقرار والجودة
- [x] تغييرات قاعدة البيانات (Supabase & Room) متوافقة.
- [x] تم اختبار لوحة المفاتيح والـ IME في شاشة الدردشة.
- [x] نجح التجميع والتأكد عبر `compile_applet`.
- [x] نجحت الاختبارات المحلية عبر `./gradlew testDebugUnitTest`.
```

---

## 8. ملاحظات وإرشادات حماية استقرار المشروع (Safety Checklist)

- **عدم إضافة مكتبات غير مستقرة:** استخدم المكتبات المحددة في `gradle/libs.versions.toml` فقط.
- **الحفاظ على الأسماء والتسميات:** لا تقم بتغيير `namespace` أو `applicationId` المسجلة في المشروع.
- **الحفاظ على توافق Android OS:** تجنب استدعاء APIs غير مدعومة في Android SDK MinVersion الحالي (Min SDK 24).
