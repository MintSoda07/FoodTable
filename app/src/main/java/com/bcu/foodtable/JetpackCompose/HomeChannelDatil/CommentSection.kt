package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// 댓글 데이터 모델
data class Comment(
    val text: String = "",
    val timestamp: Long = 0L,
    val userId: String = "", // 사용자 ID 추가
    val userName: String = "", // 사용자 이름 추가
    val userProfileImage: String = "" // 사용자 프로필 사진 URL 추가
)
@Composable
fun CommentSection(
    recipeId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var commentText by remember { mutableStateOf("") }
    var commentList by remember { mutableStateOf<List<Comment>>(emptyList()) }

    // 댓글 실시간 로딩
    LaunchedEffect(recipeId) {
        db.collection("recipe")
            .document(recipeId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    commentList = snapshot.documents.mapNotNull {
                        it.toObject(Comment::class.java)
                    }
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "💬 댓글 ${commentList.size}개",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary // Color(0xFFE25532) 자동 적용
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 댓글 입력 영역
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // Color(0xFFFBE7DF) 자동 적용
            elevation = CardDefaults.run { cardElevation(defaultElevation = 4.dp) }
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("댓글을 입력하세요...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, // Color(0xFFE25532) 자동 적용
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline // Color(0xFFDDC7BD) 자동 적용
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        val user = UserManager.getUser()
                        if (commentText.isNotBlank() && user != null) {
                            val comment = Comment(
                                text = commentText,
                                timestamp = System.currentTimeMillis(),
                                userId = user.uid,
                                userName = user.name ?: "익명",
                                userProfileImage = user.image ?: ""
                            )
                            db.collection("recipe")
                                .document(recipeId)
                                .collection("comments")
                                .add(comment)
                                .addOnSuccessListener {
                                    commentText = ""
                                }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("등록")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 댓글 목록
        commentList.forEach { comment ->
            CommentItem(comment)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // Color.White 자동 적용
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = comment.userProfileImage,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    comment.userName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary // Color(0xFFE25532) 자동 적용
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Color(0xFF5F5F5F) 자동 적용
                )
            }
        }
    }
}