package com.bcu.foodtable.JetpackCompose.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// 룰렛 탭
@Composable
fun MiniGameMenu(navController: NavController) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("오늘의 게임", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        GameButton("🍽️ 메뉴 정하기 - 룰렛 돌리기") {
            navController.navigate("rouletteGame")
        }

        GameButton("🍱 메뉴 정하기 - 카드 뒤집기") {
            navController.navigate("cardGame")
        }

        GameButton("🙋 누가 낼까? - 사다리 타기") {
            navController.navigate("ladderGame")
        }

        GameButton("🎯 누가 낼까? - 룰렛 돌리기") {
            navController.navigate("payerRouletteGame")
        }
    }
}

@Composable
fun GameButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(text, color = Color.White)
    }
}