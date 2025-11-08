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

@Composable
fun RecipeCard(
    recipe: RecipeItem,
    isPurchased: Boolean = false,
    onClick: () -> Unit
) {
    val actualCost = when {
        recipe.priceInSalt > 0 -> recipe.priceInSalt
        recipe.cost > 0        -> recipe.cost
        else                   -> 0
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 배경 이미지
            AsyncImage(
                model = recipe.imageResId,
                contentDescription = "${recipe.name} 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )

            // 1) 더 진해진(불투명도 높은) 그라데이션
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f) // 이전 0.6에서 0.8로 높임
                            )
                        )
                    )
            )

            // 2) 하단 오버레이: 텍스트
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.BottomStart)
            ) {
                // 레시피 제목
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    ),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (actualCost > 0) "${actualCost}소금" else "무료",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = when {
                                isPurchased    -> Color(0xFF00C853)
                                actualCost > 0 -> Color(0xFFD32F2F)
                                else           -> Color(0xFF2196F3)
                            }
                        )
                    )
                    if (recipe.duration > 0) {
                        Text(
                            text = "⏱ ${recipe.duration}분",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "👁 ${recipe.clicked}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                    )
                    Text(
                        text = "❤️ ${recipe.likes}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                    )
                }
            }

            // 3) 상단 뱃지
            when {
                isPurchased -> {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp),
                        containerColor = Color(0xFF00C853)
                    ) {
                        Text(
                            text = "구매됨",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                        )
                    }
                }
                actualCost > 0 -> {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp),
                        containerColor = Color(0xFFD32F2F)
                    ) {
                        Text(
                            text = "${actualCost}소금",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                        )
                    }
                }
                else -> {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp),
                        containerColor = Color(0xFF2196F3)
                    ) {
                        Text(
                            text = "무료",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                        )
                    }
                }
            }
        }
    }
}
