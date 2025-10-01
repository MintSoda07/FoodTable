package com.bcu.foodtable.JetpackCompose

/**
 * Gradle (Module) dependencies 예시
 * ---------------------------------
 * implementation(platform("androidx.compose:compose-bom:2024.09.01"))
 * implementation("androidx.compose.ui:ui")
 * implementation("androidx.compose.material3:material3:1.3.0")
 * implementation("androidx.compose.foundation:foundation:1.6.8") // VerticalPager
 * implementation("io.coil-kt:coil-compose:2.6.0")
 * implementation("androidx.media3:media3-exoplayer:1.4.1")
 * implementation("androidx.media3:media3-ui:1.4.1")
 * implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
 * implementation("com.google.firebase:firebase-firestore-ktx:25.0.0")
 * implementation("com.google.firebase:firebase-storage-ktx:21.0.0")
 */

import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

// ----------------------------
// Theme — WarmLightColorScheme
// ----------------------------
private val WarmLightColorScheme = lightColorScheme(
    primary = Color(0xFFE25532),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2D6),
    onPrimaryContainer = Color(0xFF5C2B1B),
    secondary = Color(0xFFFFF4ED),
    onSecondary = Color(0xFF4B3C35),
    secondaryContainer = Color(0xFFFDE1D5),
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = Color(0xFFB9806D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E0DC),
    onTertiaryContainer = Color(0xFF4E342E),
    background = Color(0xFF000000), // 전체 화면 감성 살리기
    onBackground = Color(0xFFEFEFEF),
    surface = Color(0xFF101010),
    onSurface = Color(0xFFEAEAEA),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFB5B5B5),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A),
    inverseSurface = Color(0xFFEFEFEF),
    inverseOnSurface = Color.Black,
    inversePrimary = Color(0xFFFF8F6B),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFF330000),
    onErrorContainer = Color(0xFFFFDAD4)
)

@Composable
fun WarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WarmLightColorScheme, content = content)
}

// ---------- Data model ----------
data class UserProfile(val name: String = "익명", val image: String = "")
data class RecipeDoc(val id: String = "", val name: String = "", val imageResId: String = "")
data class Review(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImage: String = "",
    val recipeId: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image", // "image" | "video"
    val rating: Int = 8,
    val createdAt: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val likedUsers: List<String> = emptyList()
)

// --------- ViewModel ---------
class UserReviewViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val auth = Firebase.auth

    private val _recipe = MutableStateFlow(RecipeDoc())
    val recipe: StateFlow<RecipeDoc> = _recipe

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun observeRecipe(recipeId: String) {
        db.collection("recipe").document(recipeId)
            .addSnapshotListener { snap, e ->
                if (e != null) { _error.value = e.message; return@addSnapshotListener }
                if (snap != null && snap.exists()) {
                    _recipe.value = RecipeDoc(
                        id = snap.getString("id") ?: recipeId,
                        name = snap.getString("name") ?: "",
                        imageResId = snap.getString("imageResId") ?: ""
                    )
                }
            }
    }

    fun observeReviews(recipeId: String) {
        db.collection("recipe").document(recipeId)
            .collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { qs, e ->
                if (e != null) { _error.value = e.message; return@addSnapshotListener }
                _reviews.value = qs?.documents?.map { it.toReview() } ?: emptyList()
            }
    }

    suspend fun loadUserProfile(uid: String): UserProfile {
        val snap = db.collection("user").document(uid).get().await()
        return if (snap.exists()) {
            UserProfile(
                name = snap.getString("name") ?: "익명",
                image = snap.getString("image") ?: ""
            )
        } else UserProfile()
    }

    suspend fun uploadReview(
        recipeId: String,
        text: String,
        localMediaUri: Uri,
        rating: Int
    ) {
        val user = auth.currentUser ?: run { _error.value = "로그인이 필요합니다"; return }
        _isUploading.value = true
        try {
            val profile = loadUserProfile(user.uid)
            val mime = guessMime(localMediaUri)
            val isVideo = mime.startsWith("video")

            val ref = storage.reference.child(
                if (isVideo) "videos/reviews/${user.uid}/${System.currentTimeMillis()}"
                else "images/reviews/${user.uid}/${System.currentTimeMillis()}"
            ).let { base ->
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: if (isVideo) "mp4" else "jpg"
                base.child("media.$ext")
            }

            ref.putFile(localMediaUri).await()
            val mediaUrl = ref.downloadUrl.await().toString()

            val newDoc = db.collection("recipe").document(recipeId)
                .collection("reviews").document()

            val review = Review(
                id = newDoc.id,
                userId = user.uid,
                userName = profile.name.ifBlank { user.displayName ?: "익명" },
                userImage = profile.image,
                recipeId = recipeId,
                text = text,
                mediaUrl = mediaUrl,
                mediaType = if (isVideo) "video" else "image",
                rating = rating,
                createdAt = System.currentTimeMillis()
            )
            newDoc.set(review).await()
        } catch (t: Throwable) {
            _error.value = t.message
        } finally {
            _isUploading.value = false
        }
    }

    fun toggleLike(recipeId: String, reviewId: String) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val ref = db.collection("recipe").document(recipeId)
            .collection("reviews").document(reviewId)

        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val liked = (snap.get("likedUsers") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val likes = (snap.getLong("likes") ?: 0L).toInt()
            if (liked.contains(uid)) {
                tx.update(ref, mapOf("likedUsers" to liked.filter { it != uid }, "likes" to (likes - 1).coerceAtLeast(0)))
            } else {
                tx.update(ref, mapOf("likedUsers" to (liked + uid), "likes" to (likes + 1)))
            }
        }
    }

    private fun guessMime(uri: Uri): String {
        val c = Firebase.storage.app.applicationContext.contentResolver
        return c.getType(uri) ?: "application/octet-stream"
    }
}

// --------------- Activity host ---------------
class UserReviewActivity : ComponentActivity() {
    companion object { const val EXTRA_RECIPE_ID = "extra_recipe_id" }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val recipeId = intent.getStringExtra(EXTRA_RECIPE_ID).orEmpty()
        setContent {
            WarmTheme {
                Surface {
                    UserReviewScreen(
                        recipeId = recipeId,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

// 비율 모드
enum class MediaScaleMode { Fit, Crop }

// --------------- Screen ---------------
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserReviewScreen(
    recipeId: String,
    onBack: () -> Unit,
    vm: UserReviewViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    LaunchedEffect(recipeId) {
        vm.observeRecipe(recipeId)
        vm.observeReviews(recipeId)
    }

    val recipe by vm.recipe.collectAsState()
    val reviews by vm.reviews.collectAsState()
    val isUploading by vm.isUploading.collectAsState()
    val error by vm.error.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 비율 모드 (전체 페이지 공통)
    var mediaScaleMode by remember { mutableStateOf(MediaScaleMode.Crop) }

    // composer bottom sheet state
    var showComposer by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("사용자 후기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (recipe.name.isNotBlank())
                            Text(recipe.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = "back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showComposer = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp, end = 2.dp)
            ) { Icon(Icons.Rounded.Add, contentDescription = "add") }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        val pagerState = rememberPagerState(pageCount = { reviews.size.coerceAtLeast(1) })

        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (reviews.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("아직 후기가 없어요. 첫 후기를 남겨보세요!", color = Color.White.copy(alpha = 0.8f))
                }
            } else {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val r = reviews[page]
                    ShortFormReviewFullCard(
                        review = r,
                        isActive = (pagerState.currentPage == page),
                        mediaScaleMode = mediaScaleMode,
                        onLike = { vm.toggleLike(recipeId, r.id) }
                    )
                }
            }

            // 상단 우: 비율 토글 (원본/가득)
            ScaleToggle(
                mode = mediaScaleMode,
                onChange = { mediaScaleMode = it },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }

        // Composer bottom sheet
        if (showComposer) {
            ModalBottomSheet(
                onDismissRequest = { showComposer = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ReviewComposerSheet(
                    isUploading = isUploading,
                    onSubmit = { text, mediaUri, rating ->
                        scope.launch {
                            vm.uploadReview(recipeId, text, mediaUri!!, rating)
                            showComposer = false
                        }
                    }
                )
            }
        }
    }
}

// 비율 토글 칩(원본 / 가득)
@Composable
private fun ScaleToggle(
    mode: MediaScaleMode,
    onChange: (MediaScaleMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        @Composable
        fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
            Text(
                text = text,
                color = if (selected) Color.Black else Color.White,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Chip("원본", mode == MediaScaleMode.Fit) { onChange(MediaScaleMode.Fit) }
        Chip("가득", mode == MediaScaleMode.Crop) { onChange(MediaScaleMode.Crop) }
    }
}

// ─────────────────────────────────────────────────────────────
// 풀스크린 숏폼 카드 (Shorts 스타일) + 하트 버튼 분리 + 비율 모드 반영
// ─────────────────────────────────────────────────────────────
@Composable
private fun ShortFormReviewFullCard(
    review: Review,
    isActive: Boolean,
    mediaScaleMode: MediaScaleMode,
    onLike: () -> Unit
) {
    val liked = remember(review.likedUsers) {
        val uid = Firebase.auth.currentUser?.uid
        uid != null && review.likedUsers.contains(uid)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 미디어 (영상/이미지) – 화면 꽉 채움
        if (review.mediaType == "video") {
            VideoBox(
                url = review.mediaUrl,
                play = isActive, // 활성 페이지에서만 자동재생
                scaleMode = mediaScaleMode,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(review.mediaUrl).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = if (mediaScaleMode == MediaScaleMode.Crop) ContentScale.Crop else ContentScale.Fit
            )
        }

        // 상·하 가독성 그라디언트
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        ),
                        tileMode = TileMode.Clamp
                    )
                )
        )

        // 상단 좌: 유저 정보 칩
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(14.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            AsyncImage(
                model = review.userImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(Modifier.size(8.dp))
            Column {
                Text(
                    review.userName.ifBlank { "익명" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    review.createdAt.toDateLabel(),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }

        // 하단: 텍스트/평점 + ‘좋아요 버튼’(분리)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 16.dp)
        ) {
            if (review.text.isNotBlank()) {
                Text(
                    review.text,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
            }
            RatingDots(value = review.rating, size = 10.dp)

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLike,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (liked) MaterialTheme.colorScheme.primary else Color(0x22FFFFFF),
                        contentColor = if (liked) MaterialTheme.colorScheme.onPrimary else Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "like"
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("${review.likes}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// VideoBox: 활성 페이지에서만 재생 + 화면 이탈 시 release + 비율 모드 반영
// ─────────────────────────────────────────────────────────────
@OptIn(UnstableApi::class)
@Composable
private fun VideoBox(
    url: String,
    play: Boolean,
    scaleMode: MediaScaleMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            volume = 0f // 무음
        }
    }

    // 미디어 설정/준비
    LaunchedEffect(url) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }
    // 재생 상태 반영
    LaunchedEffect(play) {
        player.playWhenReady = play
        if (play) player.play()
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = false
                // 비율 모드 반영
                resizeMode = if (scaleMode == MediaScaleMode.Crop)
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                else
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = { pv ->
            pv.resizeMode = if (scaleMode == MediaScaleMode.Crop)
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else
                AspectRatioFrameLayout.RESIZE_MODE_FIT
        },
        modifier = modifier
    )

    // 화면에서 사라질 때 리소스 해제
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }
}

// ─────────────────────────────────────────────────────────────
// Composer (BottomSheet): 미디어 필수 (사진 or 영상)
// ─────────────────────────────────────────────────────────────
@Composable
private fun ReviewComposerSheet(
    isUploading: Boolean,
    onSubmit: (text: String, media: Uri?, rating: Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(8) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> pickedUri = uri }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
            Text("후기 작성", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = isUploading, enter = fadeIn(), exit = fadeOut()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(6.dp))
                    Text("업로드 중…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("짧은 한 줄 평이나 팁을 적어주세요") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            maxLines = 3
        )

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("만족도", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(8.dp))
            androidx.compose.material3.Slider(
                value = rating.toFloat(),
                onValueChange = { rating = it.toInt().coerceIn(1, 10) },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.weight(1f)
            )
            Text("${rating}/10", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) { Text(if (pickedUri == null) "사진/영상 선택" else "다시 선택") }

            Spacer(Modifier.weight(1f))

            Button(
                enabled = pickedUri != null && !isUploading,
                onClick = { onSubmit(text.trim(), pickedUri, rating) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("올리기") }
        }

        Spacer(Modifier.height(10.dp))

        AnimatedVisibility(visible = pickedUri != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                AsyncImage(
                    model = pickedUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

// -------- Small bits --------
@Composable
private fun RatingDots(value: Int, max: Int = 10, size: Dp = 8.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(max) { idx ->
            val filled = idx < value
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

private fun DocumentSnapshot.toReview(): Review = Review(
    id = id,
    userId = getString("userId") ?: "",
    userName = getString("userName") ?: "",
    userImage = getString("userImage") ?: "",
    recipeId = getString("recipeId") ?: "",
    text = getString("text") ?: "",
    mediaUrl = getString("mediaUrl") ?: getString("photoUrl") ?: "",
    mediaType = getString("mediaType") ?: if (!getString("photoUrl").isNullOrBlank()) "image" else "image",
    rating = (getLong("rating") ?: 8L).toInt(),
    createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
    likes = (getLong("likes") ?: 0L).toInt(),
    likedUsers = (get("likedUsers") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
)

private fun Long.toDateLabel(): String {
    val fmt = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    return fmt.format(Date(this))
}
