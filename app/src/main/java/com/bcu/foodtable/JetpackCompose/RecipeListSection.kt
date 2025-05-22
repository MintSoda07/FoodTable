package com.bcu.foodtable.JetpackCompose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.viewmodel.HomeViewModel

@Composable
fun RecipeListSection(
    viewModel: HomeViewModel = viewModel(),
    onRecipeClick: (RecipeItem) -> Unit = {}
) {
    val recipes by viewModel.recipes.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (recipes.isEmpty()) {
            // 레시피가 없을 때 표시할 빈 상태
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "레시피가 없습니다",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "다른 검색어나 필터를 시도해보세요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = recipes,
                    key = { recipe -> recipe.id }
                ) { recipe ->
                    RecipeCard(
                        title = recipe.name,
                        description = recipe.description,
                        imageUrl = recipe.imageResId,
                        saltReward = recipe.clicked,
                        onClick = { onRecipeClick(recipe) }
                    )
                }
            }
        }
    }
}
