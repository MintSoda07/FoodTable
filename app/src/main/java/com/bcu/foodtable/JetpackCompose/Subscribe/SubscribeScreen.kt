package com.bcu.foodtable.JetpackCompose.Subscribe

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi // AnimatedContent를 위해 추가
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background // 배경색 지정을 위해 추가
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState // animateItemPlacement를 위해 추가
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.bcu.foodtable.JetpackCompose.Mypage.CreateChannel.ChannelCreationActivity
import com.bcu.foodtable.JetpackCompose.Subscribe.Channel.ChannelCard
import com.bcu.foodtable.useful.Channel

@OptIn(ExperimentalAnimationApi::class) // AnimatedContent 사용을 위해 필요
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

    val context = LocalContext.current
    val hasChannel = remember(myChannels) { myChannels.isNotEmpty() }
    LaunchedEffect(Unit) {
        viewModel.fetchSubscribedChannels()
        viewModel.fetchMyChannels()
        viewModel.fetchRecommendedChannels()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchSubscribedChannels()
                viewModel.fetchMyChannels()
                viewModel.fetchRecommendedChannels()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    //채널 생성으로 이동 함수
    fun navigateToChannelCreation(context: Context) {
        val intent = Intent(context, ChannelCreationActivity::class.java)
        context.startActivity(intent)
    }

    // LazyColumn 배경색 및 패딩 조정
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // 테마 배경색 적용
            .padding(horizontal = 24.dp) // 좌우 패딩 증가
    ) {
        // 상단 헤더 영역 (기존 코드의 제목과 유사하게 구성)
        item {
            Spacer(modifier = Modifier.height(28.dp)) // 상단 여백
            Text(
                text = "구독", // "구독" 제목은 헤더 영역에서 처리
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "관심 채널의 소식을 가장 먼저 만나보세요.", // 설명 추가
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(28.dp)) // 제목과 첫 섹션 사이 여백
        }

        sectionHeader("내 구독 채널")
        sectionContent(subscribedChannels, navController, "구독한 채널이 없습니다.", "관심 있는 채널을 구독하면 여기에 표시됩니다!")
        sectionSpacer()

        sectionHeaderWithAction("내 채널") {
            AssistChip(
                onClick = { navigateToChannelCreation(context) },
                label = { Text(if (hasChannel) "새 채널" else "채널 만들기") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
        }
        sectionContent(myChannels, navController, "내가 만든 채널이 없습니다.", "직접 만든 채널은 여기에 표시됩니다.")
        sectionSpacer()

        sectionHeader("추천 채널")
        sectionContent(recommendedChannels, navController, "추천 채널이 없습니다.", "지금은 추천할 채널이 없습니다.")

        item {
            Spacer(modifier = Modifier.height(100.dp)) // 하단 패딩
        }
    }
}
private fun LazyListScope.sectionHeaderWithAction(
    title: String,
    action: @Composable RowScope.() -> Unit
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // 오른쪽 끝 액션 자리
            action()
        }
    }
}
private fun LazyListScope.sectionHeader(title: String) {
    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge, // 섹션 제목 스타일 개선
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp) // 수직 패딩 추가
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
private fun LazyListScope.sectionContent(
    channels: List<Channel>,
    navController: NavHostController,
    emptyTitle: String,
    emptyDesc: String
) {
    item {
        // AnimatedContent를 사용하여 채널 목록과 빈 상태 카드 간의 전환 애니메이션 적용
        AnimatedContent(
            targetState = channels.isNotEmpty(),
            transitionSpec = {
                // 새로운 콘텐츠가 들어올 때 아래에서 위로 슬라이드하며 페이드인
                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                    // 이전 콘텐츠가 나갈 때 위에서 아래로 슬라이드하며 페이드아웃
                    slideOutVertically { height -> height } + fadeOut()
                ).using(
                    SizeTransform(clip = false) // 크기 변화 애니메이션을 위해 clip 해제
                )
            },
            label = "channelListTransition"
        ) { hasChannels ->
            if (hasChannels) {
                HorizontalChannelList(channels, navController)
            } else {
                ModernEmptyChannelCard(title = emptyTitle, description = emptyDesc) // 개선된 빈 카드 사용
            }
        }
    }
}


private fun LazyListScope.sectionSpacer() {
    item {
        Spacer(modifier = Modifier.height(32.dp)) // 섹션 사이의 간격 조정
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HorizontalChannelList(
    items: List<Channel>,
    navController: NavHostController
) {
    val tag = "ChannelNavigation"
    val filteredItems = items.filter { it.name.isNotBlank() }

    // LazyListState를 사용하여 스크롤 위치에 따른 애니메이션 제어 가능 (선택 사항)
    val listState = rememberLazyListState()

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        state = listState // LazyListState 적용
    ) {
        items(filteredItems , key = { it.name }) { channel ->
            // animateItemPlacement()를 사용하여 항목 등장 및 이동 애니메이션 적용
            ChannelCard(
                channel = channel,
                onClick = {
                    Log.d(tag, "Navigating to channel: ${channel.name}")
                    navController.navigate("channelView/${channel.name}")
                },
                modifier = Modifier.animateItemPlacement(tween(durationMillis = 300)) // 애니메이션 시간 조정 가능
            )
        }
    }

    LaunchedEffect(items) {
        Log.d(tag, "Loaded ${items.size} channels")
    }
}

@Composable
fun ModernEmptyChannelCard( // 함수명 변경 (디자인 개선을 위해)
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp), // 수직 패딩 조정
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // 테마 색상 활용
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // 미니멀한 그림자
        shape = RoundedCornerShape(12.dp) // 모서리 둥글기 조정
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp), // 내부 패딩 조정
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp) // 내부 요소 간격 조정
        ) {
            Icon(
                imageVector = Icons.Default.Info, // 아이콘 유지
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), // 아이콘 색상 조정
                modifier = Modifier.size(48.dp) // 아이콘 크기 조정
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium, // 제목 스타일 조정
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center // 텍스트 중앙 정렬
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium, // 설명 스타일 조정
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), // 부드러운 색상
                textAlign = TextAlign.Center, // 텍스트 중앙 정렬
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}