@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bcu.foodtable.JetpackCompose.RecipeStorage

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import com.bcu.foodtable.useful.GalleryItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

// ★ Coachmark imports
import com.bcu.foodtable.JetpackCompose.coach.CoachTargets
import com.bcu.foodtable.JetpackCompose.coach.CoachScreen
import com.bcu.foodtable.JetpackCompose.coach.CoachStep
import com.bcu.foodtable.JetpackCompose.coach.CoachmarkOverlay
import com.bcu.foodtable.JetpackCompose.coach.CoachmarkStoreDataStore
import com.bcu.foodtable.JetpackCompose.coach.coachTarget
import com.bcu.foodtable.JetpackCompose.coach.CoachTour // 투어 사용 시

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.bcu.foodtable.JetpackCompose.coach.CoachScrimColor
import kotlin.math.cos
import kotlin.math.sin

// 로컬 이징
private val EaseOutQuad = Easing { t -> 1f - (1f - t) * (1f - t) }
private val EaseOutCubic = Easing { t -> 1f - (1f - t) * (1f - t) * (1f - t) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRecipeStorageScreen(
    modifier: Modifier = Modifier,
    viewModel: RecipeGalleryViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    navController: NavHostController,
    onOverlayActiveChange: (Boolean) -> Unit = {},
    bottomObstructionDp: Dp = 0.dp
) {
    val context = LocalContext.current
    val galleryItems by viewModel.galleryItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadFailed by viewModel.loadFailed.collectAsState()
    val user by homeViewModel.user.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var draggingItem: GalleryItem? by remember { mutableStateOf(null) }
    var dragPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    val recipeCardBoundsMap = remember { mutableStateMapOf<String, Rect>() }

    var unfoldingGroupId by remember { mutableStateOf<String?>(null) }
    var showGroupDetailOverlay by remember { mutableStateOf<String?>(null) }
    var explodingGroup by remember { mutableStateOf<List<GalleryItem>?>(null) }

    val gridState = rememberLazyStaggeredGridState()
    var isInitialAnimationReady by remember { mutableStateOf(false) }

    // ★ Coachmark store/targets/state
    val store = remember { CoachmarkStoreDataStore(context) }
    val targets = remember { CoachTargets() }
    var showCoach by remember { mutableStateOf(true) }
    // ★ 타깃 자동 스크롤용 requester (그리드에 부착)
    val coachBringer = remember { BringIntoViewRequester() }
    val bottomBarHeight = 0.dp

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            delay(100)
            isInitialAnimationReady = true
        }
    }
    LaunchedEffect(Unit) {
        viewModel.loadGalleryItems()
        homeViewModel.loadUserInfo()
    }

    // 화면에 보여줄 리스트 생성
    val groupedItemsMap = remember(galleryItems) {
        galleryItems
            .filter { it.groupId.isNotBlank() }
            .groupBy { it.groupId }
    }

    val displayList = remember(galleryItems, groupedItemsMap, explodingGroup) {
        val groupReps = groupedItemsMap.values.mapNotNull { items ->
            items.minByOrNull { it.creationTimestamp ?: 0L }
        }
        val nonGrouped = galleryItems.filter { it.groupId.isBlank() }
        val sortedGroupReps = groupReps.sortedByDescending { it.creationTimestamp ?: 0L }
        val sortedNonGrouped = nonGrouped.sortedByDescending { it.creationTimestamp ?: 0L }

        var combined = sortedGroupReps + sortedNonGrouped
        explodingGroup?.let { exploding ->
            combined = combined.filterNot { item ->
                exploding.any { it.recipeId == item.recipeId }
            }
        }
        combined
    }

    // ★ 코치마크 앵커: 첫 "개별 카드", 첫 "그룹 대표" 계산
    val firstCardIndex = remember(displayList) {
        displayList.indexOfFirst { it.groupId.isBlank() }
    }
    val firstGroupIndex = remember(displayList) {
        displayList.indexOfFirst { item ->
            item.groupId.isNotBlank() &&
                    (groupedItemsMap[item.groupId]?.minByOrNull { it.creationTimestamp ?: 0L }?.recipeId == item.recipeId)
        }
    }

    val processDrop = remember(viewModel, displayList, recipeCardBoundsMap, groupedItemsMap) {
        { sourceItem: GalleryItem, finalDropPosition: Offset ->
            var dropHandled = false
            // 그룹 대표로 드롭 → 그룹에 추가
            val groupRepresentativeTargets = displayList.filter {
                it.groupId.isNotBlank() &&
                        (groupedItemsMap[it.groupId]?.minByOrNull { g -> g.creationTimestamp ?: 0L }?.recipeId == it.recipeId) &&
                        it.recipeId != sourceItem.recipeId
            }
            for (groupRepTarget in groupRepresentativeTargets) {
                val groupBounds = recipeCardBoundsMap[groupRepTarget.recipeId]
                if (groupBounds != null && groupBounds.contains(finalDropPosition)) {
                    if (sourceItem.groupId != groupRepTarget.groupId) {
                        viewModel.addToGroup(groupRepTarget.groupId, sourceItem)
                    }
                    dropHandled = true
                    break
                }
            }
            // 카드 ↔ 카드 드롭 → 새 그룹 생성
            if (!dropHandled) {
                val individualTargets = displayList.filter { target ->
                    target.recipeId != sourceItem.recipeId && target.groupId.isBlank()
                }
                for (targetItem in individualTargets) {
                    val targetBounds = recipeCardBoundsMap[targetItem.recipeId]
                    if (targetBounds != null && targetBounds.contains(finalDropPosition)) {
                        viewModel.createGroup(sourceItem, targetItem)
                        dropHandled = true
                        break
                    }
                }
            }
            draggingItem = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3EFEA))
    ) {
        // 가벼운 패럴랙스 배경
        val parallaxOffset by remember { derivedStateOf { gridState.firstVisibleItemScrollOffset * 0.5f } }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.5f)
                .graphicsLayer { translationY = -parallaxOffset }
        ) {
            drawScrapbookBackground()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            CookbookHeader(
                userName = user?.name ?: "나",
                totalRecipes = galleryItems.size,
                totalGroups = groupedItemsMap.size
            )

            when {
                isLoading -> {
                    isInitialAnimationReady = false
                    CookbookLoadingState()
                }
                loadFailed -> CookbookErrorState(onRetry = { viewModel.loadGalleryItems() })
                displayList.isEmpty() && explodingGroup == null -> CookbookEmptyState()
                else -> {
                    val isAnimatingGroup = unfoldingGroupId != null
                    val gridAlpha by animateFloatAsState(
                        targetValue = if (isAnimatingGroup) 0f else 1f,
                        label = "gridAlpha"
                    )

                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = StaggeredGridCells.Adaptive(minSize = 180.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            // ⬇️ 코치마크용 bringer를 그리드에 부착
                            .bringIntoViewRequester(coachBringer)
                            .alpha(gridAlpha),
                    ) {
                        itemsIndexed(
                            displayList,
                            key = { _, item -> item.recipeId + (item.groupId.ifBlank { item.recipeId }) }
                        ) { index, item ->
                            AnimatedVisibility(
                                visible = isInitialAnimationReady,
                                enter = fadeIn(
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        delayMillis = 100 * (index % 10)
                                    )
                                ) + slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = tween(
                                        durationMillis = 500,
                                        delayMillis = 100 * (index % 10)
                                    )
                                )
                            ) {
                                var itemModifier = Modifier.onGloballyPositioned { coordinates ->
                                    recipeCardBoundsMap[item.recipeId] = coordinates.boundsInWindow()
                                }

                                val isGroupRepresentative =
                                    item.groupId.isNotBlank() &&
                                            (groupedItemsMap[item.groupId]?.minByOrNull { it.creationTimestamp ?: 0L }?.recipeId == item.recipeId)

                                // ★ 앵커 부착: 첫 개별 카드 / 첫 그룹 대표
                                if (!isGroupRepresentative && index == firstCardIndex && firstCardIndex >= 0) {
                                    itemModifier = itemModifier.coachTarget(
                                        id = "stor_card",
                                        targets = targets,
                                        bringer = coachBringer,
                                        expandPx = 12f
                                    )
                                }
                                if (isGroupRepresentative && index == firstGroupIndex && firstGroupIndex >= 0) {
                                    itemModifier = itemModifier.coachTarget(
                                        id = "stor_group",
                                        targets = targets,
                                        bringer = coachBringer,
                                        expandPx = 12f
                                    )
                                }

                                if (isGroupRepresentative) {
                                    CookbookGroupFolderCard(
                                        modifier = itemModifier,
                                        groupName = item.groupName ?: "새로운 그룹",
                                        itemCount = groupedItemsMap[item.groupId]?.size ?: 0,
                                        previewImageUrls = groupedItemsMap[item.groupId]
                                            ?.take(3)
                                            ?.mapNotNull { it.image } ?: emptyList(),
                                        onClick = { unfoldingGroupId = item.groupId },
                                        onUngroupClick = {
                                            coroutineScope.launch {
                                                explodingGroup = groupedItemsMap[item.groupId]
                                                delay(800)
                                                viewModel.ungroup(item.groupId)
                                                explodingGroup = null
                                            }
                                        }
                                    )
                                } else {
                                    CookbookRecipeCard(
                                        item = item,
                                        modifier = itemModifier,
                                        onClick = {
                                            val intent = Intent(
                                                context,
                                                RecipeCookingActivity::class.java
                                            ).apply { putExtra("recipe_id", item.recipeId) }
                                            context.startActivity(intent)
                                        },
                                        onDragStart = { draggingItem = it },
                                        onUpdateDragPosition = { dragPositionInWindow = it },
                                        onDragEnd = {
                                            draggingItem?.let {
                                                processDrop(it, dragPositionInWindow)
                                            }
                                        },
                                        draggingItem = draggingItem
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val isAnimatingGroup = unfoldingGroupId != null
        AnimatedVisibility(
            visible = isAnimatingGroup,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            unfoldingGroupId?.let { groupId ->
                val items = groupedItemsMap[groupId] ?: emptyList()
                GroupUnfoldingAnimation(
                    items = items,
                    onAnimationFinished = { showGroupDetailOverlay = groupId }
                )
            }
        }

        AnimatedVisibility(
            visible = showGroupDetailOverlay != null,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.8f)
        ) {
            showGroupDetailOverlay?.let { groupId ->
                val items = groupedItemsMap[groupId] ?: emptyList()
                val groupName = items.firstOrNull()?.groupName ?: "Unnamed Group"
                CookbookGroupDetailOverlay(
                    groupName = groupName,
                    itemsInGroup = items,
                    onDismiss = { showGroupDetailOverlay = null; unfoldingGroupId = null },
                    onItemClick = { item ->
                        val intent = Intent(context, RecipeCookingActivity::class.java)
                            .apply { putExtra("recipe_id", item.recipeId) }
                        context.startActivity(intent)
                    },
                    onRenameGroup = { newName -> viewModel.renameGroup(groupId, newName) }
                )
            }
        }

        explodingGroup?.let { items ->
            GroupExplosionAnimation(items = items)
        }

        // ★ CoachmarkOverlay를 최상단에
        if (showCoach) {
            CoachmarkOverlay(
                screen = CoachScreen.STORAGE,
                steps = listOf(
                    CoachStep("stor_card", "드래그로 그룹", "카드를 다른 카드/폴더에 드롭해서 묶을 수 있어요."),
                    CoachStep("stor_group", "그룹 폴더", "탭하면 펼치고, 길게 눌러 해제할 수 있어요.")
                ),
                targets = targets,
                store = store,
                bottomObstructionDp = bottomObstructionDp,
                onClose = {
                    showCoach = false
                    // 투어를 쓰는 프로젝트라면 다음 스텝으로 진행
                    if (CoachTour.running.value == true && CoachTour.currentScreen.value == CoachScreen.STORAGE) {
                        CoachTour.next(navController, context, store) // 필요 시 navController 넘겨도 됨
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(999f),
                onOverlayActiveChange = onOverlayActiveChange,
                scrim  = CoachScrimColor
            )
        }
    }
}

@Composable
fun CookbookRecipeCard(
    item: GalleryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDragStart: (GalleryItem) -> Unit,
    onUpdateDragPosition: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    draggingItem: GalleryItem?
) {
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val isDragging = draggingItem?.recipeId == item.recipeId
    val isPotentialDropTarget =
        !isDragging && draggingItem != null && draggingItem.groupId.isBlank() && item.groupId.isBlank()
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var tapePeeling by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val gestureModifier = modifier
        .onGloballyPositioned { coordinates -> layoutCoordinates = coordinates }
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    coroutineScope.launch {
                        tapePeeling = true
                        delay(150)
                        onDragStart(item)
                        tapePeeling = false
                    }
                },
                onTap = { onClick() }
            )
        }
        .pointerInput(isDragging, layoutCoordinates) {
            if (isDragging) {
                forEachGesture {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        drag(down.id) { change ->
                            dragOffset += change.positionChange()
                            layoutCoordinates?.let {
                                val screenPosition = it.localToWindow(change.position)
                                onUpdateDragPosition(screenPosition)
                            }
                            change.consume()
                        }
                        onDragEnd()
                        dragOffset = Offset.Zero
                    }
                }
            }
        }

    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.1f
            isPotentialDropTarget -> 0.95f
            else -> 1f
        },
        animationSpec = spring(),
        label = "cardScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (draggingItem != null && !isDragging && !isPotentialDropTarget) 0.5f else 1f,
        label = "cardAlpha"
    )

    Box(
        modifier = gestureModifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                shadowElevation = if (isDragging) 24f else 8f
                rotationZ = if (isDragging) -5f else (item.recipeId.hashCode() % 10 - 5).toFloat() / 2f
            }
            .alpha(alpha)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF8E1),
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
        ) {
            Column {
                AsyncImage(
                    model = item.image,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                )
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.creationTimestamp?.let {
                            java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.getDefault())
                                .format(java.util.Date(it))
                        } ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }

        AnimatedVisibility(visible = isPotentialDropTarget, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = "그룹 만들기",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        val tapeRotation by animateFloatAsState(
            targetValue = if (tapePeeling) 20f else 0f,
            label = "tapePeel"
        )
        TapeDecoration(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-10).dp)
                .rotate(tapeRotation)
        )
    }
}

@Composable
fun GroupExplosionAnimation(items: List<GalleryItem>) {
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animate = true }
    val transition = updateTransition(targetState = animate, label = "explosionTransition")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        items.forEachIndexed { index, item ->
            val random = remember { Random(index) }
            val angle = remember { random.nextFloat() * 360f }
            val distance = remember { 800f + random.nextFloat() * 200f }

            val x by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 800, easing = EaseOutQuad) },
                label = "explode_x"
            ) { if (it) distance * cos(Math.toRadians(angle.toDouble())).toFloat() else 0f }

            val y by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 800, easing = EaseOutQuad) },
                label = "explode_y"
            ) { if (it) distance * sin(Math.toRadians(angle.toDouble())).toFloat() else 0f }

            val rotation by transition.animateFloat(
                transitionSpec = { tween(800) },
                label = "explode_rot"
            ) { if (it) random.nextFloat() * 720f - 360f else 0f }

            val alpha by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 800, delayMillis = 200) },
                label = "explode_alpha"
            ) { if (it) 0f else 1f }

            Surface(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = x
                        translationY = y
                        rotationZ = rotation
                        this.alpha = alpha
                    }
                    .width(180.dp)
                    .height(220.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFF8E1),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
            ) {
                AsyncImage(
                    model = item.image,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun GroupUnfoldingAnimation(items: List<GalleryItem>, onAnimationFinished: () -> Unit) {
    var animationState by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationState = true
        delay(400 + (items.size * 50L))
        onAnimationFinished()
    }
    val transition = updateTransition(targetState = animationState, label = "unfoldingTransition")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        items.forEachIndexed { index, item ->
            val angleOffset = -15f + (30f / (items.size - 1).coerceAtLeast(1) * index)
            val translationYOffset = -20f + (40f / (items.size - 1).coerceAtLeast(1) * index)

            val rotation by transition.animateFloat(
                transitionSpec = { spring(dampingRatio = 0.6f, stiffness = 100f) },
                label = "unfold_rotation_$index"
            ) { if (it) angleOffset else 0f }

            val translationY by transition.animateFloat(
                transitionSpec = { tween(durationMillis = 300, delayMillis = index * 50, easing = EaseOutCubic) },
                label = "unfold_translationY_$index"
            ) { if (it) translationYOffset else 0f }

            val scale by transition.animateFloat(
                transitionSpec = { tween(300) },
                label = "unfold_scale_$index"
            ) { if (it) 1f else 0.8f }

            val alpha by transition.animateFloat(
                transitionSpec = { tween(200) },
                label = "unfold_alpha_$index"
            ) { if (it) 1f else 0f }

            Surface(
                modifier = Modifier
                    .width(180.dp)
                    .height(220.dp)
                    .graphicsLayer {
                        this.rotationZ = rotation
                        this.translationY = translationY
                        this.scaleX = scale
                        this.scaleY = scale
                        this.alpha = alpha
                    },
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFF8E1),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
            ) {
                AsyncImage(
                    model = item.image,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun CookbookGroupFolderCard(
    modifier: Modifier,
    groupName: String,
    itemCount: Int,
    previewImageUrls: List<String>,
    onClick: () -> Unit,
    onUngroupClick: () -> Unit
) {
    var showUngroupDialog by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onClick() }, onLongPress = { showUngroupDialog = true })
        },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.width(180.dp).height(220.dp), contentAlignment = Alignment.Center) {
            previewImageUrls.forEachIndexed { index, url ->
                val rotation = (index - 1) * 7f
                val offset = IntOffset((index - 1) * 4, (index - 1) * 4)
                Surface(
                    modifier = Modifier
                        .offset { offset }
                        .rotate(rotation)
                        .width(160.dp)
                        .height(200.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF8E1),
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Preview ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 8.dp)
                .background(Color(0xFFF3EFEA).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = "Group Clip",
                    tint = Color(0xFF795548),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )
            }
            Text(
                text = "$itemCount 개 레시피",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF795548)
            )
        }
        if (showUngroupDialog) {
            AlertDialog(
                onDismissRequest = { showUngroupDialog = false },
                title = { Text("그룹 해제") },
                text = { Text("이 그룹을 해제하시겠습니까?") },
                confirmButton = {
                    TextButton(onClick = { onUngroupClick(); showUngroupDialog = false }) {
                        Text("해제")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUngroupDialog = false }) { Text("취소") }
                }
            )
        }
    }
}

@Composable
fun CookbookGroupDetailOverlay(
    groupName: String,
    itemsInGroup: List<GalleryItem>,
    onDismiss: () -> Unit,
    onItemClick: (GalleryItem) -> Unit,
    onRenameGroup: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isEditingName by remember(groupName) { mutableStateOf(false) }
    var editedName by remember(groupName) { mutableStateOf(groupName) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .fillMaxWidth(0.9f)
                    .clickable(enabled = false, onClick = {}),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF3EFEA)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditingName) {
                            Icon(
                                Icons.Default.DriveFileRenameOutline,
                                contentDescription = "Rename",
                                tint = Color(0xFF5D4037)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    color = Color(0xFF5D4037),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    onRenameGroup(editedName)
                                    isEditingName = false
                                }
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "저장", tint = Color(0xFF5D4037))
                            }
                        } else {
                            Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color(0xFF5D4037))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                groupName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { isEditingName = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color(0xFF5D4037))
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color(0xFF5D4037))
                        }
                    }

                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(160.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(itemsInGroup, key = { _, it -> "detail_${it.recipeId}" }) { _, item ->
                            CookbookRecipeCard(
                                item = item,
                                onClick = { onItemClick(item) },
                                onDragStart = {},
                                onUpdateDragPosition = {},
                                onDragEnd = {},
                                draggingItem = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CookbookHeader(userName: String, totalRecipes: Int, totalGroups: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "${userName}의", style = MaterialTheme.typography.titleMedium, color = Color(0xFF8D6E63))
            Text(text = "디지털 쿡북", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
        }
        Text(text = "총 ${totalRecipes}개 레시피 | ${totalGroups}개 그룹", style = MaterialTheme.typography.bodySmall, color = Color(0xFF795548))
    }
}

@Composable
fun CookbookLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF795548))
            Spacer(Modifier.height(16.dp))
            Text("나의 쿡북을 펼치는 중...", style = MaterialTheme.typography.titleMedium, color = Color(0xFF795548))
        }
    }
}

@Composable
fun CookbookErrorState(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFF8D6E63), modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("불러오기에 실패했어요.", color = Color(0xFF5D4037))
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry) { Text("다시 시도") }
        }
    }
}

@Composable
fun CookbookEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null, tint = Color(0xFF8D6E63), modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text("저장된 레시피가 없어요.", color = Color(0xFF5D4037))
            Text("레시피 카드에서 ⭐를 눌러 저장해 보세요.", color = Color(0xFF795548), fontSize = 13.sp)
        }
    }
}

@Composable
private fun TapeDecoration(modifier: Modifier = Modifier) {
    // 종이 테이프 느낌 간단 구현
    Canvas(modifier = modifier.size(width = 72.dp, height = 18.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = Color(0xFFFFF176).copy(alpha = 0.85f),
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        // 가장자리 톱니 모양 살짝
        val path = Path()
        path.moveTo(0f, h)
        path.lineTo(w * 0.12f, h - 3f)
        path.lineTo(w * 0.24f, h)
        path.lineTo(w * 0.36f, h - 3f)
        path.lineTo(w * 0.48f, h)
        path.lineTo(w * 0.60f, h - 3f)
        path.lineTo(w * 0.72f, h)
        path.lineTo(w * 0.84f, h - 3f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        drawPath(path, Color(0xFFFFEE58))
    }
}

private fun DrawScope.drawScrapbookBackground() {
    // 따뜻한 페이퍼 텍스처 느낌의 줄무늬
    val stripeColor = Color(0xFFBCAAA4).copy(alpha = 0.08f)
    val gap = 24f
    var y = 0f
    while (y < size.height) {
        drawRect(
            color = stripeColor,
            topLeft = Offset(0f, y),
            size = androidx.compose.ui.geometry.Size(size.width, 2f)
        )
        y += gap
    }
}

/* -----------------------------------------
   ViewModel 스텁: 실제 프로젝트의 ViewModel을 사용하세요.
   이 파일만 테스트하려면 아래 스텁 유지
   ----------------------------------------- */
// class RecipeGalleryViewModel : ViewModel() { ... }  // 실제 구현 사용
