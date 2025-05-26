package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun StepTimer(durationString: String) {
    val totalMillis = remember(durationString) {
        parseDuration(durationString)
    }

    var remainingMillis by remember { mutableStateOf(totalMillis) }
    var isRunning by remember { mutableStateOf(false) }

    val formattedTime = remember(remainingMillis) {
        formatMillis(remainingMillis)
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (remainingMillis > 0) {
                delay(1000L)
                remainingMillis -= 1000L
            }
            isRunning = false
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("남은 시간: $formattedTime", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(8.dp))

        if (!isRunning) {
            Button(onClick = { isRunning = true }) {
                Text("타이머 시작")
            }
        } else {
            Button(onClick = {
                isRunning = false
                remainingMillis = totalMillis
            }) {
                Text("초기화")
            }
        }
    }
}
fun parseDuration(duration: String): Long {
    val parts = duration.split(":").map { it.toIntOrNull() ?: 0 }
    return when (parts.size) {
        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
        2 -> (parts[0] * 60 + parts[1]) * 1000L
        else -> 0L
    }
}

fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0)
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    else
        String.format("%02d:%02d", minutes, seconds)
}
