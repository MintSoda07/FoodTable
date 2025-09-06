package com.bcu.foodtable.ui.merchant

import ads_mobile_sdk.dp
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.floor
import kotlin.random.Random

/* ───────────────────────── 주문 라인(장바구니) ───────────────────────── */

data class OrderLine(
    val productId: String,
    val name: String,
    val unitPrice: Long, // 단가(표시가, 부가세 포함)
    val qty: Int,
    val category: String
) { val amount: Long get() = unitPrice * qty }

/* ───────────────────────── 표시 전용 세금 분해 ───────────────────────── */
// 세금은 총액에 추가하지 않음(표시만): base + vat = amount(표시가)
private fun splitTax(amount: Long, taxPercent: Double): Pair<Long, Long> {
    val vat = floor(amount * (taxPercent / 100.0)).toLong()
    val base = amount - vat
    return base to vat
}

private fun calculateDiscount(subtotal: Long, coupon: Coupon?): Long {
    coupon ?: return 0L
    if (!coupon.active) return 0L
    if (coupon.minOrder > subtotal) return 0L
    return when (coupon.type) {
        CouponType.PERCENT -> floor(subtotal * (coupon.value / 100.0)).toLong()
        CouponType.FIXED -> coupon.value.coerceAtMost(subtotal)
    }
}

/* ───────────────────────── 메인 화면 ───────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrOrderScreen(
    storeId: String,
    onBack: () -> Unit = {}
) {
    val db = Firebase.firestore

    // 가게 정보
    var profile by remember { mutableStateOf(StoreProfile(storeId = storeId)) }
    var loadedProfile by remember { mutableStateOf(false) }
    LaunchedEffect(storeId) {
        if (storeId.isBlank()) return@LaunchedEffect
        db.collection("merchants").document(storeId).get()
            .addOnSuccessListener { snap ->
                profile = snap.toObject(StoreProfile::class.java)?.copy(storeId = storeId)
                    ?: StoreProfile(storeId = storeId)
                loadedProfile = true
            }.addOnFailureListener { loadedProfile = true }
    }

    // 상품 실시간
    val products = remember { mutableStateListOf<Product>() }
    DisposableEffect(storeId) {
        if (storeId.isBlank()) return@DisposableEffect onDispose { }
        val reg = db.collection("merchants").document(storeId)
            .collection("products").addSnapshotListener { snap, _ ->
                snap ?: return@addSnapshotListener
                for (dc in snap.documentChanges) {
                    val p = dc.document.toObject(Product::class.java).copy(id = dc.document.id)
                    when (dc.type) {
                        DocumentChange.Type.ADDED ->
                            if (products.none { it.id == p.id }) products.add(p)
                        DocumentChange.Type.MODIFIED ->
                            products.indexOfFirst { it.id == p.id }.takeIf { it >= 0 }?.let { idx -> products[idx] = p }
                        DocumentChange.Type.REMOVED ->
                            products.removeAll { it.id == p.id }
                    }
                }
            }
        onDispose { reg.remove() }
    }

    // 장바구니/쿠폰 상태
    val cart = remember { mutableStateListOf<OrderLine>() }
    var couponCode by remember { mutableStateOf("") }
    var appliedCoupon: Coupon? by remember { mutableStateOf(null) }
    var applying by remember { mutableStateOf(false) }

    // 금액 계산: 총 결제금액 = 표시가(부가세포함) 합 - 할인 (세금은 표시만)
    val subtotal = cart.sumOf { it.amount }
    val shownVatSum = cart.sumOf { splitTax(it.amount, profile.taxPercent).second }
    val discount = calculateDiscount(subtotal, appliedCoupon)
    val total = (subtotal - discount).coerceAtLeast(0)

    // QR 모달
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showQr by remember { mutableStateOf(false) }

    fun addToCart(p: Product) {
        val idx = cart.indexOfFirst { it.productId == p.id }
        if (idx >= 0) {
            val old = cart[idx]
            cart[idx] = old.copy(qty = (old.qty + 1).coerceAtMost(99))
        } else {
            cart.add(OrderLine(p.id, p.name, p.price, 1, p.category))
        }
    }
    fun decLine(id: String) {
        val idx = cart.indexOfFirst { it.productId == id }
        if (idx >= 0) {
            val old = cart[idx]; val q = old.qty - 1
            if (q <= 0) cart.removeAt(idx) else cart[idx] = old.copy(qty = q)
        }
    }
    fun removeLine(id: String) { cart.removeAll { it.productId == id } }
    fun clearCoupon() { appliedCoupon = null; couponCode = "" }
    fun applyCouponByCode() {
        val code = couponCode.trim().uppercase()
        if (code.isBlank()) return
        applying = true
        db.collection("merchants").document(storeId).collection("coupons")
            .whereEqualTo("code", code).get()
            .addOnSuccessListener { qs ->
                val doc = qs.documents.firstOrNull()
                val c = doc?.toObject(Coupon::class.java)?.copy(id = doc.id)
                val ok = c != null && c.active && (c.minOrder <= subtotal)
                appliedCoupon = if (ok) c else null
                applying = false
            }.addOnFailureListener { applying = false }
    }

    fun buildPayload(): String {
        val orderId = "OFF-${System.currentTimeMillis()}-${Random.nextInt(1000, 9999)}"
        val itemsArr = JSONArray()
        cart.forEach {
            itemsArr.put(JSONObject().apply {
                put("pid", it.productId); put("name", it.name)
                put("price", it.unitPrice); put("qty", it.qty); put("amount", it.amount)
            })
        }
        return JSONObject().apply {
            put("type", "OFFLINE_ORDER_QR"); put("ver", 1)
            put("storeId", storeId); put("storeName", profile.storeName)
            put("orderId", orderId); put("currency", "KRW")
            put("subtotal", subtotal)               // 표시가 합
            put("vatShown", shownVatSum)            // 표시용 부가세 합
            put("discount", discount)
            put("total", total)                     // 결제금액 (표시가 - 할인)
            put("coupon", appliedCoupon?.code ?: "")
            put("ts", Timestamp.now().seconds)
            put("items", itemsArr)
        }.toString()
    }
    fun generateQr() {
        val json = buildPayload()
        qrBitmap = generateQrImageBitmap(json, sizePx = 900)
        showQr = true
    }

    // 하단 주문서 높이(접힘/펼침)
    val CART_COLLAPSED_HEIGHT = 240.dp
    val CART_EXPANDED_HEIGHT = 420.dp
    var cartExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR 결제 생성") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { cartExpanded = !cartExpanded }) {
                        Icon(if (cartExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess, null)
                    }
                }
            )
        },
        floatingActionButton = {
            // ExtendedFloatingActionButton 은 enabled 파라미터가 없어 클릭 가드 + 색상으로 표현
            ExtendedFloatingActionButton(
                onClick = { if (cart.isNotEmpty()) generateQr() },
                icon = { Icon(Icons.Default.QrCode, null) },
                text = { Text("QR 생성") },
                expanded = true,
                containerColor = if (cart.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (cart.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
            )
        }
    ) { padding ->
        if (!loadedProfile) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 1) 상단 요약
                SummaryBar(
                    profile = profile,
                    subtotal = subtotal,
                    vatShown = shownVatSum,
                    discount = discount,
                    total = total
                )

                // 2) 가운데: 메뉴(카테고리 접기/펼치기)
                //    weight 없이 fraction으로 공간 배분
                ProductPickerPanelCollapsible(
                    products = products.filter { it.available },
                    onTap = ::addToCart,
                    fractionHeight = 0.60f // 화면의 60% 차지
                )

                // 3) 하단: 주문서(고정 높이 + 접기/펼치기)
                Spacer(Modifier.height(8.dp))
                OrderCartPaneBottom(
                    heightDp = if (cartExpanded) CART_EXPANDED_HEIGHT else CART_COLLAPSED_HEIGHT,
                    cart = cart,
                    taxPercent = profile.taxPercent,
                    couponCode = couponCode,
                    appliedCoupon = appliedCoupon,
                    applying = applying,
                    onInc = { id -> products.find { it.id == id }?.let { addToCart(it) } },
                    onDec = ::decLine,
                    onRemove = ::removeLine,
                    onChangeCoupon = { couponCode = it },
                    onApplyCoupon = ::applyCouponByCode,
                    onClearCoupon = ::clearCoupon,
                    onToggle = { cartExpanded = !cartExpanded }
                )
            }
        }
    }

    if (showQr) {
        AlertDialog(
            onDismissRequest = { showQr = false },
            title = { Text("결제 QR") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    qrBitmap?.let {
                        Image(bitmap = it, contentDescription = "결제 QR", modifier = Modifier.size(320.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("고객용 앱에서 이 QR을 스캔해 결제를 진행하세요.", style = MaterialTheme.typography.bodySmall)
                    } ?: CircularProgressIndicator()
                }
            },
            confirmButton = { TextButton(onClick = { showQr = false }) { Text("닫기") } }
        )
    }
}

/* ───────────────────────── 상단 요약 바 ───────────────────────── */

@Composable
private fun SummaryBar(
    profile: StoreProfile,
    subtotal: Long,   // 표시가 합(부가세 포함)
    vatShown: Long,   // 표시용 부가세 합
    discount: Long,
    total: Long
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    profile.storeName.ifBlank { "가맹점" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "부가세 ${profile.taxPercent.toInt()}% (표시)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("표시가(부가세포함): %,d원".format(subtotal))
                Text("부가세(표시용): %,d원".format(vatShown), color = MaterialTheme.colorScheme.outline)
                if (discount > 0) Text("할인: -%,d원".format(discount), color = MaterialTheme.colorScheme.primary)
                Divider(Modifier.padding(vertical = 4.dp))
                Text("결제금액: %,d원".format(total), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ───────────────────────── 메뉴(카테고리 접기/펼치기) ───────────────────────── */

@Composable
private fun ProductPickerPanelCollapsible(
    products: List<Product>,
    onTap: (Product) -> Unit,
    fractionHeight: Float = 0.6f
) {
    val heightFrac = remember(fractionHeight) { fractionHeight.coerceIn(0f, 1f) }
    var query by remember { mutableStateOf("") }

    // 검색 필터링
    val filtered = remember(products, query) {
        val q = query.trim()
        if (q.isBlank()) products
        else products.filter { it.name.contains(q, true) || it.category.contains(q, true) }
    }

    // 카테고리 그룹
    val grouped = remember(filtered) {
        filtered.groupBy { it.category.ifBlank { "미분류" } }
            .toSortedMap(compareBy<String> { if (it == "미분류") "zzz" else it.lowercase() })
    }

    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(grouped.keys) {
        grouped.keys.forEach { cat -> if (expandedMap[cat] == null) expandedMap[cat] = true }
        expandedMap.keys.filter { it !in grouped.keys }.forEach { expandedMap.remove(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(heightFrac)
    ) {
        // 🔹 상단 UI: 검색(1행) + 펼치기/접기 버튼행(2행)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("메뉴 검색") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                }
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { grouped.keys.forEach { expandedMap[it] = true } }) {
                    Icon(Icons.Default.UnfoldMore, null)
                    Spacer(Modifier.width(8.dp))
                    Text("전체 펼치기")
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = { grouped.keys.forEach { expandedMap[it] = false } }) {
                    Icon(Icons.Default.UnfoldLess, null)
                    Spacer(Modifier.width(6.dp))
                    Text("전체 접기")
                }
                // 필요하면 여유 공간 채우기
                Spacer(Modifier.weight(1f))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            grouped.forEach { (cat, list) ->
                item(key = "hdr_$cat") {
                    ElevatedCard(onClick = { expandedMap[cat] = !(expandedMap[cat] ?: true) }) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (expandedMap[cat] == true) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                cat,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("${list.size}개") },
                                leadingIcon = { Icon(Icons.Default.Inventory2, null) }
                            )
                        }
                    }
                }

                if (expandedMap[cat] == true) {
                    items(list, key = { it.id }) { p ->
                        ElevatedCard(onClick = { onTap(p) }) {
                            Box(Modifier.fillMaxWidth().padding(12.dp)) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(end = 96.dp)
                                ) {
                                    Text(p.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(
                                        "부가세 포함 %,d원".format(p.price),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onTap(p) },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    Icon(Icons.Default.AddShoppingCart, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("담기")
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}


/* ───────────────────────── 하단 주문서(고정 높이, 토글) ───────────────────────── */

// 2) 컴포저블 시그니처/내용 교체
@Composable
private fun OrderCartPaneBottom(
    heightDp: androidx.compose.ui.unit.Dp,   // ← Dp 로 수정
    cart: MutableList<OrderLine>,
    taxPercent: Double,
    couponCode: String,
    appliedCoupon: Coupon?,
    applying: Boolean,
    onInc: (String) -> Unit,
    onDec: (String) -> Unit,
    onRemove: (String) -> Unit,
    onChangeCoupon: (String) -> Unit,
    onApplyCoupon: () -> Unit,
    onClearCoupon: () -> Unit,
    onToggle: () -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(heightDp)               // ← 여기도 Dp 사용
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // weight 대신 SpaceBetween 사용
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("주문서", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onToggle) {
                    Icon(Icons.Default.SwapVert, contentDescription = "크기 전환")
                }
            }

            if (cart.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("메뉴를 선택해 담아주세요.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(cart, key = { it.productId }) { line ->
                        val (base, vat) = splitTax(line.amount, taxPercent)
                        ElevatedCard {
                            Box(Modifier.fillMaxWidth().padding(12.dp)) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(end = 140.dp)
                                ) {
                                    Text(line.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    Text(
                                        "%,d원 × %d = %,d원".format(line.unitPrice, line.qty, line.amount),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        "기본가 %,d + 부가세 %,d".format(base, vat),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                ) {
                                    IconButton(onClick = { onDec(line.productId) }) { Icon(Icons.Default.RemoveCircle, null) }
                                    Text("${line.qty}", modifier = Modifier.padding(horizontal = 6.dp))
                                    IconButton(onClick = { onInc(line.productId) }) { Icon(Icons.Default.AddCircle, null) }
                                    IconButton(onClick = { onRemove(line.productId) }) { Icon(Icons.Default.Delete, null) }
                                }
                            }
                        }
                    }

                    // 쿠폰 영역
                    item {
                        Spacer(Modifier.height(4.dp))
                        if (appliedCoupon == null) {
                            Column {
                                OutlinedTextField(
                                    value = couponCode,
                                    onValueChange = onChangeCoupon,
                                    placeholder = { Text("쿠폰 코드") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Button(onClick = onApplyCoupon, enabled = !applying) {
                                        if (applying) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                        else Text("적용")
                                    }
                                }
                            }
                        } else {
                            ElevatedCard {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f, fill = false)) {
                                        Text(
                                            "쿠폰 적용됨",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text("${appliedCoupon.code} · ${appliedCoupon.title}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    TextButton(onClick = onClearCoupon) { Text("해제") }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

