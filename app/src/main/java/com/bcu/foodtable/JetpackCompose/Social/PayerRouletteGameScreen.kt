package com.bcu.foodtable.JetpackCompose.Social

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayerRouletteGameScreen(navController: NavController) {
    // ── 상태 ──
    // initial two slots
    val names: SnapshotStateList<String> = remember { mutableStateListOf("", "") }
    var isAnimating by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    // 애니메이션 로직
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            // 얼마나 굴릴지: 최소 한 바퀴 이상 + 랜덤 오프셋
            val total = names.size * 12 + Random.nextInt(names.size)
            var delayTime = 50L
            repeat(total) { i ->
                currentIndex = i % names.size
                delay(delayTime)
                // 마지막 40% 구간에서 점점 느려지게
                if (i > total * 0.6f) delayTime += 20L
            }
            isAnimating = false
            showResult = true
        }
    }

    // 테마 적용
    FoodTableTheme {
        Scaffold { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    "낼 사람 룰렛",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))

                // 이름 입력 리스트
                names.forEachIndexed { idx, name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (!isAnimating) names[idx] = it },
                            label = { Text("이름 ${idx + 1}") },
                            enabled = !isAnimating,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { if (!isAnimating && names.size > 1) names.removeAt(idx) },
                            enabled = !isAnimating
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "삭제")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // 사람 추가 버튼
                Button(
                    onClick = { if (!isAnimating) names.add("") },
                    enabled = !isAnimating,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("사람 추가")
                }

                Spacer(Modifier.height(24.dp))

                // 룰렛 카드 영역
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    itemsIndexed(names) { idx, item ->
                        Card(
                            modifier = Modifier
                                .size(60.dp)
                                .border(
                                    width = if (idx == currentIndex && isAnimating) 3.dp else 1.dp,
                                    color = if (idx == currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = item.ifBlank { "${idx + 1}" },
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 시작 / 돌아가기 버튼
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { if (!isAnimating && names.size > 1) isAnimating = true },
                        enabled = !isAnimating && names.size > 1,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("시작!", fontSize = 18.sp)
                    }
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("돌아가기", fontSize = 18.sp)
                    }
                }
            }

            // 결과 모달
            if (showResult) {
                AlertDialog(
                    onDismissRequest = { showResult = false },
                    title = { Text("🎉 축하합니다!") },
                    text = {
                        Text(
                            text = "${names[currentIndex]}님! 오늘의 낼 사람으로 선택되었습니다!",
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showResult = false }) {
                            Text("확인")
                        }
                    }
                )
            }
        }
    }
}
