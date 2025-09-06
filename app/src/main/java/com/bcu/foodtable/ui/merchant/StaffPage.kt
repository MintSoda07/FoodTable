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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    storeId: String,
    onBack: () -> Unit = {}
) {
    val db = Firebase.firestore
    val staff = remember { mutableStateListOf<StaffProfile>() }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StaffProfile?>(null) }

    // 권한
    var ownerUid by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(storeId) {
        db.collection("merchants").document(storeId).get().addOnSuccessListener { snap ->
            ownerUid = snap.getString("ownerUid")
        }
    }
    val myRole by rememberMyStoreRole(storeId, ownerUid)
    val canEdit = canEditStaff(myRole)

    // 실시간 구독
    DisposableEffect(storeId) {
        if (storeId.isBlank()) return@DisposableEffect onDispose { }
        val ref = db.collection("merchants").document(storeId).collection("staff")
        val reg = ref.addSnapshotListener { snap, _ ->
            snap ?: return@addSnapshotListener
            for (dc in snap.documentChanges) {
                val sp = StaffProfile(
                    uid = dc.document.getString("uid").orEmpty(),
                    name = dc.document.getString("name").orEmpty(),
                    role = when (dc.document.getString("role")) {
                        "OWNER" -> StoreRole.OWNER
                        "MANAGER" -> StoreRole.MANAGER
                        "STAFF" -> StoreRole.STAFF
                        else -> StoreRole.VIEWER
                    },
                    phone = dc.document.getString("phone").orEmpty(),
                    email = dc.document.getString("email").orEmpty(),
                    active = dc.document.getBoolean("active") ?: true
                )
                when (dc.type) {
                    DocumentChange.Type.ADDED -> if (staff.none { it.uid == sp.uid }) staff.add(sp)
                    DocumentChange.Type.MODIFIED -> staff.indexOfFirst { it.uid == sp.uid }.takeIf { it >= 0 }?.let { idx -> staff[idx] = sp }
                    DocumentChange.Type.REMOVED -> staff.removeAll { it.uid == sp.uid }
                }
            }
        }
        onDispose { reg.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("직원관리") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (canEdit) {
                        IconButton(onClick = { editing = null; showEditor = true }) { Icon(Icons.Default.PersonAdd, null) }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(staff, key = { it.uid }) { s ->
                StaffRow(
                    staff = s,
                    canEdit = canEdit,
                    myRole = myRole,
                    onToggleActive = {
                        if (!canEdit) return@StaffRow
                        db.collection("merchants").document(storeId).collection("staff").document(s.uid)
                            .set(mapOf("active" to !s.active), SetOptions.merge())
                    },
                    onChangeRole = { newRole ->
                        if (!canChangeRole(myRole, s.role, newRole)) return@StaffRow
                        db.collection("merchants").document(storeId).collection("staff").document(s.uid)
                            .set(mapOf("role" to newRole.name), SetOptions.merge())
                    },
                    onEditInfo = {
                        editing = s
                        showEditor = true
                    },
                    onRemove = {
                        if (myRole != StoreRole.OWNER) return@StaffRow // remove는 OWNER만 하게끔
                        db.collection("merchants").document(storeId).collection("staff").document(s.uid)
                            .delete()
                    }
                )
            }
        }
    }

    if (showEditor) {
        StaffEditorDialog(
            initial = editing,
            canChangeRole = { targetOld, targetNew -> canChangeRole(myRole, targetOld, targetNew) },
            onDismiss = { showEditor = false },
            onSave = { data ->
                val col = db.collection("merchants").document(storeId).collection("staff")
                val docId = data.uid.ifBlank { data.email.ifBlank { data.phone } }.ifBlank { System.currentTimeMillis().toString() }
                col.document(docId).set(
                    mapOf(
                        "uid" to data.uid, // 실 서비스에서는 초대/가입 플로우에서 uid 매핑 추천
                        "name" to data.name.trim(),
                        "email" to data.email.trim(),
                        "phone" to data.phone.trim(),
                        "role" to data.role.name,
                        "active" to data.active,
                        "createdAt" to Timestamp.now()
                    ),
                    SetOptions.merge()
                ).addOnSuccessListener { showEditor = false }
            }
        )
    }
}

@Composable
private fun StaffRow(
    staff: StaffProfile,
    canEdit: Boolean,
    myRole: StoreRole,
    onToggleActive: () -> Unit,
    onChangeRole: (StoreRole) -> Unit,
    onEditInfo: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, null)
                Spacer(Modifier.width(8.dp))
                Text("${staff.name.ifBlank { "(이름없음)" }}  ·  ${staff.role.name}${if (!staff.active) " · 비활성" else ""}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Row {
                    IconButton(onClick = onEditInfo, enabled = canEdit) { Icon(Icons.Default.Edit, null) }
                    IconButton(onClick = onRemove, enabled = (myRole == StoreRole.OWNER)) { Icon(Icons.Default.Delete, null) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("이메일: ${staff.email.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Text("전화: ${staff.phone.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("상태: ")
                AssistChip(onClick = {}, label = { Text(if (staff.active) "활성" else "비활성") }, leadingIcon = { Icon(if (staff.active) Icons.Default.CheckCircle else Icons.Default.Block, null) })
                Spacer(Modifier.width(8.dp))
                Text("역할: ")
                RoleSelector(current = staff.role, enabled = canEdit, myRole = myRole, onChange = onChangeRole)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onToggleActive, enabled = canEdit) { Text(if (staff.active) "비활성화" else "활성화") }
            }
        }
    }
}

@Composable
private fun RoleSelector(
    current: StoreRole,
    enabled: Boolean,
    myRole: StoreRole,
    onChange: (StoreRole) -> Unit
) {
    // 단순 토글 버튼들
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(StoreRole.OWNER, StoreRole.MANAGER, StoreRole.STAFF).forEach { r ->
            FilterChip(
                selected = current == r,
                onClick = {
                    if (!enabled) return@FilterChip
                    // MANAGER는 OWNER 관련 변경 불가
                    if (!canChangeRole(myRole, current, r)) return@FilterChip
                    onChange(r)
                },
                label = { Text(r.name) }
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun StaffEditorDialog(
    initial: StaffProfile? = null,
    canChangeRole: (StoreRole, StoreRole) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (StaffProfile) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var role by remember { mutableStateOf(initial?.role ?: StoreRole.STAFF) }
    var active by remember { mutableStateOf(initial?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "직원 추가" else "직원 정보 수정") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("이메일") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '-' } }, label = { Text("전화번호") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("역할")
                Row {
                    listOf(StoreRole.OWNER, StoreRole.MANAGER, StoreRole.STAFF).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = {
                                if (initial == null) {
                                    // 신규 추가 시에도 역할 제한 반영
                                    if (canChangeRole(StoreRole.VIEWER, r)) role = r
                                } else {
                                    if (canChangeRole(initial.role, r)) role = r
                                }
                            },
                            label = { Text(r.name) }
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = active, onCheckedChange = { active = it })
                    Spacer(Modifier.width(8.dp)); Text("활성화")
                }
                Spacer(Modifier.height(4.dp))
                Text("※ 실제 회원(UID) 매핑/초대는 추후 이메일 초대 흐름에서 연결 권장", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        StaffProfile(
                            uid = initial?.uid ?: "",
                            name = name.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            role = role,
                            active = active
                        )
                    )
                }
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
