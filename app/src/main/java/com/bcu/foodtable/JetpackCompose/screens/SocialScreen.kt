package com.bcu.foodtable.JetpackCompose.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import kotlin.math.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R
import com.bcu.foodtable.model.Challenge
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
        WheelItem(Icons.Default.Search, "커뮤니티") {
            CommunityTab(
                navToWrite = { navController.navigate("write") },
                navToDetail = { post -> navController.navigate("postDetail/${post.id}") }
            )
        },
        WheelItem(Icons.Default.Fastfood, "오늘밥") { MiniGameTab(navController) },
        WheelItem(Icons.Default.Star, "랭킹") { ScreenStub("랭킹 탭") },
        WheelItem(Icons.Default.Star, "챌린지") { ChallengeTab()},
        WheelItem(Icons.Default.Face, "친구") { ScreenStub("친구 탭") },
        WheelItem(Icons.Default.Chat, "채팅") { ScreenStub("채팅 탭") },
        WheelItem(Icons.Default.Place, "맛집도") { ScreenStub("맛집도 탭") },
    )

    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }

    Surface(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                if (wheelItems[selectedIndex].label in listOf("커뮤니티", "친구")) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = {
                            Text("검색", fontWeight = FontWeight.W500)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }

                AnimatedVisibility(isLoading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        LottieAnimationView(R.raw.loading, Modifier.size(200.dp))
                    }
                }

                if (!isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    ) {
                        wheelItems[selectedIndex].screen()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 50.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                DynamicRadialWheel(
                    items = wheelItems.map { it.icon to it.label },
                    haloColor = MaterialTheme.colorScheme.primary,
                    onSelectionChanged = { selectedIndex = it },
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
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp

    val radius = with(density) {
        (screenWidth * 0.25f).coerceIn(100.dp, 220.dp).toPx()
    }

    val sliceAngle = 360f / items.size
    var expanded by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }

    var rotation by rememberSaveable {
        mutableStateOf((270f - sliceAngle * 0).mod(360f))
    }

    val haloAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.18f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1900, easing = EaseInOutCubic), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .pointerInput(expanded) {
                if (expanded) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragging = true },
                        onDrag = { _, dragAmount ->
                            val angle = dragAmount.x * 0.4f
                            rotation = (rotation + angle).mod(360f)
                        },
                        onDragEnd = {
                            dragging = false
                            val selectedIndex = calculateSelectedIndex(rotation, sliceAngle)
                            rotation = (270f - sliceAngle * selectedIndex).mod(360f)
                            onSelectionChanged(selectedIndex)
                        },
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (expanded) Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = haloColor.copy(alpha = haloAlpha),
                radius = size.minDimension / 1.15f,
                style = Stroke(width = size.minDimension / 12f)
            )
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = if (expanded) haloColor else MaterialTheme.colorScheme.surfaceVariant
        ) {
            val rot by animateFloatAsState(if (expanded) 135f else 0f)
            Icon(Icons.Default.Add, null, Modifier.rotate(rot))
        }

        AnimatedVisibility(expanded) {
            items.forEachIndexed { idx, (icon, label) ->
                val angle = sliceAngle * idx + rotation
                val rad = Math.toRadians(angle.toDouble())
                val xOff = cos(rad) * radius
                val yOff = sin(rad) * radius
                val isSelected = idx == calculateSelectedIndex(rotation, sliceAngle)

                val scale by animateFloatAsState(if (isSelected) 1.35f else 1f)
                val alpha by animateFloatAsState(if (yOff > 0) 0.3f else 1f)

                Column(
                    Modifier
                        .offset { IntOffset(xOff.roundToInt(), yOff.roundToInt()) }
                        .width(72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .scale(scale)
                            .alpha(alpha)
                            .then(if (!dragging && isSelected) Modifier.shadow(16.dp, CircleShape) else Modifier)
                            .background(
                                if (isSelected) haloColor else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    rotation = (270f - sliceAngle * idx).mod(360f)
                                    onSelectionChanged(idx)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { dragging = true },
                                    onDrag = { _, dragAmount ->
                                        val angle = dragAmount.x * 0.4f
                                        rotation = (rotation + angle).mod(360f)
                                    },
                                    onDragEnd = {
                                        dragging = false
                                        val selectedIndex = calculateSelectedIndex(rotation, sliceAngle)
                                        rotation = (270f - sliceAngle * selectedIndex).mod(360f)
                                        onSelectionChanged(selectedIndex)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            label,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    AnimatedVisibility(isSelected) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            color = haloColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun calculateSelectedIndex(rotation: Float, sliceAngle: Float): Int {
    val norm = ((rotation % 360f) + 360f) % 360f
    return ((270f - norm + sliceAngle / 2 + 360f) % 360f / sliceAngle).toInt()
}


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




// 1. 커뮤니티 탭
@Composable
fun CommunityTab(
    navToWrite: () -> Unit = {},
    navToDetail: (CommunityPost) -> Unit = {}
) {
    val userLocation = remember { UserManager.getUser()!!.location }
    var selectedTab by rememberSaveable { mutableStateOf("전체") }
    var sortOption by rememberSaveable { mutableStateOf("조회순") }

    var posts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Firebase 불러오기
    LaunchedEffect(Unit) {
        isLoading = true
        posts = loadPostsFromFirebase().also {
            it.forEach { post ->
                Log.d("CommunityTab", "사용자 지역: ${userLocation} / 게시글 지역: ${post.location} / 일치: ${post.location == userLocation}")

            }
        }
        isLoading = false
    }

    val tabOptions = listOf("전체", "지역")
    val sortOptions = listOf("조회순", "최신순", "추천순")

    Box(Modifier.fillMaxSize()) {
        Column {
            TabRow(selectedTabIndex = tabOptions.indexOf(selectedTab)) {
                tabOptions.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab) }
                    )
                }
            }

            Row(Modifier.padding(8.dp)) {
                sortOptions.forEach {
                    FilterChip(
                        selected = sortOption == it,
                        onClick = { sortOption = it },
                        label = { Text(it) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtered = posts.filter {
                    when (selectedTab) {
                        "전체" -> true
                        "지역" -> it.location == userLocation
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

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { post ->
                        CommunityPostItem(post) { navToDetail(post) }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = navToWrite,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Create, contentDescription = "글쓰기", tint = Color.White)
        }

    }
}

// 2. 게시글 항목
@Composable
fun CommunityPostItem(post: CommunityPost, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onClick() }, // 클릭 처리 추가
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (post.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(post.location, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(post.createdAt.toRelativeTime(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            Spacer(Modifier.height(6.dp))

            Text(post.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, null, Modifier.size(16.dp), tint = Color.Gray)
                Text("${post.visited}", Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Icon(Icons.Default.ThumbUp, null, Modifier.size(16.dp), tint = Color.Gray)
                Text("${post.likes}", Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Icon(Icons.Default.Bookmark, null, Modifier.size(16.dp), tint = Color.Gray)
                Text("${post.bookmarks}", Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(16.dp), tint = Color.Gray)
                Text("${post.comments}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

// 3. Timestamp -> 상대 시간 포맷
fun Timestamp.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this.toDate().time
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "방금 전"
        minutes < 60 -> "$minutes 분 전"
        hours < 24 -> "$hours 시간 전"
        days < 7 -> "$days 일 전"
        else -> SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(this.toDate())
    }
}

// 4. Firestore에서 게시글 불러오기
suspend fun loadPostsFromFirebase(): List<CommunityPost> = withContext(Dispatchers.IO) {
    val communityRef = Firebase.firestore.collection("community")
    val snapshot = communityRef.get().await()

    val posts = snapshot.documents.mapNotNull { doc ->
        val post = doc.toObject(CommunityPost::class.java)?.copy(id = doc.id)
        post
    }

    // 댓글 수 동시 조회
    posts.map { post ->
        val commentSnapshot = communityRef.document(post.id).collection("comments").get().await()
        post.copy(comments = commentSnapshot.size())
    }
}

// 5. 글쓰기 화면
@Composable
fun WritePostScreen(
    onPostCreated: () -> Unit = {},
    navController: NavController
) {
    val user = UserManager.getUser()!!
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("제목") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("내용") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            // 이미지 선택 로직
            // TODO: 권한 처리 및 이미지 피커
        }) {
            Text("이미지 선택 (${imageUris.size}개)")
        }

        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !isUploading && title.isNotBlank(),
            onClick = {
                isUploading = true
                uploadPost(
                    title, content, user.location, user.uid, imageUris
                ) {
                    isUploading = false
                    Toast.makeText(context, "작성 완료!", Toast.LENGTH_SHORT).show()
                    onPostCreated()
                    navController.popBackStack()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isUploading) CircularProgressIndicator(Modifier.size(16.dp))
            else Text("작성 완료")
        }
    }
}
fun uploadPost(
    title: String,
    content: String,
    location: String,
    userId: String,
    imageUris: List<Uri>,
    onComplete: () -> Unit
) {
    val firestore = Firebase.firestore
    val storage = Firebase.storage
    val postRef = firestore.collection("community").document()

    val uploadImageTasks = imageUris.mapIndexed { idx, uri ->
        val imageRef = storage.reference.child("posts/${postRef.id}/img_$idx.jpg")
        imageRef.putFile(uri).continueWithTask { it.result?.storage?.downloadUrl }
    }

    Tasks.whenAllSuccess<Uri>(uploadImageTasks).addOnSuccessListener { uris ->
        val postData = mapOf(
            "title" to title,
            "content" to content,
            "location" to location,
            "imageUrls" to uris.map { it.toString() },
            "visited" to 0,
            "likes" to 0,
            "bookmarks" to 0,
            "comments" to 0,
            "createdAt" to Timestamp.now(),
            "userId" to userId
        )
        postRef.set(postData).addOnSuccessListener {
            onComplete()
        }
    }
}

fun loadComments(postId: String): Flow<List<Comment>> = callbackFlow {
    val ref = Firebase.firestore.collection("community").document(postId).collection("comments")
    val listener = ref.orderBy("createdAt").addSnapshotListener { snapshot, _ ->
        val comments = snapshot?.documents?.mapNotNull {
            it.toObject(Comment::class.java)?.copy(id = it.id)
        } ?: emptyList()
        trySend(comments)
    }
    awaitClose { listener.remove() }
}
fun commentCountFlow(postId: String): Flow<Int> = callbackFlow {
    val ref = Firebase.firestore.collection("community")
        .document(postId).collection("comments")

    val listener = ref.addSnapshotListener { snapshot, _ ->
        trySend(snapshot?.size() ?: 0)
    }

    awaitClose { listener.remove() }
}

@Composable
fun MiniGameTab(navController: NavController? = null) {
    val gameTabs = listOf("메뉴 정하기", "누가 낼까?")
    var selectedTab by rememberSaveable { mutableStateOf(gameTabs.first()) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = gameTabs.indexOf(selectedTab)) {
            gameTabs.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab) }
                )
            }
        }

        when (selectedTab) {
            "메뉴 정하기" -> MenuGameList(navController)
            "누가 낼까?" -> PayerGameList()
        }
    }
}


@Composable
fun MenuGameList(navController: NavController? = null) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("메뉴 정하기 게임", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        GameButton("룰렛 돌리기") {
            navController?.navigate("rouletteGame")
        }

        GameButton("음식 셔플") {
            navController?.navigate("cardGame")
        }
    }
}

@Composable
fun PayerGameList(navController: NavController? = null) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("누가 돈을 낼까요?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        GameButton("사다리 타기") {
            navController?.navigate("ladderGame")
        }

        GameButton("룰렛 돌리기") {
            navController?.navigate("payerRouletteGame")
        }
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
        error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("⚠ ${error ?: "오류 발생"}", color = MaterialTheme.colorScheme.error)
        }
        else -> ChallengeScreenContent(
            challenges = challenges,
            salt = userSalt,
            onProgressUpdate = { id, value -> viewModel.updateProgress(id, value) },
            onStartChallenge = { id -> viewModel.startChallenge(id) }
        )
    }
}
