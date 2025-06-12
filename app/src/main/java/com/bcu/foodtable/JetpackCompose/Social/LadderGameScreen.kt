package com.bcu.foodtable.JetpackCompose.Social

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.random.Random

// ─── 테마 정의 ────────────────────────────────────────────────────────────
private val LadderGameColorScheme = lightColorScheme(
    primary = Color(0xFFF57C00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    secondary = Color(0xFF4CAF50),
    tertiary = Color(0xFFE91E63),
    background = Color(0xFFFFF8E1),
    onBackground = Color(0xFF4E4539),
    surface = Color.White.copy(alpha = 0.8f),
    onSurface = Color(0xFF4E4539),
    surfaceVariant = Color.White.copy(alpha = 0.9f),
    onSurfaceVariant = Color(0xFF8D6E63),
    outline = Color(0xFFD7CCC8),
    error = Color(0xFFD32F2F)
)

@Composable
fun LadderGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LadderGameColorScheme,
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        ),
        typography = Typography(),
        content = content
    )
}
// ────────────────────────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LadderGameScreen(navController: NavController) {
    var playerCount by rememberSaveable { mutableStateOf(2) }
    val playerNames = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(
            save = { it.toList() },
            restore = { restored -> mutableStateListOf<String>().apply { addAll(restored) } }
        )
    ) { mutableStateListOf("참가자 1", "참가자 2") }

    // [앱 종료 버그 완벽 수정]
    // 버튼 클릭 시 호출될 안전한 상태 업데이트 함수
    val onPlayerCountChange = { newCount: Int ->
        val safeNewCount = newCount.coerceIn(2, 10)
        val currentSize = playerNames.size

        if (safeNewCount > currentSize) {
            val itemsToAdd = safeNewCount - currentSize
            repeat(itemsToAdd) {
                playerNames.add("참가자 ${playerNames.size + 1}")
            }
        } else if (safeNewCount < currentSize) {
            val itemsToRemove = currentSize - safeNewCount
            repeat(itemsToRemove) {
                // 가장 마지막 항목부터 안전하게 제거
                if (playerNames.isNotEmpty()) {
                    playerNames.removeLast()
                }
            }
        }
        playerCount = safeNewCount
    }

    var started by rememberSaveable { mutableStateOf(false) }
    var showResult by rememberSaveable { mutableStateOf(false) }
    var winnerName by rememberSaveable { mutableStateOf<String?>(null) }
    var ladderData by remember { mutableStateOf(emptyList<List<Boolean>>()) }
    var paths by remember { mutableStateOf(emptyList<List<Int>>()) }

    val rows = 12
    val animProgress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    fun startGame() {
        coroutineScope.launch {
            started = true
            showResult = false
            winnerName = null
            ladderData = generateLadder(rows, playerCount)
            paths = computePaths(ladderData)
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = rows.toFloat(),
                animationSpec = tween(durationMillis = rows * 500)
            )
            val winnerIndex = paths.indexOfFirst { it.lastOrNull() == 0 }
            if (winnerIndex != -1) {
                winnerName = playerNames.getOrNull(winnerIndex) ?: "참가자 ${winnerIndex + 1}"
            }
            showResult = true
        }
    }

    val playerColors = remember(playerCount) {
        (0 until playerCount).map { i ->
            val hue = (i * (360f / playerCount) + 15f) % 360f
            Color.hsv(hue, 0.7f, 0.9f)
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.background
        )
    )

    LadderGameTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("🍀 행운의 사다리 게임", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
            ) {
                AnimatedIconsBackground()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    AnimatedVisibility(
                        visible = !started,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        SetupSection(
                            playerCount = playerCount,
                            onPlayerCountChange = onPlayerCountChange, // 수정된 함수 전달
                            playerNames = playerNames
                        )
                    }

                    if(started) Spacer(Modifier.height(16.dp))

                    LadderDisplaySection(
                        modifier = Modifier.weight(1f),
                        playerCount = playerCount,
                        playerNames = playerNames,
                        playerColors = playerColors,
                        ladderData = ladderData,
                        paths = paths,
                        animProgress = animProgress.value,
                        started = started
                    )

                    Spacer(Modifier.height(16.dp))

                    ActionButton(
                        isStarted = started,
                        onClick = {
                            if (started) {
                                started = false
                                showResult = false
                            } else {
                                startGame()
                            }
                        }
                    )
                    Spacer(Modifier.height(24.dp))
                }

                if (showResult && winnerName != null) {
                    ResultDialog(winnerName = winnerName!!) { showResult = false }
                }
            }
        }
    }
}

@Composable
private fun SetupSection(
    playerCount: Int,
    onPlayerCountChange: (Int) -> Unit,
    playerNames: SnapshotStateList<String>
) {
    val gradientBorder = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        )
    )
    Column {
        Text("게임 설정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, gradientBorder, MaterialTheme.shapes.large)
        ) {
            Column(Modifier.padding(16.dp)) {
                PlayerCountPicker(
                    count = playerCount,
                    onCountChange = onPlayerCountChange
                )
                Divider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                PlayerNamesInput(names = playerNames)
            }
        }
    }
}

@Composable
private fun LadderDisplaySection(
    modifier: Modifier = Modifier,
    playerCount: Int,
    playerNames: List<String>,
    playerColors: List<Color>,
    ladderData: List<List<Boolean>>,
    paths: List<List<Int>>,
    animProgress: Float,
    started: Boolean
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val titleText = if(started) "과연 결과는...?" else "참가자를 확인하세요"
        Text(titleText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        PlayerNameTags(playerNames = playerNames, playerColors = playerColors)

        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LadderCanvas(
                playerCount = playerCount,
                ladder = ladderData,
                animProgress = animProgress,
                paths = paths,
                playerColors = playerColors,
                lineColor = MaterialTheme.colorScheme.onSurfaceVariant,
                started = started
            )
        }
        Spacer(Modifier.height(8.dp))
        ResultTags(playerCount = playerCount)
    }
}

@Composable
private fun ActionButton(isStarted: Boolean, onClick: () -> Unit) {
    val buttonText = if (isStarted) "처음으로" else "게임 시작!"
    val gradient = Brush.horizontalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, MaterialTheme.shapes.medium, spotColor = MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Text(buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun PlayerCountPicker(count: Int, onCountChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("참여 인원", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = { onCountChange(count - 1) },
            enabled = count > 2,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) { Icon(Icons.Default.Remove, "감소") }
        Text(
            "$count 명",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(60.dp),
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = { onCountChange(count + 1) },
            enabled = count < 10,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) { Icon(Icons.Default.Add, "증가") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerNamesInput(names: SnapshotStateList<String>) {
    LazyColumn(modifier = Modifier.heightIn(max = 180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(names, key = { index, _ -> index }) { idx, name ->
            OutlinedTextField(
                value = name,
                onValueChange = { if (idx < names.size) names[idx] = it },
                label = { Text("참가자 ${idx + 1} 이름") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
        }
    }
}

@Composable
private fun LadderCanvas(
    playerCount: Int, ladder: List<List<Boolean>>, animProgress: Float,
    paths: List<List<Int>>, playerColors: List<Color>, lineColor: Color, started: Boolean
) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (playerCount < 2) return@Canvas

        val colSpacing = w / (playerCount - 1)
        val rowSpacing = if (ladder.isNotEmpty()) h / ladder.size else h
        val stroke = 4.dp.toPx()

        (0 until playerCount).forEach { i -> drawLine(lineColor, Offset(i * colSpacing, 0f), Offset(i * colSpacing, h), stroke, StrokeCap.Round) }

        if (started) {
            ladder.forEachIndexed { r, row ->
                row.forEachIndexed { c, on ->
                    if (on) {
                        val y = r * rowSpacing + rowSpacing / 2
                        drawLine(lineColor, Offset(c * colSpacing, y), Offset((c + 1) * colSpacing, y), stroke, StrokeCap.Round)
                    }
                }
            }
        }

        if (started && paths.isNotEmpty()) {
            paths.forEachIndexed { idx, path ->
                if (path.isEmpty()) return@forEachIndexed
                val prog = animProgress.coerceIn(0f, ladder.size.toFloat())
                val base = floor(prog).toInt().coerceIn(0, ladder.size)
                val frac = prog - base

                val currentPos = path.getOrElse(base) { path.last() }
                val nextPos = path.getOrElse(base + 1) { path.last() }

                val cx = (currentPos * colSpacing) + ((nextPos - currentPos) * colSpacing) * frac
                val cy = prog * rowSpacing
                val radius = 8.dp.toPx()

                drawCircle(color = playerColors[idx], radius = radius, center = Offset(cx, cy))
                drawCircle(color = Color.White, radius = radius * 0.5f, center = Offset(cx, cy))
            }
        }
    }
}

@Composable
private fun PlayerNameTags(playerNames: List<String>, playerColors: List<Color>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        playerNames.forEachIndexed { index, name ->
            Text(
                text = name,
                modifier = Modifier
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(playerColors[index])
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ResultTags(playerCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        (0 until playerCount).forEach { index ->
            val isWinner = index == 0
            val color = if (isWinner) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            val text = if (isWinner) "🎉 당첨" else "꽝"
            Text(
                text = text,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f))
                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ResultDialog(winnerName: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.EmojiEvents, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) },
        title = { Text("결과 발표!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
        text = { Text("🎉 $winnerName 님이 당첨되었습니다! 🎉\n오늘의 주인공이 되신 걸 축하해요!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), lineHeight = 24.sp) },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("처음으로")
            }
        },
        shape = MaterialTheme.shapes.large
    )
}

@Composable
fun AnimatedIconsBackground() {
    val icons = remember { listOf("🥐", "🍩", "🍓", "🥕", "🍔", "🍕", "🍰", "🍜") }
    val density = LocalDensity.current
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        icons.forEach { icon ->
            val random = remember { Random(icon.hashCode()) }
            val startX = remember { random.nextFloat() }
            val duration = remember { random.nextInt(15000, 25000) }
            val size = remember { random.nextInt(20, 40).dp }

            val infiniteTransition = rememberInfiniteTransition(label = "")
            val yPos by infiniteTransition.animateFloat(
                initialValue = screenHeightPx + size.value * 2,
                targetValue = -size.value * 2,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = LinearEasing, delayMillis = random.nextInt(0, 5000)),
                    repeatMode = RepeatMode.Restart
                ), label = ""
            )

            Text(
                text = icon,
                fontSize = size.value.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = LocalConfiguration.current.screenWidthDp.dp * startX,
                        y = with(density) { yPos.toDp() }
                    )
                    .alpha(0.6f)
            )
        }
    }
}

private fun generateLadder(rows: Int, cols: Int): List<List<Boolean>> {
    if (cols < 2) return emptyList()
    val rnd = Random(System.currentTimeMillis())
    return List(rows) {
        val rung = MutableList(cols - 1) { false }
        var prev = false
        for (i in rung.indices) {
            if (!prev && rnd.nextDouble() > 0.6) {
                rung[i] = true
                prev = true
            } else {
                prev = false
            }
        }
        rung
    }
}

private fun computePaths(ladder: List<List<Boolean>>): List<List<Int>> {
    if (ladder.isEmpty()) return emptyList()
    val cols = ladder.first().size + 1
    if (cols < 2) return (0 until cols).map { listOf(it) }

    return (0 until cols).map { start ->
        val path = mutableListOf(start)
        var currentPos = start
        ladder.forEach { row ->
            val newPos = when {
                currentPos > 0 && row.getOrNull(currentPos - 1) == true -> currentPos - 1
                currentPos < cols - 1 && row.getOrNull(currentPos) == true -> currentPos + 1
                else -> currentPos
            }
            path.add(newPos)
            currentPos = newPos
        }
        path
    }
}