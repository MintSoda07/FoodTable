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
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bcu.foodtable.JetpackCompose.Mypage.CreateChannel.ChannelCreationActivity
import com.bcu.foodtable.JetpackCompose.coach.*
import com.bcu.foodtable.useful.Channel
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.min

/* ──────────────────────────────────────────────────────────────── */
/* Design tokens                                                   */
/* ──────────────────────────────────────────────────────────────── */
private val P_H = 14.dp                 // 모바일 좌우 여백
private val GAP_V_SECTION = 12.dp       // 섹션 간격 (컴팩트)
private val CARD_RADIUS = 16.dp
private val GRID_SPACING_H = 12.dp
private val GRID_SPACING_V = 12.dp

/* ──────────────────────────────────────────────────────────────── */
/* Lightweight UI utils (아기자기 디테일)                           */
/* ──────────────────────────────────────────────────────────────── */

@Composable
private fun PulseDot(modifier: Modifier = Modifier, color: Color) {
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 0.9f,
            targetValue = 1.15f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun UnderlineReveal(
    progress: Float,
    height: Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val brush = remember {
        Brush.horizontalGradient(
            listOf(
                color.copy(alpha = 0f),
                color,
                color.copy(alpha = 0.7f)
            )
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(1.dp))
            .background(color.copy(alpha = 0.10f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .background(brush)
        )
    }
}

/* 페이드+슬라이드 인 공용 */
@Composable
private fun AppearBlock(
    key: Any,
    delayMs: Int,
    content: @Composable () -> Unit
) {
    var visible by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key1 = key) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(380, easing = LinearOutSlowInEasing)
        ) + fadeIn(tween(320))
    ) { content() }
}

/* ──────────────────────────────────────────────────────────────── */
/* 미니 헤더바(스티키): 검색 + 세그먼트 탭                         */
/* ──────────────────────────────────────────────────────────────── */

private enum class SubTab(val label: String) {
    All("모두"), Subscribed("구독"), Mine("내 채널"), Recommended("추천")
}

@Composable
private fun CuteStickyHeader(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    tab: SubTab,
    onTabChange: (SubTab) -> Unit,
    targets: CoachTargets
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = P_H, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(10.dp)) {
            // 검색
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .coachTarget("sub_search", targets, expandPx = 8f)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                ) { inner ->
                    if (query.text.isBlank()) {
                        Text(
                            "채널 검색…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            }
            Spacer(Modifier.height(10.dp))
            // 세그먼트 탭(아기자기 칩 스타일)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SubTab.values().forEach { t ->
                    val selected = t == tab
                    FilterChip(
                        selected = selected,
                        onClick = { onTabChange(t) },
                        label = {
                            Text(
                                t.label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            if (selected) PulseDot(Modifier.size(6.dp), MaterialTheme.colorScheme.primary)
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}

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

    // 검색/탭 상태
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var tab by remember { mutableStateOf(SubTab.All) }

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

    // 필터링
    fun List<Channel>.filterByQuery(): List<Channel> {
        if (query.text.isBlank()) return this
        val q = query.text.trim().lowercase()
        return this.filter { ch ->
            val name = runCatching { ch.javaClass.getDeclaredField("name").apply { isAccessible = true }.get(ch) as? String }
                .getOrNull()?.lowercase().orEmpty()
            val desc = runCatching { ch.javaClass.getDeclaredField("description").apply { isAccessible = true }.get(ch) as? String }
                .getOrNull()?.lowercase().orEmpty()
            name.contains(q) || desc.contains(q)
        }
    }

    // UI
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .semantics { contentDescription = "Subscribe screen" }
    ) {
        // Sticky mini header
        stickyHeader {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .zIndex(2f)
            ) {
                CuteStickyHeader(
                    query = query,
                    onQueryChange = { query = it },
                    tab = tab,
                    onTabChange = { tab = it },
                    targets = targets
                )
            }
        }

        /* 선택 탭에 따라 섹션 배치 */
        when (tab) {
            SubTab.All -> {
                subscribeSection(subscribedChannels, navController, targets)
                mySection(myChannels, navController, targets, hasChannel, context)
                recoSection(recommendedChannels, navController, targets)
            }
            SubTab.Subscribed -> {
                subscribeSection(subscribedChannels.filterByQuery(), navController, targets)
            }
            SubTab.Mine -> {
                mySection(myChannels.filterByQuery(), navController, targets, hasChannel, context)
            }
            SubTab.Recommended -> {
                recoSection(recommendedChannels.filterByQuery(), navController, targets)
            }
        }

        item { Spacer(Modifier.height(96.dp)) }
    }

    /* Coachmark Overlay */
    AnimatedVisibility(visible = showCoach) {
        CoachmarkOverlay(
            screen = CoachScreen.SUBSCRIBE,
            steps = listOf(
                CoachStep("sub_search", "검색/탭", "원하는 채널을 빠르게 찾고, 탭으로 범위를 좁혀 보세요."),
                CoachStep("sub_create", "채널 만들기", "나만의 채널을 만들어 레시피를 공유해 보세요.")
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
/* Section builders (이름 유지)                                    */
/* ──────────────────────────────────────────────────────────────── */

private fun LazyListScope.subscribeSection(
    data: List<Channel>,
    nav: NavHostController,
    targets: CoachTargets
) {
    sectionHeader(
        title = "내 구독 채널",
        targets = targets,
        anchorKey = "sub_my",
        expandPx = 10f
    )
    sectionGridContent(
        channels = data,
        navController = nav,
        emptyTitle = "구독한 채널이 없습니다.",
        emptyDesc = "관심 채널을 구독하면 최신 소식을 빠르게 받아볼 수 있어요.",
        emptyAction = null
    )
    sectionSpacer()
}

private fun LazyListScope.mySection(
    data: List<Channel>,
    nav: NavHostController,
    targets: CoachTargets,
    hasChannel: Boolean,
    context: Context
) {
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
        channels = data,
        navController = nav,
        emptyTitle = "내가 만든 채널이 없습니다.",
        emptyDesc = "나만의 채널을 만들어 레시피와 소식을 공유해 보세요.",
        emptyAction = {
            Button(onClick = { navigateToChannelCreation(context) }, modifier = Modifier.fillMaxWidth()) {
                Text("지금 채널 만들기")
            }
        }
    )
    sectionSpacer()
}

private fun LazyListScope.recoSection(
    data: List<Channel>,
    nav: NavHostController,
    targets: CoachTargets
) {
    sectionHeader(
        title = "추천 채널",
        targets = targets,
        anchorKey = "sub_reco",
        expandPx = 10f
    )
    sectionGridContent(
        channels = data,
        navController = nav,
        emptyTitle = "추천 채널이 없습니다.",
        emptyDesc = "지금은 추천할 채널이 없어요. 잠시 후 다시 확인해 보세요.",
        emptyAction = null
    )
}

/* 공통 헤더(아기자기 연출) */
private fun LazyListScope.sectionHeaderWithAction(
    title: String,
    targets: CoachTargets,
    actionAnchorKey: String? = null,
    expandPx: Float = 0f,
    action: @Composable RowScope.() -> Unit
) {
    item(key = "header_action:$title") {
        var lineProgress by remember { mutableStateOf(0f) }
        LaunchedEffect(title) { delay(40); lineProgress = 1f }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = GAP_V_SECTION, bottom = GAP_V_SECTION / 2)
        ) {
            AppearBlock(key = "h_$title", delayMs = 40) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulseDot(Modifier.size(6.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(8.dp))
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
            Spacer(Modifier.height(6.dp))
            UnderlineReveal(progress = lineProgress)
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
        var lineProgress by remember { mutableStateOf(0f) }
        LaunchedEffect(title) { delay(20); lineProgress = 1f }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = GAP_V_SECTION, bottom = GAP_V_SECTION / 2)
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
        ) {
            AppearBlock(key = "h_plain_$title", delayMs = 20) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseDot(Modifier.size(6.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "섹션: $title" }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            UnderlineReveal(progress = lineProgress)
        }
    }
}

/* 섹션 공백 */
private fun LazyListScope.sectionSpacer() {
    item { Spacer(modifier = Modifier.height(6.dp)) }
}

/* ──────────────────────────────────────────────────────────────── */
/* 카드 디자인: PrettyChannelCard (구독자 뱃지 심플화 + 사이즈 업)   */
/* ──────────────────────────────────────────────────────────────── */
@Composable
private fun PrettyChannelCard(
    channel: Channel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val name = channel.name.ifBlank { "이름없는 채널" }
    val desc = channel.description
    val bg = channel.backgroundResId
    val avatar = channel.imageResId
    val subs = channel.subscribers

    // 1) 한국식 간단 표기: 1.2만 / 3.4억
    fun formatSubs(n: Int): String {
        if (n >= 100_000_000) { // 억 단위
            val v = n / 100_000_0f
            return String.format("%.1f억", v / 10f)
        }
        if (n >= 10_000) { // 만 단위
            val v = n / 1_000f
            return String.format("%.1f만", v / 10f)
        }
        return n.toString()
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)   // ⬆️ 높이 살짝 키움 (이미지 몰입감)
                .clip(RoundedCornerShape(20.dp))
        ) {
            // 배경 이미지
            if (bg.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(bg).crossfade(true).build(),
                    contentDescription = "$name 배경",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                            )
                        )
                )
            }

            // 상/하단 가독성 오버레이
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.08f),
                            0.45f to Color.Transparent,
                            0.75f to Color.Black.copy(alpha = 0.32f),
                            1f to Color.Black.copy(alpha = 0.52f)
                        )
                    )
            )

            // ✅ 간단 구독자 캡슐 뱃지 (우상단)
            if (subs > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.28f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(999.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = formatSubs(subs),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // 하단: 프로필 + 텍스트(화이트)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                // 프로필 이미지 (흰 링)
                Box(
                    modifier = Modifier
                        .size(60.dp) // ⬆️ 살짝 키움
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(2.dp)
                ) {
                    if (avatar.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(avatar).crossfade(true).build(),
                            contentDescription = "$name 프로필",
                            modifier = Modifier
                                .clip(CircleShape)
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (name.firstOrNull()?.uppercaseChar() ?: 'C').toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1
                    )
                    if (desc.isNotBlank()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}



/* ──────────────────────────────────────────────────────────────── */
/* Lists                                                           */
/* ──────────────────────────────────────────────────────────────── */

/** 2열 그리드 (FlowRow, 비-스크롤) : 이미지 중심 카드 + 스태거 인
 *  - 그리드 특성상 ‘폭’을 직접 키우긴 제한적이므로, 카드 ‘높이/내부요소’를 키워 존재감을 올렸습니다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChannelGridFlowRow(
    items: List<Channel>,
    navController: NavHostController
) {
    val filtered = remember(items) { items.filter { it.name.isNotBlank() } }
    FlowRow(
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .semantics { contentDescription = "channel grid" }
    ) {
        filtered.forEachIndexed { index, channel ->
            Box(modifier = Modifier.weight(1f, fill = true)) {
                key(channel.documentId.ifBlank { channel.name }) {
                    // 스태거 등장
                    var visible by remember(channel.documentId) { mutableStateOf(false) }
                    LaunchedEffect(channel.documentId) { delay(30L * (index % 4)); visible = true }
                    val enter = slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(360, easing = LinearOutSlowInEasing)
                    ) + fadeIn(tween(300))

                    this@FlowRow.AnimatedVisibility(visible = visible, enter = enter) {
                        PrettyChannelCard(
                            channel = channel,
                            onClick = { navController.navigate("channelView/${channel.name}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "채널 카드: ${channel.name}" }
                        )
                    }
                }
            }
        }
        if (filtered.size % 2 == 1) {
            Spacer(modifier = Modifier.weight(1f, fill = true))
        }
    }
}



/** 가로 캐러셀(스냅) - 내 채널
 *  - 카드 폭을 화면 폭 기준으로 유연하게 계산(보기 편한 비율)
 *  - 채널이 많아도 폭이 과도하게 커지거나 작아지지 않도록 범위 제한
 */
@Composable
fun HorizontalChannelList(
    items: List<Channel>,
    navController: NavHostController
) {
    val tag = "ChannelNavigation"
    val filtered = remember(items) { items.filter { it.name.isNotBlank() } }
    val listState = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(listState)

    // 화면 폭 기반 카드 폭 계산 (최소 260dp, 최대 360dp)
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val base = maxWidth * 0.82f           // 화면의 약 82% 폭을 한 카드로
        val cardWidth = when {
            base < 260.dp -> 260.dp
            base > 360.dp -> 360.dp
            else -> base
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            state = listState,
            contentPadding = PaddingValues(horizontal = 6.dp),
            flingBehavior = fling
        ) {
            items(
                items = filtered,
                key = { it.documentId.ifBlank { it.name } }
            ) { channel ->
                // 등장 연출
                var visible by remember(channel.documentId) { mutableStateOf(false) }
                LaunchedEffect(channel.documentId) { delay(30); visible = true }
                val enter = slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(360, easing = LinearOutSlowInEasing)
                ) + fadeIn(tween(320))

                AnimatedVisibility(visible = visible, enter = enter) {
                    // 가운데 근처에서 살짝 스케일 업
                    val firstIndex = listState.firstVisibleItemIndex
                    val centerBias = (filtered.indexOf(channel) - firstIndex).absoluteValue
                    val baseScale = 0.96f
                    val scaleTarget = (1f - min(centerBias * 0.04f, 0.10f)).coerceAtLeast(baseScale)
                    val scale by animateFloatAsState(
                        targetValue = scaleTarget,
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        label = "scale"
                    )

                    Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
                        PrettyChannelCard(
                            channel = channel,
                            onClick = {
                                Log.d(tag, "navigate: ${channel.name}")
                                navController.navigate("channelView/${channel.name}")
                            },
                            modifier = Modifier.width(cardWidth)
                        )
                    }
                }
            }
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
            .padding(horizontal = P_H, vertical = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(CARD_RADIUS)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
/* 구독/추천: 비-스크롤 2열 그리드 (FlowRow 사용 → 중첩 스크롤 문제 없음)
   - 각 카드에 가벼운 등장 애니메이션(스태거) 적용
   - 모바일 폭에서 카드가 ‘답답해 보이지 않게’ 간격 최적화  */
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


/* 내 채널: 가로 캐러셀(스냅) ↔ 2열 그리드 자동 전환
   - items.size >= 8 이면 그리드가 더 가독성 좋아서 그리드로 렌더링
   - 비어있을 때는 기존 빈 카드 사용 그대로 유지
*/
private fun LazyListScope.sectionCarouselContent(
    channels: List<Channel>,
    navController: NavHostController,
    emptyTitle: String,
    emptyDesc: String,
    emptyAction: (@Composable () -> Unit)?
) {
    item(key = "carousel_or_grid:${emptyTitle.hashCode()}") {
        AnimatedContent(
            targetState = when {
                channels.isEmpty() -> "empty"
                channels.size >= 8 -> "grid"
                else -> "carousel"
            },
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
            label = "myChannelsLayoutSwitch"
        ) { state ->
            when (state) {
                "empty" -> {
                    ModernEmptyChannelCard(
                        title = emptyTitle,
                        description = emptyDesc,
                        trailing = emptyAction
                    )
                }
                "grid" -> {
                    // 내 채널이 많을 때: 2열 그리드로 한눈에
                    ChannelGridFlowRow(
                        items = channels,
                        navController = navController
                    )
                }
                else -> {
                    // 소수일 때: 가로 캐러셀로 큼직하게
                    HorizontalChannelList(
                        items = channels,
                        navController = navController
                    )
                }
            }
        }
    }
}
