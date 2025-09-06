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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreManageScreen(
    storeId: String,
    onBack: () -> Unit,
    onEditInfo: () -> Unit
) {
    val db = Firebase.firestore
    var profile by remember { mutableStateOf<StoreProfile?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(storeId) {
        if (storeId.isBlank()) return@LaunchedEffect
        db.collection("merchants").document(storeId).get().addOnSuccessListener { snap ->
            profile = snap.toObject(StoreProfile::class.java)?.copy(storeId = storeId) ?: StoreProfile(storeId = storeId)
            loading = false
        }.addOnFailureListener { loading = false }
    }

    fun savePatch(patch: Map<String, Any?>) {
        db.collection("merchants").document(storeId).set(patch, SetOptions.merge())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("가게관리") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "뒤로") } },
                actions = { IconButton(onClick = onEditInfo) { Icon(Icons.Default.Edit, contentDescription = "가게정보관리") } }
            )
        }
    ) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val p = profile ?: StoreProfile(storeId = storeId)

        // ✅ 스크롤 가능 리스트
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(p.storeName.ifBlank { "가맹점" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatusChip(label = if (p.openNow) "영업중" else "영업종료", icon = if (p.openNow) Icons.Default.CheckCircle else Icons.Default.DoNotDisturb)
                            StatusChip(label = if (p.onlineOrderEnabled) "주문가능" else "주문중지", icon = Icons.Default.Wifi)
                            StatusChip(label = p.category.ifBlank { "미분류" }, icon = Icons.Default.LocalDining)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    profile = p.copy(openNow = !p.openNow)
                                    savePatch(mapOf("openNow" to !p.openNow))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Icon(Icons.Default.Schedule, null); Spacer(Modifier.width(6.dp)); Text(if (p.openNow) "지금 영업종료" else "지금 영업시작") }
                            FilledTonalButton(
                                onClick = {
                                    profile = p.copy(onlineOrderEnabled = !p.onlineOrderEnabled)
                                    savePatch(mapOf("onlineOrderEnabled" to !p.onlineOrderEnabled))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Icon(Icons.Default.ShoppingCart, null); Spacer(Modifier.width(6.dp)); Text(if (p.onlineOrderEnabled) "주문 일시중지" else "주문 재개") }
                        }
                    }
                }
            }

            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("운영 채널", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SwitchRow("포장(Takeout) 허용", p.takeoutEnabled) {
                            profile = p.copy(takeoutEnabled = it); savePatch(mapOf("takeoutEnabled" to it))
                        }
                        SwitchRow("매장 식사(Dine-In) 허용", p.dineInEnabled) {
                            profile = p.copy(dineInEnabled = it); savePatch(mapOf("dineInEnabled" to it))
                        }
                    }
                }
            }

            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("휴무일 설정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        WeekdayMultiSelector(selected = p.daysOff.toSet()) { sel ->
                            profile = p.copy(daysOff = sel.toList()); savePatch(mapOf("daysOff" to sel.toList()))
                        }
                    }
                }
            }

            item {
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("고객 안내", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        KeyValueRow("전화번호", p.phone)
                        KeyValueRow("주소", p.address)
                        KeyValueRow("최소주문금액", if (p.minOrderPrice > 0) "${p.minOrderPrice}원" else "없음")
                        KeyValueRow("부가세율", "${p.taxPercent}%")
                    }
                }
            }
        }
    }
}

@Composable private fun StatusChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    AssistChip(onClick = {}, label = { Text(label) }, leadingIcon = { Icon(icon, null) })
}

@Composable private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable private fun KeyValueRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(value.ifBlank { "-" }, style = MaterialTheme.typography.bodyMedium)
    }
}
