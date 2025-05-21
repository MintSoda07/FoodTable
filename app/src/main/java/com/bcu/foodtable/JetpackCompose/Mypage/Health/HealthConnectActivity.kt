package com.bcu.foodtable.JetpackCompose.Mypage.Health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HealthConnectActivity : ComponentActivity() {

    private val viewModel: HealthConnectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthConnectScreen(viewModel = viewModel)
        }
    }
}
