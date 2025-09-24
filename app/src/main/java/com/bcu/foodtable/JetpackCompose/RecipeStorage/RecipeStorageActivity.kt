package com.bcu.foodtable.JetpackCompose.RecipeStorage
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface // Material3 Surface 사용 권장
import androidx.navigation.compose.rememberNavController
// MyRecipeStorageScreen의 실제 경로로 수정해주세.
import com.bcu.foodtable.JetpackCompose.RecipeStorage.MyRecipeStorageScreen
// 앱의 테마 Composable (예: com.bcu.foodtable.ui.theme.FoodTableTheme)
import com.bcu.foodtable.ui.home.*// 실제 앱 테마로 변경해주세요.
class RecipeStorageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodTableTheme {
                val navController = rememberNavController()
                Surface {
                    MyRecipeStorageScreen(navController = navController) // ← 전달
                }
            }
        }
    }
}
