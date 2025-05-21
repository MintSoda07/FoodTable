package com.bcu.foodtable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.bcu.foodtable.ui.home.HomeScreen

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ HomeViewModel은 기본 생성자만 사용
        val homeViewModel = HomeViewModel()
        setContent {
            FoodTableTheme {
                HomeScreen(viewModel = homeViewModel)
            }

        }
    }
}
