package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.RecipeItem


@Composable
fun RecipeDetailScreen(recipe: RecipeItem, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AsyncImage(
                model = recipe.imageResId,
                contentDescription = "레시피 이미지",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("설명: ${recipe.description}")
            Text("예상 칼로리: ${recipe.estimatedCalories ?: "알 수 없음"}")
            Spacer(modifier = Modifier.height(12.dp))

            Text("카테고리: ${recipe.C_categories.joinToString()}")
            Text("태그: ${recipe.tags.joinToString()}")
            Spacer(modifier = Modifier.height(12.dp))

            Text("비고: ${recipe.note}")
            Spacer(modifier = Modifier.height(12.dp))

            Text("조리 순서", style = MaterialTheme.typography.titleMedium)
        }

        itemsIndexed(recipe.order.split("○").filter { it.isNotBlank() }) { index, step ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "○${index + 1}. $step")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onEditClick) {
                    Text("수정")
                }
                Button(onClick = onDeleteClick, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("삭제")
                }
            }
        }
    }
}
