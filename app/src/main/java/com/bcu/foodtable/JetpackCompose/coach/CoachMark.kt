@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bcu.foodtable.JetpackCompose.coach

import android.graphics.RectF
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/* ─────────────────────────────────────────────────────────────
 * Target 저장소: 좌표 + (있다면) BringIntoViewRequester 함께 저장
 * ───────────────────────────────────────────────────────────── */
@Stable
class CoachTargets {
    data class Anchor(val rect: RectF, val bringer: BringIntoViewRequester?)
    private val map = mutableStateMapOf<String, Anchor>()
    fun put(id: String, rect: RectF, bringer: BringIntoViewRequester? = null) { map[id] = Anchor(rect, bringer) }
    fun get(id: String): Anchor? = map[id]
    fun contains(id: String) = map.containsKey(id)
}

/** 타깃 앵커: 스크롤 부모가 있으면 bringer 넘겨주세요. expandPx로 하이라이트 박스 여유 */
fun Modifier.coachTarget(
    id: String,
    targets: CoachTargets,
    bringer: BringIntoViewRequester? = null,
    expandPx: Float = 0f
): Modifier = this
    .then(if (bringer != null) Modifier.bringIntoViewRequester(bringer) else Modifier)
    .onGloballyPositioned { c: LayoutCoordinates ->
        val b = c.boundsInRoot()
        val rect = RectF(
            b.left - expandPx,
            b.top - expandPx,
            b.right + expandPx,
            b.bottom + expandPx
        )
        targets.put(id, rect, bringer)
    }

data class CoachStep(
    val id: String,
    val title: String,
    val desc: String,
    val cornerRadiusDp: Float = 16f,
    val tapToNext: Boolean = true
)

private enum class BubbleSide { Top, Bottom, Left, Right }

private fun chooseSide(
    target: RectF,
    canvasWidth: Float,
    canvasHeight: Float,
    marginPx: Float
): BubbleSide {
    val topSpace = target.top - marginPx
    val bottomSpace = canvasHeight - target.bottom - marginPx
    val leftSpace = target.left - marginPx
    val rightSpace = canvasWidth - target.right - marginPx
    return listOf(
        BubbleSide.Top    to topSpace,
        BubbleSide.Bottom to bottomSpace,
        BubbleSide.Left   to leftSpace,
        BubbleSide.Right  to rightSpace
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

private fun arrowPath(
    side: BubbleSide,
    target: RectF,
    bubble: RectF,
    arrowSize: Float
): Path = Path().apply {
    when (side) {
        BubbleSide.Top -> {
            val bx = bubble.centerX()
            moveTo(bx - arrowSize, bubble.bottom)
            lineTo(bx + arrowSize, bubble.bottom)
            lineTo(target.centerX(), target.top)
            close()
        }
        BubbleSide.Bottom -> {
            val bx = bubble.centerX()
            moveTo(bx - arrowSize, bubble.top)
            lineTo(bx + arrowSize, bubble.top)
            lineTo(target.centerX(), target.bottom)
            close()
        }
        BubbleSide.Left -> {
            val by = bubble.centerY()
            moveTo(bubble.right, by - arrowSize)
            lineTo(bubble.right, by + arrowSize)
            lineTo(target.left, target.centerY())
            close()
        }
        BubbleSide.Right -> {
            val by = bubble.centerY()
            moveTo(bubble.left, by - arrowSize)
            lineTo(bubble.left, by + arrowSize)
            lineTo(target.right, target.centerY())
            close()
        }
    }
}

private fun RectF.isOnScreen(w: Float, h: Float): Boolean =
    right > 0f && left < w && bottom > 0f && top < h

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
    bubbleBorderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
) {
    if (steps.isEmpty()) return
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(true) }
    var ready by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val cfg = LocalConfiguration.current
    val canvasW = with(density) { cfg.screenWidthDp.dp.toPx() }
    val canvasH = with(density) { cfg.screenHeightDp.dp.toPx() }

    val bubbleFill = bubbleColor
    val bubbleStroke = bubbleBorderColor

    // 1) 처음 진입: 이미 봤으면 스킵, 아니면 첫 타깃 준비될 때까지 대기
    LaunchedEffect(Unit) {
        if (store.isSeen(screen)) {
            visible = false
            onClose()
        } else {
            repeat(90) {
                if (targets.get(steps.first().id) != null) {
                    ready = true
                    return@repeat
                }
                delay(16)
            }
            ready = targets.get(steps.first().id) != null
        }
    }
    if (!visible || !ready) return

    val step = steps.getOrNull(current) ?: return
    val anchor = targets.get(step.id) ?: return
    val rect = anchor.rect
    val corner = with(density) { step.cornerRadiusDp.dp.toPx() }
    val marginPx = with(density) { margin.toPx() }
    val bubbleW = with(density) { bubbleWidth.toPx() }
    val bubbleH = with(density) { bubbleMaxHeight.toPx() }
    val arrowPx = with(density) { arrowSize.toPx() }

    // 2) 스텝이 바뀌면: 타깃이 화면 밖이면 bringIntoView()로 스크롤
    LaunchedEffect(step.id) {
        delay(1) // 좌표 반영 대기 (레이아웃 턴)
        val a = targets.get(step.id)
        if (a != null && !a.rect.isOnScreen(canvasW, canvasH)) {
            a.bringer?.bringIntoView()
            // 스크롤 애니 끝나고 좌표 갱신 기다리기
            repeat(30) {
                delay(16)
                val latest = targets.get(step.id)
                if (latest?.rect?.isOnScreen(canvasW, canvasH) == true) return@repeat
            }
        }
    }

    val side = remember(rect, canvasW, canvasH) { chooseSide(rect, canvasW, canvasH, marginPx) }
    val bubble = remember(rect, side, canvasW, canvasH) {
        bubbleRectFor(side, rect, bubbleW, bubbleH, canvasW, canvasH, marginPx)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .clickable(enabled = step.tapToNext) {
                if (current < steps.lastIndex) current++ else {
                    scope.launch {
                        store.setSeen(screen, true)
                        visible = false
                        onClose()
                    }
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // 어두운 배경
            drawRect(color = scrim)

            // 타깃 구멍 (진짜로 뚫기)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                size = androidx.compose.ui.geometry.Size(rect.width(), rect.height()),
                cornerRadius = CornerRadius(corner, corner),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )
            // 테두리
            withTransform({ translate(rect.left, rect.top) }) {
                drawRoundRect(
                    color = accent.copy(alpha = 0.95f),
                    size = androidx.compose.ui.geometry.Size(rect.width(), rect.height()),
                    cornerRadius = CornerRadius(corner, corner),
                    style = Stroke(width = 3f)
                )
            }
            // 화살표 (버블 → 타깃)
            val tri = arrowPath(side, rect, bubble, arrowPx)
            drawPath(path = tri, color = bubbleFill, style = Fill)
            drawPath(path = tri, color = bubbleStroke, style = Stroke(width = 1.2f))
        }

        // 말풍선 카드 (타깃 옆에 배치)
        Surface(
            modifier = Modifier
                .absoluteOffset(
                    x = with(density) { bubble.left.toDp() },
                    y = with(density) { bubble.top.toDp() }
                )
                .width(bubbleWidth)
                .heightIn(max = bubbleMaxHeight),
            color = bubbleFill,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, bubbleStroke)
        ) {
            Column(Modifier.padding(bubblePadding)) {
                Text(step.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    step.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        scope.launch {
                            store.setSeen(screen, true)
                            visible = false
                            onClose()
                        }
                    }) { Text("이번 탭 건너뛰기") }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        if (current < steps.lastIndex) current++ else {
                            scope.launch {
                                store.setSeen(screen, true)
                                visible = false
                                onClose()
                            }
                        }
                    }) { Text(if (current < steps.lastIndex) "다음" else "완료") }
                }
            }
        }
    }
}
