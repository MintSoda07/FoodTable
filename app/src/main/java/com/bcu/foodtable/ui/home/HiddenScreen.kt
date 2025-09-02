// =====================================================
// HiddenScreen_Animated_OGG.kt — UX 업그레이드 + OGG 사운드 (FIXED)
//  - 액티비티 분리로 네비게이터 간섭 제거 (SlotActivity에서 사용)
//  - Firebase Realtime DB 포인트 증감 (user/{uid}/point)
//  - 사운드 로딩 콜백 경합 수정, safeDrawing 인셋 적용
// =====================================================
package com.bcu.foodtable.ui.home

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// ---------------------------
// 심볼 / 점수 / 판정 로직
// ---------------------------
private val SYMBOLS = listOf("🍒", "🍋", "⭐", "🍉", "🔔", "💎", "🍀", "7️⃣")
private val SYMBOL_WEIGHTS = mapOf(
    "🍒" to 28, "🍋" to 26, "⭐" to 18, "🍉" to 22,
    "🔔" to 20, "💎" to 12, "🍀" to 10, "7️⃣" to 6
)
private val SYMBOL_SCORE = mapOf(
    "🍒" to 10, "🍋" to 8, "⭐" to 20, "🍉" to 12,
    "🔔" to 15, "💎" to 30, "🍀" to 25, "7️⃣" to 50
)

private data class SpinResult(
    val base: Int, val multiplier: Double, val bonus: Int,
    val total: Int, val pattern: String
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

    val bonus = (if (centerRowSymbols.contains("🍀")) 5 else 0) +
            (centerRowSymbols.count { it == "7️⃣" } * 20)

    if (centerRowSymbols.all { it == "7️⃣" }) {
        pattern = "JACKPOT"; multiplier = 12.0
    }
    val total = ((base + bonus) * multiplier).roundToInt()
    return SpinResult(base, multiplier, bonus, total, pattern)
}

// ---------------------------------
// Firebase 포인트 리포지토리
// ---------------------------------
private class PointsRepository(
    private val db: DatabaseReference,
    private val uid: String
) {
    private fun ref() = db.child("user").child(uid).child("point")

    suspend fun adjust(delta: Int) = withContext(Dispatchers.IO) {
        ref().runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val cur = (currentData.getValue(Int::class.java) ?: 0)
                currentData.value = cur + delta
                return Transaction.success(currentData)
            }
            override fun onComplete(
                error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?
            ) { /* no-op */ }
        })
    }
}

// ---------------------------------
// 사운드: 효과음(SoundPool) + 음악(MediaPlayer)
// ---------------------------------
private class SoundEngine(
    private val soundPool: SoundPool,
    private val music: MediaPlayer,
    private val ids: Ids
) {
    data class Ids(
        val lever: Int, val spinStart: Int, val tick: Int,
        val stop: Int, val winLow: Int, val fail: Int
    )
    private val loadedIdSet = mutableSetOf<Int>()
    var allLoaded = false; private set
    private var timeoutGate = false
    fun allowAfterTimeout() { timeoutGate = true }
    var soundsEnabled = true
    var musicEnabled = true
    private fun canPlay(): Boolean = soundsEnabled && (allLoaded || timeoutGate)

    fun onLoaded(sampleId: Int) {
        loadedIdSet += sampleId
        allLoaded = loadedIdSet.containsAll(
            setOf(ids.lever, ids.spinStart, ids.tick, ids.stop, ids.winLow, ids.fail)
        )
    }
    fun playLever()     { if (canPlay()) soundPool.play(ids.lever,     1f,   1f,   1, 0, 1f) }
    fun playSpinStart() { if (canPlay()) soundPool.play(ids.spinStart, 0.95f,0.95f,1, 0, 1f) }
    fun playTick()      { if (canPlay()) soundPool.play(ids.tick,      0.30f,0.30f,1, 0, 1f) }
    fun playStop()      { if (canPlay()) soundPool.play(ids.stop,      0.75f,0.75f,1, 0, 1f) }
    fun playWinLow()    { if (canPlay()) soundPool.play(ids.winLow,    1f,   1f,   1, 0, 1f) }
    fun playFail()      { if (canPlay()) soundPool.play(ids.fail,      0.9f, 0.9f, 1, 0, 1f) }
    fun startMusic() {
        if (!musicEnabled) return
        try { if (!music.isPlaying) { music.isLooping = true; music.start() } } catch (_: Throwable) {}
    }
    fun stopMusic() { try { if (music.isPlaying) music.pause(); music.seekTo(0) } catch (_: Throwable) {} }
    fun release()   { try { soundPool.release() } catch (_: Throwable) {}; try { music.release() } catch (_: Throwable) {} }
}

@Composable
private fun rememberSoundEngine(): SoundEngine {
    val context = LocalContext.current
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
    }
    val preLoaded = remember { mutableStateListOf<Int>() }
    var engineRef by remember { mutableStateOf<SoundEngine?>(null) }

    DisposableEffect(Unit) {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                engineRef?.onLoaded(sampleId) ?: preLoaded.add(sampleId)
            }
        }
        onDispose { engineRef?.release(); engineRef = null }
    }

    val music = remember {
        MediaPlayer.create(context, R.raw.slot_music).apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            isLooping = true
            setVolume(0.35f, 0.35f)
        }
    }

    val ids = remember {
        SoundEngine.Ids(
            lever     = soundPool.load(context, R.raw.slot_lever, 1),
            spinStart = soundPool.load(context, R.raw.slot_spin,  1),
            tick      = soundPool.load(context, R.raw.slot_tick,  1),
            stop      = soundPool.load(context, R.raw.slot_stop,  1),
            winLow    = soundPool.load(context, R.raw.slot_win,   1),
            fail      = soundPool.load(context, R.raw.slot_fail,  1)
        )
    }

    val engine = remember { SoundEngine(soundPool, music, ids).also { engineRef = it } }
    LaunchedEffect(preLoaded) { preLoaded.forEach { engine.onLoaded(it) }; preLoaded.clear() }
    return engine
}

// ---------------------------
// 릴 상태/애니메이션
// ---------------------------
private class ReelState(indexInit: Int) {
    var index by mutableIntStateOf(indexInit)
    val offsetPx = Animatable(0f)
}
@Composable
private fun Animatable<Float, *>.asState(): State<Float> {
    val s = produceState(initialValue = value, this) {
        snapshotFlow { this@asState.value }.collect { value = it }
    }
    return s
}

// ---------------------------
// 3행 뷰포트 릴 뷰
// ---------------------------
@Composable
private fun ReelView(
    symbolList: List<String>,
    index: Int,
    offsetPx: Float,
    cellPx: Float,
    cellDp: Dp,
    highlight: Boolean
) {
    val prev = symbolList[(index - 1 + symbolList.size) % symbolList.size]
    val curr = symbolList[index % symbolList.size]
    val next = symbolList[(index + 1) % symbolList.size]

    val neon = Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A)))
    val frame = Color(0xFFFFEC6E)

    Box(
        modifier = Modifier
            .size(cellDp, cellDp * 3f)
            .clip(RoundedCornerShape(14.dp))
            .background(neon)
            .border(2.dp, frame.copy(0.9f), RoundedCornerShape(14.dp))
            .graphicsLayer { clip = true },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.offset { IntOffset(0, -offsetPx.roundToInt()) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            ReelCell(prev, cellDp, false)
            ReelCell(curr, cellDp, highlight)
            ReelCell(next, cellDp, false)
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(0.50f),
                        0.18f to Color.Transparent,
                        0.82f to Color.Transparent,
                        1f to Color.Black.copy(0.50f)
                    )
                )
        )
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = 0.35f }
                .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(14.dp))
        )
    }
}
@Composable
private fun ReelCell(symbol: String, cellDp: Dp, highlight: Boolean) {
    val ring = if (highlight) Brush.radialGradient(
        listOf(Color(0x33FFFFAA), Color.Transparent)
    ) else null

    Box(
        modifier = Modifier
            .size(cellDp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (highlight && ring != null) {
            Box(
                modifier = Modifier
                    .size(cellDp * 0.9f)
                    .clip(CircleShape)
                    .background(ring)
            )
        }
        Text(
            text = symbol,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

// ---------------------------
// 잭팟 폭죽 (Lottie)
// ---------------------------
@Composable
private fun FireworksOverlay(visible: Boolean) {
    AnimatedVisibility(visible, enter = fadeIn(tween(220)), exit = fadeOut(tween(220))) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.cash)
        )
        val progress by animateLottieCompositionAsState(
            composition, iterations = LottieConstants.IterateForever
        )
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize().alpha(0.9f)
        )
    }
}

// =====================================================
// ✨ 애니메이션 버튼/위젯
// =====================================================
@Composable
private fun NeonPulse(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFE36E),
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val t = rememberInfiniteTransition(label = "neon")
    val a by t.animateFloat(
        initialValue = 0.35f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier
            .border(2.dp, color.copy(alpha = a), shape)
            .shadow(
                elevation = 12.dp,
                ambientColor = color.copy(alpha = a * 0.25f),
                spotColor = color.copy(alpha = a * 0.25f),
                shape = shape
            )
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedSpinButton(
    enabled: Boolean,
    turbo: Boolean,
    bet: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "scale"
    )

    val t = rememberInfiniteTransition(label = "ring")
    val sweep: Float by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (turbo) 1500 else 2800, easing = LinearEasing)
        ),
        label = "sweep"
    )

    val baseGrad = Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFFF6B6B)))

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) baseGrad else Brush.linearGradient(listOf(Color.DarkGray, Color.Gray)))
            .border(2.dp, Color.White.copy(if (enabled) 0.18f else 0.08f), RoundedCornerShape(18.dp))
            .then(
                if (enabled) Modifier.clickable(interactionSource = interaction, indication = null) {
                    pressed = true
                    scope.launch { delay(80); pressed = false }
                    onClick()
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val r = min(w, h) / 2f - 6.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.2f to Color.White.copy(0.35f),
                    0.5f to Color.Transparent,
                    1f to Color.Transparent
                ),
                startAngle = sweep,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset((w - r * 2) / 2, (h - r * 2) / 2),
                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style = Stroke(width = 6f)
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (enabled) if (turbo) "TURBO SPIN" else "SPIN" else "LOADING…",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Spacer(Modifier.width(8.dp))
            Text("(Bet $bet)", color = Color.White.copy(0.85f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun AnimatedRoundInfoButton(
    label: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFFFFE36E)
) {
    val t = rememberInfiniteTransition(label = "info")
    val glow by t.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val rotation by t.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing)),
        label = "rot"
    )

    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1C1C1C), Color(0xFF101010))))
            .border(2.dp, accent.copy(0.5f + glow * 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).graphicsLayer { rotationZ = rotation }) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(color = accent.copy(0.25f))
                    drawCircle(color = Color.White.copy(0.15f), radius = size.minDimension / 2.6f)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

// =====================================================
// HiddenScreen 본체
// =====================================================
@Composable
fun HiddenScreen() {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val engine = rememberSoundEngine()
    val scope = rememberCoroutineScope()

    val uid = remember { UserManager.getUser()!!.uid } // 요청대로 사용
    val pointsRepo = remember {
        PointsRepository(FirebaseDatabase.getInstance().reference, uid)
    }

    val reelCount = 5
    val reels = remember { List(reelCount) { ReelState(Random.nextInt(SYMBOLS.size)) } }

    var credits by remember { mutableIntStateOf(UserManager.getUser()?.point ?: 0) }
    // 서버 포인트를 시작 시 한 번 동기화 (존재하지 않거나 0이면 '덮어쓰지 않음' 또는 서버에 기록)
    LaunchedEffect(uid) {
        val ref = FirebaseDatabase.getInstance().reference
            .child("user").child(uid).child("point")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val serverVal = snapshot.getValue(Int::class.java)
                val localInit = UserManager.getUser()?.point ?: credits

                when {
                    serverVal == null -> {
                        // 서버에 값이 없으면 '로컬 기본값'을 서버에 기록
                        ref.setValue(localInit)
                        credits = localInit
                    }
                    serverVal <= 0 && localInit > 0 -> {
                        // 서버가 0이지만 로컬 기본값이 있다면, 서버를 로컬로 보정 (원하면 주석 처리)
                        ref.setValue(localInit)
                        credits = localInit
                    }
                    else -> {
                        // 정상 동기화
                        credits = serverVal
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(ctx, "포인트 동기화 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    var bet by remember { mutableIntStateOf(10) }
    var autoSpins by remember { mutableIntStateOf(0) }
    var turbo by remember { mutableStateOf(false) }
    var soundsEnabled by remember { mutableStateOf(true) }
    var musicEnabled by remember { mutableStateOf(true) }
    var uiVolume by remember { mutableIntStateOf(7) }
    var isSpinning by remember { mutableStateOf(false) }
    var showFireworks by remember { mutableStateOf(false) }

    var totalScore by remember { mutableIntStateOf(0) }
    var lastWin by remember { mutableIntStateOf(0) }
    var lastPattern by remember { mutableStateOf("—") }
    var lastMultiplier by remember { mutableDoubleStateOf(1.0) }

    // 서버 포인트를 시작 시 한 번 동기화
    LaunchedEffect(uid) {
        val ref = FirebaseDatabase.getInstance().reference
            .child("user").child(uid).child("point")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val p = snapshot.getValue(Int::class.java) ?: 0
                credits = p
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(ctx, "포인트 동기화 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    val bgAnim = rememberInfiniteTransition(label = "neon")
    val glowShift by bgAnim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4200, easing = LinearEasing)),
        label = "shift"
    )

    LaunchedEffect(soundsEnabled) { engine.soundsEnabled = soundsEnabled }
    LaunchedEffect(musicEnabled)  { engine.musicEnabled  = musicEnabled; if (!musicEnabled) engine.stopMusic() }

    LaunchedEffect(Unit) {
        val am = ctx.getSystemService(AudioManager::class.java)
        val vol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        if (vol == 0) Toast.makeText(ctx, "기기 미디어 볼륨이 0입니다 🔇", Toast.LENGTH_SHORT).show()
    }

    // 오디오 로딩 최대 6초 타임아웃
    var audioGateOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withTimeoutOrNull(6000L) {
            snapshotFlow { engine.allLoaded }.first { it }
        }
        audioGateOpen = true
        if (!engine.allLoaded) engine.allowAfterTimeout()
    }
    val showAudioLoading = !(engine.allLoaded || audioGateOpen)

    fun centerSymbols(): List<String> = reels.map { SYMBOLS[it.index % SYMBOLS.size] }

    suspend fun spinOneReel(
        reel: ReelState, cellPx: Float, baseDur: Long, steps: Int, turbo: Boolean,
        onTick: () -> Unit, onStop: () -> Unit
    ) {
        repeat(steps) { step ->
            val dur = (baseDur + step * if (turbo) 1L else 5L).toInt()
            reel.offsetPx.snapTo(0f)
            reel.offsetPx.animateTo(cellPx, animationSpec = tween(dur, easing = LinearEasing))
            reel.index = (reel.index + 1) % SYMBOLS.size
            reel.offsetPx.snapTo(0f)
            onTick()
        }
        onStop()
        reel.offsetPx.animateTo(cellPx * 0.18f, tween(80,0, FastOutLinearInEasing))
        reel.offsetPx.animateTo(0f, tween(100, 0,LinearEasing))
    }
    suspend fun doSpin(cellPx: Float) {
        if (isSpinning) return
        if (!audioGateOpen) {
            Toast.makeText(ctx, "오디오 로딩 중… 잠시만요", Toast.LENGTH_SHORT).show()
            return
        }
        if (credits < bet) {
            Toast.makeText(ctx, "크레딧이 부족합니다", Toast.LENGTH_SHORT).show()
            return
        }

        isSpinning = true
        showFireworks = false

        // 1) 베팅 차감 (로컬 + 서버)
        credits -= bet
        try {
            pointsRepo.adjust(-bet)
        } catch (t: Throwable) {
            Toast.makeText(ctx, "포인트 차감 실패: ${t.message}", Toast.LENGTH_SHORT).show()
        }

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        engine.playLever(); engine.playSpinStart(); engine.startMusic()

        // 2) 릴 회전
        coroutineScope {
            val jobs: List<Job> = reels.mapIndexed { i, reel ->
                launch {
                    val steps = (if (turbo) 14 else 22) + i * (if (turbo) 4 else 7)
                    val base = if (turbo) 38L else 70L
                    spinOneReel(
                        reel = reel, cellPx = cellPx, baseDur = base, steps = steps, turbo = turbo,
                        onTick = { engine.playTick() }, onStop = { engine.playStop() }
                    )
                }
            }
            jobs.joinAll()
        }

        // 3) 결과 계산
        val result = evaluateSpin(centerSymbols())

        // --- 지급금 계산 규칙 ---
        // 기본: 결과점수 / (베팅 + 6)   (베팅 10 → 분모 16)
        // 잭팟/5종류: 최소 보장 (잭팟 15배, 5종류 8배)
        val baseScore = result.total
        var payout = (baseScore / (bet + 6)).coerceAtLeast(0)

        when (result.pattern) {
            "JACKPOT" -> payout = max(payout, bet * 15)
            "5 of a Kind" -> payout = max(payout, bet * 8)
        }
        // -----------------------

        // 4) UI 업데이트는 '지급금' 기준으로
        lastWin = payout
        lastPattern = result.pattern
        lastMultiplier = result.multiplier
        totalScore += payout
        credits += payout

        // 5) 서버 포인트 적립(지급금만큼)
        if (payout > 0) {
            try {
                pointsRepo.adjust(payout)
            } catch (t: Throwable) {
                Toast.makeText(ctx, "포인트 적립 실패: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 6) 사운드/이펙트
        engine.stopMusic()
        when {
            result.pattern == "JACKPOT" || result.pattern == "5 of a Kind" -> {
                engine.playWinLow()
                showFireworks = true
                withContext(Dispatchers.Main) { delay(2800); showFireworks = false }
            }
            payout > 0 -> engine.playWinLow()
            else -> engine.playFail()
        }

        isSpinning = false
    }


    // 규칙 다이얼로그 상태
    var showRules by remember { mutableStateOf(false) }
    var rulesTabInit by remember { mutableIntStateOf(0) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = Color.Black
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val maxW = constraints.maxWidth.toFloat()
            val maxH = constraints.maxHeight.toFloat()
            val density = LocalDensity.current

            val cellPx = min(maxW / (reelCount + 1.4f), maxH / 6.5f)
            val cellDp = with(density) { cellPx.toDp() }

            // 배경 네온
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2B1F5E).copy(0.42f),
                                Color(0xFF0D0D0D).copy(0.92f)
                            ),
                            center = Offset(
                                x = maxW * (0.28f + 0.44f * glowShift),
                                y = maxH * (0.35f + 0.22f * (1 - glowShift))
                            ),
                            radius = maxW * 0.95f
                        )
                    )
            )

            // 상단 고정 + 하단 스크롤
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // ─────────── 상단: 고정 영역 ───────────
                Column(
                    modifier = Modifier.wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🎰 SECRET SLOT MACHINE 🎰",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color(0xFFFFE36E), fontWeight = FontWeight.ExtraBold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))

                    if (showAudioLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 180.dp)
                                    .height(6.dp),
                                trackColor = Color.White.copy(0.08f),
                                color = Color(0xFFFFE36E)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("오디오 로딩 중…", color = Color(0xFFCCCCCC), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // 규칙 버튼
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                        AnimatedRoundInfoButton(
                            label = "과일",
                            emoji = "🍒",
                            onClick = {
                                rulesTabInit = 0
                                showRules = true
                            }
                        )
                        AnimatedRoundInfoButton(
                            label = "패턴",
                            emoji = "🧩",
                            accent = Color(0xFF76E3FF),
                            onClick = {
                                rulesTabInit = 1
                                showRules = true
                            }
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // 릴 박스
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF171717), Color(0xFF101010))
                                )
                            )
                            .border(2.dp, Color(0xFFFFE36E).copy(0.65f), RoundedCornerShape(18.dp))
                            .padding(vertical = 12.dp, horizontal = 10.dp)
                    ) {
                        Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            reels.forEach { reel ->
                                val offset by reel.offsetPx.asState()
                                ReelView(
                                    symbolList = SYMBOLS,
                                    index = reel.index,
                                    offsetPx = offset,
                                    cellPx = cellPx,
                                    cellDp = cellDp,
                                    highlight = true
                                )
                            }
                        }
                        Box(
                            Modifier
                                .matchParentSize()
                                .graphicsLayer { alpha = 0.35f }
                        ) {
                            Divider(
                                color = Color.White.copy(0.28f),
                                thickness = 2.dp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                // ─────────── 하단: 스크롤 영역 ───────────
                val bottomScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(bottomScroll)
                        .padding(top = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ControlsRow(
                        credits = credits,
                        bet = bet,
                        onBetChange = { bet = max(1, it.coerceAtMost(500)) },
                        soundsEnabled = soundsEnabled,
                        onToggleSound = { soundsEnabled = it },
                        musicEnabled = musicEnabled,
                        onToggleMusic = { musicEnabled = it },
                        turbo = turbo,
                        onToggleTurbo = { turbo = it },
                        autoSpins = autoSpins,
                        onAuto = { count -> if (!isSpinning && audioGateOpen) autoSpins = count },
                        volumeHint = uiVolume,
                        onVolumeHint = { uiVolume = it }
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                        AnimatedSpinButton(
                            enabled = (audioGateOpen && !isSpinning),
                            turbo = turbo,
                            bet = bet,
                            onClick = {
                                if (isSpinning) return@AnimatedSpinButton
                                scope.launch {
                                    doSpin(cellPx)
                                    if (autoSpins > 0) {
                                        repeat(autoSpins) {
                                            if (credits < bet) return@repeat
                                            delay(if (turbo) 120L else 320L)
                                            doSpin(cellPx)
                                        }
                                        autoSpins = 0
                                    }
                                }
                            }
                        )

                        OutlinedButton(
                            onClick = {
                                if (isSpinning || !audioGateOpen) return@OutlinedButton
                                autoSpins = 10
                                scope.launch {
                                    repeat(10) {
                                        if (credits < bet) return@repeat
                                        doSpin(cellPx)
                                        delay(if (turbo) 120L else 320L)
                                    }
                                    autoSpins = 0
                                }
                            },
                            enabled = (audioGateOpen && !isSpinning)
                        ) { Text("AUTO ×10") }
                    }

                    Spacer(Modifier.height(14.dp))

                    ResultPanel(lastWin = lastWin, lastPattern = lastPattern, lastMultiplier = lastMultiplier)
                    Spacer(Modifier.height(10.dp))
                    ScorePanel(totalScore = totalScore)
                    Spacer(Modifier.height(24.dp))

                    Text(
                        "리소스는 모두 .ogg로 준비해 주세요 (slot_*.ogg)",
                        color = Color(0xFF888888),
                        fontSize = 11.sp,
                        modifier = Modifier.animateContentSize()
                    )
                }
            }

            FireworksOverlay(showFireworks)
        }
    }

    // 규칙 다이얼로그 (최상위)
    RulesDialog(show = showRules, tabInit = rulesTabInit, onDismiss = { showRules = false })
}

// ---------------------------
// UI 패널/컨트롤
// ---------------------------
@Composable
private fun ControlsRow(
    credits: Int,
    bet: Int,
    onBetChange: (Int) -> Unit,
    soundsEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    musicEnabled: Boolean,
    onToggleMusic: (Boolean) -> Unit,
    turbo: Boolean,
    onToggleTurbo: (Boolean) -> Unit,
    autoSpins: Int,
    onAuto: (Int) -> Unit,
    volumeHint: Int,
    onVolumeHint: (Int) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xFF111111))
                .padding(12.dp)
        ) {
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Credits: $credits", color = Color.White, fontWeight = FontWeight.Black)
                Text("Bet: $bet", color = Color(0xFFFFE36E), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onBetChange(max(1, bet - 5)) }) { Text("-5") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onBetChange(max(1, bet - 1)) }) { Text("-1") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onBetChange(bet + 1) }) { Text("+1") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onBetChange(bet + 5) }) { Text("+5") }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    Text("Turbo", color = Color(0xFFCCCCCC), fontSize = 12.sp)
                    Switch(checked = turbo, onCheckedChange = onToggleTurbo)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                Text("Sound", color = Color(0xFFCCCCCC), fontSize = 12.sp)
                Switch(checked = soundsEnabled, onCheckedChange = onToggleSound)
                Text("Music", color = Color(0xFFCCCCCC), fontSize = 12.sp)
                Switch(checked = musicEnabled, onCheckedChange = onToggleMusic)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { onAuto(10) }) { Text("Auto 10") }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("볼륨(안내)", color = Color(0xFF999999), fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = volumeHint / 15f,
                    onValueChange = { onVolumeHint((it * 15f).roundToInt()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
// 베팅 금액을 반영한 실제 지급 금액 계산
private fun calculatePayout(bet: Int, res: SpinResult): Int {
    // 과도한 값 방지를 위해 INT 범위에 맞춰 클램프 (필요 없으면 제거해도 OK)
    return (res.total.toLong() * bet.toLong())
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()
}
@Composable
private fun ResultPanel(lastWin: Int, lastPattern: String, lastMultiplier: Double) {
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
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
        ) {
            Text("Last Win", color = Color(0xFFAAAAAA), fontSize = 12.sp)
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("$lastWin", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${String.format("x%.1f", lastMultiplier)}  •  $lastPattern",
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
            Text("$totalScore", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}
