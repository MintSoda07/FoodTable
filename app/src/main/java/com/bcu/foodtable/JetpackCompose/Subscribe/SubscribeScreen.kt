@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bcu.foodtable.JetpackCompose.Subscribe

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.bcu.foodtable.JetpackCompose.Mypage.CreateChannel.ChannelCreationActivity
import com.bcu.foodtable.JetpackCompose.Subscribe.Channel.ChannelCard
import com.bcu.foodtable.JetpackCompose.coach.CoachScreen
import com.bcu.foodtable.JetpackCompose.coach.CoachStep
import com.bcu.foodtable.JetpackCompose.coach.CoachTargets
import com.bcu.foodtable.JetpackCompose.coach.CoachmarkOverlay
import com.bcu.foodtable.JetpackCompose.coach.CoachmarkStoreDataStore
import com.bcu.foodtable.JetpackCompose.coach.coachTarget
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

    val context = LocalContext.current
    val hasChannel = remember(myChannels) { myChannels.isNotEmpty() }

    // 코치마크 준비
    val store = remember { CoachmarkStoreDataStore(context) }
    val targets = remember { CoachTargets() }
    var showCoach by remember { mutableStateOf(true) }

    // 코치마크가 화면 밖 타깃을 자동 스크롤하기 위한 requester
    val coachBringer = remember { BringIntoViewRequester() }

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

    fun navigateToChannelCreation(context: Context) {
        val intent = Intent(context, ChannelCreationActivity::class.java)
        context.startActivity(intent)
    }

    // 본문
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .bringIntoViewRequester(coachBringer) // ⬅️ 스크롤 컨테이너에 부착
    ) {
        // 상단 헤더
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "구독",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "관심 채널의 소식을 가장 먼저 만나보세요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(28.dp))
        }

        // 섹션 1: 내 구독 채널 (앵커: sub_my)
        sectionHeader(
            title = "내 구독 채널",
            targets = targets,
            bringer = coachBringer,
            anchorKey = "sub_my",
            expandPx = 10f
        )
        sectionContent(
            channels = subscribedChannels,
            navController = navController,
            emptyTitle = "구독한 채널이 없습니다.",
            emptyDesc = "관심 있는 채널을 구독하면 여기에 표시됩니다!",
            targets = targets
        )
        sectionSpacer()

        // 섹션 2: 내 채널 (액션 칩 앵커: sub_create)
        sectionHeaderWithAction(
            title = "내 채널",
            targets = targets,
            bringer = coachBringer,
            actionAnchorKey = "sub_create",
            expandPx = 12f
        ) {
            AssistChip(
                onClick = { navigateToChannelCreation(context) },
                label = { Text(if (hasChannel) "새 채널" else "채널 만들기") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                modifier = Modifier.coachTarget(
                    id = "sub_create",
                    targets = targets,
                    bringer = coachBringer,
                    expandPx = 12f
                )
            )
        }
        sectionContent(
            channels = myChannels,
            navController = navController,
            emptyTitle = "내가 만든 채널이 없습니다.",
            emptyDesc = "직접 만든 채널은 여기에 표시됩니다.",
            targets = targets
        )
        sectionSpacer()

        // 섹션 3: 추천 채널 (앵커: sub_reco)
        sectionHeader(
            title = "추천 채널",
            targets = targets,
            bringer = coachBringer,
            anchorKey = "sub_reco",
            expandPx = 10f
        )
        sectionContent(
            channels = recommendedChannels,
            navController = navController,
            emptyTitle = "추천 채널이 없습니다.",
            emptyDesc = "지금은 추천할 채널이 없습니다.",
            targets = targets
        )

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }

    if (showCoach) {
        CoachmarkOverlay(
            screen = CoachScreen.SUBSCRIBE,
            steps = listOf(
                CoachStep("sub_my", "내 구독", "구독한 채널의 최신 소식을 한 곳에서 확인하세요."),
                CoachStep("sub_create", "채널 만들기", "나만의 채널을 만들어 레시피를 공유해 보세요."),
                CoachStep("sub_reco", "추천 채널", "취향에 맞는 채널을 발견해 보세요!")
            ),
            targets = targets,
            store = store,
            onClose = { showCoach = false },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(999f) // 항상 최상단
        )
    }
}

/* ──────────────────────────────────────────────────────────────── */
/* LazyListScope helpers — 코치마크 앵커 파라미터(Bringer/expandPx)  */
/* ──────────────────────────────────────────────────────────────── */

private fun LazyListScope.sectionHeaderWithAction(
    title: String,
    targets: CoachTargets,
    bringer: BringIntoViewRequester,
    actionAnchorKey: String? = null,
    expandPx: Float = 0f,
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
            // 오른쪽 액션(필요 시 여기에도 앵커를 둠)
            action()
        }
    }
}

private fun LazyListScope.sectionHeader(
    title: String,
    targets: CoachTargets,
    bringer: BringIntoViewRequester,
    anchorKey: String? = null,
    expandPx: Float = 0f
) {
    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .then(
                    if (anchorKey != null) {
                        Modifier.coachTarget(
                            id = anchorKey,
                            targets = targets,
                            bringer = bringer,
                            expandPx = expandPx
                        )
                    } else Modifier
                )
        )
    }
}

private fun LazyListScope.sectionContent(
    channels: List<Channel>,
    navController: NavHostController,
    emptyTitle: String,
    emptyDesc: String,
    targets: CoachTargets
) {
    item {
        AnimatedContent(
            targetState = channels.isNotEmpty(),
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(
                    slideOutVertically { height -> height } + fadeOut()
                ).using(SizeTransform(clip = false))
            },
            label = "channelListTransition"
        ) { hasChannels ->
            if (hasChannels) {
                HorizontalChannelList(channels, navController)
            } else {
                ModernEmptyChannelCard(title = emptyTitle, description = emptyDesc)
            }
        }
    }
}

private fun LazyListScope.sectionSpacer() {
    item { Spacer(modifier = Modifier.height(32.dp)) }
}

/* ──────────────────────────────────────────────────────────────── */
/* Channels Row & Empty Card                                       */
/* ──────────────────────────────────────────────────────────────── */

@Composable
fun HorizontalChannelList(
    items: List<Channel>,
    navController: NavHostController
) {
    val tag = "ChannelNavigation"
    val filteredItems = items.filter { it.name.isNotBlank() }
    val listState = rememberLazyListState()

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        state = listState
    ) {
        items(filteredItems, key = { it.name }) { channel ->
            ChannelCard(
                channel = channel,
                onClick = {
                    Log.d(tag, "Navigating to channel: ${channel.name}")
                    navController.navigate("channelView/${channel.name}")
                },
                modifier = Modifier.animateItemPlacement(tween(durationMillis = 300))
            )
        }
    }

    LaunchedEffect(items) {
        Log.d(tag, "Loaded ${items.size} channels")
    }
}

@Composable
fun ModernEmptyChannelCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
