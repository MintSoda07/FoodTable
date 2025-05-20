package com.bcu.foodtable.JetpackCompose.Recipe
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.RecipeViewActivity

@Composable
fun CommentSection(
    comments: List<RecipeViewActivity.Comment>,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onSendComment: () -> Unit
) {
    Column {
        // 댓글 리스트
        LazyColumn(modifier = Modifier.height(150.dp)) {
            items(comments) { comment ->
                Text("${comment.userName}: ${comment.text}")
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 댓글 입력 + 전송 버튼
        Row {
            TextField(
                value = commentText,
                onValueChange = onCommentTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("댓글 입력...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onSendComment) {
                Text("전송")
            }
        }
    }
}
