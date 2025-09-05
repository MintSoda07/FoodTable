// OpenChatRoomScreen.kt
package com.bcu.foodtable.JetpackCompose.Social.Openchat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
            // TODO: Storage 업로드 후 downloadUrl 사용
            val url = uri.toString()
            vm.sendImage(roomId, myUid, url)
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(room!!.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${room!!.memberCount}명 참여중",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                // ✅ 뒤로가기 버튼 제거(시스템 Back은 유지)
                actions = {
                    IconButton(onClick = { shareOpenChatLink(ctx, roomId) }) { Icon(Icons.Default.Share, null) }
                    IconButton(onClick = { showMembers = true }) { Icon(Icons.Default.Group, null) }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("멤버 보기") }, onClick = { showMenu = false; showMembers = true })
                            DropdownMenuItem(
                                text = { Text("링크 복사") },
                                onClick = {
                                    showMenu = false
                                    val uri = "foodtable://openchat?roomId=$roomId"
                                    clipboard.setText(AnnotatedString(uri))
                                    Toast.makeText(ctx, "링크 복사됨", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(text = { Text("친구 초대") }, onClick = { showMenu = false; showInvite = true })
                            DropdownMenuItem(
                                text = { Text("나가기") },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        vm.leaveRoom(roomId, myUid)
                                        vm.sendSystem(roomId, "leave", myNick.ifBlank { myGlobalNick })
                                        vm.removeMessageListener()
                                        navController.popBackStack()
                                    }
                                }
                            )
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
                }
            )
        },
        floatingActionButton = {
            // ✅ 사용자가 위로 스크롤했을 때만 노출되는 "맨 아래로" FAB
            AnimatedVisibility(visible = !isNearBottom) {
                ExtendedFloatingActionButton(
                    icon = { Icon(Icons.Default.ArrowDownward, null) },
                    text = { Text("맨 아래로") },
                    onClick = { scope.launch { listState.animateScrollToItem(messages.lastIndex) } }
                )
            }
        },
        bottomBar = {
            com.bcu.foodtable.JetpackCompose.Social.ChatInputBar(
                onSendMessage = { text ->
                    scope.launch { if (text.isNotBlank()) vm.sendText(roomId, myUid, text) }
                },
                onSendImage = { pickImageLauncher.launch("image/*") },
                onSendMoney = { /* 미사용 */ }
            )
        }
    ) { pad ->
        LazyColumn(
            state = listState, // ✅
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                when (msg.type) {
                    "system" -> SystemBubble(text = msg.text ?: "")
                    else -> RoomMessageBubble(
                        message = msg,
                        isMe = msg.senderUid == myUid,
                        unreadCount = (room!!.memberCount - (msg.readBy.size)).coerceAtLeast(0)
                    )
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

@Composable
private fun RoomMessageBubble(message: RoomMessage, isMe: Boolean, unreadCount: Long) {
    val align = if (isMe) Arrangement.End else Arrangement.Start
    Row(Modifier.fillMaxWidth(), horizontalArrangement = align) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            if (!isMe) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(Color.Gray, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        message.senderNickname,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
            Surface(
                color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(10.dp)) {
                    message.text?.let { Text(it) }
                    message.imageUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier.sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                        )
                    }
                    AnimatedVisibility(visible = unreadCount > 0 && isMe) {
                        Text(
                            "안 읽은 사람: $unreadCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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

    LaunchedEffect(roomId) {
        val snap = db.collection("openRooms").document(roomId).collection("members").get().await()
        members = snap.documents.mapNotNull { it.toObject(OpenChatMember::class.java) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("멤버 (${members.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            members.forEach { m ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(m.nickname)
                        Text(if (m.role == "owner") "방장" else "멤버", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
