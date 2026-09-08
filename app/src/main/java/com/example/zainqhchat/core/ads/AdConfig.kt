package com.example.zainqhchat.core.ads

/**
 * معرّفات إعلانات AdMob — مركزية في مكان واحد لسهولة الاستبدال لاحقًا.
 *
 * إعلانات Banner وInterstitial حُذفت بالكامل من التطبيق بناءً على طلب
 * صريح — الإعلان الوحيد المتبقي هو إعلان المكافأة (Rewarded)، ولا يعمل
 * إلا بضغط المستخدم عليه صراحةً في "زيادة العملات"، بحد أقصى 3 مرات في
 * اليوم (يُطبَّق هذا الحد على الخادم في دالة claim_ad_reward، راجع
 * supabase/migrations/006_daily_ad_reward_limit.sql — وليس فقط في الواجهة،
 * كي لا يمكن الالتفاف عليه).
 *
 * القيمة الحالية أدناه هي معرّف الاختبار **الرسمي** من Google
 * (https://developers.google.com/admob/android/test-ads) — آمنة للاستخدام
 * أثناء التطوير، لا تُنتج أي إيرادات حقيقية، ولا تخالف سياسات Google Play
 * (استخدام معرّف إنتاج حقيقي أثناء التطوير/الاختبار يخالف سياسة AdMob
 * ويعرّض الحساب الإعلاني للحظر).
 *
 * **للانتقال إلى الإنتاج:** أنشئ تطبيقًا ووحدة إعلانية Rewarded حقيقية من
 * https://apps.admob.com ثم استبدل القيمتين أدناه فقط (ولا تنس أيضًا تحديث
 * `com.google.android.gms.ads.APPLICATION_ID` في AndroidManifest.xml
 * بمعرّف التطبيق الحقيقي المطابق).
 */
object AdConfig {
    /** معرّف تطبيق AdMob — مطابق للقيمة الموجودة في AndroidManifest.xml */
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"

    /** إعلان مكافأة (Rewarded) — الإعلان الوحيد المتبقي في التطبيق. */
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
}
