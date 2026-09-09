package com.example.zainqhchat.ui.screens.currency

import com.example.R

/**
 * نسخة محلية خفيفة من كتالوج السيارات الثلاث (id/name/صورة حقيقية) — تُستخدم
 * لعرض شارة "السيارة المختارة" في الملف الشخصي والمتصدرين بدون حاجة لطلب
 * شبكة إضافي، لأن الكتالوج ثابت ونادرًا ما يتغير. **يجب أن تبقى مطابقة
 * تمامًا** لبيانات جدول shop_cars في supabase/migrations/002_currency_system.sql.
 */
object CurrencyStaticCatalog {
    data class CarInfo(val name: String, val icon: String, val imageRes: Int)

    private val cars = mapOf(
        "car_1" to CarInfo("سيارة المدينة الذكية", "directions_car", R.drawable.shop_car_1),
        "car_2" to CarInfo("السيارة الرياضية الماسية", "sports_score", R.drawable.shop_car_2),
        "car_3" to CarInfo("الوحش الأسطوري الأرجواني", "bolt", R.drawable.shop_car_3)
    )

    fun carInfo(carId: String?): CarInfo? = carId?.let { cars[it] }
}
