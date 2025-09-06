// OpenChatRoomScreen.kt
package com.bcu.foodtable.JetpackCompose.Social.Openchat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingScreen
import com.bcu.foodtable.JetpackCompose.Social.ChatMessage
import com.bcu.foodtable.JetpackCompose.Social.ChatTheme
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    var showJoinDialog by remember { mutableStateOf(false) }        // 입장 다이얼로그 표시 여부
    var membershipChecked by remember { mutableStateOf(false) }     // 멤버십 판별 완료 게이트

    val storage = remember { FirebaseStorage.getInstance().reference }

    // 방 정보 + 내 멤버 여부 판별
    LaunchedEffect(roomId) {
        val db = FirebaseFirestore.getInstance()
        val doc = db.collection("openRooms").document(roomId).get().await()
        val r = doc.toObject(OpenChatRoom::class.java)?.copy(id = doc.id) ?: return@LaunchedEffect
        room = r
        isOwner = (r.ownerUid == myUid)

        val memDoc = db.collection("openRooms").document(roomId)
            .collection("members").document(myUid).get().await()
        joined = memDoc.exists()
        myNick = (memDoc.getString("nickname") ?: myGlobalNick).ifBlank { myGlobalNick }

        // 방장인데 아직 멤버가 아니면 자동 참가
        if (isOwner && !joined) {
            runCatching {
                vm.joinRoom(roomId, myUid, myGlobalNick)
                vm.sendSystem(roomId, "join", myGlobalNick)
                joined = true
            }
        }
        if (joined) vm.listenMessages(roomId)

        membershipChecked = true
        showJoinDialog = (!isOwner && !joined)   // 판별 끝난 뒤에 다이얼로그 열지 결정
    }

    // 멤버 실시간(상단 "n명 참여중", 멤버 시트)
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
    // 방 정보 로딩 이후 방 정보
    DisposableEffect("roomDoc_$roomId") {
        val db = FirebaseFirestore.getInstance()
        val reg = db.collection("openRooms").document(roomId)
            .addSnapshotListener { snap, _ ->
                val r = snap?.toObject(OpenChatRoom::class.java)?.copy(id = snap.id)
                if (r != null) {
                    room = r
                    isOwner = (r.ownerUid == myUid) // ← 메뉴 활성/비활성 즉시 반영
                }
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

    // 이미지 선택 → 업로드 후 downloadUrl로 전송
    val pickImageLauncher = rememberLauncherForActivityResult(GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                Toast.makeText(ctx, "이미지 업로드 중...", Toast.LENGTH_SHORT).show()
                val filename = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
                val ref = storage.child("openchatImages/$roomId/$myUid/$filename")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                vm.sendImage(roomId, myUid, url)
            } catch (e: Exception) {
                Toast.makeText(ctx, "이미지 전송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ===== 게이트: 멤버십 판별/방 정보 로딩 전에는 분기 X (깜빡임 방지) =====
    if (!membershipChecked || room == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // 미가입 & 방장 아님 → 입장 다이얼로그
    if (showJoinDialog) {
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
                        showJoinDialog = false
                    } catch (e: Exception) {
                        Toast.makeText(ctx, e.message ?: "입장 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showJoinDialog = false }
        )
    }

    // 미가입 + 다이얼로그 닫힌 자리표시 UI
    if (!joined && !showJoinDialog) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("이 방에 참여해야 대화를 볼 수 있어요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showJoinDialog = true }) { Text("참여하기") }
                    TextButton(onClick = { navController.popBackStack() }) { Text("뒤로가기") }
                }
            }
        }
        return
    }

    // ===== 여기부터 joined == true 일 때만 채팅 UI =====
    ChatTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                room!!.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "${liveMemberCount}명 참여중",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { shareOpenChatLink(ctx, roomId) }) {
                            Icon(Icons.Default.Share, contentDescription = "공유")
                        }
                        IconButton(onClick = { showMembers = true }) {
                            Icon(Icons.Default.Group, contentDescription = "멤버")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("멤버 보기") },
                                    onClick = { showMenu = false; showMembers = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("링크 복사") },
                                    onClick = {
                                        showMenu = false
                                        val uri = "foodtable://openchat?roomId=$roomId"
                                        clipboard.setText(AnnotatedString(uri))
                                        Toast.makeText(ctx, "링크 복사됨", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("친구 초대") },
                                    onClick = { showMenu = false; showInvite = true }
                                )

                                // 공개/비공개 전환: 방장만 활성
                                val willOpen = !(room?.open ?: true)
                                DropdownMenuItem(
                                    text = { Text(if (willOpen) "공개로 전환" else "비공개로 전환") },
                                    enabled = isOwner,
                                    onClick = {
                                        showMenu = false
                                        if (!isOwner) return@DropdownMenuItem
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

                                // 방장 양도: 방장 & 2명 이상일 때만 활성
                                val canTransfer = liveMemberCount >= 2
                                DropdownMenuItem(
                                    text = { Text("방장 양도") },
                                    enabled = isOwner && canTransfer,
                                    onClick = {
                                        showMenu = false
                                        if (isOwner && canTransfer) showTransfer = true
                                    }
                                )

                                // 방 삭제: 방장만
                                DropdownMenuItem(
                                    text = { Text("방 삭제") },
                                    enabled = isOwner,
                                    onClick = {
                                        showMenu = false
                                        if (isOwner) showDeleteConfirm = true
                                    }
                                )

                                // 방 나가기: 방장은 비활성(숨김), 멤버만 표시
                                if (!isOwner) {
                                    DropdownMenuItem(
                                        text = { Text("방 나가기") },
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
                com.bcu.foodtable.JetpackCompose.Social.ChatInputBar(
                    onSendMessage = { text ->
                        scope.launch { if (text.isNotBlank()) vm.sendText(roomId, myUid, text) }
                    },
                    onSendImage = { pickImageLauncher.launch("image/*") },
                    onSendMoney = { /* 오픈채팅 미사용 */ }
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
                        "recipe" -> {
                            RecipeShareBubble(
                                message = ChatMessage( // RoomMessage → 필요한 필드만 매핑
                                    id = msg.id,
                                    senderUid = msg.senderUid,
                                    text = msg.text,
                                    imageUrl = msg.imageUrl,
                                    timestamp = msg.timestamp,
                                    type = "recipe",
                                    deeplink = msg.deeplink
                                ),
                                onOpen = { rid ->
                                    navController.navigate("recipe_by_id/${Uri.encode(rid)}")
                                }
                            )
                        }
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

    // ===== 바텀시트/다이얼로그들 =====

    if (showMembers) {
        MembersBottomSheet(
            roomId = roomId,
            isOwner = isOwner,
            onKick = { target -> scope.launch { vm.kickMember(roomId, target) } },
            onBan = { target -> scope.launch { vm.banMember(roomId, target, null) } },
            onDismiss = { showMembers = false }
        )
    }

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

    if (showTransfer) {
        TransferOwnerSheet(
            members = liveMembers,
            currentOwnerUid = room!!.ownerUid,
            onTransfer = { targetUid ->
                scope.launch {
                    try {
                        room = room?.copy(ownerUid = targetUid)
                        isOwner = (targetUid == myUid)
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
        // 내가 보낸 메시지: 시간은 왼쪽(버블 왼쪽)
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
                        message.imageUrl?.let {
                            AsyncImage(
                                model = it,
                                contentDescription = null,
                                modifier = Modifier.sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
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
        // 상대가 보낸 메시지: 닉네임은 버블 '위', 시간은 버블 오른쪽 하단
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    message.senderNickname,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
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
                            message.imageUrl?.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = null,
                                    modifier = Modifier.sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                                )
                            }
                        }
                    }
                }
            }
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
                        Text(if (m.role == "owner") "방장" else "멤버", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onKick(m.uid) },
                            enabled = isOwner && m.role != "owner"
                        ) { Text("강퇴") }
                        TextButton(
                            onClick = { onBan(m.uid) },
                            enabled = isOwner && m.role != "owner"
                        ) { Text("밴") }
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
// 레시피 공유 버블
@Composable
fun RecipeShareBubble(
    message: ChatMessage,
    onOpen: (recipeId: String) -> Unit
) {
    val title = message.text ?: "레시피"
    val thumb = message.imageUrl
    val rid = remember(message.deeplink) {
        try {
            Uri.parse(message.deeplink ?: "").getQueryParameter("rid") ?: ""
        } catch (_: Exception) { "" }
    }
    val isEnabled = rid.isNotBlank()

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        ElevatedCard {
            Column(Modifier.padding(16.dp).widthIn(max = 340.dp)) {
                Text("레시피 공유", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                if (!thumb.isNullOrBlank()) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { if (isEnabled) onOpen(rid) }, enabled = isEnabled) {
                    Text("레시피 보기")
                }
            }
        }
    }
}
@Composable
fun RecipeByIdScreen(rid: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var recipe by remember { mutableStateOf<RecipeItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var purchased by remember { mutableStateOf(false) }
    var askPurchase by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var purchasing by remember { mutableStateOf(false) }

    LaunchedEffect(rid) {
        loading = true
        error = null
        try {
            android.util.Log.d("RecipeNav", "open rid=$rid")
            val doc = db.collection("recipe").document(rid).get().await()
            if (!doc.exists()) {
                error = "레시피를 찾을 수 없어요. (rid=$rid)"
                return@LaunchedEffect
            }
            recipe = doc.toObject(RecipeItem::class.java)?.copy(id = doc.id)

            if (uid != null) {
                val p = db.collection("user").document(uid)
                    .collection("purchased").document(rid)
                    .get().await()
                purchased = (p.getBoolean("purchased") == true)
                askPurchase = !purchased
            } else {
                askPurchase = true // 비로그인 → 구매 필요 처리
            }
        } catch (e: Exception) {
            error = "레시피 로딩 실패: ${e.message}"
        } finally {
            loading = false
        }
    }

    when {
        loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { navController.popBackStack() }) { Text("뒤로가기") }
                }
            }
        }
        recipe == null -> { // 이 경우도 안전하게 처리
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("레시피 데이터를 불러오지 못했어요.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { navController.popBackStack() }) { Text("뒤로가기") }
                }
            }
        }
        askPurchase -> {
            AlertDialog(
                onDismissRequest = { navController.popBackStack() },
                title = { Text("레시피 구매 필요") },
                text = { Text("이 레시피를 보려면 구매가 필요합니다. 지금 구매할까요?") },
                confirmButton = {
                    Button(
                        enabled = uid != null && !purchasing,
                        onClick = {
                            if (uid == null) {
                                Toast.makeText(ctx, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val price = (recipe?.cost ?: 100)
                            purchasing = true
                            scope.launch {
                                try {
                                    purchaseRecipeWithPoints(db, uid, rid, price)
                                    askPurchase = false
                                    purchased = true
                                    Toast.makeText(ctx, "구매 완료! 🎉 (${price} 소금 차감)", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, e.message ?: "구매 실패", Toast.LENGTH_LONG).show()
                                } finally {
                                    purchasing = false
                                }
                            }
                        }
                    ) { Text(if (purchasing) "결제 중..." else "구매") }
                },
                dismissButton = {
                    TextButton(onClick = { navController.popBackStack() }) { Text("취소") }
                }
            )
        }
        else -> {
            // 모든 조건 OK → 본문
            RecipeCookingScreen(recipe = recipe!!, navController = navController)
        }
    }
}

suspend fun purchaseRecipeWithPoints(
    db: FirebaseFirestore,
    uid: String,
    recipeId: String,
    cost: Int
) {
    val userRef = db.collection("user").document(uid)
    val purchasedRef = userRef.collection("purchased").document(recipeId)

    db.runTransaction { tx ->
        // 이미 구매했으면 아무 것도 하지 않음(재차감 방지)
        val purchasedDoc = tx.get(purchasedRef)
        if (purchasedDoc.getBoolean("purchased") == true) return@runTransaction null

        val userDoc = tx.get(userRef)
        val currentPoint = userDoc.getLong("point") ?: 0L
        val need = cost.toLong()
        if (currentPoint < need) {
            throw IllegalStateException("소금이 부족합니다. (보유: $currentPoint, 필요: $need)")
        }

        // 포인트 차감 + 구매 플래그 기록 (원자적)
        tx.update(userRef, "point", FieldValue.increment(-need))
        tx.set(
            purchasedRef,
            mapOf(
                "purchased" to true,
                "price" to need,
                "ts" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
        null
    }.await()
}