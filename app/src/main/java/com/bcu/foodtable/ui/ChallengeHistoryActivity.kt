package com.bcu.foodtable.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.model.Challenge
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ChallengeHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 테스트용 더미 데이터 (실제 앱에서는 Firestore에서 불러올 수 있음)
        val completedChallengesJson = intent.getStringExtra("completedChallenges") ?: "[]"
        val completedChallenges = Json.decodeFromString<List<Challenge>>(completedChallengesJson)

        setContent {
            ChallengeHistoryScreen(completedChallenges)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeHistoryScreen(challenges: List<Challenge>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📜 완료한 챌린지 기록") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6D4C41),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (challenges.isEmpty()) {
                item {
                    Text("아직 완료한 챌린지가 없습니다.", color = Color.Gray)
                }
            } else {
                items(challenges, key = { challenge -> challenge.id }) { challenge ->
                    CompletedChallengeCard(challenge)
                }
            }
        }
    }
}

@Composable
fun CompletedChallengeCard(challenge: Challenge) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "✅ ${challenge.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF004D40)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("보상: ${challenge.reward} 소금")
            Text("진행도: ${challenge.progress} / ${challenge.targetValue}")
        }
    }
}
