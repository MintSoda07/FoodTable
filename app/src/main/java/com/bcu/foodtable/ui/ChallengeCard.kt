package com.bcu.foodtable.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcu.foodtable.R
import com.bcu.foodtable.model.Challenge
import com.bcu.foodtable.model.ChallengeDetailActivity
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
    val isRewardClaimable = !challenge.isCompleted && challenge.progress >= challenge.targetValue
    val isStarted = challenge.progress > 0

    val progressTarget = challenge.targetValue.coerceAtLeast(1)
    val targetProgress = (challenge.progress.toFloat() / progressTarget).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(450),
        label = "progressAnimation"
    )

    val infinite = rememberInfiniteTransition(label = "rewardTransition")
    val rewardScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (isRewardClaimable) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rewardScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = {
            // 상세 화면 이동 (기존 기능 유지)
            val intent = Intent(context, com.bcu.foodtable.model.ChallengeDetailActivity::class.java)
            intent.putExtra("challenge", Json.encodeToString(challenge))
            context.startActivity(intent)
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 제목 / 완료 아이콘
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                if (challenge.isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.MilitaryTech,
                        contentDescription = "완료됨",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(Modifier.height(12.dp))

            // 진행도
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "진행도",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${challenge.progress} / ${challenge.targetValue} (${(animatedProgress * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            )

            Spacer(Modifier.height(12.dp))

            // 보상 & 액션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("보상 ${challenge.reward} 소금", fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.scale(rewardScale),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isRewardClaimable)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (isRewardClaimable)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        leadingIconContentColor = if (isRewardClaimable)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                when {
                    isRewardClaimable -> {
                        ChallengeButton(
                            text = "보상받기",
                            icon = Icons.Default.CheckCircle,
                            onClick = { onProgressUpdate(challenge.targetValue) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            )
                        )
                    }
                    challenge.isCompleted -> {
                        ChallengeButton(
                            text = "완료됨",
                            icon = Icons.Default.CheckCircle,
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        )
                    }
                    isStarted -> {
                        ChallengeButton(
                            text = "진행중",
                            icon = Icons.Default.RocketLaunch,
                            onClick = {},
                            enabled = false
                        )
                    }
                    else -> {
                        ChallengeButton(
                            text = "도전하기",
                            icon = Icons.Default.RocketLaunch,
                            onClick = onStartChallenge
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ChallengeButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}