package com.bcu.foodtable.JetpackCompose.Channel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bcu.foodtable.R
import com.bcu.foodtable.ui.subscribeNavMenu.SubscribeViewModel

import com.bcu.foodtable.useful.Channel

@Composable
fun SubscribeScreen(
    viewModel: SubscribeViewModel = hiltViewModel(),
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val subscribedChannels by viewModel.subscribedChannels.observeAsState(emptyList())
    val myChannels by viewModel.myChannels.observeAsState(emptyList())
    val recommendedChannels by viewModel.recommendedChannels.observeAsState(emptyList())


    LaunchedEffect(Unit) {
        println("📡 LaunchedEffect: 채널 불러오기 시작")
        viewModel.fetchSubscribedChannels()
        viewModel.fetchMyChannels()
        viewModel.fetchRecommendedChannels()
    }

    println("🧾 현재 구독한 채널 수: ${subscribedChannels.size}")
    println("🧾 현재 내 채널 수: ${myChannels.size}")
    println("🧾 현재 추천 채널 수: ${recommendedChannels.size}")

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // 구독한 채널
        item {
            Spacer(modifier = Modifier.height(80.dp))
            Text("구독한 채널", style = MaterialTheme.typography.titleMedium)
        }

        item {
            if (subscribedChannels.isNotEmpty()) {
                HorizontalChannelList(subscribedChannels, navController)
            } else {
                EmptyChannelCard(
                    title = "구독한 채널이 없습니다.",
                    description = "관심 있는 채널을 구독하면 여기에 표시됩니다!"
                )
            }
            }


        // 내 채널
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("내 채널", style = MaterialTheme.typography.titleMedium)
        }

        item {
            if (myChannels.isNotEmpty()) {
                HorizontalChannelList(myChannels, navController)
            } else {
                EmptyChannelCard(
                    title = "내가 만든 채널이 없습니다.",
                    description = "직접 만든 채널은 여기에 표시됩니다.",
                )
            }
        }

        // 전체 채널
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("전체 채널", style = MaterialTheme.typography.titleMedium)
        }

        item {
            if (recommendedChannels.isNotEmpty()) {
                HorizontalChannelList(recommendedChannels, navController)
            } else {
                EmptyChannelCard(
                    title = "추천 채널이 없습니다.",
                    description = "지금은 추천할 채널이 없습니다.",
                )
            }
        }
    }
}


@Composable

fun HorizontalChannelList(
    items: List<Channel>,
    navController: NavHostController
) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        items(items, key = { it.name }) { channel ->
            ChannelCard(channel = channel) {
                navController.navigate("channel/${channel.name}")
            }
        }
    }
}
@Composable
fun EmptyChannelCard(
    title: String = "구독한 채널이 없습니다.",
    description: String = "관심 있는 채널을 구독하면 여기에 표시됩니다!",
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
