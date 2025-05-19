package com.bcu.foodtable.JetpackCompose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagRow(
    selectedTags: List<String>,
    onTagRemove: (String) -> Unit
) {
    val primaryColor = Color(0xFFE76F51)

    if (selectedTags.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "선택된 태그",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${selectedTags.size}개",
                    style = MaterialTheme.typography.labelMedium,
                    color = primaryColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                selectedTags.forEach { tag ->
                    ElevatedAssistChip(
                        onClick = { onTagRemove(tag) },
                        label = { 
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            ) 
                        },
                        enabled = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "태그 삭제",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.elevatedAssistChipColors(
                            containerColor = primaryColor.copy(alpha = 0.1f),
                            labelColor = primaryColor,
                            trailingIconContentColor = primaryColor
                        ),
                        elevation = AssistChipDefaults.elevatedAssistChipElevation(
                            elevation = 2.dp
                        )
                    )
                }
            }
        }
    }
}
