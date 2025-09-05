package com.bcu.foodtable.JetpackCompose.Social.Openchat

// Compose
import androidx.compose.runtime.*
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
import androidx.compose.material.icons.filled.CheckCircle

// UI utils
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Navigation & ViewModel
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bcu.foodtable.useful.UserManager
import android.text.format.DateUtils
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenChatHomeScreen(
    navController: NavHostController,
    vm: OpenChatViewModel = viewModel()
) {
    val me = UserManager.getUser() ?: return
    val myUid = me.uid

    // ✅ 실시간 구독 (생성/변경 즉시 반영)
    LaunchedEffect(Unit) { vm.observeDiscover() }
    LaunchedEffect(myUid) { vm.observeMyRooms(myUid) }

    val discover by vm.discover.collectAsState()
    val myRooms by vm.myRooms.collectAsState()

    var tab by remember { mutableStateOf(0) } // 0=탐색, 1=내 방
    var query by rememberSaveable { mutableStateOf("") }

    val joinedIds = remember(myRooms) { myRooms.map { it.id }.toSet() }

    fun filter(list: List<OpenChatRoom>): List<OpenChatRoom> {
        val q = query.trim()
        if (q.isEmpty()) return list
        return list.filter { it.title.contains(q, true) || it.desc.contains(q, true) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("오픈채팅", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = { navController.navigate("openchat_create") }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "방 만들기")
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            // 탭
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("탐색") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("내 방") })
            }

            // 검색 바 (콤팩트)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                placeholder = { Text("방 제목/소개 검색") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "지우기")
                        }
                    }
                }
            )

            if (tab == 1) {
                // 내 방만
                val list = filter(myRooms)
                SectionsList(
                    sections = listOf("내 방" to list),
                    joinedIds = joinedIds,
                    navController = navController
                )
            } else {
                // 탐색: 내가 들어간 방 + 전체 공개 방(내 방도 함께 노출)
                val joined = filter(discover.filter { it.id in joinedIds })
                val allPublic = filter(discover) // 중복 허용

                SectionsList(
                    sections = listOf(
                        "내가 들어가있는 방" to joined,
                        "전체 공개 방" to allPublic
                    ),
                    joinedIds = joinedIds,
                    navController = navController
                )
            }
        }
    }
}

/** 섹션형 리스트 (중복 key 충돌 방지 위해 섹션 prefix 부여) */
@Composable
private fun SectionsList(
    sections: List<Pair<String, List<OpenChatRoom>>>,
    joinedIds: Set<String>,
    navController: NavHostController
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sections.forEach { (title, list) ->
            // 섹션 타이틀
            item(key = "header_$title") {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            if (list.isEmpty()) {
                item(key = "empty_$title") {
                    Text(
                        "표시할 방이 없어요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else {
                // ✅ 섹션 prefix를 key에 붙여 중복 방(같은 id)도 충돌 없이 노출
                items(
                    items = list,
                    key = { room -> "${title}_${room.id}" }
                ) { room ->
                    RoomRow(
                        room = room,
                        isJoined = joinedIds.contains(room.id),
                        onClick = { navController.navigate("openchat/${room.id}") }
                    )
                }
            }

            item(key = "sp_$title") { Spacer(Modifier.height(4.dp)) }
        }
    }
}

/** 콤팩트한 리스트형 룸 아이템 */
@Composable
private fun RoomRow(
    room: OpenChatRoom,
    isJoined: Boolean,
    onClick: () -> Unit
) {
    // 꼭 필요한 정보만: 제목 / 멤버수 / 공개여부 / 최근활동 / (참여중 배지)
    val privacyIcon = if (room.open) Icons.Default.LockOpen else Icons.Default.Lock
    val last = remember(room.lastAt) {
        if (room.lastAt <= 0L) "방금 전"
        else DateUtils.getRelativeTimeSpanString(room.lastAt).toString()
    }

    // ListItem이 가장 콤팩트함
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(room.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (isJoined) {
                        Spacer(Modifier.width(8.dp))
                        AssistChip(
                            onClick = {},
                            label = { Text("참여중") },
                            leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                        )
                    }
                }
            },
            supportingContent = {
                // 소개는 한 줄만 (있을 때만)
                if (room.desc.isNotBlank()) {
                    Text(
                        room.desc,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            leadingContent = {
                // 공개/비공개
                Icon(privacyIcon, contentDescription = null)
            },
            trailingContent = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    // 멤버 수
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${room.memberCount}", style = MaterialTheme.typography.labelMedium)
                    }
                    // 최근 활동
                    Text(last, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
