package com.bcu.foodtable.JetpackCompose.RecipeStorage

import android.content.Intent
import android.util.Log
import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // itemsIndexed 대신 items 사용
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // For state observation
import androidx.compose.runtime.mutableStateOf // For state management
import androidx.compose.runtime.setValue // For state management
import androidx.compose.runtime.snapshotFlow // For state observation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity // 변경된 import
import com.bcu.foodtable.useful.GalleryItem // 사용자 경로
import androidx.compose.foundation.layout.Box
import coil.compose.AsyncImagePainter // AsyncImage의 상태를 사용하기 위해 필요
import androidx.compose.foundation.Image // Explicit import for Image composable
import coil.compose.rememberAsyncImagePainter // Add this import
// Import navigation components from HomeScreen
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.positionInWindow

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
    // 선택된 아이템과 드래그 중인 위치 상태
    var draggingItem: GalleryItem? by remember { mutableStateOf(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val recipeCardBoundsMap = remember { mutableStateMapOf<String, Rect>() }

    var dragPositionInWindow by remember { mutableStateOf(Offset.Zero) }

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
        // 그룹당 대표 아이템 1개만 선택 (recipeId가 가장 작은 것 기준)
        val groupRepresentativeItems = groupedItemsMap.mapNotNull { (_, items) ->
            items.minByOrNull { it.recipeId } // 또는 createdTimestamp도 가능
        }

        // 그룹이 없는 아이템
        val nonGroupedItems = galleryItems.filter { it.groupId.isBlank() }

        // 그룹 대표 + 그룹 없는 것들을 하나로 묶고 정렬
        (groupRepresentativeItems + nonGroupedItems)
            .sortedByDescending { it.creationTimestamp ?: 0L }
    }
    LaunchedEffect(displayList) {
        Log.d("DisplayList", displayList.joinToString("\n") { it.recipeId + " / " + it.groupId })
    }


    var showGroupDetailOverlay by remember { mutableStateOf<String?>(null) }

    val itemSize = 180.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when {
            isLoading -> LoadingState()
            loadFailed -> ErrorState(onRetry = { viewModel.loadGalleryItems() })
            displayList.isEmpty() -> EmptyState()
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = itemSize),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),

                ) {
                    items(displayList, key = { it.recipeId + it.groupId }) { item ->

                        val isGroup = item.groupId.isNotBlank() &&
                                (groupedItemsMap[item.groupId]?.size ?: 0) > 0

                        if (isGroup) {
                            val itemsInGroup = groupedItemsMap[item.groupId] ?: emptyList()
                            val representativeItem = itemsInGroup.firstOrNull { it.recipeId == item.recipeId }
                            if (representativeItem != null) {
                                StyledGroupFolderItemCard(
                                    groupName = representativeItem.groupName ?: representativeItem.groupId,
                                    representativeImageUrl = itemsInGroup.firstOrNull()?.image,
                                    itemCount = itemsInGroup.size,
                                    itemSize = itemSize,
                                    dragOffset = dragOffset,

                                    onClick = {
                                        showGroupDetailOverlay = representativeItem.groupId  //  groupId만 저장
                                    },
                                    draggingItem = draggingItem,
                                    dragPositionInWindow = dragPositionInWindow,
                                    onDrop = { droppedItem ->
                                        draggingItem?.let { source ->
                                            // 자기 자신이면 무시
                                            if (source.recipeId == representativeItem.recipeId) return@let

                                            // 이미 같은 그룹이면 무시
                                            if (!source.groupId.isNullOrBlank() && source.groupId == representativeItem.groupId) return@let

                                            // 그룹 외부에서 온 항목만 추가 허용
                                            if (source.groupId.isNullOrBlank()) {
                                                viewModel.addToGroup(representativeItem.groupId ?: return@let, source)
                                            } else {
                                                Log.d("DropDebug", " 이미 다른 그룹 소속이라 무시됨: ${source.recipeId}")
                                            }

                                            draggingItem = null
                                            dragOffset = Offset.Zero
                                        }
                                    }
                                    ,
                                            onUngroupClick = {
                                        viewModel.ungroup(representativeItem.groupId ?: "")
                                    }

                                )
                            }
                        } else {
                            StyledRecipeItemCard(
                                item = item,
                                itemSize = itemSize,
                                modifier = Modifier.onGloballyPositioned {
                                    recipeCardBoundsMap[item.recipeId] = it.boundsInWindow()
                                },
                                onClick = {
                                    val intent = Intent(context, RecipeCookingActivity::class.java)
                                    intent.putExtra("recipe_id", item.recipeId)
                                    context.startActivity(intent)
                                },
                                onDragStart = { draggingItem = it },
                                onDrag = { offset ->
                                    Log.d("Offset", "Dragging offset: $offset")
                                    dragOffset += offset },
                                onDragEnd = {
                                    draggingItem = null
                                    dragOffset = Offset.Zero
                                    dragPositionInWindow = Offset.Zero // <- 이거 중요
                                },
                                draggingItem = draggingItem,
                                dragOffset = dragOffset,
                                dragPositionInWindow = dragPositionInWindow,
                                onUpdateDragPosition = { pos ->
                                    dragPositionInWindow = pos
                                }
                            )
                        }
                    }



                }
                LaunchedEffect(draggingItem, dragPositionInWindow) {
                    val source = draggingItem ?: return@LaunchedEffect

                    for (target in displayList) {
                        if (target.recipeId == source.recipeId) continue
                        val bounds = recipeCardBoundsMap[target.recipeId] ?: continue

                        if (bounds.contains(dragPositionInWindow)) {
                            Log.d("DropDebug", " 충돌 감지 → ${source.recipeId} vs ${target.recipeId}")

                            val sourceGroupId = source.groupId.orEmpty()
                            val targetGroupId = target.groupId.orEmpty()

                            when {
                                sourceGroupId.isBlank() && targetGroupId.isBlank() -> {
                                    viewModel.createGroup(source, target)
                                }
                                sourceGroupId.isBlank() && targetGroupId.isNotBlank() -> {
                                    viewModel.addToGroup(targetGroupId, source)
                                }
                                sourceGroupId != targetGroupId && targetGroupId.isNotBlank() -> {
                                    viewModel.addToGroup(targetGroupId, source)
                                }
                            }

                            break
                        }
                    }
                }











            }
        }

        // 그룹 상세 보기 오버레이
        AnimatedVisibility(
            visible = showGroupDetailOverlay != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            showGroupDetailOverlay?.let { groupId ->
                val items = galleryItems.filter { it.groupId == groupId }
                val groupName = items.firstOrNull()?.groupName ?: "Unnamed"

                StyledGroupDetailOverlay(
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
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "레시피를 불러오는 중입니다...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorState(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "Error Icon",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "이런, 문제가 발생했어요!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "데이터를 불러오는데 실패했습니다. 네트워크 연결을 확인하거나 잠시 후 다시 시도해주세요.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Retry Icon", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("다시 시도", color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.NoFood, // 또는 Icons.Filled.Kitchen, Icons.Filled.MenuBook
            contentDescription = "Empty Icon",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "텅 비었어요!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "저장된 레시피가 아직 없네요. 맛있는 첫 레시피를 추가해보세요!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // 여기에 레시피 추가 버튼을 넣을 수도 있습니다.
        // Spacer(Modifier.height(32.dp))
        // Button(onClick = { /* 레시피 추가 화면으로 이동 */ }) {
        //     Icon(Icons.Filled.Add, contentDescription = "Add Recipe")
        //     Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        //     Text("레시피 추가하기")
        // }
    }
}

@Composable
fun StyledRecipeItemCard(
    item: GalleryItem,
    itemSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDragStart: (GalleryItem) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    draggingItem: GalleryItem? = null,
    dragOffset: Offset = Offset.Zero,
    dragPositionInWindow: Offset = Offset.Zero,
    onUpdateDragPosition: (Offset) -> Unit = {}
) {
    var isFavorite by remember { mutableStateOf(false) }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var cardBoundsInWindow by remember { mutableStateOf<Rect?>(null) }
    // 애니메이션 효과 추가
    val cardScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    val dragModifier = Modifier
        .onGloballyPositioned {
            layoutCoordinates = it
            cardBoundsInWindow = it.boundsInWindow()

        }
        .pointerInput(item) {
            detectDragGestures(
                onDragStart = {
                    onDragStart(item)

                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)

                    layoutCoordinates?.let { coords ->
                        val dragPosInWindow = coords.localToWindow(change.position)
                        onUpdateDragPosition(dragPosInWindow)
                        Log.d("Offset", "Dragging drawPosInWindow: $dragPosInWindow")
                    }
                }
                ,
                onDragEnd = {
                    onDragEnd()
                }
            )
        }
    val offsetModifier = if (draggingItem?.recipeId == item.recipeId) {
        Modifier.offset {
            IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt())
        }
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .then(dragModifier)
            .then(offsetModifier)
            .width(itemSize)
            .aspectRatio(0.75f) // 높이를 조금 더 늘려서 정보 공간 확보
            .graphicsLayer(
                scaleX = cardScale,
                scaleY = cardScale
            )
            .clickable(onClick = onClick)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp, 
            pressedElevation = 12.dp, 
            hoveredElevation = 10.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 이미지 섹션
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f) // 이미지가 카드의 70% 차지
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                // Observe painter state properly
                val painter = rememberAsyncImagePainter(model = item.image)
                var isLoading by remember { mutableStateOf(true) }
                var isError by remember { mutableStateOf(false) }

                // Observe painter state changes
                LaunchedEffect(painter) {
                    snapshotFlow { painter.state }.collect { state ->
                        isLoading = state is AsyncImagePainter.State.Loading
                        isError = state is AsyncImagePainter.State.Error
                    }
                }


                // Always show the image
                Image(
                    painter = painter,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 그라데이션 오버레이 추가 (텍스트 가독성 향상)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )

                // 좋아요 버튼 (우상단)
                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFE91E63) else Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .shadow(4.dp, CircleShape)
                    )
                }

                // Show loading overlay
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(itemSize / 4),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Show error overlay
                if (isError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Restaurant,
                                contentDescription = "Image failed to load",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(itemSize / 3)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "이미지 로드 실패",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 정보 섹션
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f) // 정보가 카드의 30% 차지
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 레시피 이름
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // 추가 정보 (생성 시간, 그룹 정보 등)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 카테고리/그룹 정보
                    if (item.groupId.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "그룹",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 시간 정보 (생성 시간이 있다면)
                    item.creationTimestamp?.let { timestamp ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Time",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = remember(timestamp) {
                                    // 간단한 시간 포맷팅 (실제로는 더 정교한 포맷팅 필요)
                                    java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
                                        .format(java.util.Date(timestamp))
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StyledGroupFolderItemCard(
    groupName: String,
    representativeImageUrl: String?,
    itemCount: Int,
    itemSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    draggingItem: GalleryItem? = null,
    dragPositionInWindow: Offset = Offset.Zero,
    dragOffset: Offset = Offset.Zero,
    onDrop: (GalleryItem) -> Unit = {},
    onUngroupClick: () -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }
    var cardBoundsInWindow by remember { mutableStateOf<Rect?>(null) }
    var showUngroupDialog by remember { mutableStateOf(false) }
    val modifierWithPosition = modifier.onGloballyPositioned { layoutCoordinates ->
        val bounds = layoutCoordinates.boundsInWindow()
        Log.d("Bounds", " onGloballyPositioned called, bounds = $bounds")
        cardBoundsInWindow = bounds
        Log.d("DropDebug", " CardBounds: $bounds")
    }

    // 애니메이션 효과
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "groupCardScale"
    )

    val cardRotation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "groupCardRotation"
    )

    Card(
        modifier = modifierWithPosition
            .width(itemSize)
            .aspectRatio(0.75f)
            .graphicsLayer(
                scaleX = cardScale,
                scaleY = cardScale,
                rotationZ = cardRotation
            )
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
            }
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 16.dp,
            hoveredElevation = 12.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showUngroupDialog) {
                AlertDialog(
                    onDismissRequest = { showUngroupDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            onUngroupClick() //  전달받은 함수 실행
                            showUngroupDialog = false
                        }) { Text("해제하기") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUngroupDialog = false }) { Text("취소") }
                    },
                    title = { Text("그룹 해제") },
                    text = { Text("이 그룹을 해제하시겠습니까?") }
                )
            }
            // 배경 그라데이션
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            ),
                            radius = itemSize.value * 1.5f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 상단 아이콘 영역
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (representativeImageUrl != null) {
                        // Observe painter state properly
                        val painter = rememberAsyncImagePainter(model = representativeImageUrl)
                        var isLoading by remember { mutableStateOf(true) }
                        var isError by remember { mutableStateOf(false) }

                        // Observe painter state changes
                        LaunchedEffect(painter) {
                            snapshotFlow { painter.state }.collect { state ->
                                isLoading = state is AsyncImagePainter.State.Loading
                                isError = state is AsyncImagePainter.State.Error
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {

                            // Always show the image
                            Image(
                                painter = painter,
                                contentDescription = "$groupName representative image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )

                            // 폴더 효과를 위한 오버레이
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.tertiaryContainer.copy(
                                                    alpha = 0.3f
                                                )
                                            )
                                        )
                                    )
                            )

                            // 폴더 아이콘 (우하단)
                            Icon(
                                imageVector = Icons.Filled.FolderSpecial,
                                contentDescription = "Folder Icon",
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .size(20.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        CircleShape
                                    )
                                    .padding(4.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )

                            // Show loading overlay
                            if (isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(itemSize * 0.15f),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Show error overlay
                            if (isError) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderShared,
                                        contentDescription = "Group Folder Icon (error)",
                                        modifier = Modifier.size(itemSize * 0.25f),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        // 대표 이미지가 없는 경우
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderSpecial,
                                contentDescription = "Group Folder Icon",
                                modifier = Modifier.size(itemSize * 0.3f),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "$itemCount",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 하단 정보 영역
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(0.4f)
                ) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestaurantMenu,
                            contentDescription = "Recipe Icon",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$itemCount 개의 레시피",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // 반짝이는 효과 (선택적)
            if (isPressed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.White.copy(alpha = 0.1f)
                        )
                )
            }

//            LaunchedEffect(draggingItem, dragPositionInWindow) {
//                Log.d("Debug", "Bounds: ${cardBoundsInWindow?.toString()}")
//                Log.d("Debug", "Drag Pos: $dragPositionInWindow")
//
//                if (
//                    draggingItem != null &&
//                    cardBoundsInWindow != null
//                ) {
//                    // ⬇️ 바운드 확장
//                    val expandedBounds = cardBoundsInWindow!!.inset(-1000f)
//
//                    Log.d("DropCheck", " 확장된 카드 바운드: $expandedBounds")
//                    Log.d("DropCheck", " 드래그 위치: $dragPositionInWindow")
//
//                    if (expandedBounds.contains(dragPositionInWindow)) {
//                        Log.d("Drop", " Dropped into $groupName!")
//                        Log.d("DropDebug", "드래그된 카드가 이 그룹 카드에 닿았습니다!")
//                        onDrop(draggingItem!!)
//                    }
//                }
//            }
//            LaunchedEffect(draggingItem, dragPositionInWindow) {
//                Log.d("DropDebug", "draggingItem = $draggingItem")
//                Log.d("DropDebug", "cardBoundsInWindow = $cardBoundsInWindow")
//
//                val bounds = cardBoundsInWindow
//                if (draggingItem != null && bounds != null) {
//                    val expandedBounds = bounds.inflate(1000f)
//                    Log.d("DropDebug", "bounds = $expandedBounds, dragPos = $dragPositionInWindow")
//
//                    if (expandedBounds.contains(dragPositionInWindow)) {
//                        Log.d("DropDebug", " 충돌 감지됨 → 그룹화 시도")
//                        onDrop(draggingItem!!)
//                    }
//                } else {
//                    Log.d("DropDebug", " 충돌 체크 불가 - draggingItem 또는 bounds 가 null")
//                }
//            }






                // 충돌 텍스트 체크
//            Column(
//                modifier = Modifier
//                    .align(Alignment.BottomStart)
//                    .padding(8.dp)
//            ) {
//                Text(
//                    text = "dragOffset: $dragOffset",
//                    style = MaterialTheme.typography.labelSmall,
//                    color = Color.Red
//                )
//                Text(
//                    text = "cardBounds: ${
//                        cardBoundsInWindow?.let {
//                            "(${it.left.toInt()}, ${it.top.toInt()}, ${it.right.toInt()}, ${it.bottom.toInt()})"
//                        } ?: "null"
//                    }",
//                    style = MaterialTheme.typography.labelSmall,
//                    color = Color.Red
//                )
//            }

        }
    }
}
@Composable
fun StyledGroupDetailOverlay(
    groupName: String,
    itemsInGroup: List<GalleryItem>,
    onDismiss: () -> Unit,
    onItemClick: (GalleryItem) -> Unit,
    gridItemSize: Dp,
    modifier: Modifier = Modifier,
    onRenameGroup: (String) -> Unit = {} //  이름 변경
) {
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(groupName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 32.dp, horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                //  그룹명 편집 섹션
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 20.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isEditingName) {
                        TextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            textStyle = MaterialTheme.typography.titleLarge,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            onRenameGroup(editedName) //  이름 변경 호출
                            isEditingName = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "저장")
                        }
                    } else {
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { isEditingName = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "이름 수정")
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                //  레시피 목록
                if (itemsInGroup.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("이 그룹에는 레시피가 없습니다.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = gridItemSize),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(itemsInGroup, key = { item -> "detail_${item.recipeId}" }) { item ->
                            StyledRecipeItemCard(
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
// 범위 확장 함수
fun Rect.inset(pixels: Float): Rect {
    return Rect(
        left - pixels,
        top - pixels,
        right + pixels,
        bottom + pixels
    )
}