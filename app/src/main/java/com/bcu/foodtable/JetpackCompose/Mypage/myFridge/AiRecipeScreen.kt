import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.navigation.NavController
import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.RecipeSaveViewModel
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// === RecipeCookingScreen 과 동일 톤의 라이트 테마 ===
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



/** Storage 경로/gs:///http 모두를 downloadUrl로 통일 */
@Composable
private fun rememberDownloadUrl(imageResId: String): State<String?> {
    val storage = remember { FirebaseStorage.getInstance() }
    val urlState = remember(imageResId) { mutableStateOf<String?>(null) }

    LaunchedEffect(imageResId) {
        try {
            val raw = imageResId.trim()
            when {
                raw.startsWith("http", ignoreCase = true) -> {
                    urlState.value = raw // 이미 URL
                }
                raw.startsWith("gs://", ignoreCase = true) -> {
                    val ref = storage.getReferenceFromUrl(raw)
                    urlState.value = ref.downloadUrl.await().toString()
                }
                else -> {
                    // storage 상대경로 (recipe_image%2F... 처럼)
                    val path = raw.replace("%2F", "/").trimStart('/')
                    val ref = storage.reference.child(path)
                    urlState.value = ref.downloadUrl.await().toString()
                }
            }
        } catch (e: Exception) {
            Log.e("AiRecipeScreen", "이미지 URL 변환 실패: ${e.message}")
            urlState.value = null
        }
    }
    return urlState
}

@Composable
fun AiRecipeScreen(
    recipe: RecipeItem,
    navController: NavController,
    onSaveToChannel: (RecipeItem) -> Unit, // 기존 시그니처 유지
    userId: String,
    recipeSaveViewModel: RecipeSaveViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var showChannelDialog by remember { mutableStateOf(false) }
    val myChannels by recipeSaveViewModel.myChannels.collectAsState()
    val saveSuccess by recipeSaveViewModel.saveSuccess.collectAsState()
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // ===== 제목 보정: AI가 준 name이 비었거나 "AI 추천 요리"면 order에서 첫 스텝의 (제목) 추출하여 대체 =====
    val computedTitle = remember(recipe.name) {
        recipe.name.trim().ifBlank { "새 레시피" }
    }

    // ===== 이미지 downloadUrl 확보 (http/gs/storage 경로 모두 케어) =====
    val downloadUrl = rememberDownloadUrl(recipe.imageResId)

    FoodTableTheme {
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
                modifier = Modifier.padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top 카드
                item {
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
                            Text(
                                computedTitle,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE25532),
                                    letterSpacing = (-0.5).sp
                                ),
                                modifier = Modifier.padding(bottom = 16.dp)
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
                                Log.d("AiRecipeScreen", "imageResId(raw): ${recipe.imageResId}")
                                AsyncImage(
                                    model = downloadUrl.value,
                                    contentDescription = computedTitle,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(24.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recipe.estimatedCalories?.let {
                                    InfoChip(
                                        text = it,
                                        icon = "🔥",
                                        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                                    )
                                }
                                InfoChip(
                                    text = "AI 추천",
                                    icon = "🤖",
                                    backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // 설명/태그/카테고리
                if (recipe.description.isNotBlank() || recipe.tags.isNotEmpty() || recipe.C_categories.isNotEmpty()) {
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
                                if (recipe.description.isNotBlank()) {
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
                                }

                                if (recipe.C_categories.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        recipe.C_categories.forEach { category ->
                                            CategoryChip(
                                                text = category,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                if (recipe.tags.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(recipe.tags) { tag ->
                                            TagChip(tag = if (tag.startsWith("#")) tag else "#$tag")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 재료
                if (recipe.ingredients.isNotEmpty()) {
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
                                            onClick = { /* 필요시 가격 비교 액션 추가 가능 */ }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 조리 단계(문자열 그대로 노출)
                val steps = recipe.order
                    .split("○")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

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
                                        Text(
                                            text = "○ $s",
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

                // 저장 버튼
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

            // 채널 선택 다이얼로그
            if (showChannelDialog) {
                ChannelSelectDialog(
                    channels = myChannels,
                    onSelect = { channel ->
                        showChannelDialog = false
                        selectedChannel = channel
                        coroutineScope.launch {
                            recipeSaveViewModel.saveRecipeToChannel(
                                recipe.copy(name = computedTitle), // 제목 보정 적용한 상태로 저장
                                channel,
                                userId
                            )
                        }
                    },
                    onDismiss = { showChannelDialog = false }
                )
            }

            // 저장 성공 시 채널 화면으로 이동
            LaunchedEffect(saveSuccess) {
                if (saveSuccess == true && selectedChannel != null) {
                    navController.navigate("channelView/${Uri.encode(selectedChannel!!.name)}") {
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
            popUpTo(0)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
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
                    Log.i("AI ChatTest","채널 선택됨 . 선택된 채널 : ${channels.getOrNull(selectedIndex)} ")
                },
                enabled = selectedIndex != -1
            ) { Text("확인") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

// ===== RecipeCookingScreen 에서 가져온 보조 UI =====
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
                        RoundedCornerShape(4.dp)
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
            modifier = modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, backgroundColor),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = textColor
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                fontSize = 16.sp
            )
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = textColor
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                fontSize = 16.sp
            )
        }
    }
}
