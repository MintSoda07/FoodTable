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
import com.bcu.foodtable.useful.RecipeItem

@Composable
fun AiRecipeScreenPreview() {
    // 현실적인 AI 추천 예시
    val aiRecipe = RecipeItem(
        name = "트러플 크림 리조또",
        description = "고소하고 진한 트러플 향이 퍼지는 이탈리안 리조또. 부드러운 크림과 신선한 파르미지아노 레지아노 치즈, 그리고 버섯의 풍미가 가득해요.",
        imageResId = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=800&q=80",
        ingredients = listOf("쌀(리조또용)", "양송이버섯", "트러플 오일", "파르미지아노 치즈", "양파", "생크림", "올리브유", "소금", "후추"),
        order = """
            ○1. (준비) 쌀은 찬물에 살짝 헹군 뒤 체에 받쳐둡니다.
            ○2. (볶기) 양파와 버섯을 올리브유에 투명해질 때까지 볶아요.
            ○3. (쌀 넣기) 쌀을 넣고 중불에서 2분간 함께 볶아줍니다.
            ○4. (육수 추가) 뜨거운 육수를 조금씩 부으면서 계속 저어줍니다.
            ○5. (완성) 쌀이 거의 익으면 생크림, 치즈, 트러플 오일을 넣고 마무리합니다.
        """.trimIndent(),
        tags = listOf("트러플", "고급", "이탈리안", "파티"),
        id = "ai-20240612"
    )

    AiRecipeScreenPreviewContent(aiRecipe = aiRecipe)
}

@Composable
fun AiRecipeScreenPreviewContent(aiRecipe: RecipeItem) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
    ) {
        // 제목 카드
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        aiRecipe.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        aiRecipe.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // 이미지 카드 (이미지 있는 경우)
        if (!aiRecipe.imageResId.isNullOrBlank()) {
            item {
                AsyncImage(
                    model = aiRecipe.imageResId,
                    contentDescription = "레시피 이미지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.LightGray)
                )
            }
        }
        // 재료 카드
        if (!aiRecipe.ingredients.isNullOrEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "재료",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        aiRecipe.ingredients.forEach { ing ->
                            Text("• $ing", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        // 단계 카드
        val steps = aiRecipe.order.split("○").filter { it.isNotBlank() }
        if (steps.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "조리 단계",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        steps.forEachIndexed { idx, step ->
                            Text(
                                "${idx + 1}. ${step.trim()}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        // 태그
        if (!aiRecipe.tags.isNullOrEmpty()) {
            item {
                val maxPerRow = 4
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    aiRecipe.tags.chunked(maxPerRow).forEach { rowTags ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowTags.forEach { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                }
            }
        }
        // 저장 버튼 (실제 ai_recipe에서는 onSaveToChannel 전달)
        item {
            Button(
                onClick = { /* 실제로는 onSaveToChannel(aiRecipe) */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("내 채널에 저장", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
