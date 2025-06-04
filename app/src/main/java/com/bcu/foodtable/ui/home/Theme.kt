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
    primary = Color(0xFFE25532),           // 강조 주색 (강렬한 주황-레드)
    onPrimary = Color.White,               // primary 위 텍스트 (흰색)

    primaryContainer = Color(0xFFFFE2D6),  // 연한 오렌지 배경 (주요 배경 강조)
    onPrimaryContainer = Color(0xFF5C2B1B),// primaryContainer 위 텍스트

    secondary = Color(0xFFFFF4ED),         // 살구빛 보조 배경
    onSecondary = Color(0xFF4B3C35),       // 보조 텍스트 (살짝 짙은 갈색)

    secondaryContainer = Color(0xFFFDE1D5),// 부드러운 살구 베이스 카드
    onSecondaryContainer = Color(0xFF5D4037), // 진한 살구 위 텍스트

    tertiary = Color(0xFFB9806D),          // 부드러운 브라운 강조 요소
    onTertiary = Color.White,              // tertiary 위 텍스트

    tertiaryContainer = Color(0xFFF3E0DC), //  tertiary를 담는 배경
    onTertiaryContainer = Color(0xFF4E342E), // 📎 tertiary 배경 위 텍스트

    background = Color(0xFFFFFBF8),        // 전체 배경 (따뜻한 아이보리)
    onBackground = Color(0xFF3A2C28),      // 배경 위 일반 텍스트 (짙은 갈색)

    surface = Color.White,                 // 카드/버튼 배경
    onSurface = Color(0xFF2E2E2E),         // 카드/버튼 위 글자

    surfaceVariant = Color(0xFFFBE7DF),    // 연한 살구톤 카드 or 뷰 구분
    onSurfaceVariant = Color(0xFF5F5F5F),  // 대비용 보조 텍스트

    outline = Color(0xFFDDC7BD),           // 테두리, Divider 등
    outlineVariant = Color(0xFFF0E0D8),    // 좀 더 연한 테두리용

    inverseSurface = Color(0xFF3A2C28),    // 다크 느낌의 서피스 (플로팅 등 반전용)
    inverseOnSurface = Color.White,        // inverseSurface 위 텍스트
    inversePrimary = Color(0xFFFF8F6B),    // 반전용 primary (연주황)

    error = Color(0xFFD32F2F),             // 에러/경고
    onError = Color.White,                 // 에러 텍스트 (흰색)
    errorContainer = Color(0xFFFDECEA),    // 에러 배경
    onErrorContainer = Color(0xFF8B0000)   // 에러 배경 위 텍스트
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