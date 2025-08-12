// =========================
// HiddenScreen.kt (전체 코드)
// =========================

package com.bcu.foodtable.ui.home

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bcu.foodtable.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

// =========================
// 데이터/스코어 계산
// =========================

private val SYMBOLS = listOf("🍒", "🍋", "⭐", "🍉", "🔔", "💎", "🍀", "7️⃣")

private val SYMBOL_SCORE = mapOf(
    "🍒" to 10,
    "🍋" to 8,
    "⭐" to 20,
    "🍉" to 12,
    "🔔" to 15,
    "💎" to 30,
    "🍀" to 25,
    "7️⃣" to 50
)

private data class SpinResult(
    val base: Int,
    val multiplier: Double,
    val bonus: Int,
    val total: Int,
    val pattern: String
)

private fun evaluateSpin(centerRowSymbols: List<String>): SpinResult {
    val base = centerRowSymbols.sumOf { SYMBOL_SCORE[it] ?: 0 }
    val counts = centerRowSymbols.groupingBy { it }.eachCount()

    val five = counts.values.any { it == 5 }
    val four = counts.values.any { it == 4 }
    val threes = counts.values.count { it == 3 }
    val pairs = counts.values.count { it == 2 }

    var pattern = "No Match"
    var multiplier = 1.0

    when {
        five -> { pattern = "5 of a Kind"; multiplier = 10.0 }
        four -> { pattern = "4 of a Kind"; multiplier = 5.0 }
        threes == 1 && pairs == 1 -> { pattern = "Full House"; multiplier = 4.0 }
        threes == 1 -> { pattern = "3 of a Kind"; multiplier = 3.0 }
        pairs == 2 -> { pattern = "Two Pair"; multiplier = 2.0 }
        pairs == 1 -> { pattern = "One Pair"; multiplier = 1.5 }
        else -> { pattern = "No Match"; multiplier = 1.0 }
    }

    // 보너스: 🍀 포함 +5, 7️⃣ 하나당 +20
    val bonus = (if (centerRowSymbols.contains("🍀")) 5 else 0) + (centerRowSymbols.count { it == "7️⃣" } * 20)

    if (centerRowSymbols.all { it == "7️⃣" }) {
        pattern = "JACKPOT"
        multiplier = 12.0
    }

    val total = ((base + bonus) * multiplier).roundToInt()
    return SpinResult(base, multiplier, bonus, total, pattern)
}

// =========================
// 사운드 로더
// =========================

private class SlotSounds(
    private val soundPool: SoundPool,
    val lever: Int,
    val spinStart: Int,
    val music: Int,
    val tick: Int,
    val stop: Int,
    val winLow: Int,
    val fail: Int
) {
    private var musicStreamId: Int? = null

    fun playLever() = soundPool.play(lever, 0.9f, 0.9f, 1, 0, 1f)
    fun playSpinStart() = soundPool.play(spinStart, 0.8f, 0.8f, 1, 0, 1f)
    fun playTick() = soundPool.play(tick, 0.25f, 0.25f, 1, 0, 1f)
    fun playStop() = soundPool.play(stop, 0.6f, 0.6f, 1, 0, 1f)
    fun playWinLow() = soundPool.play(winLow, 1f, 1f, 1, 0, 1f)
    fun playFail() = soundPool.play(fail, 0.9f, 0.9f, 1, 0, 1f)

    fun startMusic() {
        stopMusic()
        musicStreamId = soundPool.play(music, 0.35f, 0.35f, 0, -1, 1f)
    }
    fun stopMusic() {
        musicStreamId?.let { soundPool.stop(it) }
        musicStreamId = null
    }
}

@Composable
private fun rememberSlotSounds(): Pair<SoundPool, SlotSounds> {
    val context = LocalContext.current
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
    }
    val sounds = remember {
        SlotSounds(
            soundPool = soundPool,
            lever     = soundPool.load(context, R.raw.slot_lever, 1),
            spinStart = soundPool.load(context, R.raw.slot_spin, 1),
            music     = soundPool.load(context, R.raw.slot_music, 1),
            tick      = soundPool.load(context, R.raw.slot_tick, 1),
            stop      = soundPool.load(context, R.raw.slot_stop, 1),
            winLow    = soundPool.load(context, R.raw.slot_win, 1),
            fail      = soundPool.load(context, R.raw.slot_fail, 1)
            // all of these SFX sources are licenced under pixabay, free SFX.
        )
    }
    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }
    return soundPool to sounds
}

// =========================
// 릴 상태 & 헬퍼
// =========================

private class ReelState(
    indexInit: Int
) {
    var index by mutableIntStateOf(indexInit) // 현재 "가운데" 행의 심볼 인덱스
    val offsetPx = androidx.compose.animation.core.Animatable(0f) // 위로 스크롤되는 오프셋(0..cellH)
}

@Composable
private fun androidx.compose.animation.core.Animatable<Float, *>.asState():
        androidx.compose.runtime.State<Float> {
    val state = produceState(initialValue = this.value, this) {
        snapshotFlow { this@asState.value }.collect { value = it }
    }
    return state
}

// =========================
// 릴 UI (3행 보이는 뷰포트 + 스크롤 오프셋)
// =========================

@Composable
private fun ReelView(
    symbolList: List<String>,
    index: Int,                // 현재 가운데 행의 인덱스
    offsetPx: Float,           // 0..cellHeightPx
    cellSize: Float,           // px
    cellDp: androidx.compose.ui.unit.Dp
) {
    val prev = symbolList[(index - 1 + symbolList.size) % symbolList.size]
    val curr = symbolList[index % symbolList.size]
    val next = symbolList[(index + 1) % symbolList.size]

    val neon = Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A)))
    val frameColor = Color(0xFFFFEC6E)

    Box(
        modifier = Modifier
            .size(cellDp, (cellDp * 3f))
            .clip(RoundedCornerShape(12.dp))
            .background(neon)
            .border(2.dp, frameColor.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            .graphicsLayer { clip = true },
        contentAlignment = Alignment.Center
    ) {
        // 내용 컬럼(3행)을 위로 offset
        Column(
            modifier = Modifier
                .offset { IntOffset(0, -offsetPx.roundToInt()) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            ReelCell(prev, cellDp)
            ReelCell(curr, cellDp)
            ReelCell(next, cellDp)
        }

        // 위/아래 페이드 마스크
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(0.45f),
                        0.18f to Color.Transparent,
                        0.82f to Color.Transparent,
                        1f to Color.Black.copy(0.45f)
                    )
                )
        )
        // 가운데 라인 글로우
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = 0.45f }
                .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(12.dp))
        )
    }
}

@Composable
private fun ReelCell(symbol: String, cellDp: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(cellDp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

// =========================
// 폭죽(잭팟) 오버레이 (Lottie)
// =========================

@Composable
private fun FireworksOverlay(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(250))
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.cash)
        )
        val progress by animateLottieCompositionAsState(
            composition,
            iterations = LottieConstants.IterateForever
        )
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.9f)
        )
    }
}

// =========================
// HiddenScreen 본체
// =========================

@Composable
fun HiddenScreen() {
    val slotCount = 5
    val (_, sfx) = rememberSlotSounds()
    val scope = rememberCoroutineScope()

    // 릴 상태
    val reels = remember {
        List(slotCount) { ReelState(indexInit = (SYMBOLS.indices).random()) }
    }

    // 스코어
    var totalScore by remember { mutableIntStateOf(0) }
    var lastWin by remember { mutableIntStateOf(0) }
    var lastPattern by remember { mutableStateOf("—") }
    var lastMultiplier by remember { mutableDoubleStateOf(1.0) }
    var isSpinning by remember { mutableStateOf(false) }
    var showFireworks by remember { mutableStateOf(false) }

    // 배경 네온 애니메이션(살짝 움직이는 그라데이션)
    val bgAnim = rememberInfiniteTransition(label = "bg")
    val glowShift by bgAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "bgShift"
    )

    fun currentCenterSymbols(): List<String> = reels.map { SYMBOLS[it.index % SYMBOLS.size] }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxW = constraints.maxWidth.toFloat()
            val maxH = constraints.maxHeight.toFloat()
            val density = LocalDensity.current

            // 셀 크기 결정: 화면 비에 맞춰 유연하게
            // 5릴 가로 배치 + 여백 고려
            val cellPx = min(maxW / (slotCount + 1.2f), maxH / 6f)
            val cellDp = with(density) { cellPx.toDp() }

            // 슬롯 영역 배경 네온
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2B1F5E).copy(0.4f),
                                Color(0xFF0D0D0D).copy(0.9f)
                            ),
                            center = androidx.compose.ui.geometry.Offset(
                                x = maxW * (0.3f + 0.4f * glowShift),
                                y = maxH * (0.4f + 0.2f * (1 - glowShift))
                            ),
                            radius = maxW * 0.9f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🎰 SECRET SLOT MACHINE 🎰",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color(0xFFFFE36E),
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(18.dp))

                // 릴들과 프레임
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF161616),
                                    Color(0xFF101010)
                                )
                            )
                        )
                        .border(
                            2.dp,
                            Color(0xFFFFE36E).copy(0.65f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 12.dp, horizontal = 10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        reels.forEach { reel ->
                            val offset by reel.offsetPx.asState()
                            ReelView(
                                symbolList = SYMBOLS,
                                index = reel.index,
                                offsetPx = offset,
                                cellSize = cellPx,
                                cellDp = cellDp
                            )
                        }
                    }

                    // 가운데 라인 가이드
                    Box(
                        Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = 0.35f }
                    ) {
                        Divider(
                            color = Color.White.copy(0.25f),
                            thickness = 2.dp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // SPIN 버튼
                Button(
                    onClick = {
                        if (isSpinning) return@Button
                        isSpinning = true
                        showFireworks = false
                        sfx.playLever()
                        sfx.playSpinStart()
                        sfx.startMusic()

                        scope.launch {
                            // 릴 개별 회전
                            coroutineScope {
                                val jobs: List<Job> = reels.mapIndexed { i, reel ->
                                    launch {
                                        // 오른쪽 릴일수록 오래/느리게
                                        val steps = 22 + (i * 7)
                                        val baseDur = 70L + (i * 10L)
                                        repeat(steps) { step ->
                                            // 한 칸 스크롤 애니메이션
                                            reel.offsetPx.snapTo(0f)
                                            reel.offsetPx.animateTo(
                                                targetValue = cellPx,
                                                animationSpec = tween(
                                                    durationMillis = (baseDur + step * 5).toInt(),
                                                    easing = LinearEasing
                                                )
                                            )
                                            // 다음 심볼로 확정하고 오프셋 리셋
                                            reel.index = (reel.index + 1) % SYMBOLS.size
                                            reel.offsetPx.snapTo(0f)

                                            // 틱 사운드
                                            sfx.playTick()
                                        }
                                        // 릴 멈춤 사운드 + 살짝 튕김
                                        sfx.playStop()
                                        reel.offsetPx.animateTo(
                                            targetValue = cellPx * 0.18f,
                                            animationSpec = tween(70, easing = LinearEasing)
                                        )
                                        reel.offsetPx.animateTo(
                                            targetValue = 0f,
                                            animationSpec = tween(90, easing = LinearEasing)
                                        )
                                    }
                                }
                                jobs.joinAll()
                            }

                            // 결과 계산(가운데 행 심볼)
                            val result = evaluateSpin(currentCenterSymbols())
                            lastWin = result.total
                            lastPattern = result.pattern
                            lastMultiplier = result.multiplier
                            totalScore += lastWin

                            // 음악 정지
                            sfx.stopMusic()

                            // 사운드 & 연출
                            when {
                                result.pattern == "JACKPOT" || result.pattern == "5 of a Kind" -> {
                                    sfx.playWinLow()
                                    showFireworks = true
                                    launch {
                                        delay(2600)
                                        showFireworks = false
                                    }
                                }
                                result.total > 0 -> {
                                    sfx.playWinLow()
                                }
                                else -> {
                                    sfx.playFail()
                                }
                            }

                            isSpinning = false
                        }
                    },
                    enabled = !isSpinning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (isSpinning) "Spinning..." else "SPIN",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 결과 패널
                ResultPanel(
                    lastWin = lastWin,
                    lastPattern = lastPattern,
                    lastMultiplier = lastMultiplier
                )

                Spacer(Modifier.height(12.dp))

                // 스코어 패널
                ScorePanel(totalScore = totalScore)

                Spacer(Modifier.height(18.dp))

                // 규칙/배점
                RulesPanel()
            }

            // 폭죽
            FireworksOverlay(visible = showFireworks)
        }
    }
}

// =========================
// 패널들
// =========================

@Composable
private fun ResultPanel(
    lastWin: Int,
    lastPattern: String,
    lastMultiplier: Double
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFF101010))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Last Win",
                color = Color(0xFFAAAAAA),
                fontSize = 12.sp
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "$lastWin",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${String.format("x%.1f", lastMultiplier)}  •  $lastPattern",
                    color = Color(0xFFFFE36E),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ScorePanel(totalScore: Int) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFF0E0E0E))
                .padding(14.dp)
        ) {
            Text("Total Score", color = Color(0xFFAAAAAA), fontSize = 12.sp)
            Text(
                "$totalScore",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun RulesPanel() {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFF0E0E0E))
                .padding(14.dp)
        ) {
            Text("Score Rules", color = Color(0xFFAAAAAA), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))

            Text(
                "• Symbol Scores",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "🍒10  🍋8  ⭐20  🍉12  🔔15  💎30  🍀25  7️⃣50",
                color = Color(0xFFDDDDDD),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(8.dp))
            Divider(color = Color(0x22FFFFFF))
            Spacer(Modifier.height(8.dp))

            Text(
                "• Pattern Multipliers",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "JACKPOT(7️⃣x5): x12\n5 of a Kind: x10\n4 of a Kind: x5\nFull House: x4\n3 of a Kind: x3\nTwo Pair: x2\nOne Pair: x1.5\nNo Match: x1",
                color = Color(0xFFDDDDDD),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(8.dp))
            Divider(color = Color(0x22FFFFFF))
            Spacer(Modifier.height(8.dp))

            Text(
                "• Bonuses",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "🍀 포함 시 +5, 7️⃣ 1개당 +20 (합산)",
                color = Color(0xFFDDDDDD),
                fontSize = 13.sp
            )
        }
    }
}
