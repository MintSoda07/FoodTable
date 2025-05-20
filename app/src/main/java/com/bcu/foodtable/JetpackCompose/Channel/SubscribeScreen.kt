package com.bcu.foodtable.JetpackCompose.Channel



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bcu.foodtable.useful.SubscribeItem
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.ui.subscribeNavMenu.SubscribeViewModel

@Composable
fun SubscribeScreen(
    subscribed: List<SubscribeItem>,
    myChannels: List<SubscribeItem>,
    recommended: List<SubscribeItem>,
    navController: NavController,
    userId: String,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("\uD83D\uDD17 구독한 채널", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ChannelRowList(channelList = subscribed, navController = navController, userId = userId)

        Spacer(Modifier.height(16.dp))
        Text("\uD83E\uDDD1\u200D\uD83C\uDF73 내가 만든 채널", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ChannelRowList(channelList = myChannels, navController = navController, userId = userId)

        Spacer(Modifier.height(16.dp))
        Text("\u2728 추천 채널", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ChannelRowList(channelList = recommended, navController = navController, userId = userId)
    }
}

@Composable
fun ChannelRowList(
    channelList: List<SubscribeItem>,  // 변수명 변경
    navController: NavController,
    userId: String
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(channelList) { item ->  // LazyListScope.items() 함수 호출
            ChannelCard(item = item) {
                navController.navigate("channel/${item.name}/$userId")
            }
        }
    }
}
