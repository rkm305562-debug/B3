# دليل التطوير والإعداد الشامل لـ ZainQH Chat 👑
### (CLAUDE_SETUP.md)

مرحباً بك في دليل التطوير الموحد لمشروع **ZainQH Chat**. تم إعداد هذا الملف بعناية فائقة لتنسيق العمل بين جميع المطورين والنماذج الذكية (مثل Claude و Google AI Studio)، ولتوفير دليل شامل لربط المشروع بخدمات **Supabase** وبناء التطبيق لنظام Android بدون أي تعارضات.

---

## 📐 1. هيكلية المشروع بالتفصيل (Project Architecture)

المشروع يتبع نمط **Clean Architecture** الموصى به رسمياً من Google، مقسماً إلى ثلاث طبقات رئيسية (`Domain`, `Data`, `UI`):

```
app/src/main/java/com/example/zainqhchat/
├── core/
│   └── notification/
│       └── FCMNotificationManager.kt   # إدارة إشعارات FCM والتنبيهات المحلية
├── domain/                            # [طبقة الأعمال - يمنع تعديل الهيكلية الأساسية]
│   ├── model/
│   │   ├── User.kt                    # نموذج بيانات المستخدم والرتبة النقاط
│   │   ├── UserTier.kt                # رتب المستخدمين (برونزي، فضي، ذهبي، ماسي، ملكي)
│   │   ├── OnlineStatusCategory.kt    # حالات الاتصال (متصل، مشغول، بعيد، غير متصل)
│   │   ├── ChatMessage.kt             # نموذج الرسائل
│   │   ├── ChatPreview.kt             # معاينة المحادثات
│   │   └── UserSettings.kt            # إعدادات المستخدم والخصوصية
│   └── repository/                    # الواجهات (Interfaces) لطبقة البيانات
│       ├── AuthRepository.kt
│       ├── UserRepository.kt
│       └── ChatRepository.kt
├── data/                              # [طبقة البيانات والتكامل]
│   ├── local/
│   │   ├── entities/                  # جداول Room المحلية للتخزين والمزامنة بدون إنترنت
│   │   │   ├── UserEntity.kt
│   │   │   ├── ChatMessageEntity.kt
│   │   │   └── SocialEntities.kt
│   │   ├── dao/                       # استعلامات Room (DAOs)
│   │   │   ├── UserDao.kt
│   │   │   ├── ChatMessageDao.kt
│   │   │   └── SocialDao.kt
│   │   └── AppDatabase.kt             # قاعدة بيانات SQLite المحلية
│   ├── remote/
│   │   └── supabase/                  # [جديد - طبقة الربط مع Supabase]
│   │       ├── SupabaseConfig.kt       # روابط وإعدادات Supabase
│   │       ├── SupabaseAuthService.kt  # خدمة التوثيق والتسجيل
│   │       ├── SupabaseDatabaseService.kt # استعلامات جداول PostgreSQL
│   │       └── SupabaseStorageAndRealtimeService.kt # التخزين والبث المباشر
│   └── repository/                    # تنفيذ المستودعات (Repository Implementations)
│       ├── AuthRepositoryImpl.kt
│       ├── UserRepositoryImpl.kt
│       └── ChatRepositoryImpl.kt
└── ui/                                # [طبقة الواجهات - التعديل بحرية للميزات الجديدة]
    ├── theme/
    │   ├── Color.kt                   # الألوان الذهبية والملكية الفاخرة
    │   ├── Theme.kt                   # نسق المظهر الملكي
    │   └── Type.kt                    # الخطوط وتنسيقات النصوص
    ├── components/
    │   ├── GoldComponents.kt          # المكونات والبطاقات الفاخرة
    │   ├── TierBadge.kt               # شارات الرتب والتاج الملكي
    │   ├── UserAvatar.kt              # الصور الشخصية مع مؤشر الحالة
    │   └── ChatBubble.kt              # فقاعات الرسائل الذهبية والرمادية
    ├── viewmodels/
    │   ├── AuthViewModel.kt
    │   ├── ChatViewModel.kt
    │   ├── UserViewModel.kt
    │   └── SettingsViewModel.kt
    └── screens/
        ├── SplashScreen.kt            # شاشة البداية مع التدرج الذهبي
        ├── AuthScreen.kt              # تسجيل الدخول وإنشاء الحساب
        ├── HomeScreen.kt              # الشاشة الرئيسية والتبويبات
        ├── ChatListSection.kt         # قائمة المحادثات العامة والخاصة
        ├── OnlineSection.kt           # المتصلون الآن حسب الفئات
        ├── SettingsSection.kt         # الإعدادات وتخصيص التطبيق
        ├── ChatDetailScreen.kt        # شاشة المراسلة المباشرة
        └── ProfileScreen.kt           # الملف الشخصي وتفاصيل الرتبة
```

### 🚨 قواعد تعديل الملفات (Editing Boundaries)
- **ملفات يسمح بإنشائها وتعديلها بحرية**:
  - جميع المكونات في مجلد `ui/screens/` و `ui/components/` و `ui/viewmodels/`.
  - الخدمات البرمجية في `data/remote/supabase/`.
  - اختبارات الوحدة في `src/test/`.
- **ملفات يمنع تعديلها إلا للحاجة القصوى ومع التأكد من البناء**:
  - `app/build.gradle.kts` و `build.gradle.kts` (إضافة مكتبات فقط عند الضرورة بدون تغيير إصدارات Kotlin/AGP).
  - `AndroidManifest.xml` (إضافة أذونات جديدة فقط).
  - `domain/model/` (الحفاظ على توافق الحقول لعدم كسر واجهات المستخدم).

---

## ⚡ 2. دليل إعداد وربط Supabase خطوة بخطوة (Supabase Step-by-Step Guide)

### الخطوة 1: إنشاء مشروع Supabase
1. اذهب إلى [Supabase Dashboard](https://supabase.com) وأنشئ مشروعاً جديداً باسم **ZainQH-Chat**.
2. احفظ كلمة مرور قاعدة البيانات وخادم المنطقة.
3. انتقل إلى **Project Settings -> API** وانسخ:
   - `Project URL`
   - `anon public key`
4. ضع هذه القيم داخل ملف `.env` بناءً على `.env.example`:
   ```env
   SUPABASE_URL=https://your-project-id.supabase.co
   SUPABASE_ANON_KEY=your-anon-key-here
   ```

---

### الخطوة 2: إنشاء الجداول وسياسات الأمان (PostgreSQL SQL Script)
افتح **SQL Editor** في Supabase وقم بتشغيل الاستعلام الموحد التالي لتجهيز جميع الجداول المفهرسة وسياسات RLS:

```sql
-- 1. جدول المستخدمين (users)
CREATE TABLE IF NOT EXISTS public.users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    username TEXT UNIQUE NOT NULL,
    age INT NOT NULL DEFAULT 18,
    avatar_url TEXT,
    tier TEXT NOT NULL DEFAULT 'BRONZE',
    points INT NOT NULL DEFAULT 0,
    is_online BOOLEAN NOT NULL DEFAULT false,
    last_seen_timestamp BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. جدول رسائل الدردشة (chat_messages)
CREATE TABLE IF NOT EXISTS public.chat_messages (
    id TEXT PRIMARY KEY,
    sender_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    receiver_id TEXT, -- NULL تعني دردشة عامة
    is_public BOOLEAN NOT NULL DEFAULT false,
    content TEXT NOT NULL,
    image_url TEXT,
    audio_url TEXT,
    timestamp BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. جدول المتابعات (follows)
CREATE TABLE IF NOT EXISTS public.follows (
    follower_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    followed_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    PRIMARY KEY (follower_id, followed_id)
);

-- 4. جدول البلاغات (reports)
CREATE TABLE IF NOT EXISTS public.reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    reported_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 5. جدول الحظر (blocks)
CREATE TABLE IF NOT EXISTS public.blocks (
    blocker_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    blocked_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    PRIMARY KEY (blocker_id, blocked_id)
);

-- تفعيل Row Level Security (RLS)
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.blocks ENABLE ROW LEVEL SECURITY;

-- سياسات الوصول (Policies)
CREATE POLICY "Public Read Access Users" ON public.users FOR SELECT USING (true);
CREATE POLICY "User Update Self" ON public.users FOR UPDATE USING (true);
CREATE POLICY "Insert Users" ON public.users FOR INSERT WITH CHECK (true);

CREATE POLICY "Public Read Messages" ON public.chat_messages FOR SELECT USING (true);
CREATE POLICY "Insert Messages" ON public.chat_messages FOR INSERT WITH CHECK (true);

CREATE POLICY "Public Follows" ON public.follows FOR ALL USING (true);
CREATE POLICY "Public Reports" ON public.reports FOR ALL USING (true);
CREATE POLICY "Public Blocks" ON public.blocks FOR ALL USING (true);
```

---

### الخطوة 3: إنشاء حافلات التخزين (Storage Buckets)
1. انتقل إلى قسم **Storage** في Supabase.
2. أنشئ الحافلة الأولى باسم: `avatars`
   - حدد خيار **Public Bucket** (ليتمكن جميع المستخدمين من عرض الصور الشخصية).
3. أنشئ الحافلة الثانية باسم: `chat-images`
   - حدد خيار **Public Bucket** (لعرض صور المحادثات).
4. أضف سياسة السماح بالرفع والعرض لجميع المستخدمين المسجلين:
```sql
CREATE POLICY "Public Avatars Access" ON storage.objects FOR SELECT USING (bucket_id = 'avatars');
CREATE POLICY "Upload Avatars Access" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'avatars');

CREATE POLICY "Public Chat Images Access" ON storage.objects FOR SELECT USING (bucket_id = 'chat-images');
CREATE POLICY "Upload Chat Images Access" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'chat-images');
```

---

### الخطوة 4: تفعيل Supabase Realtime للدردشة المباشرة
1. انتقل إلى **Database -> Realtime** في لوحة التحكم.
2. قم بتمكين Realtime للجدولين:
   - `chat_messages` (لثبث الرسائل المباشرة لحظياً للجميع).
   - `users` (لتحديث حالة متصل الآن والرتبة فوراً).

---

### الخطوة 5: نظام الإشعارات المستقبلية (Push Notifications Strategy)
المشروع مجهز عبر `FCMNotificationManager.kt` لاستقبال الإشعارات المحلية وعبر التنبيهات. عند الربط مع Supabase Edge Functions أو FCM:
1. يتم حفظ رمز الجهاز (`fcm_token`) في جدول `users`.
2. يتم إنشاء **Supabase Database Webhook** يُستدعى فور إضافة سطر جديد في جدول `chat_messages` لإرسال إشعار للمستلم.

---

## 🛠️ 3. استراتيجية البناء والتوافق المستمر (APK Build & Consistency Strategy)

ضمان جودة الكود وعدم حدوث أي مشاكل أثناء البناء يعتمد على الخطوات التالية:

1. **دعم منطقة الأمان الشامل (Safe Area)**:
   جميع الشاشات تطبق المبدأ التالي:
   ```kotlin
   Modifier.statusBarsPadding()
   Modifier.navigationBarsPadding()
   Modifier.imePadding()
   ```
   وهذا يضمن عدم تداخل واجهة التطبيق مع أجزاء النظام المرتفعة مثل شريط الحالة، البطارية، الكاميرا الأمامية، أو شريط الإيماءات السفلي.

2. **فحص البناء التلقائي (Compilation Check)**:
   قبل إنهاء أي مرحلة تطويرية، يتم تشغيل الأمر:
   ```bash
   ./gradlew assembleDebug
   ```
   للتأكد من أن التطبيق يبنى بنجاح بدون خطأ صيغة أو مكتبة مفقودة.

3. **الحفاظ على العمل وعدم التعارض بين المطورين**:
   - عدم تعديل المكونات الثابتة في `ui/theme/`.
   - الاعتماد على `StateFlow` و `ViewModel` لمعالجة البيانات بشكل نقي ومستقل عن الواجهات.
   - الفصل التام بين قاعدة بيانات Room المحلية وتطبيقات Supabase السحابية لتوفير تجربة عمل متكاملة متصلة وغير متصلة بالإنترنت (Offline First Strategy).

---
**تم إعداد هذا الملف ليكون المرجع الأساسي لتطوير ZainQH Chat بنجاح جاهز للنشر على متجر Google Play! 👑📱**
