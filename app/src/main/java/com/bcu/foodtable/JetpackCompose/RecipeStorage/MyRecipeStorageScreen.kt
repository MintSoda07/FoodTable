package com.bcu.foodtable.JetpackCompose.RecipeStorage
import android.content.Intent
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity
import com.bcu.foodtable.useful.GalleryItem
import kotlin.math.roundToInt

// --- 함수명과 시그니처는 기존과 완벽하게 동일하게 유지합니다 ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRecipeStorageScreen(
    modifier: Modifier = Modifier,
    viewModel: RecipeGalleryViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    // 기존의 모든 로직은 그대로 유지합니다.
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

    val groupedItemsMap = remember(galleryItems) { galleryItems.filter { it.groupId.isNotBlank() }.groupBy { it.groupId } }
    val displayList = remember(galleryItems, groupedItemsMap) {
        val groupRepresentativeItems = groupedItemsMap.mapNotNull { (_, items) -> items.minByOrNull { it.creationTimestamp ?: 0L } }
        val nonGroupedItems = galleryItems.filter { it.groupId.isBlank() }
        (groupRepresentativeItems + nonGroupedItems).sortedByDescending { it.creationTimestamp ?: 0L }
    }
    var showGroupDetailOverlay by remember { mutableStateOf<String?>(null) }
    val itemMinWidth = 180.dp

    val processDrop = remember(viewModel, displayList, recipeCardBoundsMap, groupedItemsMap) {
        { sourceItem: GalleryItem, finalDropPosition: Offset ->
            var dropHandled = false
            val groupRepresentativeTargetItems = displayList.filter { it.groupId.isNotBlank() && (groupedItemsMap[it.groupId]?.minByOrNull { g -> g.creationTimestamp ?: 0L }?.recipeId == it.recipeId) && it.recipeId != sourceItem.recipeId }
            for (groupRepTarget in groupRepresentativeTargetItems) {
                val groupBounds = recipeCardBoundsMap[groupRepTarget.recipeId]
                if (groupBounds != null && groupBounds.contains(finalDropPosition)) {
                    if (sourceItem.groupId != groupRepTarget.groupId) {
                        viewModel.addToGroup(groupRepTarget.groupId, sourceItem)
                    }
                    dropHandled = true
                    break
                }
            }
            if (!dropHandled) {
                val individualRecipeTargets = displayList.filter { target -> target.recipeId != sourceItem.recipeId && target.groupId.isBlank() }
                for (targetItem in individualRecipeTargets) {
                    val targetBounds = recipeCardBoundsMap[targetItem.recipeId]
                    if (targetBounds != null && targetBounds.contains(finalDropPosition)) {
                        viewModel.createGroup(sourceItem, targetItem)
                        dropHandled = true
                        break
                    }
                }
            }
            draggingItem = null
            dragOffset = Offset.Zero
            dragPositionInWindow = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3EFEA)) // 따뜻한 스크랩북 배경색
    ) {
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.5f)) {
            drawScrapbookBackground()
        }

        when {
            isLoading -> CookbookLoadingState()
            loadFailed -> CookbookErrorState(onRetry = { viewModel.loadGalleryItems() })
            displayList.isEmpty() -> CookbookEmptyState()
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    CookbookHeader(
                        userName = user?.name ?: "나",
                        totalRecipes = galleryItems.size,
                        totalGroups = groupedItemsMap.size
                    )
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Adaptive(minSize = itemMinWidth),
                        contentPadding = PaddingValues(16.dp),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(displayList, key = { it.recipeId + (it.groupId.ifBlank { it.recipeId }) }) { item ->
                            val itemModifier = Modifier.onGloballyPositioned { coordinates -> recipeCardBoundsMap[item.recipeId] = coordinates.boundsInWindow() }
                            val isGroupRepresentative = item.groupId.isNotBlank() && (groupedItemsMap[item.groupId]?.minByOrNull { it.creationTimestamp ?: 0L }?.recipeId == item.recipeId)

                            if (isGroupRepresentative) {
                                val itemsInGroup = groupedItemsMap[item.groupId] ?: emptyList()
                                CookbookGroupFolderCard(
                                    modifier = itemModifier,
                                    groupName = item.groupName ?: "새로운 그룹",
                                    itemCount = itemsInGroup.size,
                                    previewImageUrls = itemsInGroup.take(3).mapNotNull { it.image },
                                    onClick = { showGroupDetailOverlay = item.groupId },
                                    onUngroupClick = { viewModel.ungroup(item.groupId) }
                                )
                            } else {
                                CookbookRecipeCard(
                                    item = item,
                                    modifier = itemModifier,
                                    onClick = {
                                        val intent = Intent(context, RecipeCookingActivity::class.java)
                                        intent.putExtra("recipe_id", item.recipeId)
                                        context.startActivity(intent)
                                    },
                                    onDragStart = { draggingItem = it; dragOffset = Offset.Zero },
                                    onDrag = { dragOffset += it },
                                    onDragEnd = { draggingItem?.let { processDrop(it, dragPositionInWindow) } },
                                    onUpdateDragPosition = { dragPositionInWindow = it },
                                    draggingItem = draggingItem,
                                    dragOffset = dragOffset
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showGroupDetailOverlay != null,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.8f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.8f)
        ) {
            showGroupDetailOverlay?.let { groupId ->
                val items = galleryItems.filter { it.groupId == groupId }
                val groupName = items.firstOrNull()?.groupName ?: "Unnamed Group"
                CookbookGroupDetailOverlay(
                    groupName = groupName,
                    itemsInGroup = items,
                    onDismiss = { showGroupDetailOverlay = null },
                    onItemClick = { item ->
                        val intent = Intent(context, RecipeCookingActivity::class.java)
                        intent.putExtra("recipe_id", item.recipeId)
                        context.startActivity(intent)
                        showGroupDetailOverlay = null
                    },
                    onRenameGroup = { newName -> viewModel.renameGroup(groupId, newName) }
                )
            }
        }
    }
}


// --- 아래는 새롭게 디자인된 컴포저블들입니다 ---
// --- 외부 폰트 의존성 제거됨 ---

@Composable
fun CookbookHeader(userName: String, totalRecipes: Int, totalGroups: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${userName}의",
                style = MaterialTheme.typography.titleMedium, // 기본 스타일 사용
                color = Color(0xFF8D6E63)
            )
            Text(
                text = "디지털 쿡북",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF5D4037),
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "총 ${totalRecipes}개 레시피 | ${totalGroups}개 그룹",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF795548)
        )
    }
}

@Composable
fun CookbookRecipeCard(
    item: GalleryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDragStart: (GalleryItem) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onUpdateDragPosition: (Offset) -> Unit,
    draggingItem: GalleryItem?,
    dragOffset: Offset
) {
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val isDragging = draggingItem?.recipeId == item.recipeId

    val gestureModifier = modifier
        .onGloballyPositioned { coordinates -> layoutCoordinates = coordinates }
        .pointerInput(item) {
            forEachGesture {
                awaitPointerEventScope {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val change = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    if (change != null) {
                        onDragStart(item)
                        drag(change.id) { dragChange ->
                            onDrag(dragChange.positionChange())
                            layoutCoordinates?.let { onUpdateDragPosition(it.localToWindow(dragChange.position)) }
                            dragChange.consume()
                        }
                        onDragEnd()
                    } else {
                        onClick()
                    }
                }
            }
        }

    val dragOffsetModifier = if (isDragging) { Modifier.offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) } } else Modifier
    val cardScale by animateFloatAsState(targetValue = if (isDragging) 1.1f else 1f, label = "cardScale")

    Box(
        modifier = gestureModifier
            .then(dragOffsetModifier)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                shadowElevation = if (isDragging) 24f else 8f
                rotationZ = if (isDragging) -5f else (item.recipeId.hashCode() % 10 - 5).toFloat() / 2f
            }
            .alpha(if (draggingItem != null && !isDragging) 0.5f else 1f)
    ) {
        Surface(
            modifier = Modifier,
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
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                )
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium, // 기본 스타일 사용
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.creationTimestamp?.let { java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
        TapeDecoration(Modifier.align(Alignment.TopCenter).offset(y = (-10).dp))
    }
}

@Composable
fun CookbookGroupFolderCard(
    modifier: Modifier = Modifier,
    groupName: String,
    itemCount: Int,
    previewImageUrls: List<String>,
    onClick: () -> Unit,
    onUngroupClick: () -> Unit
) {
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
                Icon(Icons.Default.Link, contentDescription = "Group Clip", tint = Color(0xFF795548))
                Spacer(Modifier.width(4.dp))
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
fun CookbookGroupDetailOverlay(
    groupName: String,
    itemsInGroup: List<GalleryItem>,
    onDismiss: () -> Unit,
    onItemClick: (GalleryItem) -> Unit,
    onRenameGroup: (String) -> Unit
) {
    var isEditingName by remember(groupName) { mutableStateOf(false) }
    var editedName by remember(groupName) { mutableStateOf(groupName) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.fillMaxHeight(0.9f).fillMaxWidth(0.9f).clickable(enabled = false, onClick = {}), shape = RoundedCornerShape(16.dp), color = Color(0xFFF3EFEA)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color(0xFF5D4037))
                        Spacer(Modifier.width(12.dp))
                        if (isEditingName) {
                            BasicTextField(value = editedName, onValueChange = { editedName = it }, textStyle = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF5D4037), fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRenameGroup(editedName); isEditingName = false }) { Icon(Icons.Default.Check, contentDescription = "저장", tint = Color(0xFF5D4037)) }
                        } else {
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
                            CookbookRecipeCard(item = item, onClick = { onItemClick(item) }, onDragStart = {}, onDrag = {}, onDragEnd = {}, onUpdateDragPosition = {}, draggingItem = null, dragOffset = Offset.Zero)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CookbookLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF795548))
            Spacer(Modifier.height(16.dp))
            Text("나의 쿡북을 펼치는 중...", style = MaterialTheme.typography.titleMedium, color = Color(0xFF795548)) // 기본 스타일 사용
        }
    }
}

@Composable
fun CookbookErrorState(onRetry: () -> Unit) { /* ... 이전과 동일 ... */ }
@Composable
fun CookbookEmptyState() { /* ... 이전과 동일 ... */ }
@Composable
private fun TapeDecoration(modifier: Modifier = Modifier) { /* ... 이전과 동일 ... */ }
private fun DrawScope.drawScrapbookBackground() { /* ... 이전과 동일 ... */ }

// 기존 Premium... State 함수들은 Cookbook...State로 이름이 변경되었거나,
// 디자인이 비슷하여 그대로 사용 가능합니다.
// 만약 PremiumHeader 등 외부에서 직접 호출하는 함수가 있다면 해당 함수의 이름을 유지해야 합니다.
// 이 코드에서는 MyRecipeStorageScreen 내부에서만 호출되므로 Cookbook... 으로 변경했습니다.