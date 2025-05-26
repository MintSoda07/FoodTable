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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                    MaterialTheme {
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
                    MaterialTheme {
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
fun PurchaseCompleteScreen(cost: Long, onGoHome: () -> Unit) {
    val user = remember { UserManager.getUser()!! }
    var showAnimation by remember { mutableStateOf(false) }
    var displayPoint by remember { mutableStateOf(user.point) }
    val animatedPoint by animateIntAsState(
        targetValue = displayPoint,
        animationSpec = tween(durationMillis = 1200),
        label = "AnimatedPoint"
    )

    LaunchedEffect(Unit) {
        val updatedPoint = user.point + cost.toInt()
        displayPoint = updatedPoint
        user.point = updatedPoint
        UserManager.setUserByDatatype(user)
        updateFieldById("user", user.uid, "point", updatedPoint.toLong())
        delay(400)
        showAnimation = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = showAnimation,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -30 })
        ) {
            Text(
                text = "결제가 완료되었습니다!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        LottieAnimationView(asset = "coin_drop.json", modifier = Modifier.size(180.dp))

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "보유 소금: ₩ ${NumberFormat.getNumberInstance(Locale.KOREA).format(animatedPoint)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Text(
                text = "홈으로 돌아가기",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
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
