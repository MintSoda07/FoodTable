package com.bcu.foodtable.JetpackCompose.Social

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
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
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatMessage(
    val senderUid: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val amount: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedChatScreen(
    navController: NavHostController,
    targetUid: String
) {
    val currentUid = UserManager.getUser()!!.uid
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance().reference
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = lightColorScheme(
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
        onErrorContainer = Color(0xFF8B0000)
    )

    // 친구 이름 로드
    var friendName by remember { mutableStateOf(targetUid) }
    LaunchedEffect(targetUid) {
        db.collection("user").document(targetUid).get().await()
            .getString("name")?.let { friendName = it }
    }

    // 입력, 메시지 리스트, 로딩
    var input by remember { mutableStateOf(TextFieldValue("")) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var loading by remember { mutableStateOf(true) }

    // 송금 다이얼로그 상태
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferAmount by rememberSaveable { mutableStateOf("") }

    // 이미지 선택 런처
    val pickImageLauncher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        uri?.let {
            val ref = storage.child("chatImages/$currentUid/$targetUid/${System.currentTimeMillis()}")
            scope.launch {
                try {
                    ref.putFile(it).await()
                    val url = ref.downloadUrl.await().toString()
                    sendMessage(
                        db, currentUid, targetUid,
                        ChatMessage(senderUid = currentUid, imageUrl = url)
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "이미지 전송 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 실시간 메시지 구독
    DisposableEffect(targetUid) {
        val sub = db.collection("user").document(currentUid)
            .collection("chats").document(targetUid)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                snap?.let {
                    messages.clear()
                    it.documents.forEach { doc ->
                        doc.toObject(ChatMessage::class.java)?.let(messages::add)
                    }
                    loading = false
                }
            }
        onDispose { sub.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(friendName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 8.dp)
                    .offset(y = (-10).dp), // 입력창을 위로 10dp
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showTransferDialog = true }) {
                    Icon(Icons.Default.AttachMoney, contentDescription = "송금", tint = colors.primary)
                }
                IconButton(onClick = { pickImageLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Image, contentDescription = "이미지 선택", tint = Color.Gray)
                }
                Spacer(Modifier.width(8.dp))
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("메시지 입력") },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                IconButton(onClick = {
                    val txt = input.text.trim()
                    if (txt.isNotBlank()) {
                        scope.launch {
                            sendMessage(
                                db, currentUid, targetUid,
                                ChatMessage(senderUid = currentUid, text = txt)
                            )
                            input = TextFieldValue("")
                        }
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "전송", tint = colors.primary)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("아직 대화가 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        val isMe = msg.senderUid == currentUid
                        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(msg.timestamp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                Text(
                                    text = if (isMe) "나" else friendName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                msg.text?.let {
                                    Surface(
                                        color = if (isMe) colors.primary else colors.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Text(
                                            it,
                                            Modifier.padding(8.dp),
                                            color = if (isMe) Color.White else colors.onSecondaryContainer
                                        )
                                    }
                                }
                                msg.imageUrl?.let { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                                msg.amount?.let { amt ->
                                    Surface(
                                        color = if (isMe) colors.primaryContainer else colors.tertiaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Row(
                                            Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.AttachMoney, null, tint = colors.onPrimary)
                                            Spacer(Modifier.width(4.dp))
                                            Text("$amt 포인트", color = colors.onPrimary)
                                        }
                                    }
                                    if (!isMe) {
                                        TextButton(onClick = {
                                            scope.launch {
                                                db.collection("user").document(msg.senderUid)
                                                    .update("point", FieldValue.increment(-amt.toLong())).await()
                                                db.collection("user").document(currentUid)
                                                    .update("point", FieldValue.increment(amt.toLong())).await()
                                                sendMessage(
                                                    db, currentUid, targetUid,
                                                    ChatMessage(
                                                        senderUid = currentUid,
                                                        text = "✔️ $amt 포인트 수령",
                                                        timestamp = System.currentTimeMillis()
                                                    )
                                                )
                                            }
                                        }) {
                                            Text("수령")
                                        }
                                    }
                                }
                                Text(
                                    timeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 송금 다이얼로그
    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title            = { Text("포인트 송금") },
            text             = {
                OutlinedTextField(
                    value = transferAmount,
                    onValueChange = { transferAmount = it.filter(Char::isDigit) },
                    label = { Text("금액 입력") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton    = {
                TextButton(onClick = {
                    val amt = transferAmount.toIntOrNull() ?: 0
                    if (amt > 0) {
                        showTransferDialog = false
                        scope.launch {
                            sendMessage(
                                db, currentUid, targetUid,
                                ChatMessage(senderUid = currentUid, amount = amt)
                            )
                        }
                    }
                }) {
                    Text("전송")
                }
            },
            dismissButton    = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

private suspend fun sendMessage(
    db: FirebaseFirestore,
    fromUid: String,
    toUid: String,
    message: ChatMessage
) {
    val senderRef = db.collection("user").document(fromUid)
        .collection("chats").document(toUid)
        .collection("messages")
    val receiverRef = db.collection("user").document(toUid)
        .collection("chats").document(fromUid)
        .collection("messages")

    senderRef.add(message).await()
    receiverRef.add(message).await()
}
