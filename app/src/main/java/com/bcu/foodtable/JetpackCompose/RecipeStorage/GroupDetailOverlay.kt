package com.bcu.foodtable.JetpackCompose.RecipeStorage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog // 또는 Popup을 사용하여 전체 화면에 가깝게 구현 가능
import com.bcu.foodtable.useful.GalleryItem

@Composable
fun GroupDetailOverlay(
    groupName: String,
    itemsInGroup: List<GalleryItem>,
    onDismiss: () -> Unit,
    onItemClick: (GalleryItem) -> Unit,
    gridItemSize: Dp // 메인 화면의 아이템 크기와 동일하게 또는 다르게 설정 가능
) {
    // Dialog를 사용한 기본적인 오버레이. 좀 더 복잡한 UI는 Popup이나 다른 방식으로 구현 가능
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // 화면 너비의 90%
                .fillMaxHeight(0.8f), // 화면 높이의 80%
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (itemsInGroup.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("이 그룹에는 아이템이 없습니다.")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2), // 내부 그리드 열 개수
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(itemsInGroup, key = { it.recipeId }) { item ->
                            // 상세 보기 내부 아이템 표시는 RecipeItemCard를 재사용하거나
                            // 별도의 Composable을 만들 수 있습니다. 여기서는 RecipeItemCard 재사용.
                            RecipeItemCard( // RecipeItemCard가 클릭 리스너를 직접 받지 않으므로 Box로 감싸서 추가
                                item = item,
                                itemSize = gridItemSize,
                                modifier = Modifier.clickable { onItemClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}