package com.bcu.foodtable.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
            CenterAlignedTopAppBar(
                title = { Text("📜 완료한 챌린지 기록", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        if (challenges.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("아직 완료한 챌린지가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(challenges, key = { it.id }) { challenge ->
                    CompletedChallengeCard(challenge)
                }
            }
        }
    }
}

@Composable
fun CompletedChallengeCard(challenge: Challenge) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✅ ${challenge.title}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                AssistChip(
                    onClick = {},
                    label = { Text("완료") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "보상 ${challenge.reward} 소금",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "진행도 ${challenge.progress} / ${challenge.targetValue}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}