package com.bcu.foodtable.ui.rank

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    var showSortMenu by remember { mutableStateOf(false) }

    val rankingList = remember { mutableStateListOf<UserRankingItem>() }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState() // 초기값 Hidden
    var selectedUser by remember { mutableStateOf<UserRankingItem?>(null) }
    var userChannels by remember { mutableStateOf<List<ChannelUI>>(emptyList()) }

    LaunchedEffect(sortOption) {
        try {
            Log.d("RankScreen", "Fetching users...")
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
                SortOption.Channels -> items.sortedByDescending { it.channelCount }
                SortOption.Recipes -> items.sortedByDescending { it.recipeCount }
            }.take(50)
            rankingList.clear(); rankingList.addAll(sorted)
            Log.d("RankScreen", "Loaded ${sorted.size} users")
        } catch (e: Exception) {
            Log.e("RankScreen", "Error loading ranking", e)
        }
    }

    if (selectedUser != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { selectedUser = null }
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "${selectedUser!!.name}님의 채널",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn {
                    items(userChannels) { ch ->
                        Text(
                            text = "• ${ch.name} (${ch.subscribers}명)",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    navController.navigate("profile/${selectedUser!!.uid}")
                    selectedUser = null
                }) {
                    Text("프로필 보기")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("🏆 랭킹 TOP50", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.values().forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = {
                                    sortOption = opt
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { pad ->
        LazyColumn(
            contentPadding = pad,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(rankingList, key = { _, it -> it.uid }) { idx, user ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f)
                ) {
                    RankingCard(
                        rank = idx + 1,
                        item = user,
                        onClick = {
                            scope.launch {
                                val docs = firestore.collection("channel")
                                    .whereEqualTo("owner", user.uid)
                                    .get().await().documents
                                userChannels = docs.map { d -> ChannelUI(d.getString("name") ?: "-", d.getLong("subscribers")?.toInt() ?: 0) }
                                selectedUser = user
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RankingCard(
    rank: Int,
    item: UserRankingItem,
    onClick: () -> Unit
) {
    val background = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = if (item.isCurrentUser) Color.Red else MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.Black)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "$rank 위 - ${item.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "채널: ${item.channelCount}개 · 레시피: ${item.recipeCount}개 · 구독자: ${item.subscriberCount}명",
                    fontSize = 14.sp,
                    color = textColor
                )
            }
        }
    }
}

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
