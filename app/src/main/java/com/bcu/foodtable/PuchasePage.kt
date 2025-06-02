// 🌟 Ultra Glamorous Purchase Page (Compose + Lottie + 배경 + 컬러 강조 + 텍스트 효과 포함)

package com.bcu.foodtable

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
            MaterialTheme(colorScheme = lightColorScheme()) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // ✅ 상단/하단 바 대응
            .background(MaterialTheme.colorScheme.surface) // ✅ 깔끔한 배경
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "소금 충전소",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp)
            )

            Text(
                text = "₩ ${formatter.format(animatedMoney)}",
                style = TextStyle(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            if (moneyValue >= 5000) {
                Text(
                    text = "보너스 +${moneyValue / 10} 소금 예정!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(5000, 10000, 50000, 100000).forEach { amount ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { moneyValue = maxOf(0, moneyValue - amount) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            }

                            Text(
                                text = "₩ ${formatter.format(amount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { moneyValue += amount },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("결제 진행하기", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
