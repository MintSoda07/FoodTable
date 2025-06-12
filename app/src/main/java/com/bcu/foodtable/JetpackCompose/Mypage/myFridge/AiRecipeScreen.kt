import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bcu.foodtable.useful.RecipeItem

// RecipePreviewContent()를 따로 파일에 뺐으면 거기 import 필요

@Composable
fun AiRecipeScreen(
    aiRecipe: RecipeItem,
    navController: NavController,
    onSaveToChannel: (RecipeItem) -> Unit
) {
    // 1. 기존 RecipeCookingScreen의 메인 UI(카드, 재료, 단계 등) 그대로 가져오기
    RecipePreviewContent(aiRecipe)

    // 2. 하단에 저장 버튼
    Button(
        onClick = { onSaveToChannel(aiRecipe) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("내 채널에 저장", style = MaterialTheme.typography.titleMedium)
    }
}
