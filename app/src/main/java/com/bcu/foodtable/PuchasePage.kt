// 💎 Jetpack Compose 기반 화려한 결제 페이지
// ⚠️ coin_idle.json / bonus_shine.json Lottie 파일 필요

package com.bcu.foodtable

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import java.text.NumberFormat
import java.util.*

class PuchasePage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PurchaseScreen()
                }
            }
        }
    }
}

@Composable
fun PurchaseScreen() {
    val context = LocalContext.current
    var moneyValue by remember { mutableStateOf(0) }
    val animatedMoney by animateIntAsState(
        targetValue = moneyValue,
        animationSpec = spring(dampingRatio = 0.7f), label = "animatedMoney"
    )
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)

    val lottieAsset = if (moneyValue >= 5000) "bonus_shine.json" else "coin_idle.json"
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(lottieAsset))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "충전할 소금",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(220.dp)
        )

        Text(
            text = "₩ ${formatter.format(animatedMoney)}",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF388E3C)
        )

        if (moneyValue >= 5000) {
            Text(
                text = "🎁 보너스 소금 +${moneyValue / 10} 예정!",
                fontSize = 16.sp,
                color = Color(0xFFEF6C00),
                fontWeight = FontWeight.Medium
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(5000, 10000, 50000, 100000).forEach { amount ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { moneyValue = maxOf(0, moneyValue - amount) },
                        modifier = Modifier.size(56.dp).background(Color(0xFFFBE9E7), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "감소", tint = Color.Red)
                    }
                    Text(
                        text = "₩ ${formatter.format(amount)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    IconButton(
                        onClick = { moneyValue += amount },
                        modifier = Modifier.size(56.dp).background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "증가", tint = Color(0xFF43A047))
                    }
                }
            }
        }

        Button(
            onClick = {
                if (moneyValue < 100) {
                    Toast.makeText(context, "최소 100원 이상 충전해주세요", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(context, PurchaseConfirmActivity::class.java)
                    intent.putExtra("price", moneyValue.toString())
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("결제 진행하기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}