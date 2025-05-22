package com.bcu.foodtable.JetpackCompose.AI

import com.bcu.foodtable.JetpackCompose.AI.AiChattingViewModel
import com.bcu.foodtable.JetpackCompose.AI.AiHelperViewModel
import com.bcu.foodtable.JetpackCompose.AI.AiRecommendationViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable



sealed class AiScreen(val route: String, val label: String) {
    object Chat : AiScreen("ai_chatting", "채팅")
    object Helper : AiScreen("ai_helper", "재료입력")
    object Recommendation : AiScreen("ai_recommendation", "추천결과")
}


@Composable
fun AiNavGraph(
    navController: NavHostController,
    aiChattingViewModel: AiChattingViewModel,
    aiHelperViewModel: AiHelperViewModel,
    aiRecommendationViewModel: AiRecommendationViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AiScreen.Chat.route,
        modifier = modifier
    ) {
        composable(AiScreen.Chat.route) {
            AiChattingScreen(viewModel = aiChattingViewModel)
        }
        composable(AiScreen.Helper.route) {
            AiHelperScreen(viewModel = aiHelperViewModel)
        }
        composable(AiScreen.Recommendation.route) {
            AiRecommendationScreen(viewModel = aiRecommendationViewModel)
        }
    }
}

