package com.bcu.foodtable.JetpackCompose.View

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LikeButtonWithCount(
    likes: Int,
    initiallyLiked: Boolean = false,
    onClick: () -> Unit
) {
    var isLiked by remember { mutableStateOf(initiallyLiked) }
    var likeCount by remember { mutableStateOf(likes) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable {
            isLiked = !isLiked
            likeCount = if (isLiked) likeCount + 1 else likeCount - 1
            onClick()
        }
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "좋아요",
            tint = if (isLiked) Color.Red else Color.Gray
        )
        Text(text = "$likeCount", style = MaterialTheme.typography.bodyMedium)
    }
}
