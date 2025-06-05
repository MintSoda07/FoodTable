package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
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
            isOpen -> -130f
            dragOffset > 0 -> (-130f * (dragOffset / 200f)).coerceIn(-130f, 0f)
            else -> 0f
        },
        animationSpec = if (isClosing) {
            spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "doorRotation"
    )

    val doorShadow by animateFloatAsState(
        targetValue = if (isOpen) 24f else 8f,
        animationSpec = tween(600),
        label = "doorShadow"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isOpen) 500 else 200,
            delayMillis = if (isOpen) 100 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "contentAlpha"
    )

    val coldAirAlpha by animateFloatAsState(
        targetValue = if (showColdEffect && isOpen) 0.7f else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = LinearOutSlowInEasing
        ),
        finishedListener = { showColdEffect = false },
        label = "coldAirAlpha"
    )

    // 냉장고 내부 빛 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "light")
    val lightPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lightPulse"
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E27)) // 다크 네이비 배경
    ) {
        // 배경 그라데이션 효과
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E3A5F).copy(alpha = 0.3f),
                        Color(0xFF0A0E27)
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.3f),
                    radius = size.width
                )
            )
        }

        // 메인 냉장고 컨테이너
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .align(Alignment.Center)
                .graphicsLayer {
                    // 3D 원근감
                    rotationY = if (isOpen) 8f else 0f
                    cameraDistance = 12f * density.density
                }
        ) {
            // 냉장고 본체 (내부) - 글래스모피즘 효과
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.95f
                        shadowElevation = 16f
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1F3A).copy(alpha = 0.9f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF3D5AFE).copy(alpha = 0.3f)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 내부 조명 효과
                    if (isOpen) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = contentAlpha * lightPulse }
                        ) {
                            // 상단 LED 조명
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFE3F2FD).copy(alpha = 0.6f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.4f
                                )
                            )

                            // 측면 조명
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFBBDEFB).copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Color(0xFFBBDEFB).copy(alpha = 0.3f)
                                    )
                                )
                            )
                        }
                    }

                    // 냉장고 내부 컨텐츠
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = contentAlpha }
                            .padding(20.dp)
                    ) {
                        // 온도 표시 및 섹션 선택
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 온도 디스플레이
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF00E5FF).copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Thermostat,
                                        contentDescription = "Temperature",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when(selectedSection) {
                                            "냉장" -> "3°C"
                                            "냉동" -> "-18°C"
                                            else -> "7°C"
                                        },
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            // 섹션 토글 버튼
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1E88E5).copy(alpha = 0.1f))
                                    .border(
                                        1.dp,
                                        Color(0xFF1E88E5).copy(alpha = 0.3f),
                                        RoundedCornerShape(20.dp)
                                    )
                            ) {
                                fridgeSections.forEach { section ->
                                    val isSelected = selectedSection == section
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .then(
                                                if (isSelected) {
                                                    Modifier.background(
                                                        Brush.horizontalGradient(
                                                            colors = listOf(
                                                                Color(0xFF1E88E5),
                                                                Color(0xFF1976D2)
                                                            )
                                                        )
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable {
                                                selectedSection = section
                                                if (section == "냉동") showColdEffect = true
                                            }
                                            .padding(horizontal = 20.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = section,
                                            color = if (isSelected) Color.White else Color(0xFF90CAF9),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 재료 그리드 with 홀로그램 효과
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(
                                items = fridgeMap[selectedSection] ?: emptyList(),
                                key = { _, item -> item.id }
                            ) { index, ingredient ->
                                HolographicIngredientCard(
                                    ingredient = ingredient,
                                    index = index,
                                    onClick = {
                                        moveIngredientToOutside(ingredient, selectedSection)
                                    },
                                    onLongClick = { showDialog.value = ingredient }
                                )
                            }
                        }
                    }

                    // 차가운 공기 효과 (냉동칸 선택 시)
                    if (showColdEffect) {
                        ColdAirEffect(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = coldAirAlpha }
                        )
                    }
                }
            }

            // 하이테크 냉장고 문
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
                                if (isOpen) {
                                    showColdEffect = selectedSection == "냉동"
                                    scope.launch {
                                        kotlinx.coroutines.delay(100)
                                        isClosing = false
                                    }
                                }
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
                                kotlinx.coroutines.delay(500)
                                isOpen = false
                                isClosing = false
                            }
                        } else {
                            isOpen = true
                            showColdEffect = selectedSection == "냉동"
                        }
                    }
            ) {
                // 문 외관 - 미래지향적 디자인
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        width = 2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF64B5F6),
                                Color(0xFF1976D2),
                                Color(0xFF0D47A1)
                            )
                        )
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 메탈릭 그라데이션
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF37474F),
                                        Color(0xFF263238),
                                        Color(0xFF1C2833)
                                    )
                                )
                            )

                            // 디지털 패턴
                            val patternSize = 40.dp.toPx()
                            for (x in 0..size.width.toInt() step patternSize.toInt()) {
                                for (y in 0..size.height.toInt() step patternSize.toInt()) {
                                    drawCircle(
                                        color = Color(0xFF42A5F5).copy(alpha = 0.05f),
                                        radius = 2.dp.toPx(),
                                        center = Offset(x.toFloat(), y.toFloat())
                                    )
                                }
                            }
                        }

                        // 스마트 디스플레이
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 디지털 시계
                            val currentTime = remember { derivedStateOf { java.time.LocalTime.now() } }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", currentTime.value.hour, currentTime.value.minute),
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Light
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 로고
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = "Smart Fridge",
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier
                                    .size(64.dp)
                                    .graphicsLayer {
                                        rotationZ = if (isOpen) 180f else 0f
                                    }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "SMART FRIDGE",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF90CAF9),
                                fontWeight = FontWeight.Thin,
                                letterSpacing = 4.sp
                            )
                        }

                        // 터치 가이드 (홀로그램 효과)
                        if (!isOpen) {
                            val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
                            val shimmerAlpha by shimmerTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 0.8f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "shimmerAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(y = 80.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF00E5FF).copy(alpha = shimmerAlpha * 0.3f),
                                                Color(0xFF00E5FF).copy(alpha = shimmerAlpha * 0.1f),
                                                Color(0xFF00E5FF).copy(alpha = shimmerAlpha * 0.3f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Color(0xFF00E5FF).copy(alpha = shimmerAlpha),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .padding(horizontal = 32.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = "Touch",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "TOUCH TO OPEN",
                                        color = Color(0xFF00E5FF),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }

                        // 미래적인 손잡이
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(160.dp)
                                .align(Alignment.CenterEnd)
                                .offset(x = (-32).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF42A5F5),
                                            Color(0xFF1E88E5),
                                            Color(0xFF1565C0)
                                        )
                                    )
                                )
                        ) {
                            // LED 인디케이터
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .align(Alignment.Center)
                                    .background(
                                        if (isOpen) Color(0xFF00E676) else Color(0xFF448AFF)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // 하단 스마트 트레이 (꺼낸 재료)
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

        // 플로팅 액션 버튼 - 네온 효과
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
        ) {
            // 네온 글로우
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .blur(20.dp)
                    .background(
                        Color(0xFF00E5FF).copy(alpha = 0.6f),
                        CircleShape
                    )
            )

            FloatingActionButton(
                onClick = {
                    navController.navigate("add_ingredient?section=$selectedSection")
                },
                containerColor = Color(0xFF1976D2),
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "재료 추가",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    // 추천 요리 다이얼로그
    showDialog.value?.let { selectedIngredient ->
        FuturisticDialog(
            ingredient = selectedIngredient,
            recipes = viewModel.findRecipesByIngredient(selectedIngredient.name),
            onDismiss = { showDialog.value = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HolographicIngredientCard(
    ingredient: Ingredient,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // 홀로그램 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "hologram")
    val hologramOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hologramOffset"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationY = sin(hologramOffset * 2 * PI.toFloat()) * 5f
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A237E).copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.8f),
                    Color(0xFF00E5FF).copy(alpha = 0.2f),
                    Color(0xFF00E5FF).copy(alpha = 0.8f)
                ),
                start = Offset(0f, 0f),
                end = Offset(100f * hologramOffset, 100f * hologramOffset)
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 홀로그램 스캔라인 효과
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.3f }
            ) {
                val lineY = size.height * hologramOffset
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00E5FF),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // 3D 이모지 효과
                Text(
                    text = getEmojiForIngredient(ingredient.name),
                    fontSize = 36.sp,
                    modifier = Modifier.graphicsLayer {
                        shadowElevation = 8f
                    }
                )

                // 이름 (글리치 효과)
                Box {
                    Text(
                        text = ingredient.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00E5FF).copy(alpha = 0.3f),
                        modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                    )
                    Text(
                        text = ingredient.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // 수량 디지털 디스플레이
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${ingredient.quantity}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ColdAirEffect(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        // 차가운 안개 효과
        val mistParticles = 50
        for (i in 0 until mistParticles) {
            val x = size.width * kotlin.random.Random.nextFloat()
            val y = size.height * kotlin.random.Random.nextFloat()
            val radius = (20..60).random().toFloat()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD).copy(alpha = 0.3f),
                        Color(0xFFBBDEFB).copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(x, y),
                    radius = radius
                ),
                radius = radius,
                center = Offset(x, y)
            )
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
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D47A1).copy(alpha = 0.95f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF42A5F5),
                    Color(0xFF1976D2)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 애니메이션 아이콘
                    val rotation by rememberInfiniteTransition(label = "trayIcon").animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "iconRotation"
                    )

                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = "Smart Tray",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "SMART TRAY",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // 카운터
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${items.size}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 아이템 그리드
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items.size) { index ->
                    val ingredient = items[index]
                    FloatingIngredientChip(
                        ingredient = ingredient,
                        onReturn = { onItemReturn(ingredient) }
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

    // 플로팅 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    Card(
        modifier = Modifier
            .size(width = 100.dp, height = 90.dp)
            .offset {
                IntOffset(
                    offset.x.toInt(),
                    (offset.y + if (!isDragging) floatY else 0f).toInt()
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        if (offset.y < -150f) {
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
                alpha = if (isDragging) 0.8f else 1f
                scaleX = if (isDragging) 1.15f else 1f
                scaleY = if (isDragging) 1.15f else 1f
                shadowElevation = if (isDragging) 16f else 8f
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E88E5).copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFF42A5F5).copy(alpha = if (isDragging) 1f else 0.6f)
        )
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
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ingredient.name,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1
            )

            // 드래그 힌트
            if (!isDragging && offset == Offset.Zero) {
                Icon(
                    imageVector = Icons.Default.SwipeUp,
                    contentDescription = "Swipe up",
                    tint = Color(0xFF00E5FF).copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun FuturisticDialog(
    ingredient: Ingredient,
    recipes: List<String>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0A0E27).copy(alpha = 0.95f)
            ),
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFF1976D2),
                        Color(0xFF00E5FF)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 헤더
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI RECIPE SUGGESTION",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${ingredient.name} 활용 레시피",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF90CAF9)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 레시피 리스트
                recipes.forEach { recipe ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1976D2).copy(alpha = 0.2f),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFF42A5F5).copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = recipe,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 닫기 버튼
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF00E5FF)
                    )
                ) {
                    Text(
                        text = "CLOSE",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

fun getEmojiForIngredient(name: String): String {
    return when (name) {
        "계란" -> "🥚"
        "당근" -> "🥕"
        "상추" -> "🥬"
        "소고기" -> "🥩"
        "양파" -> "🧅"
        "감자" -> "🥔"
        "우유" -> "🥛"
        "치즈" -> "🧀"
        "토마토" -> "🍅"
        "사과" -> "🍎"
        "바나나" -> "🍌"
        "빵" -> "🍞"
        "닭고기" -> "🍗"
        "돼지고기" -> "🥓"
        "생선" -> "🐟"
        "새우" -> "🦐"
        "버섯" -> "🍄"
        "브로콜리" -> "🥦"
        "옥수수" -> "🌽"
        "고추" -> "🌶️"
        "버터" -> "🧈"
        "요거트" -> "🥛"
        "아이스크림" -> "🍨"
        else -> "🍽️"
    }
}