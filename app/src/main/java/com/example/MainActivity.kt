package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.zainqhchat.core.network.ConnectionState
import com.example.zainqhchat.core.network.NetworkMonitor
import com.example.zainqhchat.core.notification.LocalNotificationManager
import com.example.zainqhchat.data.local.AppDatabase
import com.example.zainqhchat.data.local.LocalUserCache
import com.example.zainqhchat.data.remote.supabase.SupabaseAuthServiceImpl
import com.example.zainqhchat.data.remote.supabase.SupabaseDatabaseServiceImpl
import com.example.zainqhchat.data.remote.supabase.SupabaseRealtimeServiceImpl
import com.example.zainqhchat.data.remote.supabase.SupabaseStorageServiceImpl
import com.example.zainqhchat.data.repository.AuthRepositoryImpl
import com.example.zainqhchat.data.repository.ChatRepositoryImpl
import com.example.zainqhchat.data.repository.NotificationRepositoryImpl
import com.example.zainqhchat.data.repository.UserRepositoryImpl
import com.example.zainqhchat.domain.model.User
import com.example.zainqhchat.ui.screens.AuthScreen
import com.example.zainqhchat.ui.screens.ChatDetailScreen
import com.example.zainqhchat.ui.screens.ChatListScreen
import com.example.zainqhchat.ui.screens.DashboardScreen
import com.example.zainqhchat.ui.screens.OnlineUsersScreen
import com.example.zainqhchat.ui.screens.ProfileScreen
import com.example.zainqhchat.ui.screens.ConnectionStatusScreen
import com.example.zainqhchat.ui.screens.SettingsScreen
import com.example.zainqhchat.ui.screens.SplashScreen
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.ZainQHChatTheme
import com.example.zainqhchat.ui.viewmodels.AuthViewModel
import com.example.zainqhchat.ui.viewmodels.ChatViewModel
import com.example.zainqhchat.ui.viewmodels.SettingsViewModel
import com.example.zainqhchat.ui.viewmodels.UserViewModel

class MainActivity : ComponentActivity() {

    // مرجع على مستوى الكلاس (وليس محليًا داخل onCreate) لأن onStart/onStop
    // بحاجة لاستدعاء setOnlineStatus منه — راجع تعليق ONLINE_STALE_THRESHOLD_MS
    // في PresenceUtils.kt لسبب الحاجة لهذه الآلية أصلًا.
    private lateinit var authRepository: com.example.zainqhchat.domain.repository.AuthRepository
    private var presenceHeartbeatJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // تهيئة Google Mobile Ads SDK مرة واحدة عند بدء التطبيق (معرّفات
        // اختبار حاليًا — راجع AdConfig.kt للتبديل لمعرّفات حقيقية لاحقًا).
        com.google.android.gms.ads.MobileAds.initialize(this) {}

        // تهيئة قاعدة بيانات Room — أصبحت الآن مستخدَمة فقط كذاكرة تخزين مؤقت
        // محلي لبيانات المستخدم الحالي (LocalUserCache)، وليست مصدر الحقيقة
        // لأي شيء؛ الدردشة (ChatRepositoryImpl) لم تعد تستخدم Room إطلاقًا.
        val database = AppDatabase.getInstance(this)
        val userDao = database.userDao()
        val localUserCache = LocalUserCache(userDao)

        // خدمات Supabase الحقيقية (Auth / PostgREST / Storage / Realtime) — بدون أي Mock.
        val supabaseAuthService = SupabaseAuthServiceImpl(this)
        val supabaseDatabaseService = SupabaseDatabaseServiceImpl(supabaseAuthService)
        val supabaseStorageService = SupabaseStorageServiceImpl(supabaseAuthService)
        val supabaseRealtimeService = SupabaseRealtimeServiceImpl(supabaseAuthService)

        authRepository = AuthRepositoryImpl(
            authService = supabaseAuthService,
            databaseService = supabaseDatabaseService,
            storageService = supabaseStorageService,
            localUserCache = localUserCache
        )
        val userRepository = UserRepositoryImpl(
            databaseService = supabaseDatabaseService,
            storageService = supabaseStorageService,
            realtimeService = supabaseRealtimeService,
            localUserCache = localUserCache
        )
        // الدردشة (عامة وخاصة) أصبحت مربوطة بالكامل بـ Supabase + Realtime.
        val chatRepository = ChatRepositoryImpl(supabaseDatabaseService, supabaseRealtimeService)
        val localNotificationManager = LocalNotificationManager(this)
        val notificationRepository = NotificationRepositoryImpl(
            supabaseDatabaseService,
            supabaseRealtimeService,
            localNotificationManager
        )
        val adminRepository = com.example.zainqhchat.data.repository.AdminRepositoryImpl(
            supabaseDatabaseService,
            supabaseStorageService,
            supabaseRealtimeService
        )

        // قسم العملات — راجع data/remote/supabase/SupabaseCurrencyService.kt
        val supabaseCurrencyService = com.example.zainqhchat.data.remote.supabase.SupabaseCurrencyServiceImpl(
            supabaseAuthService
        )
        val currencyRepository = com.example.zainqhchat.data.repository.CurrencyRepositoryImpl(
            supabaseCurrencyService
        )

        // مراقب حالة الشبكة — يُستخدم لعرض شاشة تفاعلية عند بدء التطبيق إذا
        // كان الاتصال ضعيفًا أو منقطعًا (راجع ConnectionStatusScreen).
        val networkMonitor = NetworkMonitor(this)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.settingsState.collectAsState()

            ZainQHChatTheme(darkTheme = settingsState.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LuxuryBlackBg
                ) {
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { /* لا حاجة لأي إجراء إضافي؛ عدم المنح يعني فقط عدم ظهور إشعارات النظام */ }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.Factory(authRepository)
                    )
                    val chatViewModel: ChatViewModel = viewModel(
                        factory = ChatViewModel.Factory(chatRepository, userRepository, supabaseStorageService)
                    )
                    val userViewModel: UserViewModel = viewModel(
                        factory = UserViewModel.Factory(userRepository)
                    )
                    val notificationViewModel: com.example.zainqhchat.ui.viewmodels.NotificationViewModel = viewModel(
                        factory = com.example.zainqhchat.ui.viewmodels.NotificationViewModel.Factory(
                            notificationRepository,
                            localNotificationManager
                        )
                    )
                    val adminViewModel: com.example.zainqhchat.ui.viewmodels.AdminViewModel = viewModel(
                        factory = com.example.zainqhchat.ui.viewmodels.AdminViewModel.Factory(adminRepository)
                    )
                    val currencyViewModel: com.example.zainqhchat.ui.viewmodels.CurrencyViewModel = viewModel(
                        factory = com.example.zainqhchat.ui.viewmodels.CurrencyViewModel.Factory(
                            currencyRepository,
                            userRepository
                        )
                    )

                    ZainQHChatNavHost(
                        authViewModel = authViewModel,
                        chatViewModel = chatViewModel,
                        userViewModel = userViewModel,
                        settingsViewModel = settingsViewModel,
                        notificationViewModel = notificationViewModel,
                        adminViewModel = adminViewModel,
                        currencyViewModel = currencyViewModel,
                        networkMonitor = networkMonitor
                    )
                }
            }
        }
    }

    /**
     * يُستدعى في كل مرة يظهر فيها التطبيق (حتى لو كانت الشاشة الرئيسية
     * وحيدة، هذا يعادل عمليًا "التطبيق في المقدمة" في تطبيق أحادي الشاشة
     * كهذا). يُعلِّم المستخدم "متصل الآن" فورًا، ثم يُبقي last_active_timestamp
     * حديثًا كل 60 ثانية طالما التطبيق أمام المستخدم — بدونها يظل "متصل
     * الآن" و"آخر ظهور" عالقين على آخر قيمة من وقت تسجيل الدخول فقط.
     */
    override fun onStart() {
        super.onStart()
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = lifecycleScope.launch {
            while (true) {
                runCatching { authRepository.setOnlineStatus(true) }
                kotlinx.coroutines.delay(60_000)
            }
        }
    }

    /** يُعلِّم المستخدم "غير متصل" فور مغادرة التطبيق أو تصغيره. */
    override fun onStop() {
        super.onStop()
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = null
        // lifecycleScope يبقى فعّالاً حتى onDestroy (وليس onStop)، فهذا
        // كافٍ لإتمام طلب شبكة قصير واحد لإعلام الخادم بالانقطاع.
        lifecycleScope.launch {
            runCatching { authRepository.setOnlineStatus(false) }
        }
    }
}

@Composable
fun ZainQHChatNavHost(
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    userViewModel: UserViewModel,
    settingsViewModel: SettingsViewModel,
    notificationViewModel: com.example.zainqhchat.ui.viewmodels.NotificationViewModel,
    adminViewModel: com.example.zainqhchat.ui.viewmodels.AdminViewModel,
    currencyViewModel: com.example.zainqhchat.ui.viewmodels.CurrencyViewModel,
    networkMonitor: NetworkMonitor
) {
    val navController = rememberNavController()
    val currentUserState by authViewModel.currentUser.collectAsState(initial = null)
    val connectionState by networkMonitor.observe()
        .collectAsState(initial = ConnectionState.CONNECTED)

    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = {
            slideInHorizontally(animationSpec = tween(320)) { it / 4 } + fadeIn(tween(320))
        },
        exitTransition = {
            fadeOut(tween(200))
        },
        popEnterTransition = {
            fadeIn(tween(250))
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(280)) { it / 4 } + fadeOut(tween(280))
        }
    ) {
        // 1. شاشة البداية (Splash Screen) — تُسبَق بشاشة اتصال تفاعلية إن
        // كان الإنترنت ضعيفًا أو منقطعًا عند إقلاع التطبيق.
        composable("splash") {
            if (connectionState != ConnectionState.CONNECTED) {
                ConnectionStatusScreen(
                    state = connectionState,
                    onRetryNow = { /* NetworkMonitor يُحدَّث تلقائيًا عبر الـ callback */ },
                    onConnected = { /* الانتقال يحدث تلقائيًا عبر إعادة رسم الشرط أعلاه */ }
                )
            } else {
                SplashScreen(
                    isLoggedIn = currentUserState != null,
                    onSplashFinished = { isLoggedIn ->
                        if (isLoggedIn) {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else {
                            navController.navigate("auth") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        // 2. شاشة التسجيل وتدقيق الحساب (Auth Screen)
        composable("auth") {
            AuthScreen(
                authViewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        // 3. الصفحة الرئيسية الجديدة (Dashboard — بدون قائمة سفلية)
        composable("home") {
            val currentUser = currentUserState
            if (currentUser != null) {
                LaunchedEffect(currentUser.id) {
                    notificationViewModel.setCurrentUserId(currentUser.id)
                }
                DashboardScreen(
                    currentUser = currentUser,
                    chatViewModel = chatViewModel,
                    userViewModel = userViewModel,
                    notificationViewModel = notificationViewModel,
                    onOpenChats = { navController.navigate("chat_list") },
                    onOpenOnlineUsers = { navController.navigate("online_users") },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenProfile = { userId -> navController.navigate("profile/$userId") },
                    onOpenNotifications = { navController.navigate("notifications") },
                    onOpenCurrency = { navController.navigate("currency_home") },
                    onContactAdmin = {
                        userViewModel.contactAdmin { admin ->
                            if (admin != null) {
                                navController.navigate("chat/${admin.id}/${admin.name}")
                            }
                        }
                    }
                )
            } else {
                authViewModel.checkSession()
            }
        }

        // 3أ. شاشة الدردشات الكاملة (تفتح من بطاقة "الدردشات")
        composable("chat_list") {
            val chatPreviews by chatViewModel.chatPreviews.collectAsState()
            ChatListScreen(
                previews = chatPreviews,
                onOpenPublicChat = { navController.navigate("public_chat") },
                onOpenPrivateChat = { targetUserId, targetUserName ->
                    navController.navigate("chat/$targetUserId/$targetUserName")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 3ب. شاشة المتصلون الآن الكاملة (تفتح من بطاقة "المتصلون الآن")
        composable("online_users") {
            val usersGrouped by userViewModel.usersGroupedByOnlineStatus.collectAsState()
            OnlineUsersScreen(
                usersGrouped = usersGrouped,
                onOpenProfile = { userId -> navController.navigate("profile/$userId") },
                onStartPrivateChat = { targetUserId, targetUserName ->
                    navController.navigate("chat/$targetUserId/$targetUserName")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 3ج. شاشة الإعدادات الكاملة (تفتح من بطاقة "الإعدادات")
        composable("settings") {
            SettingsScreen(
                currentUser = currentUserState,
                settingsViewModel = settingsViewModel,
                userViewModel = userViewModel,
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("auth") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onDeleteAccount = { onResult ->
                    authViewModel.deleteAccount { success ->
                        if (success) {
                            navController.navigate("auth") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                        onResult(success)
                    }
                },
                onOpenAdmin = { navController.navigate("admin") },
                onContactAdmin = {
                    userViewModel.contactAdmin { admin ->
                        if (admin != null) {
                            navController.navigate("chat/${admin.id}/${admin.name}")
                        }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 3د. لوحة الإدارة (تظهر فقط إن كان currentUser.isAdmin؛ الحماية
        // الحقيقية تبقى من جهة Supabase RLS/RPC بغض النظر عن هذا الشرط).
        composable("admin") {
            if (currentUserState?.isAdmin == true) {
                com.example.zainqhchat.ui.screens.AdminDashboardScreen(
                    adminViewModel = adminViewModel,
                    onOpenProfile = { userId -> navController.navigate("profile/$userId") },
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }

        // 3ب. الإشعارات
        composable("notifications") {
            com.example.zainqhchat.ui.screens.NotificationsScreen(
                notificationViewModel = notificationViewModel,
                onBackClick = { navController.popBackStack() },
                onOpenProfileClick = { userId ->
                    navController.navigate("profile/$userId")
                }
            )
        }

        // 3ج. قسم العملات — راجع ui/screens/currency/*
        composable("currency_home") {
            val currentUser = currentUserState
            if (currentUser != null) {
                LaunchedEffect(currentUser.id) { currencyViewModel.setCurrentUserId(currentUser.id) }
                com.example.zainqhchat.ui.screens.currency.CurrencyHomeScreen(
                    currencyViewModel = currencyViewModel,
                    fallbackUser = currentUser,
                    onBackClick = { navController.popBackStack() },
                    onOpenNotifications = { navController.navigate("notifications") },
                    onOpenProfile = { navController.navigate("profile/${currentUser.id}") },
                    onOpenLeaderboard = { navController.navigate("currency_leaderboard") },
                    onOpenEarnCoins = { navController.navigate("currency_earn") },
                    onOpenShop = { navController.navigate("currency_shop") },
                    onOpenHistory = { navController.navigate("currency_history") }
                )
            }
        }

        composable("currency_leaderboard") {
            val currentUser = currentUserState
            if (currentUser != null) {
                com.example.zainqhchat.ui.screens.currency.CurrencyLeaderboardScreen(
                    currencyViewModel = currencyViewModel,
                    currentUser = currentUser,
                    onBackClick = { navController.popBackStack() },
                    onOpenNotifications = { navController.navigate("notifications") },
                    onOpenProfile = { navController.navigate("profile/${currentUser.id}") }
                )
            }
        }

        composable("currency_earn") {
            val currentUser = currentUserState
            if (currentUser != null) {
                com.example.zainqhchat.ui.screens.currency.CurrencyEarnScreen(
                    currencyViewModel = currencyViewModel,
                    currentUser = currentUser,
                    onBackClick = { navController.popBackStack() },
                    onOpenNotifications = { navController.navigate("notifications") },
                    onOpenProfile = { navController.navigate("profile/${currentUser.id}") }
                )
            }
        }

        composable("currency_shop") {
            val currentUser = currentUserState
            if (currentUser != null) {
                com.example.zainqhchat.ui.screens.currency.CurrencyShopScreen(
                    currencyViewModel = currencyViewModel,
                    currentUser = currentUser,
                    onBackClick = { navController.popBackStack() },
                    onOpenNotifications = { navController.navigate("notifications") },
                    onOpenProfile = { navController.navigate("profile/${currentUser.id}") }
                )
            }
        }

        composable("currency_history") {
            val currentUser = currentUserState
            if (currentUser != null) {
                com.example.zainqhchat.ui.screens.currency.CurrencyHistoryScreen(
                    currencyViewModel = currencyViewModel,
                    currentUser = currentUser,
                    onBackClick = { navController.popBackStack() },
                    onOpenNotifications = { navController.navigate("notifications") },
                    onOpenProfile = { navController.navigate("profile/${currentUser.id}") }
                )
            }
        }

        // 4. الدردشة العامة (Public Chat)
        composable("public_chat") {
            val currentUser = currentUserState
            if (currentUser != null) {
                ChatDetailScreen(
                    currentUser = currentUser,
                    targetUserId = null,
                    targetUserName = "الدردشة العامة",
                    chatViewModel = chatViewModel,
                    userViewModel = userViewModel,
                    onBackClick = { navController.popBackStack() },
                    onOpenProfileClick = { userId ->
                        navController.navigate("profile/$userId")
                    }
                )
            }
        }

        // 5. المحادثات الخاصة (Private Chat)
        composable(
            route = "chat/{targetUserId}/{targetUserName}",
            arguments = listOf(
                navArgument("targetUserId") { type = NavType.StringType },
                navArgument("targetUserName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("targetUserId") ?: ""
            val targetUserName = backStackEntry.arguments?.getString("targetUserName") ?: ""
            val currentUser = currentUserState

            if (currentUser != null) {
                ChatDetailScreen(
                    currentUser = currentUser,
                    targetUserId = targetUserId,
                    targetUserName = targetUserName,
                    chatViewModel = chatViewModel,
                    userViewModel = userViewModel,
                    onBackClick = { navController.popBackStack() },
                    onOpenProfileClick = { userId ->
                        navController.navigate("profile/$userId")
                    }
                )
            }
        }

        // 6. شاشة الملف الشخصي (Profile Screen)
        composable(
            route = "profile/{userId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val currentUser = currentUserState

            if (currentUser != null) {
                ProfileScreen(
                    targetUserId = userId,
                    currentUserId = currentUser.id,
                    userViewModel = userViewModel,
                    onBackClick = { navController.popBackStack() },
                    onStartPrivateChat = { targetUserId, targetUserName ->
                        navController.navigate("chat/$targetUserId/$targetUserName")
                    }
                )
            }
        }
    }
}
