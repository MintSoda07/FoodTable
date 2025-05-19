package com.bcu.foodtable.JetpackCompose.AI

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
    modifier: Modifier = Modifier // ✅ 기본값 포함
) {
    NavHost(
        navController = navController,
        startDestination = AiScreen.Chat.route,
        modifier = modifier // ✅ 여기 반영
    ) {
        composable(AiScreen.Chat.route) { AiChattingScreen() }
        composable(AiScreen.Helper.route) { AiHelperScreen() }
        composable(AiScreen.Recommendation.route) { AiRecommendationScreen() }
    }
}

