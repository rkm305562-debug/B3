package com.example.zainqhchat.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * حالة الاتصال بالإنترنت التي تهمّ واجهة المستخدم.
 *
 * - [CONNECTED]: هناك شبكة (واي فاي أو بيانات) ومُتحقَّق أنها تصل فعليًا
 *   للإنترنت (NET_CAPABILITY_VALIDATED)، وسرعتها ليست ضعيفة جدًا.
 * - [WEAK]: هناك اتصال، لكنه إمّا لم يجتز التحقق من الوصول للإنترنت بعد
 *   (شائع جدًا مع شبكات الجوال الضعيفة) أو أن النظام يُصنّفه كاتصال ذي
 *   نطاق ترددي منخفض (NET_CAPABILITY_NOT_METERED غير متوفر + إشارات ضعف).
 * - [DISCONNECTED]: لا توجد أي شبكة نشطة إطلاقًا.
 */
enum class ConnectionState {
    CONNECTED,
    WEAK,
    DISCONNECTED
}

/**
 * يراقب حالة الاتصال بالشبكة عبر [ConnectivityManager] ويبثّها كـ [Flow]
 * قابل للجمع من داخل أي Composable عبر collectAsState().
 *
 * لا يعتمد على أي مكتبة خارجية — فقط Android SDK القياسي.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /** فحص مُتزامن فوري لحالة الاتصال الحالية — يُستخدم لزر "إعادة المحاولة". */
    fun check(): ConnectionState {
        val network = connectivityManager.activeNetwork ?: return ConnectionState.DISCONNECTED
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return ConnectionState.DISCONNECTED

        val hasTransport = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasTransport) return ConnectionState.DISCONNECTED

        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isSuspended = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
        val hasLowBandwidth = capabilities.linkDownstreamBandwidthKbps in 1..99

        return when {
            isSuspended -> ConnectionState.WEAK
            !isValidated -> ConnectionState.WEAK
            hasLowBandwidth -> ConnectionState.WEAK
            else -> ConnectionState.CONNECTED
        }
    }

    fun observe(): Flow<ConnectionState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(check())
            }

            override fun onLost(network: Network) {
                trySend(ConnectionState.DISCONNECTED)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(check())
            }

            override fun onUnavailable() {
                trySend(ConnectionState.DISCONNECTED)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        // إرسال الحالة الحالية فورًا حتى لا تنتظر الواجهة أول تغيير.
        trySend(check())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
