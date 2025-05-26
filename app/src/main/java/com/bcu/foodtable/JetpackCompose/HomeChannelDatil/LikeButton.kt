package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LikeButton(recipeId: String) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val user = UserManager.getUser()

    var likes by remember { mutableStateOf(0L) }
    var isLiked by remember { mutableStateOf(false) }

    // 좋아요 상태 초기화
    LaunchedEffect(recipeId, user?.uid) {
        db.collection("recipe").document(recipeId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    likes = doc.getLong("likes") ?: 0L
                    val likedUsers = doc.get("likedUsers") as? List<String> ?: emptyList()
                    isLiked = user?.uid in likedUsers
                }
            }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            if (user == null) {
                Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@IconButton
            }

            val updatedLikes = if (isLiked) likes - 1 else likes + 1
            val updateMap = mutableMapOf<String, Any>(
                "likes" to updatedLikes
            )

            db.collection("recipe").document(recipeId).get().addOnSuccessListener { doc ->
                val likedUsers = (doc.get("likedUsers") as? List<String>)?.toMutableList() ?: mutableListOf()

                if (isLiked) {
                    likedUsers.remove(user.uid)
                } else {
                    likedUsers.add(user.uid!!)
                }
                updateMap["likedUsers"] = likedUsers

                db.collection("recipe").document(recipeId)
                    .update(updateMap)
                    .addOnSuccessListener {
                        likes = updatedLikes
                        isLiked = !isLiked
                    }
            }
        }) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isLiked) Color.Red else Color.Gray
            )
        }

        Text(text = "$likes", style = MaterialTheme.typography.bodyMedium)
    }
}
