package com.bcu.foodtable.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.R
import com.bcu.foodtable.model.Challenge
import com.bcu.foodtable.model.ChallengeType
import com.bcu.foodtable.viewmodel.ChallengeViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun ChallengeScreen(viewModel: ChallengeViewModel) {
    val challenges by viewModel.challenges.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val userSalt by viewModel.userSalt.collectAsState()

    when {
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("⚠ ${error ?: "오류 발생"}", color = MaterialTheme.colorScheme.error)
        }
        else -> ChallengeScreenContent(
            challenges = challenges,
            salt = userSalt,
            onProgressUpdate = { id, value -> viewModel.updateProgress(id, value) },
            onStartChallenge = { id -> viewModel.startChallenge(id) }
        )
    }
}

@Composable
fun ChallengeCardAnimated(
    challenge: Challenge,
    snackbarHostState: SnackbarHostState,
    onProgressUpdate: (Int) -> Unit,
    onStartChallenge: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition()
    val animatedRewardScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val json = Json.encodeToString(challenge)
                val intent = Intent(context, ChallengeDetailActivity::class.java)
                intent.putExtra("challenge", json)
                context.startActivity(intent)
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6D4C41)
                )

                if (challenge.isCompleted) {
                    Spacer(modifier = Modifier.width(8.dp))

                    val scale = remember { Animatable(1f) }

                    Image(
                        painter = painterResource(id = R.drawable.stamp_done),
                        contentDescription = "완료 도장",
                        modifier = Modifier
                            .size(36.dp)
                            .scale(scale.value)
                            .clickable {
                                scope.launch {
                                    scale.animateTo(1.4f, animationSpec = tween(100))
                                    scale.animateTo(1f, animationSpec = tween(100))

                                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                    vibrator.vibrate(
                                        android.os.VibrationEffect.createOneShot(
                                            80,
                                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                        )
                                    )

                                    snackbarHostState.showSnackbar("도장 완료! 수고하셨어요 🎉")
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = challenge.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = if (challenge.targetValue > 0)
                    challenge.progress.toFloat() / challenge.targetValue else 0f,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFC62828),
                trackColor = Color(0xFFFFCDD2)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${challenge.progress}/${challenge.targetValue}")
                Text(
                    text = "🎁 보상: ${challenge.reward} 소금",
                    color = Color(0xFF8D6E63),
                    modifier = Modifier.scale(animatedRewardScale)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            when {
                !challenge.isCompleted && challenge.progress >= challenge.targetValue -> {
                    Button(
                        onClick = { onProgressUpdate(challenge.targetValue) },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315))
                    ) {
                        Text("완료하기", color = Color.White)
                    }
                }
                challenge.progress == 0 -> {
                    Button(
                        onClick = { onStartChallenge() },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D4C41))
                    ) {
                        Text("도전하기", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeScreenPreview() {
    val dummyChallenges = listOf(
        Challenge(
            id = "1",
            title = "🍎 아침 과일 먹기",
            description = "하루를 상큼하게 시작해보세요!",
            targetValue = 3,
            progress = 1,
            reward = 100,
            type = ChallengeType.DAILY,
            isCompleted = false
        ),
        Challenge(
            id = "2",
            title = "🚶‍♂️ 30분 걷기",
            description = "가볍게 산책하며 건강을 챙기세요.",
            targetValue = 1,
            progress = 1,
            reward = 150,
            type = ChallengeType.WEEKLY,
            isCompleted = true
        )
    )

    ChallengeScreenContent(
        challenges = dummyChallenges,
        salt = 250,
        onProgressUpdate = { _, _ -> },
        onStartChallenge = {}
    )
}

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
    val saltColor = when {
        salt >= 500 -> Color(0xFFFFD700)
        salt >= 100 -> Color(0xFFFFF176)
        else -> Color.White
    }
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
                        text = "🍽 밥상친구 챌린지",
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
                    color = saltColor,
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
