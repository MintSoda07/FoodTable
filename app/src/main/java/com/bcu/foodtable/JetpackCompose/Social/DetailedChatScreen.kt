package com.bcu.foodtable.JetpackCompose.Social

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.bcu.foodtable.AppState
import com.bcu.foodtable.R
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import com.bcu.foodtable.JetpackCompose.Social.Appointment.AppointmentInviteBubble
import com.bcu.foodtable.JetpackCompose.Social.Openchat.RecipeShareBubble
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.functions.ktx.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.withFrameNanos

@kotlinx.serialization.Serializable
data class ChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val amount: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val claimed: Boolean = false,
    val read: Boolean = false,
    val type: String? = null,
    val placeName: String? = null,
    val category: String? = null,
    val placeUrl: String? = null,
    val openchatRoomId: String? = null,
    val openchatTitle: String? = null,
    val deeplink: String? = null,
    val appointmentId: String? = null
)

/* =========================
   Chat theme (그대로 유지)
   ========================= */
private val ChatColorScheme = lightColorScheme(
    primary = Color(0xFFF57C00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    secondary = Color(0xFF4CAF50),
    background = Color(0xFFFFF3E0),
    surface = Color.White,
    onSurface = Color(0xFF4E4539),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF37474F),
)

@Composable
fun ChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChatColorScheme,
        shapes = Shapes(
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp)
        ),
        typography = Typography(),
        content = content
    )
}

/* =========================
   공용 스크롤 헬퍼 (이 파일에 포함)
   ========================= */
// 레이아웃이 실제로 끝난 뒤 안전하게 특정 인덱스로 이동
suspend fun LazyListState.scrollToIndexAfterComposition(index: Int, offset: Int = 0) {
    if (index < 0) return
    var guard = 0
    while (layoutInfo.totalItemsCount == 0 && guard < 10) {
        withFrameNanos { } // 다음 프레임까지 대기
        guard++
    }
    withFrameNanos { }     // 한 프레임 여유
    scrollToItem(index, offset)
}

// 바닥으로 애니메이션 스크롤
suspend fun LazyListState.animateToBottomIfPossible(total: Int) {
    if (total > 0) animateScrollToItem(total - 1)
}

// 내가 바닥에 있는지 (카톡식 auto-follow)
val LazyListState.isAtBottom: Boolean
    get() {
        val info = layoutInfo
        if (info.totalItemsCount == 0) return true
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
        return lastVisible >= info.totalItemsCount - 1
    }

/* =========================
   DM 상세 화면 (카톡식 적용)
   ========================= */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DetailedChatScreen(
    navController: NavHostController,
    targetUid: String,
    homeViewModel: HomeViewModel
) {
    val currentUid = UserManager.getUser()!!.uid
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance().reference
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 현재 방 트래킹 (FCM 억제)
    DisposableEffect(targetUid) {
        AppState.setCurrentChat(targetUid)
        onDispose { AppState.setCurrentChat(null) }
    }

    var friendName by remember { mutableStateOf("친구") }
    var friendProfileUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(targetUid) {
        db.collection("user").document(targetUid).get().await().let { doc ->
            friendName = doc.getString("name") ?: "친구"
            friendProfileUrl = doc.getString("profileImageUrl")
        }
    }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    var loading by remember { mutableStateOf(true) }

    // ✅ 방 ID로 키잉된 리스트 상태 (이전 복원 무력화)
    val listState = rememberSaveable(targetUid, saver = LazyListState.Saver) {
        LazyListState(0, 0)
    }

    // ✅ 첫 미읽음/앵커/미읽음 수는 derivedStateOf로 반응형 계산
    val firstUnreadIndex by remember(messages) {
        derivedStateOf { messages.indexOfFirst { it.senderUid == targetUid && it.read != true } }
    }
    val anchorIndex by remember(messages, firstUnreadIndex) {
        derivedStateOf { if (firstUnreadIndex <= 0) messages.lastIndex else (firstUnreadIndex - 1) }
    }
    val unreadCount by remember(messages) {
        derivedStateOf { messages.count { it.senderUid == targetUid && it.read != true } }
    }

    var showTransferDialog by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val filename = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        val ref = storage.child("chatImages/$currentUid/$filename")
        scope.launch {
            Toast.makeText(context, "이미지 업로드 중...", Toast.LENGTH_SHORT).show()
            runCatching {
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                sendMessage(
                    db, currentUid, targetUid,
                    ChatMessage(
                        senderUid = currentUid,
                        imageUrl = url,
                        timestamp = System.currentTimeMillis(),
                        type = "image"
                    )
                )
            }.onFailure {
                Toast.makeText(context, "이미지 전송 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 방 전환 시 메시지 초기화
    LaunchedEffect(targetUid) { messages.clear() }

    // 스냅샷 리스너 (메시지 수신/갱신)
    DisposableEffect(targetUid) {
        val query = db.collection("user").document(currentUid)
            .collection("chats").document(targetUid)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = query.addSnapshotListener { snap, e ->
            if (e != null) { loading = false; return@addSnapshotListener }
            if (snap == null) { loading = false; return@addSnapshotListener }

            for (dc in snap.documentChanges) {
                val doc = dc.document
                val msg = doc.toObject(ChatMessage::class.java).copy(id = doc.id)
                when (dc.type) {
                    DocumentChange.Type.ADDED ->
                        if (messages.none { it.id == msg.id }) messages.add(msg)
                    DocumentChange.Type.MODIFIED -> {
                        val idx = messages.indexOfFirst { it.id == msg.id }
                        if (idx >= 0) messages[idx] = msg
                    }
                    DocumentChange.Type.REMOVED -> { /* 필요 시 삭제 반영 */ }
                }
            }

            // 읽음 처리: 내/상대 문서 동시 업데이트 (상대가 보낸 미읽음만)
            val unreadDocs = snap.documents.filter {
                it.getString("senderUid") == targetUid && it.getBoolean("read") != true
            }
            if (unreadDocs.isNotEmpty()) {
                val batch = db.batch()
                unreadDocs.forEach { myDoc ->
                    batch.update(myDoc.reference, "read", true)
                    val friendRef = db.collection("user").document(targetUid)
                        .collection("chats").document(currentUid)
                        .collection("messages").document(myDoc.id)
                    batch.update(friendRef, "read", true)
                }
                batch.commit()
            }

            loading = false
        }
        onDispose { listener.remove() }
    }

    /* ===== 카톡식 스크롤 규칙 =====
       1) 진입/로딩 후: 첫 미읽음 위(앵커)로 이동 (레이아웃 후 보장)
       2) 새 메시지 도착: 내가 바닥에 있거나 내가 보낸 메시지면 따라감
    */
    // (1) 앵커로 즉시 이동 (레이아웃 보장 헬퍼 사용)
    LaunchedEffect(targetUid, loading, messages.size) {
        if (!loading && messages.isNotEmpty()) {
            listState.scrollToIndexAfterComposition(anchorIndex)
        }
    }
    // (2) 새 메시지: 바닥일 때만 or 내가 보냈을 때만 따라감
    LaunchedEffect(messages.size) {
        val last = messages.lastOrNull()
        val iSentLast = (last?.senderUid == currentUid)
        if (iSentLast || listState.isAtBottom) {
            listState.animateToBottomIfPossible(messages.size)
        }
    }

    ChatTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = friendProfileUrl,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.LightGray),
                                placeholder = painterResource(id = R.drawable.ic_profile_placeholder),
                                error = painterResource(id = R.drawable.ic_profile_placeholder)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(friendName, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            },
            bottomBar = {
                ChatInputBar(
                    onSendMessage = { text ->
                        scope.launch {
                            if (text.isNotBlank()) {
                                sendMessage(
                                    db, currentUid, targetUid,
                                    ChatMessage(
                                        senderUid = currentUid,
                                        text = text,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    },
                    onSendImage = { pickImageLauncher.launch("image/*") },
                    onSendMoney = { showTransferDialog = true }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(messages, key = { _, m -> m.id }) { index, msg ->
                            // 첫 미읽음 위치에 디바이더 삽입
                            if (index == firstUnreadIndex && firstUnreadIndex >= 0) {
                                val unreadTail = messages.drop(index)
                                    .count { it.senderUid == targetUid && it.read != true }
                                UnreadDivider(unreadCount = unreadTail)
                                Spacer(Modifier.height(8.dp))
                            }

                            val isMe = msg.senderUid == currentUid
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(220, delayMillis = 20)) +
                                        slideInVertically(initialOffsetY = { it / 3 }, animationSpec = tween(220)),
                                exit = fadeOut() + shrinkVertically(),
                                modifier = Modifier.animateItemPlacement()
                            ) {
                                when {
                                    msg.type == "appointment" -> {
                                        AppointmentInviteBubble(
                                            message = msg,
                                            onOpen = { apptId -> navController.navigate("appointment/$apptId") }
                                        )
                                    }
                                    msg.type == "openchat_invite" -> {
                                        OpenChatInviteBubble(
                                            message = msg,
                                            onJoin = { roomId ->
                                                scope.launch {
                                                    val roomDoc = db.collection("openRooms").document(roomId).get().await()
                                                    if (!roomDoc.exists()) {
                                                        Toast.makeText(context, "존재하지 않는 방입니다.", Toast.LENGTH_SHORT).show()
                                                        return@launch
                                                    }
                                                    navController.navigate("openchat/$roomId")
                                                }
                                            }
                                        )
                                    }
                                    msg.type == "place" -> {
                                        SharedPlaceMessageBubble(message = msg, isMe = isMe)
                                    }
                                    msg.type == "recipe" -> {
                                        RecipeShareBubble(
                                            message = msg,
                                            onOpen = { rid ->
                                                navController.navigate("recipe_by_id/${Uri.encode(rid)}")
                                            }
                                        )
                                    }
                                    else -> {
                                        ChatMessageBubble(
                                            message = msg,
                                            isMe = isMe,
                                            onClaim = {
                                                claimPoint(db, scope, context, currentUid, targetUid, msg)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 하단 점프 FAB: 바닥이 아니고 미읽음 있을 때만
                AnimatedVisibility(
                    visible = !listState.isAtBottom && unreadCount > 0,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { scope.launch { listState.animateToBottomIfPossible(messages.size) } },
                        icon = { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) },
                        text = { Text("새 메시지 $unreadCount") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    if (showTransferDialog) {
        MoneyTransferDialog(
            onDismiss = { showTransferDialog = false },
            onConfirm = { amount ->
                scope.launch {
                    sendMessage(
                        db, currentUid, targetUid,
                        ChatMessage(senderUid = currentUid, amount = amount, timestamp = System.currentTimeMillis())
                    )
                }
                showTransferDialog = false
            }
        )
    }
}

/* =========================
   보조 컴포넌트들 (기존 유지)
   ========================= */

@Composable
fun UnreadDivider(unreadCount: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(
                "안 읽은 메시지 ${unreadCount}개",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SharedPlaceMessageBubble(
    message: ChatMessage,
    isMe: Boolean
) {
    val context = LocalContext.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    val timeText = timeFormatter.format(Date(message.timestamp))

    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isMe) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
                AnimatedContent(
                    targetState = message.read,
                    transitionSpec = {
                        (fadeIn(tween(150)) + slideInVertically { it / 2 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                    },
                    label = "readReceiptPlaceNoBg"
                ) { read ->
                    if (read) {
                        Text("읽음", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Text("1", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(timeText, fontSize = 10.sp, color = Color.Gray)
            }
        }

        Surface(color = bubbleColor, shape = shape) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .widthIn(max = 280.dp)
            ) {
                Text("📍 ${message.placeName}", fontWeight = FontWeight.Bold, color = textColor)
                Text("🗂 ${message.category}", color = textColor)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    message.placeUrl?.let { url ->
                        runCatching {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }.onFailure {
                            Toast.makeText(context, "지도를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } ?: Toast.makeText(context, "주소가 없습니다.", Toast.LENGTH_SHORT).show()
                }) { Text("카카오맵으로 보기") }
            }
        }

        if (!isMe) {
            Text(timeText, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    onSendImage: () -> Unit,
    onSendMoney: () -> Unit
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var showExtraOptions by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Column {
            AnimatedVisibility(visible = showExtraOptions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ExtraOptionButton(icon = Icons.Default.AttachMoney, text = "송금", onClick = onSendMoney)
                    ExtraOptionButton(icon = Icons.Default.Image, text = "이미지", onClick = onSendImage)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showExtraOptions = !showExtraOptions }) {
                    Icon(
                        imageVector = if (showExtraOptions) Icons.Default.Close else Icons.Default.AddCircle,
                        contentDescription = "추가 옵션",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("메시지를 입력하세요") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(Modifier.width(8.dp))
                val isSendEnabled = input.text.isNotBlank()
                IconButton(
                    onClick = {
                        onSendMessage(input.text)
                        input = TextFieldValue("")
                    },
                    enabled = isSendEnabled,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSendEnabled) MaterialTheme.colorScheme.primary else Color.LightGray)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "전송", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ExtraOptionButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = MaterialTheme.colorScheme.primary)
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    onClaim: () -> Unit
) {
    val context = LocalContext.current
    val bubbleColor =
        if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor =
        if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.KOREA) }
    val timeText = timeFormatter.format(java.util.Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isMe) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
                AnimatedContent(
                    targetState = message.read, // true -> "읽음", false -> 숫자 "1"
                    transitionSpec = {
                        (fadeIn(tween(150)) + slideInVertically { it / 2 }) togetherWith
                                (fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                    },
                    label = "readReceiptDMNoBg",
                    modifier = Modifier.offset(y = 6.dp)
                ) { read ->
                    if (read) {
                        Text("읽음", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        Text(
                            "1", // DM은 미읽음 1표시
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(timeText, fontSize = 10.sp, color = Color.Gray)
            }
        }

        Box(modifier = Modifier.widthIn(max = 280.dp)) {
            Surface(color = bubbleColor, shape = shape) {
                Column {
                    if (message.text != null) {
                        Text(message.text, modifier = Modifier.padding(12.dp), color = textColor)
                    }
                    if (message.imageUrl != null) {
                        val scale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "imagePop"
                        )
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Chat Image",
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(shape)
                                .sizeIn(maxHeight = 250.dp, maxWidth = 250.dp)
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .clickable {
                                    Toast.makeText(
                                        context,
                                        "이미지 상세보기(미구현)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        )
                    }
                    if (message.amount != null) {
                        MoneyTransferContent(
                            amount = message.amount,
                            isMe = isMe,
                            isClaimed = message.claimed,
                            onClaim = onClaim
                        )
                    }
                }
            }
        }

        if (!isMe) {
            Text(timeText, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
fun OpenChatInviteBubble(
    message: ChatMessage,
    onJoin: (roomId: String) -> Unit
) {
    val title = message.openchatTitle ?: "오픈채팅"
    val roomId = message.openchatRoomId
    val isEnabled = !roomId.isNullOrBlank()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        ElevatedCard {
            Column(Modifier.padding(16.dp).widthIn(max = 320.dp)) {
                Text("오픈채팅 초대", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(message.text ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { roomId?.let(onJoin) },
                        enabled = isEnabled
                    ) { Text("참여하기") }
                }
            }
        }
    }
}

@Composable
fun MoneyTransferContent(
    amount: Int,
    isMe: Boolean,
    isClaimed: Boolean,
    onClaim: () -> Unit
) {
    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CardGiftcard, contentDescription = "Points", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(8.dp))
        Text("${amount}P", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(if (isMe) "포인트를 보냈습니다" else "포인트를 받았습니다")
        Spacer(Modifier.height(8.dp))
        if (!isMe && !isClaimed) {
            Button(onClick = onClaim) { Text("수령하기") }
        } else if (isClaimed) {
            Text("수령 완료", color = Color.Gray)
        }
    }
}

@Composable
fun MoneyTransferDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amount by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("포인트 송금") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter(Char::isDigit) },
                label = { Text("보낼 금액") },
                singleLine = true,
                prefix = { Text("P") }
            )
        },
        confirmButton = {
            Button(onClick = {
                val amountInt = amount.toIntOrNull()
                if (amountInt != null && amountInt > 0) onConfirm(amountInt)
            }) { Text("보내기") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

/* ========= 백엔드 유틸 ========= */

fun claimPoint(
    db: FirebaseFirestore,
    scope: CoroutineScope,
    context: android.content.Context,
    currentUid: String,
    targetUid: String,
    msg: ChatMessage
) {
    val senderRef = db.collection("user").document(msg.senderUid)
    val receiverRef = db.collection("user").document(currentUid)
    val myChatMsgRef = db.collection("user").document(currentUid)
        .collection("chats").document(targetUid)
        .collection("messages").document(msg.id)
    val friendChatMsgRef = db.collection("user").document(targetUid)
        .collection("chats").document(currentUid)
        .collection("messages").document(msg.id)

    scope.launch {
        try {
            db.runTransaction { transaction ->
                val senderDoc = transaction.get(senderRef)
                val receiverDoc = transaction.get(receiverRef)
                val chatDoc = transaction.get(myChatMsgRef)

                val senderPoints = senderDoc.getLong("point") ?: 0
                val amount = msg.amount ?: 0

                if (chatDoc.getBoolean("claimed") == true) {
                    throw IllegalStateException("이미 수령한 포인트입니다.")
                }
                if (senderPoints < amount) {
                    throw IllegalStateException("보내는 사람의 포인트가 부족합니다.")
                }

                transaction.update(senderRef, "point", senderPoints - amount)
                transaction.update(receiverRef, "point", FieldValue.increment(amount.toLong()))
                transaction.update(myChatMsgRef, "claimed", true)
                transaction.update(friendChatMsgRef, "claimed", true)
                null
            }.await()
            Toast.makeText(context, "${msg.amount}P 수령 완료!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "수령 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

suspend fun sendMessage(
    db: FirebaseFirestore,
    fromUid: String,
    toUid: String,
    message: ChatMessage
) {
    val now = if (message.timestamp > 0) message.timestamp else System.currentTimeMillis()

    val senderMsgRef = db.collection("user").document(fromUid)
        .collection("chats").document(toUid)
        .collection("messages").document()
    val receiverMsgRef = db.collection("user").document(toUid)
        .collection("chats").document(fromUid)
        .collection("messages").document(senderMsgRef.id)

    val msgId = senderMsgRef.id

    val preview = when (message.type) {
        "appointment" -> "[약속] ${message.text.orEmpty()}"
        "recipe"      -> "[레시피] ${message.text.orEmpty()}"
        "image"       -> "사진"
        else          -> message.text?.take(50).orEmpty()
    }

    val senderPayload   = message.copy(id = msgId, senderUid = fromUid, timestamp = now, read = false)
    val receiverPayload = message.copy(id = msgId, senderUid = fromUid, timestamp = now, read = false)

    val senderChatMeta = mapOf("lastAt" to now, "lastMessage" to preview)
    val receiverChatMeta = mapOf("lastAt" to now, "lastMessage" to preview)

    db.runBatch { b ->
        b.set(senderMsgRef, senderPayload)
        b.set(receiverMsgRef, receiverPayload)
        b.set(senderMsgRef.parent.parent!!, senderChatMeta, SetOptions.merge())
        b.set(receiverMsgRef.parent.parent!!, receiverChatMeta, SetOptions.merge())
    }.await()

    callSendChat(
        toUid  = toUid,
        chatUid = fromUid,
        title  = "새 메시지",
        body   = preview
    )
}
private suspend fun callSendChat(
    toUid: String,
    chatUid: String,
    title: String?,
    body: String?
) {
    val fn = Firebase.functions("asia-northeast3")
    val payload = hashMapOf(
        "toUid" to toUid,
        "chatUid" to chatUid,
        "title" to (title ?: "새 메시지"),
        "body"  to (body ?: "")
    )
    val result = fn.getHttpsCallable("sendChat").call(payload).await()
    val data = result.getData()
    android.util.Log.d("sendChat", "ok: $data")
}
