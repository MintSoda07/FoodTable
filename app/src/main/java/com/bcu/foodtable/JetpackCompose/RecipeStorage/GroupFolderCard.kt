package com.bcu.foodtable.JetpackCompose.RecipeStorage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bcu.foodtable.R // 실제 placeholder 리소스 ID로 변경 필요
import com.bcu.foodtable.useful.GalleryItem

@Composable
fun GroupFolderItemCard(
    groupName: String,
    itemsInGroup: List<GalleryItem>, // 그룹 내 실제 아이템들 (미리보기용)
    itemSize: Dp,
    modifier: Modifier = Modifier
) {
    val previewItems = itemsInGroup.take(4)

    Column(
        modifier = modifier.width(itemSize), // 아이템 크기를 인자로 받음
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .width(itemSize)
                .height(itemSize),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFC8C8C8) // XML의 #C8C8C8
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (previewItems.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        MiniPreviewImage(item = previewItems.getOrNull(0), modifier = Modifier.weight(1f))
                        MiniPreviewImage(item = previewItems.getOrNull(1), modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        MiniPreviewImage(item = previewItems.getOrNull(2), modifier = Modifier.weight(1f))
                        MiniPreviewImage(item = previewItems.getOrNull(3), modifier = Modifier.weight(1f))
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("비어있음", fontSize = 10.sp, color = Color.DarkGray)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = groupName,
            textAlign = TextAlign.Center,
            fontSize = 14.sp, // XML과 유사하게 조정
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall, // 테마 폰트 사용
            modifier = Modifier.padding(horizontal = 4.dp) // 텍스트 좌우 약간의 패딩
        )
    }
}

@Composable
private fun MiniPreviewImage(item: GalleryItem?, modifier: Modifier = Modifier) {
    if (item != null && item.image?.isNotBlank() == true){
        Card(
            modifier = modifier
                .fillMaxSize()
                .padding(6.dp),
            shape = RoundedCornerShape(15.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.image)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_dialog_alert)  // R.drawable.dish_icon 등 실제 리소스 사용
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Spacer(modifier = modifier.fillMaxSize().padding(6.dp)) // 아이템이 없거나 이미지가 없으면 빈칸
    }
}