package com.bcu.foodtable.JetpackCompose.View

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeMeta(
    categories: List<String>,
    tags: List<String>,
    note: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (categories.isNotEmpty()) {
            Text("카테고리", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { Text(text = "#${it}", style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (tags.isNotEmpty()) {
            Text("태그", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (note.isNotBlank()) {
            Text("비고", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = note, style = MaterialTheme.typography.bodySmall)
        }
    }
}
