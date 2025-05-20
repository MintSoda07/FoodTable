package com.bcu.foodtable.JetpackCompose.Channel



import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsEditor(
    tags: List<String>,
    onAddTag: (String) -> Unit
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
                label = { Text("태그 입력 (예: 건강한)") },
                modifier = Modifier.weight(1f)
            )

            Button(onClick = {
                if (input.isNotBlank()) {
                    val tag = if (input.startsWith("#")) input else "#${input}"
                    onAddTag(tag)
                    input = ""
                }
            }) {
                Text("추가")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag ->
                AssistChip(onClick = {}, label = { Text(tag) })
            }
        }
    }
}
