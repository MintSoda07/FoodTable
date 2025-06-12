import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.navigation.NavController
import com.bcu.foodtable.useful.RecipeItem

@Composable
fun AiRecipeScreen(
    recipe: RecipeItem,
    navController: NavController,
    onSaveToChannel: (RecipeItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. 이미지 (있을 때만)
        if (!recipe.imageResId.isNullOrBlank()) {
            item {
                AsyncImage(
                    model = recipe.imageResId,
                    contentDescription = recipe.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.LightGray),
                )
            }
        }

        // 2. 제목
        item {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.headlineMedium.copy(color = Color(0xFF7A4AE2)),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // 3. 설명
        if (!recipe.description.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EFFF))
                ) {
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        }

        // 4. 카테고리/태그 (있는 경우)
        if (!recipe.C_categories.isNullOrEmpty() || !recipe.tags.isNullOrEmpty()) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                ) {
                    recipe.C_categories.forEach {
                        Chip(text = it, bgColor = Color(0xFF7A4AE2).copy(alpha = 0.10f))
                    }
                    recipe.tags.forEach {
                        Chip(text = if (it.startsWith("#")) it else "#$it", bgColor = Color(0xFF6A47B8).copy(alpha = 0.08f))
                    }
                }
            }
        }

        // 5. 재료 카드
        if (!recipe.ingredients.isNullOrEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3D5F9))
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("재료", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF7A4AE2)))
                        Spacer(Modifier.height(6.dp))
                        recipe.ingredients.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // 6. 단계 카드
        val steps = recipe.order.split("○").filter { it.isNotBlank() }
        if (steps.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F3FB))
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("조리 단계", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF7A4AE2)))
                        Spacer(Modifier.height(8.dp))
                        steps.forEachIndexed { i, s ->
                            Text("${i + 1}. ${s.trim()}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                }
            }
        }

        // 7. 하단 저장 버튼
        item {
            Button(
                onClick = { onSaveToChannel(recipe) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 10.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A4AE2))
            ) {
                Text("내 채널에 저장", style = MaterialTheme.typography.titleLarge.copy(color = Color.White))
            }
        }
    }
}

@Composable
fun Chip(text: String, bgColor: Color) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF7A4AE2)),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
