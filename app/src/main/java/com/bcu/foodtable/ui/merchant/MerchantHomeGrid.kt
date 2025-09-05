package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun MerchantHomeGrid(
    onStoreManage: (String) -> Unit = {},
    onQrPay: (String) -> Unit = {},
    onSales: () -> Unit = {},
    onStoreInfo: (String) -> Unit = {},
    onOrders: () -> Unit = {},
    onProducts: (String) -> Unit = {},
    onStaff: (String) -> Unit = {},      // ✅ String 받도록
    onCoupons: (String) -> Unit = {},    // ✅ String 받도록
    onSettlements: () -> Unit = {},
    onReports: () -> Unit = {},
    onSettings: () -> Unit = {},
)  {
    val db = Firebase.firestore
    val uid = Firebase.auth.currentUser?.uid.orEmpty()

    var userLoaded by rememberSaveable { mutableStateOf(false) }

    var userName by rememberSaveable { mutableStateOf("사용자") }
    var roleLabel by rememberSaveable { mutableStateOf("일반") }
    var storeId by rememberSaveable { mutableStateOf<String?>(null) }
    var storeName by rememberSaveable { mutableStateOf("가맹점") }

    LaunchedEffect(uid) {
        if (uid.isBlank()) {
            userLoaded = true
            return@LaunchedEffect
        }
        db.collection("user").document(uid).get().addOnSuccessListener { snap ->
            userName = snap.getString("name") ?: "사용자"
            val roles = (snap.get("roles") as? List<*>)?.map { it.toString() } ?: emptyList()
            roleLabel = if ("merchant" in roles) "가맹점" else "일반"

            val sId = snap.getString("storeId")
            storeId = sId
            val directStoreName = snap.getString("storeName")
            if (!directStoreName.isNullOrBlank()) storeName = directStoreName

            if (!sId.isNullOrBlank()) {
                db.collection("merchants").document(sId).get().addOnSuccessListener { m ->
                    val mName = m.getString("storeName")
                    if (!mName.isNullOrBlank()) storeName = mName
                    userLoaded = true
                }.addOnFailureListener { userLoaded = true }
            } else {
                userLoaded = true
            }
        }.addOnFailureListener { userLoaded = true }
    }

    // 다이얼로그는 로딩 완료 이후 판단 (깜빡임 방지)
    var showSetup by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(userLoaded, storeId, storeName, roleLabel) {
        if (!userLoaded) return@LaunchedEffect
        val needRole = roleLabel != "가맹점"
        val needStore = storeId.isNullOrBlank() || storeName.isBlank() || storeName == "가맹점"
        showSetup = (uid.isNotBlank() && (needRole || needStore))
    }

    val tiles = remember {
        listOf(
            MerchantTile("store_mgmt", "가게관리", Icons.Default.Tune, span = 2, height = 128.dp),
            MerchantTile("qr_pay", "QR 결제 생성", Icons.Default.QrCode, span = 2, height = 160.dp),
            MerchantTile("sales", "매출관리", Icons.Default.RequestQuote, span = 1, height = 120.dp),
            MerchantTile("store_info", "가게정보관리", Icons.Default.Info, span = 1, height = 120.dp),
            MerchantTile("orders", "주문관리", Icons.Default.ReceiptLong, span = 1, height = 120.dp),
            MerchantTile("products", "메뉴/상품관리", Icons.Default.Inventory2, span = 1, height = 120.dp),
            MerchantTile("staff", "직원관리", Icons.Default.Badge, span = 1, height = 120.dp),
            MerchantTile("coupons", "쿠폰/프로모션", Icons.Default.CardGiftcard, span = 1, height = 120.dp),
            MerchantTile("settlement", "정산/입금", Icons.Default.AccountBalance, span = 1, height = 120.dp),
            MerchantTile("reports", "통계/리포트", Icons.Default.Insights, span = 1, height = 120.dp),
            MerchantTile("settings", "설정/권한", Icons.Default.Settings, span = 2, height = 120.dp),
        )
    }

    Scaffold(
        topBar = { TopBarMerchant(storeName = storeName, role = roleLabel, userName = userName) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(items = tiles, key = { it.key }, span = { GridItemSpan(it.span) }) { tile ->
                MerchantTileCard(
                    title = tile.title,
                    icon = tile.icon,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tile.height),
                    onClick = {
                        when (tile.key) {
                            "store_mgmt" -> storeId?.let(onStoreManage) ?: run { if (userLoaded) showSetup = true }
                            "qr_pay"     -> storeId?.let(onQrPay)      ?: run { if (userLoaded) showSetup = true } // ✅
                            "sales"      -> onSales()
                            "store_info" -> storeId?.let(onStoreInfo) ?: run { if (userLoaded) showSetup = true }
                            "orders"     -> onOrders()
                            "products"   -> storeId?.let(onProducts)  ?: run { if (userLoaded) showSetup = true }
                            "staff"      -> storeId?.let(onStaff)     ?: run { if (userLoaded) showSetup = true }   // ✅ 변경
                            "coupons"    -> storeId?.let(onCoupons)   ?: run { if (userLoaded) showSetup = true }   // ✅ 변경
                            "settlement" -> onSettlements()
                            "reports"    -> onReports()
                            "settings"   -> onSettings()
                        }

                    }
                )
            }
        }
    }

    if (showSetup) {
        MerchantSetupDialog(
            currentStoreName = if (storeName == "가맹점") "" else storeName,
            onDismiss = { showSetup = false },
            onConfirm = { inputName ->
                val sid = storeId ?: "store_${uid}"
                val db = Firebase.firestore
                val batch = db.batch()
                val storeRef = db.collection("merchants").document(sid)
                val userRef = db.collection("user").document(uid)

                batch.set(
                    storeRef,
                    mapOf(
                        "storeId" to sid,
                        "storeName" to inputName,
                        "ownerUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "status" to "ACTIVE"
                    ),
                    SetOptions.merge()
                )
                batch.set(
                    userRef,
                    mapOf(
                        "roles" to listOf("user", "merchant"),
                        "storeId" to sid,
                        "storeName" to inputName,
                        "merchantSince" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                batch.commit().addOnSuccessListener {
                    storeId = sid
                    storeName = inputName
                    roleLabel = "가맹점"
                    showSetup = false
                }
            }
        )
    }
}

@Composable
private fun TopBarMerchant(storeName: String, role: String, userName: String) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = storeName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp, fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$role, $userName",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun MerchantTileCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

private data class MerchantTile(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val span: Int,
    val height: androidx.compose.ui.unit.Dp
)

@Composable
private fun MerchantSetupDialog(
    currentStoreName: String,
    onDismiss: () -> Unit,
    onConfirm: (storeName: String) -> Unit
) {
    var name by remember { mutableStateOf(currentStoreName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) }
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("나중에") } },
        title = { Text("가맹점 정보 설정") },
        text = {
            Column {
                Text("첫 로그인처럼 보여요. 가맹점 상호를 입력해 주세요.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("상호명") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
