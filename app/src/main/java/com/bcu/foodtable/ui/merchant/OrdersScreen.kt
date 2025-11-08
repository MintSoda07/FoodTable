// screens/OrdersScreen.kt
package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
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
import java.text.SimpleDateFormat
import java.util.*

enum class OrderTab { PENDING, IN_PROGRESS, COMPLETED, CANCELED }

data class OrderRowVM(
    val id: String,
    val orderCode: String,
    val status: OrderTab,
    val totalPrice: Long,
    val createdAt: Timestamp?,
    val method: String = "QR"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    storeId: String,
    onBack: () -> Unit,
    onOpenDetail: (orderId: String) -> Unit = {} // ← 상세 진입 콜백(선택)
) {
    val db = Firebase.firestore
    var tab by remember { mutableStateOf(OrderTab.PENDING) }

    val all = remember { mutableStateListOf<OrderRowVM>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // 시간 포맷
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // ───────────────── 실시간 구독 ─────────────────
    DisposableEffect(storeId) {
        if (storeId.isBlank()) return@DisposableEffect onDispose {}

        loading = true
        error = null
        all.clear()

        // createdAt이 serverTimestamp로 null일 수 있으므로 orderBy는 그대로 두고, null은 맨 뒤로 오게 후처리
        val reg = db.collection("merchants").document(storeId)
            .collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(200)
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
                    val mappedStatus = when (statusStr) {
                        "PAID" -> OrderTab.PENDING // 결제완료=접수 대기
                        "PENDING" -> OrderTab.PENDING
                        "IN_PROGRESS" -> OrderTab.IN_PROGRESS
                        "COMPLETED" -> OrderTab.COMPLETED
                        "CANCELED" -> OrderTab.CANCELED
                        else -> OrderTab.PENDING
                    }
                    val vm = OrderRowVM(
                        id = id,
                        orderCode = d.getString("orderId") ?: id,
                        status = mappedStatus,
                        totalPrice = d.getLong("total") ?: 0L,
                        createdAt = d.getTimestamp("createdAt"),
                        method = d.getString("method") ?: "QR"
                    )

                    when (dc.type) {
                        DocumentChange.Type.ADDED -> if (all.none { it.id == id }) all.add(vm)
                        DocumentChange.Type.MODIFIED -> {
                            val idx = all.indexOfFirst { it.id == id }
                            if (idx >= 0) all[idx] = vm
                        }
                        DocumentChange.Type.REMOVED -> all.removeAll { it.id == id }
                    }
                }

                // createdAt DESC, null은 뒤로
                all.sortWith(
                    compareByDescending<OrderRowVM> { it.createdAt?.seconds ?: Long.MIN_VALUE }
                        .thenByDescending { it.createdAt?.nanoseconds ?: Int.MIN_VALUE }
                )

                loading = false
            }

        onDispose { reg.remove() }
    }

    // ───────────────── 탭 필터(파생 상태) ─────────────────
    val filtered by remember(tab) {
        derivedStateOf {
            all.filter { it.status == tab }
        }
    }

    // ───────────────── 액션: 상태 변경 ─────────────────
    fun updateStatus(o: OrderRowVM, to: OrderTab) {
        val toStr = when (to) {
            OrderTab.PENDING -> "PENDING"
            OrderTab.IN_PROGRESS -> "IN_PROGRESS"
            OrderTab.COMPLETED -> "COMPLETED"
            OrderTab.CANCELED -> "CANCELED"
        }
        Firebase.firestore.collection("merchants").document(storeId)
            .collection("orders").document(o.id)
            .update("status", toStr)
    }

    // ───────────────── UI ─────────────────
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

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("오류: $error", color = MaterialTheme.colorScheme.error) }
                filtered.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("표시할 주문이 없습니다") }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { o ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenDetail(o.id) } // ← 상세로 진입
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("#${o.orderCode}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        AssistChip(onClick = {}, label = { Text(o.method) }, leadingIcon = { Icon(Icons.Default.QrCode, null) })
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val ts = o.createdAt?.toDate()
                                        val whenTxt = ts?.let { "${timeFmt.format(it)} · ${dateFmt.format(it)}" } ?: "-"
                                        Text(whenTxt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        Text("%,d원".format(o.totalPrice), style = MaterialTheme.typography.titleSmall)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        when (o.status) {
                                            OrderTab.PENDING -> Row {
                                                FilledTonalButton(onClick = { updateStatus(o, OrderTab.IN_PROGRESS) }) { Text("접수") }
                                                Spacer(Modifier.width(8.dp))
                                                OutlinedButton(onClick = { updateStatus(o, OrderTab.CANCELED) }) { Text("거절") }
                                            }
                                            OrderTab.IN_PROGRESS -> Row {
                                                FilledTonalButton(onClick = { updateStatus(o, OrderTab.COMPLETED) }) { Text("완료 처리") }
                                                Spacer(Modifier.width(8.dp))
                                                OutlinedButton(onClick = { updateStatus(o, OrderTab.CANCELED) }) { Text("취소") }
                                            }
                                            OrderTab.COMPLETED -> OutlinedButton(onClick = { updateStatus(o, OrderTab.CANCELED) }) { Text("환불") }
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
