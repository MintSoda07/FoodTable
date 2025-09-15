package com.bcu.foodtable.JetpackCompose.Social.Appointment

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.JetpackCompose.Social.Openchat.OpenChatRoom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentCreateDialog(
    defaultTitle: String,
    placeName: String,
    placeUrl: String?,
    lat: Double, lng: Double,
    onDismiss: () -> Unit,
    onConfirm: (Appointment, List<String>, String?) -> Unit
) {
    val me = FirebaseAuth.getInstance().currentUser!!.uid
    val db = FirebaseFirestore.getInstance()

    var title by remember { mutableStateOf(defaultTitle) }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) } // 필요시 DatePicker로 교체
    var startHour by remember { mutableStateOf(19) }
    var startMin by remember { mutableStateOf(0) }
    var durationMin by remember { mutableStateOf(120) }

    val dmTargets = remember { mutableStateListOf<String>() }
    var openRoomId by remember { mutableStateOf<String?>(null) }
    val friends = remember { mutableStateListOf<User>() }
    val myOpenRooms = remember { mutableStateListOf<OpenChatRoom>() }

    // ✅ 수신자 검증: 친구(1명 이상) 또는 오픈채팅(1개) 중 최소 하나
    val hasAudience by remember { derivedStateOf { dmTargets.isNotEmpty() || !openRoomId.isNullOrBlank() } }
    val canSend by remember { derivedStateOf { hasAudience && title.isNotBlank() } }

    LaunchedEffect(Unit) {
        val snap = db.collection("user").document(me).collection("friends").get().await()
        val ids = snap.documents.map { it.id }
        ids.forEach { uid ->
            db.collection("user").document(uid).get().await()
                .toObject(User::class.java)?.copy(uid = uid)?.let { friends += it }
        }
        val rooms = db.collection("openRooms").whereArrayContains("memberIds", me).get().await()
        rooms.documents.mapNotNull { it.toObject(OpenChatRoom::class.java)?.copy(id = it.id) }
            .also { myOpenRooms.addAll(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("약속 잡기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목") },
                    supportingText = {
                        if (title.isBlank()) Text("제목을 입력하세요.", color = MaterialTheme.colorScheme.error)
                    }
                )
                Text(placeName, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startHour.toString(),
                        onValueChange = { s -> s.filter(Char::isDigit).toIntOrNull()?.let { startHour = it.coerceIn(0, 23) } },
                        label = { Text("시") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = startMin.toString(),
                        onValueChange = { s -> s.filter(Char::isDigit).toIntOrNull()?.let { startMin = it.coerceIn(0, 59) } },
                        label = { Text("분") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = durationMin.toString(),
                        onValueChange = { s -> s.filter(Char::isDigit).toIntOrNull()?.let { durationMin = it.coerceAtLeast(15) } },
                        label = { Text("소요(분)") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("메모") })

                // ===== 친구 선택 =====
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("보낼 친구(여러 명 선택 가능)")
                    if (friends.isEmpty()) {
                        Text("친구 목록이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        friends.forEach { f ->
                            val checked = dmTargets.contains(f.uid)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { ch ->
                                        if (ch) dmTargets += f.uid else dmTargets.remove(f.uid)
                                    }
                                )
                                Text(f.name)
                            }
                        }
                        if (dmTargets.isNotEmpty()) {
                            Text("선택된 친구: ${dmTargets.size}명", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // ===== 오픈채팅 선택 =====
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("오픈채팅으로도 보내기(선택)")
                    OpenRoomDropdown(
                        items = myOpenRooms.map { it.id to it.title },
                        selected = openRoomId,
                        onSelected = { openRoomId = it }
                    )
                    if (!openRoomId.isNullOrBlank()) {
                        Text("선택된 오픈채팅: 1개", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // 수신자 가이드
                if (!hasAudience) {
                    Text(
                        "보낼 친구를 1명 이상 선택하거나 오픈채팅을 지정하세요.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = date
                        set(java.util.Calendar.HOUR_OF_DAY, startHour)
                        set(java.util.Calendar.MINUTE, startMin)
                        set(java.util.Calendar.SECOND, 0)
                    }
                    val start = cal.timeInMillis
                    val end = start + durationMin * 60_000L
                    val ap = Appointment(
                        title = title,
                        creatorUid = me,
                        placeName = placeName,
                        placeUrl = placeUrl,
                        lat = lat, lng = lng,
                        startAt = start, endAt = end,
                        note = note
                    )
                    onConfirm(ap, dmTargets.toList(), openRoomId)
                },
                enabled = canSend
            ) { Text("보내기") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenRoomDropdown(
    items: List<Pair<String, String>>, // id to title
    selected: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val title = items.firstOrNull { it.first == selected }?.second ?: "선택 안 함"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = title, onValueChange = {}, readOnly = true, label = { Text("오픈채팅") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("선택 안 함") }, onClick = { onSelected(null); expanded = false })
            items.forEach { (id, t) ->
                DropdownMenuItem(text = { Text(t) }, onClick = { onSelected(id); expanded = false })
            }
        }
    }
}
