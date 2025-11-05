package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity
import com.bcu.foodtable.R
import com.bcu.foodtable.ui.home.Screen
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
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
    val userId = UserManager.getUser()?.uid.orEmpty()
    // 탭 상태
    var selectedTab by remember { mutableStateOf("레시피") }

    // ViewModel 로부터 상태 수집
    val channel         by viewModel.channel.collectAsState()
    val recipes         by viewModel.recipes.collectAsState()
    val isSubscribed    by viewModel.isSubscribed.collectAsState()
    val subscriberCount by viewModel.subscriberCount.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()

    // 최초 데이터 로드
    LaunchedEffect(channelName) {
        if (channelName.isNotBlank()) {
            viewModel.loadAll(channelName, userId)
        }
    }
    BackHandler {
        navController.navigate(Screen.Subscribe.route) {
            popUpTo(0) // 스택 전체 비우고
            launchSingleTop = true
        }
    }


    // 레시피 클릭 후 ActivityResult 콜백
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadAll(channelName, userId)
        }
    }

    // 로딩, 인증, 에러 처리
    when {
        isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        userId.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("로그인이 필요합니다.")
        }
        channel == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("채널 정보를 불러올 수 없습니다.")
        }
        else -> {
            // 레시피 목록을 미리 계산 (LazyColumn 바깥에서 remember 호출)
            val displayList = remember(recipes, selectedTab) {
                if (selectedTab == "레시피") {
                    recipes
                } else /* selectedTab == "좋아요" */ {
                    recipes.filter { it.likedUsers?.contains(userId) == true }
                }
            }

            Scaffold(
                // 배너를 topBar 에 직접 배치해 상단 여백 제거
                topBar = {
                    BannerSection(backgroundPathOrUrl = channel!!.backgroundResId)
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding      = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 프로필 카드 (배너와 살짝 겹침)
                    item {
                        ProfileSection(
                            channel           = channel!!,
                            subscriberCount   = subscriberCount,
                            isSubscribed      = isSubscribed,
                            onToggleSubscribe = { viewModel.toggleSubscription(channel!!.name, userId) },
                            currentUserId     = userId,
                            modifier          = Modifier
                                .fillMaxWidth()
                                .offset(y = (-16).dp)
                                .padding(horizontal = 4.dp)
                                .zIndex(1f)
                                .animateContentSize()
                        )
                    }

                    // 채널 소유자 전용 버튼
                    if (userId == channel!!.owner) {
                        item {
                            OwnerActionSection(
                                isOwner        = (userId == channel!!.owner),
                                channelName    = channel!!.name,
                                navController  = navController,
                                onCreateRecipe = { navController.navigate("write/${Uri.encode(channel!!.name)}") },
                                modifier       = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            )
                        }
                        item {
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }

                    // 스크롤 고정 탭바
                    stickyHeader {
                        TabSection(
                            selectedTab   = selectedTab,
                            onTabSelected = { selectedTab = it },
                            modifier      = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }

                    // 빈 상태 혹은 레시피 2열 그리드
                    if (displayList.isEmpty()) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text  = if (selectedTab == "Recipes")
                                        "등록된 레시피가 없습니다."
                                    else
                                        "좋아요한 레시피가 없습니다.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(displayList.chunked(2)) { rowRecipes ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowRecipes.forEach { recipe ->
                                    RecipeGridItem(
                                        recipe           = recipe,
                                        userId           = userId,
                                        channelName      = channel!!.name,
                                        channelOwnerId   = channel!!.owner,
                                        onRecipeClick    = { id ->
                                            launcher.launch(
                                                Intent(
                                                    context,
                                                    RecipeCookingActivity::class.java
                                                ).putExtra("recipe_id", id)
                                            )
                                        },
                                        onPurchaseRecipe = { item -> viewModel.purchaseRecipe(item, userId) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // 한 행이 홀수일 때 빈 칸 채우기
                                if (rowRecipes.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BannerSection(backgroundPathOrUrl: String) {
    val context = LocalContext.current

    var bannerUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(backgroundPathOrUrl) {
        bannerUrl = runCatching {
            if (backgroundPathOrUrl.startsWith("http")) backgroundPathOrUrl
            else Firebase.storage.reference
                .child(backgroundPathOrUrl)
                .downloadUrl
                .await()
                .toString()
        }.getOrNull()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)  // 이전 160.dp → 200.dp로 확대
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
    ) {
        if (bannerUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(bannerUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.baseline_restaurant_menu_24)
                    .error(R.drawable.baseline_restaurant_menu_24)
                    .build(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.matchParentSize()
            )
        } else {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.4f)
                    )
                )
        )
    }
}
@Composable
fun ProfileSection(
    channel: Channel,
    subscriberCount: Int,
    isSubscribed: Boolean?,
    onToggleSubscribe: () -> Unit,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier
            .fillMaxWidth(),  // 화면 폭 가득 채기
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp, vertical = 16.dp) // 내부 여백만
        ) {
            // 크게 키운 아바타
            AsyncImage(
                model             = channel.imageResId,
                contentDescription= null,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
            )

            Spacer(Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text  = channel.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "$subscriberCount 명 구독 중",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (currentUserId != channel.owner) {
                ElevatedButton(
                    onClick = onToggleSubscribe,
                    shape   = RoundedCornerShape(50),
                    colors  = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor   = Color.White
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .defaultMinSize(minWidth = 96.dp)
                ) {
                    Text(
                        text  = when (isSubscribed) {
                            true  -> "구독중"
                            false -> "구독"
                            null  -> "로딩…"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}



@Composable
fun OwnerActionSection(
    isOwner: Boolean,
    channelName: String,
    navController: NavHostController,
    onCreateRecipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOwner) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        OutlinedButton(
            onClick = onCreateRecipe,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("레시피 생성")
        }
        OutlinedButton(
            onClick = {
                // channelName을 넘겨서 채널 관리 화면으로 이동
                navController.navigate("channel_management/${Uri.encode(channelName)}")
            },
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("채널 관리")
        }
    }
}


@Composable
fun TabSection(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("레시피", "좋아요")
    // 1) indexOf 결과를 0..tabs.lastIndex 범위로 보정
    val selectedIndex = tabs.indexOf(selectedTab).coerceIn(0, tabs.lastIndex)

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier         = modifier,
        containerColor   = MaterialTheme.colorScheme.surface,
        contentColor     = MaterialTheme.colorScheme.primary,
        indicator        = { positions ->
            Box(
                Modifier
                    .tabIndicatorOffset(positions[selectedIndex])
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        },
        divider = {}
    ) {
        tabs.forEachIndexed { idx, title ->
            Tab(
                selected = (selectedIndex == idx),
                onClick  = { onTabSelected(title) },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text  = title,
                    style = if (selectedIndex == idx)
                        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    else
                        MaterialTheme.typography.bodyMedium,
                    color = if (selectedIndex == idx)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeGridItem(
    recipe: RecipeItem,
    userId: String,
    channelName: String,
    channelOwnerId: String,
    onRecipeClick: (String) -> Unit,
    onPurchaseRecipe: (RecipeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope     = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var pending    by remember { mutableStateOf(false) }
    var boughtMap  by remember { mutableStateOf<Map<String,Boolean>>(emptyMap()) }

    // 구매 여부 로드
    LaunchedEffect(recipe.id, userId) {
        if (userId.isNotBlank()) {
            val bought = try {
                FirebaseFirestore.getInstance()
                    .collection("user")
                    .document(userId)
                    .collection("purchased")
                    .document(recipe.id)
                    .get().await().exists()
            } catch (_: Exception) {
                false
            }
            boughtMap = boughtMap + (recipe.id to bought)
        }
    }

    val isPurchased = boughtMap[recipe.id] == true
    val isMine      = recipe.contained_channel == channelName && channelOwnerId == userId

    Card(
        modifier  = modifier
            .aspectRatio(1f)
            .clickable {
                if (isMine || isPurchased) onRecipeClick(recipe.id)
                else showDialog = true
            },
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model             = recipe.imageResId,
                contentDescription= recipe.name,
                contentScale      = ContentScale.Crop,
                modifier          = Modifier.matchParentSize()
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.6f)
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(
                        color = if (isPurchased)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPurchased) Icons.Default.CheckCircle else Icons.Default.AttachMoney,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))

                Text(
                    text = if (!isMine && isPurchased)  // ← 내 레시피가 아니고, 구매된 경우만
                        "구매됨"
                    else
                        "${recipe.cost}원",        // 그 외에는 모두 가격 표시
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (isMine) {
                Surface(
                    modifier      = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape         = RoundedCornerShape(8.dp),
                    color         = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                ) {
                    Text(
                        "내 레시피",
                        style    = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text      = recipe.name,
                style     = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Bold),
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                modifier  = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint    = Color.White,
                modifier= Modifier.align(Alignment.BottomEnd).padding(8.dp).size(20.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { if (!pending) showDialog = false },
            title   = { Text("${recipe.name} 구매") },
            text    = {
                if (pending) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("구매 요청 중…")
                    }
                } else {
                    Text("이 레시피를 ${recipe.cost}원에 구매하시겠습니까?")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = true
                    scope.launch {
                        onPurchaseRecipe(recipe)
                        boughtMap = boughtMap + (recipe.id to true)
                        pending = false
                        showDialog = false
                    }
                }) {
                    Text("구매하기")
                }
            },
            dismissButton = {
                if (!pending) {
                    TextButton(onClick={ showDialog = false }) {
                        Text("취소")
                    }
                }
            }
        )
    }
}
