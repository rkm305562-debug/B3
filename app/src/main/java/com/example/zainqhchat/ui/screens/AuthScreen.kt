package com.example.zainqhchat.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GenericShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zainqhchat.ui.viewmodels.AuthUiState
import com.example.zainqhchat.ui.viewmodels.AuthViewModel

// ============================================================================
// هوية بصرية جديدة كليًا لشاشة التسجيل/الدخول: "بلّورة" (Crystal). خلفية
// زمرّدية داكنة متدرّجة تحاكي عمق الحجر الكريم، بطاقة مقصوصة الزاوية كقطعة
// بلّور مصقولة، وأيقونات "معينية" (Diamond) بلون أبيض ناصع يعاكس الخلفية
// الزمرّدية الداكنة كي تكون واضحة تمامًا كما طُلب، مع لمسة مرجانية دافئة
// (Coral) كلون مقابل حاد يكسر برودة الزمرّدي في نقاط الفعل الرئيسية.
// ============================================================================

private val EmeraldDeep = Color(0xFF06322D)
private val EmeraldMid = Color(0xFF0C4F47)
private val CrystalTeal = Color(0xFF2DD8BE)
private val CrystalCoral = Color(0xFFFF7A59)
private val CrystalCoralDeep = Color(0xFFE85A3B)
private val CrystalWhite = Color(0xFFF3FFFC)
private val CrystalMuted = Color(0xFFAFE0D6)

/** شكل "القطعة البلّورية": زاوية علوية مقصوصة بحدّة كأنها وجه مصقول، وباقي
 *  الحواف بحرف دائري ناعم — يكسر رتابة المربعات المستديرة الاعتيادية. */
private fun crystalCardShape(cutDp: Float) = GenericShape { size, _ ->
    val cut = cutDp
    val r = 26f
    moveTo(cut, 0f)
    lineTo(size.width - r, 0f)
    quadraticTo(size.width, 0f, size.width, r)
    lineTo(size.width, size.height - r)
    quadraticTo(size.width, size.height, size.width - r, size.height)
    lineTo(r, size.height)
    quadraticTo(0f, size.height, 0f, size.height - r)
    lineTo(0f, cut)
    close()
}

private fun crystalButtonShape() = GenericShape { size, _ ->
    val cut = size.height * 0.42f
    moveTo(0f, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width - cut, size.height)
    lineTo(0f, size.height)
    close()
}

/**
 * شاشة التسجيل/الدخول الكاملة (تُستخدم بعد تسجيل الخروج مثلاً).
 */
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    CrystalBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AuthCrystalCard(
                authViewModel = authViewModel,
                onAuthSuccess = onAuthSuccess,
                onDismiss = null
            )
        }
    }
}

/**
 * نفس نموذج التسجيل/الدخول، كنافذة منبثقة (Dialog) فوق أي شاشة — تُستدعى
 * فقط عندما يحاول "زائر" الدخول إلى قسم يتطلب حسابًا.
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
        CrystalBackdrop(dim = true, onScrimClick = onDismiss) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {}
                        )
                ) {
                    AuthCrystalCard(
                        authViewModel = authViewModel,
                        onAuthSuccess = onAuthSuccess,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

/** الخلفية الزمرّدية المشتركة: تدرّج داكن + معينات بلّورية شبحية متناثرة. */
@Composable
private fun CrystalBackdrop(
    dim: Boolean = false,
    onScrimClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(Brush.verticalGradient(listOf(EmeraldDeep, EmeraldMid, EmeraldDeep)))
            .then(
                if (dim) {
                    Modifier
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = { onScrimClick?.invoke() }
                        )
                } else Modifier
            )
    ) {
        GhostDiamond(sizeDp = 130.dp, x = (-40).dp, y = 70.dp, alpha = 0.10f, align = Alignment.TopStart)
        GhostDiamond(sizeDp = 90.dp, x = 30.dp, y = 40.dp, alpha = 0.08f, align = Alignment.TopEnd)
        GhostDiamond(sizeDp = 160.dp, x = 20.dp, y = (-60).dp, alpha = 0.09f, align = Alignment.BottomEnd)
        GhostDiamond(sizeDp = 70.dp, x = (-25).dp, y = (-90).dp, alpha = 0.10f, align = Alignment.BottomStart)

        content()
    }
}

@Composable
private fun GhostDiamond(sizeDp: Dp, x: Dp, y: Dp, alpha: Float, align: Alignment) {
    Box(
        modifier = Modifier
            .align(align)
            .offset(x = x, y = y)
            .size(sizeDp)
            .rotate(45f)
            .border(1.dp, CrystalTeal.copy(alpha = alpha), RoundedCornerShape(18.dp))
    )
}

/**
 * البطاقة البلّورية المشتركة: شعار "معيّن" عائم أعلى البطاقة، عنوان، حقول
 * بأيقونات معينية بيضاء متباينة، وزرّ رئيسي بقصّة سداسية.
 */
@Composable
private fun AuthCrystalCard(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onDismiss: (() -> Unit)?
) {
    val uiState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val passwordHintText = stringResource(com.example.R.string.auth_password_hint)

    var isRegisterMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Authenticated -> {
                if (isRegisterMode) {
                    Toast.makeText(context, passwordHintText, Toast.LENGTH_LONG).show()
                }
                onAuthSuccess()
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                authViewModel.clearError()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.padding(top = 44.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(crystalCardShape(64f))
                .background(
                    Brush.linearGradient(
                        listOf(CrystalWhite.copy(alpha = 0.10f), CrystalWhite.copy(alpha = 0.03f))
                    )
                )
                .border(
                    BorderStroke(
                        1.4.dp,
                        Brush.linearGradient(listOf(CrystalTeal.copy(alpha = 0.65f), CrystalCoral.copy(alpha = 0.35f)))
                    ),
                    crystalCardShape(64f)
                )
                .padding(top = 40.dp, start = 26.dp, end = 26.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onDismiss != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("auth_popup_close_btn")) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(com.example.R.string.auth_close_desc), tint = CrystalMuted)
                    }
                }
            }

            Text(
                text = stringResource(com.example.R.string.app_name),
                color = CrystalWhite,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(if (isRegisterMode) com.example.R.string.auth_register_subtitle else com.example.R.string.auth_login_subtitle),
                color = CrystalMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
            )

            CrystalField(
                value = username,
                onValueChange = { username = it },
                placeholder = stringResource(com.example.R.string.auth_username_label),
                icon = Icons.Default.Person,
                testTag = "username_input"
            )

            if (!isRegisterMode) {
                Spacer(modifier = Modifier.height(14.dp))
                CrystalField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = stringResource(com.example.R.string.auth_password_label),
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    testTag = "password_input"
                )
            }

            if (isRegisterMode) {
                Spacer(modifier = Modifier.height(16.dp))
                val consentPrefix = stringResource(com.example.R.string.auth_consent_prefix)
                val consentLink = stringResource(com.example.R.string.auth_consent_link)
                val termsAnnotated = buildAnnotatedString {
                    append(consentPrefix)
                    pushStringAnnotation(tag = "terms", annotation = "terms")
                    withStyle(
                        style = SpanStyle(
                            color = CrystalTeal,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(consentLink)
                    }
                    pop()
                }
                ClickableText(
                    text = termsAnnotated,
                    style = TextStyle(
                        color = CrystalMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { offset ->
                        termsAnnotated.getStringAnnotations(tag = "terms", start = offset, end = offset)
                            .firstOrNull()?.let { showTermsDialog = true }
                    }
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(color = CrystalCoral)
            } else {
                CrystalPrimaryButton(
                    text = stringResource(if (isRegisterMode) com.example.R.string.auth_register_btn else com.example.R.string.auth_login_btn),
                    onClick = {
                        if (isRegisterMode) {
                            authViewModel.register(username = username)
                        } else {
                            authViewModel.login(username, password)
                        }
                    },
                    testTag = if (isRegisterMode) "create_account_btn" else "login_btn"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(if (isRegisterMode) com.example.R.string.auth_switch_to_login else com.example.R.string.auth_switch_to_register),
                    color = CrystalTeal,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { isRegisterMode = !isRegisterMode }
                        .testTag("toggle_auth_mode_btn")
                        .padding(8.dp)
                )
            }
        }

        // شعار "المعيّن" العائم أعلى البطاقة — تدوير 45° للإطار الخارجي مع
        // تدوير عكسي للصورة الداخلية كي يبقى محتواها مستقيمًا مقروءًا.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(74.dp)
                .rotate(45f)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(CrystalCoral, CrystalCoralDeep)))
                .border(1.5.dp, CrystalWhite.copy(alpha = 0.55f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.example.R.drawable.ic_launcher_photo),
                contentDescription = stringResource(com.example.R.string.content_desc_app_logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .rotate(-45f)
                    .clip(RoundedCornerShape(12.dp))
            )
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

/** شارة أيقونة "معينية" صغيرة: تباين أبيض ناصع فوق تدرّج مرجاني، معاكسة
 *  تمامًا للون الخلفية الزمرّدي الداكن كي تكون بارزة وواضحة دومًا. */
@Composable
private fun DiamondIconChip(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .rotate(45f)
            .clip(RoundedCornerShape(9.dp))
            .background(Brush.linearGradient(listOf(CrystalTeal, EmeraldMid)))
            .border(1.dp, CrystalWhite.copy(alpha = 0.5f), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CrystalWhite,
            modifier = Modifier
                .size(17.dp)
                .rotate(-45f)
        )
    }
}

/** حقل إدخال "بلّوري": شارة معينية + نص أبيض فوق كبسولة زجاجية شبه شفافة،
 *  بلا الحدود المعدنية الافتراضية لحقول Material المعتادة. */
@Composable
private fun CrystalField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    testTag: String = "text_field"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CrystalWhite.copy(alpha = 0.07f))
            .border(1.dp, CrystalWhite.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DiamondIconChip(icon)
        Spacer(modifier = Modifier.width(14.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = CrystalMuted.copy(alpha = 0.7f), fontSize = 15.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = CrystalWhite, fontSize = 15.sp),
                cursorBrush = Brush.linearGradient(listOf(CrystalCoral, CrystalCoral)),
                visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag)
            )
        }
        if (isPassword && onTogglePasswordVisibility != null) {
            IconButton(onClick = onTogglePasswordVisibility, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(com.example.R.string.auth_toggle_password_desc),
                    tint = CrystalMuted
                )
            }
        }
    }
}

/** الزر الرئيسي بقصّة سداسية (Hexagon) مرجانية دافئة تكسر برودة الزمرّدي. */
@Composable
private fun CrystalPrimaryButton(text: String, onClick: () -> Unit, testTag: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(crystalButtonShape())
            .background(Brush.horizontalGradient(listOf(CrystalCoral, CrystalCoralDeep)))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
