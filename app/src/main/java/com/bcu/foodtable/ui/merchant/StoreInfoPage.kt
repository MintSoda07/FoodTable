package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreInfoScreen(
    storeId: String,
    onBack: () -> Unit
) {
    val db = Firebase.firestore
    var p by remember { mutableStateOf(StoreProfile(storeId = storeId)) }
    var loaded by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(storeId) {
        if (storeId.isBlank()) return@LaunchedEffect
        db.collection("merchants").document(storeId).get()
            .addOnSuccessListener { snap ->
                p = snap.toObject(StoreProfile::class.java)?.copy(storeId = storeId)
                    ?: StoreProfile(storeId = storeId)
                loaded = true
            }
            .addOnFailureListener { loaded = true }
    }

    fun saveAll() {
        saving = true
        db.collection("merchants").document(storeId)
            .set(p.copy(createdAt = p.createdAt ?: Timestamp.now()), SetOptions.merge())
            .addOnCompleteListener { saving = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("가게정보관리") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "뒤로") } },
                actions = {
                    TextButton(onClick = { saveAll() }, enabled = loaded && !saving) {
                        if (saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("저장")
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            // 기본 정보
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("기본 정보", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = p.storeName,
                            onValueChange = { p = p.copy(storeName = it) },
                            label = { Text("상호명") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Store, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = p.category,
                            onValueChange = { p = p.copy(category = it) }, // ❗미분류 강제 제거
                            label = { Text("업종/카테고리") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.LocalDining, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = p.description,
                            onValueChange = { p = p.copy(description = it) },
                            label = { Text("소개/설명") },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 연락처 · 정책
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("연락처 · 정책", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = p.phone,
                            onValueChange = { s -> p = p.copy(phone = s.filter { ch -> ch.isDigit() || ch == '-' }) },
                            label = { Text("전화번호") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = p.address,
                            onValueChange = { p = p.copy(address = it) },
                            label = { Text("주소") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            // ❗ Long? ↔ 입력필드: 빈칸이면 null 저장
                            val minOrderText = p.minOrderPrice?.toString() ?: ""
                            OutlinedTextField(
                                value = minOrderText,
                                onValueChange = { txt ->
                                    val digits = txt.filter { it.isDigit() }
                                    p = p.copy(minOrderPrice = digits.takeIf { it.isNotEmpty() }?.toLong())
                                },
                                label = { Text("최소주문금액(원)") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                                modifier = Modifier.weight(1f)
                            )
                            // ❗ Double? ↔ 입력필드: 빈칸이면 null 저장, 범위 0~30 제한
                            val taxText = p.taxPercent?.toString() ?: ""
                            OutlinedTextField(
                                value = taxText,
                                onValueChange = { txt ->
                                    val v = txt.replace(',', '.').toDoubleOrNull()
                                    p = p.copy(taxPercent = v?.coerceIn(0.0, 30.0))
                                },
                                label = { Text("부가세(%)") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Percent, null) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // ❗ Boolean? 표시만 기본(false), 저장은 선택 값 그대로
                        val online = p.onlineOrderEnabled ?: false
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = online,
                                onCheckedChange = { p = p.copy(onlineOrderEnabled = it) }
                            )
                            Spacer(Modifier.width(8.dp)); Text("온라인 주문 허용")
                        }
                    }
                }
            }

            // 주간 영업시간
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("주간 영업시간", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        listOf("MON","TUE","WED","THU","FRI","SAT","SUN").forEach { d ->
                            val cur = p.bizHours[d] ?: BusinessHours()
                            BusinessHourRow(label = d, hours = cur) { nh ->
                                p = p.copy(bizHours = p.bizHours.toMutableMap().also { it[d] = nh })
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(
                            "※ 휴무일은 가게관리 > 휴무일 설정에서 선택하세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // 가게 상태
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("가게 상태", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = p.status == "ACTIVE", onClick = { p = p.copy(status = "ACTIVE") }, label = { Text("ACTIVE") })
                            FilterChip(selected = p.status == "PAUSED", onClick = { p = p.copy(status = "PAUSED") }, label = { Text("PAUSED") })
                            FilterChip(selected = p.status == "CLOSED", onClick = { p = p.copy(status = "CLOSED") }, label = { Text("CLOSED") })
                        }
                    }
                }
            }

            // 하단 버튼
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("취소") }
                    Button(onClick = { saveAll() }, enabled = !saving, modifier = Modifier.weight(1f)) {
                        if (saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("저장")
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessHourRow(
    label: String,
    hours: BusinessHours,
    onChange: (BusinessHours) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(48.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = hours.open,
                onValueChange = { t -> onChange(hours.copy(open = t.take(5))) },
                label = { Text("오픈") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = hours.close,
                onValueChange = { t -> onChange(hours.copy(close = t.take(5))) },
                label = { Text("마감") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = hours.closed, onCheckedChange = { onChange(hours.copy(closed = it)) })
            Text("휴무")
        }
    }
}
