import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
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
    onKakaoLoginClick: () -> Unit
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

    // Lottie 애니메이션 구성
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.food2))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
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
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(120.dp)
                )
            }
        }
    }
}