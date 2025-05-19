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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcu.foodtable.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MainLoginScreen(
    onLoginClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    onAnimationsFinished: () -> Unit = {}
) {
    val offsetX = remember { Animatable(-600f) }
    var showSubtitle by remember { mutableStateOf(false) }

    // 애니메이션 시작
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 이미지 + 어두운 오버레이
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000)) // 반투명 블랙
        )

        // 전체 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 텍스트
            Column(modifier = Modifier.padding(top = 100.dp)) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = Color.White,
                    fontSize = 48.sp,
                    modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(id = R.string.app_name_sub),
                    color = Color(0xFFCCCCCC),
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
                // 로그인 버튼
                Button(
                    onClick = onLoginClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xAAFFFFFF),
                        contentColor = Color.Black
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

                // 회원가입 버튼 (투명 스타일)
                OutlinedButton(
                    onClick = onSignUpClick,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.8f)
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
