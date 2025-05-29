package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.Channel.WriteScreen
import com.bcu.foodtable.JetpackCompose.Subscribe.RecipeCard
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.useful.Channel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
//
//class ChannelViewPage : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        Log.d("TestLog", "onCreate() 실행됨")
//        val channelName = intent.getStringExtra("channel_name") ?: ""
//        Log.d("NavDebug", "channelName = $channelName, startDestination = channelView/$channelName")
//        setContent {
//            MaterialTheme {
//                val navController = rememberNavController()
//
//                NavHost(navController = navController, startDestination = "channelView") {
//                    composable("channelView/{channelName}") { backStackEntry ->
//                        val name = backStackEntry.arguments?.getString("channelName") ?: ""
//                        ChannelViewPageScreen(channelName = name, navController = navController)
//                    }
//
//                    composable("write/{channelName}") { backStackEntry ->
//                        val name = backStackEntry.arguments?.getString("channelName") ?: ""
//                        WriteScreen(channelName = name) {
//                            navController.popBackStack()
//                        }
//                    }
//
//                    composable("recipeView/{id}") { backStackEntry ->
//                        val id = backStackEntry.arguments?.getString("id") ?: ""
//                        // RecipeViewScreen(id)
//                    }
//
//                    composable("editChannel/{channelName}") { backStackEntry ->
//                        val name = backStackEntry.arguments?.getString("channelName") ?: ""
//                        // EditChannelScreen(name)
//                    }
//                }
//
//
//            }
//        }
//    }
//}
@Composable
fun ChannelViewPageScreen(
    channelName: String,
    navController: NavHostController,
    viewModel: ChannelViewModel = viewModel()
) {
    val db = FirebaseFirestore.getInstance()
    val userId = remember { UserManager.getUser()?.uid ?: "" }

    var selectedTab by remember { mutableStateOf("Recipes") }
    val channel by viewModel.channel.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val subscriberCount by viewModel.subscriberCount.collectAsState(0)
    val isLoading by viewModel.isLoading.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(channelName) {
        Log.d("NavGraphDebug", "routes set up: write/{channelName}, channelView")
        if (!loaded && channelName.isNotBlank()) {
            viewModel.loadAll(channelName, userId)
            loaded = true
        }
    }

    if (userId.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("로그인이 필요합니다.")
        }
        return
    }
//    if (isLoading) {
//        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//            CircularProgressIndicator()
//        }
//        return
//    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. 실제 UI (channel이 null 아닐 때만)
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
                        Text(
                            text = ch.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text("$subscriberCount 명 구독 중", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.weight(1f))

                    if (isMyChannel) {
                        Row {
                            Button(
                                onClick = {
                                    if (ch.name.isNotBlank()) {
                                        navController.navigate("write/${ch.name}")
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text("레시피 생성")
                            }

                            OutlinedButton(
                                onClick = { navController.navigate("editChannel/${ch.name}") }
                            ) {
                                Text("채널 관리")
                            }
                        }
                    } else {
                        // 구독/구독중/로딩중 UI 처리
                        when (isSubscribed) {
                            true -> Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.toggleSubscription(ch.name, userId)
                                    }
                                }
                            ) { Text("구독중") }

                            false -> Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.toggleSubscription(ch.name, userId)
                                    }
                                }
                            ) { Text("구독하기") }

                            null -> OutlinedButton(
                                onClick = {},
                                enabled = false
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("로딩 중...")
                            }
                        }

                    }
                }

                Spacer(Modifier.height(24.dp))

                TabRow(selectedTabIndex = if (selectedTab == "Recipes") 0 else 1) {
                    Tab(
                        selected = selectedTab == "Recipes",
                        onClick = { selectedTab = "Recipes" }
                    ) { Text("Recipes") }

                    Tab(
                        selected = selectedTab == "Liked",
                        onClick = { selectedTab = "Liked" }
                    ) { Text("Liked") }
                }

                Spacer(Modifier.height(16.dp))

                when (selectedTab) {
                    "Recipes" -> {
                        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
                            items(recipes) { recipe ->
                                RecipeCard(recipe = recipe) {
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
        }
        ?: run {
            if (!isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("채널 정보를 불러올 수 없습니다.")
                }
            }
        }

        // 2. 로딩 중일 때 덮기
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

}


@Composable
fun ChannelRecipeCard(recipe: RecipeItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = recipe.imageResId,
                contentDescription = null,
                modifier = Modifier
                    .height(100.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = recipe.name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
