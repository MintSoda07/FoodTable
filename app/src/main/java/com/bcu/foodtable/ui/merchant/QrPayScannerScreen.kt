// screens/QrPayScannerScreen.kt
@file:Suppress("DEPRECATION")

package com.bcu.foodtable.ui.merchant

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.material3.BottomSheetDefaults.DragHandle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.JetpackCompose.biometric.BiometricGateActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "QRBIO"

// QrCameraPreview / QrPayload / parseQrPayloadOrNull 은 기존 코드 사용

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrPayScannerScreen(
    onBack: () -> Unit = {},
    requireBiometric: Boolean = true
) {
    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid.orEmpty()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 어떤 인증을 허용할지 (약한 바이오메트릭+기기잠금)
    val allow = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    // ───────── 생체 인증 게이트 상태 ─────────
    var authed by rememberSaveable { mutableStateOf(!requireBiometric) }
    var prompting by rememberSaveable { mutableStateOf(false) }
    var bioError by remember { mutableStateOf<String?>(null) }
    var lastCode by remember { mutableStateOf<Int?>(null) }

    fun logState(where: String) {
        val can = BiometricManager.from(context).canAuthenticate(allow)
        Log.d(
            TAG,
            "[$where] authed=$authed, prompting=$prompting, can=$can (0=OK,11=NONE_ENROLLED,12=NO_HW,1=HW_UNAVAIL), lastCode=$lastCode, lastErr=$bioError"
        )
    }

    // BiometricGateActivity 실행 런처
    val gateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        prompting = false
        val ok = (res.resultCode == Activity.RESULT_OK) &&
                (res.data?.getBooleanExtra(BiometricGateActivity.EXTRA_SUCCESS, false) == true)
        if (ok) {
            authed = true
            bioError = null
            lastCode = null
            logState("GateResult(OK)")
            return@rememberLauncherForActivityResult
        }

        authed = false
        val code = res.data?.getIntExtra(BiometricGateActivity.EXTRA_ERROR_CODE, -9999)
        val msg = res.data?.getStringExtra(BiometricGateActivity.EXTRA_ERROR_MSG)
        lastCode = code
        bioError = msg ?: "인증이 취소되었거나 실패했습니다."
        logState("GateResult(CANCEL)")

        // 사전 canAuthenticate 실패 케이스(NONE_ENROLLED 등)
        // → “등록/잠금 설정” 버튼 노출을 위해 상태만 유지
    }

    fun launchGate() {
        if (prompting) return
        prompting = true
        bioError = null
        lastCode = null
        logState("launchGate()")
        gateLauncher.launch(
            BiometricGateActivity.createIntent(
                ctx = context,
                title = "결제 승인",
                subtitle = "얼굴/지문 또는 기기 잠금으로 승인하세요",
                allow = allow
            )
        )
    }

    // 생체 등록/기기잠금 설정 화면 런처
    val enrollLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // 설정에서 돌아오면 다시 게이트 실행
        authed = false
        prompting = false
        bioError = null
        lastCode = null
        logState("ReturnedFromEnroll")
        launchGate()
    }

    fun launchEnroll() {
        logState("launchEnroll()")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val enroll = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, allow)
                }
                enrollLauncher.launch(enroll)
            } else {
                enrollLauncher.launch(Intent(Settings.ACTION_SECURITY_SETTINGS))
            }
        } catch (_: Throwable) {
            enrollLauncher.launch(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
    }

    // 화면 진입 시 한 번 실행
    LaunchedEffect(requireBiometric) {
        logState("LaunchedEffect-enter")
        if (requireBiometric) launchGate()
    }

    // ───────── QR/결제 상태 ─────────
    var scanned by remember { mutableStateOf<QrPayload?>(null) }
    var processing by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var paidOrderId by remember { mutableStateOf<String?>(null) }

    var showPreview by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    val previewSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val resultSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun onBarcodeRead(text: String) {
        logState("onBarcodeRead()")
        if (!authed) return
        if (processing || showPreview || showResult) return
        val payload = parseQrPayloadOrNull(text) ?: return
        if (uid.isBlank() || payload.storeId.isBlank() || payload.orderId.isBlank()) {
            resultMessage = "유효하지 않은 QR 또는 로그인 필요"
            showResult = true
            return
        }
        scanned = payload
        showPreview = true
    }

    suspend fun verifyPayloadWithProducts(payload: QrPayload): Result<Unit> {
        val docBase = db.collection("merchants").document(payload.storeId)
        var recomputedSubtotal = 0L

        for (it in payload.items) {
            val pdoc = docBase.collection("products").document(it.pid).get().await()
            if (!pdoc.exists()) return Result.failure(IllegalArgumentException("존재하지 않는 상품: ${it.name}"))
            val price = pdoc.getLong("price") ?: 0L
            if (price <= 0) return Result.failure(IllegalArgumentException("상품 가격 오류: ${it.name}"))
            if (it.qty !in 1..99) return Result.failure(IllegalArgumentException("수량 범위 초과: ${it.name}"))
            if (it.amount != price * it.qty) return Result.failure(IllegalArgumentException("항목 금액 불일치: ${it.name}"))
            recomputedSubtotal += price * it.qty
        }

        if (payload.subtotal != recomputedSubtotal) {
            return Result.failure(IllegalArgumentException("합계 불일치(서버가격): ${recomputedSubtotal}원"))
        }
        val expectedTotal = (payload.subtotal - payload.discount).coerceAtLeast(0)
        if (payload.total != expectedTotal) {
            return Result.failure(IllegalArgumentException("총액 산식 불일치: ${expectedTotal}원"))
        }
        if (payload.currency != "KRW") {
            return Result.failure(IllegalArgumentException("지원하지 않는 통화입니다."))
        }
        return Result.success(Unit)
    }

    suspend fun processPayment(payload: QrPayload): String {
        if (uid.isBlank()) return "로그인이 필요합니다."
        if (!authed) return "결제 승인(생체/잠금)이 필요합니다."
        if (payload.storeId.isBlank() || payload.orderId.isBlank()) return "유효하지 않은 QR 데이터입니다."

        val verify = verifyPayloadWithProducts(payload)
        if (verify.isFailure) return verify.exceptionOrNull()?.message ?: "결제 데이터 검증 실패"

        val userRef = db.collection("user").document(uid)
        val storeRef = db.collection("merchants").document(payload.storeId)
        val orderRef = storeRef.collection("orders").document(payload.orderId)
        val saleRef  = storeRef.collection("sales").document(payload.orderId)

        return try {
            db.runTransaction { tx ->
                if (tx.get(saleRef).exists()) return@runTransaction "이미 처리된 결제입니다."

                val userSnap = tx.get(userRef)
                val curPoint = userSnap.getLong("point") ?: 0L
                if (curPoint < payload.total) {
                    throw IllegalStateException("포인트가 부족합니다. 보유: %,d원 / 필요: %,d원".format(curPoint, payload.total))
                }

                val now = FieldValue.serverTimestamp()
                val itemsMap = payload.items.map {
                    mapOf(
                        "pid" to it.pid,
                        "name" to it.name,
                        "price" to it.price,
                        "qty" to it.qty,
                        "amount" to it.amount
                    )
                }

                val orderData = mapOf(
                    "orderId" to payload.orderId,
                    "storeId" to payload.storeId,
                    "storeName" to payload.storeName,
                    "buyerUid" to uid,
                    "items" to itemsMap,
                    "subtotal" to payload.subtotal,
                    "vatShown" to payload.vatShown,
                    "discount" to payload.discount,
                    "total" to payload.total,
                    "coupon" to (payload.coupon ?: ""),
                    "currency" to payload.currency,
                    "method" to "POINT",
                    "status" to "PAID",
                    "createdAt" to now,
                    "paidAt" to now,
                    "source" to "QR_SCAN"
                )
                tx.set(orderRef, orderData, SetOptions.merge())

                val saleData = mapOf(
                    "orderId" to payload.orderId,
                    "storeId" to payload.storeId,
                    "storeName" to payload.storeName,
                    "buyerUid" to uid,
                    "items" to itemsMap,
                    "subtotal" to payload.subtotal,
                    "vatShown" to payload.vatShown,
                    "discount" to payload.discount,
                    "total" to payload.total,
                    "coupon" to (payload.coupon ?: ""),
                    "currency" to payload.currency,
                    "status" to "PAID",
                    "method" to "POINT",
                    "createdAt" to now
                )
                tx.set(saleRef, saleData, SetOptions.merge())

                tx.update(userRef, "point", curPoint - payload.total)

                val userPurchaseRef = userRef.collection("purchases").document(payload.orderId)
                tx.set(
                    userPurchaseRef,
                    mapOf(
                        "orderId" to payload.orderId,
                        "storeId" to payload.storeId,
                        "storeName" to payload.storeName,
                        "amount" to payload.total,
                        "createdAt" to now
                    ),
                    SetOptions.merge()
                )

                "결제가 완료되었습니다."
            }.await()
        } catch (e: Exception) {
            e.message ?: "결제 처리 중 오류가 발생했습니다."
        }
    }

    // ───────── UI ─────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR 결제 스캔") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {

            if (authed) {
                QrCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onBarcode = ::onBarcodeRead
                )
            }

            // 하단 안내
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CenterFocusWeak, null)
                    Text(
                        if (authed) "QR을 화면 가운데에 맞춰 스캔해 주세요."
                        else "결제 승인을 위해 생체/기기잠금을 먼저 진행해 주세요."
                    )
                }
            }

            // 🔐 인증 오버레이
            if (!authed) {
                Surface(
                    tonalElevation = 6.dp,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(48.dp))
                        Text("결제 승인 필요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        bioError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onBack) { Text("돌아가기") }

                            // 등록/잠금 설정 버튼: NONE_ENROLLED 이거나 canAuthenticate 실패 시 노출
                            val can = BiometricManager.from(context).canAuthenticate(allow)
                            if (can == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED || lastCode == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                                OutlinedButton(onClick = { launchEnroll() }) {
                                    Text("등록/잠금 설정")
                                }
                            }

                            Button(
                                onClick = { launchGate() },
                                enabled = !prompting
                            ) {
                                if (prompting) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                } else {
                                    Icon(Icons.Default.VerifiedUser, null)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(if (prompting) "확인 중..." else "다시 인증")
                            }
                        }
                    }
                }
            }

            // 처리 중 오버레이
            AnimatedVisibility(
                visible = processing,
                enter = scaleIn(),
                exit = scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(tonalElevation = 6.dp, shape = MaterialTheme.shapes.large) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("결제를 처리하고 있습니다...", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // ───────── 미리보기 시트 ─────────
    if (showPreview && scanned != null) {
        val s = scanned!!
        ModalBottomSheet(
            onDismissRequest = { if (!processing) showPreview = false },
            sheetState = previewSheet,
            dragHandle = { DragHandle() }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode2, null)
                    Spacer(Modifier.width(8.dp))
                    Text("결제 미리보기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("가맹점: ${s.storeName}", style = MaterialTheme.typography.titleMedium)
                if (!s.coupon.isNullOrBlank()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("쿠폰: ${s.coupon}") },
                        leadingIcon = { Icon(Icons.Default.CardGiftcard, null) }
                    )
                }

                Divider()

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(s.items, key = { it.pid }) { it ->
                        ElevatedCard {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(it.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(
                                        "%,d원 × %d".format(it.price, it.qty),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Text("%,d원".format(it.amount), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }

                ElevatedCard {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KeyRow("표시가(합)", "%,d원".format(s.subtotal))
                        KeyRow("부가세(표시)", "%,d원".format(s.vatShown), dim = true)
                        if (s.discount > 0) KeyRow("할인", "-%,d원".format(s.discount), accent = true)
                        Divider()
                        KeyRow("결제 금액", "%,d원".format(s.total), strong = true)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { if (!processing) { showPreview = false; scanned = null } },
                        modifier = Modifier.weight(1f)
                    ) { Text("취소") }

                    Button(
                        onClick = {
                            if (processing) return@Button
                            processing = true
                            scope.launch {
                                val msg = processPayment(s)
                                resultMessage = msg
                                paidOrderId = if (msg.startsWith("결제가 완료")) s.orderId else null
                                processing = false
                                showPreview = false
                                showResult = true
                            }
                        },
                        enabled = !processing && authed,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (processing) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Payments, null)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("결제")
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ───────── 결과 시트 ─────────
    if (showResult && resultMessage != null) {
        val success = paidOrderId != null
        ModalBottomSheet(
            onDismissRequest = {
                showResult = false
                resultMessage = null
                paidOrderId = null
                scanned = null
            },
            sheetState = resultSheet,
            dragHandle = { DragHandle() }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val pulse = rememberInfiniteTransition()
                val scale by pulse.animateFloat(
                    initialValue = 0.95f, targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                val icon = if (success) Icons.Filled.Celebration else Icons.Outlined.ErrorOutline
                val title = if (success) "결제 완료!" else "결제 실패"

                Icon(
                    icon, contentDescription = null,
                    modifier = Modifier.size(64.dp).scale(if (success) scale else 1f)
                )
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(resultMessage!!, style = MaterialTheme.typography.bodyMedium)

                if (success) {
                    ElevatedCard {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            KeyRow("주문번호", paidOrderId ?: "-", strong = true)
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showResult = false
                            resultMessage = null
                            paidOrderId = null
                            scanned = null
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("확인") }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun KeyRow(
    key: String,
    value: String,
    strong: Boolean = false,
    dim: Boolean = false,
    accent: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            key,
            style = MaterialTheme.typography.bodyMedium,
            color = if (dim) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
        )
        val style = if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        val color = when {
            accent -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }
        Text(value, style = style, color = color, fontWeight = if (strong) FontWeight.SemiBold else null)
    }
}
