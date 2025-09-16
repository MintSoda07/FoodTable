// screens/OrderDetailScreen.kt
package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

data class OrderDetail(
    val id: String = "",
    val orderId: String = "",
    val status: String = "PAID",
    val method: String = "POINT",
    val buyerUid: String = "",
    val subtotal: Long = 0L,
    val vatShown: Long = 0L,
    val discount: Long = 0L,
    val total: Long = 0L,
    val coupon: String = "",
    val createdAt: Timestamp? = null,
    val paidAt: Timestamp? = null,
    val items: List<Map<String, Any?>> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    storeId: String,
    orderId: String,
    onBack: () -> Unit
) {
    val db = Firebase.firestore
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var model by remember { mutableStateOf<OrderDetail?>(null) }

    val timeFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    // ───────── 실시간 문서 구독 ─────────
    DisposableEffect(storeId, orderId) {
        if (storeId.isBlank() || orderId.isBlank()) return@DisposableEffect onDispose {}
        loading = true; error = null; model = null

        val ref = db.collection("merchants").document(storeId)
            .collection("orders").document(orderId)

        val reg = ref.addSnapshotListener { d, e ->
            if (e != null) { error = e.message; loading = false; return@addSnapshotListener }
            if (d == null || !d.exists()) { error = "주문을 찾을 수 없습니다"; loading = false; return@addSnapshotListener }

            @Suppress("UNCHECKED_CAST")
            val items = d.get("items") as? List<Map<String, Any?>> ?: emptyList()

            model = OrderDetail(
                id = d.id,
                orderId = d.getString("orderId") ?: d.id,
                status = d.getString("status") ?: "PAID",
                method = d.getString("method") ?: "POINT",
                buyerUid = d.getString("buyerUid") ?: "",
                subtotal = d.getLong("subtotal") ?: 0L,
                vatShown = d.getLong("vatShown") ?: 0L,
                discount = d.getLong("discount") ?: 0L,
                total = d.getLong("total") ?: 0L,
                coupon = d.getString("coupon") ?: "",
                createdAt = d.getTimestamp("createdAt"),
                paidAt = d.getTimestamp("paidAt"),
                items = items
            )
            loading = false
        }

        onDispose { reg.remove() }
    }

    // ───────── 상태 변경 액션 ─────────
    fun updateStatus(ref: DocumentReference, to: String) {
        ref.update("status", to)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("주문 상세") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("오류: $error", color = MaterialTheme.colorScheme.error) }
            model == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("데이터 없음") }
            else -> {
                val m = model!!
                val ref = Firebase.firestore.collection("merchants").document(storeId)
                    .collection("orders").document(m.id)

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(padding).fillMaxSize()
                ) {
                    item {
                        ElevatedCard {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("#${m.orderId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    AssistChip(onClick = {}, label = { Text(m.method) }, leadingIcon = { Icon(Icons.Default.ReceiptLong, null) })
                                }
                                val created = m.createdAt?.toDate()?.let(timeFmt::format) ?: "-"
                                val paid = m.paidAt?.toDate()?.let(timeFmt::format) ?: "-"
                                KeyValue("생성시각", created)
                                KeyValue("결제시각", paid)
                                KeyValue("구매자 UID", m.buyerUid.takeIf { it.isNotBlank() } ?: "-")
                                if (m.coupon.isNotBlank()) {
                                    AssistChip(onClick = {}, label = { Text("쿠폰: ${m.coupon}") }, leadingIcon = { Icon(Icons.Default.CardGiftcard, null) })
                                }
                            }
                        }
                    }

                    // 금액
                    item {
                        ElevatedCard {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                KeyValue("표시가(부가세포함 합계)", "%,d원".format(m.subtotal))
                                KeyValue("부가세(표시)", "%,d원".format(m.vatShown), dim = true)
                                if (m.discount > 0) KeyValue("할인", "-%,d원".format(m.discount), accent = true)
                                Divider()
                                KeyValue("결제금액", "%,d원".format(m.total), strong = true)
                            }
                        }
                    }

                    // 항목
                    item {
                        ElevatedCard {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("주문 항목", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (m.items.isEmpty()) {
                                    Text("항목이 없습니다", color = MaterialTheme.colorScheme.outline)
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(m.items) { itMap ->
                                            val name = (itMap["name"] as? String) ?: "-"
                                            val price = (itMap["price"] as? Number)?.toLong() ?: 0L
                                            val qty = (itMap["qty"] as? Number)?.toInt() ?: 0
                                            val amount = (itMap["amount"] as? Number)?.toLong() ?: price * qty
                                            ElevatedCard {
                                                Row(
                                                    Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                                        Text("%,d원 × %d".format(price, qty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                                    }
                                                    Text("%,d원".format(amount), style = MaterialTheme.typography.titleSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 상태 버튼
                    item {
                        ElevatedCard {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("상태", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    when (m.status) {
                                        "PAID", "PENDING" -> {
                                            Button(onClick = { updateStatus(ref, "IN_PROGRESS") }) { Text("접수") }
                                            OutlinedButton(onClick = { updateStatus(ref, "CANCELED") }) { Text("거절") }
                                        }
                                        "IN_PROGRESS" -> {
                                            Button(onClick = { updateStatus(ref, "COMPLETED") }) { Text("완료 처리") }
                                            OutlinedButton(onClick = { updateStatus(ref, "CANCELED") }) { Text("취소") }
                                        }
                                        "COMPLETED" -> {
                                            OutlinedButton(onClick = { updateStatus(ref, "CANCELED") }) { Text("환불(취소)") }
                                        }
                                        "CANCELED" -> {
                                            Text("취소됨", color = MaterialTheme.colorScheme.outline)
                                        }
                                        else -> {
                                            OutlinedButton(onClick = { updateStatus(ref, "IN_PROGRESS") }) { Text("접수") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyValue(key: String, value: String, strong: Boolean = false, dim: Boolean = false, accent: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = if (dim) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant)
        val style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        val color = when {
            accent -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }
        Text(value, style = style, color = color, fontWeight = if (strong) FontWeight.SemiBold else null)
    }
}
