package com.bcu.foodtable.ui.merchant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.NotificationsActive
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

data class LiveOrder(
    val orderId: String = "",
    val total: Long = 0,
    val createdAt: Timestamp? = null,
    val status: String = "PAID",
    val method: String = "POINT",
    val items: List<Map<String, Any?>> = emptyList()
)

@Composable
fun MerchantLiveOrdersPanel(
    storeId: String,
    modifier: Modifier = Modifier
) {
    val db = Firebase.firestore
    val orders = remember { mutableStateListOf<LiveOrder>() }
    var banner by remember { mutableStateOf<LiveOrder?>(null) }

    // 실시간 구독: 최근 20건, 상태 최신 우선
    DisposableEffect(storeId) {
        if (storeId.isBlank()) return@DisposableEffect onDispose { }
        val reg = db.collection("merchants").document(storeId)
            .collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snap, _ ->
                snap ?: return@addSnapshotListener
                val list = snap.documents.mapNotNull { d ->
                    LiveOrder(
                        orderId = d.getString("orderId") ?: d.id,
                        total = d.getLong("total") ?: 0L,
                        createdAt = d.getTimestamp("createdAt"),
                        status = d.getString("status") ?: "PAID",
                        method = d.getString("method") ?: "POINT",
                        items = (d.get("items") as? List<Map<String, Any?>>) ?: emptyList()
                    )
                }
                orders.clear()
                orders.addAll(list)

                // 최신 건 배너로 한 번 띄우기 (PAID만)
                val top = list.firstOrNull { it.status == "PAID" }
                if (top != null) banner = top
            }
        onDispose { reg.remove() }
    }

    Column(modifier) {
        // 축하 배너
        AnimatedVisibility(visible = banner != null, enter = fadeIn(), exit = fadeOut()) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Celebration, contentDescription = null)
                    Column(Modifier.weight(1f)) {
                        Text("새 결제 도착!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("주문금액: %,d원".format(banner?.total ?: 0), style = MaterialTheme.typography.bodyMedium)
                    }
                    TextButton(onClick = { banner = null }) { Text("닫기") }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 최근 주문 리스트
        ElevatedCard {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, null)
                    Spacer(Modifier.width(8.dp))
                    Text("최근 결제", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                if (orders.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("아직 들어온 결제가 없습니다.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 260.dp)
                    ) {
                        items(orders, key = { it.orderId }) { o ->
                            ElevatedCard {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("#${o.orderId}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text("${o.method} · ${o.status}", color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text("%,d원".format(o.total), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
