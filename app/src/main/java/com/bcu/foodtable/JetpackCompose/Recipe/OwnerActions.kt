package com.bcu.foodtable.JetpackCompose.Recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun OwnerActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = onEdit) {
            Text("수정")
        }
        Button(onClick = onDelete) {
            Text("삭제")
        }
    }
}
