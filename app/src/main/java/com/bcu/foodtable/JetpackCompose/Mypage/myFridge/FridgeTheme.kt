package com.bcu.foodtable.JetpackCompose.Mypage.myFridge.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun FridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(), // 원하는 색상 조합 설정 가능
        typography = Typography(),
        content = content
    )
}
