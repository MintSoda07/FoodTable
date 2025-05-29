package com.bcu.foodtable.JetpackCompose.screens

import androidx.compose.foundation.content.MediaType.Companion.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun CardGameScreen(navController: NavController) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("카드 뒤집기 게임", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Text("여기에 카드 UI를 구현하세요")

        Spacer(Modifier.height(24.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("돌아가기")
        }
    }
}
