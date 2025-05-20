package com.bcu.foodtable.JetpackCompose.Recipe

import com.bcu.foodtable.useful.RecipeItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.JetpackCompose.View.LikeButtonWithCount
import com.bcu.foodtable.JetpackCompose.View.RecipeHeader
import com.bcu.foodtable.JetpackCompose.View.RecipeMeta
import com.bcu.foodtable.RecipeViewActivity
import com.bcu.foodtable.Whisper.VoiceCookingManager



@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val recipe by viewModel.recipe.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val likes by viewModel.likes.collectAsState()
    val userHasLiked by viewModel.userHasLiked.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()

    var commentText by remember { mutableStateOf("") }
    var isVoiceActive by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        recipe?.let {
            Text(it.name, style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(8.dp))

            Text("설명: ${it.description}")

            Spacer(Modifier.height(8.dp))

            Button(onClick = {
                val html = generateHtmlForPdf(it)
                (context as? android.app.Activity)?.let { activity ->
                    createPdfFromHtml(activity, html, "레시피_${it.name}")
                }
            }) {
                Text("PDF 저장")
            }

            Spacer(Modifier.height(16.dp))

            // 좋아요 버튼 등등 추가 가능
            Text("좋아요: $likes")

            // 댓글 입력창
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                label = { Text("댓글 작성") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if(commentText.isNotBlank()) {
                        viewModel.sendComment(commentText)
                        commentText = ""
                    }
                }
            ) {
                Text("댓글 전송")
            }

            Spacer(Modifier.height(16.dp))

            Text("댓글 목록", style = MaterialTheme.typography.titleMedium)

            // 댓글 리스트 간단 표시 (실제로는 LazyColumn 등으로 구현 권장)
            comments.forEach { comment ->
                Text("${comment.userName}: ${comment.text}")
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(16.dp))

            if (isOwner) {
                Button(onClick = onEdit) {
                    Text("수정")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onDelete) {
                    Text("삭제")
                }
            }
        } ?: run {
            Text("레시피를 불러오는 중입니다...")
        }
    }
}
