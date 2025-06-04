package com.bcu.foodtable.JetpackCompose.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.navigation.NavController

@SuppressLint("MutableCollectionMutableState")
@Composable
fun RouletteGameScreen(navController: NavController) {
    var foodItems by remember { mutableStateOf(mutableListOf<String>()) }
    var currentInput by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var spinCount by remember { mutableStateOf(0) }
    var resultIndex by remember { mutableStateOf(-1) }
    var isSpinning by remember { mutableStateOf(false) } // ⭐ 룰렛 회전 상태

    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    val lottieSpec = rememberLottieComposition(LottieCompositionSpec.Asset("spin_boost.json"))
    val lottieAnimState = animateLottieCompositionAsState(
        composition = lottieSpec.value,
        isPlaying = isSpinning, // ⭐ 스핀 중일 때만 재생
        speed = 1.5f,
        restartOnPlay = true
    )

    val canvasSize = 280.dp

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("🎡 오늘의 메뉴 룰렛", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = currentInput,
                onValueChange = { currentInput = it },
                label = { Text("음식 입력") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isSpinning
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (currentInput.isNotBlank()) {
                    foodItems = foodItems.toMutableList().apply { add(currentInput.trim()) }
                    currentInput = ""
                }
            }, enabled = !isSpinning) { Text("추가") }
        }

        Spacer(Modifier.height(8.dp))
        if (foodItems.isNotEmpty() && !isSpinning) {
            Text("💡 추가한 음식은 터치하면 제거할 수 있어요.", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
        }

        Spacer(Modifier.height(8.dp))

        // ⭐ 스핀 중일 때는 Chip을 숨김
        if (!isSpinning) {
            LazyRow {
                itemsIndexed(foodItems) { index, item ->
                    AssistChip(
                        onClick = {
                            foodItems = foodItems.toMutableList().apply { removeAt(index) }
                            if (index == resultIndex) resultIndex = -1
                        },
                        label = { Text(item) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (index == resultIndex) Color(0xFFFFE082) else colorScheme.secondaryContainer,
                            labelColor = colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(320.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(canvasSize)
                    .clip(CircleShape)
                    .shadow(12.dp, CircleShape)
                    .background(Color.White)
                    .graphicsLayer { rotationZ = rotation.value % 360f }
            ) {
                if (foodItems.isNotEmpty()) {
                    val sweep = 360f / foodItems.size
                    val r = size.width / 2

                    foodItems.forEachIndexed { index, item ->
                        drawArc(
                            color = Color.hsv((index * 360f / foodItems.size) % 360, 0.6f, 1f),
                            startAngle = sweep * index,
                            sweepAngle = sweep,
                            useCenter = true
                        )
                    }

                    foodItems.forEachIndexed { index, item ->
                        val angle = Math.toRadians((sweep * index + sweep / 2 - 90).toDouble())
                        val x = center.x + cos(angle) * r * 0.65
                        val y = center.y + sin(angle) * r * 0.65
                        drawContext.canvas.nativeCanvas.drawText(
                            item,
                            x.toFloat(),
                            y.toFloat(),
                            android.graphics.Paint().apply {
                                textSize = 24f
                                textAlign = android.graphics.Paint.Align.CENTER
                                color = android.graphics.Color.BLACK
                                isFakeBoldText = true
                            }
                        )
                    }

                    drawCircle(
                        color = Color.Red,
                        radius = r - 4.dp.toPx(),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(color = colorScheme.primary, radius = 14.dp.toPx(), center = center)
                }
            }

            Box(
                Modifier
                    .offset(y = (-160).dp)
                    .size(24.dp)
                    .background(colorScheme.primary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("▼", fontSize = 18.sp, color = Color.White)
            }
        }

        if (lottieAnimState.isPlaying) {
            LottieAnimation(
                composition = lottieSpec.value,
                progress = lottieAnimState.progress,
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val spins = Random.nextInt(4, 6)
                val anglePerItem = 360f / foodItems.size

                val selectedIndex = Random.nextInt(foodItems.size)
                selectedFood = foodItems[selectedIndex]
                resultIndex = selectedIndex

                val targetAngle = 360f * spins + (360f - (selectedIndex * anglePerItem) - anglePerItem / 2)

                isSpinning = true
                scope.launch {
                    rotation.snapTo(rotation.value % 360f)
                    rotation.animateTo(
                        targetAngle,
                        animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing)
                    )
                    spinCount++
                    showResult = true
                    isSpinning = false
                }
            },
            enabled = !isSpinning && foodItems.size >= 2,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("룰렛 돌리기!", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))

        Button(onClick = { navController.popBackStack() }, enabled = !isSpinning) {
            Text("뒤로가기")
        }

        if (showResult) {
            AlertDialog(
                onDismissRequest = { showResult = false },
                confirmButton = {
                    Button(onClick = { showResult = false }) {
                        Text("확인")
                    }
                },
                title = { Text("🎉 축하합니다!") },
                text = {
                    Text(
                        if (spinCount >= 3)
                            "세 번이나 돌렸네요!\n✨ $selectedFood ✨ 드시는 건 어떠세요?"
                        else
                            "오늘의 메뉴는\n✨ $selectedFood ✨ 입니다!"
                    )
                }
            )
        }
    }
}
