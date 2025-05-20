package com.bcu.foodtable.JetpackCompose.Channel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.ui.subscribeNavMenu.ChannelViewModel
import com.bcu.foodtable.JetpackCompose.RecipeCard

@Composable
fun ChannelScreen(
    channelName: String,
    userId: String,
    viewModel: ChannelViewModel = viewModel()
) {
    val channel by viewModel.channel.observeAsState()
    val recipes by viewModel.recipes.observeAsState(emptyList())
    val isSubscribed by viewModel.isSubscribed.observeAsState(false)
    val subscriberCount by viewModel.subscriberCount.observeAsState(0)
    val isLoading by viewModel.isLoading.observeAsState(false)

    LaunchedEffect(channelName) {
        viewModel.loadChannel(channelName)
        viewModel.loadRecipes(channelName)
        viewModel.checkSubscription(channelName, userId)
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            channel?.let {
                ChannelInfoSection(channel = it, subscriberCount = subscriberCount)
            }

            Button(
                onClick = { viewModel.toggleSubscription(channelName, userId) },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Text(if (isSubscribed) "주소 취소" else "주소하기")
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            LazyColumn {
                items(recipes) { recipe ->
                    RecipeCard(
                        title = recipe.name,
                        description = recipe.description,
                        imageUrl = recipe.imageResId,
                        saltReward = 0,
                        onClick = { /* TODO: Navigate to recipe detail */ }
                    )
                }
            }
        }
    }
}

