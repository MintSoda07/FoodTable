package com.bcu.foodtable.JetpackCompose.Recipe



import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.JetpackCompose.View.LikeButtonWithCount
import com.bcu.foodtable.JetpackCompose.View.RecipeHeader
import com.bcu.foodtable.JetpackCompose.View.RecipeMeta
import com.bcu.foodtable.useful.RecipeItem

@Composable
fun RecipeDetailScreen(
    recipe: RecipeItem,
    isOwner: Boolean,
    onLikeClicked: () -> Unit,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onDownloadPdf: () -> Unit,
    onCommentSubmit: (String) -> Unit,
    comments: List<String>,
    onVoiceCommand: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        RecipeHeader(recipe = recipe)

        RecipeMeta(
            categories = recipe.C_categories,
            tags = recipe.tags,
            note = recipe.note
        )

        RecipeStepsSection(
            steps = recipe.order.split("○").filter { it.isNotBlank() }
        )

        LikeButtonWithCount(
            likes = recipe.clicked, // 대체 필요: 좋아요 수 저장용 필드 사용
            onClick = onLikeClicked
        )

        CommentSection(
            comments = comments,
            onSubmit = onCommentSubmit
        )

        VoiceControlSection(
            onCommand = onVoiceCommand
        )

        if (isOwner) {
            OwnerActions(
                onEdit = onEditClicked,
                onDelete = onDeleteClicked
            )
        }

        RecipePdfButton(
            onClick = onDownloadPdf
        )
    }
}
