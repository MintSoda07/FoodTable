package com.bcu.foodtable.JetpackCompose.Recipe

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun VoiceControlSection(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Button(onClick = if (isRunning) onStop else onStart) {
        Text(if (isRunning) "음성 종료" else "음성 시작")
    }
}
