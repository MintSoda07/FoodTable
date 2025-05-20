package com.bcu.foodtable.JetpackCompose.Channel

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.RecipeCard
import com.bcu.foodtable.ui.subscribeNavMenu.ChannelViewModel
import com.bcu.foodtable.useful.UserManager

@Composable
fun ChannelScreen(
    channelName: String,
    viewModel: ChannelViewModel = hiltViewModel(),
    onRecipeClick: (String) -> Unit,
    onWriteClick: (String) -> Unit
) {
    val channel by viewModel.channel.observeAsState()
    val recipes by viewModel.recipes.observeAsState(emptyList())
    val isSubscribed by viewModel.isSubscribed.observeAsState(false)
    val subscriberCount by viewModel.subscriberCount.observeAsState(0)
    val userId = UserManager.getUser()?.uid ?: ""

    LaunchedEffect(channelName) {
        viewModel.loadChannel(channelName)
        viewModel.loadRecipes(channelName)
        viewModel.checkSubscription(channelName, userId)
    }

    channel?.let { ch ->
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            AsyncImage(
                model = ch.BackgroundResId,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ch.imageResId,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(50)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = ch.name, style = MaterialTheme.typography.headlineSmall)
                    Text(text = "$subscriberCount 명 구독중", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                if (userId == ch.owner) {
                    Button(onClick = { onWriteClick(ch.name) }) {
                        Text("레시피 작성")
                    }
                } else {
                    Button(onClick = {
                        viewModel.toggleSubscription(ch.name, userId)
                    }) {
                        Text(if (isSubscribed) "구독중" else "구독하기")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("레시피 목록", style = MaterialTheme.typography.titleMedium)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(recipes.filter { it.id.isNotBlank() }) { recipe ->
                    RecipeCard(recipe = recipe) {
                        onRecipeClick(recipe.id)
                    }
                }
            }
        }
    }
}

