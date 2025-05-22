package com.bcu.foodtable.JetpackCompose.Channel

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bcu.foodtable.JetpackCompose.Channel.ChannelViewModel

import com.bcu.foodtable.useful.Channel


@Composable
fun SubscribeScreen(
    viewModel: SubscribeViewModel,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val subscribedChannels by viewModel.subscribedChannels.collectAsState()
    val myChannels by viewModel.myChannels.collectAsState()
    val recommendedChannels by viewModel.recommendedChannels.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchSubscribedChannels()
        viewModel.fetchMyChannels()
        viewModel.fetchRecommendedChannels()
    }

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        item {
            Spacer(modifier = Modifier.height(80.dp))
            Text("구독한 채널", style = MaterialTheme.typography.titleMedium)
        }

        item {
            if (subscribedChannels.isNotEmpty()) {
                HorizontalChannelList(subscribedChannels, context)
            } else {
                EmptyChannelCard(
                    title = "구독한 채널이 없습니다.",
                    description = "관심 있는 채널을 구독하면 여기에 표시됩니다!"
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("내 채널", style = MaterialTheme.typography.titleMedium)
        }

        item {
            if (myChannels.isNotEmpty()) {
                HorizontalChannelList(myChannels, context)
            } else {
                EmptyChannelCard(
                    title = "내가 만든 채널이 없습니다.",
                    description = "직접 만든 채널은 여기에 표시됩니다."
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("전체 채널", style = MaterialTheme.typography.titleMedium)
        }

        item {
            if (recommendedChannels.isNotEmpty()) {
                HorizontalChannelList(recommendedChannels, context)
            } else {
                EmptyChannelCard(
                    title = "추천 채널이 없습니다.",
                    description = "지금은 추천할 채널이 없습니다."
                )
            }
        }
    }
}
@Composable
fun HorizontalChannelList(
    items: List<Channel>,
    context: android.content.Context
) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        items(items, key = { it.name }) { channel ->
            ChannelCard(channel = channel) {
                val intent = Intent(context, ChannelActivity::class.java).apply {
                    putExtra("channelName", channel.name)
                }
                context.startActivity(intent)
            }
        }
    }
}

@Composable
fun EmptyChannelCard(
    title: String = "구독한 채널이 없습니다.",
    description: String = "관심 있는 채널을 구독하면 여기에 표시됩니다!"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

}

