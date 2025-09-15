// com/bcu/foodtable/JetpackCompose/Social/Appointment/AppointmentInviteBubble.kt
package com.bcu.foodtable.JetpackCompose.Social.Appointment

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.JetpackCompose.Social.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


@Composable
fun AppointmentInviteBubble(
    message: ChatMessage,
    onOpen: (String) -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid
    val scope = rememberCoroutineScope()

    // appointmentId 우선, 없으면 딥링크(foodtable://appointment?id=XXX)에서 파싱
    val apptId = remember(message) {
        when {
            !message.appointmentId.isNullOrBlank() -> message.appointmentId!!
            !message.deeplink.isNullOrBlank() -> runCatching {
                val uri = Uri.parse(message.deeplink)
                uri.getQueryParameter("id")?.let {
                    URLDecoder.decode(it, StandardCharsets.UTF_8.name())
                }
            }.getOrNull()
            else -> null
        } ?: ""
    }
    val enabled = apptId.isNotBlank()

    // 약속 본문 1회 로딩 (시간/참여자 상태 표시용)
    var ap by remember { mutableStateOf<Appointment?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(apptId) {
        loading = true
        ap = null
        if (enabled) {
            runCatching {
                val doc = db.collection("appointments").document(apptId).get().await()
                ap = doc.toObject(Appointment::class.java)?.copy(id = doc.id)
            }
        }
        loading = false
    }

    ElevatedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Event, contentDescription = null)
                Text(text = "약속 초대", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))

            // 제목(약속 이름): 우선 메시지 텍스트, 없으면 로드한 약속 제목
            val title = message.text?.takeIf { it.isNotBlank() } ?: ap?.title
            title?.let {
                Text(text = it, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
            }

            // 장소명: 우선 메시지, 없으면 로드한 약속의 placeName
            val placeName = message.placeName?.takeIf { it.isNotBlank() } ?: ap?.placeName
            placeName?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Place, contentDescription = null)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(6.dp))
            }

            // ✅ 한국식 시간 범위 (약속 로드되었을 때만 노출)
            if (!loading && ap != null) {
                Text(
                    text = formatApptRangeKorean(ap!!.startAt, ap!!.endAt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(8.dp))
            } else if (loading) {
                Text(
                    text = "약속 정보를 불러오는 중…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }

            // 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (enabled) onOpen(apptId) },
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) { Text("약속 보기") }

                val urlForMap = message.placeUrl?.takeIf { it.isNotBlank() } ?: ap?.placeUrl
                urlForMap?.let { url ->
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("지도")
                    }
                }
            }

            // 수락/거절 (내 상태 보고 버튼 노출)
            if (enabled && myUid != null && ap != null) {
                val alreadyAccepted = ap!!.acceptedIds.contains(myUid)
                Spacer(Modifier.height(8.dp))
                if (!alreadyAccepted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    runCatching { AppointmentViewModel().accept(apptId, myUid) }
                                        .onSuccess {
                                            // 로컬 반영
                                            ap = ap!!.copy(acceptedIds = ap!!.acceptedIds + myUid)
                                        }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("수락") }

                        TextButton(
                            onClick = {
                                scope.launch {
                                    runCatching { AppointmentViewModel().decline(apptId, myUid) }
                                        .onSuccess {
                                            // 거절 후에도 '약속 보기'는 가능하게 유지 (로컬 반영은 생략)
                                        }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("거절") }
                    }
                } else {
                    Text(
                        "참여 확정됨",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
