package com.bcu.foodtable

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.*
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcu.foodtable.useful.ActivityTransition
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.UserManager
import com.airbnb.lottie.compose.*
import io.portone.sdk.android.PortOne
import io.portone.sdk.android.payment.*
import io.portone.sdk.android.type.*
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

class PurchaseConfirmActivity : ComponentActivity() {

    private lateinit var costStr: String
    private var cost: Long = 0

    private lateinit var paymentLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        costStr = intent.getStringExtra("price") ?: "0"
        cost = costStr.toLongOrNull() ?: 0L

        // 최소 금액 체크
        if (cost < 100L) {
            Toast.makeText(this, "최소 100원 이상 충전해 주세요.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        paymentLauncher = PortOne.registerForPaymentActivity(activity = this, object : PaymentCallback {
            override fun onSuccess(response: PaymentResponse.Success) {
                Log.d("결제성공", response.toString())
                setContent {
                    PurchaseTheme {
                        PurchaseCompleteScreen(
                            cost = cost,
                            onGoHome = {
                                ActivityTransition.startStatic(
                                    this@PurchaseConfirmActivity,
                                    HomeActivity::class.java
                                )
                                finish()
                            }
                        )
                    }
                }
            }

            override fun onFail(response: PaymentResponse.Fail) {
                Log.e("결제실패", response.toString())
                setContent {
                    PurchaseTheme {
                        PaymentFailScreen(onGoBack = { finish() })
                    }
                }
            }
        })

        // 결제 요청 실행
        PortOne.requestPayment(
            activity = this,
            request = PaymentRequest(
                storeId = "store-38616698-cf3e-4364-9073-494e2127e935",
                channelKey = "channel-key-14ab4c31-cba3-447a-9543-941396495fd9",
                paymentId = "babsang-${UUID.randomUUID()}",
                orderName = "밥상친구 소금 $cost 개",
                amount = Amount(total = cost, currency = Currency.KRW),
                method = PaymentMethod.Card()
            ),
            resultLauncher = paymentLauncher
        )
    }
}

@Composable
fun PurchaseCompleteScreen(
    cost: Long,
    onGoHome: () -> Unit,
    // onViewWallet: (() -> Unit)? = null // 필요하면 보조 버튼 추가
) {
    val cs = MaterialTheme.colorScheme
    val user = remember { UserManager.getUser()!! }

    // 애니메이션 상태
    var showTitle by remember { mutableStateOf(false) }
    var displayPoint by remember { mutableStateOf(user.point) }
    val animatedPoint by animateIntAsState(
        targetValue = displayPoint,
        animationSpec = tween(durationMillis = 1200),
        label = "AnimatedPoint"
    )

    val numberFmt = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    val addedG = cost.toInt() // 이번에 충전(획득)한 소금

    LaunchedEffect(Unit) {
        val updatedPoint = user.point + addedG
        displayPoint = updatedPoint
        // 로컬/원격 동기화
        user.point = updatedPoint
        UserManager.setUserByDatatype(user)
        updateFieldById("user", user.uid, "point", updatedPoint.toLong())

        // 타이틀 페이드인 타이밍
        delay(250)
        showTitle = true
    }

    // 화면
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // 영수증/완료 카드
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cs.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.15f)),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 성공 배지
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50))
                        .background(cs.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = cs.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // 타이틀
                Text(
                         text = "결제가 완료되었습니다!",
                         style = MaterialTheme.typography.headlineMedium,
                         color = MaterialTheme.colorScheme.primary,
                         fontWeight = FontWeight.Bold
                             )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "소금이 충전되었어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // Lottie (브랜드 톤 유지)
                LottieAnimationView(
                    asset = "coin_drop.json",
                    modifier = Modifier.size(160.dp)
                )

                Spacer(Modifier.height(12.dp))
                Divider(color = cs.outline.copy(alpha = 0.25f))
                Spacer(Modifier.height(12.dp))

                // 요약 블록: 이번에 추가된 소금 / 보유 소금
                SummaryRow(
                    label = "추가된 소금",
                    valueMain = "+${numberFmt.format(addedG)}",
                    valueUnit = " G",
                    accent = cs.primary
                )

                Spacer(Modifier.height(6.dp))

                SummaryRow(
                    label = "보유 소금",
                    valueMain = numberFmt.format(animatedPoint),
                    valueUnit = " G",
                    accent = cs.onSurface
                )

                Spacer(Modifier.height(18.dp))

                // 액션 버튼들
                Button(
                    onClick = onGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.primary,
                        contentColor = cs.onPrimary
                    )
                ) { Text("홈으로 돌아가기", style = MaterialTheme.typography.titleSmall) }

                // 보조 버튼이 필요하면 주석 해제
                /*
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onViewWallet?.invoke() },
                    enabled = onViewWallet != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = cs.primary
                    ),
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f))
                ) { Text("충전 내역 보기") }
                */
            }
        }
    }
}

/** 라벨 + 값(숫자 크게, 단위 작게) 일관형 요약 행 */
@Composable
private fun SummaryRow(
    label: String,
    valueMain: String,
    valueUnit: String,
    accent: Color
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        // 숫자 크게 + 단위 작게
        Text(
            text = buildAnnotatedString {
                append(valueMain)
                withStyle(SpanStyle(fontSize = 14.sp, color = cs.onSurfaceVariant)) {
                    append(valueUnit)
                }
            },
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
            color = accent,
            textAlign = TextAlign.End,
            maxLines = 1
        )
    }
}


@Composable
fun PaymentFailScreen(onGoBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("결제에 실패했습니다 ❌", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onGoBack) {
            Text("돌아가기")
        }
    }
}

@Composable
fun LottieAnimationView(asset: String, modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(asset))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}
private val WarmLightColorScheme = lightColorScheme(
    primary = Color(0xFFE25532),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2D6),
    onPrimaryContainer = Color(0xFF5C2B1B),
    secondary = Color(0xFFFFF4ED),
    onSecondary = Color(0xFF4B3C35),
    secondaryContainer = Color(0xFFFDE1D5),
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = Color(0xFFB9806D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E0DC),
    onTertiaryContainer = Color(0xFF4E342E),
    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF3A2C28),
    surface = Color.White,
    onSurface = Color(0xFF2E2E2E),
    surfaceVariant = Color(0xFFFBE7DF),
    onSurfaceVariant = Color(0xFF5F5F5F),
    outline = Color(0xFFDDC7BD),
    outlineVariant = Color(0xFFF0E0D8),
    inverseSurface = Color(0xFF3A2C28),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFFF8F6B),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF8B0000)
)

@Composable
private fun PurchaseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WarmLightColorScheme,
        // 필요하면 typography/shapes도 넣을 수 있음
        content = content
    )
}