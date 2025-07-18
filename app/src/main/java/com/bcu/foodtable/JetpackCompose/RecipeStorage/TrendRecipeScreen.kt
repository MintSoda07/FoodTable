package com.bcu.foodtable.JetpackCompose.RecipeStorage

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.google.gson.Gson
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity
import com.bcu.foodtable.R

@Composable
fun TrendRecipeScreen(
    viewModel: TrendRecipeViewModel = viewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val topRecipes by viewModel.topRecipes.collectAsState()
    val channelInfo by viewModel.channelInfo.collectAsState()
    val categoryTrends by viewModel.categoryTrends.collectAsState()
    val channelOwners by viewModel.channelOwners.collectAsState()

    val excludedCategories = setOf("쉬움", "어려움", "보통")
    val filteredCategories = categoryTrends
        .filterKeys { it !in excludedCategories }
        .entries
        .sortedByDescending { it.value }
        .take(10)

    LaunchedEffect(Unit) {
        viewModel.loadTrendData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 제목
        item {
            Column {
                Text(
                    text = "지금 가장 인기있는 레시피",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "조회수 기준 상위 3개 레시피를 소개합니다!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Top 3 카드 (가로 스크롤)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(topRecipes.take(3)) { recipe ->
                    val channel = channelInfo[recipe.contained_channel]
                    val ownerName = channelOwners[channel?.name] ?: "알 수 없음"

                    TrendHighlightCard(recipe, channel, ownerName) {
                        context.startActivity(
                            Intent(context, RecipeCookingActivity::class.java).apply {
                                putExtra("recipe_id", recipe.id)
                            }
                        )
                    }
                }
            }
        }

        // 인기 카테고리
        if (filteredCategories.isNotEmpty()) {
            item {
                Column {
                    Divider()
                    Text(
                        "📈 인기 급상승 카테고리",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredCategories) { (category, count) ->
                            AssistChip(
                                onClick = {
                                    navController.navigate("category/${category}")
                                },
                                label = {
                                    Text(text = "$category ($count)")
                                }
                            )
                        }
                    }
                }
            }
        }

        // 하단 레시피 리스트
        item {
            Divider(thickness = 1.dp)
            Text(
                "🥇 인기 레시피 둘러보기",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        items(topRecipes.drop(3)) { recipe ->
            RecipePreviewCard(recipe = recipe) {
                context.startActivity(
                    Intent(context, RecipeCookingActivity::class.java).apply {
                        putExtra("recipe_id", recipe.id)
                    }
                )
            }
        }
    }
}
@Composable
fun TrendHighlightCard(
    recipe: RecipeItem,
    channel: Channel?,
    ownerName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .height(320.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column {
            AsyncImage(
                model = recipe.imageResId,
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "⏱ ${recipe.duration}분 | 좋아요 ${recipe.likes} | ${recipe.C_categories.getOrNull(1) ?: "난이도 없음"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                if (channel != null) {
                    Text(
                        text = "${channel.name} - $ownerName 쉐프의 작품",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
