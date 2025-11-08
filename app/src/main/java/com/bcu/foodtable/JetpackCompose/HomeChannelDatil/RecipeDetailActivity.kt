//package com.bcu.foodtable.JetpackCompose.HomeChannelDatil
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log // Log import 추가
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column // Column 추가
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Text // Text 추가
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import com.bcu.foodtable.ui.subscribeNavMenu.EditRecipeActivity
//import com.bcu.foodtable.useful.FirebaseHelper
//import com.bcu.foodtable.useful.RecipeItem
//import com.google.firebase.firestore.FirebaseFirestore
//
//class RecipeDetailActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val recipeId = intent.getStringExtra("recipe_id")
//        // Log 1: recipeId 확인
//        Log.d("RecipeDetailActivity_Debug", "Log 1: Received recipeId: $recipeId")
//
//        if (recipeId == null) {
//            Log.e("RecipeDetailActivity_Debug", "Log 1.1: recipeId is null, finishing activity.")
//            Toast.makeText(this, "레시피 ID가 없습니다.", Toast.LENGTH_LONG).show()
//            finish()
//            return
//        }
//
//        setContent {
//            var recipe by remember { mutableStateOf<RecipeItem?>(null) }
//            var isLoading by remember { mutableStateOf(true) } // 로딩 상태 추가
//            var errorMessage by remember { mutableStateOf<String?>(null) } // 에러 메시지 상태 추가
//
//            // Log 2: setContent 블록 진입 확인
//            Log.d("RecipeDetailActivity_Debug", "Log 2: setContent called.")
//
//            LaunchedEffect(recipeId) {
//                // Log 3: LaunchedEffect 시작 확인
//                Log.d("RecipeDetailActivity_Debug", "Log 3: LaunchedEffect started for recipeId: $recipeId")
//                isLoading = true
//                errorMessage = null
//                try {
//                    val result = FirebaseHelper.getDocumentById("recipe", recipeId, RecipeItem::class.java)
//                    // Log 4: FirebaseHelper 결과 확인
//                    if (result != null) {
//                        Log.d("RecipeDetailActivity_Debug", "Log 4: FirebaseHelper returned a result. Recipe name: ${result.name}")
//                        recipe = result.apply { id = recipeId }
//                    } else {
//                        Log.w("RecipeDetailActivity_Debug", "Log 4: FirebaseHelper returned null for recipeId: $recipeId")
//                        errorMessage = "레시피를 불러오지 못했습니다 (결과 없음)."
//                    }
//                } catch (e: Exception) {
//                    Log.e("RecipeDetailActivity_Debug", "Log 4.1: Exception while fetching recipe for recipeId: $recipeId", e)
//                    errorMessage = "레시피 로딩 중 오류 발생: ${e.message}"
//                }
//                isLoading = false
//                // Log 5: LaunchedEffect 종료 확인
//                Log.d("RecipeDetailActivity_Debug", "Log 5: LaunchedEffect finished. isLoading: $isLoading, recipe is null: ${recipe == null}")
//            }
//
//            if (isLoading) {
//                // Log 6: 로딩 중 UI 표시
//                Log.d("RecipeDetailActivity_Debug", "Log 6: Displaying loading indicator.")
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    CircularProgressIndicator()
//                }
//            } else if (recipe != null) {
//                // Log 7: RecipeDetailScreen 호출
//                Log.d("RecipeDetailActivity_Debug", "Log 7: Recipe data loaded, calling RecipeDetailScreen. Recipe name: ${recipe!!.name}")
//                RecipeDetailScreen(
//                    recipe = recipe!!, // recipe가 null이 아님을 확신 (위 if 조건)
//                    onEditClick = {
//                        startActivity(Intent(this, EditRecipeActivity::class.java).apply {
//                            putExtra("recipe_id", recipeId)
//                        })
//                    },
//                    onDeleteClick = {
//                        FirebaseFirestore.getInstance().collection("recipe")
//                            .document(recipeId)
//                            .delete()
//                            .addOnSuccessListener {
//                                Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
//                                finish()
//                            }
//                    }
//                )
//            } else {
//                // Log 8: 데이터 로드 실패/오류 UI 표시
//                Log.d("RecipeDetailActivity_Debug", "Log 8: Recipe is null after loading, displaying error message: $errorMessage")
//                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                        Text("레시피를 불러올 수 없습니다.")
//                        errorMessage?.let { Text(it) }
//                    }
//                }
//            }
//        }
//    }
//}