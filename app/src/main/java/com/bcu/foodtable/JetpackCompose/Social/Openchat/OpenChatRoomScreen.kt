// OpenChatRoomScreen.kt
package com.bcu.foodtable.JetpackCompose.Social.Openchat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.Social.ChatTheme // ✅ DM과 동일 테마 적용
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenChatRoomScreen(
    navController: NavHostController,
    roomId: String,
    vm: OpenChatViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val me = com.bcu.foodtable.useful.UserManager.getUser() ?: return
    val myUid = me.uid
    val myGlobalNick = me.name.ifBlank { "사용자" }

    var room by remember { mutableStateOf<OpenChatRoom?>(null) }
    var joined by remember { mutableStateOf(false) }
    var myNick by remember { mutableStateOf("") }
    var isOwner by remember { mutableStateOf(false) }

    var showMenu by remember { mutableStateOf(false) }
    var showMembers by remember { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var passcodeInput by remember { mutableStateOf("") }

    var liveMemberCount by remember { mutableStateOf(0L) }
    var liveMembers by remember { mutableStateOf(listOf<OpenChatMember>()) }
    var showTransfer by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val storage = remember { FirebaseStorage.getInstance().reference }
    // 방 정보 + 내 멤버 여부
    LaunchedEffect(roomId) {
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("openRooms").document(roomId).get().await()
        room = doc.toObject(OpenChatRoom::class.java)?.copy(id = doc.id) ?: return@LaunchedEffect
        isOwner = (room!!.ownerUid == myUid)

        val memDoc = db.collection("openRooms").document(roomId)
            .collection("members").document(myUid).get().await()
        joined = memDoc.exists()
        myNick = (memDoc.getString("nickname") ?: myGlobalNick).ifBlank { myGlobalNick }

        // 방장인데 아직 멤버가 아니면 자동 참가
        if (isOwner && !joined) {
            try {
                vm.joinRoom(roomId, myUid, myGlobalNick)
                vm.sendSystem(roomId, "join", myGlobalNick)
                joined = true
            } catch (_: Exception) {}
        }
        if (joined) vm.listenMessages(roomId)
    }
    DisposableEffect(roomId) {
        val db = FirebaseFirestore.getInstance()
        val reg = db.collection("openRooms").document(roomId)
            .collection("members")
            .addSnapshotListener { snap, _ ->
                liveMemberCount = (snap?.size() ?: 0).toLong()
                liveMembers = snap?.documents?.mapNotNull { it.toObject(OpenChatMember::class.java) }.orEmpty()
            }
        onDispose { reg.remove() }
    }

    val messages by vm.messages.collectAsState()

    // ===== 자동 스크롤 고도화 =====
    val listState = rememberLazyListState()
    var hasInitialScroll by remember { mutableStateOf(false) }
    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val lastIndex = messages.lastIndex
            if (lastIndex < 0) true else lastVisible >= lastIndex - 2
        }
    }
    LaunchedEffect(messages.size, joined) {
        if (!joined || messages.isEmpty()) return@LaunchedEffect
        if (!hasInitialScroll) {
            listState.scrollToItem(messages.lastIndex)
            hasInitialScroll = true
        } else if (isNearBottom) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // 이미지 선택 → 업로드 후 전송
    val pickImageLauncher = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                Toast.makeText(ctx, "이미지 업로드 중...", Toast.LENGTH_SHORT).show()


                val filename = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                val ref = storage.child("openchatImages/$roomId/$myUid/$filename")

                // 업로드
                ref.putFile(uri).await()

                val url = ref.downloadUrl.await().toString()

                // 메시지에 downloadUrl 저장
                vm.sendImage(roomId, myUid, url)
            } catch (e: Exception) {
                Toast.makeText(ctx, "이미지 전송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 미가입 & 방장 아님 → 입장 다이얼로그
    if (room != null && !joined && !isOwner) {
        JoinRoomSheet(
            room = room!!,
            onJoin = { nickname, passcode ->
                scope.launch {
                    try {
                        if (room!!.passcode?.isNotBlank() == true && room!!.passcode != passcode) {
                            Toast.makeText(ctx, "비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        vm.joinRoom(roomId, myUid, nickname)
                        vm.sendSystem(roomId, "join", nickname)
                        myNick = nickname
                        joined = true
                        vm.listenMessages(roomId)
                    } catch (e: Exception) {
                        Toast.makeText(ctx, e.message ?: "입장 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { navController.popBackStack() }
        )
    }

    if (!joined || room == null) return

    ChatTheme { // ✅ DM과 동일 톤 적용
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // 방 이름 더 크게 + 가운데
                            Text(
                                room!!.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            // "~참여중" 가운데
                            Text(
                                "${liveMemberCount}명 참여중",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { shareOpenChatLink(ctx, roomId) }) { Icon(Icons.Default.Share, null) }
                        IconButton(onClick = { showMembers = true }) { Icon(Icons.Default.Group, null) }
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("멤버 보기") }, onClick = { showMenu = false; showMembers = true })
                                DropdownMenuItem(text = { Text("링크 복사") }, onClick = {
                                    showMenu = false
                                    val uri = "foodtable://openchat?roomId=$roomId"
                                    clipboard.setText(AnnotatedString(uri))
                                    Toast.makeText(ctx, "링크 복사됨", Toast.LENGTH_SHORT).show()
                                })
                                DropdownMenuItem(text = { Text("친구 초대") }, onClick = { showMenu = false; showInvite = true })

                                if (isOwner) {
                                    val canTransfer = liveMembers.size >= 2
                                    DropdownMenuItem(
                                        text = { Text("방장 양도") },
                                        enabled = canTransfer,
                                        onClick = {
                                            showMenu = false
                                            if (canTransfer) showTransfer = true
                                            else Toast.makeText(ctx, "양도하려면 멤버가 2명 이상이어야 해요.", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("방 삭제") },
                                        onClick = {
                                            showMenu = false
                                            showDeleteConfirm = true
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("나가기") },
                                        onClick = {
                                            showMenu = false
                                            scope.launch {
                                                try {
                                                    vm.leaveRoom(roomId, myUid)
                                                    vm.sendSystem(roomId, "leave", myNick.ifBlank { myGlobalNick })
                                                    vm.removeMessageListener()
                                                    navController.popBackStack()
                                                } catch (e: Exception) {
                                                    Toast.makeText(ctx, e.message ?: "나가기 실패", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }

                                if (isOwner) {
                                    val willOpen = !(room?.open ?: true)
                                    DropdownMenuItem(
                                        text = { Text(if (willOpen) "공개로 전환" else "비공개로 전환") },
                                        onClick = {
                                            showMenu = false
                                            if (willOpen) {
                                                scope.launch {
                                                    vm.setRoomVisibility(roomId, true, null)
                                                    room = room?.copy(open = true, passcode = null)
                                                }
                                            } else {
                                                passcodeInput = ""
                                                showPasscodeDialog = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                )
            },
            floatingActionButton = {

                AnimatedVisibility(visible = !isNearBottom) {
                    FloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(messages.lastIndex) } },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "맨 아래로")
                    }
                }
            },
            bottomBar = {
                // ✅ DM과 동일 입력 바 재사용 (이미 프로젝트에 존재)
                com.bcu.foodtable.JetpackCompose.Social.ChatInputBar(
                    onSendMessage = { text ->
                        scope.launch { if (text.isNotBlank()) vm.sendText(roomId, myUid, text) }
                    },
                    onSendImage = { pickImageLauncher.launch("image/*") },
                    onSendMoney = { /* 오픈채팅은 미사용 */ }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { pad ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    when (msg.type) {
                        "system" -> SystemBubble(text = msg.text ?: "")
                        else -> RoomMessageBubble(
                            message = msg,
                            isMe = msg.senderUid == myUid,
                            unreadCount = (liveMemberCount - msg.readBy.size.toLong()).coerceAtLeast(0)
                        )
                    }
                }
            }
        }
    }

    // 멤버 시트
    if (showMembers) {
        MembersBottomSheet(
            roomId = roomId,
            isOwner = isOwner,
            onKick = { target -> scope.launch { vm.kickMember(roomId, target) } },
            onBan = { target -> scope.launch { vm.banMember(roomId, target, null) } },
            onDismiss = { showMembers = false }
        )
    }
    // 친구 초대(친구 DM으로 초대 메시지 전송; 멤버 추가는 수락 시점에 join)
    if (showInvite) {
        InviteFriendsSheet(
            onLoad = { vm.fetchFriends(myUid) },
            onInvite = { targets ->
                scope.launch {
                    try {
                        vm.sendInvitesAsDm(
                            roomId = roomId,
                            roomTitle = room?.title ?: "오픈채팅",
                            inviterUid = myUid,
                            targetUids = targets.map { it.uid }
                        )
                        Toast.makeText(ctx, "친구 채팅으로 초대 메시지를 보냈어요.", Toast.LENGTH_SHORT).show()
                        showInvite = false
                    } catch (e: Exception) {
                        Toast.makeText(ctx, e.message ?: "초대 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showInvite = false }
        )
    }
    // 방장 양도 시트
    if (showTransfer) {
        TransferOwnerSheet(
            members = liveMembers,
            currentOwnerUid = room!!.ownerUid,
            onTransfer = { targetUid ->
                scope.launch {
                    try {
                        vm.transferOwnership(roomId, targetUid)
                        Toast.makeText(ctx, "방장을 양도했습니다.", Toast.LENGTH_SHORT).show()
                        showTransfer = false
                    } catch (e: Exception) {
                        Toast.makeText(ctx, e.message ?: "양도 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showTransfer = false }
        )
    }

    // 방 삭제 확인 다이얼로그
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("방 삭제") },
            text = { Text("방의 모든 메시지가 삭제됩니다. 되돌릴 수 없습니다. 계속할까요?") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        try {
                            vm.deleteRoom(roomId, myUid)
                            vm.removeMessageListener()
                            Toast.makeText(ctx, "방을 삭제했습니다.", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Toast.makeText(ctx, e.message ?: "삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") } }
        )
    }

    // 비공개 전환 비번 다이얼로그
    if (showPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showPasscodeDialog = false },
            title = { Text("비공개 전환") },
            text = {
                Column {
                    Text("입장 비밀번호를 설정하세요.")
                    OutlinedTextField(
                        value = passcodeInput,
                        onValueChange = { passcodeInput = it },
                        label = { Text("비밀번호") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPasscodeDialog = false
                        scope.launch {
                            vm.setRoomVisibility(roomId, false, passcodeInput.ifBlank { null })
                            room = room?.copy(open = false, passcode = passcodeInput.ifBlank { "" })
                        }
                    },
                    enabled = passcodeInput.isNotBlank()
                ) { Text("적용") }
            },
            dismissButton = { TextButton(onClick = { showPasscodeDialog = false }) { Text("취소") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferOwnerSheet(
    members: List<OpenChatMember>,
    currentOwnerUid: String,
    onTransfer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }
    val candidates = remember(members, currentOwnerUid) { members.filter { it.uid != currentOwnerUid } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("방장 양도", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            if (candidates.isEmpty()) {
                Text("양도 가능한 멤버가 없습니다.")
            } else {
                candidates.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(m.nickname)
                            Text("멤버", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(
                            selected = (selected == m.uid),
                            onClick = { selected = m.uid }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { selected?.let(onTransfer) },
                    enabled = selected != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("양도하기") }
            }
        }
    }
}


@Composable
private fun JoinRoomSheet(
    room: OpenChatRoom,
    onJoin: (nickname: String, passcode: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("입장: ${room.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("닉네임") })
                if (!room.open) {
                    OutlinedTextField(value = passcode, onValueChange = { passcode = it }, label = { Text("비밀번호") })
                }
                Text("닉네임/프로필은 방 안에서만 보입니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = { onJoin(nickname.trim(), passcode.ifBlank { null }) }, enabled = nickname.isNotBlank()) {
                Text("입장하기")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
private fun SystemBubble(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape) {
            Text(
                text,
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoomMessageBubble(message: RoomMessage, isMe: Boolean, unreadCount: Long) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.KOREA) }
    val timeText = timeFormatter.format(java.util.Date(message.timestamp))

    val bubbleColor =
        if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor =
        if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = MaterialTheme.shapes.medium

    if (isMe) {
        // ===== 내가 보낸 메시지: 시간은 왼쪽(버블 왼쪽), 기존 유지 =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                timeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(end = 6.dp)
            )
            Box(modifier = Modifier.widthIn(max = 280.dp)) {
                Surface(color = bubbleColor, shape = shape) {
                    Column(
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                message.text?.takeIf { it.isNotBlank() }?.let {
                                    clipboard.setText(AnnotatedString(it))
                                    Toast.makeText(ctx, "메시지가 복사되었습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    ) {
                        message.text?.let {
                            Text(it, modifier = Modifier.padding(12.dp), color = textColor)
                        }
                        message.imageUrl?.let { url ->
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.sizeIn(maxWidth = 260.dp, maxHeight = 260.dp),
                                onError = { Toast.makeText(ctx, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show() }
                            )
                        }
                        AnimatedVisibility(visible = unreadCount > 0) {
                            Text(
                                "안 읽은 사람: $unreadCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // ===== 상대가 보낸 메시지: 닉네임은 '버블 위', 시간은 '버블 오른쪽 아래'(예전처럼) =====
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom // ← 시간(오른쪽)이 버블 하단 기준으로 정렬되도록
        ) {
            // 닉네임 + 버블을 하나의 Column으로 묶음
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                // 닉네임을 버블 '위'에
                Text(
                    message.senderNickname,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))

                // 버블
                Box(modifier = Modifier.widthIn(max = 280.dp)) {
                    Surface(color = bubbleColor, shape = shape) {
                        Column(
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    message.text?.takeIf { it.isNotBlank() }?.let {
                                        clipboard.setText(AnnotatedString(it))
                                        Toast.makeText(ctx, "메시지가 복사되었습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        ) {
                            message.text?.let {
                                Text(it, modifier = Modifier.padding(12.dp), color = textColor)
                            }
                            message.imageUrl?.let { url ->
                                AsyncImage(
                                    model = message.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.sizeIn(maxWidth = 260.dp, maxHeight = 260.dp),
                                    onError = { Toast.makeText(ctx, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show() }
                                )
                            }
                        }
                    }
                }
            }

            // 시간: 버블 오른쪽, 하단 정렬(예전과 동일 위치)
            Text(
                timeText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersBottomSheet(
    roomId: String,
    isOwner: Boolean,
    onKick: (String) -> Unit,
    onBan: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var members by remember { mutableStateOf(listOf<OpenChatMember>()) }

    DisposableEffect(roomId) {
        val reg = db.collection("openRooms").document(roomId)
            .collection("members")
            .addSnapshotListener { snap, _ ->
                members = snap?.documents?.mapNotNull { it.toObject(OpenChatMember::class.java) }.orEmpty()
            }
        onDispose { reg.remove() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("멤버 (${members.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            members.forEach { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(m.nickname)
                        Text(
                            if (m.role == "owner") "방장" else "멤버",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isOwner && m.role != "owner") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onKick(m.uid) }) { Text("강퇴") }
                            TextButton(onClick = { onBan(m.uid) }) { Text("밴") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteFriendsSheet(
    onLoad: suspend () -> List<Friend>,
    onInvite: (List<Friend>) -> Unit,
    onDismiss: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var friends by remember { mutableStateOf(listOf<Friend>()) }
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        loading = true
        friends = onLoad()
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("친구 초대", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (loading) {
                CircularProgressIndicator()
            } else if (friends.isEmpty()) {
                Text("초대할 친구가 없어요.")
            } else {
                friends.forEach { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(f.name)
                        Checkbox(
                            checked = selected[f.uid] == true,
                            onCheckedChange = { checked -> selected[f.uid] = checked }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val pick = friends.filter { selected[it.uid] == true }
                        if (pick.isNotEmpty()) onInvite(pick) else onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("초대하기") }
            }
        }
    }
}

private fun shareOpenChatLink(ctx: android.content.Context, roomId: String) {
    val uri = "foodtable://openchat?roomId=$roomId"
    ctx.startActivity(
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "오픈채팅 입장: $uri")
    )
}
