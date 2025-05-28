package com.bcu.foodtable.ui.home


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 필요시 색상 설정
private val LightColors = lightColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color.White,
    secondary = Color(0xFFFFC107),
    onSecondary = Color.Black,
    background = Color(0xFFF5F5F5),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val WarmLightColorScheme = lightColorScheme(
    primary = Color(0xFFE26D47),
    onPrimary = Color.White,
    secondary = Color(0xFFFFF3EE),
    onSecondary = Color(0xFF4B4B4B),
    background = Color(0xFFFFF9F6),
    onBackground = Color(0xFF3A3A3A),
    surface = Color.White,
    onSurface = Color(0xFF3A3A3A),
    surfaceVariant = Color(0xFFFFE2D6), //  추가: 살구색 느낌 (연한 primary 변형)
    onSurfaceVariant = Color(0xFF5F5F5F), //  추가: 대비용 텍스트 색
    outline = Color(0xFFE0E0E0)
)


@Composable
fun FoodTableTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WarmLightColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}