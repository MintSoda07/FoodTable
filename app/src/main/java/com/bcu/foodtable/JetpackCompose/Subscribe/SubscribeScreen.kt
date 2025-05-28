package com.bcu.foodtable.JetpackCompose.Subscribe

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bcu.foodtable.JetpackCompose.Subscribe.Channel.ChannelCard
import com.bcu.foodtable.useful.Channel

@Composable
fun SubscribeScreen(
    viewModel: SubscribeViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val subscribedChannels by viewModel.subscribedChannels.collectAsState()
    val myChannels by viewModel.myChannels.collectAsState()
    val recommendedChannels by viewModel.recommendedChannels.collectAsState()
    Log.d("SubscribeUI", "UI에서 받은 채널 수: ${myChannels.size}")
    LaunchedEffect(Unit) {
        viewModel.fetchSubscribedChannels()
        viewModel.fetchMyChannels()
        viewModel.fetchRecommendedChannels()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        sectionHeader("구독한 채널")
        sectionContent(subscribedChannels, navController, "구독한 채널이 없습니다.", "관심 있는 채널을 구독하면 여기에 표시됩니다!")

        sectionSpacer()
        sectionHeader("내 채널")
        sectionContent(myChannels, navController, "내가 만든 채널이 없습니다.", "직접 만든 채널은 여기에 표시됩니다.")

        sectionSpacer()
        sectionHeader("전체 채널")
        sectionContent(recommendedChannels, navController, "추천 채널이 없습니다.", "지금은 추천할 채널이 없습니다.")
    }
}

private fun LazyListScope.sectionHeader(title: String) {
    item {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp))
    }
}

private fun LazyListScope.sectionContent(
    channels: List<Channel>,
    navController: NavHostController,
    emptyTitle: String,
    emptyDesc: String
) {
    item {
        if (channels.isNotEmpty()) {
            HorizontalChannelList(channels, navController)
        } else {
            EmptyChannelCard(title = emptyTitle, description = emptyDesc)
        }
    }
}

private fun LazyListScope.sectionSpacer() {
    item {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HorizontalChannelList(
    items: List<Channel>,
    navController: NavHostController
) {
    val tag = "ChannelNavigation"

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        items(items, key = { it.name }) { channel ->
            ChannelCard(channel = channel) {
                Log.d(tag, "Navigating to channel: ${channel.name}")
                navController.navigate("channelView/${channel.name}")
            }
        }
    }

    // 리스트 로딩 로그도 추가 가능
    LaunchedEffect(items) {
        Log.d(tag, "Loaded ${items.size} channels")
    }
}

@Composable
fun EmptyChannelCard(
    title: String,
    description: String
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
