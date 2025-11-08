package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Today
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

enum class ReportTab { BY_DAY, BY_CATEGORY, BY_PRODUCT }
enum class ReportRange { TODAY, WEEK, MONTH }
data class ReportRow(val label: String, val value: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    storeId: String,
    onBack: () -> Unit
) {
    val db = Firebase.firestore

    var tab by remember { mutableStateOf(ReportTab.BY_DAY) }
    var range by remember { mutableStateOf(ReportRange.TODAY) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf(listOf<ReportRow>()) }

    // 카테고리 매핑(pid -> category)
    val pidToCategory = remember { mutableStateMapOf<String, String>() }

    // Timezone & format
    val tz = remember { TimeZone.getTimeZone("Asia/Seoul") }
    val dayFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply { timeZone = tz } }

    // ─────────── products → pid→category 캐시 ───────────
    DisposableEffect(storeId) {
        if (storeId.isBlank()) return@DisposableEffect onDispose { }
        val reg = db.collection("merchants").document(storeId)
            .collection("products")
            .addSnapshotListener { snap, _ ->
                snap ?: return@addSnapshotListener
                for (dc in snap.documentChanges) {
                    val pid = dc.document.id
                    val cat = (dc.document.getString("category") ?: "").ifBlank { "미분류" }
                    when (dc.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> pidToCategory[pid] = cat
                        DocumentChange.Type.REMOVED -> pidToCategory.remove(pid)
                    }
                }
            }
        onDispose { reg.remove() }
    }

    // ─────────── 기간 시작 계산(KST 자정) ───────────
    fun kstStartOfTodayMillis(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun rangeStart(range: ReportRange): Date {
        val dayMs = 24L * 60 * 60 * 1000
        val today0 = kstStartOfTodayMillis()
        val fromMs = when (range) {
            ReportRange.TODAY -> today0
            ReportRange.WEEK  -> today0 - 6L * dayMs
            ReportRange.MONTH -> today0 - 29L * dayMs
        }
        return Date(fromMs)
    }

    // ─────────── sales 구독 & 집계 ───────────
    DisposableEffect(storeId, range, tab) {
        if (storeId.isBlank()) return@DisposableEffect onDispose {}

        loading = true
        error = null
        rows = emptyList()

        val startTs = Timestamp(rangeStart(range))
        val reg = db.collection("merchants").document(storeId)
            .collection("sales")
            .whereGreaterThanOrEqualTo("createdAt", startTs)
            .orderBy("createdAt", Query.Direction.ASCENDING) // 일자별 시계열 표시를 위해 ASC
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

                when (tab) {
                    ReportTab.BY_DAY -> {
                        val byDay = linkedMapOf<String, Long>()
                        for (d in snap.documents) {
                            val status = d.getString("status") ?: "PAID"
                            if (status != "PAID") continue
                            val amount = d.getLong("total") ?: 0L
                            val dateKey = d.getTimestamp("createdAt")?.toDate()?.let(dayFmt::format) ?: continue
                            byDay[dateKey] = (byDay[dateKey] ?: 0L) + amount
                        }
                        rows = byDay.entries.map { ReportRow(it.key, it.value) }
                    }

                    ReportTab.BY_CATEGORY -> {
                        val byCat = mutableMapOf<String, Long>()
                        for (d in snap.documents) {
                            val status = d.getString("status") ?: "PAID"
                            if (status != "PAID") continue
                            @Suppress("UNCHECKED_CAST")
                            val items = d.get("items") as? List<Map<String, Any?>>
                            items?.forEach { itMap ->
                                val pid = (itMap["pid"] as? String)?.ifBlank { null } ?: return@forEach
                                val cat = pidToCategory[pid] ?: "미분류"
                                val amount = (itMap["amount"] as? Number)?.toLong() ?: 0L
                                byCat[cat] = (byCat[cat] ?: 0L) + amount
                            }
                        }
                        rows = byCat.entries
                            .sortedByDescending { it.value }
                            .map { ReportRow(it.key, it.value) }
                    }

                    ReportTab.BY_PRODUCT -> {
                        val byProd = mutableMapOf<String, Long>()
                        for (d in snap.documents) {
                            val status = d.getString("status") ?: "PAID"
                            if (status != "PAID") continue
                            @Suppress("UNCHECKED_CAST")
                            val items = d.get("items") as? List<Map<String, Any?>>
                            items?.forEach { itMap ->
                                val name = (itMap["name"] as? String)?.ifBlank { "이름없음" } ?: "이름없음"
                                val amount = (itMap["amount"] as? Number)?.toLong() ?: 0L
                                byProd[name] = (byProd[name] ?: 0L) + amount
                            }
                        }
                        rows = byProd.entries
                            .sortedByDescending { it.value }
                            .map { ReportRow(it.key, it.value) }
                    }
                }
                loading = false
            }

        onDispose { reg.remove() }
    }

    // ─────────── UI ───────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("통계/리포트") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 탭 & 범위
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = tab == ReportTab.BY_DAY,      onClick = { tab = ReportTab.BY_DAY },      label = { Text("일자별") })
                FilterChip(selected = tab == ReportTab.BY_CATEGORY, onClick = { tab = ReportTab.BY_CATEGORY }, label = { Text("카테고리별") })
                FilterChip(selected = tab == ReportTab.BY_PRODUCT,  onClick = { tab = ReportTab.BY_PRODUCT },  label = { Text("상품별") })
                Spacer(Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { range = ReportRange.TODAY }, label = { Text("오늘") }, leadingIcon = { Icon(Icons.Default.Today, null) })
                AssistChip(onClick = { range = ReportRange.WEEK  }, label = { Text("7일") },   leadingIcon = { Icon(Icons.Default.DateRange, null) })
                AssistChip(onClick = { range = ReportRange.MONTH }, label = { Text("30일") },  leadingIcon = { Icon(Icons.Default.CalendarMonth, null) })
            }

            Spacer(Modifier.height(4.dp))

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    loading -> {
                        item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    error != null -> {
                        item { Text("오류: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
                    }
                    rows.isEmpty() -> {
                        item { EmptyState(icon = Icons.Default.Insights, text = "리포트 데이터가 없습니다") }
                    }
                    else -> {
                        items(rows, key = { it.label }) { r ->
                            ElevatedCard {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val title = when (tab) {
                                        ReportTab.BY_DAY      -> r.label // yyyy-MM-dd
                                        ReportTab.BY_CATEGORY -> r.label // 카테고리
                                        ReportTab.BY_PRODUCT  -> r.label // 상품명
                                    }
                                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text("%,d원".format(r.value), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
