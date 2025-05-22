package com.bcu.foodtable.JetpackCompose.Channel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.RecipeCard
import com.bcu.foodtable.JetpackCompose.Channel.ChannelViewModel

import com.bcu.foodtable.useful.UserManager


@Composable
fun ChannelScreen(
    channelName: String,
    viewModel: ChannelViewModel,
    onRecipeClick: (String) -> Unit,
    onWriteClick: (String) -> Unit
) {
    // uiState 구독
    val uiState by viewModel.uiState.collectAsState()

    val userId = UserManager.getUser()?.uid.orEmpty()

    LaunchedEffect(channelName) {
        viewModel.loadChannel(channelName)
        viewModel.loadRecipes(channelName)
        viewModel.checkSubscription(channelName, userId)
    }

    val channel = uiState.channel
    val recipes = uiState.recipes
    val isSubscribed = uiState.isSubscribed
    val subscriberCount = uiState.subscriberCount

    if (channel != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Background image
            AsyncImage(
                model = channel.BackgroundResId,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Channel info
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = channel.imageResId,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "$subscriberCount 명 구독중",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action button
            Row {
                if (userId == channel.owner) {
                    Button(onClick = { onWriteClick(channel.name) }) {
                        Text("레시피 작성")
                    }
                } else {
                    Button(onClick = {
                        viewModel.toggleSubscription(channel.name, userId)
                    }) {
                        Text(if (isSubscribed) "구독중" else "구독하기")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recipe list
            Text("레시피 목록", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(recipes.filter { it.id.isNotBlank() }) { recipe ->
                    RecipeCard(recipe = recipe) {
                        onRecipeClick(recipe.id)
                    }
                }
            }
        }
    } else if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("채널 정보를 불러올 수 없습니다.")
        }
    }
}