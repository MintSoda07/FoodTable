package com.bcu.foodtable.JetpackCompose.RecipeStorage

import CategoriesViewModel
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity
import kotlinx.coroutines.tasks.await
import com.google.gson.Gson
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


@Composable
fun CategoryScreen(
    categoryName: String,
    navController: NavController,
    viewModel: CategoriesViewModel = viewModel()
) {
    val context = LocalContext.current
    val recipes by viewModel.categoryRecipes.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(categoryName) {
        isLoading = true
        viewModel.loadCategoryRecipes(categoryName)
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            recipes.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("해당 카테고리에 대한 레시피가 없습니다.")
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    RecipePreviewCard(
                        recipe = recipe,
                        onClick = {
                            context.startActivity(
                                Intent(context, RecipeCookingActivity::class.java).apply {
                                    putExtra("recipe_id", recipe.id)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RecipePreviewCard(
    recipe: RecipeItem,
    onClick: (() -> Unit)? = null
) {
    var isLiked by remember { mutableStateOf(recipe.likedUsers.isNotEmpty()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 선명한 배경 이미지
            AsyncImage(
                model = recipe.imageResId,
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 상단 좋아요 버튼
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
            ) {
                IconButton(onClick = { isLiked = !isLiked }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 하단 텍스트 배경 + 내용
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "⏱ ${recipe.duration}분 | ${recipe.C_categories.getOrNull(1) ?: "난이도 없음"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
