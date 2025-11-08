package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.JetpackCompose.AI.AiHelperViewModel
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FuturisticDialog(
    ingredients: List<Ingredient>,
    recipes: List<String>,
    navController: NavController,
    aiViewModel: AiHelperViewModel,
    onDismiss: () -> Unit,
    userId: String
) {
    val context = LocalContext.current
    val ui by aiViewModel.uiState.collectAsState()

    // 결정론적 진행 게이지
    val progress = remember { Animatable(0f) }
    LaunchedEffect(ui.isSending) {
        if (ui.isSending) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 17_000, easing = LinearEasing))
        } else {
            progress.snapTo(1f)
        }
    }

    // Lottie
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.food_prep))
    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 1.0f
    )

    // 5초마다 문구 변경
    val tips = remember {
        listOf(
            "신선함을 정렬하는 중...",
            "냉장고 온도 보정 중...",
            "유통기한을 스캔하는 중...",
            "AI 셰프가 레시피를 예열 중...",
            "맛의 조합을 시뮬레이션 중..."
        )
    }
    var tipIndex by remember { mutableStateOf(0) }
    LaunchedEffect(ui.isSending) {
        while (ui.isSending) {
            delay(5_000)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    // 완료 시 저장/이동
    LaunchedEffect(ui.done) {
        if (ui.done) {
            try {
                val storagePath = aiViewModel.uploadImageToFirebaseStorage(
                    context = context,
                    imageUrl = ui.imageUrl!!,
                    userId = userId
                )
                val encodedPath = storagePath.replace("/", "%2F")
                val recipeItem = RecipeItem(
                    id = "ai_${System.currentTimeMillis()}",
                    name = ui.recipes.firstOrNull().orEmpty(),
                    description = "",
                    imageResId = encodedPath,
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

                navController.navigate("ai_recipe/${Uri.encode(docRef.id)}") {
                    popUpTo("fridge") { inclusive = false }
                    launchSingleTop = true
                }
                aiViewModel.resetDone()
                onDismiss()
            } catch (e: Exception) {
                Log.e("AI_RECIPE_ERROR", "업로드/DB저장/화면전환 실패: ${e.message}")
            }
        }
    }

    // Dialog: 플랫폼 dim은 유지되지만, 카드 내부는 100% 불투명한 흰색/무그림자
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 전체 폭 제어
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        val shape = RoundedCornerShape(20.dp)

        // 중앙 정렬 박스
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .wrapContentSize(Alignment.Center)
        ) {
            // 완전한 흰색 + 선명한 테두리 + 그림자 0
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape),
                shape = shape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // 🔴 그림자 제거
                border = BorderStroke(2.dp, Color(0xFF9EC9FF)) // 🔵 선명한 불투명 테두리(원하면 제거 가능)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 헤더
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                            modifier = Modifier.size(52.dp)
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
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                "AI 레시피 추천",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF212121),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${ingredients.size}가지 재료 활용",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    // 로딩 섹션 (흰 배경 유지, 회색/반투명 레이어 없음)
                    if (ui.isSending) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LottieAnimation(
                                composition = composition,
                                progress = { lottieProgress },
                                modifier = Modifier.size(140.dp)
                            )
                            AnimatedContent(
                                targetState = tipIndex,
                                transitionSpec = {
                                    (fadeIn(tween(250)) + slideInVertically { it / 3 }) togetherWith
                                            (fadeOut(tween(200)) + slideOutVertically { -it / 3 })
                                },
                                label = "loadingTip"
                            ) { idx ->
                                Text(
                                    text = tips[idx],
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF3F51B5),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = progress.value,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp), // 모서리 라운드 제거해 더 선명
                                trackColor = Color(0xFFE0E0E0),
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }

                    // 재료 리스트 (완전한 흰 배경 유지)
                    Text(
                        "사용된 재료",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF263238)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        items(ingredients) { ingredient ->
                            // 각 아이템 카드도 그림자/반투명 배경 제거
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFFE3EAF5))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(getEmojiForIngredient(ingredient.name), fontSize = 22.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            ingredient.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF212121),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "수량: ${ingredient.quantity}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF5F6B7A)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 액션
                    Button(
                        onClick = {
                            val selected = ingredients.joinToString(", ") { it.name }
                            aiViewModel.onInputChange(selected)
                            aiViewModel.sendMessage(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !ui.isSending,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp) // 🔴 버튼 그림자 제거
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (ui.isSending) "로딩 중..." else "AI 추천",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp) // 선명한 외곽선
                    ) {
                        Text("닫기")
                    }
                }
            }
        }
    }
}
