// screens/SalesDashboardScreen.kt
package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesDashboardScreen(
    storeId: String,
    onBack: () -> Unit
) {
    var range by remember { mutableStateOf(SalesRange.TODAY) }

    // TODO: Replace with Firestore aggregations
    val stats = remember(range) { mockSalesStats(range) }
    val topProducts = remember(range) { mockTopProducts(range) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("매출 관리") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("요약", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = range == SalesRange.TODAY, onClick = { range = SalesRange.TODAY }, label = { Text("오늘") })
                            FilterChip(selected = range == SalesRange.WEEK,  onClick = { range = SalesRange.WEEK  }, label = { Text("최근 7일") })
                            FilterChip(selected = range == SalesRange.MONTH, onClick = { range = SalesRange.MONTH }, label = { Text("최근 30일") })
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard(title = "총 매출", value = "${stats.totalAmount}원", icon = Icons.Default.RequestQuote, modifier = Modifier.weight(1f))
                            StatCard(title = "주문 수", value = "${stats.orderCount}건", icon = Icons.Default.ReceiptLong, modifier = Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard(title = "평균 객단가", value = "${stats.avgOrderAmount}원", icon = Icons.Default.Paid, modifier = Modifier.weight(1f))
                            StatCard(title = "환불/취소", value = "${stats.cancelCount}건", icon = Icons.Default.Cancel, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("인기 상품", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (topProducts.isEmpty()) {
                            EmptyState(icon = Icons.Default.Inventory2, text = "데이터가 없습니다")
                        } else {
                            topProducts.forEach { (name, amount, count) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    AssistChip(onClick = {}, label = { Text("${count}건") }, leadingIcon = { Icon(Icons.Default.ShoppingCart, null) })
                                    Spacer(Modifier.width(8.dp))
                                    Text("${amount}원", style = MaterialTheme.typography.titleSmall)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class SalesRange { TODAY, WEEK, MONTH }

data class SalesStats(
    val totalAmount: Long,
    val orderCount: Int,
    val avgOrderAmount: Long,
    val cancelCount: Int
)

// Mock data (swap out with Firestore)
private fun mockSalesStats(range: SalesRange) = when (range) {
    SalesRange.TODAY -> SalesStats(128_000, 9, 14_222, 1)
    SalesRange.WEEK  -> SalesStats(1_254_000, 78, 16_076, 6)
    SalesRange.MONTH -> SalesStats(5_980_000, 361, 16_565, 29)
}

private fun mockTopProducts(@Suppress("UNUSED_PARAMETER") range: SalesRange): List<Triple<String, Long, Int>> =
    listOf(
        Triple("불고기덮밥", 640_000, 40),
        Triple("치킨마요", 420_000, 28),
        Triple("우동세트", 220_000, 13)
    )
