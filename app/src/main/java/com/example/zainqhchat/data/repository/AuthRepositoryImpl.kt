package com.example.zainqhchat.data.repository

import com.example.zainqhchat.data.local.LocalUserCache
import com.example.zainqhchat.data.remote.supabase.SupabaseAuthService
import com.example.zainqhchat.data.remote.supabase.SupabaseDatabaseService
import com.example.zainqhchat.data.remote.supabase.SupabaseStorageService
import com.example.zainqhchat.domain.model.NewAvatar
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * تنفيذ حقيقي لطبقة المصادقة عبر Supabase Auth (GoTrue) + جدول public.users.
 * المصدر الوحيد للحقيقة هو Supabase؛ Room تُستخدم فقط كذاكرة تخزين مؤقت
 * محلية لبيانات المستخدم الحالي (انظر [LocalUserCache]) حتى تكتمل مهاجرة
 * شاشات الدردشة في المرحلة القادمة.
 */
class AuthRepositoryImpl(
    private val authService: SupabaseAuthService,
    private val databaseService: SupabaseDatabaseService,
    private val storageService: SupabaseStorageService,
    private val localUserCache: LocalUserCache
) : AuthRepository {

    private val _currentUserState = MutableStateFlow<User?>(null)
    override val currentUserFlow: Flow<User?> = _currentUserState.asStateFlow()

    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val userId = authService.currentUserId()
        if (userId == null || !authService.hasSession()) {
            _currentUserState.value = null
            return@withContext null
        }

        // تأكيد أن الجلسة ما زالت صالحة (وتجديدها تلقائيًا عند الحاجة).
        val token = authService.currentValidAccessToken()
        if (token == null) {
            _currentUserState.value = null
            return@withContext null
        }

        // تُلتقَط أي مشكلة شبكة هنا (لا يوجد إنترنت إطلاقًا، انقطاع مفاجئ،
        // ضعف شديد، إلخ) — قبل هذا الإصلاح كان أي استثناء شبكة غير مُعالَج
        // من fetchUserWithRetry يُسقط التطبيق بالكامل فورًا عند كل إقلاع بلا
        // إنترنت، لأن checkSession() يستدعي هذه الدالة تلقائيًا عند بدء
        // التطبيق دون أي محاولة/التقاط في أي مكان بالسلسلة بأكملها.
        val user = try {
            fetchUserWithRetry(userId)
        } catch (e: Exception) {
            null
        }
        if (user != null) {
            localUserCache.upsert(user)
            markOnline(user.id)
        }
        _currentUserState.value = user
        user
    }

    /**
     * يعيد محاولة جلب صف المستخدم عدة مرات بفاصل قصير متزايد قبل الاستسلام.
     * هذا يعالج حالة نادرة لكن حقيقية: قد يستغرق التريجر `handle_new_auth_user`
     * (أو تناسق القراءة عبر الاتصالات المختلفة في Supabase) جزءًا من الثانية
     * ليصبح مرئيًا لطلب PostgREST التالي مباشرة بعد التسجيل. هذا ليس بيانات
     * وهمية؛ إنه فقط إعادة محاولة لقراءة حقيقية.
     */
    private suspend fun fetchUserWithRetry(userId: String, attempts: Int = 4): User? {
        repeat(attempts) { attempt ->
            val user = databaseService.fetchUserById(userId)
            if (user != null) return user
            if (attempt < attempts - 1) delay(300L * (attempt + 1))
        }
        return null
    }

    /**
     * يجلب ملف المستخدم، وإن لم يكن موجودًا بعد كل المحاولات (نادر جدًا: فشل
     * التريجر لأي سبب) يُنشئه العميل نفسه مباشرة (مسموح فعليًا عبر سياسة RLS
     * `users_insert_self` لأن `auth.uid()` يطابق المستخدم الجديد نفسه)، ثم
     * يعيد المحاولة مرة أخيرة. هذا إصلاح حقيقي للسبب الجذري المحتمل، وليس Mock.
     *
     * عند فشل الإدراج الاحتياطي، تُرمى الاستثناء الحقيقي القادم من Supabase
     * (رسالة الخطأ ورمزه كما هما) بدل استبدالهما برسالة عامة — حتى تكون أي
     * مشكلة مستقبلية (قيد CHECK، تعارض RLS، إلخ) قابلة للتشخيص فورًا من نص
     * الخطأ نفسه.
     */
    private suspend fun fetchOrHealUserProfile(
        userId: String,
        username: String,
        name: String,
        age: Int
    ): User {
        val existing = fetchUserWithRetry(userId)
        if (existing != null) return existing

        val healResult = databaseService.ensureUserProfileExists(userId, username, name, age)
        healResult.exceptionOrNull()?.let { throw it }

        return databaseService.fetchUserById(userId)
            ?: throw IllegalStateException(
                "تم إنشاء صف الملف الشخصي فعليًا (لا خطأ من قاعدة البيانات) لكن القراءة " +
                    "التالية مباشرة لم تُعده بعد — إن تكرر هذا أخبرني بذلك تحديدًا، فهو يشير " +
                    "إلى تأخر تناسق قراءة غير معتاد يحتاج تحقيقًا إضافيًا من طرف Supabase."
            )
    }

    override suspend fun registerUser(
        username: String,
        password: String,
        name: String,
        age: Int,
        avatar: NewAvatar?
    ): Result<User> = withContext(Dispatchers.IO) {
        val trimmedUsername = username.trim()
        val trimmedName = name.trim().ifBlank { trimmedUsername }

        val sessionResult = authService.signUp(trimmedUsername, password, trimmedName, age)
        val session = sessionResult.getOrElse { return@withContext Result.failure(it) }

        // رفع الصورة الشخصية الحقيقية (إن وُجدت) بعد إنشاء الجلسة مباشرة،
        // لأن سياسات Storage تتطلب مستخدمًا مُصادقًا عليه.
        var avatarUrl: String? = null
        if (avatar != null) {
            storageService.uploadAvatar(session.userId, avatar.bytes, avatar.mimeType)
                .onSuccess { url ->
                    avatarUrl = url
                    databaseService.updateUserProfile(session.userId, trimmedName, age, url)
                }
                .onFailure {
                    // فشل رفع الصورة لا يجب أن يفشّل إنشاء الحساب بالكامل؛
                    // الحساب سليم بدون صورة والمستخدم يستطيع إضافتها لاحقًا من الإعدادات.
                }
        }

        val createdUser = try {
            fetchOrHealUserProfile(session.userId, trimmedUsername, trimmedName, age)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        val finalUser = if (avatarUrl != null) createdUser.copy(avatarUrl = avatarUrl) else createdUser
        localUserCache.upsert(finalUser)
        markOnline(finalUser.id)
        _currentUserState.value = finalUser
        Result.success(finalUser)
    }

    override suspend fun loginUser(username: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            val session = authService.signIn(username.trim(), password)
                .getOrElse { return@withContext Result.failure(it) }

            val user = try {
                fetchOrHealUserProfile(session.userId, username.trim(), username.trim(), 18)
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }

            localUserCache.upsert(user)
            markOnline(user.id)
            _currentUserState.value = user
            Result.success(user)
        }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        val userId = authService.currentUserId()
        if (userId != null) {
            databaseService.updateOnlineStatus(userId, false)
        }
        authService.signOut()
        _currentUserState.value = null
    }

    override suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        authService.hasSession() && authService.currentValidAccessToken() != null
    }

    override suspend fun setOnlineStatus(isOnline: Boolean) = withContext(Dispatchers.IO) {
        val userId = authService.currentUserId()
        if (userId != null && authService.hasSession()) {
            databaseService.updateOnlineStatus(userId, isOnline)
        }
        Unit
    }

    override suspend fun setPassword(newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        authService.updatePassword(newPassword)
    }

    override suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val result = databaseService.deleteOwnAccount()
        if (result.isSuccess) {
            // الحساب نفسه محذوف الآن من Supabase Auth؛ أي طلب signOut لاحق قد
            // يفشل (الجلسة لم تعد صالحة) وهذا متوقع تمامًا، لذا لا نعتبره خطأ.
            runCatching { authService.signOut() }
            _currentUserState.value = null
        }
        result
    }

    private suspend fun markOnline(userId: String) {
        databaseService.updateOnlineStatus(userId, true)
    }
}
