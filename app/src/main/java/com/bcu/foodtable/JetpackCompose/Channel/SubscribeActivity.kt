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
import com.bcu.foodtable.FoodTableApplication
import com.bcu.foodtable.di.ChannelRepository
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
// Import navigation components from HomeScreen
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
                    subscribeViewModel = viewModel,
                    context = this
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
    context: ComponentActivity,
    homeViewModel: HomeViewModel = viewModel()
) {
    // Navigation state
    var selectedTab by remember { mutableStateOf(1) } // Channel tab index
    val user by homeViewModel.user.collectAsState()
    
    val screens = listOf(
        Screen.Home, Screen.Subscribe, Screen.AIService, Screen.RecipeStorage, Screen.MyPage
    )

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
                        0 -> { // Home
                            context.finish()
                        }
                        1 -> { // Channel - stay here
                            selectedTab = newTab
                        }
                        2 -> { // AI Service
                            context.startActivity(Intent(context, AiMainActivity::class.java))
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
            else -> { // Channel content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    SubscribeScreen(
                        viewModel = subscribeViewModel,
                        context = context
                    )
                }
            }
        }
    }
}
