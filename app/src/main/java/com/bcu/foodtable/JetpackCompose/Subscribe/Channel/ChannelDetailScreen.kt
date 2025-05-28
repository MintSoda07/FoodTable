package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ChannelDetailScreen.kt

@Composable
fun ChannelDetailScreen(
    channelName: String,
    navController: NavController,
) {
    val db = FirebaseFirestore.getInstance()
    val userId = UserManager.getUser()?.uid

    var channel by remember { mutableStateOf<Channel?>(null) }
    var recipes by remember { mutableStateOf<List<RecipeItem>>(emptyList()) }
    var isSubscribed by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Recipes") }
    var isLoading by remember { mutableStateOf(true) }

    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    suspend fun checkSubscription(channelName: String, userId: String): Boolean {
        val snapshot = db.collection("channel_subscribe")
            .whereEqualTo("channel", channelName)
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return !snapshot.isEmpty
    }

    suspend fun subscribeToChannel(channelName: String, subscriberId: String) {
        val channelSnapshot = db.collection("channel")
            .whereEqualTo("name", channelName)
            .limit(1)
            .get()
            .await()
        val ownerId = channelSnapshot.documents.firstOrNull()?.getString("userId") ?: return

        db.collection("channel_subscribe").add(
            mapOf(
                "channel" to channelName,
                "userId" to subscriberId,
                "date" to Timestamp.now()
            )
        )

        if (ownerId != subscriberId) {
            db.collection("notifications").add(
                mapOf(
                    "to" to ownerId,
                    "message" to "$channelName 채널을 누군가 구독했습니다.",
                    "date" to Timestamp.now(),
                    "read" to false
                )
            )
        }
    }

    LaunchedEffect(channelName) {
        try {
            val snapshot = db.collection("channel")
                .whereEqualTo("name", channelName)
                .limit(1)
                .get()
                .await()
            val data = snapshot.documents.firstOrNull()?.toObject(Channel::class.java)
            channel = data

            if (data != null) {
                val recipeSnapshot = db.collection("recipe")
                    .whereEqualTo("contained_channel", data.name)
                    .get()
                    .await()
                recipes = recipeSnapshot.documents.mapNotNull { it.toObject(RecipeItem::class.java) }

                userId?.let {
                    isSubscribed = checkSubscription(data.name, it)
                }
            }
        } catch (e: Exception) {
            Log.e("ChannelDetail", "Error loading channel: ", e)
        } finally {
            isLoading = false
        }
    }

    if (userId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("로그인이 필요합니다.")
        }
        return
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    channel?.let { ch ->
        val isMyChannel = userId == ch.owner

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            AsyncImage(
                model = ch.BackgroundResId,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = ch.imageResId,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(50)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(text = ch.name, style = typography.titleLarge)
                    Text("${ch.subscribers}명 구독 중", style = typography.bodySmall)
                }
                Spacer(Modifier.weight(1f))
                if (!isMyChannel) {
                    val coroutineScope = rememberCoroutineScope()

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                subscribeToChannel(ch.name, userId)
                                isSubscribed = true
                            }
                        }
                    ) {
                        Text(if (isSubscribed) "구독중" else "구독하기")
                    }
                } else {
                    OutlinedButton(onClick = {
                        navController.navigate("editChannel/${ch.name}")
                    }) {
                        Text("채널 관리")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            TabRow(selectedTabIndex = if (selectedTab == "Recipes") 0 else 1) {
                Tab(
                    selected = selectedTab == "Recipes",
                    onClick = { selectedTab = "Recipes" },
                    text = { Text("Recipes") }
                )
                Tab(
                    selected = selectedTab == "Liked",
                    onClick = { selectedTab = "Liked" },
                    text = { Text("Liked") }
                )
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                "Recipes" -> {
                    LazyColumn {
                        items(recipes.filter { it.contained_channel == ch.name }) { recipe ->
                            com.bcu.foodtable.JetpackCompose.Subscribe.RecipeCard(recipe = recipe) {
                                navController.navigate("recipeView/${recipe.id}")
                            }
                        }
                    }
                }
                "Liked" -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("좋아요한 레시피는 아직 없습니다.")
                    }
                }
            }
        }
    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("채널 정보를 불러올 수 없습니다.")
    }
}


