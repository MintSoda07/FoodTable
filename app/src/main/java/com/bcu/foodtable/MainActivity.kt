package com.bcu.foodtable

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    data class AppPermission(
        val permission: String,
        val reason: String
    )

    private val permissionsToRequest = mutableListOf<AppPermission>()
    private var currentPermissionIndex by mutableStateOf(0)
    private var showReasonDialog by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // 다음 권한 진행
            currentPermissionIndex++
            showReasonDialog = currentPermissionIndex < permissionsToRequest.size
        }

    private var pendingChatUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        pendingChatUid = intent?.getStringExtra("chatUid")

        // ✅ 이미 허용된 권한은 제외하고, 남은 것만 요청 리스트에 담기
        permissionsToRequest.clear()
        permissionsToRequest.addAll(pendingNormalPermissions())

        setContent {
            MaterialTheme(colorScheme = warmLightColorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var introFinished by remember { mutableStateOf(false) }

                    Box(Modifier.fillMaxSize()) {
                        MainLoginScreen(
                            onLoginClick = {
                                val i = Intent(this@MainActivity, LoginActivity::class.java).apply {
                                    pendingChatUid?.let { putExtra("chatUid", it) }
                                }
                                startActivity(i)
                            },
                            onSignUpClick = {
                                val i = Intent(this@MainActivity, SignUpActivity::class.java).apply {
                                    pendingChatUid?.let { putExtra("chatUid", it) }
                                }
                                startActivity(i)
                            },
                            onIntroEnd = {
                                introFinished = true
                                // ✅ 남은 권한이 있을 때만 안내 다이얼로그 시작
                                currentPermissionIndex = 0
                                showReasonDialog = permissionsToRequest.isNotEmpty()
                            }
                        )

                        // 권한 안내 다이얼로그
                        if (introFinished && showReasonDialog) {
                            val current = permissionsToRequest[currentPermissionIndex]
                            ReasonDialog(
                                reason = current.reason,
                                onConfirm = { permissionLauncher.launch(current.permission) },
                                onDismiss = {
                                    currentPermissionIndex++
                                    showReasonDialog = currentPermissionIndex < permissionsToRequest.size
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    //  이미 허용된 권한은 걸러주는 함수
    private fun pendingNormalPermissions(): List<AppPermission> {
        fun isGranted(p: String) =
            ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

        val list = mutableListOf<AppPermission>()

        // Android 13+ 알림
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                list += AppPermission(
                    Manifest.permission.POST_NOTIFICATIONS,
                    "알림 권한 → 레시피 알림과 앱 소식을 받기 위해 필요해요."
                )
            }
        }

        // 미디어 읽기: 13+는 READ_MEDIA_IMAGES, 그 이전은 READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isGranted(Manifest.permission.READ_MEDIA_IMAGES)) {
                list += AppPermission(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    "사진 기능 → 레시피 조리 과정을 도와드리기 위해 필요해요."
                )
            }
        } else {
            if (!isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                list += AppPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    "사진 기능 → 레시피 조리 과정을 도와드리기 위해 필요해요."
                )
            }
        }

        if (!isGranted(Manifest.permission.RECORD_AUDIO)) {
            list += AppPermission(
                Manifest.permission.RECORD_AUDIO,
                "마이크 기능 → 조리 중 음성 도우미 서비스를 위해 필요해요."
            )
        }

        if (!isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            list += AppPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                "위치 기능 → 주변 맛집 찾기 기능을 위해 필요해요."
            )
        }

        return list
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingChatUid = intent.getStringExtra("chatUid")
    }
}
@Composable
fun ReasonDialog(reason: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f)
    ) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("권한 요청 안내") },
            text = { Text(reason) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text("허용") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("거부") }
            }
        )
    }
}

private val warmLightColorScheme = lightColorScheme(
    primary = Color(0xFFE26D47),
    onPrimary = Color.White,
    secondary = Color(0xFFFFF3EE),
    onSecondary = Color(0xFF4B4B4B),
    background = Color(0xFFFFF9F6),
    onBackground = Color(0xFF3A3A3A),
    surface = Color.White,
    onSurface = Color(0xFF3A3A3A),
    outline = Color(0xFFE0E0E0)
)

@Composable
fun MainLoginScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onIntroEnd: () -> Unit
) {
    val offsetX = remember { Animatable(-600f) }
    var showSubtitle by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        offsetX.animateTo(0f, tween(1200, easing = FastOutSlowInEasing))
        delay(400)
        showSubtitle = true
        delay(1200) // 인트로 종료 타이밍
        onIntroEnd()
    }

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (showSubtitle) 1f else 0f,
        animationSpec = tween(700),
        label = "fade-subtitle"
    )

    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.Asset("sparkle_overlay.json"))
    val progress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = LottieConstants.IterateForever
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xBB000000), Color.Transparent, Color(0xFF2B2B2B)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        LottieAnimation(
            composition = lottieComposition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(top = 100.dp)) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = Color.White,
                    fontSize = 52.sp,
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(id = R.string.app_name_sub),
                    color = Color(0xFFEEEEEE),
                    fontSize = 20.sp,
                    modifier = Modifier
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        .alpha(subtitleAlpha)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = showSubtitle,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        Button(
                            onClick = onLoginClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE26D47),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(8.dp)
                        ) {
                            Text(text = stringResource(id = R.string.login_button), fontSize = 17.sp)
                        }
                        OutlinedButton(
                            onClick = onSignUpClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White.copy(alpha = 0.95f)
                            )
                        ) {
                            Text(text = stringResource(id = R.string.signup_button), fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
