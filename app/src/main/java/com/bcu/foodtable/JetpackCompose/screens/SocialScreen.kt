package com.bcu.foodtable.JetpackCompose.screens

import androidx.annotation.RawRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen() {
    val tabs = listOf("커뮤니티", "랭킹", "친구", "내 채팅")
    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val gradientBrush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.background
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 검색창
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("검색") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // 탭
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // 로딩 시 Lottie
            AnimatedVisibility(visible = isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LottieAnimationView(
                        resId = R.raw.loading,
                        modifier = Modifier.size(200.dp)
                    )
                }
            }

            if (!isLoading) {
                when (selectedTabIndex) {
                    0 -> CommunityTab(searchText)
                    1 -> RankingTab(searchText)
                    2 -> FriendsTab(searchText)
                    3 -> ChatTab(searchText)
                }
            }
        }
    }
}

@Composable
fun CommunityTab(searchText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("커뮤니티 게시판 (검색: $searchText)", textAlign = TextAlign.Center)
    }
}

@Composable
fun RankingTab(searchText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("랭킹 리스트 (검색: $searchText)", textAlign = TextAlign.Center)
    }
}

@Composable
fun FriendsTab(searchText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("내 친구 목록 (검색: $searchText)", textAlign = TextAlign.Center)
    }
}

@Composable
fun ChatTab(searchText: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("채팅 목록 (검색: $searchText)", textAlign = TextAlign.Center)
    }
}
