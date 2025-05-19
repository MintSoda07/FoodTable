import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcu.foodtable.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SigupScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    warningText: String,
    emailVerified: Boolean,
    nicknameValid: Boolean,
    onEmailVerifyClick: () -> Unit,
    onNicknameCheckClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    val primaryColor = Color(0xFFE76F51)
    val backgroundColor = Color(0xFFFFF1E6)

    val enterAnim = remember { Animatable(300f) }
    LaunchedEffect(Unit) {
        enterAnim.animateTo(0f, tween(700))
    }

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(100), label = "button_scale"
    )
    val coroutineScope = rememberCoroutineScope()

    val opacity = remember { Animatable(0f) }
    val slideY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        slideY.animateTo(0f, tween(800))
        opacity.animateTo(1f, tween(800))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = enterAnim.value.dp)
                .alpha(1f - (enterAnim.value / 300f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .alpha(opacity.value)
                    .offset(y = slideY.value.dp)
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.dish_icon),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "한 끼의 정성, 지금부터 함께 나눠요.",
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )
            }

            Text(
                text = "회원가입",
                fontSize = 28.sp,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = warningText.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = warningText,
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("이메일") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onEmailVerifyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("이메일 인증", color = Color.White)
            }

            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = { Text("닉네임") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onNicknameCheckClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("닉네임 중복 확인", color = Color.White)
            }

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("비밀번호") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("비밀번호 확인") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    pressed = true
                    onSignUpClick()
                    coroutineScope.launch {
                        delay(150)
                        pressed = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("회원가입 완료", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}
