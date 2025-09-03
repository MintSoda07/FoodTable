package com.bcu.foodtable.ui.rank

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 랭킹 화면: 구독자/채널/레시피 정렬, TOP50 유저 표시
 * 유저 클릭 시 채널 목록 바텀시트, 프로필 화면 네비게이션
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankScreenImproved(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val myUid = remember { UserManager.getUser()?.uid ?: "" }

    var sortOption by rememberSaveable { mutableStateOf(SortOption.Subscribers) }
    var query by rememberSaveable { mutableStateOf("") }

    val rankingList = remember { mutableStateListOf<UserRankingItem>() }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedUser by remember { mutableStateOf<UserRankingItem?>(null) }
    var userChannels by remember { mutableStateOf<List<ChannelUI>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<Throwable?>(null) }

    LaunchedEffect(sortOption) {
        isLoading = true; loadError = null
        try {
            val docs = firestore.collection("user").get().await().documents
            val items = docs.mapNotNull { doc ->
                val uid = doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                val channelsDocs = firestore.collection("channel")
                    .whereEqualTo("owner", uid).get().await().documents
                val channelCount = channelsDocs.size
                val subscriberCount = channelsDocs.sumOf { it.getLong("subscribers")?.toInt() ?: 0 }
                val channelNames = channelsDocs.mapNotNull { it.getString("name") }
                val recipeCount = firestore.collection("recipe").get().await().documents
                    .count { rd -> rd.getString("contained_channel") in channelNames }
                UserRankingItem(uid, name, channelCount, recipeCount, subscriberCount, uid == myUid)
            }
            val sorted = when (sortOption) {
                SortOption.Subscribers -> items.sortedByDescending { it.subscriberCount }
                SortOption.Channels    -> items.sortedByDescending { it.channelCount }
                SortOption.Recipes     -> items.sortedByDescending { it.recipeCount }
            }.take(50)

            rankingList.clear()
            rankingList.addAll(sorted)
        } catch (e: Exception) {
            loadError = e
        } finally {
            isLoading = false
        }
    }

    // 채널 바텀시트 (구독자 내림차순 정렬)
    if (selectedUser != null) {
        ChannelSheet(
            sheetState = sheetState,
            user = selectedUser!!,
            channels = userChannels.sortedByDescending { it.subscribers },
            onDismiss = { selectedUser = null },
            onOpenProfile = {
                navController.navigate("profile/${selectedUser!!.uid}")
                selectedUser = null
            },
            onChannelClick = { channelName ->
                scope.launch {
                    sheetState.hide()
                    selectedUser = null
                    val encoded = Uri.encode(channelName)
                    navController.navigate("channelView/$encoded") // 3) 이동
                }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "🏆 랭킹 TOP 50",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {},
                actions = {},
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    )  { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            // 본문 헤더: 검색 + 정렬칩(가로 스크롤)
            HeaderControls(
                selected = sortOption,
                onSelect = { sortOption = it },
                query = query,
                onQueryChange = { query = it }
            )

            when {
                isLoading -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) { items(8) { UserRowShimmer() } }
                }
                loadError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "랭킹을 불러오지 못했어요.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            loadError?.localizedMessage ?: "알 수 없는 오류",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { sortOption = sortOption }) { Text("다시 시도") }
                    }
                }
                else -> {
                    // 매 프레임 계산 (또는 toList()로 스냅샷 고정) → 정렬/필터 즉시 반영
                    val filtered =
                        if (query.isBlank()) rankingList.toList()
                        else rankingList.filter { it.name.contains(query.trim(), ignoreCase = true) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(filtered, key = { _, it -> it.uid }) { idx, user ->
                            AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.98f)) {
                                RankingCard(
                                    rank = idx + 1,
                                    item = user,
                                    onClick = {
                                        scope.launch {
                                            val docs = firestore.collection("channel")
                                                .whereEqualTo("owner", user.uid)
                                                .get().await().documents
                                            userChannels = docs.map { d ->
                                                ChannelUI(
                                                    d.getString("name") ?: "-",
                                                    d.getLong("subscribers")?.toInt() ?: 0
                                                )
                                            }
                                            selectedUser = user
                                        }
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

@Composable
private fun HeaderControls(
    selected: SortOption,
    onSelect: (SortOption) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit
) {
    Column {
        // 검색
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            singleLine = true,
            placeholder = { Text("유저 이름 검색") }
        )
        // 정렬칩 (가로 스크롤)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selected == SortOption.Subscribers,
                onClick = { onSelect(SortOption.Subscribers) },
                label = { Text(SortOption.Subscribers.label) }
            )
            FilterChip(
                selected = selected == SortOption.Channels,
                onClick = { onSelect(SortOption.Channels) },
                label = { Text(SortOption.Channels.label) }
            )
            FilterChip(
                selected = selected == SortOption.Recipes,
                onClick = { onSelect(SortOption.Recipes) },
                label = { Text(SortOption.Recipes.label) }
            )
        }
        Divider(Modifier.padding(top = 4.dp))
    }
}


@Composable
fun RankingCard(
    rank: Int,
    item: UserRankingItem,
    onClick: () -> Unit
) {
    val isTop3 = rank in 1..3
    val container = when (rank) {
        1 -> Color(0xFFFFF7D1)
        2 -> Color(0xFFF3F4F6)
        3 -> Color(0xFFFFEFE3)
        else -> MaterialTheme.colorScheme.surface
    }
    val content = if (isTop3) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = if (isTop3) CardDefaults.cardElevation(4.dp) else CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth()
        ) {
            // 순위 배지
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (rank) {
                            1 -> Color(0xFFFFD54F)
                            2 -> Color(0xFFCFD8DC)
                            3 -> Color(0xFFBCAAA4)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    fontWeight = FontWeight.Bold,
                    color = if (isTop3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (item.isCurrentUser) MaterialTheme.colorScheme.primary else content
                    )
                    if (item.isCurrentUser) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "나",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "채널 ${item.channelCount} · 레시피 ${item.recipeCount} · 구독자 ${item.subscriberCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun SortChips(
    selected: SortOption,
    onSelect: (SortOption) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        SortOption.values().forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick = { onSelect(opt) },
                label = { Text(opt.label) }
            )
        }
    }
}

@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        singleLine = true,
        placeholder = { Text(placeholder) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelSheet(
    sheetState: SheetState,
    user: UserRankingItem,
    channels: List<ChannelUI>,
    onDismiss: () -> Unit,
    onOpenProfile: () -> Unit,
    onChannelClick: (String) -> Unit  //
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "${user.name}님의 채널",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))

            if (channels.isEmpty()) {
                Text(
                    text = "채널이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(channels, key = { it.name }) { ch ->
                        ElevatedCard(
                            onClick = { onChannelClick(ch.name) }, //
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 심볼
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(10.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        ch.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "구독자 ${ch.subscribers}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenProfile,
                modifier = Modifier.fillMaxWidth()
            ) { Text("프로필 보기") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun UserRowShimmer() {
    // 아주 간단한 Placeholder
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {}
}

// --- 기존 enum/data는 유지 ---
enum class SortOption(val label: String) {
    Subscribers("구독자순"),
    Channels("채널순"),
    Recipes("레시피순")
}

data class UserRankingItem(
    val uid: String,
    val name: String,
    val channelCount: Int,
    val recipeCount: Int,
    val subscriberCount: Int,
    val isCurrentUser: Boolean = false
)

data class ChannelUI(
    val name: String,
    val subscribers: Int
)
