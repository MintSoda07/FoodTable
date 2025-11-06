@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.bcu.foodtable.JetpackCompose.Mypage.Setting.MyRecipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun MyRecipesScreen(
    viewModel: MyRecipesViewModel,
    onOpenRecipe: (String) -> Unit,
    onBack: () -> Unit,
    titleText: String = "내 레시피"
) {
    val cs = MaterialTheme.colorScheme
    val loading by viewModel.loading.collectAsState()
    val base by viewModel.items.collectAsState()

    var query by remember { mutableStateOf("") }
    val shown = remember(base, query) { base.filter { it.matchesQuery(query) } }

    Scaffold(
        containerColor = cs.primaryContainer, // 상단 톤 동일
        topBar = {
            TopAppBar(
                title = { Text(titleText, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    // 설정 화면과 동일한 “원형 배경 + 아이콘” 패턴
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cs.onPrimaryContainer.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = cs.onPrimaryContainer)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchMyChannelRecipes() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "새로고침", tint = cs.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.primaryContainer,
                    titleContentColor = cs.onPrimaryContainer,
                    navigationIconContentColor = cs.onPrimaryContainer,
                    actionIconContentColor = cs.onPrimaryContainer
                )
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .background(
                    // 설정 화면과 동일한 “상단 톤 분리 → 본문 그라데이션/표면” 느낌
                    cs.surface
                )
        ) {
            // 검색창 (제목/카테고리/채널 이름)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("제목/카테고리/채널 검색") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )

            Box(Modifier.fillMaxSize()) {
                when {
                    loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    shown.isEmpty() -> Text(
                        "해당 조건의 레시피가 없습니다.",
                        color = cs.onBackground.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(shown, key = { it.id }) { r ->
                                RecipeRowCard(data = r, onClick = { onOpenRecipe(r.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun RecipeSummary.matchesQuery(q: String): Boolean {
    if (q.isBlank()) return true
    val needle = q.trim().lowercase()
    return title.lowercase().contains(needle) ||
            (categories.any { it.lowercase().contains(needle) }) ||
            ((containedChannel ?: "").lowercase().contains(needle))
}

@Composable
private fun RecipeRowCard(
    data: RecipeSummary,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = data.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Filled.Image),
                error = rememberVectorPainter(Icons.Filled.Image),
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant)
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    data.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )

                Spacer(Modifier.height(4.dp))

                val meta = buildList {
                    data.containedChannel?.let { add(it) }
                    data.estimatedCaloriesText?.let { add(it) }
                    data.durationMin?.let { add("${it}분") }
                    data.dateText?.let { add(it) }
                }.joinToString(" · ")

                if (meta.isNotBlank()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }

                if (data.categories.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        data.categories.take(3).forEach { c -> TagPill(c) }
                        val more = data.categories.size - 3
                        if (more > 0) TagPill("+$more")
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(cs.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = cs.onPrimary
                )
            }
        }
    }
}

@Composable
private fun TagPill(text: String) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = cs.surfaceVariant,
        contentColor = cs.onSurfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
