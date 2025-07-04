package com.bcu.foodtable.JetpackCompose.Subscribe

import androidx.annotation.RawRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R
import com.github.tehras.charts.bar.*
import com.github.tehras.charts.bar.renderer.bar.SimpleBarDrawer
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
import com.github.tehras.charts.bar.renderer.xaxis.SimpleXAxisDrawer
import com.github.tehras.charts.bar.renderer.yaxis.SimpleYAxisDrawer
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import com.github.tehras.charts.piechart.animation.simpleChartAnimation
import kotlin.math.roundToInt

@Composable
fun ChannelManagementScreen(
    channelName: String,
    viewModel: ChannelViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val stats by remember { derivedStateOf { viewModel.stats } }
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }
    var selectedTab by remember { mutableStateOf(0) }
    var periodType by remember { mutableStateOf("월간") } // 일간/월간/연간
    val periodOptions = listOf("일간", "월간", "연간")
    val showCount = when (periodType) {
        "일간" -> 7
        "월간" -> 12
        else -> 10
    }

    val accentColors = listOf(
        Color(0xFF1976D2), // 조회수
        Color(0xFFF9A825), // 좋아요
        Color(0xFF00BFAE), // 구매수
        Color(0xFF6A1B9A), // 업로드수
        Color(0xFFE53935), // 수익
    )
    val metricTabs = listOf("조회수", "좋아요", "구매수", "업로드수", "수익")

    LaunchedEffect(channelName) { viewModel.loadChannelStats(channelName) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (isLoading) {
            LottieLoading(R.raw.chart)
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 헤더
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = channelName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "채널 대시보드",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    Surface(
                        color = Color(0xFF282B37),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.shadow(2.dp)
                    ) {
                        Text(
                            text = nowDateString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // 상단 카드 요약 + 변화율
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    metricTabs.forEachIndexed { idx, metric ->
                        val value = when (idx) {
                            0 -> stats.totalViews
                            1 -> stats.totalLikes
                            2 -> stats.totalPurchases
                            3 -> stats.totalUploads
                            else -> stats.totalRevenue
                        }
                        val changePercent = stats.percentChange.getOrNull(idx) ?: 0f
                        MetricSummaryCardPro(
                            metric,
                            value,
                            accentColors[idx],
                            highlight = selectedTab == idx,
                            percent = changePercent,
                            onClick = { selectedTab = idx }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // ----------- 분류 토글버튼 --------------
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    periodOptions.forEach { option ->
                        val selected = periodType == option
                        val animatedColor by animateColorAsState(
                            if (selected) accentColors[selectedTab] else Color.LightGray
                        )
                        Text(
                            option,
                            color = animatedColor,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clickable { periodType = option }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                // ----------- 동적 차트+트렌드 -------------
                DynamicChartSection(
                    stats = stats,
                    selectedTab = selectedTab,
                    accentColors = accentColors,
                    metricTabs = metricTabs,
                    periodType = periodType,
                    showCount = showCount
                )

                Spacer(Modifier.height(18.dp))

                // ----------- 동적 진행률, 파이차트 -----------
                DynamicAdvancedMetricsSection(
                    stats = stats,
                    accentColors = accentColors,
                    periodType = periodType,
                    showCount = showCount
                )
            }
        }
    }
}

// --------- 동적 바 차트+증감률 ---------
@Composable
fun DynamicChartSection(
    stats: ChannelStats,
    selectedTab: Int,
    accentColors: List<Color>,
    metricTabs: List<String>,
    periodType: String,
    showCount: Int
) {
    val color = accentColors[selectedTab]
    val chartTitle = when (periodType) {
        "일간" -> "최근 7일"
        "월간" -> "최근 12개월"
        else -> "최근 10년"
    }

    val (labels, valuesMap) = when (periodType) {
        "일간" -> stats.dailyLabels to listOf(
            stats.dailyViews, stats.dailyLikes, stats.dailyPurchases, stats.dailyUploads, stats.dailyRevenue
        )[selectedTab]
        "월간" -> stats.monthlyLabels to listOf(
            stats.monthlyViews, stats.monthlyLikes, stats.monthlyPurchases, stats.monthlyUploads, stats.monthlyRevenue
        )[selectedTab]
        else -> stats.yearlyLabels to listOf(
            stats.yearlyViews, stats.yearlyLikes, stats.yearlyPurchases, stats.yearlyUploads, stats.yearlyRevenue
        )[selectedTab]
    }

    // 항상 showCount개만 보여주기 (최근 n개)
    val slicedLabels = labels.toList().takeLast(showCount)
    val data = slicedLabels.associateWith { valuesMap.getOrElse(labels.indexOf(it)) { "0" }.toFloatOrNull() ?: 0f }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$chartTitle ${metricTabs[selectedTab]}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                TrendChip(
                    percent = stats.percentChange.getOrNull(selectedTab) ?: 0f,
                    color = color
                )
            }
            Spacer(Modifier.height(8.dp))
            AnimatedBarChart(
                data = data,
                color = color,
                labelFormatter = {
                    // 날짜 포맷 일/월/연도별 보기 쉽게!
                    when (periodType) {
                        "일간" -> it.substringAfter("-").replace("-", "/") // MM-dd or yy-MM-dd → MM/dd
                        "월간" -> it.substringAfter("-") // MM or yy-MM
                        else -> it // 연도
                    }
                }
            )
        }
    }
}

// --------- 동적 프로그래스바+PieChart ---------
@Composable
fun DynamicAdvancedMetricsSection(
    stats: ChannelStats,
    accentColors: List<Color>,
    periodType: String,
    showCount: Int
) {
    Spacer(Modifier.height(4.dp))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "이번 기간 메트릭별 달성률",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))

            val (labels, metrics, maxTargets) = when (periodType) {
                "일간" -> Triple(stats.dailyLabels, stats.dailyMetrics, stats.maxTargets)
                "월간" -> Triple(stats.monthlyLabels, stats.monthlyMetrics, stats.maxTargets)
                else -> Triple(stats.yearlyLabels, stats.yearlyMetrics, stats.maxTargets)
            }
            val lastIdx = (labels.size - 1).coerceAtLeast(0)
            val latestMetric = if (metrics.isNotEmpty() && lastIdx < metrics[0].size) {
                metrics.map { it.getOrElse(lastIdx) { "0" }.toFloatOrNull() ?: 0f }
            } else {
                List(5) { 0f }
            }

            listOf("조회수", "좋아요", "구매수", "업로드수", "수익").forEachIndexed { i, name ->
                val value = latestMetric.getOrNull(i) ?: 0f
                val maxTarget = maxTargets.getOrNull(i)?.takeIf { it > 0 } ?: value
                val ratio = if (maxTarget == 0f) 0f else (value / maxTarget)
                MetricProgressBar(
                    name, value, ratio, accentColors[i]
                )
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(12.dp))

            // PieChart
            Text(
                "카테고리별 비중",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                val pieData = PieChartData(
                    slices = listOf(
                        PieChartData.Slice(latestMetric[2], accentColors[2]),
                        PieChartData.Slice(latestMetric[1], accentColors[1]),
                        PieChartData.Slice(latestMetric[3], accentColors[3]),
                        PieChartData.Slice(latestMetric[0], accentColors[0]),
                        PieChartData.Slice(latestMetric[4], accentColors[4])
                    )
                )
                PieChart(
                    pieChartData = pieData,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    animation = simpleChartAnimation(),
                    sliceDrawer = com.github.tehras.charts.piechart.renderer.SimpleSliceDrawer()
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("구매수", "좋아요", "업로드수", "조회수", "수익").forEachIndexed { i, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(accentColors[i], shape = CircleShape)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(label, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// --------- 고급 카드, 트렌드, 애니/바 차트/진행률 유틸 ---------
@Composable
fun MetricSummaryCardPro(
    title: String,
    value: Number,
    color: Color,
    highlight: Boolean,
    percent: Float,
    onClick: () -> Unit
) {
    val animatedPercent by animateFloatAsState(targetValue = percent, label = "percent")
    val animatedBg by animateColorAsState(
        targetValue = if (highlight) color.copy(alpha = 0.08f) else Color.White,
        label = "cardbg"
    )
    Surface(
        modifier = Modifier
            .width(75.dp)
            .height(82.dp)
            .shadow(if (highlight) 7.dp else 1.dp, RoundedCornerShape(13.dp))
            .border(
                width = if (highlight) 2.dp else 1.dp,
                color = if (highlight) color else color.copy(alpha = 0.25f),
                shape = RoundedCornerShape(13.dp)
            )
            .background(animatedBg)
            .clickable { onClick() },
        shape = RoundedCornerShape(13.dp),
        color = animatedBg
    ) {
        Column(
            Modifier.padding(top = 9.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = color,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = if (value is Float) String.format("%,.0f", value) else value.toString(),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(2.dp))
            ChangeRateBadge(animatedPercent, color)
        }
    }
}

@Composable
fun ChangeRateBadge(percent: Float, color: Color) {
    val display = if (percent >= 0) "+%.1f%%".format(percent) else "%.1f%%".format(percent)
    val bg = if (percent >= 0) color.copy(alpha = 0.13f) else Color(0xFFE53935).copy(alpha = 0.14f)
    val txtColor = if (percent >= 0) color else Color(0xFFE53935)
    Surface(
        shape = CircleShape,
        color = bg,
        border = BorderStroke(0.7.dp, txtColor),
    ) {
        Text(
            display,
            fontSize = 11.sp,
            color = txtColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TrendChip(percent: Float, color: Color) {
    val icon = if (percent >= 0) "▲" else "▼"
    val txtColor = if (percent >= 0) color else Color(0xFFE53935)
    Surface(
        shape = RoundedCornerShape(50),
        color = txtColor.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, txtColor)
    ) {
        Text(
            text = "$icon ${if (percent >= 0) "+" else ""}${percent.roundToInt()}%",
            color = txtColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun AnimatedBarChart(
    data: Map<String, Float>,
    color: Color,
    labelFormatter: (String) -> String = { it }
) {
    val animatedValues = data.mapValues { (_, v) -> animateFloatAsState(v, label = "barAnim").value }
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp),
        contentAlignment = Alignment.Center
    ) {
        BarChart(
            barChartData = BarChartData(
                bars = data.entries.map { (label, _) ->
                    BarChartData.Bar(
                        label = labelFormatter(label),
                        value = animatedValues[label] ?: 0f,
                        color = color
                    )
                }
            ),
            modifier = Modifier.fillMaxSize(),
            animation = simpleChartAnimation(),
            barDrawer = SimpleBarDrawer(),
            xAxisDrawer = SimpleXAxisDrawer(),
            yAxisDrawer = SimpleYAxisDrawer(),
            labelDrawer = SimpleValueDrawer()
        )
    }
}

@Composable
fun MetricProgressBar(
    label: String,
    value: Float,
    ratio: Float,
    color: Color
) {
    val animatedRatio by animateFloatAsState(ratio.coerceIn(0f, 1f), label = "pb")
    Column {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(70.dp)
            )
            Spacer(Modifier.width(8.dp))
            LinearProgressIndicator(
                progress = animatedRatio,
                color = color,
                trackColor = color.copy(alpha = 0.14f),
                modifier = Modifier
                    .height(11.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
            )

            Spacer(Modifier.width(12.dp))
            Text(
                String.format("%,.0f", value),
                fontSize = 13.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun nowDateString(): String = java.time.LocalDate.now().toString()

@Composable
fun LottieLoading(@RawRes resId: Int) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LottieAnimation(composition, progress)
    }
}