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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.roundToLong

enum class SalesRange { TODAY, WEEK, MONTH }

data class SalesStats(
    val totalAmount: Long = 0L,
    val orderCount: Int = 0,
    val avgOrderAmount: Long = 0L,
    val cancelCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesDashboardScreen(
    storeId: String,
    onBack: () -> Unit
) {
    val db = Firebase.firestore
    var range by remember { mutableStateOf(SalesRange.TODAY) }

    // 상태
    var stats by remember { mutableStateOf(SalesStats()) }
    var topProducts by remember { mutableStateOf(listOf<Triple<String, Long, Int>>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 기간 시작시각 계산
    fun rangeStartTs(nowMs: Long = System.currentTimeMillis()): Timestamp {
        val dayMs = 24L * 60 * 60 * 1000
        return when (range) {
            SalesRange.TODAY -> {
                // 로컬 자정으로 내리는 간단 버전 (정확한 TZ 자정 필요하면 java.time 사용)
                val todayStart = nowMs - (nowMs % dayMs)
                Timestamp(todayStart / 1000, 0)
            }
            SalesRange.WEEK -> Timestamp((nowMs - 7L * dayMs) / 1000, 0)
            SalesRange.MONTH -> Timestamp((nowMs - 30L * dayMs) / 1000, 0)
        }
    }

    // 실시간 구독
    DisposableEffect(storeId, range) {
        if (storeId.isBlank()) return@DisposableEffect onDispose { }

        loading = true
        error = null

        val startTs = rangeStartTs()
        val reg = db.collection("merchants").document(storeId)
            .collection("sales")
            .whereGreaterThanOrEqualTo("createdAt", startTs)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    loading = false
                    error = err.message
                    return@addSnapshotListener
                }
                if (snap == null) {
                    loading = false
                    error = "데이터 없음"
                    return@addSnapshotListener
                }

                var total = 0L
                var orders = 0
                var cancels = 0

                val productAmount = mutableMapOf<String, Long>()
                val productCount = mutableMapOf<String, Int>()

                for (d in snap.documents) {
                    val status = d.getString("status") ?: "PAID"
                    val totalAmt = d.getLong("total") ?: 0L
                    if (status == "CANCELED") {
                        cancels++
                        // 취소는 총매출에 반영하지 않음 (정책에 맞게 조정 가능)
                        continue
                    }
                    orders++
                    total += totalAmt

                    @Suppress("UNCHECKED_CAST")
                    val items = d.get("items") as? List<Map<String, Any?>>
                    items?.forEach { itMap ->
                        val name = (itMap["name"] as? String)?.ifBlank { "이름없음" } ?: "이름없음"
                        val amount = (itMap["amount"] as? Number)?.toLong() ?: 0L
                        val qty = (itMap["qty"] as? Number)?.toInt() ?: 0

                        productAmount[name] = (productAmount[name] ?: 0L) + amount
                        productCount[name] = (productCount[name] ?: 0) + qty
                    }
                }

                val avg = if (orders > 0) (total.toDouble() / orders).roundToLong() else 0L

                // 금액 기준 상위 정렬 (원하면 건수 기준으로 변경)
                val top = productAmount.entries
                    .sortedByDescending { it.value }
                    .take(20)
                    .map { e -> Triple(e.key, e.value, productCount[e.key] ?: 0) }

                stats = SalesStats(
                    totalAmount = total,
                    orderCount = orders,
                    avgOrderAmount = avg,
                    cancelCount = cancels
                )
                topProducts = top
                loading = false
            }

        onDispose { reg.remove() }
    }

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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                ElevatedCard {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("요약", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = range == SalesRange.TODAY, onClick = { range = SalesRange.TODAY }, label = { Text("오늘") })
                            FilterChip(selected = range == SalesRange.WEEK,  onClick = { range = SalesRange.WEEK  }, label = { Text("최근 7일") })
                            FilterChip(selected = range == SalesRange.MONTH, onClick = { range = SalesRange.MONTH }, label = { Text("최근 30일") })
                        }
                        Spacer(Modifier.height(6.dp))

                        if (loading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else if (error != null) {
                            Text("오류: $error", color = MaterialTheme.colorScheme.error)
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard(title = "총 매출", value = "%,d원".format(stats.totalAmount), icon = Icons.Default.RequestQuote, modifier = Modifier.weight(1f))
                                StatCard(title = "주문 수", value = "${stats.orderCount}건", icon = Icons.Default.ReceiptLong, modifier = Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard(title = "평균 객단가", value = "%,d원".format(stats.avgOrderAmount), icon = Icons.Default.Paid, modifier = Modifier.weight(1f))
                                StatCard(title = "환불/취소", value = "${stats.cancelCount}건", icon = Icons.Default.Cancel, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("인기 상품", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (loading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else if (topProducts.isEmpty()) {
                            EmptyState(icon = Icons.Default.Inventory2, text = "데이터가 없습니다")
                        } else {
                            topProducts.forEach { (name, amount, count) ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    AssistChip(onClick = {}, label = { Text("${count}개") }, leadingIcon = { Icon(Icons.Default.ShoppingCart, null) })
                                    Spacer(Modifier.width(8.dp))
                                    Text("%,d원".format(amount), style = MaterialTheme.typography.titleSmall)
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
