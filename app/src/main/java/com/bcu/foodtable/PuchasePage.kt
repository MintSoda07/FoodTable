package com.bcu.foodtable

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.*

class PuchasePage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PurchasePageScreen()
                }
            }
        }
    }
}

@Composable
fun PurchasePageScreen(
    context: Context = LocalContext.current
) {
    var moneyValue by remember { mutableStateOf(0) }
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
    val formattedValue = formatter.format(moneyValue)

    // ✅ 화면 전환용 상태
    var paymentComplete by remember { mutableStateOf(false) }

    if (paymentComplete) {
        // ✅ 결제 완료 화면으로 전환
        PurchaseCompleteScreen(cost = moneyValue.toLong()) {
            (context as? ComponentActivity)?.finish()
        }
        return
    }

    // 💳 기존 결제 화면
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primary.copy(alpha = 0.05f))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "금액 선택",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = primary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "₩ $formattedValue",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = onPrimary
                    )
                }
            }

            val buttonPairs = listOf(
                5000 to "5,000원",
                10000 to "10,000원",
                50000 to "50,000원",
                100000 to "100,000원"
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                buttonPairs.forEach { (amount, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ElevatedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                moneyValue = (moneyValue - amount).coerceAtLeast(0)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("- $label")
                        }

                        ElevatedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                moneyValue += amount
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ $label")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ✅ 결제 버튼 → Compose 전환
            AnimatedVisibility(
                visible = moneyValue > 0,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 })
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        paymentComplete = true // 화면 전환 트리거
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 10.dp,
                        pressedElevation = 4.dp
                    ),
                    colors = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Text(
                        text = "₩ ${formatter.format(moneyValue)} 결제하기",
                        color = onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

