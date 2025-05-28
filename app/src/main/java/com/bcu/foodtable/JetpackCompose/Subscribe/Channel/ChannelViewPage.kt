package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.useful.Channel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class ChannelViewPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channelName = intent.getStringExtra("channel_name") ?: ""
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "channelView") {
                    composable("channelView") {
                        ChannelViewPageScreen(
                            channelName = channelName,
                            navController = navController
                        )
                    }
                    composable("recipeView/{id}") { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: ""
                        // TODO: Replace with RecipeViewScreen(id)
                    }
                    composable("write/{channelName}") { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("channelName") ?: ""
                        // TODO: Replace with WriteScreen(name)
                    }
                    composable("editChannel/{channelName}") { backStackEntry ->
                        val name = backStackEntry.arguments?.getString("channelName") ?: ""
                        // TODO: Replace with EditChannelScreen(name)
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelViewPageScreen(
    channelName: String,
    viewModel: ChannelViewModel = viewModel(),
    navController: NavHostController
) {
    val channel: Channel? by viewModel.channel.collectAsState()
    val recipes: List<RecipeItem> by viewModel.recipes.collectAsState(emptyList())
    val subscriberCount: Int by viewModel.subscriberCount.collectAsState(0)
    val isSubscribed: Boolean by viewModel.isSubscribed.collectAsState(false)
    val userId = remember { UserManager.getUser()?.uid ?: "" }

    LaunchedEffect(channelName) {
        viewModel.loadChannel(channelName)
        viewModel.loadRecipes(channelName)
        viewModel.checkSubscription(channelName, userId)
        viewModel.loadSubscriberCount(channelName)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        channel?.let { ch: Channel ->
            val isMyChannel = userId == ch.owner

            AsyncImage(model = ch.BackgroundResId, contentDescription = null, modifier = Modifier.fillMaxWidth().height(200.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                AsyncImage(model = ch.imageResId, contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = ch.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            }
            Text(text = "$subscriberCount 명", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isMyChannel) {
                    Button(onClick = { navController.navigate("write/${ch.name}") }) {
                        Text("글쓰기")
                    }
                    OutlinedButton(onClick = { navController.navigate("editChannel/${ch.name}") }) {
                        Text("채널 편집")
                    }
                } else {
                    Button(onClick = {
                        viewModel.toggleSubscription(ch.name, userId)
                    }) {
                        Text(if (isSubscribed) "구독중" else "구독하기")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize()) {
            items(recipes) { recipe: RecipeItem ->RecipeCard(
                    recipe = recipe,
                    onClick = { navController.navigate("recipeView/${recipe.id}") })
            }
        }
    }
}

@Composable
fun RecipeCard(recipe: RecipeItem, onClick: () -> Unit) {
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
