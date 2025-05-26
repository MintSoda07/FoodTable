package com.bcu.foodtable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.bcu.foodtable.ui.home.HomeScreen
import com.bcu.foodtable.di.DependencyProvider

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use DependencyProvider to get HomeViewModel with dependencies
        val dependencyProvider = DependencyProvider.getInstance()
        val homeViewModel = dependencyProvider.provideHomeViewModel(applicationContext)
        setContent {
            FoodTableTheme {
                HomeScreen(viewModel = homeViewModel)
            }

        }
    }
}
