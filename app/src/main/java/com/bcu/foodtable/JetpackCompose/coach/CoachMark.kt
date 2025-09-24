@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bcu.foodtable.JetpackCompose.coach

import android.graphics.RectF
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/* ─────────────────────────────────────────────────────────────
 * Target 저장소
 * ───────────────────────────────────────────────────────────── */
@Stable
class CoachTargets {
    data class Anchor(val rect: RectF, val bringer: BringIntoViewRequester?)
    private val map = mutableStateMapOf<String, Anchor>()
    fun put(id: String, rect: RectF, bringer: BringIntoViewRequester? = null) {
        map[id] = Anchor(rect, bringer)
        Log.d("Coach", "put id=$id rect=$rect bringer=${bringer!=null}")
    }
    fun get(id: String): Anchor? = map[id]
    fun contains(id: String) = map.containsKey(id)
}

/** 실제 위치만 저장 (center 보정은 Overlay가 수행) */
fun Modifier.coachTarget(
    id: String,
    targets: CoachTargets,
    bringer: BringIntoViewRequester? = null,
    expandPx: Float = 0f
): Modifier = this
    .then(if (bringer != null) Modifier.bringIntoViewRequester(bringer) else Modifier)
    .onGloballyPositioned { c ->
        val b = c.boundsInWindow()
        val rect = RectF(b.left, b.top, b.right, b.bottom).apply { inset(-expandPx, -expandPx) }
        targets.put(id, rect, bringer)
        Log.d("Coach", "anchor measured id=$id bounds=$b expanded=$rect")
    }

private fun LayoutCoordinates.rootBoundsInWindow(): RectF {
    var node: LayoutCoordinates = this
    while (node.parentLayoutCoordinates != null) node = node.parentLayoutCoordinates!!
    val r: Rect = node.boundsInWindow()
    return RectF(r.left, r.top, r.right, r.bottom)
}

data class CoachStep(
    val id: String,
    val title: String,
    val desc: String,
    val cornerRadiusDp: Float = 16f,
    val tapToNext: Boolean = true,
    /** 이 스텝은 중앙 보정 */
    val center: Boolean = false
)

private enum class BubbleSide { Top, Bottom, Left, Right }

private fun chooseSide(
    target: RectF, canvasWidth: Float, canvasHeight: Float, marginPx: Float
): BubbleSide {
    val topSpace = target.top - marginPx
    val bottomSpace = canvasHeight - target.bottom - marginPx
    val leftSpace = target.left - marginPx
    val rightSpace = canvasWidth - target.right - marginPx
    return listOf(
        BubbleSide.Top to topSpace,
        BubbleSide.Bottom to bottomSpace,
        BubbleSide.Left to leftSpace,
        BubbleSide.Right to rightSpace
    ).maxBy { it.second }.first
}

private fun bubbleRectFor(
    side: BubbleSide,
    target: RectF,
    bubbleW: Float,
    bubbleH: Float,
    canvasW: Float,
    canvasH: Float,
    marginPx: Float
): RectF {
    val (x, y) = when (side) {
        BubbleSide.Top    -> target.centerX() - bubbleW / 2f to target.top - marginPx - bubbleH
        BubbleSide.Bottom -> target.centerX() - bubbleW / 2f to target.bottom + marginPx
        BubbleSide.Left   -> target.left - marginPx - bubbleW to target.centerY() - bubbleH / 2f
        BubbleSide.Right  -> target.right + marginPx to target.centerY() - bubbleH / 2f
    }
    val clampedX = min(max(marginPx, x), canvasW - bubbleW - marginPx)
    val clampedY = min(max(marginPx, y), canvasH - bubbleH - marginPx)
    return RectF(clampedX, clampedY, clampedX + bubbleW, clampedY + bubbleH)
}

private fun arrowPath(side: BubbleSide, target: RectF, bubble: RectF, arrowSize: Float): Path =
    Path().apply {
        when (side) {
            BubbleSide.Top -> {
                val bx = bubble.centerX()
                moveTo(bx - arrowSize, bubble.bottom); lineTo(bx + arrowSize, bubble.bottom)
                lineTo(target.centerX(), target.top); close()
            }
            BubbleSide.Bottom -> {
                val bx = bubble.centerX()
                moveTo(bx - arrowSize, bubble.top); lineTo(bx + arrowSize, bubble.top)
                lineTo(target.centerX(), target.bottom); close()
            }
            BubbleSide.Left -> {
                val by = bubble.centerY()
                moveTo(bubble.right, by - arrowSize); lineTo(bubble.right, by + arrowSize)
                lineTo(target.left, target.centerY()); close()
            }
            BubbleSide.Right -> {
                val by = bubble.centerY()
                moveTo(bubble.left, by - arrowSize); lineTo(bubble.left, by + arrowSize)
                lineTo(target.right, target.centerY()); close()
            }
        }
    }

private fun RectF.isOnOverlay(w: Float, h: Float): Boolean =
    right > 0f && left < w && bottom > 0f && top < h

/* ─────────────────────────────────────────────────────────────
 * CoachmarkOverlay
 * ───────────────────────────────────────────────────────────── */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CoachmarkOverlay(
    screen: CoachScreen,
    steps: List<CoachStep>,
    targets: CoachTargets,
    store: CoachmarkStore,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    scrim: Color = Color.Black.copy(alpha = 0.55f),
    bubbleWidth: Dp = 280.dp,
    bubbleMaxHeight: Dp = 180.dp,
    bubblePadding: Dp = 16.dp,
    margin: Dp = 12.dp,
    arrowSize: Dp = 10.dp,
    bubbleColor: Color = MaterialTheme.colorScheme.surface,
    bubbleBorderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),

    nextScreenInFlow: CoachScreen? = null,
    onRequestNavigate: ((CoachScreen) -> Unit)? = null,
    onTourComplete: (() -> Unit)? = null,

    bottomObstructionDp: Dp = 0.dp,

    lazyListState: LazyListState? = null,
    scrollState: ScrollState? = null,

    /** 코치마크 표시/해제 상태를 상위로 알려 탭/흔들기 등 막을 때 사용 */
    onOverlayActiveChange: ((Boolean) -> Unit)? = null,

    /** ⬅ 하드코딩으로 먼저 '툭' 내려줄 스텝 id들 (예: "sub_reco") */
    forceJumpIds: Set<String> = emptySet()
) {
    if (steps.isEmpty()) return

    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(true) }
    var ready by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val bottomObstructionPx = with(density) { bottomObstructionDp.toPx() }

    var ovLeft by remember { mutableStateOf(0f) }
    var ovTop by remember { mutableStateOf(0f) }
    var ovW by remember { mutableStateOf(0f) }
    var ovH by remember { mutableStateOf(0f) }

    // 상위에 "지금 코치마크가 활성"임을 1회 알림
    var announcedActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (store.isSeen(screen)) {
            visible = false
            onClose()
        } else {
            repeat(120) {
                if (targets.get(steps.first().id) != null) { ready = true; return@repeat }
                delay(16)
            }
            ready = targets.get(steps.first().id) != null
        }
    }

    // 활성화/비활성화 콜백
    LaunchedEffect(ready, visible) {
        if (ready && visible && !announcedActive) {
            onOverlayActiveChange?.invoke(true)   // ▶ 코치마크 시작
            announcedActive = true
        }
    }
    // 안전장치: Composable 사라질 때 false 보장
    DisposableEffect(Unit) {
        onDispose { onOverlayActiveChange?.invoke(false) }
    }

    if (!visible || !ready) return

    val step = steps.getOrNull(current) ?: return
    val anchor = targets.get(step.id) ?: return

    fun RectF.toOverlayLocal() = RectF(
        left - ovLeft, top - ovTop, right - ovLeft, bottom - ovTop
    )

    val corner   = with(density) { step.cornerRadiusDp.dp.toPx() }
    val marginPx = with(density) { margin.toPx() }
    val bubbleW  = with(density) { bubbleWidth.toPx() }
    val bubbleH  = with(density) { bubbleMaxHeight.toPx() }
    val arrowPx  = with(density) { arrowSize.toPx() }

    fun completeThisOverlayAndNext() {
        scope.launch {
            store.setSeen(screen, true)
            visible = false
            onOverlayActiveChange?.invoke(false)  // ▶ 코치마크 종료
            onClose()
            if (nextScreenInFlow != null && onRequestNavigate != null) {
                onRequestNavigate.invoke(nextScreenInFlow)
            } else {
                onTourComplete?.invoke()
            }
        }
    }

    // ⬇ 현재 스텝에 포커스 맞추기 (원문 유지)
    suspend fun focusStep(s: CoachStep) {
        suspend fun latestLocal(): RectF? = targets.get(s.id)?.rect?.toOverlayLocal()

        // 0) 먼저 bringIntoView 시도 (있다면)
        targets.get(s.id)?.bringer?.bringIntoView()

        // 0-1) 타깃이 아직 없으면 ↓ 스캔 스크롤로 구성 유도
        var local = latestLocal()
        if (local == null && (lazyListState != null || scrollState != null)) {
            val viewportH = (ovH - bottomObstructionPx).coerceAtLeast(1f)
            val stepDown = viewportH * 0.66f          // 한 번에 내릴 양
            val maxTries = 10                         // 너무 멀면 중단
            var tries = 0

            while (local == null && tries++ < maxTries) {
                when {
                    lazyListState != null -> lazyListState.animateScrollBy(stepDown)
                    scrollState   != null -> scrollState.animateScrollBy(stepDown)
                }
                delay(32)
                // 매 스텝마다 다시 bringIntoView 시도 (타깃이 생겼을 수도 있으니까)
                targets.get(s.id)?.bringer?.bringIntoView()
                local = latestLocal()
            }

            // 그래도 못 찾으면 (가능하면) 반대로도 조금 스캔
            if (local == null && (lazyListState != null || scrollState != null)) {
                tries = 0
                while (local == null && tries++ < maxTries / 2) {
                    when {
                        lazyListState != null -> lazyListState.animateScrollBy(-stepDown)
                        scrollState   != null -> scrollState.animateScrollBy(-stepDown)
                    }
                    delay(32)
                    targets.get(s.id)?.bringer?.bringIntoView()
                    local = latestLocal()
                }
            }
        }

        // 1) 좌표가 생길 때까지 혹시 몰라 마지막으로 잠깐 더 대기
        if (local == null) {
            repeat(60) {
                local = latestLocal()
                if (local != null) return@repeat
                delay(16)
            }
        }
        val rect = local ?: return

        val viewportH = (ovH - bottomObstructionPx).coerceAtLeast(1f)
        val vCenterY = viewportH / 2f

        if (s.center && (lazyListState != null || scrollState != null)) {
            // 2) 중앙 보정
            var tries = 0
            var cur = rect
            while (tries++ < 6) {
                val dy = cur.centerY() - vCenterY
                if (kotlin.math.abs(dy) < 2f) break
                when {
                    lazyListState != null -> lazyListState.animateScrollBy(dy)
                    scrollState   != null -> scrollState.animateScrollBy(dy)
                }
                delay(16)
                latestLocal()?.let { cur = it }
            }
        } else {
            // 2') 화면 안에만 들여오기
            if (!(rect.right > 0f && rect.left < ovW && rect.top < (ovH - bottomObstructionPx) && rect.bottom > 0f)) {
                val dy = when {
                    rect.bottom < 0f     -> rect.bottom - 24f
                    rect.top > viewportH -> rect.top - viewportH + 24f
                    else -> 0f
                }
                if (dy != 0f) {
                    when {
                        lazyListState != null -> lazyListState.animateScrollBy(dy)
                        scrollState   != null -> scrollState.animateScrollBy(dy)
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { c ->
                val b = c.boundsInWindow()
                ovLeft = b.left; ovTop = b.top; ovW = b.width; ovH = b.height
            }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .background(Color.Transparent)
            .clickable(enabled = step.tapToNext) {
                scope.launch {
                    if (current < steps.lastIndex) {
                        current++
                        delay(1)
                        focusStep(steps[current])
                    } else {
                        completeThisOverlayAndNext()
                    }
                }
            }
    ) {
        LaunchedEffect(step.id, ovLeft, ovTop, ovW, ovH) {
            focusStep(step)
        }

        val rectLocal = anchor.rect.toOverlayLocal()
        val side = remember(rectLocal, ovW, ovH, bottomObstructionPx) {
            chooseSide(rectLocal, ovW, ovH - bottomObstructionPx, marginPx)
        }
        val bubble = remember(rectLocal, side, ovW, ovH, bottomObstructionPx) {
            bubbleRectFor(side, rectLocal, bubbleW, bubbleH, ovW, ovH - bottomObstructionPx, marginPx)
        }

        Canvas(Modifier.fillMaxSize()) {
            drawRect(color = scrim)

            drawRoundRect(
                color = Color.Transparent,
                topLeft = androidx.compose.ui.geometry.Offset(rectLocal.left, rectLocal.top),
                size = androidx.compose.ui.geometry.Size(rectLocal.width(), rectLocal.height()),
                cornerRadius = CornerRadius(corner, corner),
                blendMode = BlendMode.Clear
            )

            withTransform({ translate(rectLocal.left, rectLocal.top) }) {
                drawRoundRect(
                    color = accent.copy(alpha = 0.95f),
                    size = androidx.compose.ui.geometry.Size(rectLocal.width(), rectLocal.height()),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = 3f)
                )
            }

            val tri = arrowPath(side, rectLocal, bubble, arrowPx)
            drawPath(path = tri, color = bubbleColor, style = Fill)
            drawPath(path = tri, color = bubbleBorderColor, style = Stroke(width = 1.2f))
        }

        Surface(
            modifier = Modifier
                .absoluteOffset(
                    x = with(density) { bubble.left.toDp() },
                    y = with(density) { bubble.top.toDp() }
                )
                .width(bubbleWidth)
                .heightIn(max = bubbleMaxHeight),
            color = bubbleColor,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, bubbleBorderColor)
        ) {
            Column(Modifier.padding(bubblePadding)) {
                Text(steps[current].title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    steps[current].desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { completeThisOverlayAndNext() }) { Text("이번 탭 건너뛰기") }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        scope.launch {
                            if (current < steps.lastIndex) {
                                current++; delay(1); focusStep(steps[current])
                            } else completeThisOverlayAndNext()
                        }
                    }) {
                        val hasNext = nextScreenInFlow != null && onRequestNavigate != null
                        Text(
                            when {
                                current < steps.lastIndex -> "다음"
                                hasNext -> "다음 탭으로"
                                else -> "완료"
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
 * 화면 전체 터치/탭 흡수용 (탭 전환, 버튼, 제스처 등 비활성화)
 * ───────────────────────────────────────────────────────────── */
@Composable
fun InteractionBlocker(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    Box(
        modifier
            .fillMaxSize()                 // 화면 전체 덮기
            .background(Color.Transparent) // 시각적 변화 없음
            .pointerInput(Unit) {
                // 모든 포인터 이벤트를 소비해서 아래 컴포저블로 전달되지 않게 함
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    )
}