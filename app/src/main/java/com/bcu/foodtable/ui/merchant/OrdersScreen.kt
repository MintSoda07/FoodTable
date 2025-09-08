// screens/OrdersScreen.kt
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    storeId: String,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf(OrderTab.PENDING) }

    // TODO: Replace with Firestore live query
    val items = remember(tab) { mockOrders(tab) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("주문 관리") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == OrderTab.PENDING,    onClick = { tab = OrderTab.PENDING },    label = { Text("접수 대기") })
                FilterChip(selected = tab == OrderTab.IN_PROGRESS,onClick = { tab = OrderTab.IN_PROGRESS },label = { Text("준비 중") })
                FilterChip(selected = tab == OrderTab.COMPLETED,  onClick = { tab = OrderTab.COMPLETED },  label = { Text("완료") })
                FilterChip(selected = tab == OrderTab.CANCELED,   onClick = { tab = OrderTab.CANCELED },   label = { Text("취소") })
            }

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (items.isEmpty()) {
                    item { EmptyState(icon = Icons.Default.ReceiptLong, text = "표시할 주문이 없습니다") }
                } else {
                    items(items, key = { it.id }) { o -> OrderRow(o) }
                }
            }
        }
    }
}

enum class OrderTab { PENDING, IN_PROGRESS, COMPLETED, CANCELED }

data class OrderItem(
    val id: String,
    val code: String,
    val status: OrderTab,
    val totalPrice: Long,
    val createdAt: String,
    val method: String = "QR"
)

@Composable
private fun OrderRow(o: OrderItem) {
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("#${o.code}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                AssistChip(onClick = {}, label = { Text(o.method) }, leadingIcon = { Icon(Icons.Default.QrCode, null) })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(o.createdAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text("${o.totalPrice}원", style = MaterialTheme.typography.titleSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (o.status) {
                    OrderTab.PENDING -> Row {
                        FilledTonalButton(onClick = { /* TODO: accept */ }) { Text("접수") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { /* TODO: cancel */ }) { Text("거절") }
                    }
                    OrderTab.IN_PROGRESS -> Row {
                        FilledTonalButton(onClick = { /* TODO: complete */ }) { Text("완료 처리") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { /* TODO: cancel */ }) { Text("취소") }
                    }
                    OrderTab.COMPLETED -> OutlinedButton(onClick = { /* TODO: refund */ }) { Text("환불") }
                    OrderTab.CANCELED -> Text("취소됨", color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

private fun mockOrders(tab: OrderTab): List<OrderItem> = when (tab) {
    OrderTab.PENDING -> listOf(
        OrderItem("1","A1023", OrderTab.PENDING, 12000, "11:32", "QR"),
        OrderItem("2","A1024", OrderTab.PENDING,  8500, "11:40", "카드")
    )
    OrderTab.IN_PROGRESS -> listOf(
        OrderItem("3","A1020", OrderTab.IN_PROGRESS, 15900, "11:18", "QR")
    )
    OrderTab.COMPLETED -> listOf(
        OrderItem("4","A1013", OrderTab.COMPLETED, 9900,  "10:50", "현금"),
        OrderItem("5","A1011", OrderTab.COMPLETED, 21200, "10:35", "QR")
    )
    OrderTab.CANCELED -> listOf(
        OrderItem("6","A1009", OrderTab.CANCELED, 15600, "10:20", "카드")
    )
}
