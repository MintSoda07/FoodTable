package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FridgeScreen(viewModel: FridgeViewModel, navController: NavController) {
    val allIngredients = viewModel.ingredientList
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // 냉장고 상태
    var isOpen by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf("냉장") }
    var dragOffset by remember { mutableStateOf(0f) }
    var isClosing by remember { mutableStateOf(false) }

    // 냉장고 섹션
    val fridgeSections = listOf("냉장", "냉동", "문칸")
    val fridgeMap = remember { fridgeSections.associateWith { mutableStateListOf<Ingredient>() }.toMutableMap() }
    val outsideFridge = remember { mutableStateListOf<Ingredient>() }
    val showDialog = remember { mutableStateOf<Ingredient?>(null) }


    // 파티클 효과를 위한 상태
    var showColdEffect by remember { mutableStateOf(false) }

    // 애니메이션 값들
    val doorRotation by animateFloatAsState(
        targetValue = when {
            isClosing -> 0f
            isOpen -> -120f
            dragOffset > 0 -> (-120f * (dragOffset / 200f)).coerceIn(-120f, 0f)
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "doorRotation"
    )

    val doorShadow by animateFloatAsState(
        targetValue = if (isOpen) 20f else 12f,
        animationSpec = tween(400),
        label = "doorShadow"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isOpen) 300 else 150,
            delayMillis = if (isOpen) 100 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "contentAlpha"
    )

    // 초기 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadIngredients()
    }

    LaunchedEffect(allIngredients) {
        fridgeSections.forEach { fridgeMap[it]?.clear() }
        outsideFridge.clear()

        allIngredients.forEach { ingredient ->
            val alreadyInFridge = fridgeMap[ingredient.section]?.any { it.id == ingredient.id } ?: false
            val alreadyOutside = outsideFridge.any { it.id == ingredient.id }

            if (!alreadyInFridge && !alreadyOutside) {
                fridgeMap[ingredient.section]?.add(ingredient)
            }
        }
    }

    // 재료 이동 함수들
    fun moveIngredientToOutside(ingredient: Ingredient, fromSection: String) {
        Log.d("FridgeDebug", "moveIngredientToOutside() called for ${ingredient.name} from $fromSection")
        val removed = fridgeMap[fromSection]?.removeIf { it.id == ingredient.id } == true
        if (removed) {
            outsideFridge.removeAll { it.id == ingredient.id }
            outsideFridge.add(ingredient)
            Log.d("FridgeDebug", " → outsideFridge now: ${outsideFridge.map { it.name }}")
        }
        else {
            Log.d("FridgeDebug", " → remove failed: not found in $fromSection")
        }
    }

    fun moveIngredientToFridge(ingredient: Ingredient, toSection: String) {
        Log.d("FridgeDebug", "moveIngredientToFridge() called for ${ingredient.name} to $toSection")
        val removed = outsideFridge.removeIf { it.id == ingredient.id }
        val exists = fridgeMap[toSection]?.any { it.id == ingredient.id } == true
        Log.d("FridgeDebug", " → removed from outside: $removed, already exists in fridge: $exists")
        if (removed && !exists) {
            val updated = ingredient.copy(section = toSection)
            fridgeMap[toSection]?.add(updated)
            Log.d("FridgeDebug", " → fridgeMap[$toSection] now: ${fridgeMap[toSection]?.map { it.name }}")
            viewModel.updateIngredientSection(ingredient.id, toSection)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F5F5),
                        Color(0xFFE8E8E8)
                    )
                )
            )
    ) {
        // 메인 냉장고 컨테이너
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.75f)
                .align(Alignment.Center)
                .graphicsLayer {
                    rotationX = -5f
                    cameraDistance = 16f * density.density
                }
        ) {
            // 냉장고 본체 (내부)
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        shadowElevation = 16f
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 내부 배경 그라데이션
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFAFAFA),
                                    Color(0xFFF0F0F0)
                                )
                            )
                        )
                    }

                    // 냉장고 내부 컨텐츠
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = contentAlpha }
                            .padding(16.dp)
                    ) {
                        // 헤더 영역
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 온도 표시
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Thermostat,
                                        contentDescription = "Temperature",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when(selectedSection) {
                                            "냉장" -> "3°C"
                                            "냉동" -> "-18°C"
                                            else -> "7°C"
                                        },
                                        color = Color(0xFF2196F3),
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            // 섹션 선택 탭
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    fridgeSections.forEach { section ->
                                        val isSelected = selectedSection == section
                                        Card(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedSection = section
                                                    if (section == "냉동") showColdEffect = true
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected)
                                                    Color(0xFF2196F3)
                                                else
                                                    Color.Transparent
                                            )
                                        ) {
                                            Text(
                                                text = section,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                color = if (isSelected) Color.White else Color(0xFF666666),
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 재료 그리드
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = fridgeMap[selectedSection] ?: emptyList(),
                                key = { _, item -> item.id }
                            ) { index, ingredient ->
                                DraggableHolographicIngredientCard(
                                    ingredient = ingredient,
                                    index = index,
                                    onClick = {
                                        moveIngredientToOutside(ingredient, selectedSection)
                                    },
                                    onLongClick = { showDialog.value = ingredient },
                                    onDragEnd = {
                                        moveIngredientToOutside(ingredient, selectedSection)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 냉장고 문
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = doorRotation
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        shadowElevation = doorShadow
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                isOpen = dragOffset > 100f
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragOffset = (dragOffset + dragAmount).coerceIn(0f, 200f)
                            }
                        )
                    }
                    .clickable {
                        if (isOpen) {
                            isClosing = true
                            scope.launch {
                                kotlinx.coroutines.delay(300)
                                isOpen = false
                                isClosing = false
                            }
                        } else {
                            isOpen = true
                        }
                    }
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE0E0E0)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 메탈릭 효과
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFEEEEEE),
                                        Color(0xFFDDDDDD),
                                        Color(0xFFCCCCCC),
                                        Color(0xFFDDDDDD),
                                        Color(0xFFEEEEEE)
                                    )
                                )
                            )

                            // 광택 효과
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.1f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width * 0.5f, size.height)
                                )
                            )
                        }

                        // 중앙 로고/브랜드
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-50).dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Kitchen,
                                        contentDescription = "Fridge",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Smart Fridge",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF424242),
                                fontWeight = FontWeight.Light
                            )
                        }

                        // 문 손잡이
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(120.dp)
                                .align(Alignment.CenterEnd)
                                .offset(x = (-24).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF9E9E9E),
                                            Color(0xFF757575),
                                            Color(0xFF616161),
                                            Color(0xFF757575),
                                            Color(0xFF9E9E9E)
                                        )
                                    )
                                )
                        )

                        // 터치 힌트
                        if (!isOpen) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Black.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 32.dp)
                                    .alpha(if (!isOpen) 1f else 0f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = "Touch",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "터치하여 열기",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI 추천 버튼
        AnimatedVisibility(
            visible = isOpen && (fridgeMap[selectedSection]?.isNotEmpty() == true),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 24.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    // AI 추천 기능 - 현재 섹션의 랜덤 재료 선택
                    fridgeMap[selectedSection]?.randomOrNull()?.let { ingredient ->
                        showDialog.value = ingredient
                    }
                },
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI 추천",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI 레시피 추천",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 하단 트레이 (꺼낸 재료)
        AnimatedVisibility(
            visible = outsideFridge.isNotEmpty(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SmartTray(
                items = outsideFridge,
                onItemReturn = { ingredient ->
                    moveIngredientToFridge(ingredient, selectedSection)
                }
            )
        }

        // 재료 추가 버튼
        FloatingActionButton(
            onClick = {
                navController.navigate("add_ingredient?section=$selectedSection")
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Color(0xFF2196F3),
            contentColor = Color.White
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "재료 추가",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // AI 추천 다이얼로그
    showDialog.value?.let { selectedIngredient ->
        FuturisticDialog(
            ingredients = GlobalTray.items,
            recipes = viewModel.findRecipesByIngredient(selectedIngredient.name),
            onDismiss = { showDialog.value = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableHolographicIngredientCard(
    ingredient: Ingredient,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragEnd: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.15f
            isPressed -> 0.95f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (isDragging) 0.8f else 1f
                shadowElevation = if (isDragging) 16f else 4f
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true

                    },
                    onDragEnd = {
                        if (offset.getDistance() > 100f) {
                            if (!GlobalTray.items.contains(ingredient)) {
                                GlobalTray.items.add(ingredient)
                            }
                            onDragEnd()
                        }
                        offset = Offset.Zero
                        isDragging = false
                    },
                    onDrag = { _, dragAmount ->
                        offset += dragAmount
                    }
                )
            }
            .combinedClickable(
                onClick = { if (!isDragging) onClick() },
                onLongClick = { if (!isDragging) onLongClick() }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (!isDragging) {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                Color(0xFF2196F3).copy(alpha = 0.1f)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 이모지
            Text(
                text = getEmojiForIngredient(ingredient.name),
                fontSize = 32.sp,
                modifier = Modifier.graphicsLayer {
                    if (isDragging) {
                        rotationZ = 10f
                    }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 이름
            Text(
                text = ingredient.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242),
                maxLines = 1
            )

            // 수량
            Card(
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "${ingredient.quantity}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF666666),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
fun SmartTray(
    items: List<Ingredient>,
    onItemReturn: (Ingredient) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBasket,
                        contentDescription = "Smart Tray",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "꺼낸 재료",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF424242),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "${items.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                // itemsCount 대신 items(items = , key = ) 사용
            ) {
                items(
                    items = items,
                    key = { ing -> ing.id }     // ← 여기서 고유 key 지정
                ) { ingredient ->
                    FloatingIngredientChip(
                        ingredient = ingredient,
                        onReturn = {
                            Log.d("FridgeDebug", "Returning ${ingredient.name}")
                            onItemReturn(ingredient)
                            GlobalTray.items.remove(ingredient)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingIngredientChip(
    ingredient: Ingredient,
    onReturn: () -> Unit
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .size(width = 90.dp, height = 80.dp)
            .offset {
                IntOffset(
                    offset.x.toInt(),
                    offset.y.toInt()
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true
                        Log.d("FridgeDebug", "Drag started on ${ingredient.name}")
                    },
                    onDragEnd = {
                        Log.d("FridgeDebug", "Drag ended on ${ingredient.name} with offset y=${offset.y}")
                        if (offset.y < -100f) {
                            Log.d("FridgeDebug", " → offset threshold passed, calling onReturn()")
                            onReturn()
                        } else {
                            Log.d("FridgeDebug", " → offset threshold NOT passed, cancelling return")
                        }
                        offset = Offset.Zero
                        isDragging = false
                    },
                    onDrag = { _, dragAmount ->
                        offset += dragAmount
                    }
                )
            }
            .graphicsLayer {
                alpha = if (isDragging) 0.8f else 1f
                scaleX = if (isDragging) 1.1f else 1f
                scaleY = if (isDragging) 1.1f else 1f
                shadowElevation = if (isDragging) 12f else 4f
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = getEmojiForIngredient(ingredient.name),
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = ingredient.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF666666),
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )

            if (!isDragging && offset == Offset.Zero) {
                Icon(
                    imageVector = Icons.Default.SwipeUp,
                    contentDescription = "Swipe up",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}


fun getEmojiForIngredient(name: String): String {
    return when (name) {
        // 단백질류
        "계란"              -> "🥚"
        "소고기"            -> "🥩"
        "닭고기", "치킨"    -> "🍗"
        "돼지고기"          -> "🥓"
        "생선", "참치", "연어" -> "🐟"
        "새우"              -> "🦐"
        "오징어"            -> "🦑"
        "문어"              -> "🐙"
        "조개"              -> "🦪"

        // 채소류
        "당근"              -> "🥕"
        "상추", "양배추"     -> "🥬"
        "브로콜리"          -> "🥦"
        "감자"              -> "🥔"
        "고구마"            -> "🍠"
        "양파"              -> "🧅"
        "마늘"              -> "🧄"
        "고추", "피망"      -> "🌶️"
        "토마토"            -> "🍅"
        "버섯"              -> "🍄"
        "호박"              -> "🎃"
        "옥수수"            -> "🌽"

        // 과일류
        "사과"              -> "🍎"
        "바나나"            -> "🍌"
        "수박"              -> "🍉"
        "포도"              -> "🍇"
        "딸기"              -> "🍓"
        "키위"              -> "🥝"
        "파인애플"          -> "🍍"

        // 유제품·가공품
        "우유", "요거트"     -> "🥛"
        "치즈"              -> "🧀"
        "버터"              -> "🧈"
        "아이스크림"        -> "🍨"

        // 곡류·빵·간식
        "쌀", "밥"          -> "🍚"
        "빵", "토스트"      -> "🍞"
        "케이크"            -> "🎂"
        "쿠키"              -> "🍪"
        "초코", "초콜릿"    -> "🍫"

        // 패스트푸드·간편식
        "피자"              -> "🍕"
        "햄버거"            -> "🍔"
        "핫도그", "소시지"  -> "🌭"
        "샌드위치"          -> "🥪"
        "타코"              -> "🌮"
        "라면"              -> "🍜"
        "스파게티", "파스타"-> "🍝"

        else -> "🍽️"
    }
}
