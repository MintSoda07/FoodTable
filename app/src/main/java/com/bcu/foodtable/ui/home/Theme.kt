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
    primary = Color(0xFFE25532),
    onPrimary = Color.White,

    primaryContainer = Color(0xFFFFE2D6),
    onPrimaryContainer = Color(0xFF5C2B1B),

    secondary = Color(0xFFFFF4ED),
    onSecondary = Color(0xFF4B3C35),

    secondaryContainer = Color(0xFFFDE1D5),
    onSecondaryContainer = Color(0xFF5D4037),

    tertiary = Color(0xFFB9806D),
    onTertiary = Color.White,

    tertiaryContainer = Color(0xFFF3E0DC),
    onTertiaryContainer = Color(0xFF4E342E),

    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF3A2C28),

    surface = Color.White,
    onSurface = Color(0xFF2E2E2E),

    surfaceVariant = Color(0xFFFBE7DF),
    onSurfaceVariant = Color(0xFF5F5F5F),

    outline = Color(0xFFDDC7BD),
    outlineVariant = Color(0xFFF0E0D8),

    inverseSurface = Color(0xFF3A2C28),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFFF8F6B),

    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF8B0000)
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