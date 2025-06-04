package com.bcu.foodtable.model

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.bcu.foodtable.ui.ChallengeDetailScreen
import com.bcu.foodtable.ui.home.FoodTableTheme
import kotlinx.serialization.json.Json

class ChallengeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val challengeJson = intent.getStringExtra("challenge") ?: ""
        val challenge = Json.decodeFromString<Challenge>(challengeJson)

        setContent {
            FoodTableTheme {
                ChallengeDetailScreen(
                    challenge = challenge,
                    onShareClick = { shareChallenge(challenge) } // ✅ 이 함수 아래에 정의돼 있어야 함
                )
            }
        }
    }

    private fun shareChallenge(challenge: Challenge) {
        val shareText = """
            [🍽 밥상친구 챌린지]
            
            ${challenge.title}
            ${challenge.description}
            
            🎯 목표: ${challenge.targetValue}회
            🎁 보상: ${challenge.reward} 소금
            #밥상친구 #챌린지
        """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, "챌린지 공유하기"))
    }
}