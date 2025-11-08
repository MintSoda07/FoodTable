@file:OptIn(ExperimentalMaterial3Api::class)

package com.bcu.foodtable.JetpackCompose.Social // 사용자의 패키지 경로에 맞게 수정해주세요

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino // 룰렛 아이콘으로 변경
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Lottie 관련 import 제거
// import com.airbnb.lottie.compose.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.navigation.NavController
import androidx.wear.compose.material.LocalContentAlpha
// androidx.wear.compose.material.ContentAlpha 사용 대신 LocalContentAlpha.current 또는 직접 alpha 값 지정
import com.bcu.foodtable.R // R 클래스 import 경로 확인

// LottieAnimationView 정의 제거

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
    val localContentAlpha = LocalContentAlpha.current // M3에서 권장하는 방식

    val animatedGradientOffset by animateFloatAsState(
        targetValue = if (isSpinning) 1f else 0f,
        animationSpec = tween(3000, easing = LinearEasing),
        label = "gradient_animation"
    )

    val borderAnimation by animateFloatAsState(
        targetValue = if (isSpinning) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_animation"
    )

    val canvasSize = 260.dp
    val buttonHeight = 52.dp
    val buttonShape = RoundedCornerShape(12.dp)

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            colorScheme.primaryContainer.copy(alpha = 0.3f),
            colorScheme.secondaryContainer.copy(alpha = 0.3f),
            colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        startY = animatedGradientOffset * 300f // 이 부분은 Float 연산이므로 문제가 없을 것입니다.
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface.copy(alpha = 0.9f)
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
                            color = colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "맛있는 선택의 순간",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface.copy(alpha = 0.9f)
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
                                focusedBorderColor = colorScheme.primary,
                                focusedLabelColor = colorScheme.primary,
                                unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha=0.3f),
                                focusedContainerColor = colorScheme.surfaceVariant.copy(alpha=0.5f),
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
                            containerColor = if (!isSpinning) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.12f),
                            contentColor = if(!isSpinning) colorScheme.onPrimary else colorScheme.onSurface.copy(alpha = localContentAlpha),
                            modifier = Modifier.size(56.dp),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = if(!isSpinning) 6.dp else 0.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "추가"
                            )
                        }
                    }

                    if (foodItems.isNotEmpty() && !isSpinning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "💡 추가한 음식을 터치하면 제거됩니다",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    if (!isSpinning && foodItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(foodItems) { index, item ->
                                val isResultItem = index == resultIndex && showResult
                                AssistChip(
                                    onClick = {
                                        foodItems = foodItems.toMutableList().apply { removeAt(index) }
                                        if (index == resultIndex) resultIndex = -1
                                    },
                                    label = { Text(item, fontWeight = FontWeight.Medium) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isResultItem) colorScheme.tertiaryContainer else colorScheme.secondaryContainer,
                                        labelColor = if (isResultItem) colorScheme.onTertiaryContainer else colorScheme.onSecondaryContainer
                                    ),
                                    // 수정된 부분: AssistChip의 border를 BorderStroke로 명시적으로 지정
                                    border = BorderStroke(
                                        width = 1.dp, // 기본 두께 또는 AssistChipDefaults.BorderThickness 사용
                                        color = if(isResultItem) colorScheme.tertiary else colorScheme.outline
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.size(canvasSize + 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(canvasSize)
                            .clip(CircleShape)
                            .graphicsLayer { rotationZ = rotation.value % 360f }
                    ) {
                        if (foodItems.isNotEmpty()) {
                            val sweep = 360f / foodItems.size
                            val radius = size.width / 2

                            foodItems.forEachIndexed { index, item ->
                                val baseHue = (index * 360f / foodItems.size).mod(360f)
                                val sectionColor = Color.hsv(baseHue, 0.75f, 0.95f)
                                val shadowColor = Color.hsv(baseHue, 0.8f, 0.75f)

                                drawArc(
                                    color = shadowColor.copy(alpha = 0.3f),
                                    startAngle = sweep * index,
                                    sweepAngle = sweep,
                                    useCenter = true,
                                    topLeft = Offset(3f, 3f),
                                    size = size.copy(width = size.width - 6f, height = size.height - 6f)
                                )
                                drawArc(
                                    color = sectionColor,
                                    startAngle = sweep * index,
                                    sweepAngle = sweep,
                                    useCenter = true
                                )
                            }

                            foodItems.forEachIndexed { index, item ->
                                val angleRad = Math.toRadians((sweep * index + sweep / 2 - 90).toDouble())
                                val textRadius = radius * 0.6f

                                drawContext.canvas.nativeCanvas.save()
                                drawContext.canvas.nativeCanvas.rotate( (sweep * index + sweep / 2), center.x, center.y)
                                drawContext.canvas.nativeCanvas.drawText(
                                    item,
                                    center.x + textRadius, // .toFloat() 불필요, textRadius가 이미 Float
                                    center.y + 8.sp.toPx()/2,
                                    android.graphics.Paint().apply {
                                        textSize = 15.sp.toPx()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        color = android.graphics.Color.WHITE
                                        isFakeBoldText = true
                                        setShadowLayer(5f, 1f, 1f, android.graphics.Color.argb(128,0,0,0))
                                    }
                                )
                                drawContext.canvas.nativeCanvas.restore()
                            }

                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        colorScheme.primary,
                                        colorScheme.secondary,
                                        colorScheme.tertiary,
                                        colorScheme.primary
                                    )
                                ),
                                radius = radius - 2.dp.toPx(),
                                style = Stroke(
                                    width = (4.dp + (3.dp * borderAnimation)).toPx(), // borderAnimation(Float) * Dp -> Dp
                                    cap = StrokeCap.Round
                                )
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(colorScheme.primary, colorScheme.primaryContainer)
                                ),
                                radius = 20.dp.toPx(),
                                center = center
                            )
                        } else {
                            val radius = size.width / 2
                            drawCircle(color = colorScheme.surfaceVariant, radius = radius)
                            drawCircle(color = colorScheme.outline, radius = radius, style = Stroke(width = 2.dp.toPx()))
                            drawContext.canvas.nativeCanvas.drawText(
                                "음식을 추가해주세요", center.x, center.y + 8.sp.toPx()/2,
                                android.graphics.Paint().apply {
                                    textSize = 16.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER
                                    color = colorScheme.onSurfaceVariant.toArgb()
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-10).dp)
                            .size(width = 24.dp, height = 30.dp)
                            .shadow(6.dp, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(colorScheme.errorContainer, colorScheme.error)
                                ),
                                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text("▼", fontSize = 18.sp, color = colorScheme.onError, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top=2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isSpinning) 8.dp + 80.dp + 8.dp else 16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { if (!isSpinning) navController.popBackStack() },
                    modifier = Modifier.weight(1f).height(buttonHeight),
                    shape = buttonShape,
                    enabled = !isSpinning,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colorScheme.primary,
                        disabledContentColor = colorScheme.onSurface.copy(alpha = localContentAlpha)
                    ),
                    border = BorderStroke(1.dp, if(!isSpinning) colorScheme.primary else colorScheme.onSurface.copy(alpha = localContentAlpha))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("뒤로가기", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (!isSpinning && foodItems.size >= 2) {
                            val spins = Random.nextInt(4, 7)
                            val anglePerItem = 360f / foodItems.size
                            val randomSelectedIndex = Random.nextInt(foodItems.size)
                            selectedFood = foodItems[randomSelectedIndex]
                            resultIndex = randomSelectedIndex

                            val targetAngle = (360f * spins) + (270f - (randomSelectedIndex * anglePerItem) - (anglePerItem / 2f))

                            isSpinning = true
                            scope.launch {
                                rotation.snapTo(rotation.value % 360f)
                                rotation.animateTo(
                                    targetAngle,
                                    animationSpec = tween(durationMillis = 4000, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f))
                                )
                                spinCount++
                                showResult = true
                                isSpinning = false
                            }
                        } else if (foodItems.size < 2) {
                            // 예: Toast.makeText(LocalContext.current, "음식을 2개 이상 추가해주세요.", Toast.SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(2f).height(buttonHeight),
                    shape = buttonShape,
                    enabled = !isSpinning && foodItems.size >= 2,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = colorScheme.onSurface.copy(alpha = 0.05f)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = if (!isSpinning && foodItems.size >= 2) {
                                    Brush.horizontalGradient(
                                        colors = listOf(colorScheme.primary, colorScheme.secondary, colorScheme.tertiary)
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(colorScheme.onSurface.copy(alpha = 0.12f), colorScheme.onSurface.copy(alpha = 0.1f))
                                    )
                                },
                                shape = buttonShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Casino,
                                contentDescription = null,
                                tint = if(!isSpinning && foodItems.size >=2) Color.White else colorScheme.onSurface.copy(alpha = localContentAlpha),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpinning) "돌리는 중..." else "룰렛 돌리기!",
                                color = if(!isSpinning && foodItems.size >=2) Color.White else colorScheme.onSurface.copy(alpha = localContentAlpha),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showResult) {
        AlertDialog(
            onDismissRequest = { showResult = false },
            confirmButton = {
                Button(
                    onClick = { showResult = false },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) { Text("확인", color = colorScheme.onPrimary) }
            },
            title = {
                Text(
                    text = "🎉 축하합니다!",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = colorScheme.primary)
                )
            },
            text = {
                Text(
                    text = if (spinCount >= 3 && foodItems.contains(selectedFood))
                        "세 번이나 돌렸네요!\n✨ $selectedFood ✨\n드시는 건 어떠세요?"
                    else if (foodItems.contains(selectedFood))
                        "오늘의 메뉴는\n✨ $selectedFood ✨\n입니다!"
                    else "룰렛 결과가 이상해요. 다시 시도해주세요.",
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
