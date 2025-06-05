@file:OptIn(ExperimentalMaterial3Api::class)

package com.bcu.foodtable.JetpackCompose.Social

import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.math.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.wear.compose.material.ContentAlpha
import coil.compose.AsyncImage
import com.bcu.foodtable.R // 프로젝트의 R 임포트 경로 확인
import com.bcu.foodtable.ui.ChallengeScreenContent
import com.bcu.foodtable.useful.Comment
import com.bcu.foodtable.useful.CommunityPost
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.viewmodel.ChallengeViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit


// --- 애니메이션 스펙 정의 (DynamicRadialWheel에서 사용) ---
private val DefaultSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessVeryLow // 부드러움을 위해 강성 낮춤
)
private val FastSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow // 아이템 반응 속도 조절
)
private val AngleSpring = spring<Float>(
    dampingRatio = 0.8f, // 이전보다 약간 더 탄력 있게
    stiffness = Spring.StiffnessMediumLow
)
private val WheelExpansionSpring = spring<Dp>(
    dampingRatio = 0.65f, // 확장/축소 시 약간의 바운스
    stiffness = Spring.StiffnessMedium
)
private val ItemEntrySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)


//// --- LottieAnimationView (만약 실제 구현이 있다면 여기에 두거나, 별도 파일로 분리) ---
//@Composable
//fun LottieAnimationView(animationResId: Int, modifier: Modifier = Modifier) {
//    Box(modifier.background(Color.LightGray.copy(alpha = 0.5f))) {
//        Text("Lottie Animation Placeholder", Modifier.align(Alignment.Center))
//    }
//}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(navController: NavHostController) {
    @Stable
    data class WheelItem(
        val icon: ImageVector,
        val label: String,
        val screen: @Composable () -> Unit,
    )

    val wheelItems = listOf(
        WheelItem(Icons.Default.Forum, "커뮤니티") { // 아이콘 변경 제안
            CommunityTab(
                navToWrite = { navController.navigate("write") },
                navToDetail = { post -> navController.navigate("postDetail/${post.id}") }
            )
        },
        WheelItem(Icons.Default.RestaurantMenu, "오늘밥") { MiniGameTab(navController) }, // 아이콘 변경 제안
        WheelItem(Icons.Default.EmojiEvents, "랭킹") { ScreenStub("랭킹 탭") }, // 아이콘 변경 제안
        WheelItem(Icons.Default.MilitaryTech, "챌린지") { ChallengeTab()}, // 아이콘 변경 제안
        WheelItem(Icons.Default.People, "친구") { ScreenStub("친구 탭") }, // 아이콘 변경 제안
        WheelItem(Icons.Default.QuestionAnswer, "채팅") { ScreenStub("채팅 탭") }, // 아이콘 변경 제안
        WheelItem(Icons.Default.Map, "맛집도") { ScreenStub("맛집도 탭") }, // 아이콘 변경 제안
    )

    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }

    Surface(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)) // 컨텐츠 영역 배경
                ) {
                    Column {
                        if (wheelItems[selectedIndex].label in listOf("커뮤니티", "친구")) {
                            OutlinedTextField( // 스타일 약간 변경
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = { Text("어떤 이야기를 찾고 계신가요?") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") },
                                shape = CircleShape, // 완전한 원형
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        AnimatedVisibility(isLoading && wheelItems[selectedIndex].label != "커뮤니티") {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                LottieAnimationView(R.raw.loading, Modifier.size(200.dp))
                            }
                        }

                        if (!(isLoading && wheelItems[selectedIndex].label != "커뮤니티")) {
                            wheelItems[selectedIndex].screen()
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp), // 하단 여백 조정
                contentAlignment = Alignment.Center
            ) {
                DynamicRadialWheel(
                    items = wheelItems.map { it.icon to it.label },
                    haloColor = MaterialTheme.colorScheme.primary,
                    onSelectionChanged = { newIndex ->
                        if (selectedIndex != newIndex) {
                            selectedIndex = newIndex
                        }
                    },
                    initialSelectedIndex = selectedIndex
                )
            }
        }
    }
}

@Composable
fun DynamicRadialWheel(
    items: List<Pair<ImageVector, String>>,
    haloColor: Color,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedIndex: Int = 0,
    fabSize: Dp = 72.dp, // FAB 크기 증가
    expandedWheelDiameter: Dp = 320.dp, // 확장 시 지름 증가
    itemSize: Dp = 64.dp, // 아이템 크기 증가
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val itemSizePx = with(density) { itemSize.toPx() }
    val actualExpandedRadiusPx = with(density) {
        (expandedWheelDiameter / 2 - itemSize / 2).coerceAtLeast(itemSize + 8.dp).toPx() // 아이템간 간격 고려
    }

    val sliceAngle = if (items.isNotEmpty()) 360f / items.size else 0f
    var expanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    var currentRotationAngle by rememberSaveable {
        mutableStateOf((270f - sliceAngle * initialSelectedIndex).mod(360f))
    }
    var dragRotationOffset by remember { mutableStateOf(0f) }

    val animatedContainerSize by animateDpAsState(
        targetValue = if (expanded) expandedWheelDiameter else fabSize,
        animationSpec = WheelExpansionSpring, // 확장/축소 애니메이션 개선
        label = "containerSize"
    )

    // 부드러운 헤일로 효과를 위한 애니메이션 값들
    val infiniteTransition = rememberInfiniteTransition(label = "wheelEffects")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2200, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "haloScale"
    )
    // haloRotation은 Canvas 내부에서 직접 사용되지 않으므로 제거하거나, 필요 시 다른 방식으로 활용
    // val haloRotation by infiniteTransition.animateFloat(
    //     initialValue = 0f, targetValue = 360f,
    //     animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
    //     label = "haloBgRotation"
    // )
    val haloAlpha by animateFloatAsState(
        targetValue = if (expanded) 0.7f else 0f, // 확장 시에만 보이도록
        animationSpec = tween(500, easing = LinearOutSlowInEasing),
        label = "haloAlpha"
    )


    val displayRotation by animateFloatAsState(
        targetValue = currentRotationAngle + dragRotationOffset,
        animationSpec = AngleSpring,
        label = "displayRotation"
    )
    val fabElevation by animateDpAsState(targetValue = if (expanded) 2.dp else 6.dp, label = "fabElevation")


    Box(
        modifier = modifier
            .size(animatedContainerSize)
            .clip(CircleShape)
            .pointerInput(expanded, items.size) {
                if (expanded && items.isNotEmpty()) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { isDragging = true; dragRotationOffset = 0f },
                        onDrag = { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val previousPosition = change.previousPosition - center
                            val currentPosition = change.position - center
                            val previousAngleRad = atan2(previousPosition.y, previousPosition.x)
                            val currentAngleRad = atan2(currentPosition.y, currentPosition.x)
                            var angleDelta = Math.toDegrees((currentAngleRad - previousAngleRad).toDouble()).toFloat()

                            if (angleDelta > 180) angleDelta -= 360
                            if (angleDelta < -180) angleDelta += 360

                            dragRotationOffset = (dragRotationOffset + angleDelta * 0.8f).mod(360f) // 드래그 민감도 조절
                        },
                        onDragEnd = {
                            isDragging = false
                            currentRotationAngle = (currentRotationAngle + dragRotationOffset).mod(360f)
                            dragRotationOffset = 0f
                            val selectedIndex = calculateSelectedIndex(currentRotationAngle, sliceAngle, items.size)
                            currentRotationAngle = (270f - sliceAngle * selectedIndex).mod(360f)
                            onSelectionChanged(selectedIndex)
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // --- 혁신적인 배경 헤일로 ---
        if (expanded && items.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = haloAlpha }) {
                val canvasRadius = size.minDimension / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // 1. 부드럽게 회전하는 배경 그라데이션
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            haloColor.copy(alpha = 0.1f),
                            haloColor.copy(alpha = 0.25f),
                            haloColor.copy(alpha = 0.1f),
                            haloColor.copy(alpha = 0.05f), // 더 투명한 부분 추가
                            haloColor.copy(alpha = 0.1f)
                        ),
                        center = centerOffset
                    ),
                    radius = canvasRadius * haloScale, // 맥동하는 크기
                    center = centerOffset,
                    blendMode = BlendMode.Screen // 부드러운 혼합
                )

                // 2. 점선 스타일의 미세한 테두리 (선택적, "혁신적인" 느낌 가미)
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 25f), 0f)
                drawCircle(
                    color = haloColor.copy(alpha = 0.4f),
                    radius = canvasRadius * 0.9f * haloScale,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect),
                    center = centerOffset
                )
                // 3. 내부 소프트 글로우
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(haloColor.copy(alpha = 0.2f), Color.Transparent),
                        center = centerOffset,
                        radius = canvasRadius * 0.7f
                    ),
                    radius = canvasRadius * 0.7f,
                    center = centerOffset
                )
            }
        }

        // --- 아이템 ---
        val expansionFactor by animateFloatAsState(
            targetValue = if (expanded && items.isNotEmpty()) 1f else 0f,
            animationSpec = ItemEntrySpring,
            label = "itemExpansionFactor"
        )

        if (items.isNotEmpty()) { // AnimatedVisibility 대신 graphicsLayer alpha/scale 사용으로 더 부드럽게
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerCenter = Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)

                items.forEachIndexed { index, (icon, label) ->
                    val targetAngleDegrees = sliceAngle * index + displayRotation // 여기가 itemAngleDegrees의 원래 이름
                    val itemAngleRadians = Math.toRadians(targetAngleDegrees.toDouble())

                    // 확장 비율에 따라 아이템이 중심으로 모였다가 펼쳐지는 효과
                    val currentRadiusPx = actualExpandedRadiusPx * expansionFactor

                    val itemX = containerCenter.x + (cos(itemAngleRadians) * currentRadiusPx).toFloat()
                    val itemY = containerCenter.y + (sin(itemAngleRadians) * currentRadiusPx).toFloat()

                    val currentSelectedIndex = calculateSelectedIndex(displayRotation, sliceAngle, items.size)
                    val isSelected = index == currentSelectedIndex && expanded

                    // 선택된 아이템과의 각도 차이 (더 부드러운 알파 전환 로직)
                    val angleDiff = abs((targetAngleDegrees - (270f - sliceAngle * currentSelectedIndex + displayRotation)).mod(360f))
                    val normalizedAngleDiff = min(angleDiff, 360f - angleDiff)
                    val targetItemAlpha = (1f - (normalizedAngleDiff / 180f).pow(2)).coerceIn(0.1f, 1f) * expansionFactor


                    val itemAlpha by animateFloatAsState(
                        targetValue = if (isDragging && expanded) 0.7f else targetItemAlpha, // 드래그 중엔 약간 흐리게
                        animationSpec = tween(durationMillis = if(isDragging) 100 else 300),
                        label = "itemDetailAlpha"
                    )

                    val itemScale by animateFloatAsState(
                        targetValue = (if (isSelected) 1.2f else 1f) * expansionFactor,
                        animationSpec = FastSpring, label = "itemScale"
                    )
                    // 아이템 자체의 회전 (선택 시 정면, 아닐 시 약간 기울임)
                    val itemRotationZ by animateFloatAsState(
                        targetValue = if (isSelected || !expanded) 0f else (targetAngleDegrees - 270f), // itemAngleDegrees -> targetAngleDegrees 로 수정
                        animationSpec = AngleSpring,
                        label = "itemRotationZ"
                    )


                    Column(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = itemX - (itemSizePx / 2f)
                                translationY = itemY - (itemSizePx / 2f)
                                this.alpha = itemAlpha
                                scaleX = itemScale
                                scaleY = itemScale
                                rotationZ = itemRotationZ // 아이템 개별 회전
                            }
                            .size(itemSize + 24.dp) // 터치 영역 및 텍스트 공간 확보
                            .clickable(enabled = expanded) {
                                if (!isDragging) {
                                    currentRotationAngle = (270f - sliceAngle * index).mod(360f)
                                    onSelectionChanged(index)
                                    // expanded = false // 아이템 선택 시 자동으로 닫히도록 (선택 사항)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- 프리미엄 아이템 박스 ---
                        Box(
                            modifier = Modifier
                                .size(itemSize)
                                .shadow( // 그림자 더욱 입체적으로
                                    elevation = if (isSelected) 18.dp else 6.dp,
                                    shape = CircleShape,
                                    ambientColor = if (isSelected) haloColor.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.2f),
                                    spotColor = if (isSelected) haloColor else Color.Black.copy(alpha = 0.4f)
                                )
                                .clip(CircleShape)
                                .background( // 글래스모피즘 배경 개선
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            (if (isSelected) haloColor else MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)).copy(alpha = if (isSelected) 0.9f else 0.75f),
                                            (if (isSelected) haloColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)).copy(alpha = if (isSelected) 0.7f else 0.5f)
                                        ),
                                        radius = itemSizePx * 1.1f // 그라데이션 범위 확장
                                    )
                                )
                                .border( // 테두리 스타일 변경
                                    width = if (isSelected) 2.dp else 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = if (isSelected) listOf(haloColor.copy(alpha = 1f), haloColor.copy(alpha = 0.6f))
                                        else listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon, contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.contentColorFor(haloColor)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(itemSize * 0.5f) // 아이콘 크기는 유지 또는 약간 작게
                            )
                        }

                        // --- 선택된 아이템 레이블 애니메이션 ---
                        AnimatedVisibility(
                            visible = isSelected && expanded && expansionFactor > 0.8f, // 완전히 확장되었을 때 표시
                            enter = fadeIn(tween(150, delayMillis = 100)) + slideInVertically(tween(200, delayMillis = 50)) { it / 2 },
                            exit = fadeOut(tween(100)) + slideOutVertically(tween(150)) { it / 2 }
                        ) {
                            Text(
                                text = label, style = MaterialTheme.typography.labelMedium,
                                color = haloColor, modifier = Modifier.padding(top = 8.dp), // 패딩 증가
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold // 약간 더 굵게
                            )
                        }
                    }
                }
            }
        }

        // --- 중앙 FAB ---
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = if (expanded && items.isNotEmpty()) {
                MaterialTheme.colorScheme.surface // 확장 시 FAB은 더 중립적인 색으로
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentColor = if (expanded && items.isNotEmpty()) {
                haloColor // 확장 시 아이콘은 헤일로 색상으로
            } else {
                MaterialTheme.colorScheme.onPrimary
            },
            shape = CircleShape,
            modifier = Modifier.size(fabSize),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = fabElevation)
        ) {
            val iconRotation by animateFloatAsState(
                targetValue = if (expanded) 225f else 0f, // X 모양으로 더 확실히 회전
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "fabIconRotation"
            )
            Icon(
                Icons.Default.Add,
                contentDescription = if (expanded) "휠 닫기" else "휠 열기",
                modifier = Modifier.rotate(iconRotation)
            )
        }
    }
}

private fun calculateSelectedIndex(currentRotationDegrees: Float, sliceAngleDegrees: Float, itemCount: Int): Int {
    if (itemCount == 0) return -1
    if (sliceAngleDegrees == 0f) return 0
    val normalizedAngle = ( (270f - currentRotationDegrees).mod(360f) + (sliceAngleDegrees / 2f) ).mod(360f)
    return (normalizedAngle / sliceAngleDegrees).toInt().coerceIn(0, itemCount - 1)
}

private fun Float.pow(n: Int): Float {
    var result = 1.0
    var base = this.toDouble()
    var exp = n
    if (exp < 0) { base = 1.0 / base; exp = -exp }
    while (exp > 0) {
        if (exp % 2 == 1) result *= base
        base *= base
        exp /= 2
    }
    return result.toFloat()
}

// SineEaseInOut EasingFunction 정의 (애니메이션에 사용)
val SineEaseInOut = Easing { fraction ->
    (1 - cos(fraction * PI.toFloat())) / 2
}


// (이하 SocialScreen.kt의 나머지 코드는 동일하게 유지됩니다)
// ... ScreenStub, CommunityTab, CommunityPostItem, Timestamp.toRelativeTime, 등등 ...

@Composable
private fun ScreenStub(name: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(
            name,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
    }
}
@Composable
fun CommunityTab(
    navToWrite: () -> Unit = {},
    navToDetail: (CommunityPost) -> Unit = {}
) {
    val userLocation = remember { UserManager.getUser()?.location ?: "알 수 없음" } // Null 처리 추가
    var selectedTab by rememberSaveable { mutableStateOf("전체") }
    var sortOption by rememberSaveable { mutableStateOf("조회순") }

    var posts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedTab, sortOption) { // selectedTab이나 sortOption 변경 시 재로딩
        isLoading = true
        posts = loadPostsFromFirebase().filter { post -> // 필터링 로직을 LaunchedEffect 내부로 이동
            when (selectedTab) {
                "전체" -> true
                "지역" -> post.location == userLocation
                else -> true
            }
        }.sortedWith( // 정렬 로직도 내부로 이동
            when (sortOption) {
                "조회순" -> compareByDescending { it.visited }
                "최신순" -> compareByDescending { it.createdAt }
                "추천순" -> compareByDescending { it.likes }
                else -> compareByDescending { it.visited }
            }
        )
        isLoading = false
    }

    val tabOptions = listOf("전체", "지역")
    val sortOptions = listOf("조회순", "최신순", "추천순")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabOptions.indexOf(selectedTab)) {
            tabOptions.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sortOptions.forEach {
                FilterChip(
                    selected = sortOption == it,
                    onClick = { sortOption = it },
                    label = { Text(it) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = navToWrite) {
                Icon(
                    Icons.Default.Create,
                    contentDescription = "글쓰기",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("표시할 게시글이 없어요. 😥", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(posts, key = { it.id }) { post ->
                        CommunityPostItem(post) { navToDetail(post) }
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityPostItem(post: CommunityPost, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            if (post.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "게시글 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.height(12.dp))
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(post.location, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(post.createdAt.toRelativeTime(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(8.dp))

            Text(post.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoIconWithText(icon = Icons.Default.Visibility, text = "${post.visited}")
                InfoIconWithText(icon = Icons.Default.ThumbUp, text = "${post.likes}")
                InfoIconWithText(icon = Icons.Default.BookmarkBorder, text = "${post.bookmarks}")
                InfoIconWithText(icon = Icons.Default.ChatBubbleOutline, text = "${post.comments}")
            }
        }
    }
}

@Composable
private fun InfoIconWithText(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun Timestamp.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this.toDate().time
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        hours < 24 -> "${hours}시간 전"
        days < 7 -> "${days}일 전"
        else -> SimpleDateFormat("yy.MM.dd", Locale.getDefault()).format(this.toDate())
    }
}

suspend fun loadPostsFromFirebase(): List<CommunityPost> = withContext(Dispatchers.IO) {
    try {
        val communityRef = Firebase.firestore.collection("community")
        val snapshot = communityRef.get().await()

        val posts = snapshot.documents.mapNotNull { doc ->
            doc.toObject(CommunityPost::class.java)?.copy(id = doc.id)
        }

        posts.map { post ->
            val commentSnapshot = communityRef.document(post.id).collection("comments").get().await()
            post.copy(comments = commentSnapshot.size())
        }
    } catch (e: Exception) {
        Log.e("FirebaseLoad", "Error loading posts: ${e.message}", e)
        emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritePostScreen(
    onPostCreated: () -> Unit = {},
    navController: NavController
) {
    val user = UserManager.getUser()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var titleFocused by remember { mutableStateOf(false) }
    var contentFocused by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        imageUris = (imageUris + uris).take(5)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "이미지 선택 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    if (user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("글을 작성하려면 로그인이 필요합니다.")
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TopAppBar(
            title = { Text("새 게시글 작성", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            isUploading = true
                            uploadPost(title, content, user.location, user.uid, imageUris) { success ->
                                isUploading = false
                                if (success) {
                                    Toast.makeText(context, "게시글이 작성되었습니다!", Toast.LENGTH_SHORT).show()
                                    onPostCreated()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "업로드 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isUploading && title.isNotBlank()
                ) {
                    if (isUploading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("완료", fontWeight = FontWeight.Bold, color = if (title.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StyledWriteSection(title = "제목", icon = Icons.Default.Title, focused = titleFocused) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        placeholder = { Text("멋진 제목을 붙여주세요!", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { titleFocused = it.isFocused },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        maxLines = 2, textStyle = MaterialTheme.typography.titleMedium
                    )
                }
            }
            item {
                StyledWriteSection(title = "내용", icon = Icons.Default.Description, focused = contentFocused) {
                    OutlinedTextField(
                        value = content, onValueChange = { content = it },
                        placeholder = { Text("자세한 내용을 공유해주세요...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp).onFocusChanged { contentFocused = it.isFocused },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            item {
                StyledWriteSection(title = "사진 첨부 (${imageUris.size}/5)", icon = Icons.Default.ImageSearch) {
                    if (imageUris.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                            items(imageUris) { uri -> ImagePreviewItem(uri) { imageUris = imageUris - uri } }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.READ_MEDIA_IMAGES else android.Manifest.permission.READ_EXTERNAL_STORAGE
                            permissionLauncher.launch(permission)
                        },
                        modifier = Modifier.fillMaxWidth(), enabled = imageUris.size < 5,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(8.dp))
                        Text(if (imageUris.isEmpty()) "사진 선택하기" else "사진 추가 (${imageUris.size}/5)")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("게시 위치: ${user.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun StyledWriteSection(
    title: String,
    icon: ImageVector,
    focused: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (focused) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(if (focused) 3.dp else 1.dp)),
        border = if (focused) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun ImagePreviewItem(uri: Uri, onRemove: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        AsyncImage(
            model = uri, contentDescription = "선택된 이미지",
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp).padding(4.dp)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "이미지 삭제", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

fun uploadPost(
    title: String, content: String, location: String, userId: String,
    imageUris: List<Uri>, onComplete: (Boolean) -> Unit
) {
    val firestore = Firebase.firestore
    val storage = Firebase.storage
    val postRef = firestore.collection("community").document()

    if (imageUris.isEmpty()) {
        val postData = mapOf(
            "title" to title, "content" to content, "location" to location,
            "imageUrl" to "", "imageUrls" to emptyList<String>(),
            "visited" to 0, "likes" to 0, "bookmarks" to 0, "comments" to 0,
            "createdAt" to Timestamp.now(), "userId" to userId
        )
        postRef.set(postData).addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
        return
    }

    val uploadImageTasks = imageUris.mapIndexed { idx, uri ->
        val imageRef = storage.reference.child("posts/${postRef.id}/img_$idx.jpg")
        imageRef.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            imageRef.downloadUrl
        }
    }

    Tasks.whenAllSuccess<Uri>(uploadImageTasks).addOnSuccessListener { uris ->
        val postData = mapOf(
            "title" to title, "content" to content, "location" to location,
            "imageUrl" to if (uris.isNotEmpty()) uris.first().toString() else "",
            "imageUrls" to uris.map { it.toString() },
            "visited" to 0, "likes" to 0, "bookmarks" to 0, "comments" to 0,
            "createdAt" to Timestamp.now(), "userId" to userId
        )
        postRef.set(postData).addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }.addOnFailureListener {
        Log.e("UploadPost", "Image upload failed", it)
        onComplete(false)
    }
}

fun loadComments(postId: String): Flow<List<Comment>> = callbackFlow {
    val ref = Firebase.firestore.collection("community").document(postId).collection("comments")
    val listener = ref.orderBy("createdAt").addSnapshotListener { snapshot, e ->
        if (e != null) {
            Log.w("LoadComments", "Listen failed.", e)
            close(e)
            return@addSnapshotListener
        }
        val comments = snapshot?.documents?.mapNotNull {
            it.toObject(Comment::class.java)?.copy(id = it.id)
        } ?: emptyList()
        trySend(comments).isSuccess
    }
    awaitClose { listener.remove() }
}

fun commentCountFlow(postId: String): Flow<Int> = callbackFlow {
    val ref = Firebase.firestore.collection("community").document(postId).collection("comments")
    val listener = ref.addSnapshotListener { snapshot, e ->
        if (e != null) {
            Log.w("CommentCount", "Listen failed.", e)
            close(e)
            return@addSnapshotListener
        }
        trySend(snapshot?.size() ?: 0).isSuccess
    }
    awaitClose { listener.remove() }
}

@Composable
fun MiniGameTab(navController: NavController? = null) {
    val gameTabs = listOf("메뉴 정하기", "누가 낼까?")
    var selectedTab by rememberSaveable { mutableStateOf(gameTabs.first()) }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = gameTabs.indexOf(selectedTab),
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            gameTabs.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(modifier = Modifier.padding(16.dp)) {
            when (selectedTab) {
                "메뉴 정하기" -> MenuGameList(navController)
                "누가 낼까?" -> PayerGameList(navController)
            }
        }
    }
}

@Composable
fun MenuGameList(navController: NavController? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("메뉴 정하기 게임", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        GameButton("룰렛 돌리기", Icons.Default.Casino) { navController?.navigate("rouletteGame") }
        GameButton("음식 카드 뽑기", Icons.Default.Style) { navController?.navigate("cardGame") }
    }
}

@Composable
fun PayerGameList(navController: NavController? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("누가 돈을 낼까요?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        GameButton("사다리 타기", Icons.Default.DeviceHub) { navController?.navigate("ladderGame") }
        GameButton("결제자 룰렛", Icons.Default.Payments) { navController?.navigate("payerRouletteGame") }
    }
}

@Composable
fun GameButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ChallengeTab() {
    val viewModel: ChallengeViewModel = viewModel()
    val challenges by viewModel.challenges.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val userSalt by viewModel.userSalt.collectAsState()

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = "오류", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("⚠ ${error ?: "챌린지 정보를 불러오는 중 오류가 발생했어요."}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
        else -> ChallengeScreenContent(
            challenges = challenges,
            salt = userSalt,
            onProgressUpdate = { id, value -> viewModel.updateProgress(id, value) },
            onStartChallenge = { id -> viewModel.startChallenge(id) }
        )
    }
}
