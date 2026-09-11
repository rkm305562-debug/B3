package com.example.zainqhchat.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.zainqhchat.ui.components.GoldButton
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxurySurfaceDark
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted

/**
 * يعرض رابطًا خارجيًا (مثل غرفة الدردشة العامة الخارجية) داخل التطبيق نفسه
 * عبر WebView، بدل فتح متصفح خارجي منفصل — مع شريط علوي بسيط وزر رجوع
 * ومؤشر تحميل أثناء فتح الصفحة.
 *
 * ملاحظة أمان مهمة: بعض الأجهزة (خصوصًا إصدارات أندرويد Go/بعض الأنظمة
 * المخصّصة أو الأجهزة المُدارة عبر MDM) لا تملك مكوّن "Android System
 * WebView" مفعّلاً أو مثبَّتًا إطلاقًا، أو يكون قيد التحديث لحظة الفتح.
 * إنشاء WebView() في تلك الحالة يرمي استثناءً غير مُعالَج يُسقط التطبيق
 * بالكامل فورًا. لذلك يُغلَّف الإنشاء هنا بمحاولة/التقاط، مع بديل بسيط
 * (فتح الرابط بمتصفح النظام الخارجي) بدل انهيار التطبيق.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppWebViewScreen(
    url: String,
    title: String,
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var webViewUnavailable by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(top = 56.dp)) {
            if (webViewUnavailable) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = TextSecondaryMuted, modifier = Modifier.size(48.dp))
                    Text(
                        "تعذّر فتح الصفحة داخل التطبيق على هذا الجهاز",
                        color = TextPrimaryWhite,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                    Text(
                        "يمكنك فتحها مباشرة من متصفح جهازك بدلاً من ذلك.",
                        color = TextSecondaryMuted,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    GoldButton(
                        text = "فتح في المتصفح",
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (e: Exception) {
                                // لا يوجد متصفح على الجهاز أصلاً — نادر جدًا، نتجاهل بأمان.
                            }
                        }
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        try {
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }
                                }
                                loadUrl(url)
                            }
                        } catch (e: Throwable) {
                            // أي فشل هنا (مكوّن WebView مفقود/معطَّل/قيد التحديث) يُعالَج
                            // بأمان بدل أن يُسقط التطبيق بالكامل.
                            isLoading = false
                            webViewUnavailable = true
                            android.widget.FrameLayout(ctx)
                        }
                    }
                )
            }

            if (isLoading && !webViewUnavailable) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            }
        }

        Surface(
            color = LuxurySurfaceDark,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag("in_app_webview_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = TextPrimaryWhite
                    )
                }
                Text(
                    text = title,
                    color = TextPrimaryWhite,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}

