package com.bcu.foodtable.JetpackCompose

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bcu.foodtable.JetpackCompose.Channel.ChannelScreen
import com.bcu.foodtable.JetpackCompose.Channel.SubscribeScreen
import com.bcu.foodtable.ui.home.HomeScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home" // ✅ 홈 화면이 기본
    ) {
        // ✅ 홈 화면
        composable("home") {
            HomeScreen(navController = navController)
        }

        // ✅ 채널 허브 화면
        composable("subscribe") {
            SubscribeScreen(navController = navController)
        }

        // ✅ 채널 상세
        composable(
            "channel/{channelName}",
            arguments = listOf(navArgument("channelName") { defaultValue = "" })
        ) { backStackEntry ->
            val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
            ChannelScreen(
                channelName = channelName,
                onRecipeClick = { recipeId ->
                    navController.navigate("recipe/$recipeId")
                },
                onWriteClick = { chName ->
                    navController.navigate("write/$chName")
                }
            )
        }
    }
}

//        composable(
//            route = "recipe/{recipeId}",
//            arguments = listOf(navArgument("recipeId") {})
//        ) { backStackEntry ->
//            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
//            RecipeViewScreen(recipeId = recipeId) // <- 이건 Compose 버전 필요
//        }

//        composable(
//            route = "write/{channelName}",
//            arguments = listOf(navArgument("channelName") {})
//        ) { backStackEntry ->
//            val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
//            WriteScreen(channelName = channelName) // <- 이것도 Compose 버전 필요
//        }
//    }


