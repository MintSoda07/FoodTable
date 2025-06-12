package com.bcu.foodtable.JetpackCompose.Social

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── 테마 정의 ────────────────────────────────────────────────────────────
private val FriendsColorScheme = lightColorScheme(
    primary = Color(0xFFE25532),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2D6),
    secondary = Color(0xFF4CAF50),
    background = Color(0xFFFFFBF8),
    surface = Color.White,
    surfaceVariant = Color(0xFFFBE7DF),
    outline = Color(0xFFDDC7BD),
    onSurfaceVariant = Color(0xFF4E342E)
)

@Composable
private fun FriendsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FriendsColorScheme,
        shapes = Shapes(medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp)),
        typography = Typography(),
        content = content
    )
}
// ────────────────────────────────────────────────────────────────────────────


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsTab(
    navController: NavHostController
) {
    val context = LocalContext.current
    val currentUid = UserManager.getUser()!!.uid
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    // State
    val friends = remember { mutableStateListOf<User>() }
    val requests = remember { mutableStateListOf<User>() }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) } // 0: 친구, 1: 검색, 2: 요청
    var showAddDialog by remember { mutableStateOf(false) }

    // Real-time listeners
    DisposableEffect(currentUid) {
        val listeners = mutableListOf<ListenerRegistration>()

        // 친구 목록 리스너
        listeners.add(
            db.collection("user").document(currentUid).collection("friends")
                .addSnapshotListener { snap, e ->
                    if (e != null) return@addSnapshotListener
                    scope.launch {
                        val friendUids = snap?.documents?.map { it.id } ?: emptyList()
                        val friendUsers = friendUids.mapNotNull { uid ->
                            try {
                                db.collection("user").document(uid).get().await().toObject(User::class.java)?.copy(uid = uid)
                            } catch (e: Exception) { null }
                        }
                        friends.clear()
                        friends.addAll(friendUsers)
                        isLoading = false
                    }
                }
        )

        // 친구 요청 리스너
        listeners.add(
            db.collection("user").document(currentUid).collection("friendRequests")
                .addSnapshotListener { snap, e ->
                    if (e != null) return@addSnapshotListener
                    scope.launch {
                        val requestUids = snap?.documents?.map { it.id } ?: emptyList()
                        val requestUsers = requestUids.mapNotNull { uid ->
                            try {
                                db.collection("user").document(uid).get().await().toObject(User::class.java)?.copy(uid = uid)
                            } catch (e: Exception) { null }
                        }
                        requests.clear()
                        requests.addAll(requestUsers)
                    }
                }
        )
        onDispose { listeners.forEach { it.remove() } }
    }

    FriendsTheme {
        Scaffold(
            topBar = {
                HomeTopAppBar(
                    requestCount = requests.size,
                    onMyUidClick = { showAddDialog = true },
                    onTabSelected = { selectedTab = it }
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.padding(padding),
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState > initialState) width else -width } togetherWith
                            slideOutHorizontally { width -> if (targetState > initialState) -width else width }
                }, label = ""
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> FriendListScreen(navController, friends, isLoading)
                    1 -> FriendSearchScreen(navController)
                    2 -> FriendRequestScreen(requests)
                }
            }

            if (showAddDialog) {
                AddFriendByUidDialog(
                    myUid = currentUid,
                    onDismiss = { showAddDialog = false },
                    onConfirm = { uid ->
                        scope.launch {
                            // 로직은 Dialog 내부에서 처리
                        }
                    }
                )
            }
        }
    }
}


// ─── 화면별 Composable ───────────────────────────────────────────────────

@Composable
fun FriendListScreen(navController: NavHostController, friends: List<User>, isLoading: Boolean) {
    if (isLoading) {
        LoadingState()
    } else if (friends.isEmpty()) {
        EmptyState(message = "아직 친구가 없어요.\n친구를 추가하고 소통해보세요!", icon = Icons.Default.SentimentVeryDissatisfied)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(friends, key = { it.uid }) { user ->
                FriendCard(user = user, onChatClick = { navController.navigate("chat/${user.uid}") })
            }
        }
    }
}

@Composable
fun FriendSearchScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val currentUid = UserManager.getUser()!!.uid

    var nameQuery by rememberSaveable { mutableStateOf("") }
    val searchResults = remember { mutableStateListOf<User>() }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(nameQuery) {
        if (nameQuery.length >= 2) {
            isSearching = true
            try {
                val result = db.collection("user").orderBy("name")
                    .startAt(nameQuery).endAt("$nameQuery\uf8ff").get().await()
                searchResults.clear()
                result.documents.mapNotNullTo(searchResults) { doc ->
                    if (doc.id != currentUid) doc.toObject(User::class.java)?.copy(uid = doc.id) else null
                }
            } finally { isSearching = false }
        } else {
            searchResults.clear()
        }
    }

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = nameQuery,
            onValueChange = { nameQuery = it },
            label = { Text("2글자 이상으로 친구 이름 검색") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        if (isSearching) {
            SearchingState()
        } else if (nameQuery.length >= 2 && searchResults.isEmpty()) {
            EmptyState(message = "검색 결과가 없습니다.", icon = Icons.Default.SearchOff)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(searchResults, key = { it.uid }) { user ->
                    FriendCard(user = user, isFriend = false, onRequestClick = {
                        scope.launch {
                            db.collection("user").document(user.uid).collection("friendRequests")
                                .document(currentUid).set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
                            Toast.makeText(context, "${user.name}님에게 친구 요청을 보냈습니다.", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun FriendRequestScreen(requests: List<User>) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val currentUid = UserManager.getUser()!!.uid

    if (requests.isEmpty()) {
        EmptyState(message = "받은 친구 요청이 없습니다.", icon = Icons.Default.NotificationsOff)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests, key = { it.uid }) { user ->
                RequestCard(
                    user = user,
                    onAccept = {
                        scope.launch {
                            val batch = db.batch()
                            batch.set(db.collection("user").document(currentUid).collection("friends").document(user.uid), emptyMap<String, Any>())
                            batch.set(db.collection("user").document(user.uid).collection("friends").document(currentUid), emptyMap<String, Any>())
                            batch.delete(db.collection("user").document(currentUid).collection("friendRequests").document(user.uid))
                            batch.commit().await()
                            Toast.makeText(context, "${user.name}님과 친구가 되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDecline = {
                        scope.launch {
                            db.collection("user").document(currentUid).collection("friendRequests").document(user.uid).delete().await()
                            Toast.makeText(context, "${user.name}님의 요청을 거절했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}


// ─── UI 컴포넌트 ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(requestCount: Int, onMyUidClick: () -> Unit, onTabSelected: (Int) -> Unit) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val titles = listOf("친구", "검색", "받은 요청")

    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        CenterAlignedTopAppBar(
            title = { Text("친구", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = onMyUidClick) {
                    Icon(Icons.Default.Add, contentDescription = "UID로 친구 추가")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
        )
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        selectedTabIndex = index
                        onTabSelected(index)
                    },
                    text = {
                        if (index == 2 && requestCount > 0) {
                            BadgedBox(badge = { Badge { Text("$requestCount") } }) {
                                Text(title)
                            }
                        } else {
                            Text(title)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FriendCard(user: User, isFriend: Boolean = true, onChatClick: () -> Unit = {}, onRequestClick: () -> Unit = {}) {
    val context = LocalContext.current
    val cardBrush = Brush.horizontalGradient(
        colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    )
    ElevatedCard(shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(cardBrush).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(user.image.ifBlank { R.drawable.ic_profile_placeholder }).crossfade(true).build(),
                contentDescription = "${user.name}의 프로필 사진",
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
            )
            Spacer(Modifier.width(16.dp))
            Text(user.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (isFriend) {
                IconButton(onClick = onChatClick, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Chat, contentDescription = "채팅하기", tint = Color.White)
                }
            } else {
                Button(onClick = onRequestClick) { Text("요청") }
            }
        }
    }
}

@Composable
fun RequestCard(user: User, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(user.image.ifBlank { R.drawable.ic_profile_placeholder }).build(),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(user.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Row {
                TextButton(onClick = onAccept) { Text("수락") }
                TextButton(onClick = onDecline, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("거절") }
            }
        }
    }
}


@Composable
fun AddFriendByUidDialog(myUid: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var addUidText by rememberSaveable { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PersonAdd, null) },
        title = { Text("친구 추가") },
        text = {
            Column {
                Text("내 UID: $myUid", style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(myUid))
                    Toast.makeText(context, "내 UID가 복사되었습니다.", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Default.ContentCopy, "내 UID 복사") }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = addUidText,
                    onValueChange = { addUidText = it },
                    label = { Text("친구의 UID를 입력하세요") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isAdding = true
                        try {
                            if (addUidText == myUid) {
                                Toast.makeText(context, "자기 자신에게는 요청할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            if (db.collection("user").document(addUidText).get().await().exists()) {
                                db.collection("user").document(addUidText).collection("friendRequests")
                                    .document(myUid).set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
                                Toast.makeText(context, "친구 요청을 보냈습니다.", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, "존재하지 않는 UID 입니다.", Toast.LENGTH_SHORT).show()
                            }
                        } finally { isAdding = false }
                    }
                },
                enabled = addUidText.isNotBlank() && !isAdding
            ) {
                if (isAdding) CircularProgressIndicator(Modifier.size(ButtonDefaults.IconSize), color = Color.White, strokeWidth = 2.dp)
                else Text("요청 보내기")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
fun LoadingState() {
    Box(Modifier.fillMaxSize().padding(64.dp), contentAlignment = Alignment.Center) {
        val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.profile))
        LottieAnimation(comp, modifier = Modifier.size(150.dp), iterations = LottieConstants.IterateForever)
        Text("친구 목록을 불러오는 중...", modifier = Modifier.align(Alignment.BottomCenter), color = Color.Gray)
    }
}

@Composable
fun SearchingState() {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(message: String, icon: ImageVector) {
    Column(
        modifier = Modifier.fillMaxSize().padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text(message, color = Color.Gray, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}