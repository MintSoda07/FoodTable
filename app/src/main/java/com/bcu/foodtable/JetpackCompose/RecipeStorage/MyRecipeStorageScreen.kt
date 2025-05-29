package com.bcu.foodtable.JetpackCompose.RecipeStorage

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

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
            items.firstOrNull()
        }
        val nonGroupedItems = galleryItems.filter { it.groupId.isBlank() }
        (groupRepresentativeItems + nonGroupedItems)
            .sortedByDescending { it.creationTimestamp ?: 0L }
    }

    var showGroupDetailOverlay by remember { mutableStateOf<Pair<String, List<GalleryItem>>?>(null) }
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
                    modifier = Modifier.fillMaxSize()
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
                                    onClick = {
                                        showGroupDetailOverlay =
                                            (representativeItem.groupName ?: representativeItem.groupId) to itemsInGroup
                                    }
                                )
                            }
                        } else {
                            StyledRecipeItemCard(
                                item = item,
                                itemSize = itemSize,
                                onClick = {
                                    val intent = Intent(context, RecipeCookingActivity::class.java)
                                    intent.putExtra("recipe_id", item.recipeId)
                                    context.startActivity(intent)
                                }
                            )
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
            showGroupDetailOverlay?.let { (groupName, items) ->
                StyledGroupDetailOverlay(
                    groupName = groupName,
                    itemsInGroup = items,
                    onDismiss = { showGroupDetailOverlay = null },
                    onItemClick = { clickedItem ->
                        val intent = Intent(context, RecipeCookingActivity::class.java)
                        intent.putExtra("recipe_id", clickedItem.recipeId)
                        context.startActivity(intent)
                        showGroupDetailOverlay = null
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
    modifier: Modifier = Modifier
) {
    var isFavorite by remember { mutableStateOf(false) }
    
    // 애니메이션 효과 추가
    val cardScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    Card(
        modifier = modifier
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
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
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
        modifier = modifier
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
                    onTap = { onClick() }
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
                                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
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
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // 전체 너비 사용
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize() // 화면을 덮도록
                .padding(vertical = 32.dp, horizontal = 16.dp), // 화면 가장자리에서 약간의 여백
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh, // 다이얼로그 배경색
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
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close Group Detail",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (itemsInGroup.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("이 그룹에는 레시피가 없습니다.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = gridItemSize),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f) // 남은 공간 채우기
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