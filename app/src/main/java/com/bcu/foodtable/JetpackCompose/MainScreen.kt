package com.bcu.foodtable.JetpackCompose

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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val WarmLightColorScheme = lightColorScheme(
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
    onLoginClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    onAnimationsFinished: () -> Unit = {}
) {
    val offsetX = remember { Animatable(-600f) }
    var showSubtitle by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        offsetX.animateTo(0f, tween(1000))
        delay(300)
        showSubtitle = true
        onAnimationsFinished()
    }

    val subtitleAlpha by animateFloatAsState(
        targetValue = if (showSubtitle) 1f else 0f,
        animationSpec = tween(700), label = "fade-in"
    )

    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.Asset("warm_welcome.json"))
    val progress by animateLottieCompositionAsState(lottieComposition, iterations = LottieConstants.IterateForever)

    MaterialTheme(colorScheme = WarmLightColorScheme) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 패치마다 모자 이미지
            Image(
                painter = painterResource(id = R.drawable.login_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 상단 그래디언트 + 테스트 오버레이
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xAAFFEFEC), Color.Transparent, Color(0xAA000000)),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // 전체 컨테츠
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 상단 테스트
                Column(modifier = Modifier.padding(top = 100.dp)) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        color = WarmLightColorScheme.primary,
                        fontSize = 48.sp,
                        modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(id = R.string.app_name_sub),
                        color = WarmLightColorScheme.onBackground.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        modifier = Modifier
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                            .alpha(subtitleAlpha)
                    )
                }

                // 하단 버튼
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onLoginClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmLightColorScheme.primary,
                            contentColor = WarmLightColorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.login_button),
                            fontSize = 16.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onSignUpClick,
                        border = BorderStroke(1.dp, WarmLightColorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WarmLightColorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.signup_button),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}