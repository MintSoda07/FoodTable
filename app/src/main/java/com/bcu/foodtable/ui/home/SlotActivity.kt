package com.bcu.foodtable.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat

class SlotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 엣지-투-엣지: 시스템바 투명
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // 앱의 MaterialTheme로 감싸주세요(프로젝트 테마 사용)
            HiddenScreen()
        }
    }
}
