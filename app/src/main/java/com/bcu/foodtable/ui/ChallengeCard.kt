package com.bcu.foodtable.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.R
import com.bcu.foodtable.model.Challenge
import com.bcu.foodtable.model.ChallengeDetailActivity
import com.bcu.foodtable.ui.ChallengeDetailScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
                val intent = Intent(context, ChallengeDetailActivity::class.java) // ✅ Activity로 수정
                intent.putExtra("challenge", Json.encodeToString(challenge))
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