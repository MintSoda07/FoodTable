package com.bcu.foodtable

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.JetpackCompose.Social.Openchat.purchaseRecipeWithPoints
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun RecipePurchaseDialogExact(
    recipe: RecipeItem,              // RecipeByIdScreen과 동일 정보 사용
    onPurchased: () -> Unit,         // 구매 성공 후 실행 (레시피 열기 등)
    onDismiss: () -> Unit            // 취소/닫기
) {
    val ctx = LocalContext.current
    val db  = remember { FirebaseFirestore.getInstance() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val scope = rememberCoroutineScope()

    var currentPoint by remember { mutableStateOf<Long?>(null) }
    var working by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid != null) {
            val u = db.collection("user").document(uid).get().await()
            currentPoint = u.getLong("point") ?: 0L
        } else {
            currentPoint = 0L
        }
    }

    val have = currentPoint ?: 0L
    val cost = recipe.cost
    val lack = (cost - have).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("레시피 구매 필요") },
        text = {
            Column {
                Text("이 레시피를 보려면 구매가 필요합니다.")
                Divider(Modifier.padding(vertical = 8.dp))
                Text("가격: ${cost} 소금", style = MaterialTheme.typography.bodyMedium)
                Text("보유: ${have} 소금", style = MaterialTheme.typography.bodyMedium)
                if (lack > 0) {
                    Text("부족: ${lack} 소금", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            val canBuy = uid != null && !working
            Button(
                enabled = canBuy,
                onClick = {
                    if (uid == null) {
                        Toast.makeText(ctx, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        working = true
                        try {
                            purchaseRecipeWithPoints(
                                db = db,
                                uid = uid,
                                recipeId = recipe.id,
                                cost = cost
                            )
                            Toast.makeText(ctx, "구매 완료! 🎉", Toast.LENGTH_SHORT).show()
                            onPurchased()
                        } catch (e: Exception) {
                            Toast.makeText(ctx, e.message ?: "구매 실패", Toast.LENGTH_SHORT).show()
                        } finally {
                            working = false
                        }
                    }
                }
            ) { Text("구매하기") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}