package com.example.zainqhchat.data.remote.supabase

import com.example.BuildConfig

/**
 * إعدادات وتكوين عميل Supabase الخاص بتطبيق ZainQH Chat.
 * يتيح الربط المباشر مع Supabase Auth و Database (PostgreSQL) و Storage و Realtime.
 *
 * القيم تُقرأ من BuildConfig، والذي بدوره يُغذّى من ملف `.env` (أو `.env.example`
 * كقيمة احتياطية) عبر `app/build.gradle.kts`. لا تضع أي مفاتيح مباشرة هنا.
 */
object SupabaseConfig {

    fun getSupabaseUrl(): String = BuildConfig.SUPABASE_URL

    fun getSupabaseAnonKey(): String = BuildConfig.SUPABASE_ANON_KEY

    object Tables {
        const val USERS = "users"
        const val USER_SETTINGS = "user_settings"
        const val CHAT_MESSAGES = "chat_messages"
        const val FOLLOWS = "follows"
        const val BLOCKS = "blocks"
        const val REPORTS = "reports"
        const val POINTS_TRANSACTIONS = "points_transactions"
        const val NOTIFICATIONS = "notifications"
        // قسم العملات — راجع supabase/migrations/002_currency_system.sql
        const val SHOP_CARS = "shop_cars"
        const val USER_CARS = "user_cars"
        const val SHOP_GIFTS = "shop_gifts"
        const val USER_GIFTS_RECEIVED = "user_gifts_received"
        const val CURRENCY_TRANSACTIONS = "currency_transactions"
    }

    object Views {
        const val CHAT_PREVIEWS = "chat_previews"
        const val LEADERBOARD = "leaderboard"
        const val CURRENCY_LEADERBOARD = "currency_leaderboard"
    }

    object Buckets {
        const val AVATARS = "avatars"
        const val CHAT_IMAGES = "chat-images"
    }
}
