//package com.bcu.foodtable.JetpackCompose.RecipeStorage
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import coil.compose.AsyncImage
//import coil.request.ImageRequest
//import com.bcu.foodtable.R // 실제 placeholder 리소스 ID로 변경 필요
//import com.bcu.foodtable.useful.GalleryItem
//
//@Composable
//fun RecipeItemCard(
//    item: GalleryItem,
//    itemSize: Dp,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier.width(itemSize),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Card(
//            modifier = Modifier
//                .width(itemSize)
//                .height(itemSize), // 이미지 영역을 정사각형으로 가정
//            shape = RoundedCornerShape(16.dp), // 적절한 코너 값
//            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//        ) {
//            AsyncImage(
//                model = ImageRequest.Builder(LocalContext.current)
//                    .data(item.image)
//                    .placeholder(android.R.drawable.ic_menu_gallery)
//                    .error(android.R.drawable.ic_dialog_alert) // 여기에 괄호가 추가되었습니다!
//                    .crossfade(true)
//                    .build(),
//                contentDescription = item.name,
//                contentScale = ContentScale.Crop,
//                modifier = Modifier.fillMaxSize()
//            )
//        }
//        Spacer(modifier = Modifier.height(6.dp))
//        Text(
//            text = item.name,
//            textAlign = TextAlign.Center,
//            fontSize = 14.sp,
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis,
//            style = MaterialTheme.typography.titleSmall,
//            modifier = Modifier.padding(horizontal = 4.dp)
//        )
//    }
//}