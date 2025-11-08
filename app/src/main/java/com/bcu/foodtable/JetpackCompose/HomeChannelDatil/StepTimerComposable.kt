package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import StepTimerState
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun StepTimer(
    timerState: StepTimerState,
    onFinish: () -> Unit
) {
    val remaining by timerState.remainingTime
    val total = remember { timerState.remainingTime.value }
    val progress = remaining.toFloat() / total
    val timeText = formatMillis(remaining)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 진행 바
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // 남은 시간
        Text(
            text = "남은 시간: $timeText",
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


fun formatMillis(millis: Long): String {
    val totalSec = millis / 1000
    val min = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}

fun parseDuration(duration: String): Long {
    val parts = duration.split(":").map { it.toIntOrNull() ?: 0 }
    return when (parts.size) {
        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
        2 -> (parts[0] * 60 + parts[1]) * 1000L
        else -> 0L
    }
}