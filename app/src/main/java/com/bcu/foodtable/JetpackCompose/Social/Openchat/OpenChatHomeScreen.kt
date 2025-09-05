package com.bcu.foodtable.JetpackCompose.Social.Openchat

// Compose
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState

// Layout & lists
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// Material 3
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api

// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.WorkspacePremium // 왕관 느낌 뱃지

// UI utils
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Navigation & ViewModel
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenChatHomeScreen(
    navController: NavHostController,
    vm: OpenChatViewModel = viewModel()
) {
    val me = UserManager.getUser() ?: return
    val myUid = me.uid

    // 실시간 구독 시작
    LaunchedEffect(Unit) { vm.observeDiscover() }     // 공개방
    LaunchedEffect(myUid) { vm.observeMyRooms(myUid) } // 내가 들어간 방

    val discover by vm.discover.collectAsState() // open == true 만
    val myRooms by vm.myRooms.collectAsState()   // memberIds array-contains

    var tab by rememberSaveable { mutableStateOf(0) } // 0: 탐색, 1: 내 방
    var query by rememberSaveable { mutableStateOf("") }

    // 내가 들어간 방 id 집합 (탐색에서 제외용)
    val myRoomIds = remember(myRooms) { myRooms.map { it.id }.toSet() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("오픈채팅", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = { navController.navigate("openchat_create") }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "방 만들기")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("탐색") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("내 방") })
            }

            // 검색창
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "지우기")
                            }
                        }
                    },
                    placeholder = { Text("방 제목/소개 검색") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (tab == 0) {
                //
                val publicList = remember(discover, myRoomIds, query) {
                    discover // observeDiscover가 open==true로 가져옴
                        .filter { it.id !in myRoomIds }
                        .filter { it.title.contains(query, true) || it.desc.contains(query, true) }
                }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(publicList, key = { it.id }) { room ->
                        RoomRow(
                            room = room,
                            myUid = myUid,               // crown 판단에 사용 (탐색에서도 방장 표시 가능)
                            onClick = { navController.navigate("openchat/${room.id}") }
                        )
                    }
                }
            } else {
                //
                val joinedList = remember(myRooms, query) {
                    myRooms.filter { it.title.contains(query, true) || it.desc.contains(query, true) }
                }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(joinedList, key = { it.id }) { room ->
                        RoomRow(
                            room = room,
                            myUid = myUid,               // 방장 여부 판단해서 왕관 표시
                            onClick = { navController.navigate("openchat/${room.id}") }
                        )
                    }
                }
            }
        }
    }
}

/** 콤팩트 카드: 썸네일 + 제목(방장=왕관) + 한줄소개 + 실시간 인원 + 공개/비공개 아이콘 */
@Composable
private fun RoomRow(
    room: OpenChatRoom,
    myUid: String?,
    onClick: () -> Unit
) {
    // 실시간 멤버 수
    var liveCount by remember { mutableStateOf(0) }
    DisposableEffect(room.id) {
        val db = FirebaseFirestore.getInstance()
        val reg = db.collection("openRooms").document(room.id)
            .collection("members")
            .addSnapshotListener { snap, _ -> liveCount = snap?.size()?.toInt() ?: 0 }
        onDispose { reg.remove() }
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 썸네일
            AsyncImage(
                model = room.thumbUrl.ifBlank { null },
                contentDescription = null,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    //
                    if (myUid != null && room.ownerUid == myUid) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = "방장",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(room.title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                }
                if (room.desc.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        room.desc,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text("$liveCount") },
                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) }
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (room.open) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = if (room.open) "공개" else "비공개",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
