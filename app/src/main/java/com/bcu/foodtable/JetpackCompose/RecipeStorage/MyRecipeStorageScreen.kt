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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity
import com.bcu.foodtable.useful.GalleryItem
import coil.compose.AsyncImagePainter
import androidx.compose.foundation.Image
import coil.compose.rememberAsyncImagePainter
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import com.bcu.foodtable.ui.home.AppBottomNavigationBar
import com.bcu.foodtable.ui.home.AppTopBar
import com.bcu.foodtable.ui.home.Screen

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

    val screens = listOf(
        Screen.Home,
        Screen.Subscribe,
        Screen.Social,
        Screen.RecipeStorage,
        Screen.MyPage
    )
//    // 2) Scaffold 에 TopBar/BottomBar 달기
//    Scaffold(
//        topBar = {
//            AppTopBar(
//                selectedTab = selectedTab,
//                screens = screens,
//                user = user
//            )
//        },
//        bottomBar = {
//            AppBottomNavigationBar(
//                screens = screens,
//                selectedTab = selectedTab,
//                onTabSelected = { index ->
//                    selectedTab = index
//                    navController.navigate(screens[index].route) {
//                        popUpTo(navController.graph.startDestinationId) { saveState = true }
//                        launchSingleTop = true
//                        restoreState = true
//                    }
//                }
//            )
//        }
//    ) { innerPadding ->
//        // 3) 실제 콘텐츠 호출부: MyRecipeStorageScreen
//        Box(Modifier.padding(innerPadding)) {
//            MyRecipeStorageScreen(
//                modifier = Modifier.fillMaxSize(),
//                viewModel = recipeGalleryViewModel,
//                homeViewModel = homeViewModel
//            )
//        }
//    }
    // RecipeStorage가 네 번째 탭(인덱스 3)이니까
    var selectedTab by remember { mutableStateOf(3) }

    // 각 레시피 카드/그룹 대표 카드의 화면 내 경계(bounds)를 저장합니다.
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

    // 드롭 처리를 위한 함수
    val processDrop = remember(viewModel, displayList, recipeCardBoundsMap, groupedItemsMap) {
        { sourceItem: GalleryItem, finalDropPosition: Offset ->
            var dropHandled = false
            Log.d("ProcessDrop", "Processing drop for ${sourceItem.recipeId} at $finalDropPosition")

            // 1. 그룹 폴더 위로 드롭했는지 확인 (displayList에 있는 그룹 대표 아이템 기준)
            val groupRepresentativeTargetItems = displayList.filter {
                it.groupId.isNotBlank() &&
                        (groupedItemsMap[it.groupId]?.minByOrNull { g -> g.creationTimestamp ?: 0L }?.recipeId == it.recipeId) &&
                        it.recipeId != sourceItem.recipeId // 자기 자신 그룹으론 드롭 방지 (소스가 그룹 대표일 경우)
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

            // 2. 다른 레시피 카드 위로 드롭했는지 확인
            if (!dropHandled) {
                // 그룹 대표가 아닌 아이템들 + 그룹이 비어있는 아이템들 (잠재적 개별 타겟)
                val individualRecipeTargets = displayList.filter { target ->
                    target.recipeId != sourceItem.recipeId &&
                            (target.groupId.isBlank() || (groupedItemsMap[target.groupId]?.size ?: 0) == 0 ||
                                    (groupedItemsMap[target.groupId]?.size == 1 && groupedItemsMap[target.groupId]?.first()?.recipeId == target.recipeId) // 그룹이지만 사실상 혼자인 경우
                                    ) && // 이미 위에서 처리된 그룹 대표가 아니어야 함
                            !groupRepresentativeTargetItems.any { grpRep -> grpRep.recipeId == target.recipeId }
                }

                for (targetItem in individualRecipeTargets) {
                    val targetBounds = recipeCardBoundsMap[targetItem.recipeId]
                    if (targetBounds != null && targetBounds.contains(finalDropPosition)) {
                        Log.d("ProcessDrop", "Attempting drop of ${sourceItem.recipeId} onto item ${targetItem.recipeId}")
                        val sourceGroupId = sourceItem.groupId.orEmpty()
                        val targetGroupId = targetItem.groupId.orEmpty() // 이 targetItem은 그룹 대표가 아님

                        when {
                            // 소스: 그룹 없음, 타겟: 그룹 없음 -> 새 그룹 생성
                            sourceGroupId.isBlank() && targetGroupId.isBlank() -> {
                                viewModel.createGroup(sourceItem, targetItem)
                                Log.d("ProcessDrop", "Created group with ${sourceItem.recipeId} and ${targetItem.recipeId}")
                            }
                            // 소스: 그룹 있음, 타겟: 그룹 없음 -> 새 그룹 생성 (소스는 기존 그룹에서 제거됨)
                            sourceGroupId.isNotBlank() && targetGroupId.isBlank() -> {
                                viewModel.createGroup(sourceItem, targetItem) // ViewModel에서 sourceItem을 이전 그룹에서 제거하는 로직 필요
                                Log.d("ProcessDrop", "Source grouped, Target ungrouped. Creating new group with ${sourceItem.recipeId} and ${targetItem.recipeId}. Source removed from $sourceGroupId.")
                            }
                            // 이 외의 경우 (예: 그룹된 아이템을 다른 그룹된 아이템(대표X) 위에 놓는 경우)는 현재 로직에서 복잡성을 야기할 수 있어,
                            // 위의 그룹 폴더 타겟팅 또는 명확한 개별 아이템 타겟팅으로 단순화.
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

            // 드래그 상태 초기화
            draggingItem = null
            dragOffset = Offset.Zero
            dragPositionInWindow = Offset.Zero
        }
    }


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
                    items(displayList, key = { it.recipeId + (it.groupId.ifBlank { it.recipeId }) }) { item ->
                        // 각 아이템의 bounds를 recipeCardBoundsMap에 저장/업데이트
                        val itemModifier = Modifier.onGloballyPositioned { coordinates ->
                            recipeCardBoundsMap[item.recipeId] = coordinates.boundsInWindow()
                        }

                        val isGroupRepresentative = item.groupId.isNotBlank() &&
                                (groupedItemsMap[item.groupId]?.minByOrNull { it.creationTimestamp ?: 0L }?.recipeId == item.recipeId) &&
                                (groupedItemsMap[item.groupId]?.size ?: 0) > 0


                        if (isGroupRepresentative) {
                            val itemsInGroup = groupedItemsMap[item.groupId] ?: emptyList()
                            // val representativeItem = itemsInGroup.firstOrNull { it.recipeId == item.recipeId } // 이미 item이 representative임
                            StyledGroupFolderItemCard(
                                modifier = itemModifier, // onGloballyPositioned 적용
                                groupName = item.groupName ?: item.groupId,
                                representativeImageUrl = itemsInGroup.firstOrNull()?.image, // 그룹 내 첫번째 아이템 이미지 사용
                                itemCount = itemsInGroup.size,
                                itemSize = itemSize,
                                onClick = {
                                    showGroupDetailOverlay = item.groupId
                                },
                                onUngroupClick = {
                                    viewModel.ungroup(item.groupId)
                                }
                                // onDrop 콜백은 processDrop으로 중앙화되므로 제거 또는 다른 용도로 사용
                            )
                        } else {
                            StyledRecipeItemCard(
                                item = item,
                                itemSize = itemSize,
                                modifier = itemModifier, // onGloballyPositioned 적용
                                onClick = {
                                    val intent = Intent(context, RecipeCookingActivity::class.java)
                                    intent.putExtra("recipe_id", item.recipeId)
                                    context.startActivity(intent)
                                },
                                onDragStart = { startedItem ->
                                    draggingItem = startedItem
                                    dragOffset = Offset.Zero // 드래그 시작 시 오프셋 초기화
                                },
                                onDrag = { offsetDelta ->
                                    dragOffset += offsetDelta
                                },
                                onDragEnd = {
                                    draggingItem?.let { currentDraggingItem ->
                                        processDrop(currentDraggingItem, dragPositionInWindow)
                                    }
                                    // processDrop 내부에서 draggingItem = null 등으로 상태 초기화
                                },
                                draggingItem = draggingItem,
                                dragOffset = dragOffset,
                                // dragPositionInWindow 는 StyledRecipeItemCard 내부에서 업데이트된 값을 사용
                                onUpdateDragPosition = { newPosition ->
                                    dragPositionInWindow = newPosition
                                }
                            )
                        }
                    }
                }
                // 중요: 기존의 LaunchedEffect(draggingItem, dragPositionInWindow) { ... } 블록은 제거합니다.
                // 모든 드롭 로직은 processDrop 함수를 통해 onDragEnd에서 처리됩니다.
            }
        }

        AnimatedVisibility(
            visible = showGroupDetailOverlay != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            showGroupDetailOverlay?.let { groupId ->
                val items = galleryItems.filter { it.groupId == groupId }
                val groupName = items.firstOrNull()?.groupName ?: "Unnamed Group"

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
    }
}

@Composable
fun StyledRecipeItemCard(
    item: GalleryItem,
    itemSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier, // onGloballyPositioned를 위해 외부에서 Modifier를 받도록 함
    onDragStart: (GalleryItem) -> Unit = {},
    onDrag: (Offset) -> Unit = {}, // Offset은 드래그로 인한 위치 변화량(delta)
    onDragEnd: () -> Unit = {},
    draggingItem: GalleryItem? = null,
    dragOffset: Offset = Offset.Zero, // 전체 드래그 오프셋 (부모로부터 받음)
    onUpdateDragPosition: (Offset) -> Unit = {} // 현재 드래그 포인터의 화면 내 절대 위치 업데이트 콜백
) {
    var isFavorite by remember { mutableStateOf(false) }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // cardBoundsInWindow는 이제 MyRecipeStorageScreen의 recipeCardBoundsMap을 통해 관리됨

    val cardScale by animateFloatAsState(
        targetValue = if (draggingItem?.recipeId == item.recipeId) 1.1f else if (isFavorite) 1.05f else 1.0f, // 드래그 중인 아이템은 살짝 크게
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    // 드래그 제스처와 위치 업데이트를 위한 Modifier 통합
    val combinedModifier = modifier // 외부에서 전달된 Modifier (onGloballyPositioned 포함)
        .pointerInput(item) { // key를 item으로 하여 item이 변경되면 제스처 감지 재시작 (안정성)
            detectDragGestures(
                onDragStart = {
                    Log.d("DragTest", "onDragStart for ${item.recipeId}")
                    onDragStart(item)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount) // 부모에게 dragAmount(delta) 전달

                    // 현재 드래그 중인 포인터의 화면 내 절대 위치 계산 및 업데이트
                    layoutCoordinates?.let { lc ->
                        // change.position은 해당 Composable 내의 로컬 좌표
                        val positionInRoot = lc.localToRoot(change.position) // 화면 루트 기준 좌표
                        // boundsInWindow()는 윈도우 기준 좌표. 일반적으로 localToWindow가 더 적합할 수 있음.
                        // 여기서는 dragPositionInWindow가 윈도우 기준 좌표를 의미하므로 localToWindow 사용
                        val windowPosition = lc.localToWindow(change.position)
                        onUpdateDragPosition(windowPosition)
                        Log.d(
                            "DragPosition",
                            "Item ${item.recipeId} drag windowPosition: $windowPosition, localPos: ${change.position}, dragAmount: $dragAmount"
                        )
                    }
                },
                onDragEnd = {
                    Log.d("DragTest", "onDragEnd for ${item.recipeId}")
                    onDragEnd()
                },
                onDragCancel = {
                    Log.d("DragTest", "onDragCancel for ${item.recipeId}")
                    onDragEnd() // 취소 시에도 onDragEnd 로직 수행하여 상태 초기화
                }
            )
        }
        .onGloballyPositioned { coordinates -> // Modifier 체인 순서 중요
            layoutCoordinates = coordinates
            // recipeCardBoundsMap 업데이트는 이제 MyRecipeStorageScreen에서 Modifier를 통해 직접 수행
        }


    val currentOffsetModifier = if (draggingItem?.recipeId == item.recipeId) {
        Modifier.offset {
            IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt())
        }
    } else {
        Modifier
    }

    Card(
        modifier = combinedModifier // .onGloballyPositioned가 포함된 Modifier
            .then(currentOffsetModifier) // 그 다음에 오프셋 적용
            .width(itemSize)
            .aspectRatio(0.75f)
            .graphicsLayer(
                scaleX = cardScale,
                scaleY = cardScale,
                alpha = if (draggingItem != null && draggingItem.recipeId != item.recipeId) 0.7f else 1.0f // 다른 아이템 드래그 시 반투명
            )
            .clickable(onClick = onClick)
            .shadow(
                elevation = if (draggingItem?.recipeId == item.recipeId) 16.dp else 8.dp, // 드래그 중 그림자 강화
                shape = RoundedCornerShape(16.dp),
                clip = false
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                val painter = rememberAsyncImagePainter(model = item.image)
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
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
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

                if (isLoadingImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(itemSize / 4), color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (isErrorImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Restaurant,
                                contentDescription = "Image failed to load",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(itemSize / 3)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("이미지 로드 실패", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.groupId.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "그룹", // 또는 item.groupName
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    item.creationTimestamp?.let { timestamp ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, contentDescription = "Time", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = remember(timestamp) { java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp)) },
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
    modifier: Modifier = Modifier, // onGloballyPositioned를 위해 외부에서 Modifier를 받도록 함
    onUngroupClick: () -> Unit = {}
    // onDrop 콜백 제거: MyRecipeStorageScreen의 processDrop에서 중앙 처리
) {
    var isPressed by remember { mutableStateOf(false) }
    var showUngroupDialog by remember { mutableStateOf(false) }

    // 애니메이션 효과
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "groupCardScale"
    )
    val cardRotation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "groupCardRotation"
    )

    Card(
        modifier = modifier // 외부에서 전달된 Modifier (onGloballyPositioned 포함)
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
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp), clip = false),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showUngroupDialog) {
                AlertDialog(
                    onDismissRequest = { showUngroupDialog = false },
                    confirmButton = { TextButton(onClick = { onUngroupClick(); showUngroupDialog = false }) { Text("해제하기") } },
                    dismissButton = { TextButton(onClick = { showUngroupDialog = false }) { Text("취소") } },
                    title = { Text("그룹 해제") },
                    text = { Text("이 그룹을 해제하시겠습니까?") }
                )
            }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (representativeImageUrl != null) {
                        val painter = rememberAsyncImagePainter(model = representativeImageUrl)
                        var isLoadingImage by remember(representativeImageUrl) { mutableStateOf(true) }
                        var isErrorImage by remember(representativeImageUrl) { mutableStateOf(false) }

                        LaunchedEffect(painter) {
                            snapshotFlow { painter.state }.collect { state ->
                                isLoadingImage = state is AsyncImagePainter.State.Loading
                                isErrorImage = state is AsyncImagePainter.State.Error
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painter,
                                contentDescription = "$groupName representative image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
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
                            if (isLoadingImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(itemSize * 0.15f), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (isErrorImage) {
                                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(Icons.Filled.FolderShared, contentDescription = "Group Folder Icon (error)", modifier = Modifier.size(itemSize * 0.25f), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.FolderSpecial, contentDescription = "Group Folder Icon", modifier = Modifier.size(itemSize * 0.3f), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                Text(text = "$itemCount", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.4f)) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.RestaurantMenu, contentDescription = "Recipe Icon", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "$itemCount 개의 레시피", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
            if (isPressed) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.1f)))
            }
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
    onRenameGroup: (String) -> Unit = {}
) {
    var isEditingName by remember(groupName) { mutableStateOf(false) } // groupName 변경 시 초기화
    var editedName by remember(groupName) { mutableStateOf(groupName) } // groupName 변경 시 초기화

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
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            )
                        )
                        IconButton(onClick = {
                            if (editedName.isNotBlank()) { // 빈 이름 방지
                                onRenameGroup(editedName)
                            }
                            isEditingName = false
                        }) { Icon(Icons.Default.Check, contentDescription = "저장") }
                    } else {
                        Text(text = groupName, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        IconButton(onClick = { editedName = groupName; isEditingName = true }) { Icon(Icons.Default.Edit, contentDescription = "이름 수정") }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "닫기", tint = MaterialTheme.colorScheme.onSurface) }
                }
                if (itemsInGroup.isEmpty()) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp), contentAlignment = Alignment.Center) {
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
                            StyledRecipeItemCard( // StyledRecipeItemCard는 onGloballyPositioned를 내부적으로 사용하지 않으므로 modifier를 직접 전달
                                item = item,
                                itemSize = gridItemSize,
                                onClick = { onItemClick(item) }
                                // 상세 오버레이 내에서는 드래그 기능 불필요
                            )
                        }
                    }
                }
            }
        }
    }
}

// 범위 확장 함수 (사용하지 않는다면 제거 가능)
// fun Rect.inset(pixels: Float): Rect {
// return Rect(
// left - pixels,
// top - pixels,
// right + pixels,
// bottom + pixels
// )
// }