package com.bcu.foodtable.JetpackCompose.AI

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.R


class AiMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainAiScreen()
            }
        }
    }
}

@Composable
fun MainAiScreen() {
    val navController = rememberNavController()

    // Manual DI: OpenAIClient 한 번만 생성
    val openAIClient = remember { OpenAIClient() }

    val aiChattingViewModel = remember { AiChattingViewModel(openAIClient) }
    val aiHelperViewModel = remember { AiHelperViewModel(openAIClient) }
    val aiRecommendationViewModel = remember { AiRecommendationViewModel(openAIClient) }

    AiNavGraph(
        navController = navController,
        aiChattingViewModel = aiChattingViewModel,
        aiHelperViewModel = aiHelperViewModel,
        aiRecommendationViewModel = aiRecommendationViewModel
    )
}
