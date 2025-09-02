@file:Suppress("FunctionName")

package com.bcu.foodtable.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 규칙 다이얼로그 (과일별 점수 / 패턴·보너스)
 * - show: 표시 여부
 * - tabInit: 초기 탭(0=과일 점수, 1=패턴/보너스)
 * - onDismiss: 닫기 콜백
 *
 * 개선 포인트:
 * - 작은 화면에서 콘텐츠가 길 때 스크롤되도록 sizeIn(maxHeight=…) 추가
 * - usePlatformDefaultWidth=false 로 양쪽 여백 최소화(모바일에서 더 넓게)
 * - 다크 카드 + 노란 포인트(#FFE36E) 톤 통일
 */
@Composable
fun RulesDialog(
    show: Boolean,
    tabInit: Int = 0,
    onDismiss: () -> Unit
) {
    if (!show) return

    val accentYellow = Color(0xFFFFE36E)
    val accentBlue = Color(0xFF76E3FF)
    val cardShape = RoundedCornerShape(18.dp)

    var tab by rememberSaveable { mutableIntStateOf(tabInit.coerceIn(0, 1)) }
    val tabs = listOf("과일 점수", "패턴/보너스")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 화면이 작은 기기에서도 스크롤 가능하도록 최대 높이 제한
        Card(
            shape = cardShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, Color.White.copy(0.07f), cardShape)
                .sizeIn(maxHeight = 560.dp) // 필요시 프로젝트 톤에 맞춰 높이 조절
        ) {
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .background(Color(0xFF0F0F0F))
                    .verticalScroll(scroll)
                    .padding(bottom = 8.dp)
            ) {
                // 제목
                Text(
                    text = "점수 규칙 안내",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    color = accentYellow,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )

                Spacer(Modifier.height(10.dp))

                // 탭
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { positions ->
                        val p = positions[tab]
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.BottomStart)
                                .offset(x = p.left)
                                .width(p.width)
                                .height(3.dp)
                                .background(accentYellow)
                        )
                    }
                ) {
                    tabs.forEachIndexed { i, title ->
                        Tab(
                            selected = tab == i,
                            onClick = { tab = i },
                            text = {
                                Text(
                                    title,
                                    color = if (tab == i) accentYellow else Color(0xFFCCCCCC),
                                    fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // 콘텐츠
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                ) { which ->
                    when (which) {
                        0 -> RulesFruitsPage(accentYellow)
                        else -> RulesPatternPage(accentBlue)
                    }
                }

                // 하단 버튼
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("닫기") }
                }
            }
        }
    }
}

/* ────────────────────────────────────────────────────
 * 과일 점수 탭
 * ──────────────────────────────────────────────────── */
@Composable
private fun RulesFruitsPage(accent: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RulesSectionTitle("기호별 점수")

        // 2열 칩 레이아웃
        val pairs = listOf(
            "🍒" to 10, "🍋" to 8, "⭐" to 20, "🍉" to 12,
            "🔔" to 15, "💎" to 30, "🍀" to 25, "7️⃣" to 50
        )
        pairs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (s, v) ->
                    RulesChip(text = "$s  $v", accent = accent)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("희귀도(출현 확률) 가중치", color = Color(0xFFCCCCCC), style = MaterialTheme.typography.labelSmall)
        Text(
            "🍒28  🍋26  ⭐18  🍉22  🔔20  💎12  🍀10  7️⃣6",
            color = Color(0xFFBBBBBB),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/* ────────────────────────────────────────────────────
 * 패턴/보너스 탭
 * ──────────────────────────────────────────────────── */
@Composable
private fun RulesPatternPage(accent: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RulesSectionTitle("패턴 배당")

        listOf(
            "JACKPOT (7️⃣×5)  :  x12",
            "5 of a Kind       :  x10",
            "4 of a Kind       :  x5",
            "Full House        :  x4",
            "3 of a Kind       :  x3",
            "Two Pair          :  x2",
            "One Pair          :  x1.5",
            "No Match          :  x1"
        ).forEach { RulesChip(it, accent) }

        Spacer(Modifier.height(6.dp))
        RulesSectionTitle("보너스")
        RulesChip("🍀 포함 시 +5", accent)
        RulesChip("7️⃣ 1개당 +20", accent)
    }
}

/* ────────────────────────────────────────────────────
 * 공용: 타이틀/칩
 * ──────────────────────────────────────────────────── */
@Composable
private fun RulesSectionTitle(text: String) {
    Text(
        text,
        color = Color.White,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
private fun RulesChip(text: String, accent: Color) {
    Surface(
        color = Color(0xFF1A1A1A),
        contentColor = Color.White,
        shape = RoundedCornerShape(12.dp),
        // Material3 내부 API 의존 없이 심플 보더
        modifier = Modifier
            .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
