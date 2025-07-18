package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import ads_mobile_sdk.ui
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.bcu.foodtable.JetpackCompose.AI.AiHelperViewModel
import com.bcu.foodtable.useful.RecipeItem
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers

@Composable
fun FuturisticDialog(
    ingredients: List<Ingredient>,
    recipes: List<String>,
    navController: NavController,
    aiViewModel: AiHelperViewModel,
    onDismiss: () -> Unit,
    userId: String

    ) {

    // 1) 게이지 진행 상태를 위한 Animatable
    val progress = remember { Animatable(0f) }

    // 1) ViewModel 상태 구독
    val ui by aiViewModel.uiState.collectAsState()

    val context = LocalContext.current

    // 2) isSending 변할 때마다 0→1 애니메이션
    LaunchedEffect(ui.isSending) {
        if (ui.isSending) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 17000,
                    easing = LinearEasing
                )
            )
        } else {
            // 이미지도 받았거나 에러가 났다면 즉시 100%
            progress.snapTo(1f)
        }
    }
    LaunchedEffect(ui.done) {
        if (ui.done) {
            try {
                // 1. Storage에 이미지 업로드 후 Storage 경로(path) 받기
                val storagePath = aiViewModel.uploadImageToFirebaseStorage(context, ui.imageUrl!!, userId)
                Log.d("이미지업로드", "Storage 경로: $storagePath")

                // 2. Firestore에 저장할 때 '/'를 '%2F'로 인코딩
                val encodedPath = storagePath.replace("/", "%2F")

                val recipeItem = RecipeItem(
                    id = "ai_${System.currentTimeMillis()}",
                    name = ui.recipes.firstOrNull().orEmpty(),
                    description = "",
                    imageResId = encodedPath,  // 인코딩된 Storage 경로 저장
                    ingredients = ingredients.map { it.name },
                    order = ui.resultText,
                    estimatedCalories = null,
                    C_categories = emptyList(),
                    tags = emptyList()
                )
                val docRef = FirebaseFirestore.getInstance()
                    .collection("user").document(userId)
                    .collection("ai_recipe")
                    .add(recipeItem)
                    .await()
                Log.d("파베업로드", "Firestore 저장 완료: ${docRef.id}")

                // 3. 저장 완료 후 화면 전환
                navController.navigate("ai_recipe/${Uri.encode(docRef.id)}") {
                    popUpTo("fridge") { inclusive = false }
                    launchSingleTop = true
                }
                aiViewModel.resetDone()
                onDismiss()
            } catch (e: Exception) {
                Log.e("AI_RECIPE_ERROR", "업로드/DB저장/화면전환 실패: ${e.message}")
                // 사용자 안내 등 추가 처리 가능
            }
        }
    }







    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // 헤더
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "AI 레시피 추천",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF212121)
                        )
                        Text(
                            "${ingredients.size}가지 재료 활용",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF757575)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 재료 리스트
                Text(
                    "사용된 재료",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                ) {
                    items(ingredients) { ingredient ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    getEmojiForIngredient(ingredient.name),
                                    fontSize = 24.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        ingredient.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF424242)
                                    )
                                    Text(
                                        "수량: ${ingredient.quantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // AI 추천 버튼
                Spacer(Modifier.height(20.dp))

                if (ui.isSending) {
                    LinearProgressIndicator(
                        progress = progress.value,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        trackColor = Color(0xFFE0E0E0),
                        color      = Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // 3) 항상 버튼은 보여주되, 로딩 중이면 비활성화
                Button(
                    onClick = {
                        val selected = ingredients.map { it.name }.joinToString(", ")
                        aiViewModel.onInputChange(selected)
                        aiViewModel.sendMessage(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = !ui.isSending,                  // ← 로딩 중엔 눌리지 않도록
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector   = Icons.Default.Restaurant,
                        contentDescription = "AI 추천",
                        tint          = Color.White,
                        modifier      = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (ui.isSending) "로딩 중..." else "AI 추천",  // 상태에 따라 텍스트 변경
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(12.dp))

                // 4) 닫기 버튼
                OutlinedButton(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("닫기")
                }
            }
        }
    }
}

suspend fun waitUntilImageIsAvailable(url: String, maxAttempts: Int = 10, delayMs: Long = 800): Boolean {
    repeat(maxAttempts) { attempt ->
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 1500
            connection.readTimeout = 1500
            val code = connection.responseCode
            Log.d("IMAGE_WAIT", "시도 ${attempt + 1}: response=$code for $url")
            connection.disconnect()
            if (code == 200) return true
        } catch (e: Exception) {
            Log.d("IMAGE_WAIT", "시도 중 에러: ${e.message}")
        }
        delay(delayMs)
    }
    return false
}