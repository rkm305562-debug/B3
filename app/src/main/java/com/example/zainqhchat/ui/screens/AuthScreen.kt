package com.example.zainqhchat.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.ui.components.GoldButton
import com.example.zainqhchat.ui.components.GoldOutlinedButton
import com.example.zainqhchat.ui.components.LuxuryCard
import com.example.zainqhchat.ui.theme.GoldDark
import com.example.zainqhchat.ui.theme.GoldPrimary
import com.example.zainqhchat.ui.theme.LuxuryBlackBg
import com.example.zainqhchat.ui.theme.LuxuryBorderGold
import com.example.zainqhchat.ui.theme.LuxurySurfaceCard
import com.example.zainqhchat.ui.theme.TextPrimaryWhite
import com.example.zainqhchat.ui.theme.TextSecondaryMuted
import com.example.zainqhchat.ui.viewmodels.AuthUiState
import com.example.zainqhchat.ui.viewmodels.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isRegisterMode by remember { mutableStateOf(true) }

    // حقول التسجيل — التسجيل نفسه أصبح باسم فقط؛ العمر والصورة وكلمة المرور
    // تُضاف لاحقًا من "تعديل الملف الشخصي" و"الأمان" في الإعدادات.
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Authenticated -> {
                onAuthSuccess()
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                authViewModel.clearError()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LuxuryBlackBg, Color(0xFF13131A), LuxuryBlackBg)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // شعار التطبيق الحقيقي — نفس صورة الأيقونة والشاشة الرئيسية في كل مكان.
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_logo),
                contentDescription = "شعار دردشة توتة",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier
                    .size(120.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .border(2.dp, GoldPrimary.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // عنوان ورأس الصفحة الفاخر
            Text(
                text = "دردشة توتة",
                color = GoldPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Text(
                text = if (isRegisterMode) "أنشئ حسابك الآن ✨" else "تسجيل الدخول إلى حسابك 🔑",
                color = TextSecondaryMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            LuxuryCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // اسم المستخدم (يدعم العربية والإنجليزية والأرقام والمسافات بالكامل)
                    LuxuryTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "اسم المستخدم",
                        leadingIcon = Icons.Default.Person,
                        testTag = "username_input"
                    )

                    // في وضع تسجيل الدخول فقط نحتاج كلمة المرور (لمن أضافها
                    // سابقًا من الإعدادات). التسجيل نفسه أصبح بالاسم فقط.
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
                            text = "إذا أردت الاحتفاظ بحسابك (الدخول من جهاز آخر أو بعد حذف " +
                                "التطبيق)، أضف كلمة مرور لاحقًا من الإعدادات ⚙️",
                            color = TextSecondaryMuted,
                            fontSize = 11.5.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        val termsAnnotated = buildAnnotatedString {
                            append("بالضغط على زر التسجيل فإنك توافق على ")
                            pushStringAnnotation(tag = "terms", annotation = "terms")
                            withStyle(
                                style = SpanStyle(
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append("الشروط والسياسات")
                            }
                            pop()
                        }
                        ClickableText(
                            text = termsAnnotated,
                            style = androidx.compose.ui.text.TextStyle(
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
            }

            Spacer(modifier = Modifier.height(32.dp))
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
            focusedContainerColor = LuxurySurfaceCard,
            unfocusedContainerColor = LuxurySurfaceCard
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
