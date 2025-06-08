
package com.bcu.foodtable.JetpackCompose.RecipeStorage
                                        import android.content.Intent
                                        import android.util.Log
                                        import androidx.compose.ui.geometry.Rect
                                        import androidx.compose.animation.*
                                        import androidx.compose.animation.core.*
                                        import androidx.compose.foundation.*
                                        import androidx.compose.foundation.layout.*
                                        import androidx.compose.foundation.lazy.grid.GridCells
                                        import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
                                        import androidx.compose.foundation.lazy.grid.items
                                        import androidx.compose.foundation.shape.CircleShape
                                        import androidx.compose.foundation.shape.RoundedCornerShape
                                        import androidx.compose.material.icons.Icons
                                        import androidx.compose.material.icons.filled.*
                                        import androidx.compose.material3.*
                                        import androidx.compose.runtime.*
                                        import androidx.compose.ui.Alignment
                                        import androidx.compose.ui.Modifier
                                        import androidx.compose.ui.draw.*
                                        import androidx.compose.ui.geometry.Offset
                                        import androidx.compose.ui.graphics.*
                                        import androidx.compose.ui.graphics.drawscope.DrawScope
                                        import androidx.compose.ui.layout.ContentScale
                                        import androidx.compose.ui.platform.LocalContext
                                        import androidx.compose.ui.text.font.FontWeight
                                        import androidx.compose.ui.text.style.TextAlign
                                        import androidx.compose.ui.text.style.TextOverflow
                                        import androidx.compose.ui.unit.Dp
                                        import androidx.compose.ui.unit.dp
                                        import androidx.compose.ui.unit.sp
                                        import androidx.compose.ui.window.Dialog
                                        import androidx.compose.ui.window.DialogProperties
                                        import androidx.lifecycle.viewmodel.compose.viewModel
                                        import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity
                                        import com.bcu.foodtable.useful.GalleryItem
                                        import coil.compose.AsyncImagePainter
                                        import androidx.compose.foundation.Image
                                        import androidx.compose.foundation.gestures.awaitFirstDown
                                        import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
                                        import coil.compose.rememberAsyncImagePainter
                                        import com.bcu.foodtable.JetpackCompose.HomeViewModel
                                        import androidx.compose.foundation.gestures.detectTapGestures
                                        import androidx.compose.foundation.gestures.detectDragGestures
                                        import androidx.compose.foundation.gestures.drag
                                        import androidx.compose.foundation.gestures.forEachGesture
                                        import androidx.compose.foundation.gestures.waitForUpOrCancellation
                                        import androidx.compose.ui.graphics.drawscope.Stroke
                                        import androidx.compose.ui.input.pointer.pointerInput
                                        import androidx.compose.ui.input.pointer.positionChange
                                        import androidx.compose.ui.layout.LayoutCoordinates
                                        import androidx.compose.ui.layout.boundsInWindow
                                        import androidx.compose.ui.layout.onGloballyPositioned
                                        import androidx.compose.ui.unit.IntOffset

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

                                            var draggingItem: GalleryItem? by remember { mutableStateOf(null) }
                                            var dragOffset by remember { mutableStateOf(Offset.Zero) }
                                            var dragPositionInWindow by remember { mutableStateOf(Offset.Zero) }

                                            val recipeCardBoundsMap = remember { mutableStateMapOf<String, Rect>() }

                                            LaunchedEffect(Unit) {
                                                viewModel.loadGalleryItems()
                                                homeViewModel.loadUserInfo()
                                            }

                                            val groupedItemsMap = remember(galleryItems) {
                                                galleryItems
                                                    .filter { it.groupId.isNotBlank() }
                                                    .groupBy { it.groupId }
                                            }

                                            val displayList = remember(galleryItems, groupedItemsMap) {
                                                val groupRepresentativeItems = groupedItemsMap.mapNotNull { (_, items) ->
                                                    items.minByOrNull { it.creationTimestamp ?: 0L }
                                                }
                                                val nonGroupedItems = galleryItems.filter { it.groupId.isBlank() }
                                                (groupRepresentativeItems + nonGroupedItems)
                                                    .sortedByDescending { it.creationTimestamp ?: 0L }
                                            }

                                            var showGroupDetailOverlay by remember { mutableStateOf<String?>(null) }
                                            val itemSize = 180.dp

                                            val processDrop = remember(viewModel, displayList, recipeCardBoundsMap, groupedItemsMap) {
                                                { sourceItem: GalleryItem, finalDropPosition: Offset ->
                                                    var dropHandled = false
                                                    Log.d("ProcessDrop", "Processing drop for ${sourceItem.recipeId} at $finalDropPosition")

                                                    val groupRepresentativeTargetItems = displayList.filter {
                                                        it.groupId.isNotBlank() &&
                                                                (groupedItemsMap[it.groupId]?.minByOrNull { g -> g.creationTimestamp ?: 0L }?.recipeId == it.recipeId) &&
                                                                it.recipeId != sourceItem.recipeId
                                                    }

                                                    for (groupRepTarget in groupRepresentativeTargetItems) {
                                                        val groupBounds = recipeCardBoundsMap[groupRepTarget.recipeId]
                                                        if (groupBounds != null && groupBounds.contains(finalDropPosition)) {
                                                            Log.d("ProcessDrop", "Attempting drop of ${sourceItem.recipeId} onto group ${groupRepTarget.groupId}")
                                                            if (sourceItem.groupId == groupRepTarget.groupId) {
                                                                Log.d("ProcessDrop", "Item ${sourceItem.recipeId} already in group ${groupRepTarget.groupId}. Ignoring.")
                                                            } else {
                                                                viewModel.addToGroup(groupRepTarget.groupId, sourceItem)
                                                                Log.d("ProcessDrop", "Added ${sourceItem.recipeId} to group ${groupRepTarget.groupId}")
                                                            }
                                                            dropHandled = true
                                                            break
                                                        }
                                                    }

                                                    if (!dropHandled) {
                                                        val individualRecipeTargets = displayList.filter { target ->
                                                            target.recipeId != sourceItem.recipeId &&
                                                                    (target.groupId.isBlank() || (groupedItemsMap[target.groupId]?.size ?: 0) == 0 ||
                                                                            (groupedItemsMap[target.groupId]?.size == 1 && groupedItemsMap[target.groupId]?.first()?.recipeId == target.recipeId)
                                                                            ) &&
                                                                    !groupRepresentativeTargetItems.any { grpRep -> grpRep.recipeId == target.recipeId }
                                                        }

                                                        for (targetItem in individualRecipeTargets) {
                                                            val targetBounds = recipeCardBoundsMap[targetItem.recipeId]
                                                            if (targetBounds != null && targetBounds.contains(finalDropPosition)) {
                                                                Log.d("ProcessDrop", "Attempting drop of ${sourceItem.recipeId} onto item ${targetItem.recipeId}")
                                                                val sourceGroupId = sourceItem.groupId.orEmpty()
                                                                val targetGroupId = targetItem.groupId.orEmpty()

                                                                when {
                                                                    sourceGroupId.isBlank() && targetGroupId.isBlank() -> {
                                                                        viewModel.createGroup(sourceItem, targetItem)
                                                                        Log.d("ProcessDrop", "Created group with ${sourceItem.recipeId} and ${targetItem.recipeId}")
                                                                    }
                                                                    sourceGroupId.isNotBlank() && targetGroupId.isBlank() -> {
                                                                        viewModel.createGroup(sourceItem, targetItem)
                                                                        Log.d("ProcessDrop", "Source grouped, Target ungrouped. Creating new group with ${sourceItem.recipeId} and ${targetItem.recipeId}. Source removed from $sourceGroupId.")
                                                                    }
                                                                    else -> {
                                                                        Log.d("ProcessDrop", "Unhandled drop case: source ${sourceItem.recipeId}(${sourceGroupId}) on target ${targetItem.recipeId}(${targetGroupId})")
                                                                    }
                                                                }
                                                                dropHandled = true
                                                                break
                                                            }
                                                        }
                                                    }

                                                    if (dropHandled) {
                                                        Log.d("ProcessDrop", "Drop action handled for ${sourceItem.recipeId}")
                                                    } else {
                                                        Log.d("ProcessDrop", "No valid drop target found for ${sourceItem.recipeId} at $finalDropPosition")
                                                    }

                                                    draggingItem = null
                                                    dragOffset = Offset.Zero
                                                    dragPositionInWindow = Offset.Zero
                                                }
                                            }

                                            Box(
                                                modifier = modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFFF8F9FA))
                                            ) {
                                                // 배경 패턴
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    drawPremiumBackground()
                                                }

                                                when {
                                                    isLoading -> PremiumLoadingState()
                                                    loadFailed -> PremiumErrorState(onRetry = { viewModel.loadGalleryItems() })
                                                    displayList.isEmpty() -> PremiumEmptyState()
                                                    else -> {
                                                        Column(modifier = Modifier.fillMaxSize()) {
                                                            // 프리미엄 헤더
                                                            PremiumHeader(
                                                                totalRecipes = galleryItems.size,
                                                                totalGroups = groupedItemsMap.size
                                                            )

                                                            // 레시피 그리드
                                                            LazyVerticalGrid(
                                                                columns = GridCells.Adaptive(minSize = itemSize),
                                                                contentPadding = PaddingValues(16.dp),
                                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                                modifier = Modifier.fillMaxSize(),
                                                            ) {
                                                                items(displayList, key = { it.recipeId + (it.groupId.ifBlank { it.recipeId }) }) { item ->
                                                                    val itemModifier = Modifier.onGloballyPositioned { coordinates ->
                                                                        recipeCardBoundsMap[item.recipeId] = coordinates.boundsInWindow()
                                                                    }

                                                                    val isGroupRepresentative = item.groupId.isNotBlank() &&
                                                                            (groupedItemsMap[item.groupId]?.minByOrNull { it.creationTimestamp ?: 0L }?.recipeId == item.recipeId) &&
                                                                            (groupedItemsMap[item.groupId]?.size ?: 0) > 0

                                                                    if (isGroupRepresentative) {
                                                                        val itemsInGroup = groupedItemsMap[item.groupId] ?: emptyList()
                                                                        PremiumGroupFolderCard(
                                                                            modifier = itemModifier,
                                                                            groupName = item.groupName ?: item.groupId,
                                                                            representativeImageUrl = itemsInGroup.firstOrNull()?.image,
                                                                            itemCount = itemsInGroup.size,
                                                                            itemSize = itemSize,
                                                                            onClick = {
                                                                                showGroupDetailOverlay = item.groupId
                                                                            },
                                                                            onUngroupClick = {
                                                                                viewModel.ungroup(item.groupId)
                                                                            }
                                                                        )
                                                                    } else {
                                                                        PremiumRecipeCard(
                                                                            item = item,
                                                                            itemSize = itemSize,
                                                                            modifier = itemModifier,
                                                                            onClick = {
                                                                                val intent = Intent(context, RecipeCookingActivity::class.java)
                                                                                intent.putExtra("recipe_id", item.recipeId)
                                                                                context.startActivity(intent)
                                                                            },
                                                                            onDragStart = { startedItem ->
                                                                                draggingItem = startedItem
                                                                                dragOffset = Offset.Zero
                                                                            },
                                                                            onDrag = { offsetDelta ->
                                                                                dragOffset += offsetDelta
                                                                            },
                                                                            onDragEnd = {
                                                                                draggingItem?.let { currentDraggingItem ->
                                                                                    processDrop(currentDraggingItem, dragPositionInWindow)
                                                                                }
                                                                            },
                                                                            draggingItem = draggingItem,
                                                                            dragOffset = dragOffset,
                                                                            onUpdateDragPosition = { newPosition ->
                                                                                dragPositionInWindow = newPosition
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                AnimatedVisibility(
                                                    visible = showGroupDetailOverlay != null,
                                                    enter = fadeIn() + slideInVertically(),
                                                    exit = fadeOut() + slideOutVertically()
                                                ) {
                                                    showGroupDetailOverlay?.let { groupId ->
                                                        val items = galleryItems.filter { it.groupId == groupId }
                                                        val groupName = items.firstOrNull()?.groupName ?: "Unnamed Group"

                                                        PremiumGroupDetailOverlay(
                                                            groupName = groupName,
                                                            itemsInGroup = items,
                                                            onDismiss = { showGroupDetailOverlay = null },
                                                            onItemClick = { item ->
                                                                val intent = Intent(context, RecipeCookingActivity::class.java)
                                                                intent.putExtra("recipe_id", item.recipeId)
                                                                context.startActivity(intent)
                                                                showGroupDetailOverlay = null
                                                            },
                                                            onRenameGroup = { newName ->
                                                                viewModel.renameGroup(groupId, newName)
                                                            },
                                                            gridItemSize = itemSize * 0.9f
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                @Composable
                                fun PremiumHeader(
                                    totalRecipes: Int,
                                    totalGroups: Int
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF6750A4),
                                        shadowElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "나의 레시피 컬렉션",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "구매한 프리미엄 레시피를 관리하세요",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // 레시피 수
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = "$totalRecipes",
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "레시피",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.8f)
                                                    )
                                                }

                                                // 구분선
                                                Box(
                                                    modifier = Modifier
                                                        .width(1.dp)
                                                        .height(40.dp)
                                                        .background(Color.White.copy(alpha = 0.3f))
                                                )

                                                // 그룹 수
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = "$totalGroups",
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "그룹",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.8f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                @Composable
                                fun PremiumLoadingState(modifier: Modifier = Modifier) {
                                    Column(
                                        modifier = modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // 로딩 애니메이션
                                        val infiniteTransition = rememberInfiniteTransition(label = "loading")
                                        val rotation by infiniteTransition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1500, easing = LinearEasing),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "rotation"
                                        )

                                        Box(
                                            modifier = Modifier.size(80.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(modifier = Modifier
                                                .size(80.dp)
                                                .graphicsLayer { rotationZ = rotation }
                                            ) {
                                                drawCircle(
                                                    brush = Brush.sweepGradient(
                                                        colors = listOf(
                                                            Color(0xFF6750A4),
                                                            Color(0xFF9575CD),
                                                            Color(0xFF6750A4)
                                                        )
                                                    ),
                                                    radius = size.minDimension / 2,
                                                    style = Stroke(width = 4.dp.toPx())
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Restaurant,
                                                contentDescription = null,
                                                tint = Color(0xFF6750A4),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        Spacer(Modifier.height(24.dp))
                                        Text(
                                            "레시피를 불러오는 중입니다...",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF6750A4)
                                        )
                                    }
                                }

                                @Composable
                                fun PremiumErrorState(modifier: Modifier = Modifier, onRetry: () -> Unit) {
                                    Column(
                                        modifier = modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFE53935).copy(alpha = 0.1f),
                                            modifier = Modifier.size(100.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudOff,
                                                    contentDescription = "Error Icon",
                                                    modifier = Modifier.size(56.dp),
                                                    tint = Color(0xFFE53935)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(24.dp))
                                        Text(
                                            "연결에 실패했습니다",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color(0xFF1A1A1A),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "네트워크 연결을 확인하고\n다시 시도해주세요",
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center,
                                            color = Color(0xFF666666),
                                            lineHeight = 24.sp
                                        )
                                        Spacer(Modifier.height(32.dp))
                                        Button(
                                            onClick = onRetry,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF6750A4)
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("다시 시도")
                                        }
                                    }
                                }

                                @Composable
                                fun PremiumEmptyState(modifier: Modifier = Modifier) {
                                    Column(
                                        modifier = modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // 애니메이션 아이콘
                                        val infiniteTransition = rememberInfiniteTransition(label = "empty")
                                        val scale by infiniteTransition.animateFloat(
                                            initialValue = 1f,
                                            targetValue = 1.1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1500, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "scale"
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF6750A4).copy(alpha = 0.1f),
                                            modifier = Modifier
                                                .size(120.dp)
                                                .graphicsLayer {
                                                    scaleX = scale
                                                    scaleY = scale
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.BookmarkBorder,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(64.dp),
                                                    tint = Color(0xFF6750A4)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(32.dp))
                                        Text(
                                            "아직 저장된 레시피가 없어요",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color(0xFF1A1A1A),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "마음에 드는 레시피를 구매하고\n나만의 컬렉션을 만들어보세요",
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center,
                                            color = Color(0xFF666666),
                                            lineHeight = 24.sp
                                        )
                                        Spacer(Modifier.height(32.dp))
                                        OutlinedButton(
                                            onClick = { /* 레시피 둘러보기 */ },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.5.dp, Color(0xFF6750A4)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF6750A4)
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("레시피 둘러보기")
                                        }
                                    }
                                }

@Composable
fun PremiumRecipeCard(
    item: GalleryItem,
    itemSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDragStart: (GalleryItem) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    draggingItem: GalleryItem? = null,
    dragOffset: Offset = Offset.Zero,
    onUpdateDragPosition: (Offset) -> Unit = {}
) {
    var isFavorite by remember { mutableStateOf(false) }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var isHovered by remember { mutableStateOf(false) }

    val cardScale by animateFloatAsState(
        targetValue = when {
            draggingItem?.recipeId == item.recipeId -> 1.05f
            isHovered -> 1.02f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    // START OF MODIFICATION
    // launch 오류를 해결하고 탭과 드래그를 올바르게 구분하는 새 제스처 로직입니다.
    val gestureModifier = modifier
        .onGloballyPositioned { coordinates ->
            layoutCoordinates = coordinates
        }
        .pointerInput(item) {
            forEachGesture {
                awaitPointerEventScope {
                    isHovered = true
                    // 사용자가 손가락을 누를 때까지 기다립니다.
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // 사용자가 손가락을 일정 거리 이상 움직이는지(드래그), 아니면 그냥 떼는지(탭) 기다립니다.
                    val change = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                        // 임계값을 넘기 전까지의 움직임은 소비(consume)하여 다른 곳에 영향을 주지 않도록 합니다.
                        change.consume()
                    }

                    if (change != null) {
                        // change가 null이 아니면 사용자가 손가락을 충분히 움직였다는 의미 -> 드래그 시작
                        onDragStart(item)

                        // 기본으로 제공되는 drag 함수를 호출하여 드래그를 처리합니다.
                        drag(change.id) { dragChange ->
                            onDrag(dragChange.positionChange())
                            layoutCoordinates?.let { lc ->
                                val windowPosition = lc.localToWindow(dragChange.position)
                                onUpdateDragPosition(windowPosition)
                            }
                            dragChange.consume()
                        }
                        onDragEnd()
                    } else {
                        // change가 null이면 사용자가 손가락을 움직이기 전에 뗐다는 의미 -> 탭으로 처리
                        onClick()
                    }

                    isHovered = false
                }
            }
        }
    // END OF MODIFICATION

    val currentOffsetModifier = if (draggingItem?.recipeId == item.recipeId) {
        Modifier.offset {
            IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt())
        }
    } else {
        Modifier
    }

    Card(
        modifier = gestureModifier
            .then(currentOffsetModifier)
            .width(itemSize)
            .aspectRatio(0.8f)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = if (draggingItem != null && draggingItem.recipeId != item.recipeId) 0.6f else 1f
                shadowElevation = if (draggingItem?.recipeId == item.recipeId) 16f else 8f
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            hoveredElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        // Card 내부 컨텐츠는 기존과 동일합니다.
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 이미지 영역
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.65f)
                ) {
                    val painter = rememberAsyncImagePainter(
                        model = item.image,
                        contentScale = ContentScale.Crop
                    )
                    var isLoadingImage by remember(item.image) { mutableStateOf(true) }
                    var isErrorImage by remember(item.image) { mutableStateOf(false) }

                    LaunchedEffect(painter) {
                        snapshotFlow { painter.state }.collect { state ->
                            isLoadingImage = state is AsyncImagePainter.State.Loading
                            isErrorImage = state is AsyncImagePainter.State.Error
                        }
                    }

                    Image(
                        painter = painter,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    )

                    if (isErrorImage || item.image.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.4f)
                                    ),
                                    startY = 100f
                                )
                            )
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(36.dp)
                            .clickable { isFavorite = !isFavorite }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFE91E63) else Color(0xFF666666),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF6750A4),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PREMIUM",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isLoadingImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(0xFF6750A4)
                            )
                        }
                    }

                    if (isErrorImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.BrokenImage,
                                contentDescription = null,
                                tint = Color(0xFF9E9E9E),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // 정보 영역
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF1A1A1A),
                        lineHeight = 22.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item.creationTimestamp?.let { timestamp ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = Color(0xFF9E9E9E),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = remember(timestamp) {
                                        java.text.SimpleDateFormat("MM.dd", java.util.Locale.getDefault())
                                            .format(java.util.Date(timestamp))
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }

                        if (item.groupId.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF6750A4).copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color(0xFF6750A4),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "그룹",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6750A4)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isHovered || draggingItem?.recipeId == item.recipeId) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color(0xFF6750A4).copy(alpha = 0.05f),
                            RoundedCornerShape(16.dp)
                        )
                )
            }
        }
    }
}
                                @Composable
                                fun PremiumGroupFolderCard(
                                    groupName: String,
                                    representativeImageUrl: String?,
                                    itemCount: Int,
                                    itemSize: Dp,
                                    onClick: () -> Unit,
                                    modifier: Modifier = Modifier,
                                    onUngroupClick: () -> Unit = {}
                                ) {
                                    var isPressed by remember { mutableStateOf(false) }
                                    var showUngroupDialog by remember { mutableStateOf(false) }

                                    val cardScale by animateFloatAsState(
                                        targetValue = if (isPressed) 0.95f else 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )

                                    Card(
                                        modifier = modifier
                                            .width(itemSize)
                                            .aspectRatio(0.8f)
                                            .graphicsLayer {
                                                scaleX = cardScale
                                                scaleY = cardScale
                                            }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onPress = {
                                                        isPressed = true
                                                        tryAwaitRelease()
                                                        isPressed = false
                                                    },
                                                    onTap = { onClick() },
                                                    onLongPress = { showUngroupDialog = true }
                                                )
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF6750A4)
                                        )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            // 배경 패턴
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawFolderPattern()
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // 상단 아이콘
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color.White.copy(alpha = 0.2f),
                                                    modifier = Modifier.size(56.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Default.Folder,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    }
                                                }

                                                // 미리보기 이미지들
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .weight(1f)
                                                        .padding(vertical = 12.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (representativeImageUrl != null) {
                                                        // 스택 형태로 이미지 표시
                                                        Box {
                                                            // 배경 카드들
                                                            repeat(minOf(3, itemCount)) { index ->
                                                                if (index > 0) {
                                                                    Card(
                                                                        modifier = Modifier
                                                                            .size(width = 80.dp, height = 60.dp)
                                                                            .offset(
                                                                                x = (index * 10).dp,
                                                                                y = (index * 10).dp
                                                                            )
                                                                            .rotate(-5f + (index * 5f)),
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        elevation = CardDefaults.cardElevation(2.dp),
                                                                        colors = CardDefaults.cardColors(
                                                                            containerColor = Color.White.copy(alpha = 0.3f)
                                                                        )
                                                                    ) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .fillMaxSize()
                                                                                .background(Color.White.copy(alpha = 0.5f))
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            // 메인 이미지
                                                            Card(
                                                                modifier = Modifier
                                                                    .size(width = 80.dp, height = 60.dp),
                                                                shape = RoundedCornerShape(8.dp),
                                                                elevation = CardDefaults.cardElevation(4.dp)
                                                            ) {
                                                                Box(modifier = Modifier.fillMaxSize()) {
                                                                    val painter = rememberAsyncImagePainter(
                                                                        model = representativeImageUrl,
                                                                        contentScale = ContentScale.Crop
                                                                    )

                                                                    Image(
                                                                        painter = painter,
                                                                        contentDescription = null,
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.fillMaxSize()
                                                                    )

                                                                    // 이미지 로딩 실패 시 폴백
                                                                    if (painter.state is AsyncImagePainter.State.Error) {
                                                                        Box(
                                                                            modifier = Modifier
                                                                                .fillMaxSize()
                                                                                .background(Color.White.copy(alpha = 0.9f)),
                                                                            contentAlignment = Alignment.Center
                                                                        ) {
                                                                            Icon(
                                                                                Icons.Default.Image,
                                                                                contentDescription = null,
                                                                                tint = Color(0xFF9E9E9E),
                                                                                modifier = Modifier.size(24.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        // 이미지가 없을 때
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Folder,
                                                                contentDescription = null,
                                                                tint = Color.White.copy(alpha = 0.7f),
                                                                modifier = Modifier.size(48.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text(
                                                                text = "$itemCount",
                                                                style = MaterialTheme.typography.titleLarge,
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }

                                                // 그룹 정보
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = groupName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = Color.White.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "$itemCount 레시피",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // 그룹 해제 다이얼로그
                                            if (showUngroupDialog) {
                                                AlertDialog(
                                                    onDismissRequest = { showUngroupDialog = false },
                                                    confirmButton = {
                                                        TextButton(
                                                            onClick = {
                                                                onUngroupClick()
                                                                showUngroupDialog = false
                                                            }
                                                        ) {
                                                            Text("해제하기")
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { showUngroupDialog = false }) {
                                                            Text("취소")
                                                        }
                                                    },
                                                    title = { Text("그룹 해제") },
                                                    text = { Text("이 그룹을 해제하시겠습니까?\n그룹 내 레시피는 개별 항목으로 표시됩니다.") }
                                                )
                                            }
                                        }
                                    }
                                }

                                @Composable
                                fun PremiumGroupDetailOverlay(
                                    groupName: String,
                                    itemsInGroup: List<GalleryItem>,
                                    onDismiss: () -> Unit,
                                    onItemClick: (GalleryItem) -> Unit,
                                    gridItemSize: Dp,
                                    modifier: Modifier = Modifier,
                                    onRenameGroup: (String) -> Unit = {}
                                ) {
                                    var isEditingName by remember(groupName) { mutableStateOf(false) }
                                    var editedName by remember(groupName) { mutableStateOf(groupName) }

                                    Dialog(
                                        onDismissRequest = onDismiss,
                                        properties = DialogProperties(usePlatformDefaultWidth = false)
                                    ) {
                                        Surface(
                                            modifier = modifier
                                                .fillMaxSize()
                                                .padding(24.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            color = Color.White,
                                            tonalElevation = 8.dp
                                        ) {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                // 헤더
                                                Surface(
                                                    color = Color(0xFF6750A4),
                                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(20.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Folder,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(12.dp))

                                                            if (isEditingName) {
                                                                TextField(
                                                                    value = editedName,
                                                                    onValueChange = { editedName = it },
                                                                    textStyle = MaterialTheme.typography.titleLarge.copy(
                                                                        color = Color.White,
                                                                        fontWeight = FontWeight.SemiBold
                                                                    ),
                                                                    singleLine = true,
                                                                    modifier = Modifier.weight(1f),
                                                                    colors = TextFieldDefaults.colors(
                                                                        focusedContainerColor = Color.Transparent,
                                                                        unfocusedContainerColor = Color.Transparent,
                                                                        cursorColor = Color.White,
                                                                        focusedIndicatorColor = Color.White,
                                                                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f)
                                                                    )
                                                                )
                                                                IconButton(
                                                                    onClick = {
                                                                        if (editedName.isNotBlank()) {
                                                                            onRenameGroup(editedName)
                                                                        }
                                                                        isEditingName = false
                                                                    }
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Check,
                                                                        contentDescription = "저장",
                                                                        tint = Color.White
                                                                    )
                                                                }
                                                            } else {
                                                                Text(
                                                                    text = groupName,
                                                                    style = MaterialTheme.typography.titleLarge,
                                                                    color = Color.White,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                IconButton(
                                                                    onClick = {
                                                                        editedName = groupName
                                                                        isEditingName = true
                                                                    }
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Edit,
                                                                        contentDescription = "이름 수정",
                                                                        tint = Color.White
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        IconButton(onClick = onDismiss) {
                                                            Icon(
                                                                Icons.Default.Close,
                                                                contentDescription = "닫기",
                                                                tint = Color.White
                                                            )
                                                        }
                                                    }
                                                }

                                                // 컨텐츠
                                                if (itemsInGroup.isEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(32.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Icon(
                                                                Icons.Default.FolderOpen,
                                                                contentDescription = null,
                                                                tint = Color(0xFF9E9E9E),
                                                                modifier = Modifier.size(64.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(16.dp))
                                                            Text(
                                                                "이 그룹에는 레시피가 없습니다.",
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = Color(0xFF666666)
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    LazyVerticalGrid(
                                                        columns = GridCells.Adaptive(minSize = gridItemSize),
                                                        contentPadding = PaddingValues(20.dp),
                                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        items(itemsInGroup, key = { item -> "detail_${item.recipeId}" }) { item ->
                                                            PremiumRecipeCard(
                                                                item = item,
                                                                itemSize = gridItemSize,
                                                                onClick = { onItemClick(item) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 배경 패턴 그리기 함수들
                                private fun DrawScope.drawPremiumBackground() {
                                    // 부드러운 그라데이션 원
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF6750A4).copy(alpha = 0.05f),
                                                Color.Transparent
                                            ),
                                            radius = size.width * 0.8f
                                        ),
                                        center = Offset(size.width * 0.8f, size.height * 0.2f)
                                    )

                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF9575CD).copy(alpha = 0.05f),
                                                Color.Transparent
                                            ),
                                            radius = size.width * 0.6f
                                        ),
                                        center = Offset(size.width * 0.2f, size.height * 0.7f)
                                    )
                                }

                                private fun DrawScope.drawPremiumPattern() {
                                    val patternSize = 30.dp.toPx()
                                    for (x in 0..size.width.toInt() step patternSize.toInt()) {
                                        for (y in 0..size.height.toInt() step patternSize.toInt()) {
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.1f),
                                                radius = 2.dp.toPx(),
                                                center = Offset(x.toFloat(), y.toFloat())
                                            )
                                        }
                                    }
                                }

                                private fun DrawScope.drawFolderPattern() {
                                    // 폴더 내부 패턴
                                    val lineSpacing = 20.dp.toPx()
                                    for (y in 0..size.height.toInt() step lineSpacing.toInt()) {
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.1f),
                                            start = Offset(0f, y.toFloat()),
                                            end = Offset(size.width, y.toFloat()),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }