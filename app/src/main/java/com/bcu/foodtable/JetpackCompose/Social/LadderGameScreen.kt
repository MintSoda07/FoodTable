package com.bcu.foodtable.JetpackCompose.Social

import android.graphics.Paint as AndroidPaint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.floor
import kotlin.random.Random

// ─── 테마 정의 ────────────────────────────────────────────────────────────
val WarmLightColorScheme = lightColorScheme(
    primary = Color(0xFFE25532), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2D6), onPrimaryContainer = Color(0xFF5C2B1B),
    background = Color(0xFFFFFBF8), onBackground = Color(0xFF3A2C28),
    surface = Color.White, onSurface = Color(0xFF2E2E2E),
    surfaceVariant = Color(0xFFFBE7DF), onSurfaceVariant = Color(0xFF5F5F5F),
    outline = Color(0xFFDDC7BD), inversePrimary = Color(0xFFFF8F6B),
    error = Color(0xFFD32F2F), onError = Color.White
)

@Composable
fun FoodTableTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WarmLightColorScheme,
        typography = Typography(),
        content = content
    )
}
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LadderGameScreen(navController: NavController) {
    // 1) 상태 선언
    var playerCount by rememberSaveable { mutableStateOf(2) }
    val playerNames = rememberSaveable(
        playerCount,
        saver = listSaver<SnapshotStateList<String>, String>(
            save = { it.toList() },
            restore = { restored ->
                mutableStateListOf<String>().apply { addAll(restored) }
            }
        )
    ) {
        mutableStateListOf<String>().apply { repeat(playerCount) { add("") } }
    }
    LaunchedEffect(playerCount) {
        if (playerNames.size < playerCount) {
            repeat(playerCount - playerNames.size) { playerNames.add("") }
        } else if (playerNames.size > playerCount) {
            repeat(playerNames.size - playerCount) { playerNames.removeAt(playerNames.lastIndex) }
        }
    }

    var started by rememberSaveable { mutableStateOf(false) }
    var loserName by rememberSaveable { mutableStateOf<String?>(null) }

    var ladderData by remember { mutableStateOf(emptyList<List<Boolean>>()) }
    var paths by remember { mutableStateOf(emptyList<List<Int>>()) }

    val rows = 12
    val animProgress = remember { Animatable(0f) }

    // 2) 시작 → 사다리 생성 → 애니메이션 → 결과 판정
    LaunchedEffect(started) {
        if (started) {
            ladderData = generateLadder(rows, playerCount)
            paths = computePaths(ladderData)
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = rows.toFloat(),
                animationSpec = tween(durationMillis = (rows + 1) * 600)
            )
            // 1번 열(index=0)에 도달한 사람 찾기
            val idx = paths.indexOfFirst { it.last() == 0 }
            loserName = playerNames.getOrNull(idx)
        }
    }

    // 3) 플레이어별 원 색상 (HSV 방식)
    val circleColors = remember(playerCount) {
        List(playerCount) { idx ->
            val hsv = floatArrayOf(360f * idx / playerCount, 0.7f, 0.9f)
            Color(android.graphics.Color.HSVToColor(hsv))
        }
    }

    // ─── 전체를 테마로 감싼 뒤 UI 렌더링 ───────────────────────────
    FoodTableTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("🍀 진짜 사다리 타기") },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    // 참여자 수 조절
                    PlayerCountPicker(
                        count = playerCount,
                        disabled = started,
                        onCountChange = { delta ->
                            playerCount = (playerCount + delta).coerceAtLeast(2)
                        }
                    )
                    Spacer(Modifier.height(8.dp))

                    // 이름 입력
                    PlayerNamesInput(
                        names = playerNames,
                        disabled = started
                    )
                    Spacer(Modifier.height(16.dp))

                    // 1번 페이 안내 텍스트
                    Text(
                        text = "맨 왼쪽에 도착한 사람이 쏩니다!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    // 사다리 그리기 영역에 내부 여백 추가
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp) // 내부 여백
                    ) {
                        LadderCanvas(
                            playerCount = playerCount,
                            ladder = ladderData,
                            animProgress = animProgress.value,
                            paths = paths,
                            playerNames = playerNames,
                            circleColors = circleColors,
                            lineColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            started = started
                        )
                    }
                    Spacer(Modifier.height(24.dp))

                    // 시작 / 돌아가기
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { started = true },
                            enabled = !started,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("시작!", fontSize = 18.sp)
                        }
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("돌아가기", fontSize = 18.sp)
                        }
                    }
                }

                // 결과 모달
                loserName?.let { name ->
                    AlertDialog(
                        onDismissRequest = { loserName = null },
                        title = { Text("결과") },
                        text = {
                            Text(
                                "$name 님이 1등입니다! \n 기쁜 마음으로 한 턱 쏘시는 거 어때요?",
                                textAlign = TextAlign.Center
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { loserName = null }) {
                                Text("확인")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerCountPicker(
    count: Int,
    disabled: Boolean,
    onCountChange: (delta: Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("참여 인원: ", color = MaterialTheme.colorScheme.onBackground)
        IconButton(onClick = { onCountChange(-1) }, enabled = !disabled) {
            Icon(Icons.Default.Remove, contentDescription = "감소")
        }
        Text(
            "$count 명",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.widthIn(56.dp),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { onCountChange(+1) }, enabled = !disabled) {
            Icon(Icons.Default.Add, contentDescription = "증가")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerNamesInput(
    names: MutableList<String>,
    disabled: Boolean
) {
    Column {
        names.forEachIndexed { idx, name ->
            OutlinedTextField(
                value = name,
                onValueChange = { names[idx] = it },
                label = { Text("이름 ${idx + 1}") },
                singleLine = true,
                enabled = !disabled,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun LadderCanvas(
    playerCount: Int,
    ladder: List<List<Boolean>>,
    animProgress: Float,
    paths: List<List<Int>>,
    playerNames: List<String>,
    circleColors: List<Color>,
    lineColor: Color,
    started: Boolean
) {
    val density = LocalDensity.current
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cols = playerCount
        val rows = ladder.size
        if (cols < 2) return@Canvas

        val colSpacing = w / (cols - 1)
        val rowSpacing = if (rows > 0) h / rows else h
        val stroke = 4.dp.toPx()
        val radius = 12.dp.toPx()

        // 수직선
        repeat(cols) { i ->
            drawLine(lineColor, Offset(i * colSpacing, 0f), Offset(i * colSpacing, h), stroke)
        }

        if (started) {
            // 가로선
            ladder.forEachIndexed { r, row ->
                val y = r * rowSpacing + rowSpacing / 2
                row.forEachIndexed { c, on ->
                    if (on) {
                        drawLine(lineColor, Offset(c * colSpacing, y), Offset((c + 1) * colSpacing, y), stroke)
                    }
                }
            }
            // 애니메이션 + 원/이름
            paths.forEachIndexed { idx, path ->
                val prog = animProgress.coerceIn(0f, rows.toFloat())
                val base = floor(prog).toInt().coerceIn(0, rows)
                val frac = prog - base
                val x0 = (path.getOrNull(base) ?: 0) * colSpacing
                val x1 = (path.getOrNull(base + 1) ?: 0) * colSpacing
                val cx = x0 + (x1 - x0) * frac
                val cy = prog * rowSpacing

                drawCircle(circleColors[idx], radius = radius, center = Offset(cx, cy))

                playerNames.getOrNull(idx)?.takeIf(String::isNotBlank)?.let { name ->
                    val paint = AndroidPaint().apply {
                        color = android.graphics.Color.WHITE
                        textAlign = AndroidPaint.Align.CENTER
                        textSize = density.run { 14.sp.toPx() }
                        isFakeBoldText = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(name, cx, cy + paint.textSize / 3, paint)
                }
            }
        }
    }
}

private fun generateLadder(rows: Int, cols: Int): List<List<Boolean>> {
    val rnd = Random(System.currentTimeMillis())
    return List(rows) {
        val rung = MutableList(cols - 1) { false }
        var prev = false
        rung.indices.forEach { i ->
            if (!prev && rnd.nextBoolean()) {
                rung[i] = true; prev = true
            } else prev = false
        }
        rung
    }
}

private fun computePaths(ladder: List<List<Boolean>>): List<List<Int>> {
    if (ladder.isEmpty()) return emptyList()
    val cols = ladder[0].size + 1
    return List(cols) { start ->
        var pos = start
        val path = mutableListOf(pos)
        ladder.forEach { row ->
            pos = when {
                pos > 0 && row[pos - 1] -> pos - 1
                pos < cols - 1 && row[pos]  -> pos + 1
                else                        -> pos
            }
            path += pos
        }
        path
    }
}
