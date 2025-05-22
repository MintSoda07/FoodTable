import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.JetpackCompose.AI.AiChattingViewModel
import com.bcu.foodtable.JetpackCompose.AI.AiHelperViewModel
import com.bcu.foodtable.JetpackCompose.AI.AiRecommendationViewModel
import com.bcu.foodtable.JetpackCompose.AI.BottomNavigationBar
import com.bcu.foodtable.JetpackCompose.AI.AiNavGraph
@Composable
fun MainAiScreen() {
    val navController = rememberNavController()

    // ViewModel 수동 생성
    val openAIClient = remember { OpenAIClient() }
    val aiChattingViewModel = remember { AiChattingViewModel(openAIClient) }
    val aiHelperViewModel = remember { AiHelperViewModel(openAIClient) }
    val aiRecommendationViewModel = remember { AiRecommendationViewModel(openAIClient) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        AiNavGraph(
            navController = navController,
            aiChattingViewModel = aiChattingViewModel,
            aiHelperViewModel = aiHelperViewModel,
            aiRecommendationViewModel = aiRecommendationViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
