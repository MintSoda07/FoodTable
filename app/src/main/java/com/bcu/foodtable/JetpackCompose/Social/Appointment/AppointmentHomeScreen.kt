package com.bcu.foodtable.JetpackCompose.Social.Appointment

import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.Date

@Composable
fun AppointmentHomeScreen(
    nav: NavHostController,
    vm: AppointmentViewModel = viewModel()
) {
    val me = FirebaseAuth.getInstance().currentUser!!.uid
    var list by remember { mutableStateOf<List<Appointment>>(emptyList()) }

    // Firestore: acceptedIds 기준 실시간
    DisposableEffect(me) {
        val reg = vm.listenMyAppointments(me) { list = it }
        onDispose { reg.remove() }
    }

    // 🚧 UI 방어 로직: 혹시라도 잘못 들어온 데이터가 있어도 내가 수락한 것만 표시
    val acceptedOnly by remember(me, list) {
        mutableStateOf(list.filter { it.acceptedIds.contains(me) }.sortedBy { it.startAt })
    }

    if (acceptedOnly.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "약속이 없습니다.\n채팅에서 약속 초대를 수락하면 여기에 표시됩니다.",
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(acceptedOnly, key = { it.id }) { ap ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate("appointment/${ap.id}") }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(ap.title, style = MaterialTheme.typography.titleMedium)
                    Text("📍 ${ap.placeName}", color = Color.Gray)
                    Text(
                        formatApptRangeKorean(ap.startAt, ap.endAt),
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "참여자 ${ap.acceptedIds.size}명",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
