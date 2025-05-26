package com.bcu.foodtable.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.bcu.foodtable.viewmodel.ChallengeViewModel

class ChallengeActivity : ComponentActivity() {

    private val challengeViewModel: ChallengeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ChallengeScreen(viewModel = challengeViewModel)
            }
        }
    }
}
