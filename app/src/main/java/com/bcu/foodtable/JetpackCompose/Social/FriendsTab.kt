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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
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
    val colors     = WarmLightColorScheme
    val currentUid = UserManager.getUser()!!.uid
    val scope      = rememberCoroutineScope()

    // 친구 목록 로드 상태
    val friends   = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(true) }

    // 이름 검색 상태
    var nameQuery     by rememberSaveable { mutableStateOf("") }
    val searchResults = remember { mutableStateListOf<User>() }
    var isSearching   by remember { mutableStateOf(false) }

    // UID 추가 다이얼로그 상태
    var showAddDialog by remember { mutableStateOf(false) }
    var addUidText    by rememberSaveable { mutableStateOf("") }
    var isAdding      by remember { mutableStateOf(false) }

    // 초기 친구 목록 불러오기
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val snap = Firebase.firestore
                .collection("user").document(currentUid)
                .collection("friends")
                .get().await()
            friends.clear()
            snap.documents.forEach { doc ->
                val friendUid = doc.id
                val userSnap = Firebase.firestore
                    .collection("user").document(friendUid)
                    .get().await()
                userSnap.toObject(User::class.java)
                    ?.let { friends.add(it.copy(uid = friendUid)) }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "친구 목록 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    .endAt(nameQuery + "\uf8ff")
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

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        // 내 UID 표시 및 공유
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("내 UID: $currentUid", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                // 클립보드 복사
                clipboard.setText(AnnotatedString(currentUid))
                Toast.makeText(context, "내 UID 복사됨", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.Person, contentDescription = "클립보드 복사", tint = colors.primary)
            }
            IconButton(onClick = {
                // SMS 공유
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:")
                    putExtra("sms_body", "내 UID: $currentUid")
                }
                context.startActivity(intent)
            }) {
                Icon(Icons.Default.Share, contentDescription = "SMS 공유", tint = colors.primary)
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
                isLoading -> {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.profile))
                    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LottieAnimation(composition, progress, modifier = Modifier.size(150.dp).clip(CircleShape))
                    }
                }
                isSearching -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                else -> {
                    val displayList = if (nameQuery.isBlank()) friends else searchResults
                    if (displayList.isEmpty()) {
                        val msg = if (nameQuery.isBlank()) "친구가 없습니다 😥" else "검색 결과가 없습니다 🙁"
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(msg, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                            items(displayList, key = { it.uid }) { user ->
                                Card(
                                    shape  = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = colors.secondaryContainer,
                                        contentColor   = colors.onSecondaryContainer
                                    ),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    modifier  = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(user.image.ifBlank { null })
                                                .placeholder(R.drawable.baseline_restaurant_menu_24)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(colors.primaryContainer)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(user.name, style = MaterialTheme.typography.titleMedium, color = colors.onSecondaryContainer)
                                            Text(user.description.ifBlank { "설명이 없습니다." },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSecondaryContainer.copy(alpha = 0.7f),
                                                maxLines = 1)
                                        }
                                        if (nameQuery.isBlank()) {
                                            TextButton(onClick = { navController.navigate("chat/${user.uid}") }) {
                                                Text("채팅하기")
                                            }
                                        } else {
                                            TextButton(onClick = {
                                                // 친구 요청
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
