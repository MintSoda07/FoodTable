// Gradle (app/build.gradle)
// dependencies {
//    implementation("com.airbnb.android:lottie-compose:6.4.0")
// }

package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R
import com.bcu.foodtable.JetpackCompose.AI.AiHelperViewModel
import com.bcu.foodtable.ai.OpenAIClient
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.*

// ----- 외부에서 이미 존재한다고 가정되는 타입들 (원본 코드 기준) -----

// --------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FridgeScreen(viewModel: FridgeViewModel, navController: NavController) {
    val allIngredients = viewModel.ingredientList
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // 냉장고 상태
    var isOpen by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf("냉장") }
    var dragOffset by remember { mutableStateOf(0f) }
    var isClosing by remember { mutableStateOf(false) }

    // MERGED: 재료 아이템 애니메이션을 위한 상태
    var itemsVisible by remember { mutableStateOf(false) }

    // 냉장고 섹션
    val fridgeSections = listOf("냉장", "냉동", "문칸")
    val fridgeMap = remember { fridgeSections.associateWith { mutableStateListOf<Ingredient>() }.toMutableMap() }
    val outsideFridge = remember { mutableStateListOf<Ingredient>() }
    val showDialog = remember { mutableStateOf<Ingredient?>(null) }

    val aiViewModel: AiHelperViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AiHelperViewModel(OpenAIClient()) as T
            }
        }
    )
    val aiState by aiViewModel.uiState.collectAsState()
    val gson = remember { Gson() }

    // 로딩 & 로딩 문구 롤링
    var isLoading by remember { mutableStateOf(true) }
    val loadingTips = remember {
        listOf(
            "신선함을 정렬하는 중...",
            "냉장고 온도 보정 중...",
            "식재료 유통기한을 스캔하는 중...",
            "AI 셰프가 레시피를 예열 중...",
            "맛의 조합을 시뮬레이션 중..."
        )
    }
    var tipIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isLoading) {
        while (isLoading) {
            delay(5_000)
            tipIndex = (tipIndex + 1) % loadingTips.size
        }
    }

    // 문/아이템 글로시·스윙·호버용 무한 애니메이션
    val inf = rememberInfiniteTransition(label = "inf")
    val shimmerX by inf.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    val hoverPhase by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hoverPhase"
    )

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
        isLoading = true
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
        isLoading = false
    }

    // 문이 열릴 때 아이템 애니메이션 트리거
    LaunchedEffect(isOpen) {
        if (isOpen) {
            delay(200)
            itemsVisible = true
        } else {
            itemsVisible = false
        }
    }

    // 재료 이동 함수들
    fun moveIngredientToOutside(ingredient: Ingredient, fromSection: String) {
        val removed = fridgeMap[fromSection]?.removeIf { it.id == ingredient.id } == true
        if (removed) {
            outsideFridge.removeAll { it.id == ingredient.id }
            outsideFridge.add(ingredient)
        }
    }

    fun moveIngredientToFridge(ingredient: Ingredient, toSection: String) {
        val removed = outsideFridge.removeIf { it.id == ingredient.id }
        val exists = fridgeMap[toSection]?.any { it.id == ingredient.id } == true
        if (removed && !exists) {
            val updated = ingredient.copy(section = toSection)
            fridgeMap[toSection]?.add(updated)
            viewModel.updateIngredientSection(ingredient.id, toSection)
        }
    }

    // ======= 메인 레이아웃 =======
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF5F5F5), Color(0xFFE8E8E8))
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
                    .graphicsLayer { shadowElevation = 16f },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFAFAFA), Color(0xFFF0F0F0))
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
                                        text = when (selectedSection) {
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

                            // 섹션 토글
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Row(modifier = Modifier.padding(4.dp)) {
                                    fridgeSections.forEach { section ->
                                        val isSelected = selectedSection == section
                                        Card(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { selectedSection = section },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) Color(0xFF2196F3) else Color.Transparent
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
                        val gridItems = fridgeMap[selectedSection] ?: emptyList()
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = gridItems,
                                key = { _, item -> item.id }
                            ) { index, ingredient ->
                                AnimatedVisibility(
                                    visible = itemsVisible,
                                    enter = slideInVertically(
                                        initialOffsetY = { it + 20 },
                                        animationSpec = tween(durationMillis = 500, delayMillis = index * 40)
                                    ) + fadeIn(animationSpec = tween(durationMillis = 400)),
                                    exit = fadeOut(animationSpec = tween(100))
                                ) {
                                    DraggableHolographicIngredientCard(
                                        ingredient = ingredient,
                                        index = index,
                                        hoverPhase = hoverPhase,
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
            }

            // 냉장고 문 (글로시+스윙)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = doorRotation
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        shadowElevation = doorShadow
                        rotationZ = if (isOpen) sin(hoverPhase.toDouble()).toFloat() * 0.6f else 0f
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
                                delay(300)
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // 기본 금속 질감
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
                            // 왼쪽 soft highlight
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.28f),
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.1f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width * 0.5f, size.height)
                                )
                            )
                            // NEW: 대각선 글로시 라이트 스윕
                            val w = size.width
                            val h = size.height
                            val stripeWidth = w * 0.28f
                            val x = w * shimmerX
                            rotate(degrees = -22f) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Transparent,
                                        0.45f to Color.White.copy(alpha = 0.22f),
                                        0.5f to Color.White.copy(alpha = 0.32f),
                                        0.55f to Color.White.copy(alpha = 0.22f),
                                        1f to Color.Transparent
                                    ),
                                    topLeft = Offset(x - stripeWidth, h * -0.1f),
                                    size = Size(stripeWidth * 2f, h * 1.2f)
                                )
                            }
                        }

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
                                Box(contentAlignment = Alignment.Center) {
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

                        // 손잡이
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

                        if (!isOpen) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 32.dp)
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
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isOpen && GlobalTray.items.isNotEmpty(),
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 0.dp, end = 16.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { showDialog.value = Ingredient(id = "", name = "AI Trigger", quantity = 1) },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI 추천", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AI 레시피 추천", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 하단 트레이
        AnimatedVisibility(
            visible = isOpen && GlobalTray.items.isNotEmpty(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SmartTray(
                items = outsideFridge,
                onItemReturn = { ingredient ->
                    moveIngredientToFridge(ingredient, selectedSection)
                    GlobalTray.items.removeAll { it.id == ingredient.id }
                },
                onItemDelete = { ingredient ->
                    viewModel.deleteIngredientFromTray(ingredient)
                    outsideFridge.removeAll { it.id == ingredient.id }
                    GlobalTray.items.removeAll { it.id == ingredient.id }
                }
            )
        }

        // 재료 추가 버튼
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 40.dp, bottom = 100.dp)
        ) {
            FloatingActionButton(
                onClick = { navController.navigate("add_ingredient?section=$selectedSection") },
                containerColor = Color(0xFF2196F3),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "재료 추가", modifier = Modifier.size(24.dp))
            }
        }

        // Lottie 로딩 오버레이 (isLoading 또는 AI 진행 시에도 재사용 가능)
        val showLoader = isLoading // || (aiState.isLoading == true)
        if (showLoader) {
            LoadingOverlay(
                tip = loadingTips[tipIndex],
                rawRes = R.raw.food_prep
            )
        }
    }

    // AI 추천 다이얼로그
    showDialog.value?.let { _ ->
        FuturisticDialog(
            ingredients = GlobalTray.items,
            recipes = emptyList(),
            navController = navController,
            aiViewModel = aiViewModel,
            userId = UserManager.getUser()?.uid ?: "",
            onDismiss = { showDialog.value = null }
        )
    }

    // Ai추천 전환
    LaunchedEffect(aiState.resultText, aiState.imageUrl) {
        val order = aiState.resultText
        val image = aiState.imageUrl
        if (order.isNotBlank() && !image.isNullOrBlank()) {
            val title = aiState.recipes.firstOrNull()?.trim().takeIf { !it.isNullOrEmpty() } ?: "새 레시피"
            val recipe = RecipeItem(
                id = UUID.randomUUID().toString(),
                name = title,
                description = "AI가 추천한 요리입니다.",
                imageResId = image,
                ingredients = aiState.ingredients,
                order = order,
                tags = listOf("AI추천"),
                C_categories = listOf("AI")
            )

            val userId = UserManager.getUser()?.uid ?: ""
            val db = Firebase.firestore
            val batch = db.batch()
            GlobalTray.items.forEach { ing ->
                batch.delete(
                    db.collection("user")
                        .document(userId)
                        .collection("fridge")
                        .document(ing.docId)
                )
            }
            batch.commit()
                .addOnSuccessListener { Log.d("AI", "DB 재료 삭제 완료!") }
                .addOnFailureListener { e -> Log.e("AI", "DB 재료 삭제 실패: $e") }

            val encoded = Uri.encode(Gson().toJson(recipe))
            navController.navigate("ai_recipe/$encoded")

            GlobalTray.items.clear()
            aiViewModel.hideWarning()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableHolographicIngredientCard(
    ingredient: Ingredient,
    index: Int,
    hoverPhase: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDragEnd: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.15f
            isPressed -> 0.96f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    val tilt = if (isDragging) 6f else sin((hoverPhase + index * 0.6f).toDouble()).toFloat() * 1.2f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .graphicsLayer {
                scaleX = scale * if (!isDragging) pulse else 1f
                scaleY = scale * if (!isDragging) pulse else 1f
                alpha = if (isDragging) 0.9f else 1f
                rotationZ = tilt
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) Color(0xFF2196F3).copy(alpha = 0.10f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 3.dp)
    ) {
        // 테두리 글로우
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFB3E5FC).copy(alpha = 0.6f),
                            Color(0xFFE1BEE7).copy(alpha = 0.6f),
                            Color(0xFFC8E6C9).copy(alpha = 0.6f)
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White,
                            Color(0xFFF9FBFF)
                        )
                    )
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = getEmojiForIngredient(ingredient.name),
                    fontSize = 32.sp,
                    modifier = Modifier.graphicsLayer {
                        if (isDragging) rotationZ = 10f
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF424242),
                    maxLines = 1
                )
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F6FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = "${ingredient.quantity}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF3F51B5),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SmartTray(
    items: List<Ingredient>,
    onItemReturn: (Ingredient) -> Unit,
    onItemDelete: (Ingredient) -> Unit
) {
    val trayHeightCollapsed = 90.dp
    val trayHeightExpanded = 260.dp

    val trayHeightCollapsedPx = with(LocalDensity.current) { trayHeightCollapsed.toPx() }
    val trayHeightExpandedPx = with(LocalDensity.current) { trayHeightExpanded.toPx() }

    var offsetY by remember { mutableStateOf(0f) }
    var isExpanded by remember { mutableStateOf(false) }

    val trayHeightPx = trayHeightExpandedPx - trayHeightCollapsedPx
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isExpanded) 0f else trayHeightPx,
        animationSpec = spring(),
        label = "trayOffset"
    )

    // 배경 유리(Glass) 느낌
    Box(
        Modifier
            .fillMaxWidth()
            .height(trayHeightExpanded)
            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0x22000000), Color(0x11000000))),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        offsetY = (offsetY + dragAmount).coerceIn(0f, trayHeightPx)
                    },
                    onDragEnd = {
                        isExpanded = offsetY < trayHeightPx / 2
                        offsetY = 0f
                    }
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(
                Modifier
                    .size(width = 36.dp, height = 6.dp)
                    .background(Color.LightGray, RoundedCornerShape(3.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(6.dp))

            Row(
                Modifier.fillMaxWidth(),
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.1f))
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

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    items = items,
                    key = { it.id }
                ) { ingredient ->
                    FloatingIngredientChip(
                        ingredient = ingredient,
                        onReturn = {
                            onItemReturn(ingredient)
                            GlobalTray.items.removeAll { it.id == ingredient.id }
                        },
                        onDelete = { ing ->
                            onItemDelete(ing)
                            GlobalTray.items.removeAll { it.id == ing.id }
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
    onReturn: () -> Unit,
    onDelete: (Ingredient) -> Unit
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .size(width = 90.dp, height = 80.dp)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        Log.d("FridgeDebug", "Drag started on ${ingredient.name}")
                    },
                    onDragEnd = {
                        Log.d("FridgeDebug", "Drag ended on ${ingredient.name} y=${offset.y}")
                        if (offset.y < -100f) {
                            onReturn()
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
                alpha = if (isDragging) 0.9f else 1f
                scaleX = if (isDragging) 1.06f else 1f
                scaleY = if (isDragging) 1.06f else 1f
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            // 삭제버튼
            IconButton(
                onClick = { onDelete(ingredient) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "재료 삭제",
                    tint = Color(0xFFE57373)
                )
            }
            // 내용
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = getEmojiForIngredient(ingredient.name), fontSize = 24.sp)
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
}

@Composable
fun LoadingOverlay(
    tip: String,
    rawRes: Int
) {
    // 반투명 블러 느낌 오버레이
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCCFFFFFF))
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(rawRes))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            speed = 1.0f
        )

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lottie
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .size(140.dp)
                        .padding(top = 8.dp, bottom = 8.dp)
                )
                Spacer(Modifier.height(8.dp))
                // 텍스트 (5초마다 바뀜)
                Text(
                    text = tip,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF3949AB),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "잠시만 기다려 주세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF616161)
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    trackColor = Color(0xFFE8EAF6),
                    color = Color(0xFF3F51B5)
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

fun getEmojiForIngredient(name: String): String {
    return when (name) {
        // 단백질류
        "계란" -> "🥚"
        "소고기" -> "🥩"
        "닭고기", "치킨" -> "🍗"
        "돼지고기" -> "🥓"
        "생선", "참치", "연어" -> "🐟"
        "새우" -> "🦐"
        "오징어" -> "🦑"
        "문어" -> "🐙"
        "조개" -> "🦪"

        // 채소류
        "당근" -> "🥕"
        "상추", "양배추" -> "🥬"
        "브로콜리" -> "🥦"
        "감자" -> "🥔"
        "고구마" -> "🍠"
        "양파" -> "🧅"
        "마늘" -> "🧄"
        "고추", "피망" -> "🌶️"
        "토마토" -> "🍅"
        "버섯" -> "🍄"
        "호박" -> "🎃"
        "옥수수" -> "🌽"

        // 과일류
        "사과" -> "🍎"
        "바나나" -> "🍌"
        "수박" -> "🍉"
        "포도" -> "🍇"
        "딸기" -> "🍓"
        "키위" -> "🥝"
        "파인애플" -> "🍍"

        // 유제품·가공품
        "우유", "요거트" -> "🥛"
        "치즈" -> "🧀"
        "버터" -> "🧈"
        "아이스크림" -> "🍨"

        // 곡류·빵·간식
        "쌀", "밥" -> "🍚"
        "빵", "토스트" -> "🍞"
        "케이크" -> "🎂"
        "쿠키" -> "🍪"
        "초코", "초콜릿" -> "🍫"

        // 패스트푸드·간편식
        "피자" -> "🍕"
        "햄버거" -> "🍔"
        "핫도그", "소시지" -> "🌭"
        "샌드위치" -> "🥪"
        "타코" -> "🌮"
        "라면" -> "🍜"
        "스파게티", "파스타" -> "🍝"

        else -> "🍽️"
    }
}
