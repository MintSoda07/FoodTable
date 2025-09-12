package com.bcu.foodtable.JetpackCompose.Social.Appointment

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(
    apptId: String,
    nav: NavHostController,
    vm: AppointmentViewModel = viewModel()
) {
    val me = FirebaseAuth.getInstance().currentUser!!.uid
    var ap by remember { mutableStateOf<Appointment?>(null) }
    var parts by remember { mutableStateOf<List<AppointmentParticipant>>(emptyList()) }
    var isOwner by remember { mutableStateOf(false) }
    var inDevice by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // 확인 다이얼로그
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 참여자 이름 캐시 (uid -> name)
    var nameMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // 약속/참여자 실시간 구독
    DisposableEffect(apptId) {
        val reg = vm.listenAppointment(apptId) { a, p ->
            ap = a
            parts = p
            isOwner = (a.creatorUid == me)
        }
        onDispose { reg.remove() }
    }

    // 디바이스 캘린더 매핑 여부
    LaunchedEffect(apptId, me) {
        val m = db.collection("appointments").document(apptId)
            .collection("deviceEvents").document(me).get().await()
        inDevice = m.exists()
    }

    // 참여자 이름 로드 (whereIn: 10개씩)
    LaunchedEffect(parts) {
        val ids = parts.map { it.uid }.distinct().filter { it.isNotBlank() }
        if (ids.isEmpty()) {
            nameMap = emptyMap()
            return@LaunchedEffect
        }
        val tmp = mutableMapOf<String, String>()
        ids.chunked(10).forEach { chunk ->
            val snap = db.collection("user")
                .whereIn(FieldPath.documentId(), chunk)
                .get().await()
            snap.documents.forEach { d ->
                val name = d.getString("name")?.takeIf { it.isNotBlank() }
                    ?: d.getString("nickname")?.takeIf { it.isNotBlank() }
                    ?: d.id.take(6)
                tmp[d.id] = name
            }
        }
        // user 문서가 없는 uid 폴백
        ids.forEach { uid -> if (uid !in tmp) tmp[uid] = uid.takeLast(6) }
        nameMap = tmp.toMap()
    }

    // 로딩
    val a = ap
    if (a == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // ---- 캘린더 권한/요청 런처 ----
    val calendarPerms = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )
    var hasCalPerm by remember {
        mutableStateOf(calendarPerms.all {
            ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    var pendingAdd by remember { mutableStateOf(false) } // 권한 허용 후 자동 추가용

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        hasCalPerm = res.values.all { it }
        if (hasCalPerm && pendingAdd) {
            // 권한 방금 허용됨 → 실제 추가 실행
            scope.launch {
                val ok = runCatching { addToDeviceCalendarAndMap(ctx, apptId, a) }.getOrDefault(false)
                inDevice = ok
                pendingAdd = false
                Toast.makeText(ctx, if (ok) "캘린더에 추가됨" else "추가 실패(캘린더 없음/쓰기불가)", Toast.LENGTH_SHORT).show()
            }
        } else if (!hasCalPerm) {
            pendingAdd = false
            Toast.makeText(ctx, "캘린더 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(a.title) },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제")
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📍 ${a.placeName}")
            Text(
                formatApptRangeKorean(a.startAt, a.endAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            if (a.note.isNotBlank()) Text("📝 ${a.note}")

            Divider()

            // 참여자 이름 + 상태
            Text("참여자", style = MaterialTheme.typography.titleMedium)
            parts.forEach { p ->
                val statusKo = when (p.status) {
                    "accepted" -> "수락"
                    "declined" -> "거절"
                    else -> "대기"
                }
                val display = if (p.uid == me) "나" else (nameMap[p.uid] ?: p.uid.takeLast(6))
                Text("• $display : $statusKo")
            }

            Divider()

            // 내 응답 버튼
            val myStatus = parts.firstOrNull { it.uid == me }?.status ?: "pending"
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (myStatus != "accepted") {
                    Button(
                        onClick = {
                            scope.launch {
                                runCatching { vm.accept(apptId, me) }
                                    .onSuccess { Toast.makeText(ctx, "수락했습니다.", Toast.LENGTH_SHORT).show() }
                                    .onFailure { Toast.makeText(ctx, it.message ?: "수락 실패", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    ) { Text("수락") }
                }
                if (myStatus != "declined") {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                runCatching { vm.decline(apptId, me) }
                                    .onSuccess { Toast.makeText(ctx, "거절했습니다.", Toast.LENGTH_SHORT).show() }
                                    .onFailure { Toast.makeText(ctx, it.message ?: "거절 실패", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    ) { Text("거절") }
                }
            }

            // 디바이스 캘린더 스위치
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("내 디바이스 캘린더에 추가")
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = inDevice,
                    onCheckedChange = { checked ->
                        scope.launch {
                            if (checked) {
                                if (!hasCalPerm) {
                                    pendingAdd = true
                                    permLauncher.launch(calendarPerms)
                                    return@launch
                                }
                                val ok = runCatching { addToDeviceCalendarAndMap(ctx, apptId, a) }.getOrDefault(false)
                                inDevice = ok
                                Toast.makeText(
                                    ctx,
                                    if (ok) "캘린더에 추가됨" else "추가 실패(캘린더 없음/쓰기불가)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val ok = runCatching { removeFromDeviceCalendarByMap(ctx, apptId) }.getOrDefault(false)
                                inDevice = !ok
                                Toast.makeText(
                                    ctx,
                                    if (ok) "캘린더에서 제거됨" else "제거 실패",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("약속 삭제") },
            text = { Text("모든 참여자에게서 이 약속이 삭제됩니다. 계속할까요?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        scope.launch {
                            runCatching { vm.cancel(apptId, me) }
                                .onSuccess {
                                    Toast.makeText(ctx, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    nav.popBackStack()
                                }
                                .onFailure {
                                    Toast.makeText(ctx, it.message ?: "삭제 실패", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                ) { Text("삭제") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            }
        )
    }
}

/* --------- 한국형 약속 시간 포맷터 --------- */

private val tzSeoul: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

private fun sameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance(tzSeoul, Locale.KOREA).apply { timeInMillis = a }
    val cb = Calendar.getInstance(tzSeoul, Locale.KOREA).apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

/**
 * 예)
 * - 같은 날: "9월 11일(목) 오전 11:00 ~ 오후 1:00"
 * - 다른 날: "9월 11일(목) 오전 11:00 ~ 9월 12일(금) 오후 1:00"
 */
fun formatApptRangeKorean(startMillis: Long, endMillis: Long): String {
    val dayFmt = SimpleDateFormat("M월 d일(E)", Locale.KOREA).apply { timeZone = tzSeoul }
    val timeFmt = SimpleDateFormat("a h:mm", Locale.KOREA).apply { timeZone = tzSeoul }

    return if (sameDay(startMillis, endMillis)) {
        "${dayFmt.format(Date(startMillis))} ${timeFmt.format(Date(startMillis))} ~ ${timeFmt.format(Date(endMillis))}"
    } else {
        "${dayFmt.format(Date(startMillis))} ${timeFmt.format(Date(startMillis))} ~ " +
                "${dayFmt.format(Date(endMillis))} ${timeFmt.format(Date(endMillis))}"
    }
}
