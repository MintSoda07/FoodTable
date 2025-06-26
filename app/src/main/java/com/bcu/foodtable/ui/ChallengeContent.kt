package com.bcu.foodtable.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.model.Challenge
import com.bcu.foodtable.model.ChallengeType


@Composable
fun ChallengeScreenContent(
    challenges: List<Challenge>,
    salt: Int,
    onProgressUpdate: (String, Int) -> Unit,
    onStartChallenge: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    val tabTitles = listOf("🌞 일일 챌린지", "📅 주간 챌린지")
    // 소금 텍스트 색상을 검은색으로 변경하여 가독성 확보
    val saltColor = Color.Black

    val saltScale by animateFloatAsState(
        targetValue = if (salt >= 500) 1.2f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "SaltScale"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFEF9A9A), Color(0xFFFFCC80))
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "밥상친구 챌린지",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Text(
                    text = "현재 보유 소금: ${salt}g",
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 16.dp, bottom = 8.dp)
                        .scale(saltScale),
                    color = saltColor, // 변경된 색상 적용
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            val filtered = when (selectedTab) {
                0 -> challenges.filter { it.type == ChallengeType.DAILY }
                else -> challenges.filter { it.type == ChallengeType.WEEKLY }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { challenge -> challenge.id }) { challenge ->
                    ChallengeCardAnimated(
                        challenge = challenge,
                        snackbarHostState = snackbarHostState,
                        onProgressUpdate = { onProgressUpdate(challenge.id, it) },
                        onStartChallenge = { onStartChallenge(challenge.id) }
                    )
                }
            }
        }
    }
}