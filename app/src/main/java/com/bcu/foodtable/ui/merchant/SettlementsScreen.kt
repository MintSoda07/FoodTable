// screens/SettlementsScreen.kt
package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementsScreen(
    storeId: String,
    onBack: () -> Unit
) {
    var range by remember { mutableStateOf(SalesRange.WEEK) }

    // TODO: Wire to payouts/settlements collection
    val items = remember(range) { mockSettlements(range) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("정산/입금") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("조회 기간", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = range == SalesRange.WEEK,  onClick = { range = SalesRange.WEEK  }, label = { Text("주간") })
                            FilterChip(selected = range == SalesRange.MONTH, onClick = { range = SalesRange.MONTH }, label = { Text("월간") })
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item { EmptyState(icon = Icons.Default.AccountBalance, text = "정산 내역이 없습니다") }
            } else {
                items(items, key = { it.id }) { s ->
                    ElevatedCard {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${s.period}", style = MaterialTheme.typography.titleSmall)
                                Text("${s.amount}원", style = MaterialTheme.typography.titleMedium)
                            }
                            Text("상태: ${s.status}", color = MaterialTheme.colorScheme.outline)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { /* TODO: download */ }) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("명세서") }
                                FilledTonalButton(onClick = { /* TODO: detail */ }) { Text("상세") }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SettlementItem(
    val id: String,
    val period: String,
    val amount: Long,
    val status: String
)

private fun mockSettlements(range: SalesRange) = when (range) {
    SalesRange.TODAY -> emptyList()
    SalesRange.WEEK  -> listOf(
        SettlementItem("s1", "2025-09-01 ~ 2025-09-07", 1_220_000, "지급완료")
    )
    SalesRange.MONTH -> listOf(
        SettlementItem("s1", "2025-08", 5_910_000, "지급완료"),
        SettlementItem("s2", "2025-07", 5_102_000, "지급완료")
    )
}
