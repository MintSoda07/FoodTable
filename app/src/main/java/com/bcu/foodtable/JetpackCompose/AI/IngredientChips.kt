package com.bcu.foodtable.JetpackCompose.AI

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow

@Composable
fun IngredientChips(ingredients: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp
    ) {
        ingredients.forEach { ingredient ->
            AssistChip(
                onClick = { },
                label = { Text(ingredient) }
            )
        }
    }
}
