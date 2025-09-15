import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    emailVerificationSent: Boolean,
    nicknameValid: Boolean,
    onEmailVerifyClick: () -> Unit,
    onCheckVerificationClick: () -> Unit,
    onNicknameCheckClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    // Palette
    val primaryColor = Color(0xFFE76F51)
    val backgroundColor = Color(0xFFFFF1E6)
    val cardColor = Color.White.copy(alpha = 0.95f)

    // Animations
    val enterY = remember { Animatable(300f) }
    LaunchedEffect(Unit) { enterY.animateTo(0f, tween(600)) }

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(100), label = "scale")
    val scope = rememberCoroutineScope()

    val opacity = remember { Animatable(0f) }
    val slideY = remember { Animatable(20f) }
    LaunchedEffect(Unit) {
        slideY.animateTo(0f, tween(700))
        opacity.animateTo(1f, tween(700))
    }

    // Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.92f)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-40).dp, y = (-40).dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.09f))
                .blur(40.dp)
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = 240.dp, y = 100.dp)
                .clip(CircleShape)
                .background(primaryColor.copy(alpha = 0.07f))
                .blur(36.dp)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            // ===== 헤더(아이콘 크게 + 담백한 타이틀) =====
            Column(
                modifier = Modifier
                    .alpha(opacity.value)
                    .offset(y = slideY.value.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp) // ★ 컨테이너 크게
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                listOf(
                                    primaryColor.copy(alpha = 0.20f),
                                    primaryColor.copy(alpha = 0.10f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.dish_icon),
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(84.dp) // ★ 아이콘은 여유 있게
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "한 끼의 정성,\n지금부터 함께 나눠요.",
                    fontSize = 18.sp,
                    color = Color.Gray,
                    lineHeight = 26.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "회원가입",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }

            Spacer(Modifier.height(32.dp))

            // Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = enterY.value.dp)
                    .alpha(1f - (enterY.value / 300f)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 경고/안내 배너
                    AnimatedVisibility(visible = warningText.isNotBlank()) {
                        InfoBanner(
                            bg = Color(0xFFFFEBEE),
                            fg = Color(0xFFD32F2F),
                            icon = Icons.Default.Warning,
                            text = warningText
                        )
                    }

                    // 이메일
                    SectionLabel("이메일")
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("example@domain.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !emailVerified,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        )
                    )
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val compact = maxWidth < 360.dp
                        val btnHeight = 52.dp
                        val gap = 8.dp
                        if (compact) {
                            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                PrimaryBtn(
                                    title = "인증 메일 보내기",
                                    onClick = onEmailVerifyClick,
                                    enabled = email.isNotBlank() && !emailVerified,
                                    height = btnHeight,
                                    color = primaryColor
                                )
                                OutlinedBtn(
                                    title = "인증 완료 확인",
                                    onClick = onCheckVerificationClick,
                                    enabled = email.isNotBlank() && !emailVerified && emailVerificationSent,
                                    height = btnHeight,
                                    leading = { Icon(Icons.Default.MarkEmailRead, null) }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(gap),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PrimaryBtn(
                                    title = "인증 메일 보내기",
                                    onClick = onEmailVerifyClick,
                                    enabled = email.isNotBlank() && !emailVerified,
                                    height = btnHeight,
                                    color = primaryColor,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedBtn(
                                    title = "인증 완료 확인",
                                    onClick = onCheckVerificationClick,
                                    enabled = email.isNotBlank() && !emailVerified && emailVerificationSent,
                                    height = btnHeight,
                                    leading = { Icon(Icons.Default.MarkEmailRead, null) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = emailVerificationSent || emailVerified) {
                        val bg = if (emailVerified) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                        val fg = if (emailVerified) Color(0xFF2E7D32) else Color(0xFFF57F17)
                        val icon = if (emailVerified) Icons.Default.CheckCircle else Icons.Default.MarkEmailRead
                        InfoBanner(
                            bg = bg, fg = fg, icon = icon,
                            text = if (emailVerified) "이메일 인증 완료"
                            else "인증 메일을 보냈어요. 메일의 링크를 눌러 인증을 완료해 주세요."
                        )
                    }

                    // 닉네임
                    SectionDivider()
                    SectionLabel("닉네임")
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = onNicknameChange,
                        label = { Text("닉네임") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        )
                    )
                    PrimaryBtn(
                        title = "닉네임 중복 확인",
                        onClick = onNicknameCheckClick,
                        height = 52.dp,
                        color = primaryColor
                    )
                    AnimatedVisibility(visible = nicknameValid) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "사용 가능한 닉네임입니다.",
                                color = Color(0xFF2E7D32),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 비밀번호
                    SectionDivider()
                    SectionLabel("비밀번호")
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("비밀번호") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        )
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("비밀번호 확인") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        )
                    )

                    // 완료 버튼
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            pressed = true
                            onSignUpClick()
                            scope.launch {
                                delay(140)
                                pressed = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
                    ) {
                        Text(
                            "회원가입 완료",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

/* ---------- Helpers ---------- */

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SectionDivider() {
    Divider(color = Color(0x14000000))
}

@Composable
private fun InfoBanner(
    bg: Color,
    fg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = fg)
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                color = fg,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PrimaryBtn(
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Text(title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OutlinedBtn(
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(6.dp))
        }
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
