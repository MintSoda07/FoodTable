package com.bcu.foodtable.JetpackCompose.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 주어진 영역의 터치를 모두 흡수해 하위로 내려가지 않게 막는 레이어.
 */
@Composable
fun InteractionBlocker(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            // 모든 포인터 이벤트 선점/소비
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            // 클릭도 흡수 (시각적 표시 없음)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { /* swallow */ }
            .fillMaxSize()
    )
}
