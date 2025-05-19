package com.bcu.foodtable.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.model.Challenge
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

class ChallengeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val challengeJson = intent.getStringExtra("challenge") ?: ""
        val challenge = Json.decodeFromString<Challenge>(challengeJson)

        setContent {
            ChallengeDetailScreen(
                challenge = challenge,
                onShareClick = { shareChallenge(challenge) }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(
    challenge: Challenge,
    onShareClick: () -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    }
    val start = if (challenge.startDate > 0) dateFormatter.format(Date(challenge.startDate)) else "-"
    val end = if (challenge.endDate > 0) dateFormatter.format(Date(challenge.endDate)) else "-"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "🍽 챌린지 상세",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF8D6E63)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFFFF8E1))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFCC80), Color(0xFFFFE0B2))))
                    .padding(24.dp)
            ) {
                Column {
                    Text(challenge.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(challenge.description, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("🎯 목표 횟수", "${challenge.targetValue}회")
                    InfoRow("📅 기간", "$start ~ $end")
                    InfoRow("📈 진행도", "${challenge.progress} / ${challenge.targetValue}")
                    InfoRow("✅ 상태", if (challenge.isCompleted) "완료됨" else "진행 중")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "🎁 보상: ${challenge.reward} 소금",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(Color(0xFFFFEB3B), shape = MaterialTheme.shapes.medium)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* 다시 도전 */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D4C41))
                ) {
                    Text("🔄 다시 도전하기", color = Color.White)
                }

                OutlinedButton(
                    onClick = { onShareClick() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📤 공유하기")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
