package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.theme.FridgeTheme //

class FridgeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FridgeApp()
        }
    }
}
@Composable
fun FridgeApp() {
    val navController = rememberNavController()
    val fridgeViewModel: FridgeViewModel = viewModel()

    FridgeTheme {
        NavHost(navController = navController, startDestination = "fridge") {
            composable("fridge") {
                FridgeScreen(viewModel = fridgeViewModel, navController = navController)
            }
            composable("add_ingredient?section={section}") { backStackEntry ->
                val section = backStackEntry.arguments?.getString("section") ?: "냉장"
                AddIngredientScreen(
                    viewModel = fridgeViewModel,
                    navController = navController,
                    section = section
                )
            }
        }
    }
}