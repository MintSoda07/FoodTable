package com.bcu.foodtable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.useful.ActivityTransition
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.UserManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class PurchaseConfirmActivity : AppCompatActivity() {
    // 결제 완료/실패 이후 응답을 처리 하기 위한 ResultLauncher 생성
    private val paymentActivityResultLauncher =
        PortOne.registerForPaymentActivity(this, callback = object :
            PaymentCallback {
            override fun onSuccess(response: PaymentResponse.Success) {
                AlertDialog.Builder(this@PurchaseConfirmActivity)
                    .setTitle("결제가 완료되었습니다!")
                    .setMessage(response.toString())
                    .show()
                val user = UserManager.getUser()!!
                val costStr = intent.getStringExtra("price")
                val cost: Long = costStr?.toLongOrNull() ?: 0L
                val point = user.point + cost
                CoroutineScope(Dispatchers.IO).launch {
                    updateFieldById(
                        collectionPath = "user",
                        documentId = user.uid,
                        fieldName = "point",
                        newValue = point
                    )
                }
                user.point = point.toInt()
                UserManager.setUser(
                    name = user.name,
                    email = user.email,
                    imageURL = user.image,
                    phoneNumber = user.phoneNumber,
                    point = user.point,
                    uid = user.uid,
                    rankPoint = user.rankPoint,
                    description = user.description
                )
                ActivityTransition.startStatic(this@PurchaseConfirmActivity, HomeActivity::class.java)
            }
                override fun onFail(response: PaymentResponse.Fail) {
                    AlertDialog.Builder(this@PurchaseConfirmActivity)
                        .setTitle("결제 실패")
                        .setMessage(response.toString())
                        .show()
                }

            })
class PurchaseConfirmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val costStr = intent.getStringExtra("price")
        val cost = costStr?.toLongOrNull() ?: 0L

        setContent {
            MaterialTheme {
                Surface {
                    PurchaseCompleteScreen(cost = cost) {
                        ActivityTransition.startStatic(this, HomeAcitivity::class.java)
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseCompleteScreen(
    cost: Long,
    onGoHome: () -> Unit
) {
    val context = LocalContext.current
    val user = remember { UserManager.getUser()!! }

    var showAnimation by remember { mutableStateOf(false) }
    var displayPoint by remember { mutableStateOf(user.point) }

    // 애니메이션 숫자 증가
    val animatedPoint by animateIntAsState(
        targetValue = displayPoint,
        animationSpec = tween(durationMillis = 1200),
        label = "AnimatedPoint"
    )

    // 코루틴으로 포인트 충전 및 애니메이션 표시
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

        // 동전 드롭 애니메이션 (Lottie)
        LottieAnimationView(asset = "coin_drop.json", modifier = Modifier.size(180.dp))

        Spacer(modifier = Modifier.height(32.dp))

        // 소금 포인트 표시
        Text(
            text = "보유 소금: ₩ ${NumberFormat.getNumberInstance(Locale.KOREA).format(animatedPoint)}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 홈으로 버튼
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
