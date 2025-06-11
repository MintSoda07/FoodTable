package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box // Box 추가
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush // 그라디언트를 위해 추가
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.Channel

@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.96f else 1.0f

    Card(
        modifier = modifier
            .padding(8.dp)
            .width(160.dp)
            .height(180.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp), // 모서리 둥글기 더 크게
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), // 그림자 더 강조
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant // 테마의 surfaceVariant 색상 사용
        )
    ) {
        Column {
            Box( // 이미지와 그라디언트 오버레이를 감싸는 Box
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) // 카드 상단 모서리와 일치
            ) {
                // 상단 이미지 영역
                AsyncImage(
                    model = channel.imageResId,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize(), // Box를 가득 채우도록
                    contentScale = ContentScale.Crop
                )

                // 이미지 위에 그라디언트 오버레이 추가
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), // 거의 투명한 primary
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) // 살짝 더 진한 primary
                                )
                            )
                        )
                )
            }

            // 하단 텍스트 정보
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = channel.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${channel.subscribers}명 구독중",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) // 구독자 수 텍스트를 더 부드럽게
                )
            }
        }
    }
}