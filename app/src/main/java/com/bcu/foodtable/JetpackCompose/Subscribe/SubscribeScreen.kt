package com.bcu.foodtable.JetpackCompose.Subscribe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bcu.foodtable.useful.Channel
import kotlin.math.absoluteValue


@Composable
fun SubscribeScreen(
    viewModel: SubscribeViewModel,
    navController: NavHostController,
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. 헤더
        item {
            GalleryHeader(
                title = "구독",
                description = "관심 채널의 소식을 가장 먼저 만나보세요."
            )
        }

        // 2. 내 구독 채널
        item {
            Section(
                title = "내 구독 채널",
                icon = Icons.Default.FavoriteBorder,
                channels = subscribedChannels,
                navController = navController,
                emptyTitle = "구독 중인 채널이 없습니다",
                emptyDescription = "마음에 드는 채널을 구독해보세요."
            )
        }

        // 3. 내가 만든 채널
        item {
            Section(
                title = "내 채널",
                icon = Icons.Default.PersonPin,
                channels = myChannels,
                navController = navController,
                emptyTitle = "아직 채널이 없어요",
                emptyDescription = "나만의 채널을 만들고 레시피를 공유해보세요.",
                showCreateButton = true
            )
        }

        // 4. 추천 채널
        item {
            Section(
                title = "추천 채널",
                icon = Icons.Default.AutoAwesome,
                channels = recommendedChannels,
                navController = navController,
                emptyTitle = "추천 채널을 찾고 있어요",
                emptyDescription = "곧 멋진 채널들을 추천해드릴게요."
            )
        }
    }
}



@Composable
private fun GalleryHeader(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    }
}

@Composable
private fun Section(
    title: String,
    icon: ImageVector,
    channels: List<Channel>,
    navController: NavHostController,
    emptyTitle: String,
    emptyDescription: String,
    showCreateButton: Boolean = false
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { /* 더보기 */ }) {
                Text("더보기")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (channels.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(channels, key = { "channel_${it.name}" }) { channel ->
                    ThemedChannelCard(
                        channel = channel,
                        onClick = { navController.navigate("channelView/${channel.name}") }
                    )
                }
            }
        } else {
            ThemedEmptyCard(
                title = emptyTitle,
                description = emptyDescription,
                showCreateButton = showCreateButton
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemedChannelCard(channel: Channel, onClick: () -> Unit) {
    val cardWidth = 160.dp


    val themeColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
    )

    // 이제 remember 안에서는 계산만 수행합니다.
    val backgroundColor = remember(channel.name) {
        themeColors[channel.name.hashCode().absoluteValue % themeColors.size]
    }

    Card(
        onClick = onClick,
        modifier = Modifier.width(cardWidth),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val channelInitial = channel.name.firstOrNull()?.toString()?.uppercase() ?: "?"

            Surface(
                shape = CircleShape,
                color = backgroundColor, // remember로 계산된 색상 사용
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = channelInitial,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(100..9999).random()}명 구독중",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemedEmptyCard(title: String, description: String, showCreateButton: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (showCreateButton) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(onClick = { /* 채널 만들기 */ }) {
                    Icon(Icons.Default.Add, contentDescription = "만들기", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text("내 채널 만들기")
                }
            }
        }
    }
}