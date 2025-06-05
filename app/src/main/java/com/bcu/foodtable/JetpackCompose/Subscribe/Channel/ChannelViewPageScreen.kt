package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity
import com.bcu.foodtable.JetpackCompose.Subscribe.RecipeCard
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun ChannelViewPageScreen(
    channelName: String,
    navController: NavHostController,
    viewModel: ChannelViewModel = viewModel()
) {
    val context = LocalContext.current
    val userId = remember { UserManager.getUser()?.uid ?: "" }

    var selectedTab by remember { mutableStateOf("Recipes") }
    val channel by viewModel.channel.collectAsState(initial = null)
    val recipes by viewModel.recipes.collectAsState(initial = emptyList())
    val isSubscribed by viewModel.isSubscribed.collectAsState(initial = null)
    val subscriberCount by viewModel.subscriberCount.collectAsState(initial = 0)
    val isLoading by viewModel.isLoading.collectAsState(initial = false)

    val coroutineScope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(channelName) {
        if (!loaded && channelName.isNotBlank()) {
            viewModel.loadAll(channelName, userId)
            loaded = true
        }
    }

    // 1) 로딩 중
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // 2) 로그인 필요
    if (userId.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("로그인이 필요합니다.")
        }
        return
    }

    // 3) 채널 로딩 실패
    if (channel == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("채널 정보를 불러올 수 없습니다.")
        }
        return
    }

    // 4) 실제 UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 4-1) 채널 배너
        AsyncImage(
            model = channel!!.BackgroundResId,
            contentDescription = "채널 배경 이미지",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4-2) 프로필 / 구독 / 관리 버튼
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = channel!!.imageResId,
                contentDescription = "채널 프로필",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(50.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = channel!!.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "$subscriberCount 명 구독 중",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val isMyChannel = (userId == channel!!.owner)
            if (isMyChannel) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { navController.navigate("write/${channel!!.name}") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("레시피 생성", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = { navController.navigate("editChannel/${channel!!.name}") },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("채널 관리", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                when (isSubscribed) {
                    true -> Button(onClick = {
                        coroutineScope.launch {
                            viewModel.toggleSubscription(channel!!.name, userId)
                        }
                    }) {
                        Text("구독중")
                    }
                    false -> Button(onClick = {
                        coroutineScope.launch {
                            viewModel.toggleSubscription(channel!!.name, userId)
                        }
                    }) {
                        Text("구독하기")
                    }
                    null -> OutlinedButton(onClick = {}, enabled = false) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("로딩 중...")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4-3) 탭바
        TabRow(
            selectedTabIndex = if (selectedTab == "Recipes") 0 else 1,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Tab(
                selected = (selectedTab == "Recipes"),
                onClick = { selectedTab = "Recipes" }
            ) { Text("Recipes", modifier = Modifier.padding(8.dp)) }

            Tab(
                selected = (selectedTab == "Liked"),
                onClick = { selectedTab = "Liked" }
            ) { Text("Liked", modifier = Modifier.padding(8.dp)) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4-4) 탭별 콘텐츠: 레시피 그리드
        when (selectedTab) {
            "Recipes" -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recipes, key = { it.id }) { recipe ->
                        RecipeCard(recipe = recipe) {
                            // Compose가 아닌, Android Activity로 전환
                            val intent = Intent(context, RecipeCookingActivity::class.java).apply {
                                putExtra("recipe_id", recipe.id)
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            }
            "Liked" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("좋아요한 레시피는 아직 없습니다.")
                }
            }
        }
    }
}
