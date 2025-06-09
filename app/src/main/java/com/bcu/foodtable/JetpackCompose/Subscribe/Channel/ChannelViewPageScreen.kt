// 파일: ChannelViewPageScreen.kt
package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity
import com.bcu.foodtable.JetpackCompose.Subscribe.RecipeCard
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChannelViewPageScreen(
    channelName: String,
    navController: NavHostController,
    viewModel: ChannelViewModel = viewModel()
) {
    val context = LocalContext.current
    val user = remember { UserManager.getUser() }
    val userId = user?.uid ?: ""

    var selectedTab by remember { mutableStateOf("Recipes") }
    val channel by viewModel.channel.collectAsState(initial = null)
    val recipes by viewModel.recipes.collectAsState(initial = emptyList())
    val isSubscribed by viewModel.isSubscribed.collectAsState(initial = null)
    val subscriberCount by viewModel.subscriberCount.collectAsState(initial = 0)
    val isLoading by viewModel.isLoading.collectAsState(initial = false)

    val coroutineScope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(channelName) {
        if (!loaded && channelName.isNotBlank()) {
            viewModel.loadAll(channelName, userId)
            loaded = true
        }
    }
    // 1) launcher 정의
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 수정/삭제 직후 다시 불러오기
            viewModel.loadAll(channelName, userId)
        }
    }

    // 1) 로딩 중 표시
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // 2) 로그인 필요
    if (userId.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("로그인이 필요합니다.")
        }
        return
    }

    // 3) 채널 로딩 실패
    if (channel == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("채널 정보를 불러올 수 없습니다.")
        }
        return
    }

    // ============================================================
    // 4) 실제 UI
    Scaffold(
        topBar = {
            // 유튜브 스타일의 배너 이미지를 상단에 표시
            LargeTopAppBar(
                title = { /* 빈 람다 */ },
                modifier = Modifier
                    .height(200.dp)
                    .background(Color.Transparent),
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                navigationIcon = { /* 빈 람다 */ },
                actions = { /* 빈 람다 */ },
                scrollBehavior = null
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = channel!!.BackgroundResId,
                    contentDescription = "채널 배너",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.5f to Color.Black.copy(alpha = 0.3f),
                                1f to Color.Black.copy(alpha = 0.7f)
                            )
                        )
                )
            }
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 4-2) 프로필 + 구독 영역
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        // **기존 offset(y = (-32).dp) → offset(y = (-27).dp) 로 조정**
                        .offset(y = (-5).dp)
                        .animateContentSize(animationSpec = spring()),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        AsyncImage(
                            model = channel!!.imageResId,
                            contentDescription = "채널 프로필",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = channel!!.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$subscriberCount 명 구독 중",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // 구독 / 구독중 버튼
                        if (userId != channel!!.owner) {
                            when (isSubscribed) {
                                true -> ElevatedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.toggleSubscription(channel!!.name, userId)
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("구독중")
                                }

                                false -> ElevatedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.toggleSubscription(channel!!.name, userId)
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("구독하기")
                                }

                                null -> OutlinedButton(
                                    onClick = { },
                                    enabled = false,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("로딩中", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // 4-3) 작성자 전용 버튼 (FilledTonalButton)
                if (userId == channel!!.owner) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // 레시피 생성 버튼
                        FilledTonalButton(
                            onClick = { navController.navigate("write/${channel!!.name}") },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "레시피 생성",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "레시피 생성",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        // 채널 관리 버튼
                        FilledTonalButton(
                            onClick = { navController.navigate("editChannel/${channel!!.name}") },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "채널 관리",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "채널 관리",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 4-4) 탭바
                TabRow(
                    selectedTabIndex = if (selectedTab == "Recipes") 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier
                                .tabIndicatorOffset(
                                    tabPositions[if (selectedTab == "Recipes") 0 else 1]
                                )
                                .height(3.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(
                        selected = (selectedTab == "Recipes"),
                        onClick = { selectedTab = "Recipes" }
                    ) {
                        Text(
                            "Recipes",
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                            style = if (selectedTab == "Recipes")
                                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            else
                                MaterialTheme.typography.bodyMedium,
                            color = if (selectedTab == "Recipes")
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Tab(
                        selected = (selectedTab == "Liked"),
                        onClick = { selectedTab = "Liked" }
                    ) {
                        Text(
                            "Liked",
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                            style = if (selectedTab == "Liked")
                                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            else
                                MaterialTheme.typography.bodyMedium,
                            color = if (selectedTab == "Liked")
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4-5) 탭별 콘텐츠: 레시피 그리드
                when (selectedTab) {
                    "Recipes" -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = recipes,
                                key = { recipe ->
                                    recipe.id.ifBlank { recipe.name + recipe.hashCode() }
                                }
                            ) { recipe ->
                                var isPurchased by remember { mutableStateOf(false) }

                                // Firestore에서 구매 여부를 한 번만 확인
                                LaunchedEffect(recipe.id, userId) {
                                    if (userId.isNotBlank() && recipe.id.isNotBlank()) {
                                        val docRef = FirebaseFirestore
                                            .getInstance()
                                            .collection("user")
                                            .document(userId)
                                            .collection("purchased")
                                            .document(recipe.id)

                                        try {
                                            val snapshot = docRef.get().await()
                                            isPurchased = snapshot.exists()
                                        } catch (e: Exception) {
                                            Log.e("ChannelViewPageScreen", "구매 상태 확인 실패: ${e.message}")
                                            isPurchased = false
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .animateItemPlacement()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            Log.d("RECIPE_DEBUG", "Clicked recipe id=${recipe.id}")
                                            val intent = Intent(context, RecipeCookingActivity::class.java).apply {
                                                putExtra("recipe_id", recipe.id)
                                            }
                                            launcher.launch(intent)
                                        }
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(4.dp)
                                ) {
                                    RecipeCard(
                                        recipe = recipe,
                                        isPurchased = isPurchased,
                                        onClick = {
                                            Log.d("RECIPE_DEBUG", "Clicked recipe id=${recipe.id}")
                                            val intent = Intent(context, RecipeCookingActivity::class.java).apply {
                                                putExtra("recipe_id", recipe.id)
                                            }
                                            launcher.launch(intent)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    "Liked" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "좋아요한 레시피가 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    )
}
