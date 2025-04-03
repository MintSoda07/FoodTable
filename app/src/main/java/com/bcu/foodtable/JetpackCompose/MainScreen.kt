package com.bcu.foodtable.JetpackCompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

    // 1. 슬라이드 인 & 딜레이 후 subtitle 표시
    LaunchedEffect(Unit) {
        offsetX.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 1000)
        )
        delay(300)
        showSubtitle = true
        onAnimationsFinished()
    }

    // 2. 서브타이틀 페이드 인 애니메이션
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (showSubtitle) 1f else 0f,
        animationSpec = tween(durationMillis = 700), label = "fade-in"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x5D000000))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 타이틀 영역
            Column(modifier = Modifier.padding(top = 85.dp)) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    color = Color.White,
                    fontSize = 60.sp,
                    modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.app_name_sub),
                    color = Color(0xFFDBDBDB),
                    fontSize = 24.sp,
                    modifier = Modifier
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        .alpha(subtitleAlpha) // ← 여기만 페이드 인 적용
                )
            }

            // 버튼 영역
            Column(
                modifier = Modifier
                    .padding(bottom = 50.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onLoginClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x32818181),
                        contentColor = Color(0xC7FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 35.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.login_button),
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = onSignUpClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x00818181),
                        contentColor = Color(0xA6CECECE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 35.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.signup_button),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
