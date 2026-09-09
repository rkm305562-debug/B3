package com.example.zainqhchat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.ui.components.GoldButton
import com.example.zainqhchat.ui.components.GoldOutlinedButton
import com.example.zainqhchat.ui.theme.GoldGradientEnd
import com.example.zainqhchat.ui.theme.GoldGradientStart
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.AuthUiState
import com.example.zainqhchat.ui.viewmodels.AuthViewModel

/**
 * شاشة التسجيل/الدخول الكاملة (تُستخدم بعد تسجيل الخروج مثلاً) — بطاقة
 * زجاجية (Glassmorphism) عائمة فوق خلفية داكنة متدرّجة مع بقعتي ضوء ملوّنتين
 * ضبابيتين خلفها لإحساس عمق. التصميم بالكامل جديد (زجاجي شفاف بحدود ناعمة)
 * بدل البطاقة الصلبة القديمة.
 */
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LuxuryBlackBg, Color(0xFF10192E), LuxuryBlackBg)
                )
            )
    ) {
        // بقعتا ضوء ضبابيتان خلف البطاقة الزجاجية لإحساس عمق (Glassmorphism)
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = 40.dp)
                .clip(CircleShape)
                .background(GoldGradientStart.copy(alpha = 0.28f))
                .blur(80.dp)
                .align(Alignment.TopStart)
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = 60.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(GoldGradientEnd.copy(alpha = 0.25f))
                .blur(80.dp)
                .align(Alignment.BottomEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AuthGlassCard(
                authViewModel = authViewModel,
                onAuthSuccess = onAuthSuccess,
                onDismiss = null
            )
        }
    }
}

/**
 * نفس نموذج التسجيل/الدخول، لكن كنافذة منبثقة (Dialog) زجاجية فوق أي شاشة —
 * تُستدعى فقط عندما يحاول "زائر" (غير مسجّل) الدخول إلى قسم يتطلب حسابًا.
 * التصفح العام في التطبيق لا يطلب تسجيلًا إطلاقًا قبل ذلك.
 */
@Composable
fun AuthPopupDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    onClick = onDismiss
                )
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        onClick = {} // يمتص النقر كي لا يُغلق النافذة عند الضغط داخل البطاقة
                    )
            ) {
                AuthGlassCard(
                    authViewModel = authViewModel,
                    onAuthSuccess = onAuthSuccess,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

/**
 * البطاقة الزجاجية المشتركة: الشعار + العنوان + الحقول + الأزرار. تُستخدم من
 * [AuthScreen] (صفحة كاملة) ومن [AuthPopupDialog] (نافذة منبثقة) معًا حتى لا
 * يتكرر أي منطق تسجيل/دخول.
 */
@Composable
private fun AuthGlassCard(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onDismiss: (() -> Unit)?
) {
    val uiState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isRegisterMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Authenticated -> onAuthSuccess()
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                authViewModel.clearError()
            }
            else -> {}
        }
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(TextPrimaryWhite.copy(alpha = 0.06f))
                .border(1.dp, TextPrimaryWhite.copy(alpha = 0.16f), RoundedCornerShape(32.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onDismiss != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("auth_popup_close_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondaryMuted)
                    }
                }
            }

            Image(
                painter = painterResource(id = com.example.R.drawable.app_logo),
                contentDescription = "شعار قروب بنات و شباب",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "قروب بنات و شباب",
                color = GoldPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )

            Text(
                text = if (isRegisterMode) "أنشئ حسابك الآن ✨" else "تسجيل الدخول إلى حسابك 🔑",
                color = TextSecondaryMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
            )

            LuxuryTextField(
                value = username,
                onValueChange = { username = it },
                label = "اسم المستخدم",
                leadingIcon = Icons.Default.Person,
                testTag = "username_input"
            )

            if (!isRegisterMode) {
                Spacer(modifier = Modifier.height(12.dp))
                LuxuryTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "كلمة المرور",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    testTag = "password_input"
                )
            }

            if (isRegisterMode) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "🔞 يجب أن يكون عمرك 18 عامًا فأكثر لإنشاء حساب واستخدام هذا التطبيق.",
                    color = TextSecondaryMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "إذا أردت الاحتفاظ بحسابك (الدخول من جهاز آخر أو بعد حذف " +
                        "التطبيق)، أضف كلمة مرور لاحقًا من الإعدادات ⚙️",
                    color = TextSecondaryMuted,
                    fontSize = 11.5.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(10.dp))
                val termsAnnotated = buildAnnotatedString {
                    append("بموافقتك على التسجيل وإنشاء الحساب فإنك تقرّ بأنك تجاوزت 18 عامًا، وتوافق على ")
                    pushStringAnnotation(tag = "terms", annotation = "terms")
                    withStyle(
                        style = SpanStyle(
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("الشروط والأحكام وسياسة الخصوصية")
                    }
                    pop()
                    append(" كاملةً.")
                }
                ClickableText(
                    text = termsAnnotated,
                    style = TextStyle(
                        color = TextSecondaryMuted,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Start
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { offset ->
                        termsAnnotated.getStringAnnotations(tag = "terms", start = offset, end = offset)
                            .firstOrNull()?.let { showTermsDialog = true }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(color = GoldPrimary)
            } else {
                GoldButton(
                    text = if (isRegisterMode) "إنشاء الحساب 👑" else "تسجيل الدخول 🔑",
                    onClick = {
                        if (isRegisterMode) {
                            authViewModel.register(username = username)
                        } else {
                            authViewModel.login(username, password)
                        }
                    },
                    testTag = if (isRegisterMode) "create_account_btn" else "login_btn"
                )

                Spacer(modifier = Modifier.height(12.dp))

                GoldOutlinedButton(
                    text = if (isRegisterMode) "لدي حساب بالفعل (تسجيل الدخول)" else "إنشاء حساب جديد",
                    onClick = { isRegisterMode = !isRegisterMode },
                    testTag = "toggle_auth_mode_btn"
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showTermsDialog) {
        TermsAndConditionsDialog(onDismiss = { showTermsDialog = false })
    }
}

@Composable
fun LuxuryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    testTag: String = "text_field"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondaryMuted) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = GoldPrimary) },
        trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
            {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "تبديل الرؤية",
                        tint = TextSecondaryMuted
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldPrimary,
            unfocusedBorderColor = LuxuryBorderGold,
            focusedTextColor = TextPrimaryWhite,
            unfocusedTextColor = TextPrimaryWhite,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
