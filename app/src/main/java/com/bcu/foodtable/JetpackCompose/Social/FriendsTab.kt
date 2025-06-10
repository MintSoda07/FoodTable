package com.bcu.foodtable.JetpackCompose.Social

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FriendsTab(
    navController: NavHostController
) {
    val context    = LocalContext.current
    val clipboard  = LocalClipboardManager.current
    val colors     = lightColorScheme(
        primary = Color(0xFFE25532),
        onPrimary = Color.White,

        primaryContainer = Color(0xFFFFE2D6),
        onPrimaryContainer = Color(0xFF5C2B1B),

        secondary = Color(0xFFFFF4ED),
        onSecondary = Color(0xFF4B3C35),

        secondaryContainer = Color(0xFFFDE1D5),
        onSecondaryContainer = Color(0xFF5D4037),

        tertiary = Color(0xFFB9806D),
        onTertiary = Color.White,

        tertiaryContainer = Color(0xFFF3E0DC),
        onTertiaryContainer = Color(0xFF4E342E),

        background = Color(0xFFFFFBF8),
        onBackground = Color(0xFF3A2C28),

        surface = Color.White,
        onSurface = Color(0xFF2E2E2E),

        surfaceVariant = Color(0xFFFBE7DF),
        onSurfaceVariant = Color(0xFF5F5F5F),

        outline = Color(0xFFDDC7BD),
        outlineVariant = Color(0xFFF0E0D8),

        inverseSurface = Color(0xFF3A2C28),
        inverseOnSurface = Color.White,
        inversePrimary = Color(0xFFFF8F6B),

        error = Color(0xFFD32F2F),
        onError = Color.White,
        errorContainer = Color(0xFFFDECEA),
        onErrorContainer = Color(0xFF8B0000),)
    val currentUid = UserManager.getUser()!!.uid
    val scope      = rememberCoroutineScope()

    // 친구 목록 및 요청 목록 상태
    val friends   = remember { mutableStateListOf<User>() }
    val requests  = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(true) }

    // 이름 검색 상태
    var nameQuery     by rememberSaveable { mutableStateOf("") }
    val searchResults = remember { mutableStateListOf<User>() }
    var isSearching   by remember { mutableStateOf(false) }

    // UID 요청 다이얼로그 상태
    var showAddDialog     by remember { mutableStateOf(false) }
    var addUidText        by rememberSaveable { mutableStateOf("") }
    var isAdding          by remember { mutableStateOf(false) }
    var showRequestsModal by remember { mutableStateOf(false) }

    // 초기 로드: 친구 목록 & 요청 목록
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val db = Firebase.firestore
            // 친구 목록
            val snapF = db.collection("user").document(currentUid)
                .collection("friends").get().await()
            friends.clear()
            snapF.documents.forEach { doc ->
                db.collection("user").document(doc.id).get().await()
                    .toObject(User::class.java)
                    ?.let { friends.add(it.copy(uid = doc.id)) }
            }
            // 받은 요청
            val snapR = db.collection("user").document(currentUid)
                .collection("friendRequests").get().await()
            requests.clear()
            snapR.documents.forEach { doc ->
                db.collection("user").document(doc.id).get().await()
                    .toObject(User::class.java)
                    ?.let { requests.add(it.copy(uid = doc.id)) }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "초기 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // 이름 검색
    LaunchedEffect(nameQuery) {
        if (nameQuery.isBlank()) {
            searchResults.clear()
            isSearching = false
        } else {
            isSearching = true
            searchResults.clear()
            try {
                val snap = Firebase.firestore
                    .collection("user")
                    .orderBy("name")
                    .startAt(nameQuery)
                    .endAt("$nameQuery\uf8ff")
                    .get().await()
                snap.documents.forEach { doc ->
                    doc.toObject(User::class.java)
                        ?.let { searchResults.add(it.copy(uid = doc.id)) }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "검색 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSearching = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            // 상단: 내 UID, 공유, 요청 알림 버튼
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("내 UID: $currentUid", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(currentUid))
                    Toast.makeText(context, "내 UID 복사됨", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = colors.primary)
                }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
                    intent.putExtra("sms_body", "내 UID: $currentUid")
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = colors.primary)
                }
                BadgedBox(
                    badge = {
                        if (requests.isNotEmpty()) {
                            Badge { Text(requests.size.toString()) }
                        }
                    }
                ) {
                    IconButton(onClick = { showRequestsModal = true }) {
                        Icon(Icons.Default.Mail, contentDescription = "요청 보기", tint = colors.primary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 검색 바 + 추가 버튼
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value         = nameQuery,
                    onValueChange = { nameQuery = it },
                    label         = { Text("이름으로 검색") },
                    modifier      = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "UID로 친구 요청", tint = colors.primary)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 친구 목록 / 검색 결과
            Box(Modifier.fillMaxSize()) {
                when {
                    // 로딩 모달
                    isLoading -> {
                        Dialog(onDismissRequest = {}) {
                            Box(
                                Modifier.size(200.dp).clip(CircleShape).background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.profile))
                                val prog by animateLottieCompositionAsState(comp, iterations = LottieConstants.IterateForever)
                                LottieAnimation(comp, prog, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    isSearching -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                    }
                    else -> {
                        val listToShow = if (nameQuery.isBlank()) friends else searchResults
                        if (listToShow.isEmpty()) {
                            val msg = if (nameQuery.isBlank()) "친구가 없습니다 😥" else "검색 결과가 없습니다 🙁"
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text(msg, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                                items(listToShow, key = { it.uid }) { user ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = colors.secondaryContainer,
                                            contentColor   = colors.onSecondaryContainer
                                        ),
                                        elevation = CardDefaults.cardElevation(4.dp),
                                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("profile/${user.uid}") }
                                    ) {
                                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context).data(user.image.ifBlank { null })
                                                    .placeholder(R.drawable.baseline_restaurant_menu_24).build(),
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.primaryContainer)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(user.name, style = MaterialTheme.typography.titleMedium, color = colors.onSecondaryContainer)
                                            }
                                            if (nameQuery.isBlank()) {
                                                TextButton(onClick = { navController.navigate("chat/${user.uid}") }) {
                                                    Text("채팅하기")
                                                }
                                            } else {
                                                TextButton(onClick = {
                                                    scope.launch {
                                                        Firebase.firestore
                                                            .collection("user").document(user.uid)
                                                            .collection("friendRequests")
                                                            .document(currentUid)
                                                            .set(mapOf("timestamp" to System.currentTimeMillis()))
                                                            .await()
                                                        Toast.makeText(context, "친구 요청 보냄", Toast.LENGTH_SHORT).show()
                                                    }
                                                }) {
                                                    Text("요청")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 요청 수락 모달
    if (showRequestsModal) {
        AlertDialog(
            onDismissRequest = { showRequestsModal = false },
            title            = { Text("친구 요청") },
            text             = {
                Column {
                    requests.forEach { user ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(user.name, Modifier.weight(1f))
                            TextButton(onClick = {
                                scope.launch {
                                    val db = Firebase.firestore
                                    // 수락: 양쪽 friends 컬렉션에 추가
                                    db.collection("user").document(currentUid)
                                        .collection("friends").document(user.uid).set(emptyMap<String,Any>()).await()
                                    db.collection("user").document(user.uid)
                                        .collection("friends").document(currentUid).set(emptyMap<String,Any>()).await()
                                    // 요청 삭제
                                    db.collection("user").document(currentUid)
                                        .collection("friendRequests").document(user.uid).delete().await()
                                    friends.add(user)
                                    requests.remove(user)
                                }
                            }) { Text("수락") }
                            TextButton(onClick = {
                                scope.launch {
                                    Firebase.firestore
                                        .collection("user").document(currentUid)
                                        .collection("friendRequests").document(user.uid).delete().await()
                                    requests.remove(user)
                                }
                            }) { Text("거절") }
                        }
                    }
                    if (requests.isEmpty()) {
                        Text("처리할 요청이 없습니다.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton    = {
                TextButton(onClick = { showRequestsModal = false }) {
                    Text("닫기")
                }
            }
        )
    }

    // UID 요청 다이얼로그
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title            = { Text("UID로 친구 요청") },
            text             = {
                OutlinedTextField(
                    value         = addUidText,
                    onValueChange = { addUidText = it },
                    label         = { Text("UID 입력") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            },
            confirmButton    = {
                TextButton(onClick = {
                    scope.launch {
                        isAdding = true
                        try {
                            val snap = Firebase.firestore.collection("user").document(addUidText).get().await()
                            if (snap.exists()) {
                                Firebase.firestore
                                    .collection("user").document(addUidText)
                                    .collection("friendRequests")
                                    .document(currentUid)
                                    .set(mapOf("timestamp" to System.currentTimeMillis()))
                                    .await()
                                Toast.makeText(context, "친구 요청 보냄", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "존재하지 않는 UID", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "요청 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isAdding = false
                            showAddDialog = false
                        }
                    }
                }) {
                    if (isAdding) CircularProgressIndicator(modifier=Modifier.size(20.dp), strokeWidth=2.dp, color=colors.primary)
                    else Text("요청")
                }
            },
            dismissButton    = {
                TextButton(onClick = { showAddDialog = false }) { Text("취소") }
            }
        )
    }
}
