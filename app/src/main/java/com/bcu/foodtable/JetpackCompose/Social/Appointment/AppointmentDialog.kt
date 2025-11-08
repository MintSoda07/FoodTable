package com.bcu.foodtable.JetpackCompose.Social.Appointment

import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.JetpackCompose.Social.Openchat.OpenChatRoom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Lottie
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentCreateDialog(
    defaultTitle: String,
    placeName: String,
    placeUrl: String?,
    lat: Double, lng: Double,
    onDismiss: () -> Unit,
    // ✅ suspend 콜백: 내부에서 로딩을 돌리고, 성공 시 true를 리턴해 다이얼로그를 닫음
    onConfirm: suspend (Appointment, List<String>, String?) -> Boolean
) {
    val me = FirebaseAuth.getInstance().currentUser!!.uid
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(defaultTitle) }
    var note by remember { mutableStateOf("") }

    // 날짜(자정 기준) + 시간(state)
    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var dateMillis by remember { mutableLongStateOf(todayMidnight) }
    val timeState = rememberTimePickerState(initialHour = 19, initialMinute = 0, is24Hour = false)

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 소요시간: 프리셋 + 직접설정
    val durationPresets = listOf(30, 60, 90, 120)
    var durationMin by remember { mutableIntStateOf(120) }
    var showDurationDialog by remember { mutableStateOf(false) }

    // 대상 선택
    val dmTargets = remember { mutableStateListOf<String>() }
    var openRoomId by remember { mutableStateOf<String?>(null) }
    val friends = remember { mutableStateListOf<User>() }
    val myOpenRooms = remember { mutableStateListOf<OpenChatRoom>() }
    var friendQuery by remember { mutableStateOf("") }
    val filteredFriends by remember(friendQuery, friends) {
        mutableStateOf(
            if (friendQuery.isBlank()) friends
            else friends.filter { u ->
                val name = (u.name ?: "").lowercase()
                val email = (u.email ?: "").lowercase()
                val q = friendQuery.lowercase()
                name.contains(q) || email.contains(q)
            }
        )
    }

    val hasAudience by remember { derivedStateOf { dmTargets.isNotEmpty() || !openRoomId.isNullOrBlank() } }
    val canSendBase by remember { derivedStateOf { title.isNotBlank() && hasAudience && durationMin > 0 } }

    // ✅ 로딩 상태: 데이터/전송
    var loadingData by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    val inputsEnabled = !sending
    val canSend = canSendBase && !sending

    // 데이터 로드
    LaunchedEffect(Unit) {
        loadingData = true
        try {
            val snap = db.collection("user").document(me).collection("friends").get().await()
            val ids = snap.documents.map { it.id }
            ids.forEach { uid ->
                db.collection("user").document(uid).get().await()
                    .toObject(User::class.java)?.copy(uid = uid)?.let { friends += it }
            }
            val rooms = db.collection("openRooms").whereArrayContains("memberIds", me).get().await()
            rooms.documents.mapNotNull { it.toObject(OpenChatRoom::class.java)?.copy(id = it.id) }
                .also { myOpenRooms.addAll(it) }
        } finally {
            loadingData = false
        }
    }

    val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    val scrollState = rememberScrollState()

    val startCal = remember(dateMillis, timeState.hour, timeState.minute) {
        Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, timeState.hour)
            set(Calendar.MINUTE, timeState.minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
    }
    val startAt = startCal.timeInMillis
    val endAt = startAt + durationMin * 60_000L

    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = {
            Column {
                Text(
                    "약속 잡기",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(100),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = placeName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        text = {
            // 로딩(데이터 로딩 / 전송) 땐 meet.lottie 크게
            if (loadingData || sending) {
                MeetLottieBig(
                    text = if (loadingData) "데이터 불러오는 중…" else "초대 전송 중…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxDialogHeight)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 기본 정보
                    SectionCard(title = "기본 정보") {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("제목") },
                            supportingText = {
                                if (title.isBlank()) Text("제목을 입력하세요.", color = MaterialTheme.colorScheme.error)
                            },
                            enabled = inputsEnabled,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { showDatePicker = true },
                                enabled = inputsEnabled,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(koreanDate(dateMillis))
                            }
                            FilledTonalButton(
                                onClick = { showTimePicker = true },
                                enabled = inputsEnabled,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.AccessTime, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(koreanTime(timeState.hour, timeState.minute))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // 참고: formatApptRangeKorean은 프로젝트 내 기존 헬퍼를 사용
                        Text(
                            formatApptRangeKorean(startAt, endAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 소요시간
                    SectionCard(title = "소요시간") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(durationPresets) { m ->
                                FilterChip(
                                    selected = durationMin == m,
                                    onClick = { if (inputsEnabled) durationMin = m },
                                    label = { Text(presentDuration(m)) },
                                    enabled = inputsEnabled
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = { if (inputsEnabled) showDurationDialog = true },
                                    label = { Text("직접 설정") },
                                    enabled = inputsEnabled
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "예상: ${presentDuration(durationMin)} • 종료 ${koreanTimeFrom(startAt, durationMin)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 메모
                    SectionCard(title = "메모") {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("메모 (선택)") },
                            enabled = inputsEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                        )
                    }

                    // 대상 선택
                    SectionCard(title = "보낼 대상") {
                        OutlinedTextField(
                            value = friendQuery,
                            onValueChange = { friendQuery = it },
                            label = { Text("친구 검색(이름/이메일)") },
                            singleLine = true,
                            enabled = inputsEnabled,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))

                        if (friends.isEmpty()) {
                            Text("친구 목록이 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (filteredFriends.isEmpty()) {
                            Text("검색 결과가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredFriends, key = { it.uid ?: it.hashCode().toString() }) { f ->
                                    val uid = f.uid ?: return@items
                                    val checked = uid in dmTargets
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { ch ->
                                                if (!inputsEnabled) return@Checkbox
                                                if (ch) { if (uid !in dmTargets) dmTargets += uid }
                                                else dmTargets.remove(uid)
                                            },
                                            enabled = inputsEnabled
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(f.name ?: uid.takeLast(6), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        OpenRoomDropdown(
                            items = myOpenRooms.map { it.id to it.title },
                            selected = openRoomId,
                            onSelected = { if (inputsEnabled) openRoomId = it },
                            enabled = inputsEnabled
                        )

                        Spacer(Modifier.height(6.dp))
                        if (!hasAudience) {
                            Text(
                                "보낼 친구를 1명 이상 선택하거나 오픈채팅을 지정하세요.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                when {
                                    dmTargets.isNotEmpty() && !openRoomId.isNullOrBlank() ->
                                        "선택된 친구 ${dmTargets.size}명 • 오픈채팅 1개"
                                    dmTargets.isNotEmpty() ->
                                        "선택된 친구 ${dmTargets.size}명"
                                    else ->
                                        "선택된 오픈채팅 1개"
                                },
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sending) return@Button
                    val start = startAt
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
                    sending = true
                    scope.launch {
                        val ok = runCatching { onConfirm(ap, dmTargets.toList(), openRoomId) }
                            .getOrElse { false }
                        sending = false
                        if (ok) onDismiss()
                    }
                },
                enabled = canSend
            ) {
                Text(if (sending) "보내는 중…" else "보내기")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!sending) onDismiss() }, enabled = !sending) { Text("취소") }
        }
    )

    /* ----------------- Pickers ----------------- */

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateMillis = dateState.selectedDateMillis ?: dateMillis
                        showDatePicker = false
                    }
                ) { Text("확인") }
            },
            dismissButton = { TextButton({ showDatePicker = false }) { Text("취소") } }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        TimePickerDialogM3(
            state = timeState,
            onDismissRequest = { showTimePicker = false },
            onConfirm = { showTimePicker = false }
        )
    }

    if (showDurationDialog) {
        var temp by remember { mutableIntStateOf(durationMin) }
        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            title = { Text("소요시간 직접 설정") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(presentDuration(temp), style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = temp.toFloat(),
                        onValueChange = { v ->
                            val snapped = (v / 15f).toInt() * 15
                            temp = snapped.coerceIn(15, 240)
                        },
                        valueRange = 15f..240f,
                        steps = 15
                    )
                    Text("15분 ~ 4시간", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { durationMin = temp; showDurationDialog = false }) { Text("적용") }
            },
            dismissButton = { TextButton({ showDurationDialog = false }) { Text("취소") } }
        )
    }
}

/* ---------- Custom M3 Time Picker Dialog ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogM3(
    state: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { TextButton(onClick = onConfirm) { Text("확인") } },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("취소") } },
        text = { TimePicker(state = state) }
    )
}

/* ---------- Helpers ---------- */

@Composable
private fun MeetLottieBig(
    text: String,
    modifier: Modifier = Modifier
) {
    val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.meet))
    val progress by animateLottieCompositionAsState(
        composition = comp,
        iterations = LottieConstants.IterateForever,
        speed = 1.0f
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = comp,
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1.5f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

private fun koreanDate(millis: Long): String {
    val tz = TimeZone.getTimeZone("Asia/Seoul")
    val fmt = SimpleDateFormat("M월 d일(E)", Locale.KOREA).apply { timeZone = tz }
    return fmt.format(Date(millis))
}

private fun koreanTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
    }
    val tz = TimeZone.getTimeZone("Asia/Seoul")
    val fmt = SimpleDateFormat("a h:mm", Locale.KOREA).apply { timeZone = tz }
    return fmt.format(cal.time)
}

private fun koreanTimeFrom(startAt: Long, plusMin: Int): String {
    val tz = TimeZone.getTimeZone("Asia/Seoul")
    val fmt = SimpleDateFormat("a h:mm", Locale.KOREA).apply { timeZone = tz }
    return fmt.format(Date(startAt + plusMin * 60_000L))
}

private fun presentDuration(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h > 0 && m > 0 -> "${h}시간 ${m}분"
        h > 0 -> "${h}시간"
        else -> "${m}분"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenRoomDropdown(
    items: List<Pair<String, String>>,
    selected: String?,
    onSelected: (String?) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val title = items.firstOrNull { it.first == selected }?.second ?: "선택 안 함"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("오픈채팅") },
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
