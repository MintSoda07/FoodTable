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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.RecipeItem
import com.github.tehras.charts.bar.*
import com.github.tehras.charts.bar.renderer.bar.BarDrawer
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


// ----- Composable Screen -----

// ----- Composable Screen -----

@Composable
fun ChannelManagementScreen(
    channelName: String,
    viewModel: ChannelViewModel = viewModel()
) {
    val stats by remember { derivedStateOf { viewModel.stats } }
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }
    var selectedTab by remember { mutableStateOf(0) }
    val metricTabs = listOf("조회수", "좋아요", "구매수", "업로드수", "수익")

    LaunchedEffect(channelName) { viewModel.loadChannelStats(channelName) }

    if (isLoading) {
        LottieLoading(R.raw.chart)
    } else {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "$channelName 대시보드",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

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

            val chartData = when (selectedTab) {
                0 -> stats.monthlyViews.mapValues { it.value.toFloat() }
                1 -> stats.monthlyLikes.mapValues { it.value.toFloat() }
                2 -> stats.monthlyPurchases.mapValues { it.value.toFloat() }
                3 -> stats.monthlyUploads.mapValues { it.value.toFloat() }
                else -> stats.monthlyRevenue
            }
            ChartWithAxisLabels(chartData, "월별 ${metricTabs[selectedTab]}")

            Spacer(Modifier.height(24.dp))

            CombinedChartSection(stats)
        }
    }
}

@Composable
fun ChartWithAxisLabels(
    data: Map<String, Float>,
    title: String
) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BarChart(
                barChartData = BarChartData(
                    bars = data.entries.map { (key, value) ->
                        BarChartData.Bar(
                            label = key,
                            value = value,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                labelDrawer = SimpleValueDrawer(drawLocation = SimpleValueDrawer.DrawLocation.Inside)
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
    val combined = if (period == 0) {
        stats.monthlyViews.keys.sorted().associateWith { key ->
            stats.monthlyViews[key]!! +
                    stats.monthlyLikes[key]!! +
                    stats.monthlyPurchases[key]!! +
                    stats.monthlyUploads[key]!! +
                    stats.monthlyRevenue[key]!!.toInt()
        }.mapValues { it.value.toFloat() }
    } else {
        stats.monthlyViews.keys.groupBy { it.substring(0, 4) }
            .mapValues { (_, months) ->
                months.sumOf { m ->
                    stats.monthlyViews[m]!! +
                            stats.monthlyLikes[m]!! +
                            stats.monthlyPurchases[m]!! +
                            stats.monthlyUploads[m]!! +
                            stats.monthlyRevenue[m]!!.toInt()
                }
            }.mapValues { it.value.toFloat() }
    }
    ChartWithAxisLabels(combined, if (period == 0) "월별 합산 지표" else "연도별 합산 지표")
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
