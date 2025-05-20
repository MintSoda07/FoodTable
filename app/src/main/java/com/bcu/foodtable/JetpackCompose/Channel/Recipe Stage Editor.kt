package com.bcu.foodtable.JetpackCompose.Channel


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun RecipeStageEditor(
    steps: List<String>,
    onStepAdded: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var useTimer by remember { mutableStateOf(false) }
    var cookingMethod by remember { mutableStateOf("") }
    var timerHour by remember { mutableStateOf("") }
    var timerMin by remember { mutableStateOf("") }
    var timerSec by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("단계 제목") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("설명") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = useTimer, onCheckedChange = { useTimer = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text("타이머 사용")
        }

        if (useTimer) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = cookingMethod,
                onValueChange = { cookingMethod = it },
                label = { Text("조리 방식") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = timerHour,
                    onValueChange = { timerHour = it },
                    label = { Text("시") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = timerMin,
                    onValueChange = { timerMin = it },
                    label = { Text("분") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = timerSec,
                    onValueChange = { timerSec = it },
                    label = { Text("초") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (title.isNotBlank() && description.isNotBlank()) {
                val stepIndex = steps.size + 1
                val timeString = if (useTimer) String.format("%02d:%02d:%02d",
                    timerHour.toIntOrNull() ?: 0,
                    timerMin.toIntOrNull() ?: 0,
                    timerSec.toIntOrNull() ?: 0) else null
                val stepText = if (useTimer && timeString != null) {
                    "$stepIndex.($title) $description ($cookingMethod, $timeString)"
                } else {
                    "$stepIndex.($title) $description"
                }
                onStepAdded(stepText)
                title = ""
                description = ""
                cookingMethod = ""
                timerHour =""
                timerMin =""
                timerSec = ""
                useTimer = false
            }
        }) {
            Text("단계 추가")
        }

        Spacer(modifier = Modifier.height(12.dp))

        steps.forEach { step ->
            Text(text = step, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
