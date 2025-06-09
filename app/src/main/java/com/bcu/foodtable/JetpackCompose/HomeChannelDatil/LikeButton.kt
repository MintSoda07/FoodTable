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
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

@Composable
fun LikeButton(recipeId: String) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val user = UserManager.getUser()

    var likes by remember { mutableStateOf(0L) }
    var isLiked by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) } // 업데이트 중 플래그

    // 초기 상태 읽기
    LaunchedEffect(recipeId, user?.uid) {
        db.collection("recipe").document(recipeId).get()
            .addOnSuccessListener { doc ->
                likes = doc.getLong("likes") ?: 0L
                val likedUsers = doc.get("likedUsers") as? List<String> ?: emptyList()
                isLiked = user?.uid in likedUsers
            }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            enabled = !isUpdating,  // 업데이트 중에는 비활성화
            onClick = {
                if (user == null) {
                    Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                    return@IconButton
                }
                if (isUpdating) return@IconButton

                isUpdating = true
                val docRef = db.collection("recipe").document(recipeId)

                db.runTransaction { tx ->
                    val snapshot = tx.get(docRef)
                    val currentLikes = snapshot.getLong("likes") ?: 0L
                    val likedUsers = (snapshot.get("likedUsers") as? List<String>)?.toMutableList()
                        ?: mutableListOf()

                    val newLikes: Long
                    if (user.uid in likedUsers) {
                        likedUsers.remove(user.uid)
                        newLikes = currentLikes - 1
                    } else {
                        likedUsers.add(user.uid)
                        newLikes = currentLikes + 1
                    }

                    tx.update(docRef, mapOf(
                        "likes" to newLikes,
                        "likedUsers" to likedUsers
                    ))
                }.addOnSuccessListener {
                    // 트랜잭션 성공 시 로컬 상태 업데이트
                    isLiked = !isLiked
                    likes += if (isLiked) +1 else -1
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "좋아요 업데이트 중 오류: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }.addOnCompleteListener {
                    isUpdating = false
                }
            }
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = if (isLiked) Color.Red else Color.Gray
            )
        }

        Text(
            text = likes.toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
