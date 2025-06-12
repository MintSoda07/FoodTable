package com.bcu.foodtable.JetpackCompose.Mypage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.compose.rememberNavController

class ProfileActivity : ComponentActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController() //  NavController 생성

            ProfileMainScreen(
                paddingValues = PaddingValues(),
                viewModel = viewModel,
                navController = navController //  NavController 전달
            )
        }
    }
}
