import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcu.foodtable.R

@Composable
fun LoginScreen(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    warningText: String,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    val primaryColor = Color(0xFFE76F51)
    val overlayColor = Color(0xF2FFFFFF) // 반투명 흰색 배경

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF1E6), // 크림톤
                        Color(0xFFFDE8D4)  // 오렌지 빛 살짝
                    )
                )
            )
    ) {
        // 반투명 Surface 로그인 카드
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(24.dp),
            color = overlayColor,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 앱 로고
                Image(
                    painter = painterResource(id = R.drawable.dish_icon),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    colorFilter = ColorFilter.tint(primaryColor)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 앱 이름
                Text(
                    text = stringResource(id = R.string.app_name),
                    fontSize = 30.sp,
                    color = primaryColor
                )

                // 서브 타이틀
                Text(
                    text = "따뜻한 한끼, 함께 나눠요",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // 경고 메시지
                AnimatedVisibility(visible = warningText.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
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

                // 이메일 입력
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("이메일") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_person_24),
                            contentDescription = null
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // 비밀번호 입력
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("비밀번호") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_lock_24),
                            contentDescription = null
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                // 로그인 버튼
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    )
                ) {
                    Text("로그인", fontSize = 16.sp)
                }

                // 회원가입 버튼
                TextButton(onClick = onSignUpClick) {
                    Text(
                        text = "아직 계정이 없으신가요? 회원가입",
                        color = primaryColor,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}