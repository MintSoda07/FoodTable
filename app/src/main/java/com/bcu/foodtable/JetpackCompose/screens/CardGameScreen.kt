package com.bcu.foodtable.JetpackCompose.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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
    var currentInput by remember { mutableStateOf("") }
    var isShuffling by remember { mutableStateOf(false) }
    var isResultRevealed by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }

    val scope = rememberCoroutineScope()
    val itemAnimOffsets = remember { mutableStateListOf<Animatable<Float, AnimationVector1D>>() }

    val itemSpacing = 96.dp
    val density = LocalDensity.current

    fun initAnimatables() {
        itemAnimOffsets.clear()
        repeat(foodItems.size) {
            itemAnimOffsets.add(Animatable(0f))
        }
    }

    LaunchedEffect(foodItems.size) {
        initAnimatables()
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("🥄 음식 셔플 게임", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = currentInput,
                onValueChange = { if (foodItems.size < 5) currentInput = it },
                label = { Text("음식 입력 (최대 5개)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (currentInput.isNotBlank() && foodItems.size < 5) {
                        foodItems = foodItems.toMutableList().apply { add(currentInput.trim()) }
                        itemAnimOffsets.add(Animatable(0f))
                        currentInput = ""
                    }
                },
                enabled = foodItems.size < 5
            ) {
                Text("추가")
            }
        }

        Spacer(Modifier.height(8.dp))

        // 결과를 본 후에만 Chip 리스트를 보여줌
        if (foodItems.isNotEmpty() && isResultRevealed) {
            LazyRow {
                itemsIndexed(foodItems) { index, item ->
                    AssistChip(
                        onClick = {
                            if (!isShuffling && !isResultRevealed) {
                                foodItems = foodItems.toMutableList().apply { removeAt(index) }
                                itemAnimOffsets.removeAt(index)
                                if (selectedIndex == index) selectedIndex = -1
                            }
                        },
                        label = { Text(item) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (index == selectedIndex) Color(0xFFE0F7FA)
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        if (foodItems.isNotEmpty() && !isResultRevealed) {
            Text("💡 은식기를 눌러서 결과를 확인하세요.", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            foodItems.forEachIndexed { index, _ ->
                val offset by remember { derivedStateOf { itemAnimOffsets.getOrNull(index)?.value ?: 0f } }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = with(density) { offset.dp.roundToPx() }, y = 0) }
                        .size(80.dp)
                        .clickable(enabled = !isShuffling && !isResultRevealed) {
                            if (!isResultRevealed) {
                                selectedIndex = index
                                isResultRevealed = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (isResultRevealed && index == selectedIndex)
                                R.drawable.cloche_open
                            else
                                R.drawable.cloche_closed
                        ),
                        contentDescription = null
                    )
                }
            }
        }

        if (isResultRevealed && selectedIndex in foodItems.indices) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "🍽 오늘의 메뉴는 ${foodItems[selectedIndex]} 입니다!",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Button(
                onClick = {
                    if (foodItems.size >= 2 && !isShuffling) {
                        isShuffling = true
                        isResultRevealed = false
                        selectedIndex = -1

                        scope.launch {
                            repeat(12) {
                                val a = Random.nextInt(foodItems.size)
                                val b = (a + 1 + Random.nextInt(foodItems.size - 1)) % foodItems.size

                                val offsetPx = with(density) { itemSpacing.toPx() }

                                val aAnim = itemAnimOffsets[a]
                                val bAnim = itemAnimOffsets[b]

                                // 교차 애니메이션
                                val jobA = launch {
                                    aAnim.animateTo(offsetPx, animationSpec = tween(100))
                                    aAnim.animateTo(0f, animationSpec = tween(100))
                                }
                                val jobB = launch {
                                    bAnim.animateTo(-offsetPx, animationSpec = tween(100))
                                    bAnim.animateTo(0f, animationSpec = tween(100))
                                }

                                // 실제 항목 순서도 바꾸기 (swap)
                                val newList = foodItems.toMutableList()
                                newList[a] = foodItems[b].also { newList[b] = foodItems[a] }
                                foodItems = newList

                                jobA.join(); jobB.join()
                                delay(100)
                            }
                            isShuffling = false
                        }
                    }
                },
                enabled = foodItems.size >= 2
            ) {
                Text("셔플")
            }

            Spacer(Modifier.width(16.dp))

            Button(onClick = { navController.popBackStack() }) {
                Text("뒤로가기")
            }
        }
    }
}
