package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.bcu.foodtable.useful.FirebaseHelper
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.JetpackCompose.Subscribe.Channel.EditRecipeScreen
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingScreen
import kotlinx.coroutines.launch

class RecipeCookingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recipeId = intent.getStringExtra("recipe_id")
        if (recipeId.isNullOrEmpty()) {
            Toast.makeText(this, "레시피 ID가 유효하지 않습니다.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            // 1) NavController 생성
            val navController = rememberNavController()

            // 2) 전체 NavHost 정의
            NavHost(
                navController = navController,
                startDestination = "cooking/$recipeId"
            ) {
                // ───────────────────────────────────────────────────
                //  a) 레시피 보기 화면: route = "cooking/{recipeId}"
                // ───────────────────────────────────────────────────
                composable(
                    route = "cooking/{recipeId}",
                    arguments = listOf(navArgument("recipeId") {
                        type = NavType.StringType
                    })
                ) { backStackEntry ->
                    // route 인자로 받은 recipeId
                    val id = backStackEntry.arguments?.getString("recipeId") ?: ""
                    // 내부 상태 로딩
                    var recipe by remember { mutableStateOf<RecipeItem?>(null) }
                    var isLoading by remember { mutableStateOf(true) }
                    var errorMessage by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(id) {
                        isLoading = true
                        errorMessage = null
                        try {
                            val result = FirebaseHelper.getDocumentById(
                                "recipe",
                                id,
                                RecipeItem::class.java
                            )
                            if (result != null) {
                                recipe = result.apply { this.id = id }
                            } else {
                                errorMessage = "레시피를 불러올 수 없습니다."
                            }
                        } catch (e: Exception) {
                            errorMessage = "레시피 로딩 중 오류: ${e.localizedMessage}"
                        }
                        isLoading = false
                    }

                    // 3) UI 분기
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (recipe != null) {
                        // **RecipeCookingScreen** 에는 NavController를 넘겨서 내부에서 '수정'으로 이동 가능하게 함
                        RecipeCookingScreen(
                            recipe = recipe!!,
                            navController = navController
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("레시피를 불러올 수 없습니다.")
                                errorMessage?.let { Text(it) }
                            }
                        }
                    }
                }

                // ───────────────────────────────────────────────────
                //  b) 레시피 수정 화면: route = "edit/{recipeId}/{channelName}"
                // ───────────────────────────────────────────────────
                composable(
                    route = "edit/{recipeId}/{channelName}",
                    arguments = listOf(
                        navArgument("recipeId")    { type = NavType.StringType },
                        navArgument("channelName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val id          = backStackEntry.arguments?.getString("recipeId")    ?: ""
                    val channelName = backStackEntry.arguments?.getString("channelName") ?: ""

                    // 현재 Activity 레퍼런스
                    val activity = (LocalContext.current as? Activity)

                    EditRecipeScreen(
                        recipeId        = id,
                        channelName     = channelName,
                        onModifySuccess = {
                            // 수정 완료 시에는 그냥 뒤로
                            navController.popBackStack()
                        },
                        onDeleteSuccess = {
                            setResult(Activity.RESULT_OK)
                            // 삭제 완료 시에는 이 Activity 자체를 종료해서 Home으로 돌아감
                            activity?.finish()
                        }
                    )
                }

            }
        }
    }
}
