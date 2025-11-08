package com.bcu.foodtable.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.model.Challenge
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(
    challenge: Challenge,
    onShareClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val dateFormatter = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    val start = if (challenge.startDate > 0) dateFormatter.format(Date(challenge.startDate)) else "-"
    val end = if (challenge.endDate > 0) dateFormatter.format(Date(challenge.endDate)) else "-"

    val progressTarget = challenge.targetValue.coerceAtLeast(1)
    val progress = (challenge.progress.toFloat() / progressTarget).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(progress, tween(450), label = "detailProgress")

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "챌린지 상세",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 타이틀/설명
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        challenge.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        challenge.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onPrimaryContainer
                    )
                }
            }

            // 진행도 블록
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("진행도", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${challenge.progress} / ${challenge.targetValue} (${(animatedProgress * 100).toInt()}%)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = colorScheme.primary,
                        trackColor = colorScheme.surfaceColorAtElevation(4.dp)
                    )
                }
            }

            // 정보
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("🎯 목표 횟수", "${challenge.targetValue}회", colorScheme)
                    InfoRow("📅 기간", "$start ~ $end", colorScheme)
                    InfoRow("✅ 상태", if (challenge.isCompleted) "완료됨" else "진행 중", colorScheme)
                    InfoRow("🎁 보상", "${challenge.reward} 소금", colorScheme)
                }
            }

            Spacer(Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { /* TODO: 다시 도전 */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                ) {
                    Text("🔄 다시 도전하기", color = colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onShareClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.primary)
                ) {
                    Text("📤 공유하기", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, colorScheme: ColorScheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = colorScheme.onSecondaryContainer)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSecondaryContainer)
    }
}
