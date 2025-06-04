// AiRecipeScreen.kt
package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bcu.foodtable.useful.RecipeItem

@Composable
fun AiRecipeScreen(recipe: RecipeItem, navController: NavController? = null) {
    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("AI 추천 레시피") },
//                navigationIcon = {
//                    IconButton(onClick = { navController?.popBackStack() }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
//                    }
//                }
//            )
//        },
        content = { padding ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                // 1) 레시피 이름
                item {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // 2) 설명(고정 텍스트)
                item {
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // 3) 재료 목록
                if (recipe.ingredients.isNotEmpty()) {
                    item {
                        Text(
                            text = "재료",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(recipe.ingredients) { ing ->
                        Text(
                            text = "• $ing",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 4) 조리 단계
                val steps = recipe.order
                    .split("○")
                    .filter { it.isNotBlank() }
                if (steps.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "조리 단계",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(steps) { raw ->
                        Text(
                            text = "○${raw.trim()}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    )
}
