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
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRecipeStorageScreen(
    modifier: Modifier = Modifier,
    viewModel: RecipeGalleryViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
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

    LaunchedEffect(isLoading) {
        // 로딩이 false로 바뀌는 순간 (즉, 로딩이 끝난 순간) 딱 한 번만 실행
        if (!isLoading) {
            // 로딩 인디케이터가 사라지고 그리드가 그려질 미세한 시간을 확보
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
        // 1) 그룹 대표 아이템: 각 그룹에서 가장 오래된(혹은 원하는 기준) 아이템
        val groupReps = groupedItemsMap.values.mapNotNull { items ->
            items.minByOrNull { it.creationTimestamp ?: 0L }
        }
        // 2) 그룹이 없는 순수 개별 아이템
        val nonGrouped = galleryItems.filter { it.groupId.isBlank() }

        // 3) 각각 생성시간 내림차순 정렬
        val sortedGroupReps = groupReps.sortedByDescending { it.creationTimestamp ?: 0L }
        val sortedNonGrouped = nonGrouped.sortedByDescending { it.creationTimestamp ?: 0L }

        // 4) 그룹 → 개별 순으로 합치기
        var combined = sortedGroupReps + sortedNonGrouped

        // 5) 폭발 애니메이션 중인 그룹 아이템은 일단 제외
        explodingGroup?.let { exploding ->
            combined = combined.filterNot { item ->
                exploding.any { it.recipeId == item.recipeId }
            }
        }

        combined
    }


    val processDrop = remember(viewModel, displayList, recipeCardBoundsMap, groupedItemsMap) {
        { sourceItem: GalleryItem, finalDropPosition: Offset ->
            var dropHandled = false
            val groupRepresentativeTargetItems = displayList.filter { it.groupId.isNotBlank() && (groupedItemsMap[it.groupId]?.minByOrNull { g -> g.creationTimestamp ?: 0L }?.recipeId == it.recipeId) && it.recipeId != sourceItem.recipeId }
            for (groupRepTarget in groupRepresentativeTargetItems) {
                val groupBounds = recipeCardBoundsMap[groupRepTarget.recipeId]
                if (groupBounds != null && groupBounds.contains(finalDropPosition)) {
                    if (sourceItem.groupId != groupRepTarget.groupId) { viewModel.addToGroup(groupRepTarget.groupId, sourceItem) }
                    dropHandled = true
                    break
                }
            }
            if (!dropHandled) {
                val individualRecipeTargets = displayList.filter { target -> target.recipeId != sourceItem.recipeId && target.groupId.isBlank() }
                for (targetItem in individualRecipeTargets) {
                    val targetBounds = recipeCardBoundsMap[targetItem.recipeId]
                    if (targetBounds != null && targetBounds.contains(finalDropPosition)) { viewModel.createGroup(sourceItem, targetItem); dropHandled = true; break }
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
                    val gridAlpha by animateFloatAsState(targetValue = if (isAnimatingGroup) 0f else 1f, label = "gridAlpha")

                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = StaggeredGridCells.Adaptive(minSize = 180.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(gridAlpha),
                    ) {
                        itemsIndexed(displayList, key = { _, item -> item.recipeId + (item.groupId.ifBlank { item.recipeId }) }) { index, item ->
                            AnimatedVisibility(
                                visible = isInitialAnimationReady,
                                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 100 * (index % 10))) +
                                        slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(durationMillis = 500, delayMillis = 100 * (index % 10)))
                            ) {
                                val itemModifier = Modifier.onGloballyPositioned { coordinates -> recipeCardBoundsMap[item.recipeId] = coordinates.boundsInWindow() }
                                val isGroupRepresentative = item.groupId.isNotBlank() && (groupedItemsMap[item.groupId]?.minByOrNull { it.creationTimestamp ?: 0L }?.recipeId == item.recipeId)

                                if (isGroupRepresentative) {
                                    CookbookGroupFolderCard(
                                        modifier = itemModifier,
                                        groupName = item.groupName ?: "새로운 그룹",
                                        itemCount = groupedItemsMap[item.groupId]?.size ?: 0,
                                        previewImageUrls = groupedItemsMap[item.groupId]?.take(3)?.mapNotNull { it.image } ?: emptyList(),
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
                                            val intent = Intent(context, RecipeCookingActivity::class.java).apply { putExtra("recipe_id", item.recipeId) }
                                            context.startActivity(intent)
                                        },
                                        onDragStart = { draggingItem = it },
                                        onUpdateDragPosition = { dragPositionInWindow = it },
                                        onDragEnd = { draggingItem?.let { processDrop(it, dragPositionInWindow) } },
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
        AnimatedVisibility(visible = isAnimatingGroup, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            unfoldingGroupId?.let { groupId ->
                val items = groupedItemsMap[groupId] ?: emptyList()
                GroupUnfoldingAnimation(items = items, onAnimationFinished = { showGroupDetailOverlay = groupId })
            }
        }

        AnimatedVisibility(visible = showGroupDetailOverlay != null, enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f), exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.8f)) {
            showGroupDetailOverlay?.let { groupId ->
                val items = groupedItemsMap[groupId] ?: emptyList()
                val groupName = items.firstOrNull()?.groupName ?: "Unnamed Group"
                CookbookGroupDetailOverlay(
                    groupName = groupName,
                    itemsInGroup = items,
                    onDismiss = { showGroupDetailOverlay = null; unfoldingGroupId = null },
                    onItemClick = { item ->
                        val intent = Intent(context, RecipeCookingActivity::class.java).apply { putExtra("recipe_id", item.recipeId) }
                        context.startActivity(intent)
                    },
                    onRenameGroup = { newName -> viewModel.renameGroup(groupId, newName) }
                )
            }
        }

        explodingGroup?.let { items ->
            GroupExplosionAnimation(items = items)
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
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) } // 카드의 좌표 정보를 저장할 변수
    val isDragging = draggingItem?.recipeId == item.recipeId
    val isPotentialDropTarget = !isDragging && draggingItem != null && draggingItem.groupId.isBlank() && item.groupId.isBlank()
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var tapePeeling by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val gestureModifier = modifier
        .onGloballyPositioned { coordinates ->
            layoutCoordinates = coordinates
        }
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
        .pointerInput(isDragging, layoutCoordinates) { // isDragging과 layoutCoordinates가 바뀔 때마다 이 블록을 다시 시작
            if (isDragging) {
                forEachGesture {
                    awaitPointerEventScope {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        drag(down.id) { change ->
                            dragOffset += change.positionChange()
                            // 드래그 중인 포인터의 위치(change.position)를
                            // 실시간으로 화면 전체 좌표로 변환하여 업데이트.
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


    val scale by animateFloatAsState(targetValue = when { isDragging -> 1.1f; isPotentialDropTarget -> 0.95f; else -> 1f }, animationSpec = spring(), label = "cardScale")
    val alpha by animateFloatAsState(targetValue = if (draggingItem != null && !isDragging && !isPotentialDropTarget) 0.5f else 1f, label = "cardAlpha")

    Box(
        modifier = gestureModifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .graphicsLayer { this.scaleX = scale; this.scaleY = scale; shadowElevation = if (isDragging) 24f else 8f; rotationZ = if (isDragging) -5f else (item.recipeId.hashCode() % 10 - 5).toFloat() / 2f }
            .alpha(alpha)
    ) {
        Surface(modifier = Modifier, shape = RoundedCornerShape(8.dp), color = Color(0xFFFFF8E1), shadowElevation = 8.dp, border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))) {
            Column {
                AsyncImage(model = item.image, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(text = item.creationTimestamp?.let { java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }

        AnimatedVisibility(visible = isPotentialDropTarget, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "그룹 만들기", tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        val tapeRotation by animateFloatAsState(targetValue = if (tapePeeling) 20f else 0f, label = "tapePeel")
        TapeDecoration(Modifier.align(Alignment.TopCenter).offset(y = (-10).dp).rotate(tapeRotation))
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

            val x by transition.animateFloat(transitionSpec = { tween(durationMillis = 800, easing = EaseOutQuad) }, label = "explode_x") {
                if (it) distance * kotlin.math.cos(angle.toDouble()).toFloat() else 0f
            }
            val y by transition.animateFloat(transitionSpec = { tween(durationMillis = 800, easing = EaseOutQuad) }, label = "explode_y") {
                if (it) distance * kotlin.math.sin(angle.toDouble()).toFloat() else 0f
            }
            val rotation by transition.animateFloat(transitionSpec = { tween(800) }, label = "explode_rot") { if (it) random.nextFloat() * 720f - 360f else 0f }
            val alpha by transition.animateFloat(transitionSpec = { tween(durationMillis = 800, delayMillis = 200) }, label = "explode_alpha") { if (it) 0f else 1f }

            Surface(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = x
                        translationY = y
                        rotationZ = rotation
                        this.alpha = alpha
                    }
                    .width(180.dp).height(220.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFF8E1),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
            ) {
                AsyncImage(model = item.image, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}



@Composable
fun GroupUnfoldingAnimation(items: List<GalleryItem>, onAnimationFinished: () -> Unit) {
    var animationState by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationState = true
        delay(400 + (items.size * 50L)) // 애니메이션 지속 시간
        onAnimationFinished()
    }

    val transition = updateTransition(targetState = animationState, label = "unfoldingTransition")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        items.forEachIndexed { index, item ->
            val angleOffset = -15f + (30f / (items.size - 1).coerceAtLeast(1) * index)
            val translationYOffset = -20f + (40f / (items.size - 1).coerceAtLeast(1) * index)

            val rotation by transition.animateFloat(transitionSpec = { spring(dampingRatio = 0.6f, stiffness = 100f) }, label = "unfold_rotation_$index") { if (it) angleOffset else 0f }
            val translationY by transition.animateFloat(transitionSpec = { tween(durationMillis = 300, delayMillis = index * 50, easing = EaseOutCubic) }, label = "unfold_translationY_$index") { if (it) translationYOffset else 0f }
            val scale by transition.animateFloat(transitionSpec = { tween(300) }, label = "unfold_scale_$index") { if (it) 1f else 0.8f }
            val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "unfold_alpha_$index") { if (it) 1f else 0f }

            Surface(
                modifier = Modifier
                    .width(180.dp)
                    .height(220.dp)
                    .graphicsLayer { this.rotationZ = rotation; this.translationY = translationY; this.scaleX = scale; this.scaleY = scale; this.alpha = alpha },
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFF8E1),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
            ) {
                AsyncImage(model = item.image, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun CookbookGroupFolderCard(modifier: Modifier, groupName: String, itemCount: Int, previewImageUrls: List<String>, onClick: () -> Unit, onUngroupClick: () -> Unit) {
    var showUngroupDialog by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { showUngroupDialog = true }) },
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.width(180.dp).height(220.dp), contentAlignment = Alignment.Center) {
            previewImageUrls.forEachIndexed { index, url ->
                val rotation = (index - 1) * 7f
                val offset = IntOffset((index - 1) * 4, (index - 1) * 4)
                Surface(
                    modifier = Modifier.offset { offset }.rotate(rotation).width(160.dp).height(200.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF8E1),
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
                ) {
                    AsyncImage(model = url, contentDescription = "Preview ${index + 1}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
        }
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 8.dp).background(Color(0xFFF3EFEA).copy(alpha = 0.8f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, contentDescription = "Group Clip", tint = Color(0xFF795548), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = groupName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
            }
            Text(text = "$itemCount 개 레시피", style = MaterialTheme.typography.bodySmall, color = Color(0xFF795548))
        }
        if (showUngroupDialog) {
            AlertDialog(
                onDismissRequest = { showUngroupDialog = false },
                title = { Text("그룹 해제") }, text = { Text("이 그룹을 해제하시겠습니까?") },
                confirmButton = { TextButton(onClick = { onUngroupClick(); showUngroupDialog = false }) { Text("해제") } },
                dismissButton = { TextButton(onClick = { showUngroupDialog = false }) { Text("취소") } }
            )
        }
    }
}

@Composable
fun CookbookGroupDetailOverlay(groupName: String, itemsInGroup: List<GalleryItem>, onDismiss: () -> Unit, onItemClick: (GalleryItem) -> Unit, onRenameGroup: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isEditingName by remember(groupName) { mutableStateOf(false) }
    var editedName by remember(groupName) { mutableStateOf(groupName) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).pointerInput(Unit) { detectTapGestures(onTap = { onDismiss() }) },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxHeight(0.9f).fillMaxWidth(0.9f).clickable(enabled = false, onClick = {}),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF3EFEA)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isEditingName) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename", tint = Color(0xFF5D4037))
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(value = editedName, onValueChange = { editedName = it }, textStyle = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF5D4037), fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                            IconButton(onClick = { coroutineScope.launch { onRenameGroup(editedName); isEditingName = false } }) { Icon(Icons.Default.Check, contentDescription = "저장", tint = Color(0xFF5D4037)) }
                        } else {
                            Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color(0xFF5D4037))
                            Spacer(Modifier.width(12.dp))
                            Text(groupName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037), modifier = Modifier.weight(1f))
                            IconButton(onClick = { isEditingName = true }) { Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color(0xFF5D4037)) }
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color(0xFF5D4037)) }
                    }
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(160.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(itemsInGroup, key = { "detail_${it.recipeId}" }) { item ->
                            CookbookRecipeCard(item = item, onClick = { onItemClick(item) }, onDragStart = {}, onUpdateDragPosition = {}, onDragEnd = {}, draggingItem = null)
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
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
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

}

@Composable
fun CookbookEmptyState() {

}

@Composable
private fun TapeDecoration(modifier: Modifier = Modifier) {

}

private fun DrawScope.drawScrapbookBackground() {

}