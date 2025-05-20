package com.bcu.foodtable.JetpackCompose.Channel



import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun IngredientsEditor(
    ingredients: List<String>,
    onAddIngredient: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("재료 입력") },
                modifier = Modifier.weight(1f)
            )

            Button(onClick = {
                if (input.isNotBlank()) {
                    onAddIngredient(input)
                    input = ""
                }
            }) {
                Text("추가")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ingredients.forEach { item ->
            Text(text = "\u2022 $item", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
