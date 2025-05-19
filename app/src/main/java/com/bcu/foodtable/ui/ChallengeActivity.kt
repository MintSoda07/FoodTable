package com.bcu.foodtable.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.bcu.foodtable.repository.ChallengeRepository
import com.bcu.foodtable.viewmodel.ChallengeViewModel



class ChallengeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                ChallengeScreenPreview() // ✔️ 더미 데이터 화면 테스트 OK
            }
        }
    }
}