package com.bcu.foodtable.JetpackCompose.Social

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.bcu.foodtable.InAppEvents
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

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
    val placeUrl: String? = null
)

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

@OptIn(ExperimentalMaterial3Api::class)
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
    val listState = rememberLazyListState()

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
                    ChatMessage(senderUid = currentUid, imageUrl = url, timestamp = System.currentTimeMillis())
                )
            }.onFailure {
                Toast.makeText(context, "이미지 전송 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 방 전환 시 메시지 초기화
    LaunchedEffect(targetUid) { messages.clear() }

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
                    DocumentChange.Type.ADDED -> if (messages.none { it.id == msg.id }) messages.add(msg)
                    DocumentChange.Type.MODIFIED -> {
                        val idx = messages.indexOfFirst { it.id == msg.id }
                        if (idx >= 0) messages[idx] = msg
                    }
                    DocumentChange.Type.REMOVED -> { /* 필요 시 삭제 반영 */ }
                }
            }

            // 읽음 처리: 내/상대 문서 동시 업데이트
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

    // 새 메시지 자동 스크롤(하단 근처일 때만)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.lastIndex
            val atBottom = listState.firstVisibleItemIndex >= lastIndex - 2
            if (atBottom) listState.animateScrollToItem(lastIndex)
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
                            sendMessage(
                                db, currentUid, targetUid,
                                ChatMessage(senderUid = currentUid, text = text, timestamp = System.currentTimeMillis())
                            )
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
                        items(messages, key = { it.id }) { msg ->
                            val isMe = msg.senderUid == currentUid
                            when {
                                msg.type == "place" -> {
                                    SharedPlaceMessageBubble(message = msg, isMe = isMe)
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
                }) {
                    Text("카카오맵으로 보기")
                }
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
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    val timeText = timeFormatter.format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (isMe) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
                if (message.read) Text("읽음", fontSize = 10.sp, color = Color.Gray)
                Text(timeText, fontSize = 10.sp, color = Color.Gray)
            }
        }

        Box(modifier = Modifier.widthIn(max = 280.dp)) {
            Surface(color = bubbleColor, shape = shape) {
                if (message.text != null) {
                    Text(message.text, modifier = Modifier.padding(12.dp), color = textColor)
                }
                if (message.imageUrl != null) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Chat Image",
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(shape)
                            .sizeIn(maxHeight = 250.dp, maxWidth = 250.dp)
                            .clickable {
                                Toast.makeText(context, "이미지 상세보기(미구현)", Toast.LENGTH_SHORT).show()
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

        if (!isMe) {
            Text(timeText, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
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

// ==== 백엔드 유틸 ====

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
    val batch = db.batch()
    val senderMsgRef = db.collection("user").document(fromUid)
        .collection("chats").document(toUid)
        .collection("messages").document()
    val receiverMsgRef = db.collection("user").document(toUid)
        .collection("chats").document(fromUid)
        .collection("messages").document(senderMsgRef.id)

    batch.set(senderMsgRef, message)
    batch.set(receiverMsgRef, message)
    batch.commit().await()

    // ✅ Firestore 저장 완료 후 FCM 발송 함수 호출
    // 기존 흐름을 따르면 chatUid는 상대방 목록에서 '대화방 식별자'로 fromUid를 사용 중입니다.
    // 별도의 방 ID가 있다면 그 값을 넣으세요.
    callSendChat(
        toUid  = toUid,
        chatUid = fromUid,              // 방 ID가 따로 있으면 그걸로 교체
        title  = "새 메시지",
        body   = message.text
    )
}
private suspend fun callSendChat(
    toUid: String,
    chatUid: String,
    title: String?,
    body: String?
) {
    val fn = Firebase.functions("asia-northeast3") // ✅ 리전 맞춰주기
    val payload = hashMapOf(
        "toUid" to toUid,
        "chatUid" to chatUid,
        "title" to (title ?: "새 메시지"),
        "body"  to (body ?: "")
    )

    val result = fn.getHttpsCallable("sendChat").call(payload).await()
    // await()의 반환 타입은 HttpsCallableResult → data는 Any? 타입
    val data = result.getData()
    android.util.Log.d("sendChat", "ok: $data")
}
