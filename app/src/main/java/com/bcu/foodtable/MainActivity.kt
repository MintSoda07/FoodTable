package com.bcu.foodtable

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.ActivityTransition
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colorScheme = warmLightColorScheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainLoginScreen(
                        onLoginClick = {
                            ActivityTransition.startStatic(this@MainActivity, LoginActivity::class.java)
                        },
                        onSignUpClick = {
                            ActivityTransition.startStatic(this@MainActivity, SignUpActivity::class.java)
                        }
                    )
                }
            }
        }
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
    onSignUpClick: () -> Unit
) {
    val offsetX = remember { Animatable(-600f) }
    var showSubtitle by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        offsetX.animateTo(0f, tween(1200))
        delay(400)
        showSubtitle = true
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

        // 🔳 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 🌫️ 블러/그라데이션 오버레이
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

        // ✨ Lottie 애니메이션 (반짝이 효과)
        LottieAnimation(
            composition = lottieComposition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )

        // 🧱 메인 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 🧾 타이틀
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

            // 🔘 로그인 & 회원가입 버튼
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

