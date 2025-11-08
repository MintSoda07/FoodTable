package com.bcu.foodtable.JetpackCompose.Mypage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StepProgressBar(current: Int, goal: Int) {
    val progress = (current / goal.toFloat()).coerceIn(0f, 1f)

    Column {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$current / $goal 걸음",
            fontSize = 14.sp
        )
    }
}
