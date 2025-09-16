package com.bcu.foodtable.ui

import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R
import java.security.MessageDigest
import androidx.fragment.app.FragmentActivity
import com.bcu.foodtable.JetpackCompose.LoginViewModel
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreenImproved(
    email: String,
    password: String,
    isAutoLogin: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAutoLoginChange: (Boolean) -> Unit,
    warningText: String,
    isLoggingIn: Boolean,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    onDebugBio: (android.content.Context) -> Unit,
    // 생체 로그인 콜백(복호화된 토큰 전달)
    onBiometricLoginWithToken: (tokenPlain: ByteArray) -> Unit,
    // ViewModel
    vm: LoginViewModel
) {
    val primaryColor = Color(0xFFE76F51)
    val backgroundColorStart = Color(0xFFFFF7F0)
    val backgroundColorEnd = Color(0xFFFFF1E6)
    val cardBackgroundColor = Color(0xFAFFFFFF)
    val textPrimaryColor = Color(0xFF333333)
    val textSecondaryColor = Color.Gray
    val errorColor = Color(0xFFD32F2F)

    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Lottie
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.food2))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    // 🔐 생체 상태
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() } // 안전 캐스팅
    var bioAvailable by remember { mutableStateOf(false) }
    var bioStored by remember { mutableStateOf(false) }

    // VM 이벤트 수신
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            when (ev) {
                is LoginViewModel.Event.ShowSnack -> Log.d("BIO", ev.msg)
                is LoginViewModel.Event.BiometricReady -> {
                    bioAvailable = ev.available
                    bioStored = ev.hasStored
                }
                LoginViewModel.Event.BiometricLoginSuccess -> { /* 네비게이션 등 필요시 */ }
            }
        }
    }

    // 초기 가용/저장 상태 체크
    LaunchedEffect(activity) {
        activity?.let {
            vm.refreshBiometricState(it)
            onDebugBio(it.applicationContext)
        }
    }
    PrintKakaoKeyHash()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(backgroundColorStart, backgroundColorEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.dish_icon),
                contentDescription = stringResource(id = R.string.app_name) + " 로고",
                modifier = Modifier.size(72.dp),
                colorFilter = ColorFilter.tint(primaryColor)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Text(
                text = "따뜻한 한끼, 함께 나눠요",
                fontSize = 14.sp,
                color = textSecondaryColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = cardBackgroundColor,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(visible = warningText.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "경고",
                                tint = errorColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = warningText,
                                color = errorColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("이메일") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "이메일") },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("비밀번호") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "비밀번호") },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기"
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            onLoginClick()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = onForgotPasswordClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("비밀번호를 잊으셨나요?", color = textSecondaryColor, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = isAutoLogin,
                                onValueChange = onAutoLoginChange
                            )
                    ) {
                        Checkbox(checked = isAutoLogin, onCheckedChange = null)
                        Text("자동 로그인", fontSize = 14.sp, color = textPrimaryColor)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onLoginClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text("로그인", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                        Text("또는", modifier = Modifier.padding(horizontal = 8.dp), fontSize = 13.sp, color = textSecondaryColor)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 소셜 로그인 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onGoogleLoginClick,
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text("G", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                        }

                        OutlinedButton(
                            onClick = onKakaoLoginClick,
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = BorderStroke(1.dp, Color(0xFFFEE500))
                        ) {
                            Text("K", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3C1E1E))
                        }
                    }

                    // 🔽🔽 소셜 버튼 '아래'에 가로 상태 + 액션 Row
                    Spacer(Modifier.height(12.dp))

                    // 설정 화면 → 복귀 시 상태 리프레시
                    val enrollLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { activity?.let { vm.refreshBiometricState(it) } }

                    // ✅ Keystore 설정과 일치하도록 STRONG | DEVICE_CREDENTIAL 사용
                    val enrollAllow = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 왼쪽: 상태 텍스트
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (bioAvailable) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "생체 로그인 상태 · 기기 지원: ${if (bioAvailable) "가능" else "미설정"} · 저장된 토큰: ${if (bioStored) "있음" else "없음"}",
                                fontSize = 13.sp,
                                color = Color(0xFF555555),
                                maxLines = 1
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // 오른쪽: 상황별 액션
                        when {
                            !bioAvailable -> {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                                                putExtra(
                                                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                                    enrollAllow // ✅ 여기!
                                                )
                                            }
                                            enrollLauncher.launch(intent)
                                        } catch (_: Exception) {
                                            // OS < 11 등 폴백
                                            enrollLauncher.launch(Intent(Settings.ACTION_SECURITY_SETTINGS))
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) { Text("설정 열기", fontSize = 13.sp) }
                            }
                            bioAvailable && !bioStored -> {
                                AssistChip(
                                    onClick = { /* 안내용 */ },
                                    label = { Text("먼저 이메일/비번으로 1회 로그인", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                                )
                            }
                            else -> {
                                // bioAvailable && bioStored → 아래에 실제 생체 로그인 버튼이 뜸
                                Spacer(Modifier.width(0.dp))
                            }
                        }
                    }
                    // 🔼🔼 가로 상태 Row 끝

                    // 생체 로그인 버튼: 기기 지원 & 저장된 토큰이 있을 때 노출
                    Spacer(modifier = Modifier.height(12.dp))
                    if (bioAvailable && bioStored) {
                        OutlinedButton(
                            onClick = {
                                activity?.let { a ->
                                    onDebugBio(a.applicationContext)
                                    vm.startBiometricLogin(a) { tokenPlain ->
                                        onBiometricLoginWithToken(tokenPlain)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("얼굴/지문으로 로그인")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("아직 계정이 없으신가요?", fontSize = 14.sp, color = textSecondaryColor)
                TextButton(onClick = onSignUpClick) {
                    Text("회원가입", color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 로딩 모달
        if (isLoggingIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(BorderStroke(2.dp, Color(0xFFE0E0E0)), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun PrintKakaoKeyHash() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            val signatures = info.signingInfo?.apkContentsSigners
            if (signatures != null) {
                val md = MessageDigest.getInstance("SHA")
                for (signature in signatures) {
                    md.update(signature.toByteArray())
                    val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                    Log.d("KeyHash", "카카오 해시 키: $keyHash")
                }
            } else {
                Log.e("KeyHash", "signingInfo is null")
            }
        } catch (e: Exception) {
            Log.e("KeyHash", "Unable to get key hash", e)
        }
    }
}

/** Context에서 안전하게 FragmentActivity 찾기 (ContextWrapper 중첩 대응) */
private tailrec fun Context.findFragmentActivity(): FragmentActivity? =
    when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
