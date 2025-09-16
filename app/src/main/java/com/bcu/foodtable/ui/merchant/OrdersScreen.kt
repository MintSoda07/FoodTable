package com.bcu.foodtable.ui.merchant

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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class OrderTab { PENDING, IN_PROGRESS, COMPLETED, CANCELED }

data class OrderItem(
    val id: String,
    val code: String,
    val status: OrderTab,
    val totalPrice: Long,
    val createdAt: Timestamp?,
    val method: String = "QR"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    storeId: String,
    onBack: () -> Unit
) {
    val db = Firebase.firestore
    var tab by remember { mutableStateOf(OrderTab.PENDING) }

    val all = remember { mutableStateListOf<OrderItem>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 실시간 구독: 최신 100건
    DisposableEffect(storeId) {
        if (storeId.isBlank()) return@DisposableEffect onDispose { }

        loading = true
        error = null
        all.clear()

        val reg = db.collection("merchants").document(storeId)
            .collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    error = err.message
                    loading = false
                    return@addSnapshotListener
                }
                if (snap == null) {
                    error = "데이터 없음"
                    loading = false
                    return@addSnapshotListener
                }

                for (dc in snap.documentChanges) {
                    val d = dc.document
                    val id = d.id
                    val statusStr = d.getString("status") ?: "PENDING"
                    val status = when (statusStr) {
                        "PENDING" -> OrderTab.PENDING
                        "IN_PROGRESS" -> OrderTab.IN_PROGRESS
                        "COMPLETED" -> OrderTab.COMPLETED
                        "CANCELED" -> OrderTab.CANCELED
                        "PAID" -> OrderTab.PENDING // 결제 직후 매장 접수 대기
                        else -> OrderTab.PENDING
                    }
                    val item = OrderItem(
                        id = id,
                        code = d.getString("orderId") ?: id,
                        status = status,
                        totalPrice = d.getLong("total") ?: 0L,
                        createdAt = d.getTimestamp("createdAt"),
                        method = d.getString("method") ?: "QR"
                    )
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> if (all.none { it.id == id }) all.add(item)
                        DocumentChange.Type.MODIFIED -> {
                            val idx = all.indexOfFirst { it.id == id }
                            if (idx >= 0) all[idx] = item
                        }
                        DocumentChange.Type.REMOVED -> all.removeAll { it.id == id }
                    }
                }
                loading = false
            }

        onDispose { reg.remove() }
    }

    // 탭 필터 + 클라이언트 정렬 보강(createdAt null 방어)
    val filtered by remember(tab, all) {
        mutableStateOf(
            when (tab) {
                OrderTab.PENDING     -> all.filter { it.status == OrderTab.PENDING }
                OrderTab.IN_PROGRESS -> all.filter { it.status == OrderTab.IN_PROGRESS }
                OrderTab.COMPLETED   -> all.filter { it.status == OrderTab.COMPLETED }
                OrderTab.CANCELED    -> all.filter { it.status == OrderTab.CANCELED }
            }.sortedByDescending { it.createdAt?.toDate()?.time ?: Long.MIN_VALUE }
        )
    }

    // 액션
    suspend fun updateStatus(o: OrderItem, to: OrderTab) {
        val ref = db.collection("merchants").document(storeId)
            .collection("orders").document(o.id)
        val toStr = when (to) {
            OrderTab.PENDING -> "PENDING"
            OrderTab.IN_PROGRESS -> "IN_PROGRESS"
            OrderTab.COMPLETED -> "COMPLETED"
            OrderTab.CANCELED -> "CANCELED"
        }
        ref.update("status", toStr)
    }

    val scope = rememberCoroutineScope()
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("주문 관리") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = tab == OrderTab.PENDING,     onClick = { tab = OrderTab.PENDING },     label = { Text("접수 대기") })
                FilterChip(selected = tab == OrderTab.IN_PROGRESS, onClick = { tab = OrderTab.IN_PROGRESS }, label = { Text("준비 중") })
                FilterChip(selected = tab == OrderTab.COMPLETED,   onClick = { tab = OrderTab.COMPLETED },   label = { Text("완료") })
                FilterChip(selected = tab == OrderTab.CANCELED,    onClick = { tab = OrderTab.CANCELED },    label = { Text("취소") })
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("오류: $error", color = MaterialTheme.colorScheme.error) }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (filtered.isEmpty()) {
                        item { EmptyState(icon = Icons.Default.ReceiptLong, text = "표시할 주문이 없습니다") }
                    } else {
                        items(filtered, key = { it.id }) { o ->
                            ElevatedCard {
                                Column(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("#${o.code}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        AssistChip(onClick = {}, label = { Text(o.method) }, leadingIcon = { Icon(Icons.Default.QrCode, null) })
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val time = o.createdAt?.toDate()?.let { timeFmt.format(it) } ?: "-"
                                        Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        Text("%,d원".format(o.totalPrice), style = MaterialTheme.typography.titleSmall)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        when (o.status) {
                                            OrderTab.PENDING -> Row {
                                                FilledTonalButton(onClick = { scope.launch { updateStatus(o, OrderTab.IN_PROGRESS) } }) { Text("접수") }
                                                Spacer(Modifier.width(8.dp))
                                                OutlinedButton(onClick = { scope.launch { updateStatus(o, OrderTab.CANCELED) } }) { Text("거절") }
                                            }
                                            OrderTab.IN_PROGRESS -> Row {
                                                FilledTonalButton(onClick = { scope.launch { updateStatus(o, OrderTab.COMPLETED) } }) { Text("완료 처리") }
                                                Spacer(Modifier.width(8.dp))
                                                OutlinedButton(onClick = { scope.launch { updateStatus(o, OrderTab.CANCELED) } }) { Text("취소") }
                                            }
                                            OrderTab.COMPLETED -> OutlinedButton(onClick = {
                                                // 환불 정책: 필요 시 sales/point 롤백 추가
                                                scope.launch { updateStatus(o, OrderTab.CANCELED) }
                                            }) { Text("환불") }
                                            OrderTab.CANCELED -> Text("취소됨", color = MaterialTheme.colorScheme.outline)
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
