package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bcu.foodtable.RecipeViewActivity
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun CommentSection(
    recipeId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var commentText by remember { mutableStateOf("") }
    var commentList by remember { mutableStateOf<List<RecipeViewActivity.Comment>>(emptyList()) }

    // 댓글 불러오기
    LaunchedEffect(recipeId) {
        db.collection("recipe")
            .document(recipeId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    commentList = snapshot.documents.mapNotNull { it.toObject(RecipeViewActivity.Comment::class.java) }
                }
            }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text("댓글 (${commentList.size})", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        // 댓글 입력창
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("댓글을 입력하세요") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val currentUser = UserManager.getUser()
                if (commentText.isNotBlank() && currentUser != null) {
                    val comment = RecipeViewActivity.Comment(
                        text = commentText,
                        timestamp = System.currentTimeMillis(),
                        userId = currentUser.uid,
                        userName = currentUser.name ?: "익명",
                        userProfileImage = currentUser.image ?: ""
                    )
                    db.collection("recipe")
                        .document(recipeId)
                        .collection("comments")
                        .add(comment)
                        .addOnSuccessListener {
                            commentText = ""
                        }
                }
            }) {
                Text("등록")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 댓글 리스트
        commentList.forEach { comment ->
            CommentItem(comment)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun CommentItem(comment: RecipeViewActivity.Comment) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = comment.userProfileImage,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text("${comment.userName}", style = MaterialTheme.typography.bodyMedium)
            Text(comment.text, style = MaterialTheme.typography.bodySmall)
        }
    }
}
