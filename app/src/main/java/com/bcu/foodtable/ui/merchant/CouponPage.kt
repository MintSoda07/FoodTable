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
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

enum class CouponType { PERCENT, FIXED }

data class Coupon(
    val id: String = "",
    val code: String = "",
    val title: String = "",
    val type: CouponType = CouponType.PERCENT,
    val value: Long = 10,          // percent 또는 원화
    val minOrder: Long = 0,
    val startDate: String = "",    // "YYYY-MM-DD"
    val endDate: String = "",
    val active: Boolean = true,
    val usageLimit: Long = 0,      // 0 = 제한없음
    val usedCount: Long = 0,
    val createdAt: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponPromotionScreen(
    storeId: String,
    onBack: () -> Unit = {}
) {
    val db = Firebase.firestore
    val coupons = remember { mutableStateListOf<Coupon>() }
    var query by remember { mutableStateOf("") }
    var filterOnlyActive by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Coupon?>(null) }

    // 권한
    var ownerUid by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(storeId) {
        db.collection("merchants").document(storeId).get().addOnSuccessListener { snap ->
            ownerUid = snap.getString("ownerUid")
        }
    }
    val myRole by rememberMyStoreRole(storeId, ownerUid)
    val canEdit = canEditCoupons(myRole)

    // 실시간 구독
    DisposableEffect(storeId) {
        if (storeId.isBlank()) return@DisposableEffect onDispose { }
        val ref = db.collection("merchants").document(storeId).collection("coupons")
        val reg = ref.orderBy("createdAt").addSnapshotListener { snap, _ ->
            snap ?: return@addSnapshotListener
            for (dc in snap.documentChanges) {
                val c = dc.document.toObject(Coupon::class.java).copy(id = dc.document.id)
                when (dc.type) {
                    DocumentChange.Type.ADDED -> if (coupons.none { it.id == c.id }) coupons.add(c)
                    DocumentChange.Type.MODIFIED -> coupons.indexOfFirst { it.id == c.id }.takeIf { it >= 0 }?.let { idx -> coupons[idx] = c }
                    DocumentChange.Type.REMOVED -> coupons.removeAll { it.id == c.id }
                }
            }
        }
        onDispose { reg.remove() }
    }

    val filtered = remember(coupons, query, filterOnlyActive) {
        coupons.filter {
            (query.isBlank() || it.title.contains(query, true) || it.code.contains(query, true)) &&
                    (!filterOnlyActive || it.active)
        }
    }.sortedWith(compareBy<Coupon>({ !it.active }, { it.title.lowercase() }))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("쿠폰/프로모션") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { filterOnlyActive = !filterOnlyActive }) {
                        Icon(if (filterOnlyActive) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                    }
                }
            )
        },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(onClick = { editing = null; showEditor = true }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("코드/제목 검색") },
                    singleLine = true, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = filterOnlyActive,
                    onClick = { filterOnlyActive = !filterOnlyActive },
                    label = { Text(if (filterOnlyActive) "활성만" else "전체") }
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { c ->
                    CouponRow(
                        coupon = c,
                        canEdit = canEdit,
                        onToggleActive = {
                            if (!canEdit) return@CouponRow
                            db.collection("merchants").document(storeId)
                                .collection("coupons").document(c.id)
                                .set(mapOf("active" to !c.active), SetOptions.merge())
                        },
                        onEdit = { editing = c; showEditor = true },
                        onDelete = {
                            if (!canEdit) return@CouponRow
                            db.collection("merchants").document(storeId)
                                .collection("coupons").document(c.id).delete()
                        }
                    )
                }
            }
        }
    }

    if (showEditor) {
        CouponEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSave = { data ->
                val col = db.collection("merchants").document(storeId).collection("coupons")
                if (data.id.isBlank()) {
                    col.add(
                        mapOf(
                            "code" to data.code.trim().uppercase(),
                            "title" to data.title.trim(),
                            "type" to data.type.name,
                            "value" to data.value,
                            "minOrder" to data.minOrder,
                            "startDate" to data.startDate.trim(),
                            "endDate" to data.endDate.trim(),
                            "active" to data.active,
                            "usageLimit" to data.usageLimit,
                            "usedCount" to 0L,
                            "createdAt" to Timestamp.now()
                        )
                    ).addOnSuccessListener { showEditor = false }
                } else {
                    col.document(data.id).set(
                        mapOf(
                            "code" to data.code.trim().uppercase(),
                            "title" to data.title.trim(),
                            "type" to data.type.name,
                            "value" to data.value,
                            "minOrder" to data.minOrder,
                            "startDate" to data.startDate.trim(),
                            "endDate" to data.endDate.trim(),
                            "active" to data.active,
                            "usageLimit" to data.usageLimit
                        ),
                        SetOptions.merge()
                    ).addOnSuccessListener { showEditor = false }
                }
            }
        )
    }
}

@Composable
private fun CouponRow(
    coupon: Coupon,
    canEdit: Boolean,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("${coupon.title} (${coupon.code})", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                val benefit = if (coupon.type == CouponType.PERCENT) "${coupon.value}%" else "${coupon.value}원"
                val period = listOf(coupon.startDate, coupon.endDate).filter { it.isNotBlank() }.joinToString(" ~ ")
                Text("$benefit · 최소 ${coupon.minOrder}원 · ${if (coupon.active) "활성" else "비활성"}${if (period.isNotBlank()) " · $period" else ""}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Row {
                IconButton(onClick = onToggleActive, enabled = canEdit) {
                    Icon(if (coupon.active) Icons.Default.ToggleOn else Icons.Default.ToggleOff, null)
                }
                IconButton(onClick = onEdit, enabled = canEdit) { Icon(Icons.Default.Edit, null) }
                IconButton(onClick = onDelete, enabled = canEdit) { Icon(Icons.Default.Delete, null) }
            }
        }
    }
}

@Composable
private fun CouponEditorDialog(
    initial: Coupon? = null,
    onDismiss: () -> Unit,
    onSave: (Coupon) -> Unit
) {
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: CouponType.PERCENT) }
    var valueText by remember { mutableStateOf((initial?.value ?: 10).toString()) }
    var minOrderText by remember { mutableStateOf((initial?.minOrder ?: 0).toString()) }
    var startDate by remember { mutableStateOf(initial?.startDate ?: "") } // YYYY-MM-DD
    var endDate by remember { mutableStateOf(initial?.endDate ?: "") }
    var active by remember { mutableStateOf(initial?.active ?: true) }
    var usageLimitText by remember { mutableStateOf((initial?.usageLimit ?: 0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "쿠폰 추가" else "쿠폰 수정") },
        text = {
            Column {
                OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("코드") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("제목") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row {
                    FilterChip(selected = type == CouponType.PERCENT, onClick = { type = CouponType.PERCENT }, label = { Text("퍼센트") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = type == CouponType.FIXED, onClick = { type = CouponType.FIXED }, label = { Text("정액") })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = valueText, onValueChange = { valueText = it.filter { ch -> ch.isDigit() } }, label = { Text(if (type == CouponType.PERCENT) "할인율(%)" else "할인액(원)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = minOrderText, onValueChange = { minOrderText = it.filter { ch -> ch.isDigit() } }, label = { Text("최소주문금액(원)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("시작일 (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("종료일 (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = usageLimitText, onValueChange = { usageLimitText = it.filter { ch -> ch.isDigit() } }, label = { Text("사용 한도 (0 = 제한없음)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = active, onCheckedChange = { active = it })
                    Spacer(Modifier.width(8.dp)); Text("활성화")
                }
            }
        },
        confirmButton = {
            val value = valueText.toLongOrNull() ?: 0
            val minOrder = minOrderText.toLongOrNull() ?: 0
            val limit = usageLimitText.toLongOrNull() ?: 0
            TextButton(
                enabled = code.isNotBlank() && title.isNotBlank() && value > 0,
                onClick = {
                    onSave(
                        Coupon(
                            id = initial?.id ?: "",
                            code = code.trim().uppercase(),
                            title = title.trim(),
                            type = type,
                            value = value,
                            minOrder = minOrder,
                            startDate = startDate.trim(),
                            endDate = endDate.trim(),
                            active = active,
                            usageLimit = limit
                        )
                    )
                }
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
