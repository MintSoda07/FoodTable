package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

// import android.speech.tts.TextToSpeech // RecipeCookingActivity에서는 직접 사용하지 않으므로 주석 처리 또는 삭제 가능
import android.os.Bundle
import android.util.Log // Log 사용을 위해 import 추가
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bcu.foodtable.useful.FirebaseHelper
import com.bcu.foodtable.useful.RecipeItem
// import com.google.firebase.firestore.FirebaseFirestore // RecipeCookingActivity에서는 직접 사용하지 않으므로 주석 처리 또는 삭제 가능

class RecipeCookingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Log 0: onCreate가 호출되었는지 확인
        Log.d("RecipeCooking_Debug", "Log 0: onCreate CALLED")

        val recipeId = intent.getStringExtra("recipe_id")
        // Log 1: 전달받은 recipeId 값 확인
        Log.d("RecipeCooking_Debug", "Log 1: Received recipeId: $recipeId")

        if (recipeId.isNullOrEmpty()) { // ID가 null이거나 비어있는 경우
            Log.e("RecipeCooking_Debug", "Log 1.1: recipeId is NULL or EMPTY. Finishing activity.")
            Toast.makeText(this, "레시피 ID가 유효하지 않습니다.", Toast.LENGTH_LONG).show()
            finish() // 액티비티 종료
            return   // 함수 종료
        }

        setContent {
            // Log 2: setContent가 호출되었는지 확인
            Log.d("RecipeCooking_Debug", "Log 2: setContent CALLED for recipeId: $recipeId")

            var recipe by remember { mutableStateOf<RecipeItem?>(null) }
            var isLoading by remember { mutableStateOf(true) } // 로딩 상태 추가
            var errorMessage by remember { mutableStateOf<String?>(null) } // 에러 메시지 상태 추가

            LaunchedEffect(recipeId) { // recipeId를 키로 사용 (recipeId가 변경되면 재실행)
                // Log 3: LaunchedEffect 시작 확인
                Log.d("RecipeCooking_Debug", "Log 3: LaunchedEffect started for recipeId: $recipeId")
                isLoading = true
                errorMessage = null
                try {
                    val result = FirebaseHelper.getDocumentById("recipe", recipeId, RecipeItem::class.java)
                    // Log 4: FirebaseHelper 결과 확인
                    if (result != null) {
                        Log.d("RecipeCooking_Debug", "Log 4: FirebaseHelper returned a result. Recipe name: ${result.name}")
                        recipe = result.apply { id = recipeId } // 이미 id가 recipeId와 같다면 이 부분은 생략 가능
                    } else {
                        Log.w("RecipeCooking_Debug", "Log 4: FirebaseHelper returned null for recipeId: $recipeId")
                        errorMessage = "레시피를 불러오지 못했습니다 (결과 없음)."
                    }
                } catch (e: Exception) {
                    Log.e("RecipeCooking_Debug", "Log 4.1: Exception while fetching recipe for recipeId: $recipeId", e)
                    errorMessage = "레시피 로딩 중 오류 발생: ${e.message}"
                }
                isLoading = false
                // Log 5: LaunchedEffect 종료 확인
                Log.d("RecipeCooking_Debug", "Log 5: LaunchedEffect finished. isLoading: $isLoading, recipe is null: ${recipe == null}, errorMessage: $errorMessage")
            }

            if (isLoading) {
                // Log 6: 로딩 중 UI 표시
                Log.d("RecipeCooking_Debug", "Log 6: Displaying loading indicator.")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (recipe != null) {
                // Log 7: RecipeCookingScreen 호출
                Log.d("RecipeCooking_Debug", "Log 7: Recipe data loaded, calling RecipeCookingScreen. Recipe name: ${recipe!!.name}")
                RecipeCookingScreen(recipe = recipe!!) // recipe가 null이 아님을 확신
            } else {
                // Log 8: 데이터 로드 실패/오류 UI 표시
                Log.d("RecipeCooking_Debug", "Log 8: Recipe is null after loading, displaying error message: $errorMessage")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("레시피를 불러올 수 없습니다.")
                        errorMessage?.let { Text(it) }
                    }
                }
            }
        }
        Log.d("RecipeCooking_Debug", "Log 9: onCreate SUCCESSFULLY FINISHED (setContent was called if recipeId was valid)")
    }
}