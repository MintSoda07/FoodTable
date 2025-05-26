import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email // 변경: Person 대신 Email 아이콘
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning // 기존 아이콘 유지
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight // 추가
import androidx.compose.ui.text.input.ImeAction // 추가
import androidx.compose.ui.text.input.KeyboardType // 추가
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcu.foodtable.R // 사용자님의 리소스 경로

@OptIn(ExperimentalMaterial3Api::class) // CenterAlignedTopAppBar 등에 필요
@Composable
fun LoginScreenImproved( // 함수 이름 변경
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    warningText: String,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit, // 추가: 비밀번호 찾기 클릭
    onGoogleLoginClick: () -> Unit, // 추가: 구글 로그인 클릭
    onKakaoLoginClick: () -> Unit // 추가: 카카오 로그인 클릭 (예시)
) {
    val primaryColor = Color(0xFFE76F51) // 주황색 계열 (메인)
    val backgroundColorStart = Color(0xFFFFF7F0) // 매우 연한 오렌지/크림색 (배경 시작)
    val backgroundColorEnd = Color(0xFFFFF1E6)   // 연한 오렌지/크림색 (배경 끝)
    val cardBackgroundColor = Color(0xFAFFFFFF) // 반투명 흰색 (카드 배경, 약간 더 불투명하게)
    val textPrimaryColor = Color(0xFF333333) // 어두운 회색 (본문 텍스트)
    val textSecondaryColor = Color.Gray // 회색 (보조 텍스트)
    val errorColor = Color(0xFFD32F2F) // 에러 메시지 색상 (기존 Red보다 약간 톤다운)

    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current // 키보드 숨김 처리용

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundColorStart, backgroundColorEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()) // 스크롤 가능하도록 추가
                .imePadding(), // 키보드가 올라올 때 UI 가려짐 방지
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // 수직 중앙 정렬
        ) {
            // 앱 로고 (기존과 유사, 크기 및 간격 조정)
            Image(
                painter = painterResource(id = R.drawable.dish_icon), // 사용자 리소스
                contentDescription = stringResource(id = R.string.app_name) + " 로고",
                modifier = Modifier.size(72.dp),
                colorFilter = ColorFilter.tint(primaryColor)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 앱 이름
            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold, // 굵게
                color = primaryColor
            )

            // 서브 타이틀
            Text(
                text = "따뜻한 한끼, 함께 나눠요",
                fontSize = 14.sp,
                color = textSecondaryColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 로그인 카드
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp), // 모서리 곡률 증가
                color = cardBackgroundColor,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp) // 패딩 조정
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 경고 메시지 (기존과 유사, 색상 및 아이콘 조정)
                    AnimatedVisibility(visible = warningText.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp), // 간격 조정
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

                    // 이메일 입력
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("이메일") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email, // 아이콘 변경
                                contentDescription = "이메일 아이콘"
                            )
                        },
                        shape = RoundedCornerShape(16.dp), // 모서리 곡률 조정
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 비밀번호 입력
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("비밀번호") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "비밀번호 아이콘"
                            )
                        },
                        trailingIcon = { // 비밀번호 보기/숨기기 토글 아이콘
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기"
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp), // 모서리 곡률 조정
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done // 완료 시 키보드 닫기
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus() // 키보드 숨기기
                                onLoginClick() // 로그인 시도
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 비밀번호 찾기 버튼 (오른쪽 정렬)
                    TextButton(
                        onClick = onForgotPasswordClick,
                        modifier = Modifier.align(Alignment.End) // 오른쪽 정렬
                    ) {
                        Text(
                            text = "비밀번호를 잊으셨나요?",
                            color = textSecondaryColor,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 로그인 버튼
                    Button(
                        onClick = {
                            focusManager.clearFocus() // 키보드 숨기기
                            onLoginClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp), // 높이 증가
                        shape = RoundedCornerShape(18.dp), // 모서리 곡률 조정
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text("로그인", fontSize = 18.sp, fontWeight = FontWeight.Bold) // 폰트 크기 및 굵기 조정
                    }

                    Spacer(modifier = Modifier.height(24.dp)) // 간격 추가

                    // 소셜 로그인 영역
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                        Text(
                            text = "또는",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            fontSize = 13.sp,
                            color = textSecondaryColor
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally) // 버튼 간 간격 및 중앙 정렬
                    ) {
                        // 구글 로그인 버튼 (예시)
                        OutlinedButton(
                            onClick = onGoogleLoginClick,
                            shape = CircleShape, // 원형 버튼
                            modifier = Modifier.size(52.dp), // 크기 고정
                            contentPadding = PaddingValues(0.dp), // 내부 패딩 제거
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Text(
                                text = "G",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4) // 구글 브랜드 색상 중 파란색 (선택 사항)
                            )
                        }

                        // 카카오 로그인 버튼 (예시) - 실제 카카오 SDK 가이드라인 준수 필요
                        OutlinedButton(
                            onClick = onKakaoLoginClick,
                            shape = CircleShape,
                            modifier = Modifier.size(52.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = BorderStroke(1.dp, Color(0xFFFEE500)) // 카카오 노란색 테두리 (예시)
                        ) {
                            Text(
                                text = "K",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3C1E1E) // 카카오 로고 텍스트 색상과 유사하게 (선택 사항)
                            )
                        }
                        // TODO: 필요시 다른 소셜 로그인 버튼 추가 (예: 네이버, Apple)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp)) // 카드와 하단 텍스트 버튼 사이 간격

            // 회원가입 버튼
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("아직 계정이 없으신가요?", fontSize = 14.sp, color = textSecondaryColor)
                TextButton(onClick = onSignUpClick) {
                    Text(
                        text = "회원가입",
                        color = primaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp)) // 하단 여백
        }
    }
}