package com.bcu.foodtable.JetpackCompose.Social

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.Comment
import com.bcu.foodtable.useful.CommunityPost
import com.bcu.foodtable.useful.Reply
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

// ──────────────────────────────────────────────────────────────
// 유틸
// ──────────────────────────────────────────────────────────────

fun chat_warn(reason: String) { println("[경고 전송] 사유: $reason") }

fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000      -> "${count / 1_000}k"
    else                -> count.toString()
}

fun formatDate(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

@Composable fun AdminTag() {
    Text("관리자", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
}

@Composable fun ReportButton(onReport: () -> Unit) {
    IconButton(onClick = onReport) {
        Icon(Icons.Default.Report, contentDescription = "신고", tint = Color.Gray)
    }
}

// ──────────────────────────────────────────────────────────────
// Firestore 동작
// ──────────────────────────────────────────────────────────────

suspend fun reportComment(postId: String, commentId: String, reporterId: String) {
    val db = Firebase.firestore
    val reportRef = db.collection("community").document(postId)
        .collection("commentReports").document()
    val reportData = mapOf("commentId" to commentId, "reporterId" to reporterId, "timestamp" to Timestamp.now())
    reportRef.set(reportData).await()
}

suspend fun updateLikeStatus(postId: String, userId: String, liked: Boolean): Boolean {
    return try {
        val db = Firebase.firestore
        val postRef = db.collection("community").document(postId)
        val userLikeRef = db.collection("user").document(userId).collection("likedPosts").document(postId)

        Firebase.firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val currentLikes = snapshot.getLong("likes") ?: 0
            if (liked) {
                transaction.set(userLikeRef, mapOf("liked" to true))
                transaction.update(postRef, "likes", currentLikes + 1)
            } else {
                transaction.delete(userLikeRef)
                transaction.update(postRef, "likes", max(currentLikes - 1, 0))
            }
        }.await()
        true
    } catch (e: Exception) {
        e.printStackTrace(); false
    }
}

suspend fun updateBookmarkStatus(postId: String, userId: String, bookmarked: Boolean): Boolean {
    return try {
        val db = Firebase.firestore
        val userBookmarkRef = db.collection("user").document(userId).collection("bookmarkedPosts").document(postId)
        if (bookmarked) userBookmarkRef.set(mapOf("bookmarked" to true)).await()
        else userBookmarkRef.delete().await()
        true
    } catch (e: Exception) {
        e.printStackTrace(); false
    }
}

suspend fun addComment(postId: String, userId: String, nickname: String, content: String): Boolean {
    return try {
        val db = Firebase.firestore
        val commentRef = db.collection("community").document(postId).collection("comments").document()
        val comment = Comment(
            id = commentRef.id, userId = userId, nickname = nickname,
            content = content, createdAt = Timestamp.now(), likes = 0
        )
        commentRef.set(comment).await(); true
    } catch (e: Exception) {
        e.printStackTrace(); false
    }
}

suspend fun deleteComment(postId: String, commentId: String) {
    Firebase.firestore.collection("community").document(postId)
        .collection("comments").document(commentId).delete().await()
}

suspend fun toggleCommentLike(postId: String, commentId: String, userId: String, liked: Boolean) {
    val db = Firebase.firestore
    val commentRef = db.collection("community").document(postId).collection("comments").document(commentId)
    val likeRef = commentRef.collection("likes").document(userId)
    Firebase.firestore.runTransaction { transaction ->
        val snapshot = transaction.get(commentRef)
        val currentLikes = snapshot.getLong("likes") ?: 0
        if (liked) {
            transaction.set(likeRef, mapOf("liked" to true))
            transaction.update(commentRef, "likes", currentLikes + 1)
        } else {
            transaction.delete(likeRef)
            transaction.update(commentRef, "likes", max(currentLikes - 1, 0))
        }
    }.await()
}

suspend fun addReply(postId: String, commentId: String, userId: String, nickname: String, content: String) {
    val db = Firebase.firestore
    val replyRef = db.collection("community").document(postId)
        .collection("comments").document(commentId)
        .collection("replies").document()
    val reply = Reply(id = replyRef.id, userId = userId, nickname = nickname, content = content, createdAt = Timestamp.now())
    replyRef.set(reply).await()
}

suspend fun reportCommentWithReason(postId: String, commentId: String, reporterId: String, reason: String) {
    val db = Firebase.firestore
    val reportRef = db.collection("community").document(postId).collection("commentReports").document()
    val reportData = mapOf("commentId" to commentId, "reporterId" to reporterId, "reason" to reason, "timestamp" to Timestamp.now())
    reportRef.set(reportData).await()
}

suspend fun setCommentPinned(postId: String, commentId: String, pinned: Boolean) {
    Firebase.firestore.collection("community").document(postId)
        .collection("comments").document(commentId).update("pinned", pinned).await()
}

suspend fun getBlockedUserIds(currentUserId: String): List<String> {
    val snapshot = Firebase.firestore.collection("user").document(currentUserId).collection("blockedUsers").get().await()
    return snapshot.documents.mapNotNull { it.id }
}

suspend fun blockUser(currentUserId: String, targetUserId: String) {
    Firebase.firestore.collection("user").document(currentUserId).collection("blockedUsers").document(targetUserId)
        .set(mapOf("blocked" to true)).await()
}

suspend fun unblockUser(currentUserId: String, targetUserId: String) {
    Firebase.firestore.collection("user").document(currentUserId).collection("blockedUsers").document(targetUserId).delete().await()
}

// user 문서 → User 모델 로드
suspend fun fetchUser(uid: String): User? {
    if (uid.isBlank()) return null
    val doc = Firebase.firestore.collection("user").document(uid).get().await()
    return doc.toObject(User::class.java)
}

// ──────────────────────────────────────────────────────────────
// 에브리타임 스타일 UI (하단 고정바 제거, 인라인 입력 도입)
// ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(postId: String, navController: NavController) {
    val context = LocalContext.current
    val me = remember { UserManager.getUser()!! }
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    var post by remember { mutableStateOf<CommunityPost?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLiked by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    var likeCount by remember { mutableStateOf(0) }
    var bookmarkCount by remember { mutableStateOf(0) }
    var viewCount by remember { mutableStateOf(0) }

    val likeScaleAnim = remember { Animatable(1f) }
    val bookmarkScaleAnim = remember { Animatable(1f) }
    val likeTint by animateColorAsState(targetValue = if (isLiked) cs.primary else Color.Gray, label = "likeTint")
    val bookmarkTint by animateColorAsState(targetValue = if (isBookmarked) cs.primary else Color.Gray, label = "bookmarkTint")

    var author by remember { mutableStateOf<User?>(null) }
    val userCache = remember { mutableStateMapOf<String, User>() }

    var isLoading by remember { mutableStateOf(true) }

    val expandedReplies = remember { mutableStateMapOf<String, Boolean>() }
    val repliesMap = remember { mutableStateMapOf<String, List<Reply>>() }

    // 게시글 + 상태 로딩
    LaunchedEffect(postId) {
        isLoading = true
        val snap = Firebase.firestore.collection("community").document(postId).get().await()
        val loaded = snap.toObject(CommunityPost::class.java)?.copy(id = snap.id)
        post = loaded
        loaded?.let {
            likeCount     = it.likes
            bookmarkCount = it.bookmarks
            viewCount     = it.visited + 1
            Firebase.firestore.collection("community").document(postId).update("visited", viewCount)
        }
        val db = Firebase.firestore
        val likedSnap = db.collection("user").document(me.uid).collection("likedPosts").document(postId).get().await()
        isLiked = likedSnap.exists()
        val markedSnap = db.collection("user").document(me.uid).collection("bookmarkedPosts").document(postId).get().await()
        isBookmarked = markedSnap.exists()

        // 작성자 uid 문자열만 안전하게 추출
        val authorUid: String? =
            snap.getString("authorUid") ?: snap.getString("userId") ?: snap.getString("uid")
        if (!authorUid.isNullOrBlank()) author = fetchUser(authorUid)

        isLoading = false
    }

    // 댓글 실시간
    LaunchedEffect(postId) {
        val ref = Firebase.firestore.collection("community").document(postId).collection("comments")
        ref.orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshot?.documents?.mapNotNull { doc -> doc.toObject(Comment::class.java) } ?: emptyList()
                comments = list.sortedWith(
                    compareByDescending<Comment> { c -> c.pinned == true }
                        .thenBy { c -> c.createdAt.seconds }
                        .thenBy { c -> c.createdAt.nanoseconds }
                )
            }
    }

    // 댓글 작성자 캐시
    LaunchedEffect(comments) {
        val missing = comments.map { c -> c.userId }.distinct()
            .filter { uid -> uid.isNotBlank() && !userCache.containsKey(uid) }
        for (uid in missing) {
            val u = fetchUser(uid)
            if (u != null) userCache[uid] = u
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val p = post ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("게시글을 불러오지 못했습니다.") }
        return
    }

    //  하단 고정바 제거, 기본 인셋 제거하여 이중 여백 방지
    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0)
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            // 헤더
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cs.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .animateContentSize()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            p.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = cs.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "[공유] ${p.title} - ${p.content}\n\n앱에서 확인하세요!")
                            }
                            context.startActivity(Intent.createChooser(intent, "공유하기"))
                        }) { Icon(Icons.Default.Share, contentDescription = "공유") }

                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("신고") },
                                    onClick = {
                                        menuOpen = false
                                        chat_warn("post-report:${p.id}")
                                        Toast.makeText(context, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Report, contentDescription = null) }
                                )
                            }
                        }
                    }

                    // 작성자 아바타 + 이름
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val avatar = author?.image.orEmpty()
                        if (avatar.isNotEmpty()) {
                            AsyncImage(
                                model = avatar,
                                contentDescription = "작성자",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = author?.name?.takeIf { name -> name.isNotBlank() } ?: "익명",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurface
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("작성 지역: ${p.location}", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text("· ${formatDate(p.createdAt)}", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                        if (UserManager.getUser()?.manager == true) AdminTag()
                    }
                }
            }

            // 이미지
            if (p.imageUrl.isNotEmpty()) {
                item {
                    AsyncImage(
                        model = p.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            // 본문
            item {
                Text(
                    p.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 액션 바
            item {
                Spacer(Modifier.height(16.dp))
                ActionChipsRow(
                    isLiked = isLiked,
                    isBookmarked = isBookmarked,
                    likeCount = likeCount,
                    bookmarkCount = bookmarkCount,
                    viewCount = viewCount,
                    likeColor = likeTint,
                    bookmarkColor = bookmarkTint,
                    onLike = { clicked ->
                        isLiked = clicked
                        likeCount += if (clicked) 1 else -1
                        scope.launch {
                            likeScaleAnim.animateTo(1.15f, spring(stiffness = Spring.StiffnessMedium))
                            likeScaleAnim.animateTo(1f)
                            val ok = updateLikeStatus(postId, me.uid, clicked)
                            if (!ok) {
                                isLiked = !clicked
                                likeCount += if (!clicked) 1 else -1
                                Toast.makeText(context, "추천 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onBookmark = { clicked ->
                        isBookmarked = clicked
                        bookmarkCount += if (clicked) 1 else -1
                        scope.launch {
                            bookmarkScaleAnim.animateTo(1.15f, spring(stiffness = Spring.StiffnessMedium))
                            bookmarkScaleAnim.animateTo(1f)
                            val ok = updateBookmarkStatus(postId, me.uid, clicked)
                            if (!ok) {
                                isBookmarked = !clicked
                                bookmarkCount += if (!clicked) 1 else -1
                                Toast.makeText(context, "북마크 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    likeScale = likeScaleAnim.value,
                    bookmarkScale = bookmarkScaleAnim.value,
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "[공유] ${p.title} - ${p.content}\n\n앱에서 확인하세요!")
                        }
                        context.startActivity(Intent.createChooser(intent, "공유하기"))
                    }
                )
                Spacer(Modifier.height(8.dp))
                Divider(color = cs.outline.copy(alpha = 0.4f))
            }

            // 댓글 헤더
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("댓글 ${comments.size}개", style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                }
            }

            // 댓글 목록
            items(
                items = comments,
                key = { c: Comment -> c.id }
            ) { c: Comment ->
                val u: User? = userCache[c.userId]
                CommentItem(
                    postId = postId,
                    c = c,
                    isManager = me.manager,
                    avatarUrl = u?.image,
                    authorName = (u?.name?.takeIf { name -> name.isNotBlank() }) ?: c.nickname,
                    onReport = {
                        scope.launch {
                            reportComment(postId, c.id, me.uid)
                            Toast.makeText(context, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onPinToggle = { wantPin: Boolean ->
                        scope.launch { setCommentPinned(postId, c.id, wantPin) }
                    },
                    onToggleReplies = {
                        val was = expandedReplies[c.id] == true
                        expandedReplies[c.id] = !was
                        if (!was && repliesMap[c.id] == null) {
                            Firebase.firestore.collection("community").document(postId)
                                .collection("comments").document(c.id)
                                .collection("replies")
                                .orderBy("createdAt", Query.Direction.ASCENDING)
                                .addSnapshotListener { snap, _ ->
                                    val list = snap?.documents?.mapNotNull { d -> d.toObject(Reply::class.java) } ?: emptyList()
                                    repliesMap[c.id] = list
                                }
                        }
                    },
                    expanded = expandedReplies[c.id] == true,
                    replies = repliesMap[c.id] ?: emptyList(),
                    onAddReply = { content: String ->
                        scope.launch { addReply(postId, c.id, me.uid, me.name, content) }
                    }
                )
            }

            // ⬇⬇⬇ 전역 댓글 입력 (대댓글과 동일 스타일, 키보드에 딱 붙음)
            item {
                GlobalInlineCommentInput(
                    text = commentText,
                    onTextChange = { new -> commentText = new },
                    onSend = {
                        if (commentText.isNotBlank()) {
                            scope.launch {
                                val ok = addComment(postId, me.uid, me.name, commentText)
                                if (ok) {
                                    commentText = ""
                                    Toast.makeText(context, "댓글이 등록되었습니다", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "댓글 등록 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 컴포저블 파츠
// ──────────────────────────────────────────────────────────────

@Composable
private fun ActionChipsRow(
    isLiked: Boolean,
    isBookmarked: Boolean,
    likeCount: Int,
    bookmarkCount: Int,
    viewCount: Int,
    likeColor: Color,
    bookmarkColor: Color,
    onLike: (Boolean) -> Unit,
    onBookmark: (Boolean) -> Unit,
    likeScale: Float,
    bookmarkScale: Float,
    onShare: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = { onLike(!isLiked) },
            leadingIcon = {
                Icon(
                    imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                    contentDescription = null,
                    tint = likeColor,
                    modifier = Modifier.scale(likeScale)
                )
            },
            label = { Text("추천 ${formatCount(likeCount)}") },
            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.4f))
        )
        Spacer(Modifier.width(8.dp))
        AssistChip(
            onClick = { onBookmark(!isBookmarked) },
            leadingIcon = {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = bookmarkColor,
                    modifier = Modifier.scale(bookmarkScale)
                )
            },
            label = { Text("스크랩 ${formatCount(bookmarkCount)}") },
            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.4f))
        )
        Spacer(Modifier.width(8.dp))
        AssistChip(
            onClick = onShare,
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
            label = { Text("공유") },
            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.4f))
        )

        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = cs.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(formatCount(viewCount), color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CommentItem(
    postId: String,
    c: Comment,
    isManager: Boolean,
    avatarUrl: String?,
    authorName: String?,
    onReport: () -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onToggleReplies: () -> Unit,
    expanded: Boolean,
    replies: List<Reply>,
    onAddReply: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var replyText by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val avatar = avatarUrl.orEmpty()
            if (avatar.isNotEmpty()) {
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                Text(authorName ?: "익명", style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
                Text(formatDate(c.createdAt), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (c.pinned == true) {
                    AssistChip(onClick = {}, label = { Text("PIN") })
                    Spacer(Modifier.width(6.dp))
                }
                IconButton(onClick = onReport) {
                    Icon(Icons.Default.Report, contentDescription = "신고", tint = cs.onSurfaceVariant)
                }
                if (isManager) {
                    IconButton(onClick = { onPinToggle(!(c.pinned == true)) }) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "핀 고정",
                            tint = if (c.pinned == true) cs.primary else cs.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(c.content, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)

        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onToggleReplies() }
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = cs.onSurfaceVariant
            )
            Text(
                text = if (expanded) "답글 숨기기" else "답글 보기",
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            for (r in replies) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.surface.copy(alpha = 0.7f))
                        .padding(10.dp)
                ) {
                    Text("${r.nickname} • ${formatDate(r.createdAt)}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(r.content, style = MaterialTheme.typography.bodySmall, color = cs.onSurface)
                }
            }

            // 대댓글 입력 (기존 스타일 유지)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { txt -> replyText = txt },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("답글을 입력하세요") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        unfocusedBorderColor = cs.outline,
                        cursorColor = cs.primary,
                        focusedContainerColor = cs.surfaceVariant,
                        unfocusedContainerColor = cs.surfaceVariant
                    )
                )
                IconButton(onClick = {
                    if (replyText.isNotBlank()) {
                        onAddReply(replyText)
                        replyText = ""
                    }
                }) { Icon(Icons.Default.Send, contentDescription = "답글 전송", tint = cs.primary) }
            }
        }
    }
}

//  전역 댓글 입력: 대댓글과 동일 스타일, 리스트 맨 아래 아이템으로 사용
@Composable
private fun GlobalInlineCommentInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()

            //.windowInsetsPadding(WindowInsets.ime.only(WindowInsetsSides.Bottom))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("댓글을 입력하세요") }, // 문구만 다르고 UI는 답글과 동일
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = cs.primary,
                unfocusedBorderColor = cs.outline,
                cursorColor = cs.primary,
                focusedContainerColor = cs.surfaceVariant,
                unfocusedContainerColor = cs.surfaceVariant
            )
        )
        IconButton(onClick = onSend) {
            Icon(Icons.Default.Send, contentDescription = "댓글 전송", tint = cs.primary)
        }
    }
}
