package com.example.zainqhchat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.zainqhchat.ui.theme.GoldDark
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxurySurfaceDark
import com.example.zainqhchat.ui.theme.StatusOnlineGreen
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted

/**
 * الصفحة الرئيسية لـ"الزائر" (غير مسجّل) — يمكن تصفّحها بحرّية دون أي طلب
 * تسجيل. فقط عند الضغط على قسم يحتاج حسابًا فعليًا (الدردشات الخاصة،
 * العملات، الإعدادات، تواصل مع المدير) تظهر نافذة التسجيل المنبثقة عبر
 * [onRequireAuth]. قسم "المتصلون الآن" و"الدردشة العامة" (الرابط الخارجي)
 * متاحان للجميع دون تسجيل.
 */
@Composable
fun GuestDashboardScreen(
    onOpenOnlineUsers: () -> Unit,
    onOpenPublicChatLink: () -> Unit,
    onRequireAuth: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBlackBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { GuestHeader(onSignInClick = onRequireAuth) }

            item {
                DashboardCard(
                    title = "الدردشات",
                    subtitle = "سجّل دخولك للدردشة العامة والمحادثات الخاصة",
                    icon = Icons.Default.ChatBubble,
                    iconGradient = listOf(GoldPrimary, GoldDark),
                    statCount = 0,
                    statLabel = null,
                    avatars = emptyList(),
                    onClick = onRequireAuth,
                    testTag = "dashboard_card_chats"
                )
            }

            item {
                DashboardCard(
                    title = "الدردشة العامة 🌐",
                    subtitle = "انضم إلى غرفة الدردشة العامة الخارجية — مفتوحة للجميع",
                    icon = Icons.Default.Language,
                    iconGradient = listOf(StatusOnlineGreen, Color(0xFF16A34A)),
                    statCount = 0,
                    statLabel = null,
                    avatars = emptyList(),
                    onClick = onOpenPublicChatLink,
                    testTag = "dashboard_card_public_chat_link"
                )
            }

            item {
                DashboardCard(
                    title = "المتصلون الآن",
                    subtitle = "اكتشف من هو متصل الآن (التصفح متاح للجميع)",
                    icon = Icons.Default.People,
                    iconGradient = listOf(StatusOnlineGreen, Color(0xFF16A34A)),
                    statCount = 0,
                    statLabel = null,
                    avatars = emptyList(),
                    onClick = onOpenOnlineUsers,
                    testTag = "dashboard_card_online"
                )
            }

            item {
                DashboardCard(
                    title = "العملات",
                    subtitle = "سجّل دخولك لعرض رصيدك والمتجر",
                    icon = Icons.Default.MonetizationOn,
                    iconGradient = listOf(GoldPrimary, GoldDark),
                    statCount = 0,
                    statLabel = null,
                    avatars = emptyList(),
                    onClick = onRequireAuth,
                    testTag = "dashboard_card_currency"
                )
            }

            item {
                DashboardCard(
                    title = "تواصل مع المدير",
                    subtitle = "سجّل دخولك للتواصل مع إدارة التطبيق",
                    icon = Icons.Default.SupportAgent,
                    iconGradient = listOf(GoldPrimary, Color(0xFF0EA5E9)),
                    statCount = 0,
                    statLabel = null,
                    avatars = emptyList(),
                    onClick = onRequireAuth,
                    testTag = "dashboard_card_contact_admin"
                )
            }

            item {
                DashboardCard(
                    title = "الإعدادات",
                    subtitle = "سجّل دخولك لعرض ملفك الشخصي وإعداداتك",
                    icon = Icons.Default.Settings,
                    iconGradient = listOf(Color(0xFF64748B), Color(0xFF475569)),
                    statCount = 0,
                    statLabel = null,
                    avatars = emptyList(),
                    onClick = onRequireAuth,
                    testTag = "dashboard_card_settings"
                )
            }
        }
    }
}

@Composable
private fun GuestHeader(onSignInClick: () -> Unit) {
    Surface(
        color = LuxurySurfaceDark,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_photo),
                            contentDescription = "شعار قروب بنات و شباب",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "قروب بنات و شباب",
                        color = TextPrimaryWhite,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }

                Surface(
                    color = GoldPrimary,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .clickable(onClick = onSignInClick)
                        .testTag("guest_sign_in_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, tint = LuxuryBlackBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل الدخول", color = LuxuryBlackBg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "مرحبًا بك زائرًا 👋",
                color = TextPrimaryWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
            Text(
                text = "تصفّح بحرية، وسجّل حسابك عند أول محادثة أو قسم يتطلب ذلك.",
                color = TextSecondaryMuted,
                fontSize = 14.sp
            )
        }
    }
}
