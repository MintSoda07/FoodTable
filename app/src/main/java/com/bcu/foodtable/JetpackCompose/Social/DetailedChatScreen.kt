package com.bcu.foodtable.JetpackCompose.Social

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// 메시지 데이터 클래스
@kotlinx.serialization.Serializable
data class ChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val amount: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val claimed: Boolean = false,
    val read: Boolean = false
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

    // 송금 다이얼로그
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
                    sendMessage(db, currentUid, targetUid,
                        ChatMessage(senderUid = currentUid, imageUrl = url)
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "이미지 전송 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 실시간 메시지 구독 & 읽음 표시 자동 업데이트
    DisposableEffect(targetUid) {
        val sub = db.collection("user").document(currentUid)
            .collection("chats").document(targetUid)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, _ ->
                snap?.documentChanges?.forEach { dc ->
                    val doc = dc.document
                    val msg = doc.toObject(ChatMessage::class.java).copy(id = doc.id)
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> messages.add(msg)
                        DocumentChange.Type.MODIFIED -> {
                            val idx = messages.indexOfFirst { it.id == msg.id }
                            if (idx != -1) messages[idx] = msg
                        }
                        else -> {}
                    }
                }
                // 수신 메시지 읽음 처리
                snap?.documents
                    ?.filter { it.getString("senderUid") != currentUid && it.getBoolean("read") != true }
                    ?.forEach { doc ->
                        doc.reference.update("read", true)
                        db.collection("user").document(doc.getString("senderUid")!!)
                            .collection("chats").document(currentUid)
                            .collection("messages").document(doc.id)
                            .update("read", true)
                    }
                loading = false
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
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp)
                    .offset(y = (-10).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showTransferDialog = true }) {
                    Icon(Icons.Default.AttachMoney, contentDescription = "송금", tint = MaterialTheme.colorScheme.primary)
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
                    if (txt.isNotEmpty()) {
                        val msg = ChatMessage(senderUid = currentUid, text = txt)
                        scope.launch {
                            sendMessage(db, currentUid, targetUid, msg)
                        }
                        input = TextFieldValue("")
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "전송", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                messages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("아직 대화가 없습니다.", color = Color.Gray)
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(bottom = 60.dp),
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
                            Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isMe) "나" else friendName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                    if (isMe) {
                                        Icon(
                                            imageVector = if (msg.read) Icons.Default.DoneAll else Icons.Default.Done,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp).padding(start = 4.dp),
                                            tint = if (msg.read) Color.Blue else Color.Gray
                                        )
                                    }
                                }
                                msg.text?.let {
                                    Surface(
                                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Text(it, Modifier.padding(8.dp), color = if (isMe) Color.White else MaterialTheme.colorScheme.onSecondaryContainer)
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
                                        color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(Modifier.width(4.dp))
                                            Text("$amt 포인트", color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                    if (!isMe && !msg.claimed) {
                                        TextButton(onClick = { claimPoint(db, scope, context, currentUid, targetUid, msg) }) {
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
            title = { Text("포인트 송금") },
            text = {
                OutlinedTextField(
                    value = transferAmount,
                    onValueChange = { transferAmount = it.filter(Char::isDigit) },
                    label = { Text("금액 입력") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val amt = transferAmount.toIntOrNull() ?: 0
                    if (amt > 0) {
                        showTransferDialog = false
                        scope.launch {
                            sendMessage(db, currentUid, targetUid,
                                ChatMessage(senderUid = currentUid, amount = amt)
                            )
                        }
                    }
                }) { Text("전송") }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) { Text("취소") }
            }
        )
    }
}

// 포인트 수령 처리 함수
fun claimPoint(
    db: FirebaseFirestore,
    scope: CoroutineScope,
    context: android.content.Context,
    currentUid: String,
    targetUid: String,
    msg: ChatMessage
) {
    val senderRef   = db.collection("user").document(msg.senderUid)
    val receiverRef = db.collection("user").document(currentUid)
    val chatRefA    = db.collection("user").document(currentUid)
        .collection("chats").document(targetUid)
        .collection("messages").document(msg.id)
    val chatRefB    = db.collection("user").document(targetUid)
        .collection("chats").document(currentUid)
        .collection("messages").document(msg.id)

    db.runTransaction { tx ->
        val snapshot = tx.get(chatRefA)
        val already = snapshot.getBoolean("claimed") ?: false
        if (!already && (msg.amount ?: 0) > 0) {
            tx.update(chatRefA, "claimed", true)
            tx.update(chatRefB, "claimed", true)
            tx.update(senderRef,   "point", FieldValue.increment(-(msg.amount!!).toLong()))
            tx.update(receiverRef, "point", FieldValue.increment((msg.amount).toLong()))
        }
    }.addOnSuccessListener {
        // 성공 시 UI는 snapshot listener가 처리
    }.addOnFailureListener {
        Toast.makeText(context, "수령 실패: ${it.message}", Toast.LENGTH_SHORT).show()
    }
}

// 메시지 전송 함수
suspend fun sendMessage(
    db: FirebaseFirestore,
    fromUid: String,
    toUid: String,
    message: ChatMessage
) {
    val senderRef   = db.collection("user").document(fromUid)
        .collection("chats").document(toUid)
        .collection("messages")
    val receiverRef = db.collection("user").document(toUid)
        .collection("chats").document(fromUid)
        .collection("messages")

    senderRef.add(message).await()
    receiverRef.add(message).await()
}
