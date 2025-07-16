package com.bcu.foodtable.JetpackCompose.RecipeStorage

import CategoriesViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.tasks.await


@Composable
fun CategoryScreen(
    categoryName: String,
    viewModel: CategoriesViewModel = viewModel()
) {
    val recipes by viewModel.categoryRecipes.collectAsState()

    LaunchedEffect(categoryName) {
        viewModel.loadCategoryRecipes(categoryName)
    }

    Column {
        // 상단 제목
        Text(
            text = categoryName,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            items(recipes) { recipe ->
                Text(text = recipe.name)
                // ModernRecipeCard 등 대체 가능
            }
        }
    }
}
