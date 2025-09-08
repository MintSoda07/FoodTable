// screens/ReportsScreen.kt
package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    storeId: String,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf(ReportTab.BY_DAY) }

    // TODO: plug charts / analytics source
    val rows = remember(tab) { mockReportRows(tab) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("통계/리포트") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == ReportTab.BY_DAY,      onClick = { tab = ReportTab.BY_DAY },      label = { Text("일자별") })
                FilterChip(selected = tab == ReportTab.BY_CATEGORY, onClick = { tab = ReportTab.BY_CATEGORY }, label = { Text("카테고리별") })
                FilterChip(selected = tab == ReportTab.BY_PRODUCT,  onClick = { tab = ReportTab.BY_PRODUCT },  label = { Text("상품별") })
            }

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (rows.isEmpty()) {
                    item { EmptyState(icon = Icons.Default.Insights, text = "리포트 데이터가 없습니다") }
                } else {
                    items(rows, key = { it.label }) { r ->
                        ElevatedCard {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(r.label, style = MaterialTheme.typography.bodyLarge)
                                Text("${r.value}원", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class ReportTab { BY_DAY, BY_CATEGORY, BY_PRODUCT }
data class ReportRow(val label: String, val value: Long)

private fun mockReportRows(tab: ReportTab) = when (tab) {
    ReportTab.BY_DAY -> listOf(
        ReportRow("2025-09-01", 221_000), ReportRow("2025-09-02", 180_500), ReportRow("2025-09-03", 199_000)
    )
    ReportTab.BY_CATEGORY -> listOf(
        ReportRow("덮밥", 2_210_000), ReportRow("면류", 1_590_000), ReportRow("사이드", 780_000)
    )
    ReportTab.BY_PRODUCT -> listOf(
        ReportRow("불고기덮밥", 1_120_000), ReportRow("치킨마요", 860_000), ReportRow("우동세트", 450_000)
    )
}
