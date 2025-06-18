package com.bcu.foodtable.JetpackCompose.Subscribe

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R
import com.github.tehras.charts.bar.*
import com.github.tehras.charts.bar.renderer.bar.SimpleBarDrawer
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
import com.github.tehras.charts.bar.renderer.xaxis.SimpleXAxisDrawer
import com.github.tehras.charts.bar.renderer.yaxis.SimpleYAxisDrawer
import com.github.tehras.charts.piechart.animation.simpleChartAnimation
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun ChannelManagementScreen(
    channelName: String,
    viewModel: ChannelViewModel = viewModel()
) {
    val stats by remember { derivedStateOf { viewModel.stats } }
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }
    var selectedTab by remember { mutableStateOf(0) }
    val metricTabs = listOf("조회수", "좋아요", "구매수", "업로드수", "수익")

    LaunchedEffect(channelName) {
        viewModel.loadChannelStats(channelName)
    }

    if (isLoading) {
        LottieLoading(R.raw.chart)
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "$channelName 대시보드",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // 상단 메트릭 선택 탭
            TabRow(selectedTabIndex = selectedTab) {
                metricTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // ── 위쪽: 최근 5개월 차트 ──
            val recentMonths = getLastNMonths(5)
            val rawMap = when (selectedTab) {
                0 -> stats.monthlyViews.mapValues { it.value.toFloat() }
                1 -> stats.monthlyLikes.mapValues { it.value.toFloat() }
                2 -> stats.monthlyPurchases.mapValues { it.value.toFloat() }
                3 -> stats.monthlyUploads.mapValues { it.value.toFloat() }
                else -> stats.monthlyRevenue
            }
            val topChartData = recentMonths.associateWith { rawMap[it] ?: 0f }

            ChartWithAxisLabels(
                data = topChartData,
                title = "월별 ${metricTabs[selectedTab]}"
            )

            Spacer(Modifier.height(24.dp))

            // ── 아래쪽: 메트릭별 분포 차트 ──
            CombinedChartSection(stats)
        }
    }
}

// 최근 n개월("yyyy-MM") 리스트 반환
fun getLastNMonths(n: Int): List<String> {
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    val now = YearMonth.now()
    return (0 until n)
        .map { now.minusMonths(it.toLong()).format(fmt) }
        .reversed()
}

@Composable
fun ChartWithAxisLabels(
    data: Map<String, Float>,
    title: String
) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            BarChart(
                barChartData = BarChartData(
                    bars = data.entries.map { (label, value) ->
                        BarChartData.Bar(
                            label = label,
                            value = value,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ),
                modifier = Modifier.fillMaxSize(),
                animation = simpleChartAnimation(),
                barDrawer = SimpleBarDrawer(),
                xAxisDrawer = SimpleXAxisDrawer(),
                yAxisDrawer = SimpleYAxisDrawer(),
                labelDrawer = SimpleValueDrawer(
                    drawLocation = SimpleValueDrawer.DrawLocation.Inside
                )
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.keys.forEach { key ->
                Text(
                    key,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CombinedChartSection(stats: ChannelStats) {
    var period by remember { mutableStateOf(0) }
    val options = listOf("월별", "연도별")

    Row(Modifier.fillMaxWidth()) {
        options.forEachIndexed { idx, title ->
            TextButton(
                onClick = { period = idx },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontWeight = if (period == idx) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))

    // 최신 키(월 또는 연도) 선택
    val latestKey = remember(stats) {
        if (period == 0) {
            stats.monthlyViews.keys.maxOrNull()
        } else {
            stats.monthlyViews.keys
                .map { it.substring(0, 4) }
                .distinct()
                .maxOrNull()
        }
    }

    val combinedData = if (latestKey != null) {
        if (period == 0) {
            mapOf(
                "조회수" to (stats.monthlyViews[latestKey]?.toFloat() ?: 0f),
                "좋아요" to (stats.monthlyLikes[latestKey]?.toFloat() ?: 0f),
                "구매수" to (stats.monthlyPurchases[latestKey]?.toFloat() ?: 0f),
                "업로드수" to (stats.monthlyUploads[latestKey]?.toFloat() ?: 0f),
                "수익" to (stats.monthlyRevenue[latestKey] ?: 0f)
            )
        } else {
            val months = stats.monthlyViews.keys.filter { it.startsWith(latestKey) }
            val v = months.sumOf { stats.monthlyViews[it]!! }.toFloat()
            val l = months.sumOf { stats.monthlyLikes[it]!! }.toFloat()
            val p = months.sumOf { stats.monthlyPurchases[it]!! }.toFloat()
            val u = months.sumOf { stats.monthlyUploads[it]!! }.toFloat()
            val r = months.sumOf { stats.monthlyRevenue[it]!!.toDouble() }.toFloat()
            mapOf(
                "조회수" to v,
                "좋아요" to l,
                "구매수" to p,
                "업로드수" to u,
                "수익" to r
            )
        }
    } else emptyMap()

    ChartWithAxisLabels(
        data = combinedData,
        title = if (period == 0) "월별 메트릭 분포" else "연도별 메트릭 분포"
    )
}

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
