package com.bcu.foodtable.JetpackCompose.Social

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.bcu.foodtable.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CardGameScreen(navController: NavController) {
    var foodItems by remember { mutableStateOf(mutableListOf<String>()) }
    var shuffledIndexes by remember { mutableStateOf(listOf<Int>()) }
    var currentInput by remember { mutableStateOf("") }
    var isShuffling by remember { mutableStateOf(false) }
    var isResultRevealed by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }

    val scope = rememberCoroutineScope()
    val itemAnimOffsets = remember { mutableStateListOf<Animatable<Float, AnimationVector1D>>() }

    val itemSpacing = 96.dp
    val density = LocalDensity.current

    // 애니메이션 상태들
    val shuffleScale by animateFloatAsState(
        targetValue = if (isShuffling) 1.1f else 1f,
        animationSpec = tween(300), label = ""
    )

    val resultCardScale by animateFloatAsState(
        targetValue = if (isResultRevealed) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = ""
    )

    fun initAnimatables() {
        itemAnimOffsets.clear()
        repeat(foodItems.size) {
            itemAnimOffsets.add(Animatable(0f))
        }
        shuffledIndexes = foodItems.indices.toList()
    }

    LaunchedEffect(foodItems.size) {
        initAnimatables()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2),
                        Color(0xFF1e3c72)
                    ),
                    radius = 1200f
                )
            )
    ) {
        // 배경 장식 원들
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset((-100).dp, (-100).dp)
                .background(
                    Color.White.copy(alpha = 0.1f),
                    CircleShape
                )
                .blur(20.dp)
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(250.dp, 100.dp)
                .background(
                    Color.White.copy(alpha = 0.08f),
                    CircleShape
                )
                .blur(15.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // 헤더
            Text(
                text = "🎯 오늘의 음식 추천",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "맛있는 선택의 순간",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // 글래스모피즘 입력 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    OutlinedTextField(
                        value = currentInput,
                        onValueChange = { if (foodItems.size < 5) currentInput = it },
                        label = {
                            Text(
                                "음식 입력 (최대 5개)",
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                            focusedBorderColor = Color.White.copy(alpha = 0.8f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (currentInput.isNotBlank() && foodItems.size < 5) {
                                foodItems = foodItems.toMutableList().apply { add(currentInput.trim()) }
                                currentInput = ""
                                initAnimatables()
                            }
                        },
                        enabled = foodItems.size < 5 && currentInput.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B6B),
                            disabledContainerColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .align(Alignment.End)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "✨ 추가",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // 음식 태그들 (네온 스타일)
            if (foodItems.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(foodItems) { index, item ->
                        val chipColor by animateColorAsState(
                            targetValue = Color(0xFF4ECDC4),
                            animationSpec = tween(300), label = ""
                        )

                        AssistChip(
                            onClick = {
                                if (!isShuffling && !isResultRevealed) {
                                    foodItems = foodItems.toMutableList().apply { removeAt(index) }
                                    initAnimatables()
                                    selectedIndex = -1
                                    isResultRevealed = false
                                }
                            },
                            label = {
                                Text(
                                    item,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = chipColor.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier
                                .shadow(6.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // 세련된 셔플 카드 영역
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .scale(shuffleScale)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    shuffledIndexes.forEachIndexed { visualIndex, actualIndex ->
                        val offset by remember {
                            derivedStateOf { itemAnimOffsets.getOrNull(actualIndex)?.value ?: 0f }
                        }

                        val isSelected = isResultRevealed && actualIndex == selectedIndex
                        val cardScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.1f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = ""
                        )

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(x = with(density) { offset.dp.roundToPx() }, y = 0) }
                                .size(80.dp)
                                .scale(cardScale)
                                .shadow(12.dp, CircleShape)
                                .background(
                                    if (isSelected)
                                        Color(0xFFFFD93D).copy(alpha = 0.3f)
                                    else
                                        Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .clickable(enabled = !isShuffling && !isResultRevealed) {
                                    if (!isResultRevealed) {
                                        selectedIndex = actualIndex
                                        isResultRevealed = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(
                                    if (isResultRevealed && actualIndex == selectedIndex)
                                        R.drawable.cloche_open
                                    else
                                        R.drawable.cloche_closed
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }
                }
            }

            // 결과 카드와 버튼들 사이에 적절한 간격 확보
            Spacer(Modifier.height(32.dp))

            // 결과 카드 (더 드라마틱하게)
            if (isResultRevealed && selectedIndex in foodItems.indices) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(resultCardScale)
                        .shadow(20.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD93D).copy(alpha = 0.1f),
                                        Color(0xFFFF6B6B).copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🎉 오늘의 추천 메뉴",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color(0xFF333333)
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = foodItems[selectedIndex],
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF6B6B)
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "맛있게 드세요! 🍽️",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF666666)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // 나머지 공간을 차지하되, 최소한의 공간만 확보 (스크롤 시에는 필요없음)
            // Spacer(Modifier.weight(1f, fill = false))

            // 하단 버튼들 (항상 표시되도록 보장)
            Column {
                // 다시 시작 버튼 (결과가 나온 후에만 표시)
                if (isResultRevealed) {
                    Button(
                        onClick = {
                            isResultRevealed = false
                            selectedIndex = -1
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD93D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            "🔄 다시 선택하기",
                            color = Color(0xFF333333),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            if (foodItems.size >= 2 && !isShuffling) {
                                isShuffling = true
                                isResultRevealed = false
                                selectedIndex = -1

                                scope.launch {
                                    repeat(12) {
                                        val a = Random.nextInt(shuffledIndexes.size)
                                        val b = (a + 1 + Random.nextInt(shuffledIndexes.size - 1)) % shuffledIndexes.size

                                        val offsetPx = with(density) { itemSpacing.toPx() }

                                        val aAnim = itemAnimOffsets[shuffledIndexes[a]]
                                        val bAnim = itemAnimOffsets[shuffledIndexes[b]]

                                        val jobA = launch {
                                            aAnim.animateTo(offsetPx, animationSpec = tween(100))
                                            aAnim.animateTo(0f, animationSpec = tween(100))
                                        }
                                        val jobB = launch {
                                            bAnim.animateTo(-offsetPx, animationSpec = tween(100))
                                            bAnim.animateTo(0f, animationSpec = tween(100))
                                        }

                                        shuffledIndexes = shuffledIndexes.toMutableList().apply {
                                            val temp = this[a]
                                            this[a] = this[b]
                                            this[b] = temp
                                        }

                                        jobA.join(); jobB.join()
                                        delay(100)
                                    }
                                    isShuffling = false
                                }
                            }
                        },
                        enabled = foodItems.size >= 2,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4ECDC4),
                            disabledContainerColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            if (isShuffling) "🎲 셔플 중..." else "🎲 셔플하기",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            Color.White.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text(
                            "⬅ 뒤로가기",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // 하단 여백 확보
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}