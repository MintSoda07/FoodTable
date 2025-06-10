package com.bcu.foodtable.JetpackCompose.Social

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ChatTab(
    navController: NavHostController
) {
    val context = LocalContext.current
    val colors  = WarmLightColorScheme
    val uid     = UserManager.getUser()!!.uid
    val db      = FirebaseFirestore.getInstance()

    // 1) /users/{uid}/chats 문서 ID 목록
    var friendUids by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingUids by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        try {
            val snap = db.collection("users")           // ← users로 변경
                .document(uid)
                .collection("chats")
                .get()
                .await()
            friendUids = snap.documents.map { it.id }
        } catch (e: Exception) {
            Toast.makeText(context, "채팅 스레드 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            loadingUids = false
        }
    }

    // 2) 각 friendUid에 대응하는 User 로딩
    val friends = remember { mutableStateListOf<User>() }
    var loadingUsers by remember { mutableStateOf(false) }

    LaunchedEffect(friendUids) {
        if (friendUids.isNotEmpty()) {
            loadingUsers = true
            friends.clear()
            friendUids.forEach { friendUid ->
                try {
                    val doc = db.collection("users")       // ← users로 변경
                        .document(friendUid)
                        .get()
                        .await()
                    doc.toObject(User::class.java)
                        ?.let { friends.add(it.copy(uid = friendUid)) }
                } catch (_: Exception) { /* ignore */ }
            }
            loadingUsers = false
        }
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        Text(
            text = "채팅 친구 목록",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        when {
            loadingUids || loadingUsers -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
            friends.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "대화를 시작한 친구가 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onBackground
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(friends, key = { it.uid }) { user ->
                        ChatContactCard(
                            user = user,
                            colors = colors,
                            onClick = { navController.navigate("chat/${user.uid}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatContactCard(
    user: User,
    colors: ColorScheme,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.image.ifBlank { null } ?: R.drawable.baseline_restaurant_menu_24,
                contentDescription = null,
                placeholder = painterResource(R.drawable.baseline_restaurant_menu_24),
                error       = painterResource(R.drawable.baseline_restaurant_menu_24),
                modifier    = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = user.name.ifBlank { "이름 없음" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = user.description.ifBlank { "설명이 없습니다." },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Chat, contentDescription = "채팅", tint = colors.primary)
            }
        }
    }
}
