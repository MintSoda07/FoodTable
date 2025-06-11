package com.bcu.foodtable.JetpackCompose.Social

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "ChatTabDebug"

@Composable
fun ChatTab(
    navController: NavHostController
) {
    val context = LocalContext.current
    val colors  = lightColorScheme(
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
    val uid     = UserManager.getUser()!!.uid
    val db      = FirebaseFirestore.getInstance()
    val scope   = rememberCoroutineScope()

    // 1) /user/{uid}/chats 문서 ID 목록
    var friendUids by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingUids by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        Log.d(TAG, "▶ start loading chat threads for user=$uid at /user/$uid/chats")
        try {
            val snap = db.collection("user")
                .document(uid)
                .collection("chats")
                .get()
                .await()
            Log.d(TAG, "✔ chat threads snapshot size=${snap.size()}")
            friendUids = snap.documents.map { it.id }
            Log.d(TAG, "▶ friendUids=$friendUids")
        } catch (e: Exception) {
            Log.e(TAG, "✖ failed to load chat threads", e)
            Toast.makeText(context, "채팅 스레드 로드 실패", Toast.LENGTH_SHORT).show()
        } finally {
            loadingUids = false
            Log.d(TAG, "▶ loadingUids=false")
        }
    }

    // 2) 각 friendUid에 대응하는 User 객체 로딩
    val friends = remember { mutableStateListOf<User>() }
    var loadingUsers by remember { mutableStateOf(false) }

    LaunchedEffect(friendUids) {
        Log.d(TAG, "▶ start loading user details for friendUids")
        if (friendUids.isNotEmpty()) {
            loadingUsers = true
            Log.d(TAG, "▶ loadingUsers=true")
            friends.clear()
            friendUids.forEach { fid ->
                try {
                    Log.d(TAG, "  • loading user for uid=$fid at /user/$fid")
                    val doc = db.collection("user")
                        .document(fid)
                        .get()
                        .await()
                    val user = doc.toObject(User::class.java)?.copy(uid = fid)
                    if (user != null) {
                        friends.add(user)
                        Log.d(TAG, "  ✔ loaded user=$user")
                    } else {
                        Log.w(TAG, "  ⚠ user doc exists but toObject returned null: $fid")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "  ✖ failed to load user $fid", e)
                }
            }
            loadingUsers = false
            Log.d(TAG, "▶ loadingUsers=false, total friends=${friends.size}")
        } else {
            Log.d(TAG, "▶ friendUids empty, skip loading users")
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
                Log.d(TAG, "▶ showing loading (uids=$loadingUids, users=$loadingUsers)")
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
            friends.isEmpty() -> {
                Log.d(TAG, "▶ no friends to display")
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "대화를 시작한 친구가 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onBackground
                    )
                }
            }
            else -> {
                Log.d(TAG, "▶ displaying ${friends.size} friends")
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
                            onClick = {
                                Log.d(TAG, "▶ navigate to chat with uid=${user.uid}")
                                navController.navigate("chat/${user.uid}")
                            }
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
    val context = LocalContext.current
    val db      = FirebaseFirestore.getInstance()

    var friendName by remember { mutableStateOf(user.uid) }
    var friendImage by remember { mutableStateOf<String?>(null) }

    // 프로필 로드
    LaunchedEffect(user.uid) {
        Log.d(TAG, "▶ fetch profile for uid=${user.uid} at /user/${user.uid}")
        try {
            val doc = db.collection("user")
                .document(user.uid)
                .get()
                .await()
            doc.getString("name")?.let {
                friendName = it
                Log.d(TAG, "  ✔ name=$it")
            }
            doc.getString("image")?.let {
                friendImage = it
                Log.d(TAG, "  ✔ image=$it")
            }
        } catch (e: Exception) {
            Log.e(TAG, "  ✖ failed to fetch profile for ${user.uid}", e)
            Toast.makeText(context, "프로필 로드 실패", Toast.LENGTH_SHORT).show()
        }
    }

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
                model = friendImage ?: R.drawable.baseline_restaurant_menu_24,
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
                    text = friendName.ifBlank { "이름 없음" },
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
