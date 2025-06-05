//package com.bcu.foodtable.JetpackCompose.HomeChannelDatil
//
//import android.content.Intent
//import android.net.Uri
//import android.widget.Toast
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items // itemsIndexed 대신 사용 가능
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.Card
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import coil.compose.AsyncImage
//import com.bcu.foodtable.useful.RecipeItem
//@Composable
//fun RecipeDetailScreen(recipe: RecipeItem, onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
//    // Log 1: RecipeDetailScreen 함수 자체가 호출되는지 확인
//    android.util.Log.d("RecipeDetailScreen_Debug", "Log 1: RecipeDetailScreen 함수 시작. 레시피명: ${recipe.name}")
//    val context = LocalContext.current
//
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        // Log 2: LazyColumn 블록이 시작되는지 확인
//        android.util.Log.d("RecipeDetailScreen_Debug", "Log 2: LazyColumn 블록 시작.")
//
//        // --- 첫 번째 아이템: 레시피 기본 정보 (이름부터 비고까지) ---
//        item {
//            // Log 3: 첫 번째 item 블록이 호출되는지 확인
//            android.util.Log.d("RecipeDetailScreen_Debug", "Log 3: 첫 번째 item 블록 시작.")
//            Text(
//                text = recipe.name,
//                style = MaterialTheme.typography.headlineMedium,
//                modifier = Modifier.padding(bottom = 8.dp)
//            )
//            AsyncImage(
//                model = recipe.imageResId,
//                contentDescription = "레시피 이미지",
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(200.dp)
//                    .clip(RoundedCornerShape(12.dp))
//            )
//            Spacer(modifier = Modifier.height(12.dp))
//            Text("설명: ${recipe.description}")
//            Text("예상 칼로리: ${recipe.estimatedCalories ?: "알 수 없음"}")
//            Spacer(modifier = Modifier.height(12.dp))
//            Text("카테고리: ${recipe.C_categories.joinToString()}")
//            Text("태그: ${recipe.tags.joinToString()}")
//            Spacer(modifier = Modifier.height(12.dp))
//            Text("비고: ${recipe.note}")
//            Spacer(modifier = Modifier.height(12.dp))
//            android.util.Log.d("RecipeDetailScreen_Debug", "Log 3: 첫 번째 item 블록 끝.")
//        } // --- 첫 번째 아이템 끝 ---
//
//        // --- 여기가 "깡통 테스트" 코드가 적용된 재료 item 블록입니다 ---
//        item {
//            // Log 4: 재료 테스트 item 블록이 호출되는지 확인
//            android.util.Log.d("RecipeDetailScreen_Debug", "Log 4: 재료 테스트 item 블록 시작")
//
//            Text(
//                text = "--- 재료 섹션 테스트 시작 ---",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier.padding(bottom = 4.dp)
//            )
//            val ingredientsString = recipe.ingredients.joinToString(", ")
//            // Log 5: 재료 데이터 문자열 로그
//            android.util.Log.d("RecipeDetailScreen_Debug", "Log 5: 실제 재료 데이터 문자열: $ingredientsString")
//            Text("테스트용 재료 데이터: $ingredientsString")
//
//            Text("--- 재료 섹션 테스트 끝 ---")
//            Spacer(modifier = Modifier.height(12.dp))
//            // Log 6: 재료 테스트 item 블록 끝 로그
//            android.util.Log.d("RecipeDetailScreen_Debug", "Log 6: 재료 테스트 item 블록 끝")
//        } // --- 두 번째 아이템 끝 ---
//
//        // --- 세 번째 아이템: "조리 순서" 제목 ---
//        item {
//            android.util.Log.d("RecipeDetailScreen_Debug", "Log 7: '조리 순서' 제목 item 블록 시작.")
//            Text(
//                text = "조리 순서",
//                style = MaterialTheme.typography.titleMedium
//            )
//            android.util.Log.d("RecipeDetailScreen_Debug", "Log 7: '조리 순서' 제목 item 블록 끝.")
//        } // --- 세 번째 아이템 끝 ---
//
//        // --- 조리 단계 목록 ---
//        itemsIndexed(recipe.order.split("○").filter { it.isNotBlank() }) { index, step ->
//            android.util.Log.d("RecipeDetailScreen_Debug", "Log 8: 조리 단계 itemsIndexed - 인덱스 $index")
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 4.dp)
//            ) {
//                Column(modifier = Modifier.padding(12.dp)) {
//                    Text(text = "○${index + 1}. $step")
//                }
//            }
//        }
//
//        // --- 수정/삭제 버튼 ---
//        item {
//            android.util.Log.d("RecipeDetailScreen_Debug", "Log 9: 수정/삭제 버튼 item 블록 시작.")
//            Spacer(modifier = Modifier.height(20.dp))
//            Row( /* ... */ ) { /* ... */ }
//            android.util.Log.d("RecipeDetailScreen_Debug", "Log 9: 수정/삭제 버튼 item 블록 끝.")
//        }
//    }
//    android.util.Log.d("RecipeDetailScreen_Debug", "Log 10: RecipeDetailScreen 함수 끝.")
//}