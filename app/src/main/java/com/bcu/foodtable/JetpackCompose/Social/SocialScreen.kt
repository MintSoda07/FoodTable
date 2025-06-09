@file:OptIn(ExperimentalMaterial3Api::class)

package com.bcu.foodtable.JetpackCompose.Social // 기존 패키지 경로 유지

import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.math.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.bcu.foodtable.R
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

// --- 애니메이션 스펙 정의 (기존과 동일하게 유지) ---
private val DefaultSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow)
private val FastSpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
private val AngleSpring = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
private val WheelExpansionSpring = spring<Dp>(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
private val ItemEntrySpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)




@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class) // AnimatedContent를 위한 Opt-in 추가
@Composable
fun SocialScreen(navController: NavHostController) {
    // WheelItem 데이터 클래스 정의
    @Stable
    data class WheelItem(
        val icon: ImageVector,
        val label: String,
        val screen: @Composable () -> Unit,
    )

    val wheelItems = remember {
        listOf(
            WheelItem(Icons.Default.Forum, "커뮤니티") {
                CommunityTab(
                    navToWrite = { navController.navigate("write") },
                    navToDetail = { post -> navController.navigate("postDetail/${post.id}") }
                )
            },
            WheelItem(Icons.Default.RestaurantMenu, "오늘밥") { MiniGameTab(navController) },
            WheelItem(Icons.Default.EmojiEvents, "랭킹") { ScreenStub("랭킹 탭") },
            WheelItem(Icons.Default.MilitaryTech, "챌린지") { ChallengeTab() },
            WheelItem(Icons.Default.People, "친구") { FriendsTab() },
            WheelItem(Icons.Default.QuestionAnswer, "채팅") { ChatTab() },
            WheelItem(Icons.Default.Map, "맛집도")   {
                // 이제 여기서는 단순히 지도를 렌더링만
                RestaurantV2MapScreen(modifier = Modifier.fillMaxSize())
            },
        )
    }

    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    var searchText by rememberSaveable { mutableStateOf("") }

    Surface(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- 개선된 부분: 컨텐츠 영역 ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    Column {
                        // 검색바 (기존 로직 유지)
                        AnimatedVisibility(visible = wheelItems[selectedIndex].label in listOf("커뮤니티", "친구")) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = { Text("어떤 이야기를 찾고 계신가요?") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") },
                                shape = CircleShape,
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

                        // --- 개선된 부분: 부드러운 화면 전환 애니메이션 ---
                        AnimatedContent(
                            targetState = selectedIndex,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300, 50)) togetherWith
                                        fadeOut(animationSpec = tween(300))
                            },
                            label = "screenTransition"
                        ) { targetIndex ->
                            wheelItems[targetIndex].screen()
                        }
                    }
                }
            }

            // --- 다이나믹 휠 (UI 개선) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                DynamicRadialWheel(
                    items = wheelItems.map { it.icon to it.label },
                    haloColor = MaterialTheme.colorScheme.primary, // 테마 색상 사용
                    onSelectionChanged = { newIndex ->
                        if (wheelItems[newIndex].label == "맛집도") {
                            navController.navigate("matzip")
                        } else {
                            selectedIndex = newIndex
                        }
                    },
                    initialSelectedIndex = selectedIndex
                )
            }
        }
    }
}

// --- DynamicRadialWheel 컴포저블 (디자인 개선) ---
@Composable
fun DynamicRadialWheel(
    items: List<Pair<ImageVector, String>>,
    haloColor: Color,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedIndex: Int = 0,
    fabSize: Dp = 72.dp,
    expandedWheelDiameter: Dp = 320.dp,
    itemSize: Dp = 64.dp,
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val itemSizePx = with(density) { itemSize.toPx() }
    val actualExpandedRadiusPx = with(density) { (expandedWheelDiameter / 2 - itemSize / 2).coerceAtLeast(itemSize + 8.dp).toPx() }

    val sliceAngle = 360f / items.size
    var expanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    var currentRotationAngle by rememberSaveable { mutableStateOf((270f - sliceAngle * initialSelectedIndex).mod(360f)) }
    var dragRotationOffset by remember { mutableStateOf(0f) }

    val animatedContainerSize by animateDpAsState(targetValue = if (expanded) expandedWheelDiameter else fabSize, animationSpec = WheelExpansionSpring, label = "containerSize")

    // --- 개선된 부분: 헤일로 효과 애니메이션 ---
    val infiniteTransition = rememberInfiniteTransition(label = "wheelHaloTransition")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = SineEaseInOut), RepeatMode.Reverse), label = "haloScale"
    )
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart), label = "haloRotation"
    )
    val haloAlpha by animateFloatAsState(targetValue = if (expanded) 0.8f else 0f, animationSpec = tween(500), label = "haloAlpha")


    val displayRotation by animateFloatAsState(targetValue = currentRotationAngle + dragRotationOffset, animationSpec = AngleSpring, label = "displayRotation")
    val fabElevation by animateDpAsState(targetValue = if (expanded) 2.dp else 8.dp, label = "fabElevation") // 그림자 변경

    Box(
        modifier = modifier
            .size(animatedContainerSize)
            .clip(CircleShape)
            .pointerInput(expanded, items.size) { /* 기존 제스처 로직 유지 */
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

                            dragRotationOffset = (dragRotationOffset + angleDelta * 0.8f).mod(360f)
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
        // --- 개선된 부분: 배경 헤일로 렌더링 ---
        if (expanded && items.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize().rotate(haloRotation).alpha(haloAlpha)) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                val canvasRadius = size.minDimension / 2f
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            haloColor.copy(alpha = 0.05f),
                            haloColor.copy(alpha = 0.3f),
                            haloColor.copy(alpha = 0.05f),
                        ),
                        center = centerOffset
                    ),
                    radius = canvasRadius * haloScale
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(haloColor.copy(alpha = 0.2f), Color.Transparent),
                        center = centerOffset,
                        radius = canvasRadius
                    ),
                    radius = canvasRadius
                )
            }
        }

        val expansionFactor by animateFloatAsState(targetValue = if (expanded && items.isNotEmpty()) 1f else 0f, animationSpec = ItemEntrySpring, label = "itemExpansionFactor")

        if (items.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerCenter = Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)
                items.forEachIndexed { index, (icon, label) ->
                    val targetAngleDegrees = sliceAngle * index + displayRotation
                    val itemAngleRadians = Math.toRadians(targetAngleDegrees.toDouble())
                    val currentRadiusPx = actualExpandedRadiusPx * expansionFactor
                    val itemX = containerCenter.x + (cos(itemAngleRadians) * currentRadiusPx).toFloat()
                    val itemY = containerCenter.y + (sin(itemAngleRadians) * currentRadiusPx).toFloat()
                    val currentSelectedIndex = calculateSelectedIndex(displayRotation, sliceAngle, items.size)
                    val isSelected = index == currentSelectedIndex && expanded

                    // --- 개선된 부분: 아이템 UI 스타일 ---
                    val itemScale by animateFloatAsState(targetValue = (if (isSelected) 1.2f else 1f) * expansionFactor, animationSpec = FastSpring, label = "itemScale")
                    val itemAlpha by animateFloatAsState(targetValue = if (expanded) 1f else 0f, animationSpec = tween(300), label = "itemAlpha")
                    val itemRotationZ by animateFloatAsState(targetValue = if (isSelected || !expanded) 0f else (targetAngleDegrees - 270f), animationSpec = AngleSpring, label = "itemRotationZ")

                    Column(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = itemX - (itemSizePx / 2f)
                                translationY = itemY - (itemSizePx / 2f)
                                this.alpha = itemAlpha
                                scaleX = itemScale
                                scaleY = itemScale
                                rotationZ = itemRotationZ
                            }
                            .size(itemSize + 24.dp)
                            .clickable(enabled = expanded) {
                                if (!isDragging) {
                                    currentRotationAngle = (270f - sliceAngle * index).mod(360f)
                                    onSelectionChanged(index)
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- 개선된 부분: 글래스모피즘 아이템 박스 ---
                        Box(
                            modifier = Modifier
                                .size(itemSize)
                                .shadow(
                                    elevation = if (isSelected) 16.dp else 4.dp,
                                    shape = CircleShape,
                                    spotColor = if (isSelected) haloColor else Color.Black
                                )
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(if (isSelected) 8.dp else 4.dp).copy(alpha = 0.8f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = if (isSelected) listOf(haloColor, haloColor.copy(alpha = 0.5f))
                                        else listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon, contentDescription = label,
                                tint = if (isSelected) haloColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(itemSize * 0.5f)
                            )
                        }

                        AnimatedVisibility(
                            visible = isSelected && expanded && expansionFactor > 0.9f,
                            enter = fadeIn(tween(150, 100)) + slideInVertically(tween(200, 50)) { it / 2 },
                            exit = fadeOut(tween(100)) + slideOutVertically(tween(150)) { it / 2 }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = haloColor,
                                modifier = Modifier.padding(top = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- 중앙 FAB (기존과 유사, 아이콘 회전 개선) ---
        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = if (expanded && items.isNotEmpty()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
            contentColor = if (expanded && items.isNotEmpty()) haloColor else MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.size(fabSize),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = fabElevation)
        ) {
            val iconRotation by animateFloatAsState(targetValue = if (expanded) 135f else 0f, animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "fabIconRotation")
            Icon(Icons.Default.Add, contentDescription = if (expanded) "휠 닫기" else "휠 열기", modifier = Modifier.rotate(iconRotation))
        }
    }
}

// 나머지 유틸리티 함수들 (calculateSelectedIndex, pow, SineEaseInOut, ScreenStub, CommunityTab, ...)
// 기존 코드와 동일하게 유지됩니다.
// ...

// --- 기존 코드의 나머지 부분을 여기에 그대로 붙여넣어 주세요 ---
// (ScreenStub, CommunityTab, CommunityPostItem, InfoIconWithText, Timestamp.toRelativeTime,
//  loadPostsFromFirebase, WritePostScreen, StyledWriteSection, ImagePreviewItem,
//  uploadPost, loadComments, commentCountFlow, MiniGameTab, MenuGameList,
//  PayerGameList, GameButton, ChallengeTab)

private fun calculateSelectedIndex(currentRotationDegrees: Float, sliceAngleDegrees: Float, itemCount: Int): Int {
    if (itemCount == 0) return -1
    if (sliceAngleDegrees == 0f) return 0
    val normalizedAngle = ( (270f - currentRotationDegrees).mod(360f) + (sliceAngleDegrees / 2f) ).mod(360f)
    return (normalizedAngle / sliceAngleDegrees).toInt().coerceIn(0, itemCount - 1)
}

private fun Float.pow(n: Int): Float = this.toDouble().pow(n).toFloat()
val SineEaseInOut = Easing { fraction -> (1 - cos(fraction * PI.toFloat())) / 2 }
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
    val userLocation = remember { UserManager.getUser()?.location ?: "알 수 없음" }
    var selectedTab by rememberSaveable { mutableStateOf("전체") }
    var sortOption by rememberSaveable { mutableStateOf("조회순") }
    var posts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(selectedTab, sortOption) {
        isLoading = true
        posts = loadPostsFromFirebase().filter { post ->
            when (selectedTab) {
                "전체" -> true
                "지역" -> post.location == userLocation
                else -> true
            }
        }.sortedWith(
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
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
                    else Text("완료", fontWeight = FontWeight.Bold, color = if (title.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
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