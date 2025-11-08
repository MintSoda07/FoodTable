// merchant/realtime/MerchantOrderRealtime.kt
package com.bcu.foodtable.ui.merchant.realtime

import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay

data class LiveOrderBrief(
    val orderId: String = "",
    val total: Long = 0,
    val storeId: String = "",
    val buyerUid: String = "",
    val status: String = "",
    val createdAtMs: Long = 0L
)

@Composable
fun MerchantOrderRealtimeBanner(
    storeId: String,
    modifier: Modifier = Modifier,
    // 새 주문 들어오면 호출 (원하면 Orders 화면으로 라우팅)
    onTapGoOrders: () -> Unit = {}
) {
    val db = Firebase.firestore
    val ctx = LocalContext.current
    var lastDocId by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    var brief by remember { mutableStateOf<LiveOrderBrief?>(null) }

    // 🔔 사운드/진동
    fun notifyHardware() {
        // 사운드
        runCatching {
            val tone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(ctx, tone)
            r?.play()
        }
        // 진동
        runCatching {
            val vib = ctx.getSystemService(Vibrator::class.java)
            vib?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // ⚡ 실시간 리스너
    DisposableEffect(storeId) {
        if (storeId.isBlank()) return@DisposableEffect onDispose { }
        // createdAt 은 serverTimestamp라 즉시 null일 수 있음 → where 사용하지 않고 orderBy만.
        val reg = db.collection("merchants").document(storeId)
            .collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                for (dc in snap.documentChanges) {
                    if (dc.type != DocumentChange.Type.ADDED) continue
                    val d = dc.document
                    val status = d.getString("status") ?: ""
                    if (status != "PAID") continue   // 결제완료만 알림

                    // 같은 문서를 중복 표출 방지
                    if (lastDocId == d.id) continue
                    lastDocId = d.id

                    val model = LiveOrderBrief(
                        orderId = d.getString("orderId") ?: d.id,
                        total = d.getLong("total") ?: 0L,
                        storeId = d.getString("storeId") ?: "",
                        buyerUid = d.getString("buyerUid") ?: "",
                        status = status,
                        createdAtMs = (d.getTimestamp("createdAt") ?: d.getTimestamp("paidAt"))?.toDate()?.time ?: 0L
                    )
                    brief = model
                    visible = true
                    notifyHardware()
                }
            }
        onDispose { reg.remove() }
    }

    // ⏳ 자동으로 6초 후 감춤
    LaunchedEffect(visible) {
        if (visible) {
            delay(6000)
            visible = false
        }
    }

    AnimatedVisibility(visible = visible) {
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 4.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("새 결제가 도착했습니다!", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "주문금액: %,d원".format(brief?.total ?: 0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = {
                    visible = false
                    onTapGoOrders()
                }) { Text("주문보기") }
            }
        }
    }
}
