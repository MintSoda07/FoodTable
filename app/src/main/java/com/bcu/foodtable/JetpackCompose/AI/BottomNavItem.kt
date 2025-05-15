package com.bcu.foodtable.JetpackCompose.AI

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    Home("home", Icons.Filled.Home, "홈"),
    AiChat("ai_chat", Icons.Filled.Chat, "AI"),
    MyPage("my_page", Icons.Filled.Person, "내 정보")
}
