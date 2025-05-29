package com.bcu.foodtable.JetpackCompose.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.bcu.foodtable.JetpackCompose.screens.toRelativeTime
import com.bcu.foodtable.useful.CommunityPost
import com.bcu.foodtable.useful.Comment
import com.bcu.foodtable.useful.Reply
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

fun chat_warn(reason: String) {
    println("[경고 전송] 사유: $reason")
}

fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}k"
    else -> count.toString()
}

fun formatDate(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

@Composable
fun AdminTag() {
    Text("관리자", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
}

@Composable
fun ReportButton(onReport: () -> Unit) {
    IconButton(onClick = onReport) {
        Icon(Icons.Default.Report, contentDescription = "신고", tint = Color.Gray)
    }
}

suspend fun reportComment(postId: String, commentId: String, reporterId: String) {
    val db = Firebase.firestore
    val reportRef = db.collection("community").document(postId)
        .collection("commentReports").document()

    val reportData = mapOf(
        "commentId" to commentId,
        "reporterId" to reporterId,
        "timestamp" to Timestamp.now()
    )
    reportRef.set(reportData).await()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(postId: String, navController: NavController) {
    val context = LocalContext.current
    val user = remember { UserManager.getUser()!! }
    val scope = rememberCoroutineScope()

    var post by remember { mutableStateOf<CommunityPost?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLiked by remember { mutableStateOf(false) }
    var isBookmarked by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    var likeCount by remember { mutableStateOf(0) }
    var bookmarkCount by remember { mutableStateOf(0) }
    var viewCount by remember { mutableStateOf(0) }

    val likeAnim = remember { Animatable(1f) }
    val bookmarkAnim = remember { Animatable(1f) }
    val likeColorAnim = remember { Animatable(Color.Gray) }
    val bookmarkColorAnim = remember { Animatable(Color.Gray) }
    val commentAlpha = remember { Animatable(0f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val grayColor = Color.Gray

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(postId) {
        isLoading = true
        val snapshot = Firebase.firestore.collection("community").document(postId).get().await()
        val loadedPost = snapshot.toObject(CommunityPost::class.java)?.copy(id = snapshot.id)
        post = loadedPost
        loadedPost?.let {
            likeCount = it.likes
            bookmarkCount = it.bookmarks
            viewCount = it.visited + 1
            Firebase.firestore.collection("community").document(postId).update("visited", viewCount)
        }
        isLoading = false
    }

    LaunchedEffect(postId) {
        Firebase.firestore.collection("community").document(postId)
            .collection("comments").orderBy("createdAt")
            .addSnapshotListener { snapshot, _ ->
                comments = snapshot?.documents?.mapNotNull {
                    it.toObject(Comment::class.java)
                } ?: emptyList()
            }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        post?.let {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 제목 + 관리자 태그
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(it.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (user.manager) AdminTag()
                }

                Spacer(Modifier.height(8.dp))
                Text("작성 지역: ${it.location} · ${formatDate(it.createdAt)}", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(12.dp))

                it.imageUrl.takeIf { url -> url.isNotEmpty() }?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Text(it.content, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, null, Modifier.size(16.dp), tint = grayColor)
                    Text(formatCount(viewCount), Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodySmall, color = grayColor)

                    IconToggleButton(checked = isLiked, onCheckedChange = { clicked ->
                        isLiked = clicked
                        likeCount += if (clicked) 1 else -1

                        scope.launch {
                            try {
                                likeAnim.animateTo(1.3f, spring())
                                likeColorAnim.animateTo(primaryColor, tween(300))
                                likeAnim.animateTo(1f)
                                likeColorAnim.animateTo(grayColor, tween(300))
                                val result = updateLikeStatus(postId, user.uid, clicked)
                                if (!result) throw Exception()
                            } catch (e: Exception) {
                                isLiked = !clicked
                                likeCount += if (!clicked) 1 else -1
                                Toast.makeText(context, "추천 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                            contentDescription = "추천",
                            tint = likeColorAnim.value,
                            modifier = Modifier.scale(likeAnim.value)
                        )
                    }

                    Text(formatCount(likeCount), Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodySmall)

                    IconToggleButton(checked = isBookmarked, onCheckedChange = { clicked ->
                        isBookmarked = clicked
                        bookmarkCount += if (clicked) 1 else -1

                        scope.launch {
                            try {
                                bookmarkAnim.animateTo(1.3f, spring())
                                bookmarkColorAnim.animateTo(primaryColor, tween(300))
                                bookmarkAnim.animateTo(1f)
                                bookmarkColorAnim.animateTo(grayColor, tween(300))
                                val result = updateBookmarkStatus(postId, user.uid, clicked)
                                if (!result) throw Exception()
                            } catch (e: Exception) {
                                isBookmarked = !clicked
                                bookmarkCount += if (!clicked) 1 else -1
                                Toast.makeText(context, "북마크 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "북마크",
                            tint = bookmarkColorAnim.value,
                            modifier = Modifier.scale(bookmarkAnim.value)
                        )
                    }

                    Text(formatCount(bookmarkCount), Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodySmall)

                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "[공유] ${it.title} - ${it.content}\n\n앱에서 확인하세요!")
                        }
                        context.startActivity(Intent.createChooser(intent, "공유하기"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "공유")
                    }

                    Text(formatCount(comments.size), style = MaterialTheme.typography.bodySmall)
                }

                Divider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)

                Text("댓글 ${comments.size}개", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                comments.forEach { comment ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("댓글을 입력하세요") },
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(commentAlpha.value),
                                colors = outlinedTextFieldColors(
                                    focusedBorderColor = colorScheme.primary,
                                    unfocusedBorderColor = colorScheme.outline,
                                    cursorColor = colorScheme.primary,
                                    containerColor = colorScheme.surfaceVariant
                                ),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                                singleLine = true
                            )

                            IconButton(onClick = {
                                if (commentText.isNotBlank()) {
                                    scope.launch {
                                        val result = addComment(postId, user.uid, user.name, commentText)
                                        if (result) {
                                            commentText = ""
                                            Toast.makeText(context, "댓글이 등록되었습니다", Toast.LENGTH_SHORT).show()
                                            commentAlpha.snapTo(0f)
                                        } else {
                                            Toast.makeText(context, "댓글 등록 실패", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "댓글 전송",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }


                }

                Spacer(Modifier.height(12.dp))

                LaunchedEffect(commentText) {
                    if (commentText.isNotBlank()) commentAlpha.animateTo(1f, tween(300))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("댓글을 입력하세요") },
                        modifier = Modifier
                            .weight(1f)
                            .alpha(commentAlpha.value),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = primaryColor
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        if (commentText.isNotBlank()) {
                            scope.launch {
                                val result = addComment(postId, user.uid, user.name, commentText)
                                if (result) {
                                    commentText = ""
                                    Toast.makeText(context, "댓글이 등록되었습니다", Toast.LENGTH_SHORT).show()
                                    commentAlpha.snapTo(0f)
                                } else {
                                    Toast.makeText(context, "댓글 등록 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "댓글 전송", tint = primaryColor)
                    }
                }
            }
        } ?: run {
            Text("게시글을 불러오지 못했습니다.")
        }
    }
}

suspend fun updateLikeStatus(postId: String, userId: String, liked: Boolean): Boolean {
    return try {
        val db = Firebase.firestore
        val postRef = db.collection("community").document(postId)
        val userLikeRef = db.collection("user").document(userId)
            .collection("likedPosts").document(postId)

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
        e.printStackTrace()
        false
    }
}


suspend fun updateBookmarkStatus(postId: String, userId: String, bookmarked: Boolean): Boolean {
    return try {
        val db = Firebase.firestore
        val userBookmarkRef = db.collection("user").document(userId)
            .collection("bookmarkedPosts").document(postId)

        if (bookmarked) {
            userBookmarkRef.set(mapOf("bookmarked" to true)).await()
        } else {
            userBookmarkRef.delete().await()
        }

        true  // 성공 반환
    } catch (e: Exception) {
        e.printStackTrace()
        false // 실패 반환
    }
}


suspend fun addComment(
    postId: String,
    userId: String,
    nickname: String,
    content: String
): Boolean {
    return try {
        val db = Firebase.firestore
        val commentRef = db.collection("community")
            .document(postId)
            .collection("comments")
            .document()

        val comment = Comment(
            id = commentRef.id,
            userId = userId,
            nickname = nickname,
            content = content,
            createdAt = Timestamp.now(),
            likes = 0
        )

        commentRef.set(comment).await()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


suspend fun deleteComment(postId: String, commentId: String) {
    val db = Firebase.firestore
    db.collection("community").document(postId).collection("comments").document(commentId).delete().await()
}


//
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

// 대댓글 추가
suspend fun addReply(postId: String, commentId: String, userId: String, nickname: String, content: String) {
    val db = Firebase.firestore
    val replyRef = db.collection("community").document(postId)
        .collection("comments").document(commentId)
        .collection("replies").document()

    val reply = Reply(
        id = replyRef.id,
        userId = userId,
        nickname = nickname,
        content = content,
        createdAt = Timestamp.now()
    )

    replyRef.set(reply).await()
}

// 신고 사유 포함
suspend fun reportCommentWithReason(postId: String, commentId: String, reporterId: String, reason: String) {
    val db = Firebase.firestore
    val reportRef = db.collection("community").document(postId)
        .collection("commentReports").document()

    val reportData = mapOf(
        "commentId" to commentId,
        "reporterId" to reporterId,
        "reason" to reason,
        "timestamp" to Timestamp.now()
    )
    reportRef.set(reportData).await()
}

// 댓글 핀 설정
suspend fun setCommentPinned(postId: String, commentId: String, pinned: Boolean) {
    val db = Firebase.firestore
    val commentRef = db.collection("community").document(postId).collection("comments").document(commentId)
    commentRef.update("pinned", pinned).await()
}

// 차단 유저 목록 가져오기
suspend fun getBlockedUserIds(currentUserId: String): List<String> {
    val db = Firebase.firestore
    val snapshot = db.collection("user").document(currentUserId).collection("blockedUsers").get().await()
    return snapshot.documents.mapNotNull { it.id }
}

// 차단 기능
suspend fun blockUser(currentUserId: String, targetUserId: String) {
    val db = Firebase.firestore
    val blockRef = db.collection("user").document(currentUserId).collection("blockedUsers").document(targetUserId)
    blockRef.set(mapOf("blocked" to true)).await()
}

// 차단 해제 기능
suspend fun unblockUser(currentUserId: String, targetUserId: String) {
    val db = Firebase.firestore
    db.collection("user").document(currentUserId).collection("blockedUsers").document(targetUserId).delete().await()
}
