# دليل البناء والرفع السحابي وتوليد APK (ZainQH Chat) 👑📱

يوضح هذا الدليل الشامل كيفية رفع مشروع **ZainQH Chat** إلى **GitHub** واستخدام **GitHub Actions** لتوليد وتنزيل ملف ה-**APK** تلقائيًا عند كل تحديث أو إصدار جديد بدون الحاجة لإعداد بيئة تطوير محليًا.

---

## 🛠️ 1. كيفية رفع المشروع إلى GitHub (Pushing Code to GitHub)

### الخطوة الأولى: إنشاء المستودع (Repository) على GitHub
1. سجّل الدخول إلى حسابك في [GitHub](https://github.com).
2. انقر على زر **New** لإنشاء مستودع جديد.
3. سمّ المستودع باسم: `zainqh-chat`
4. اترك الخيار على **Public** أو **Private** حسب رغبتك.
5. **مهم**: لا تقم بإضافة ملفات `README` أو `.gitignore` من صفحة الإنشاء لأنها موجودة مسبقًا ومجهزة داخل المشروع.
6. انقر على **Create repository**.

### الخطوة الثانية: ربط الكود ورفعه من السطر الأوامري (Terminal)
من المجلد الرئيسي للمشروع، قم بتنفيذ الأوامر التالية:

```bash
# تهيئة المستودع المحلي
git init

# إضافة جميع ملفات المشروع
git add .

# إنشاء أول Commit للمشروع
git commit -m "إطلاق النسخة الأولى من تطبيق ZainQH Chat الملكي 👑"

# إعادة تسمية الفرع الرئيسي إلى main
git branch -M main

# ربط المستودع بمستودعك على GitHub (استبدل YOUR_USERNAME باسم حسابك)
git remote add origin https://github.com/YOUR_USERNAME/zainqh-chat.git

# رفع الكود إلى GitHub
git push -u origin main
```

---

## ⚡ 2. كيفية عمل GitHub Actions وتوليد APK تلقائيًا

بمجرد تنفيذ أمر `git push` لرفع الكود إلى الفرع `main`، سيعمل سير العمل التلقائي (Workflow) المكتوب في المسار:
`.github/workflows/android-build.yml`

### مراحل العمل التلقائية داخل GitHub Actions:
1. **Checkout Code**: جلب أحدث نسخة من الكود.
2. **Setup JDK 17**: إعداد بيئة Java 17 وتفعيل التخزين المؤقت لـ Gradle لإسراع البناء.
3. **Run Unit Tests**: تشغيل جميع فحوصات واختبارات الوحدة لضمان خلو الكود من الأخطاء.
4. **Build Debug & Release APK**: تجميع وبناء ملفات ה-APK النهائية.
5. **Upload Artifacts**: رفع ملف الـ APK الجاهز لحسابك مباشرة على GitHub.

---

## 📥 3. كيفية تنزيل ملف APK من تبويب Actions

1. افتح صفحة المستودع الخاص بك على **GitHub**.
2. انقر على تبويب **Actions** من الشريط العلوي.
3. ستشاهد سير العمل الجاري باسم: **Build Android APK (ZainQH Chat)**.
4. انقر على أحدث تشغيل (Workflow Run) مكتمل بنجاح (يحمل علامة صح خضراء `✔`).
5. انزل إلى أسفل الصفحة حتى تصل لفقرة **Artifacts**.
6. ستجد ملف APK باسم:
   - `ZainQH-Chat-Debug-APK` (جاهز للتثبيت والاستخدام المباشر على الهاتف).
7. اضغط عليه لتنزيل ملف الـ ZIP واستخراج ملف ה-`app-debug.apk` المباشر منه وتثبيته على هاتفك!

---

## 🚀 4. كيفية إنشاء إصدار جديد (GitHub Release)

عند تشغيل التطبيق وإضافة ميزات جديدة، يمكنك إنشاء Release جديد لتنزيل APK مخصص لهذا الإصدار:

1. في صفحة المستودع على GitHub، انقر على **Releases** من القائمة الجانبية المكونة على اليمين.
2. اضغط على زر **Draft a new release**.
3. اختر اسم التاج (Tag) مثل `v1.0.0` واضغط على **Create new tag**.
4. أضف عنوانًا للإصدار مثل: `ZainQH Chat v1.0.0 - Release` مع كتابة أبرز الميزات الجديدة.
5. اضغط على **Publish release**.
6. ستقوم GitHub Actions تلقائيًا ببناء الـ APK وإرفاقه ضمن أصول (Assets) هذا الإصدار!

---

## 🔧 5. حل أشهر مشاكل البناء (Troubleshooting Common Issues)

| المشكلة | السبب المحتمل | الحل |
| :--- | :--- | :--- |
| **`Permission denied: ./gradlew`** | عدم امتلاك ملف gradlew أذونات التنفيذ. | تم معالجة هذا تلقائيًا داخل Workflow بـ `chmod +x gradlew` |
| **`Invalid or corrupt jarfile gradle-wrapper.jar`** | تلف ملف الـ JAR الثنائي أثناء الرفع لـ Git بسبب تحويل السطور. | تم إضافة اختبار تلقائي وحل مجرب في GitHub Actions لإعادة بناء Wrapper تلقائيًا عبر `gradle wrapper` وإنشاء ملف `.gitattributes`. |
| **`Keystore file not found for signing config 'debugConfig'`** | عدم تتبع `debug.keystore` بواسطة Git بسبب وجوده في `.gitignore`. | تم إضافة خطوة تلقائية `Restore or Generate Keystores` لاستعادة `debug.keystore` من `debug.keystore.base64` المرفوع ضمن المستودع. |
| **`Compilation Error`** | وجود خطأ صياغة أو حزم غير متوافقة. | قم بتشغيل `./gradlew assembleDebug` محليًا وتفقد سجل الأخطاء. |
| **`Out of Memory (OOM)`** | استهلاك Gradle للذاكرة العشوائية أثناء التجميع. | تم إضافة `--no-daemon` في Workflow للحفاظ على أداء الخادم السحابي. |

---

## 👑 ختامًا
تطبيقك الآن مجهز بالكامل بأعلى معايير **CI/CD** والرفع التلقائي المستمر! لأي استفسار حول ضبط إعدادات Supabase، يرجى مراجعة ملف [`CLAUDE_SETUP.md`](./CLAUDE_SETUP.md).
