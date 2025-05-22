package com.bcu.foodtable.JetpackCompose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionSection(
    categoryItems: List<String>,
    selectedItems: List<String>,
    onItemToggle: (String) -> Unit,
    selectedCategory: String
) {
    val primaryColor = Color(0xFFE76F51)

    // 카드 애니메이션
    var isVisible by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300)
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                alpha = cardAlpha
            }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCategory,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = primaryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "${selectedItems.size}개 선택됨",
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                categoryItems
                    .distinct() // 중복 제거
                    .filter { it.isNotBlank() } // 빈 문자열 제거
                    .forEachIndexed { index, item ->

                        key(item + index) { // 고유한 key 부여
                            val isSelected = selectedItems.contains(item)
                            var isPressed by remember { mutableStateOf(false) }

                            val chipScale by animateFloatAsState(
                                targetValue = if (isPressed) 0.95f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isPressed = true
                                    try {
                                        Log.d("CategorySelection", "Click: $item at $index")
                                        onItemToggle(item)
                                    } catch (e: Exception) {
                                        Log.e("CategorySelection", "Error toggling item: $item", e)
                                    }
                                },
                                label = {
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                enabled = true,
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) primaryColor else Color.Transparent
                                ),
                                modifier = Modifier.scale(chipScale)
                            )

                            LaunchedEffect(isPressed) {
                                if (isPressed) {
                                    delay(100)
                                    isPressed = false
                                }
                            }
                        }
                    }
            }
        }
    }
}
