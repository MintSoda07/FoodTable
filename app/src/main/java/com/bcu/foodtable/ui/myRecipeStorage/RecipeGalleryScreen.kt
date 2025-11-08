//package com.bcu.foodtable.ui.myRecipeStorage
//
//import androidx.compose.foundation.ExperimentalFoundationApi
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.grid.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import coil.compose.AsyncImage
//import coil.request.ImageRequest
//import com.bcu.foodtable.useful.GalleryItem
//
//@OptIn(ExperimentalFoundationApi::class)
//@Composable
//fun RecipeGalleryScreen(
//    viewModel: RecipeGalleryViewModel = viewModel(),
//    onRecipeClick: (GalleryItem) -> Unit,
//    onGroupClick: (String, List<GalleryItem>) -> Unit
//) {
//    val galleryItems by viewModel.galleryItems.collectAsState()
//    val isLoading by viewModel.isLoading.collectAsState()
//    val loadFailed by viewModel.loadFailed.collectAsState()
//    val density = LocalDensity.current
//
//    // 최초 진입 시 로딩
//    LaunchedEffect(Unit) {
//        viewModel.loadGalleryItems()
//    }
//
//    val grouped = galleryItems.groupBy { it.groupId }
//    val displayedItems = remember(galleryItems) {
//        galleryItems.filter {
//            it.groupId.isBlank() || grouped[it.groupId]?.firstOrNull()?.recipeId == it.recipeId
//        }.toMutableStateList()
//    }
//
//    var activeGroup by remember { mutableStateOf<Pair<String, List<GalleryItem>>?>(null) }
//    var draggingIndex by remember { mutableStateOf<Int?>(null) }
//    var dragOffset by remember { mutableStateOf(Offset.Zero) }
//
//    val itemDpSize = 160.dp
//    val itemSizePx = with(density) { itemDpSize.toPx() }
//
//    when {
//        isLoading -> {
//            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                CircularProgressIndicator()
//            }
//        }
//
//        loadFailed -> {
//            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    Text("불러오기에 실패했습니다.")
//                    Spacer(Modifier.height(12.dp))
//                    Button(onClick = { viewModel.loadGalleryItems() }) {
//                        Text("다시 시도")
//                    }
//                }
//            }
//        }
//
//        displayedItems.isEmpty() -> {
//            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                Text("아이템이 없습니다...")
//            }
//        }
//
//        else -> {
//            Box(Modifier.fillMaxSize()) {
//                LazyVerticalGrid(
//                    columns = GridCells.Fixed(2),
//                    verticalArrangement = Arrangement.spacedBy(12.dp),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp),
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(horizontal = 8.dp, vertical = 50.dp)
//                ) {
//                    itemsIndexed(displayedItems, key = { _, item -> item.recipeId }) { index, item ->
//                        val isGroupFolder = item.groupId.isNotBlank()
//                        val isDragging = index == draggingIndex
//
//                        Box(
//                            modifier = Modifier
//                                .animateItemPlacement()
//                                .graphicsLayer {
//                                    if (isDragging) alpha = 0.3f // 흐림
//                                }
//                                .pointerInput(index) {
//                                    detectDragGesturesAfterLongPress(
//                                        onDragStart = { draggingIndex = index },
//                                        onDragEnd = {
//                                            draggingIndex = null
//                                            dragOffset = Offset.Zero
//                                        },
//                                        onDragCancel = {
//                                            draggingIndex = null
//                                            dragOffset = Offset.Zero
//                                        },
//                                        onDrag = { change, dragAmount ->
//                                            change.consume()
//                                            dragOffset += dragAmount
//
//                                            val targetIndex = calculateTargetIndex(
//                                                dragOffset = dragOffset,
//                                                itemSizePx = itemSizePx,
//                                                columns = 2,
//                                                itemCount = displayedItems.size
//                                            )
//
//                                            if (targetIndex != null &&
//                                                targetIndex != draggingIndex &&
//                                                targetIndex in displayedItems.indices
//                                            ) {
//                                                displayedItems.swap(draggingIndex!!, targetIndex)
//                                                draggingIndex = targetIndex
//                                                dragOffset = Offset.Zero
//                                            }
//                                        }
//                                    )
//                                }
//                                .clickable(enabled = draggingIndex == null) {
//                                    if (isGroupFolder) {
//                                        activeGroup = item.groupId to (grouped[item.groupId] ?: listOf(item))
//                                    } else {
//                                        onRecipeClick(item)
//                                    }
//                                }
//                        ) {
//                            GalleryCard(item = item, isGroup = isGroupFolder)
//                        }
//                    }
//                }
//
//                // 드래그 중인 카드의 미리보기
//                if (draggingIndex != null && draggingIndex in displayedItems.indices) {
//                    val previewItem = displayedItems[draggingIndex!!]
//
//                    Box(modifier = Modifier.fillMaxSize()) {
//                        Box(
//                            modifier = Modifier
//                                .graphicsLayer {
//                                    translationX = dragOffset.x
//                                    translationY = dragOffset.y
//                                    shadowElevation = 12f
//                                    scaleX = 1.05f
//                                    scaleY = 1.05f
//                                }
//                        ) {
//                            GalleryCard(item = previewItem, isGroup = previewItem.groupId.isNotBlank())
//                        }
//                    }
//                }
//
//                // 그룹 팝업
//                activeGroup?.let { (groupId, items) ->
//                    AlertDialog(
//                        onDismissRequest = { activeGroup = null },
//                        confirmButton = {},
//                        text = {
//                            Column {
//                                Text("그룹: $groupId", style = MaterialTheme.typography.titleMedium)
//                                Spacer(Modifier.height(12.dp))
//                                items.forEach {
//                                    Text("• ${it.name}")
//                                }
//                            }
//                        }
//                    )
//                }
//            }
//        }
//    }
//}
//
//
//
//
//@Composable
//fun GalleryCard(item: GalleryItem, isGroup: Boolean = false) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
//            .padding(8.dp)
//    ) {
//        AsyncImage(
//            model = ImageRequest.Builder(LocalContext.current)
//                .data(item.image)
//                .crossfade(true)
//                .build(),
//            contentDescription = null,
//            modifier = Modifier
//                .fillMaxWidth()
//                .aspectRatio(1f)
//        )
//        Spacer(Modifier.height(4.dp))
//        Text(
//            text = if (isGroup) "📁 그룹: ${item.groupId}" else item.name,
//            style = MaterialTheme.typography.bodyMedium
//        )
//    }
//}
//
//
//fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
//    if (index1 in indices && index2 in indices) {
//        val tmp = this[index1]
//        this[index1] = this[index2]
//        this[index2] = tmp
//    }
//}
//
//fun calculateTargetIndex(
//    dragOffset: Offset,
//    itemSizePx: Float,
//    columns: Int,
//    itemCount: Int
//): Int? {
//    val row = (dragOffset.y / itemSizePx).toInt()
//    val col = (dragOffset.x / itemSizePx).toInt()
//    val index = row * columns + col
//    return index.takeIf { it in 0 until itemCount }
//}
