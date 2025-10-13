@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class
)

package com.bcu.foodtable.JetpackCompose.Subscribe

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.bcu.foodtable.JetpackCompose.Mypage.CreateChannel.ChannelCreationActivity
import com.bcu.foodtable.JetpackCompose.Subscribe.Channel.ChannelCard
import com.bcu.foodtable.JetpackCompose.coach.*
import com.bcu.foodtable.useful.Channel

/* ──────────────────────────────────────────────────────────────── */
/* Design tokens                                                   */
/* ──────────────────────────────────────────────────────────────── */
private val P_H = 20.dp
private val GAP_V_SECTION = 16.dp
private val CARD_RADIUS = 14.dp
private val GRID_SPACING_H = 16.dp
private val GRID_SPACING_V = 16.dp

/* ──────────────────────────────────────────────────────────────── */
/* Screen                                                          */
/* ──────────────────────────────────────────────────────────────── */
@Composable
fun SubscribeScreen(
    viewModel: SubscribeViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onOverlayActiveChange: (Boolean) -> Unit = {},
    bottomObstructionDp: Dp = 0.dp
) {
    val context = LocalContext.current

    // Data
    val subscribedChannels by viewModel.subscribedChannels.collectAsState()
    val myChannels by viewModel.myChannels.collectAsState()
    val recommendedChannels by viewModel.recommendedChannels.collectAsState()
    val hasChannel by remember(myChannels) { mutableStateOf(myChannels.isNotEmpty()) }

    // Coachmark infra
    val store = remember { CoachmarkStoreDataStore(context) }
    val targets = remember { CoachTargets() }
    var showCoach by remember { mutableStateOf(true) }


    // Scroll
    val listState = rememberLazyListState()

    // Load/refresh
    LaunchedEffect(Unit) { fetchAll(viewModel) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) fetchAll(viewModel)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // UI
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = P_H),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics { contentDescription = "Subscribe screen" }
    ) {
        // Header
        item {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "구독",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Title: 구독" }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "관심 채널의 소식을 가장 먼저 만나보세요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
        }

        /* ── 섹션 1: 내 구독 채널 (2열 FlowRow, 스크롤은 바깥 LazyColumn만) ─ */
        sectionHeader(
            title = "내 구독 채널",
            targets = targets,
            anchorKey = "sub_my",
            expandPx = 10f
        )
        sectionGridContent(
            channels = subscribedChannels,
            navController = navController,
            emptyTitle = "구독한 채널이 없습니다.",
            emptyDesc = "관심 채널을 구독하면 최신 소식을 빠르게 받아볼 수 있어요.",
            emptyAction = null
        )
        sectionSpacer()

        /* ── 섹션 2: 내 채널 (가로 캐러셀 + 만들기 버튼만 유지) ───────────── */
        sectionHeaderWithAction(
            title = "내 채널",
            targets = targets,
            actionAnchorKey = "sub_create",
            expandPx = 12f
        ) {
            AssistChip(
                onClick = { navigateToChannelCreation(context) },
                label = { Text(if (hasChannel) "새 채널" else "채널 만들기") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                modifier = Modifier
                    .coachTarget(
                        id = "sub_create",
                        targets = targets,
                        expandPx = 12f
                    )
                    .semantics { contentDescription = "채널 만들기" }
            )
        }
        sectionCarouselContent(
            channels = myChannels,
            navController = navController,
            emptyTitle = "내가 만든 채널이 없습니다.",
            emptyDesc = "나만의 채널을 만들어 레시피와 소식을 공유해 보세요.",
            emptyAction = {
                Button(
                    onClick = { navigateToChannelCreation(context) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("지금 채널 만들기") }
            }
        )
        sectionSpacer()

        /* ── 섹션 3: 추천 채널 (2열 FlowRow) ─────────────────────────────── */
        sectionHeader(
            title = "추천 채널",
            targets = targets,
            anchorKey = "sub_reco",
            expandPx = 10f
        )
        sectionGridContent(
            channels = recommendedChannels,
            navController = navController,
            emptyTitle = "추천 채널이 없습니다.",
            emptyDesc = "지금은 추천할 채널이 없어요. 잠시 후 다시 확인해 보세요.",
            emptyAction = null
        )

        item { Spacer(Modifier.height(100.dp)) }
    }

    /* ── Coachmark Overlay (유지) ───────────────────────────────── */
    AnimatedVisibility(visible = showCoach) {
        CoachmarkOverlay(
            screen = CoachScreen.SUBSCRIBE,
            steps = listOf(
                CoachStep("sub_my", "내 구독", "구독한 채널의 최신 소식을 한 곳에서 확인하세요."),
                CoachStep("sub_create", "채널 만들기", "나만의 채널을 만들어 레시피를 공유해 보세요.")
                // CoachStep("sub_reco", "추천 채널", "취향에 맞는 채널을 발견해 보세요!", center = true)
            ),
            targets = targets,
            store = store,
            bottomObstructionDp = bottomObstructionDp,
            onClose = {
                showCoach = false
                if (CoachTour.running.value == true && CoachTour.currentScreen.value == CoachScreen.SUBSCRIBE) {
                    CoachTour.next(navController, context, store)
                }
            },
            lazyListState = listState,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(999f),
            onOverlayActiveChange = onOverlayActiveChange,
            scrim  = CoachScrimColor
        )
    }
}

/* ──────────────────────────────────────────────────────────────── */
/* Helpers                                                         */
/* ──────────────────────────────────────────────────────────────── */

private fun navigateToChannelCreation(context: Context) {
    context.startActivity(Intent(context, ChannelCreationActivity::class.java))
}

private fun fetchAll(vm: SubscribeViewModel) {
    vm.fetchSubscribedChannels()
    vm.fetchMyChannels()
    vm.fetchRecommendedChannels()
}

/* ──────────────────────────────────────────────────────────────── */
/* Section builders                                                */
/* ──────────────────────────────────────────────────────────────── */

private fun LazyListScope.sectionHeaderWithAction(
    title: String,
    targets: CoachTargets,
    actionAnchorKey: String? = null,
    expandPx: Float = 0f,
    action: @Composable RowScope.() -> Unit
) {
    item(key = "header_action:$title") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = GAP_V_SECTION),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (actionAnchorKey != null) Modifier.coachTarget(
                            id = actionAnchorKey,
                            targets = targets,
                            expandPx = expandPx
                        ) else Modifier
                    )
                    .semantics { contentDescription = "섹션: $title" }
            )
            action()
        }
    }
}

private fun LazyListScope.sectionHeader(
    title: String,
    targets: CoachTargets,
    anchorKey: String? = null,
    expandPx: Float = 0f
) {
    item(key = anchorKey ?: "header:$title") {
        val headerBringer = remember { BringIntoViewRequester() }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = GAP_V_SECTION)
                .then(
                    if (anchorKey != null) {
                        Modifier
                            .bringIntoViewRequester(headerBringer)
                            .coachTarget(
                                id = anchorKey,
                                targets = targets,
                                bringer = headerBringer,
                                expandPx = expandPx
                            )
                    } else Modifier
                )
                .semantics { contentDescription = "섹션: $title" }
        )
    }
}

/* 구독/추천: 비-스크롤 2열 그리드 (FlowRow 사용 → 중첩 스크롤 문제 없음) */
private fun LazyListScope.sectionGridContent(
    channels: List<Channel>,
    navController: NavHostController,
    emptyTitle: String,
    emptyDesc: String,
    emptyAction: (@Composable () -> Unit)?
) {
    item(key = "grid:${emptyTitle.hashCode()}") {
        AnimatedContent(
            targetState = channels.isNotEmpty(),
            transitionSpec = {
                val dur = 240
                (slideInVertically(
                    animationSpec = tween(dur, easing = FastOutSlowInEasing)
                ) { full -> full / 3 } + fadeIn(tween(dur)))
                    .togetherWith(
                        slideOutVertically(
                            animationSpec = tween(dur, easing = FastOutSlowInEasing)
                        ) { full -> full / 3 } + fadeOut(tween(dur))
                    )
                    .using(SizeTransform(clip = false))
            },
            label = "gridTransition"
        ) { hasData ->
            if (hasData) {
                ChannelGridFlowRow(
                    items = channels,
                    navController = navController
                )
            } else {
                ModernEmptyChannelCard(
                    title = emptyTitle,
                    description = emptyDesc,
                    trailing = emptyAction
                )
            }
        }
    }
}

/* 내 채널: 가로 캐러셀(스냅) */
private fun LazyListScope.sectionCarouselContent(
    channels: List<Channel>,
    navController: NavHostController,
    emptyTitle: String,
    emptyDesc: String,
    emptyAction: (@Composable () -> Unit)?
) {
    item(key = "carousel:${emptyTitle.hashCode()}") {
        AnimatedContent(
            targetState = channels.isNotEmpty(),
            transitionSpec = {
                val dur = 240
                (slideInVertically(
                    animationSpec = tween(dur, easing = FastOutSlowInEasing)
                ) { full -> full / 3 } + fadeIn(tween(dur)))
                    .togetherWith(
                        slideOutVertically(
                            animationSpec = tween(dur, easing = FastOutSlowInEasing)
                        ) { full -> full / 3 } + fadeOut(tween(dur))
                    )
                    .using(SizeTransform(clip = false))
            },
            label = "carouselTransition"
        ) { hasData ->
            if (hasData) {
                HorizontalChannelList(
                    items = channels,
                    navController = navController
                )
            } else {
                ModernEmptyChannelCard(
                    title = emptyTitle,
                    description = emptyDesc,
                    trailing = emptyAction
                )
            }
        }
    }
}

private fun LazyListScope.sectionSpacer() {
    item { Spacer(modifier = Modifier.height(8.dp)) }
}

/* ──────────────────────────────────────────────────────────────── */
/* Lists                                                           */
/* ──────────────────────────────────────────────────────────────── */

/** 2열 그리드 (FlowRow, 비-스크롤) */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChannelGridFlowRow(
    items: List<Channel>,
    navController: NavHostController
) {
    val filtered = remember(items) { items.filter { it.name.isNotBlank() } }
    FlowRow(
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(GRID_SPACING_H),
        verticalArrangement = Arrangement.spacedBy(GRID_SPACING_V),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "channel grid" }
    ) {
        filtered.forEach { channel ->
            // 2열 균등 너비
            Box(modifier = Modifier.weight(1f, fill = true)) {
                ChannelCard(
                    channel = channel,
                    onClick = { navController.navigate("channelView/${channel.name}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "채널 카드: ${channel.name}" }
                )
            }
        }
        // 홀수 정렬 보정
        if (filtered.size % 2 == 1) {
            Spacer(modifier = Modifier.weight(1f, fill = true))
        }
    }
}

/** 가로 캐러셀(스냅) - 내 채널 */
@Composable
fun HorizontalChannelList(
    items: List<Channel>,
    navController: NavHostController
) {
    val tag = "ChannelNavigation"
    val filtered = remember(items) { items.filter { it.name.isNotBlank() } }
    val listState = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(listState)

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        state = listState,
        contentPadding = PaddingValues(horizontal = 2.dp),
        flingBehavior = fling
    ) {
        items(
            items = filtered,
            key = { it.name }
        ) { channel ->
            ChannelCard(
                channel = channel,
                onClick = {
                    Log.d(tag, "navigate: ${channel.name}")
                    navController.navigate("channelView/${channel.name}")
                },
                modifier = Modifier
                    .semantics { contentDescription = "채널 카드: ${channel.name}" }
            )
        }
    }

    LaunchedEffect(filtered) {
        Log.d(tag, "Loaded ${filtered.size} channels")
    }
}

/* ──────────────────────────────────────────────────────────────── */
/* Empty state & small comps                                       */
/* ──────────────────────────────────────────────────────────────── */

@Composable
fun ModernEmptyChannelCard(
    title: String,
    description: String,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(CARD_RADIUS)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (trailing != null) {
                Spacer(Modifier.height(4.dp))
                trailing()
            }
        }
    }
}

@Composable
private fun CountBadge(text: String) {
    // 더 이상 사용하지 않지만, 다른 화면에서 쓸 수도 있어서 남겨둠 (미사용이면 삭제해도 됨)
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clip(CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
