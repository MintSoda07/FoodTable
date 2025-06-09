package com.bcu.foodtable.JetpackCompose.Social

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
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
fun FriendsTab() {
    val context     = LocalContext.current
    val clipboard   = LocalClipboardManager.current
    val colors      = WarmLightColorScheme
    val currentUid  = UserManager.getUser()!!.uid
    val scope       = rememberCoroutineScope()

    // 1) 친구 목록 로드 상태
    val friends   = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(true) }

    // 2) 이름 검색 상태
    var nameQuery     by rememberSaveable { mutableStateOf("") }
    val searchResults = remember { mutableStateListOf<User>() }
    var isSearching   by remember { mutableStateOf(false) }

    // 3) UID 추가 다이얼로그 상태
    var showAddDialog by remember { mutableStateOf(false) }
    var addUidText    by rememberSaveable { mutableStateOf("") }
    var isAdding      by remember { mutableStateOf(false) }

    // 초기 친구 목록 한 번만 불러오기
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val snap = Firebase.firestore
                .collection("users").document(currentUid)
                .collection("friends")
                .get().await()
            friends.clear()
            snap.documents.forEach { doc ->
                val friendUid = doc.id
                val userSnap = Firebase.firestore
                    .collection("users").document(friendUid)
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

    // nameQuery 가 바뀔 때마다 prefix 검색 실행
    LaunchedEffect(nameQuery) {
        if (nameQuery.isBlank()) {
            searchResults.clear()
            isSearching = false
        } else {
            isSearching = true
            searchResults.clear()
            try {
                val snap = Firebase.firestore
                    .collection("users")
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
        // ─── 검색 바 + + 버튼 ──────────────────────
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value         = nameQuery,
                onValueChange = { nameQuery = it },
                label         = { Text("이름으로 검색") },
                modifier      = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector       = Icons.Default.Add,
                    contentDescription = "UID로 친구 추가",
                    tint              = colors.primary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ─── 친구 목록 영역 ────────────────────────
        Box(Modifier.fillMaxSize()) {
            when {
                // A) 초기 로딩
                isLoading -> {
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.profile)
                    )
                    val progress by animateLottieCompositionAsState(
                        composition, iterations = LottieConstants.IterateForever
                    )
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LottieAnimation(
                            composition,
                            progress,
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                // B) 검색 중간
                isSearching -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }

                else -> {
                    val displayList = if (nameQuery.isBlank()) friends else searchResults

                    when {
                        displayList.isEmpty() -> {
                            val msg = if (nameQuery.isBlank())
                                "친구가 없습니다 😥"
                            else
                                "검색 결과가 없습니다 🙁"
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    msg,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.onBackground
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(displayList, key = { it.uid }) { user ->
                                    Card(
                                        shape  = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = colors.secondaryContainer,
                                            contentColor   = colors.onSecondaryContainer
                                        ),
                                        elevation = CardDefaults.cardElevation(4.dp),
                                        modifier  = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                clipboard.setText(AnnotatedString(user.uid))
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "UID 복사됨: ${user.uid}",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
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
                                            Column {
                                                Text(
                                                    user.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = colors.onSecondaryContainer
                                                )
                                                Text(
                                                    user.description.ifBlank { "설명이 없습니다." },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.onSecondaryContainer.copy(alpha = 0.7f),
                                                    maxLines = 1
                                                )
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

    // ─── UID 추가용 모달 다이얼로그 ──────────────────
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title            = { Text("UID로 친구 추가") },
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
                            val snap = Firebase.firestore
                                .collection("users")
                                .document(addUidText)
                                .get().await()
                            if (snap.exists()) {
                                Firebase.firestore
                                    .collection("users")
                                    .document(currentUid)
                                    .collection("friends")
                                    .document(addUidText)
                                    .set(emptyMap<String, Any>())
                                    .await()
                                snap.toObject(User::class.java)
                                    ?.let { friends.add(it.copy(uid = addUidText)) }
                                Toast.makeText(context, "친구 추가됨", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "존재하지 않는 UID", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isAdding = false
                            showAddDialog = false
                        }
                    }
                }) {
                    if (isAdding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                    } else {
                        Text("추가")
                    }
                }
            },
            dismissButton    = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}
