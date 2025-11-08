// screens/SalesDashboardScreen.kt
package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToLong

enum class SalesRange { TODAY, WEEK, MONTH }

data class SalesStats(
    val totalAmount: Long = 0L,
    val orderCount: Int = 0,
    val avgOrderAmount: Long = 0L,
    val cancelCount: Int = 0,
    val avgLineItems: Double = 0.0,
    val paidByMethod: Map<String, Int> = emptyMap(), // e.g., POINT, CARD, CASH
    val usedCoupons: Int = 0
)

data class OrderRowData(
    val id: String,
    val orderId: String,
    val status: String,           // PAID, CANCELED, IN_PROGRESS ...
    val total: Long,
    val method: String,
    val createdAt: Timestamp?,
    val coupon: String,
    val buyerUid: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesDashboardScreen(
    storeId: String,
    onBack: () -> Unit,
    // 상세로 이동 연결하고 싶으면: { orderId -> nav.navigate("orderDetail/$storeId/$orderId") }
    onOpenOrderDetail: (String) -> Unit = {}
) {
    val db = Firebase.firestore
    var range by remember { mutableStateOf(SalesRange.TODAY) }

    // 상태
    var stats by remember { mutableStateOf(SalesStats()) }
    var topProducts by remember { mutableStateOf(listOf<Triple<String, Long, Int>>()) } // (name, amount, qty)
    var ordersList by remember { mutableStateOf(listOf<OrderRowData>()) }               // ▼ 새로 추가: 기간 내 모든 주문
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // ────────────── 시간 범위(Asia/Seoul 자정) ──────────────
    fun kstStartOfTodayMillis(nowMs: Long = System.currentTimeMillis()): Long {
        val tz = TimeZone.getTimeZone("Asia/Seoul")
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun rangeStartTs(range: SalesRange, nowMs: Long = System.currentTimeMillis()): Timestamp {
        val dayMs = 24L * 60 * 60 * 1000
        val today0 = kstStartOfTodayMillis(nowMs)
        val fromMs = when (range) {
            SalesRange.TODAY -> today0
            SalesRange.WEEK  -> today0 - 6L * dayMs   // 오늘 포함 7일
            SalesRange.MONTH -> today0 - 29L * dayMs  // 오늘 포함 30일
        }
        return Timestamp(fromMs / 1000, 0)
    }

    // ────────────── Firestore 실시간 구독 ──────────────
    DisposableEffect(storeId, range) {
        if (storeId.isBlank()) return@DisposableEffect onDispose {}

        loading = true
        error = null
        stats = SalesStats()
        topProducts = emptyList()
        ordersList = emptyList()

        val startTs = rangeStartTs(range)
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
                var totalLineItems = 0
                var usedCoupons = 0
                val methodCount = mutableMapOf<String, Int>()

                // pid 기준 집계 (이름은 최신값 유지)
                val amountByPid = mutableMapOf<String, Long>()
                val qtyByPid = mutableMapOf<String, Int>()
                val nameByPid = mutableMapOf<String, String>()

                // 주문 리스트 생성
                val listAcc = mutableListOf<OrderRowData>()

                for (d in snap.documents) {
                    val id = d.id
                    val status = d.getString("status") ?: "PAID"
                    val totalAmt = d.getLong("total") ?: 0L
                    val method = d.getString("method") ?: "UNKNOWN"
                    val coupon = (d.getString("coupon") ?: "").trim()
                    val orderId = d.getString("orderId") ?: id
                    val createdAt = d.getTimestamp("createdAt")
                    val buyerUid = d.getString("buyerUid") ?: ""

                    // 주문 리스트는 상태 무관(취소 포함)으로 보여줌
                    listAcc += OrderRowData(
                        id = id,
                        orderId = orderId,
                        status = status,
                        total = totalAmt,
                        method = method,
                        createdAt = createdAt,
                        coupon = coupon,
                        buyerUid = buyerUid
                    )

                    // 통계: 정책상 PAID만 매출에 반영
                    if (status == "CANCELED") {
                        cancels++
                        continue
                    }
                    if (status != "PAID") continue

                    orders++
                    total += totalAmt
                    methodCount[method] = (methodCount[method] ?: 0) + 1
                    if (coupon.isNotEmpty()) usedCoupons++

                    @Suppress("UNCHECKED_CAST")
                    val items = d.get("items") as? List<Map<String, Any?>>
                    val size = items?.size ?: 0
                    totalLineItems += size

                    items?.forEach { itMap ->
                        val pid = (itMap["pid"] as? String)?.ifBlank { null } ?: return@forEach
                        val name = (itMap["name"] as? String)?.ifBlank { "이름없음" } ?: "이름없음"
                        val amount = (itMap["amount"] as? Number)?.toLong() ?: 0L
                        val qty = (itMap["qty"] as? Number)?.toInt() ?: 0

                        amountByPid[pid] = (amountByPid[pid] ?: 0L) + amount
                        qtyByPid[pid] = (qtyByPid[pid] ?: 0) + qty
                        if (pid !in nameByPid) nameByPid[pid] = name
                    }
                }

                // 주문 리스트 최신순 정렬
                ordersList = listAcc.sortedByDescending { it.createdAt?.seconds ?: Long.MIN_VALUE }

                val avgOrder = if (orders > 0) (total.toDouble() / orders).roundToLong() else 0L
                val avgLines = if (orders > 0) totalLineItems.toDouble() / orders else 0.0

                val top = amountByPid.entries
                    .sortedByDescending { it.value }
                    .take(20)
                    .map { e ->
                        val pid = e.key
                        val name = nameByPid[pid] ?: pid
                        Triple(name, e.value, qtyByPid[pid] ?: 0)
                    }

                stats = SalesStats(
                    totalAmount = total,
                    orderCount = orders,
                    avgOrderAmount = avgOrder,
                    cancelCount = cancels,
                    avgLineItems = avgLines,
                    paidByMethod = methodCount.toMap(),
                    usedCoupons = usedCoupons
                )
                topProducts = top
                loading = false
            }

        onDispose { reg.remove() }
    }

    // ────────────── UI ──────────────
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
            // 요약
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
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard(title = "평균 품목수", value = String.format("%.1f개", stats.avgLineItems), icon = Icons.Default.Summarize, modifier = Modifier.weight(1f))
                                StatCard(title = "쿠폰 사용", value = "${stats.usedCoupons}건", icon = Icons.Default.CardGiftcard, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 결제수단 분포
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("결제수단 분포", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (loading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else if (error != null) {
                            Text("오류: $error", color = MaterialTheme.colorScheme.error)
                        } else if (stats.paidByMethod.isEmpty()) {
                            EmptyState(icon = Icons.Default.Payments, text = "데이터가 없습니다")
                        } else {
                            stats.paidByMethod.entries
                                .sortedByDescending { it.value }
                                .forEach { (method, count) ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(method, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                        Text("${count}건", style = MaterialTheme.typography.titleSmall)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                        }
                    }
                }
            }

            // 인기 상품
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("인기 상품", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (loading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else if (error != null) {
                            Text("오류: $error", color = MaterialTheme.colorScheme.error)
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

            // ▼ 기간 내 주문 목록(상태별 컬러/클릭 → 상세)
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("주문 목록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (loading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else if (error != null) {
                            Text("오류: $error", color = MaterialTheme.colorScheme.error)
                        } else if (ordersList.isEmpty()) {
                            EmptyState(icon = Icons.Default.ReceiptLong, text = "주문이 없습니다")
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                            ) {
                                items(ordersList, key = { it.id }) { o ->
                                    ElevatedCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenOrderDetail(o.orderId) }
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text("#${o.orderId}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    AssistChip(
                                                        onClick = {},
                                                        label = { Text(o.method) },
                                                        leadingIcon = { Icon(Icons.Default.Payments, null) }
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    val statusColor = when (o.status) {
                                                        "PAID" -> MaterialTheme.colorScheme.primary
                                                        "COMPLETED" -> MaterialTheme.colorScheme.tertiary
                                                        "CANCELED" -> MaterialTheme.colorScheme.error
                                                        "IN_PROGRESS" -> MaterialTheme.colorScheme.secondary
                                                        else -> MaterialTheme.colorScheme.outline
                                                    }
                                                    AssistChip(
                                                        onClick = {},
                                                        label = { Text(o.status) },
                                                        leadingIcon = { Icon(Icons.Default.Flag, null) },
                                                        colors = AssistChipDefaults.assistChipColors(
                                                            labelColor = statusColor
                                                        )
                                                    )
                                                    if (o.coupon.isNotBlank()) {
                                                        Spacer(Modifier.width(8.dp))
                                                        AssistChip(
                                                            onClick = {},
                                                            label = { Text("쿠폰") },
                                                            leadingIcon = { Icon(Icons.Default.CardGiftcard, null) }
                                                        )
                                                    }
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                val timeStr = o.createdAt?.toDate()?.let {
                                                    // HH:mm 표기
                                                    val h = it.hours.toString().padStart(2, '0')
                                                    val m = it.minutes.toString().padStart(2, '0')
                                                    "$h:$m"
                                                } ?: "-"
                                                Text("%,d원".format(o.total), style = MaterialTheme.typography.titleSmall)
                                                Text(timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "총 ${ordersList.size}건 표시(취소 포함). ‘주문 수’는 매출 반영(PAID)만 집계됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
