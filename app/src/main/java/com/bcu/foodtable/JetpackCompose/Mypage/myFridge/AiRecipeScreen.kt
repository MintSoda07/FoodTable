import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.navigation.NavController
import com.bcu.foodtable.JetpackCompose.AI.AiHelperViewModel
import com.bcu.foodtable.JetpackCompose.AI.AiHelperViewModelFactory
import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.RecipeSaveViewModel
import com.bcu.foodtable.ai.OpenAIClient
import com.bcu.foodtable.ui.home.Screen
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem // RecipeItem 경로는 기존과 동일

// RecipeCookingScreen.kt 에서 가져온 WarmLightColorScheme 정의를 AiRecipeScreen.kt 에도 동일하게 적용
private val WarmLightColorScheme = lightColorScheme(
    primary = Color(0xFFE25532),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2D6),
    onPrimaryContainer = Color(0xFF5C2B1B),
    secondary = Color(0xFFFFF4ED),
    onSecondary = Color(0xFF4B3C35),
    secondaryContainer = Color(0xFFFDE1D5),
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = Color(0xFFB9806D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E0DC),
    onTertiaryContainer = Color(0xFF4E342E),
    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF3A2C28),
    surface = Color.White,
    onSurface = Color(0xFF2E2E2E),
    surfaceVariant = Color(0xFFFBE7DF),
    onSurfaceVariant = Color(0xFF5F5F5F),
    outline = Color(0xFFDDC7BD),
    outlineVariant = Color(0xFFF0E0D8),
    inverseSurface = Color(0xFF3A2C28),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFFF8F6B),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFDECEA),
    onErrorContainer = Color(0xFF8B0000)
)

@Composable
fun FoodTableTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WarmLightColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}

@Composable
fun AiRecipeScreen(
    recipe: RecipeItem,
    navController: NavController,
    onSaveToChannel: (RecipeItem) -> Unit, // 기존 기능 유지
    userId: String,
    aiViewModel: AiHelperViewModel = viewModel(
        factory = AiHelperViewModelFactory(OpenAIClient())
    ),// <- AI 뷰모델
    recipeSaveViewModel: RecipeSaveViewModel = viewModel()
) {
    var showChannelDialog by remember { mutableStateOf(false) }
    val myChannels by recipeSaveViewModel.myChannels.collectAsState()
    val saveSuccess by recipeSaveViewModel.saveSuccess.collectAsState()
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var imageError by remember { mutableStateOf(false) }
    val uiState by aiViewModel.uiState.collectAsState()
    val imageUrl = recipe.imageResId
    FoodTableTheme { // 테마 적용
        // RecipeCookingScreen.kt의 배경 그라데이션 적용

        //이미지 생성 확인 디버그
        LaunchedEffect(recipe.imageResId) {
            Log.d("AiRecipeScreen", "DEBUG: imageResId updated → ${recipe.imageResId}")
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6C63FF).copy(alpha = 0.08f),
                            Color(0xFF4ECDC4).copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = 1200f
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp), // RecipeCookingScreen.kt와 동일한 패딩
                contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp) // 간격 조정
            ) {
                item {
                    // 1. 레시피 토퍼: 제목 + 이미지 (RecipeCookingScreen.kt의 Top Card 디자인 적용)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            // 제목
                            Text(
                                recipe.name,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE25532), // primary 색상
                                    letterSpacing = (-0.5).sp
                                ),
                                modifier = Modifier.padding(bottom = 16.dp) // 간격 조정
                            )


                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    uiState.isSending -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    uiState.imageError -> {
                                        Button(
                                            onClick = { aiViewModel.generateImageAgain(recipe) },
                                            shape = RoundedCornerShape(20.dp)
                                        ) { Text("이미지 다시 불러오기") }
                                    }
                                    !uiState.imageUrl.isNullOrBlank() -> {
                                        AsyncImage(
                                            model = uiState.imageUrl,
                                            contentDescription = recipe.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                            onError = {
                                                // 이미지 자체 로딩 실패시
                                                aiViewModel.generateImageAgain(recipe)
                                            }
                                        )
                                    }
                                    else -> {
                                        Button(
                                            onClick = { aiViewModel.generateImageAgain(recipe) },
                                            shape = RoundedCornerShape(20.dp)
                                        ) { Text("이미지 불러오기") }
                                    }
                                }
                            }





                            // Floating Info Cards (RecipeCookingScreen.kt의 InfoChip 디자인 적용)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.Start) // RecipeCookingScreen과 동일
                                    .padding(top = 16.dp), // 이미지 아래 간격
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recipe.estimatedCalories?.let {
                                    InfoChip(
                                        text = it,
                                        icon = "🔥",
                                        backgroundColor = MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.9f
                                        )
                                    )
                                }
                                // AiRecipeScreen에서는 단계 수가 없을 수 있으므로, 임의로 "AI 추천" 칩 추가
                                InfoChip(
                                    text = "AI 추천",
                                    icon = "🤖",
                                    backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // 2. 설명 카드 (RecipeCookingScreen.kt의 Recipe Info Section 디자인 적용)
                if (!recipe.description.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    "설명",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    recipe.description,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onBackground,
                                        lineHeight = 28.sp
                                    ),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Category and Tags with Modern Chips (RecipeCookingScreen.kt의 CategoryChip, TagChip 디자인 적용)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recipe.C_categories.forEach { category ->
                                        CategoryChip(
                                            text = category,
                                            modifier = Modifier.weight(
                                                1f,
                                                fill = false
                                            ) // Chip 크기 조절
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(recipe.tags) { tag ->
                                        TagChip(tag = if (tag.startsWith("#")) tag else "#$tag")
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. 재료 카드 (RecipeCookingScreen.kt의 Ingredients Section 디자인 적용)
                if (!recipe.ingredients.isNullOrEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Text(
                                        "🥘",
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        "재료",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    recipe.ingredients.forEach { ingredient ->
                                        IngredientItem(
                                            ingredient = ingredient,
                                            onClick = { /* 재료 클릭 시 동작 (RecipeCookingScreen.kt처럼 네이버 쇼핑 연결 가능) */ }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. 조리 단계 카드 (RecipeCookingScreen.kt의 CookingStepCard 디자인 적용)
                val steps = recipe.order.split("○").filter { it.isNotBlank() }
                if (steps.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    "조리 단계",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                steps.forEach { s ->
                                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                                        // Text(text = "") 혹은 아예 이 줄 제거
                                        Spacer(modifier = Modifier.width(0.dp))
                                        Text(
                                            text = s.trim(),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 26.sp
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. 하단 저장 버튼 (RecipeCookingScreen.kt의 ModernActionButton 디자인 적용)
                item {
                    ModernActionButton(
                        text = "내 채널에 저장",
                        onClick = {
                            recipeSaveViewModel.loadMyChannels(userId)
                            showChannelDialog = true
                        },
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            var is_saved = false;
            if (showChannelDialog) {
                ChannelSelectDialog(
                    channels = myChannels,
                    onSelect = { channel ->
                        showChannelDialog = false
                        selectedChannel = channel
                        recipeSaveViewModel.saveRecipeToChannel(recipe, channel)
                    },
                    onDismiss = { showChannelDialog = false }
                )
            }

            // 저장 성공시 안내 및 이동
            LaunchedEffect(saveSuccess) {
                if (is_saved){
                    Log.i("AI ChatTest","새 페이지 시도했으나 차단됨 $is_saved")
                    return@LaunchedEffect}
                is_saved = true;
                Log.i("AI ChatTest","새 페이지 호출되는중 $is_saved")

                    if (saveSuccess == true && selectedChannel != null) {
                        navController.navigate("channelView/${Uri.encode(selectedChannel!!.name)}") {
                            Log.i("AI ChatTest","채널 경로 : channelView/${Uri.encode(selectedChannel!!.name)}")
                            popUpTo("subscribe") { inclusive = false }
                            launchSingleTop = true
                        }
                        recipeSaveViewModel.resetSaveSuccess()
                    }
                }



        }
        }
    BackHandler {
        navController.navigate("fridge") {
            popUpTo(0) // 스택 전부 삭제하고
            launchSingleTop = true
        }
    }
    }


@Composable
fun ChannelSelectDialog(
    channels: List<Channel>,
    onSelect: (Channel) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(-1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("내 채널 선택", style = MaterialTheme.typography.titleLarge) },
        text = {
            if (channels.isEmpty()) {
                Text("생성된 채널이 없습니다.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    itemsIndexed(channels) { idx, channel ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selectedIndex == idx) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    else Color.Transparent
                                )
                                .clickable { selectedIndex = idx }
                                .padding(8.dp)
                        ) {
                            AsyncImage(
                                model = channel.imageResId,
                                contentDescription = channel.name,
                                modifier = Modifier.size(72.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                channel.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedIndex == idx)
                                    MaterialTheme.colorScheme.primary
                                else Color.Unspecified
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedIndex != -1) onSelect(channels[selectedIndex])
                    Log.i("AI ChatTest","채널 선택됨 . 선택된 채널 : ${channels[selectedIndex]} ")
                },
                enabled = selectedIndex != -1
            ) { Text("확인") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

// RecipeCookingScreen.kt에서 가져온 Composable 함수들
@Composable
private fun InfoChip(
    text: String,
    icon: String,
    backgroundColor: Color
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = icon,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                ),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun TagChip(tag: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun IngredientItem(
    ingredient: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(4.dp) // 점 대신 약간 둥근 사각형으로 변경
                    )
            )
            Text(
                text = ingredient,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            // 클릭 가능 아이콘 제거 (AiRecipeScreen에서는 네이버 쇼핑 연결 기능이 없다고 가정)
            // Icon(
            //     imageVector = Icons.Default.ChevronRight,
            //     contentDescription = null,
            //     tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            //     modifier = Modifier.size(20.dp)
            // )
        }
    }
}

@Composable
private fun ModernActionButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    isOutlined: Boolean = false
) {
    if (isOutlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(56.dp), // RecipeCookingScreen과 동일한 높이
            shape = RoundedCornerShape(16.dp), // RecipeCookingScreen과 동일한 둥근 정도
            border = BorderStroke(2.dp, backgroundColor),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = textColor
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy( // RecipeCookingScreen과 동일한 폰트 스타일
                    fontWeight = FontWeight.SemiBold
                ),
                fontSize = 16.sp // RecipeCookingScreen과 동일한 폰트 사이즈
            )
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(56.dp), // RecipeCookingScreen과 동일한 높이
            shape = RoundedCornerShape(16.dp), // RecipeCookingScreen과 동일한 둥근 정도
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = textColor
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp, // RecipeCookingScreen과 동일한 그림자
                pressedElevation = 2.dp
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy( // RecipeCookingScreen과 동일한 폰트 스타일
                    fontWeight = FontWeight.SemiBold
                ),
                fontSize = 16.sp // RecipeCookingScreen과 동일한 폰트 사이즈
            )
        }
    }
}