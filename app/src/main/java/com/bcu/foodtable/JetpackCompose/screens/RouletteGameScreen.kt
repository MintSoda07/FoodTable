package com.bcu.foodtable.JetpackCompose.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
    var isSpinning by remember { mutableStateOf(false) }

    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    // 배경 그라데이션 애니메이션
    val animatedGradientOffset by animateFloatAsState(
        targetValue = if (isSpinning) 1f else 0f,
        animationSpec = tween(3000, easing = LinearEasing),
        label = "gradient_animation"
    )

    // 룰렛 테두리 애니메이션
    val borderAnimation by animateFloatAsState(
        targetValue = if (isSpinning) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_animation"
    )

    val lottieSpec = rememberLottieComposition(LottieCompositionSpec.Asset("spin_boost.json"))
    val lottieAnimState = animateLottieCompositionAsState(
        composition = lottieSpec.value,
        isPlaying = isSpinning,
        speed = 1.5f,
        restartOnPlay = true
    )

    val canvasSize = 260.dp

    // 메인 배경 그라데이션
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF6366F1).copy(alpha = 0.1f),
            Color(0xFF8B5CF6).copy(alpha = 0.1f),
            Color(0xFFEC4899).copy(alpha = 0.1f)
        ),
        startY = animatedGradientOffset * 300f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 헤더
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎡 오늘의 메뉴 룰렛",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "맛있는 선택의 순간",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 음식 입력 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentInput,
                            onValueChange = { currentInput = it },
                            label = { Text("음식 메뉴를 입력하세요") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !isSpinning,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                focusedLabelColor = Color(0xFF6366F1)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        FloatingActionButton(
                            onClick = {
                                if (currentInput.isNotBlank()) {
                                    foodItems = foodItems.toMutableList().apply { add(currentInput.trim()) }
                                    currentInput = ""
                                }
                            },
                            containerColor = if (!isSpinning) Color(0xFF6366F1) else Color.Gray,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "추가",
                                tint = Color.White
                            )
                        }
                    }

                    if (foodItems.isNotEmpty() && !isSpinning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "💡 추가한 음식을 터치하면 제거됩니다",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B7280)
                            )
                        )
                    }

                    // 음식 칩들
                    if (!isSpinning && foodItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(foodItems) { index, item ->
                                AssistChip(
                                    onClick = {
                                        foodItems = foodItems.toMutableList().apply { removeAt(index) }
                                        if (index == resultIndex) resultIndex = -1
                                    },
                                    label = { Text(item, fontWeight = FontWeight.Medium) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (index == resultIndex) Color(0xFFFFE082) else Color(0xFFF1F5F9),
                                        labelColor = if (index == resultIndex) Color(0xFF92400E) else Color(0xFF1F2937)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 룰렛 컨테이너
            Card(
                modifier = Modifier.size(280.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // 룰렛 캔버스
                    Canvas(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(CircleShape)
                            .graphicsLayer { rotationZ = rotation.value % 360f }
                    ) {
                        if (foodItems.isNotEmpty()) {
                            val sweep = 360f / foodItems.size
                            val radius = size.width / 2

                            // 룰렛 섹션 그리기
                            foodItems.forEachIndexed { index, item ->
                                val baseHue = (index * 360f / foodItems.size) % 360
                                val sectionColor = Color.hsv(baseHue, 0.7f, 0.9f)
                                val shadowColor = Color.hsv(baseHue, 0.8f, 0.7f)

                                // 그림자 효과
                                drawArc(
                                    color = shadowColor,
                                    startAngle = sweep * index,
                                    sweepAngle = sweep,
                                    useCenter = true,
                                    topLeft = Offset(2f, 2f),
                                    size = size.copy(width = size.width - 4f, height = size.height - 4f)
                                )

                                // 메인 섹션
                                drawArc(
                                    color = sectionColor,
                                    startAngle = sweep * index,
                                    sweepAngle = sweep,
                                    useCenter = true
                                )
                            }

                            // 텍스트 그리기
                            foodItems.forEachIndexed { index, item ->
                                val angle = Math.toRadians((sweep * index + sweep / 2 - 90).toDouble())
                                val x = center.x + cos(angle) * radius * 0.65
                                val y = center.y + sin(angle) * radius * 0.65

                                drawContext.canvas.nativeCanvas.drawText(
                                    item,
                                    x.toFloat(),
                                    y.toFloat(),
                                    android.graphics.Paint().apply {
                                        textSize = 28f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        color = android.graphics.Color.WHITE
                                        isFakeBoldText = true
                                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                                    }
                                )
                            }

                            // 애니메이션 테두리
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6),
                                        Color(0xFFEC4899),
                                        Color(0xFF6366F1)
                                    )
                                ),
                                radius = radius - 4.dp.toPx(),
                                style = Stroke(
                                    width = (6 + borderAnimation * 4).dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )

                            // 중앙 원
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                                ),
                                radius = 20.dp.toPx(),
                                center = center
                            )
                        } else {
                            // 빈 룰렛 상태
                            val radius = size.width / 2
                            drawCircle(
                                color = Color(0xFFF8FAFC),
                                radius = radius
                            )
                            drawCircle(
                                color = Color(0xFFE2E8F0),
                                radius = radius,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                "음식을 추가해주세요",
                                center.x,
                                center.y,
                                android.graphics.Paint().apply {
                                    textSize = 24f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    color = android.graphics.Color.GRAY
                                }
                            )
                        }
                    }

                    // 포인터
                    Box(
                        modifier = Modifier
                            .offset(y = (-140.dp))
                            .size(32.dp)
                            .shadow(8.dp, CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFF6B6B), Color(0xFFE63946))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "▼",
                            fontSize = 20.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 로티 애니메이션
            if (lottieAnimState.isPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                LottieAnimation(
                    composition = lottieSpec.value,
                    progress = lottieAnimState.progress,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 버튼들
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 뒤로가기 버튼
                OutlinedButton(
                    onClick = {
                        if (!isSpinning) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (!isSpinning) Color(0xFF6366F1) else Color.Gray
                    )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("뒤로가기", fontWeight = FontWeight.SemiBold)
                }

                // 룰렛 돌리기 버튼
                Button(
                    onClick = {
                        if (!isSpinning && foodItems.size >= 2) {
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
                        }
                    },
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = if (!isSpinning && foodItems.size >= 2) {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF6366F1),
                                            Color(0xFF8B5CF6),
                                            Color(0xFFEC4899)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Gray, Color.Gray)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Casino,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpinning) "돌리는 중..." else "룰렛 돌리기!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // 결과 다이얼로그
    if (showResult) {
        AlertDialog(
            onDismissRequest = { showResult = false },
            confirmButton = {
                Button(
                    onClick = { showResult = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    )
                ) {
                    Text("확인", color = Color.White)
                }
            },
            title = {
                Text(
                    text = "🎉 축하합니다!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                )
            },
            text = {
                Text(
                    text = if (spinCount >= 3)
                        "세 번이나 돌렸네요!\n✨ $selectedFood ✨\n드시는 건 어떠세요?"
                    else
                        "오늘의 메뉴는\n✨ $selectedFood ✨\n입니다!",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp
                    )
                )
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}