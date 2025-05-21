package com.bcu.foodtable.JetpackCompose.Mypage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : ComponentActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileMainScreen(
                paddingValues = androidx.compose.foundation.layout.PaddingValues(),
                viewModel = viewModel
            )
        }
    }
}
