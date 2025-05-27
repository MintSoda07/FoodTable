package com.bcu.foodtable.JetpackCompose.AI

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.bcu.foodtable.ai.OpenAIClient
// Import navigation components from HomeScreen
import com.bcu.foodtable.ui.home.HomeTopBar
import com.bcu.foodtable.ui.home.AppBottomNavigationBar
import com.bcu.foodtable.ui.home.Screen
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import com.bcu.foodtable.ui.ChallengeActivity
import com.bcu.foodtable.JetpackCompose.Channel.SubscribeActivity
import com.bcu.foodtable.JetpackCompose.RecipeStorage.RecipeStorageActivity
import com.bcu.foodtable.JetpackCompose.Mypage.ProfileMainScreen

@Composable
fun MainAiScreen(
    homeViewModel: HomeViewModel = viewModel() // HomeViewModel for user data and navigation
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    // Navigation state
    var selectedTab by remember { mutableStateOf(2) } // AI Service tab index
    val user by homeViewModel.user.collectAsState()
    
    val screens = listOf(
        Screen.Home, Screen.Subscribe, Screen.AIService, Screen.RecipeStorage, Screen.MyPage
    )

    // ViewModel 수동 생성
    val openAIClient = remember { OpenAIClient() }
    val aiChattingViewModel = remember { AiChattingViewModel(openAIClient) }
    val aiHelperViewModel = remember { AiHelperViewModel(openAIClient) }
    val aiRecommendationViewModel = remember { AiRecommendationViewModel(openAIClient) }

    LaunchedEffect(Unit) {
        homeViewModel.loadUserInfo() // Load user info for top bar
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                user = user,
                onProfileClick = {
                    selectedTab = screens.indexOf(Screen.MyPage)
                },
                onChallengeClick = {
                    context.startActivity(Intent(context, ChallengeActivity::class.java))
                }
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                screens = screens,
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    when (newTab) {
                        0 -> { // Homezz
                            (context as? androidx.activity.ComponentActivity)?.finish()
                        }
                        1 -> { // Channel
                            context.startActivity(Intent(context, SubscribeActivity::class.java))
                        }
                        2 -> { // AI Service - stay here
                            selectedTab = newTab
                        }
                        3 -> { // Recipe Storage
                            context.startActivity(Intent(context, RecipeStorageActivity::class.java))
                        }
                        4 -> { // Profile
                            selectedTab = newTab
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (selectedTab) {
            4 -> { // Profile tab
                ProfileMainScreen(paddingValues = paddingValues)
            }
            else -> { // AI Service content
                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(navController = navController)
                    },
                    modifier = Modifier.padding(paddingValues)
                ) { innerPadding ->
                    AiNavGraph(
                        navController = navController,
                        aiChattingViewModel = aiChattingViewModel,
                        aiHelperViewModel = aiHelperViewModel,
                        aiRecommendationViewModel = aiRecommendationViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
