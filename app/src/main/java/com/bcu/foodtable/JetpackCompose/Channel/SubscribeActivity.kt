package com.bcu.foodtable.JetpackCompose.Channel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bcu.foodtable.FoodTableApplication
import com.bcu.foodtable.di.ChannelRepository
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bcu.foodtable.ui.home.HomeTopBar
import com.bcu.foodtable.ui.home.AppBottomNavigationBar
import com.bcu.foodtable.ui.home.Screen
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import com.bcu.foodtable.ui.ChallengeActivity
import com.bcu.foodtable.JetpackCompose.AI.AiMainActivity
import com.bcu.foodtable.JetpackCompose.RecipeStorage.RecipeStorageActivity
import com.bcu.foodtable.JetpackCompose.Mypage.ProfileMainScreen

class SubscribeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = SubscribeViewModel(FirebaseFirestore.getInstance(), getCurrentUserId())

        setContent {
            FoodTableTheme {
                SubscribeScreenWithNavigation(
                    subscribeViewModel = viewModel
                )
            }
        }
    }

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "defaultUserId"
    }
}

@Composable
fun SubscribeScreenWithNavigation(
    subscribeViewModel: SubscribeViewModel,
    homeViewModel: HomeViewModel = viewModel()
) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Home, Screen.Subscribe, Screen.AIService, Screen.RecipeStorage, Screen.MyPage
    )
    var selectedTab by remember { mutableStateOf(1) }
    val user by homeViewModel.user.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.loadUserInfo()
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                user = user,
                onProfileClick = {
                    navController.navigate(Screen.MyPage.route)
                    selectedTab = screens.indexOf(Screen.MyPage)
                },
                onChallengeClick = {
                    navController.context.startActivity(Intent(navController.context, ChallengeActivity::class.java))
                }
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                screens = screens,
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    navController.navigate(screens[index].route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Subscribe.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Subscribe.route) {
                SubscribeScreen(
                    viewModel = subscribeViewModel,
                    navController = navController,
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(Screen.MyPage.route) {
                ProfileMainScreen(paddingValues = paddingValues)
            }
            composable(Screen.AIService.route) {
                LaunchedEffect(Unit) {
                    navController.context.startActivity(Intent(navController.context, AiMainActivity::class.java))
                }
            }
            composable(Screen.RecipeStorage.route) {
                LaunchedEffect(Unit) {
                    navController.context.startActivity(Intent(navController.context, RecipeStorageActivity::class.java))
                }
            }
            composable(Screen.Home.route) {
                LaunchedEffect(Unit) {
                    (navController.context as? ComponentActivity)?.finish() // Return to home
                }
            }
        }
    }
}