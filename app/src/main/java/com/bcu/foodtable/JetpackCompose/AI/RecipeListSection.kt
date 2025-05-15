package com.bcu.foodtable.JetpackCompose.AI

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RecipeListSection(
    recipes: List<String>,
    details: List<String>,
    onClick: (Int) -> Unit,
    onDetailClick: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        recipes.forEachIndexed { index, recipeName ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(index) },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📌 $recipeName",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (index < details.size) {
                        Text(
                            text = details[index],
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Button(
                        onClick = { onDetailClick(index) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.End)
                    ) {
                        Text("자세히 보기")
                    }
                }
            }
        }
    }
}
