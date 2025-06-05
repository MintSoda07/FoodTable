// 파일: RecipeCard.kt
package com.bcu.foodtable.JetpackCompose.Subscribe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.RecipeItem

/**
 * @param recipe: 보여줄 레시피 데이터
 * @param isPurchased: 이미 구매된 항목인지 여부 (true면 구매됨, false면 미구매).
 *                     실제 로직에 따라 호출부에서 사용자의 구매 기록을 확인해 전달해주세요.
 * @param onClick: 레시피 카드를 눌렀을 때 실행할 콜백
 */
@Composable
fun RecipeCard(
    recipe: RecipeItem,
    isPurchased: Boolean = false,
    onClick: () -> Unit
) {
    // priceInSalt가 0이거나 파이어스토어에서 잘못 매핑된 경우를 대비해
    // cost 필드도 함께 확인합니다.
    val actualCost = when {
        recipe.priceInSalt > 0 -> recipe.priceInSalt
        recipe.cost > 0        -> recipe.cost
        else                    -> 0
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(200.dp)  // 카드 높이를 고정해서 이미지가 꽉 차도록
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1) 배경 이미지가 카드 전체를 채우도록
            AsyncImage(
                model = recipe.imageResId,
                contentDescription = "${recipe.name} 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )

            // 2) 이미지 아래쪽에 반투명 그라데이션 박스를 깔고 텍스트를 겹침
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // 3) 하단 오버레이 영역: 레시피 이름, 비용/시간, 조회수/좋아요
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.BottomStart)
            ) {
                // 레시피 제목
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 비용: actualCost == 0이면 “무료”, >0이면 “소금 XX”
                    Text(
                        text = if (actualCost > 0) "${actualCost}소금" else "무료",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = when {
                            isPurchased -> MaterialTheme.colorScheme.primaryContainer
                            actualCost > 0 -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    )

                    // 소요 시간(duration) 분 단위 (0 이면 생략)
                    if (recipe.duration > 0) {
                        Text(
                            text = "⏱ ${recipe.duration}분",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 조회수
                    Text(
                        text = "👁 ${recipe.clicked}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 좋아요
                    Text(
                        text = "❤️ ${recipe.likes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4) 우측 상단 뱃지
            when {
                isPurchased -> {
                    // 구매된 상태: 녹색 계열 뱃지
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "구매됨",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                actualCost > 0 -> {
                    // 유료 레시피: 빨간 계열 뱃지에 가격 표시
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "${actualCost}소금",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
                else -> {
                    // 무료 레시피: 파란 계열 뱃지
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "무료",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}
