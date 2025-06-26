package com.bcu.foodtable.ui.home


import AiRecipeScreen
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.animation.with
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingActivity
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.JetpackCompose.HomeViewModel
import com.bcu.foodtable.JetpackCompose.Mypage.ProfileMainScreen
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import java.util.Calendar
import kotlinx.coroutines.launch
import com.bcu.foodtable.ai.AIRecommendationService
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.text.font.FontStyle
import com.bcu.foodtable.JetpackCompose.Subscribe.Channel.ChannelViewPageScreen
import com.bcu.foodtable.JetpackCompose.Subscribe.SubscribeScreen
import com.bcu.foodtable.JetpackCompose.Subscribe.SubscribeViewModel
import com.bcu.foodtable.JetpackCompose.RecipeStorage.MyRecipeStorageScreen
import com.bcu.foodtable.JetpackCompose.Social.SocialScreen
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.bcu.foodtable.JetpackCompose.Social.CardGameScreen
import com.bcu.foodtable.JetpackCompose.Social.CommunityTab
import com.bcu.foodtable.JetpackCompose.Social.LadderGameScreen
import com.bcu.foodtable.JetpackCompose.Social.MiniGameMenu
import com.bcu.foodtable.JetpackCompose.Social.PayerRouletteGameScreen
import com.bcu.foodtable.JetpackCompose.Social.PostDetailScreen
import com.bcu.foodtable.JetpackCompose.Social.RouletteGameScreen
import com.bcu.foodtable.JetpackCompose.Social.WritePostScreen
import com.bcu.foodtable.JetpackCompose.Subscribe.Channel.WriteScreen
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCalorieViewModel
import com.bcu.foodtable.JetpackCompose.Mypage.Health.HealthConnectScreen
import com.bcu.foodtable.JetpackCompose.Mypage.Health.HealthConnectViewModel
import com.bcu.foodtable.JetpackCompose.Social.DetailedChatScreen
import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.FridgeScreen
import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.FridgeViewModel
import com.bcu.foodtable.JetpackCompose.Social.MatzipViewModel
import com.bcu.foodtable.JetpackCompose.Social.RestaurantV2MapScreen
import com.bcu.foodtable.JetpackCompose.Social.UserProfileScreen
import com.bcu.foodtable.JetpackCompose.Subscribe.Channel.EditRecipeScreen
import com.bcu.foodtable.JetpackCompose.Subscribe.ChannelManagementScreen
import com.bcu.foodtable.ui.ChallengeScreen
import com.bcu.foodtable.ui.rank.RankScreenImproved
import com.bcu.foodtable.viewmodel.ChallengeViewModel
import com.bcu.foodtable.ui.home.AiChatBox as AiChatBox1
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.composed
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.animation.core.FastOutSlowInEasing

// --- 데이터 모델 및 유틸리티 컴포넌트 ---

/**
 * 텍스트에 타이핑 효과를 적용하는 Composable.
 * @param text 표시할 전체 텍스트.
 * @param typingDelay 글자 사이의 지연 시간 (밀리초).
 */
@Composable
fun TypingAnimatedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    typingDelay: Long = 75L
) {
    var displayedText by remember(text) { mutableStateOf("") }

    LaunchedEffect(key1 = text) {
        displayedText = "" // 텍스트가 변경되면 초기화
        text.forEachIndexed { index, _ ->
            // 한 글자씩 추가
            displayedText = text.substring(0, index + 1)
            delay(typingDelay)
        }
    }

    Text(
        text = displayedText,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * 하단 내비게이션 바의 각 화면을 정의하는 Sealed Class.
 * 각 화면은 레이블과 아이콘 리소스 ID를 가집니다.
 */
sealed class Screen(val route: String, val label: String, val icon: Int) {
    object Home : Screen("home", "홈", R.drawable.ic_home_black_24dp)
    object Subscribe : Screen("subscribe", "구독", R.drawable.ic_notifications_black_24dp)
    object Social : Screen("social", "소설", R.drawable.ic_dashboard_black_24dp)
    object RecipeStorage : Screen("storage", "레시피 저장소", R.drawable.baseline_menu_book_24)
    object MyPage : Screen("mypage", "마이페이지", R.drawable.ic_profile_placeholder)
}
/**
 * 카테고리를 표시하는 개별 필터 칩 컴포저블.
 * @param category 표시할 카테고리 텍스트.
 * @param selected 현재 선택되었는지 여부.
 * @param onSelected 칩이 클릭되었을 때 호출될 람다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(category: String, selected: Boolean, onSelected: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onSelected,
        label = { Text(category, style = MaterialTheme.typography.labelMedium) },
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = MaterialTheme.colorScheme.primaryContainer,
            disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledSelectedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp,
            enabled = true,
            selected = selected
        ),
        elevation = FilterChipDefaults.filterChipElevation(
            elevation = if (selected) 2.dp else 0.dp
        )
    )
}

/**
 * 레시피 카드 내에 아이콘과 텍스트 정보를 표시하는 작은 태그 컴포저블
 */
@Composable
fun InfoTag(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(end = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 향상된 레시피 정보를 표시하는 카드 컴포저블.
 * @param recipe 표시할 RecipeItem 데이터.
 * @param onClick 카드 클릭 시 실행될 람다.
 * @param modifier Modifier.
 */
/**
 * 혁신적이고 현대적인 레시피 카드 컴포저블.
 * 글래스모피즘, 그라데이션, 동적 애니메이션을 활용한 프리미엄 디자인
 */
/**
 * 향상되고 테마와 통합된 레시피 정보를 표시하는 카드 컴포저블.
 * 초기에는 축소된 형태로 표시되며, 스와이프나 탭으로 상세 정보를 확장할 수 있습니다.
 *
 * @param recipe 표시할 RecipeItem 데이터.
 * @param onCardClick 카드 자체를 클릭했을 때 실행될 람다 (예: 레시피 상세 페이지 이동).
 * @param modifier Modifier.
 */

// ModernRecipeCardResponsive -> ModernRecipeCard로 이름 유지 (사용자 코드에 맞춰)
// 내부에서 호출하는 헬퍼 함수들의 이름도 사용자의 코드 파일에 있는 이름으로 사용합니다.
// (RecipeCategoryChip, RecipeMetaInfoItem, RecipeDifficultyIndicator, RecipePurchaseButton)
// 단, 이 헬퍼 함수들의 구현을 "처음 보내준 디자인" 기준으로 되돌립니다.

/**
 * 혁신적이고 현대적인 레시피 카드 컴포저블. (원래 디자인 유지 + 기능 추가 버전)
 * 글래스모피즘, 그라데이션, 동적 애니메이션을 활용한 프리미엄 디자인
 * 초기에는 축소된 형태로 표시되며, 스와이프나 탭으로 상세 정보를 확장할 수 있습니다.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ModernRecipeCard( // 함수 이름은 사용자의 파일에 있는 ModernRecipe 그대로 사용
    recipe: RecipeItem,
    estimatedCal: String?,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 원래 디자인의 색상 정의 (다크모드 대응 포함)
    val isDarkTheme = isSystemInDarkTheme()
    val originalGlassBackground = if (isDarkTheme) Color(0x33FFFFFF) else Color(0x26000000) // 투명도 약간 높여서 내용물과 구분
    val originalGlassBorder = Color.White.copy(alpha = 0.2f)
    val originalOnGlassTextColor = Color.White // 원래 글래스 위 텍스트 색상

    // 원래 디자인의 그라데이션 색상 (GlassCategoryChip용)
    val primaryGradientStart = Color(0xFF6B63FF)
    val primaryGradientEnd = Color(0xFFFF6B9D)
    val isPurchased = recipe.isPurchased
    // --- 상태 관리 (기존 ModernRecipeCardResponsive과 동일) ---
    var isExpanded by remember { mutableStateOf(false) }
    val expansionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "expansionProgress"
    )

    // 1) duration 필드를 분 단위 문자열로
    val prepTime = "${recipe.duration}분"

    // 2) estimatedCalories 필드를 바로 사용
    val estimatedCaloriesText = recipe.estimatedCalories ?: "N/A"

    // 3) 난이도는 c_categories 리스트의 두 번째 항목(index 1)에서 파싱
    val difficultyCategory = recipe.C_categories.getOrNull(1) ?: "보통"
    val difficultyLevel = when (difficultyCategory) {
        "쉬움"   -> 1
        "보통"   -> 2
        "어려움" -> 3
        else     -> 2
    }
    val collapsedHeight = 230.dp
    val expandedHeight = 460.dp
    val animatedCardHeight by animateDpAsState(
        targetValue = if (isExpanded) expandedHeight else collapsedHeight,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "cardHeight"
    )
    // --- 상태 관리 끝 ---

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(animatedCardHeight)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)) // 카드의 기본 배경은 테마 유지
            .clickable(onClick = onCardClick)
    ) {
        AsyncImage(
            model = recipe.imageResId,
            contentDescription = "${recipe.name} 이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 카드가 확장될 때 이미지에 시차 효과(Parallax Effect)를 주기 위해 약간 확대하고 이동시킵니다.
                    val scale = 1f + (expansionProgress * 0.1f)
                    scaleX = scale
                    scaleY = scale
                    translationY = expansionProgress * -20.dp.toPx()
                },
            placeholder = painterResource(id = R.drawable.ic_placeholder_dish),
            error = painterResource(id = R.drawable.ic_placeholder_dish_error)
        )

        Box( /* ... 그라데이션 오버레이, 기존 코드와 동일 ... */
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.4f + (0.3f * expansionProgress))
                        ),
                        startY = with(LocalDensity.current) { (animatedCardHeight * 0.3f).toPx() },
                        endY = with(LocalDensity.current) { animatedCardHeight.toPx() }
                    )
                )
        )

        Column( /* ... 상단 컨텐츠 (타이틀, 축소 시 설명), 기존 코드와 동일 ... */
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color.White, // 이미지 위 텍스트는 흰색 유지
                maxLines = if (isExpanded) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 좋아요 버튼 섹션이 제거되었습니다.

        Box( /* ... 확장/축소 핸들, 기존 코드와 동일 ... */
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (isExpanded) 56.dp else 48.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val sensitivity = 2f
                        if (dragAmount < -sensitivity) { isExpanded = true }
                        else if (dragAmount > sensitivity) { isExpanded = false }
                    }
                }
                .clickable { isExpanded = !isExpanded }
                .padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                contentDescription = if (isExpanded) "축소" else "확장",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Center).size(28.dp)
            )
        }

        // --- 상세 정보 섹션 (글래스모피즘) ---
        val detailsContentHeight = 280.dp
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(detailsContentHeight)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = (1f - expansionProgress) * with(density) {
                        (detailsContentHeight - 56.dp).toPx()
                    }
                    alpha = expansionProgress
                }
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background( // << 여기가 원래 디자인의 글래스 배경으로 변경되어야 함
                    color = originalGlassBackground, // 원래 디자인의 글래스 배경색 사용
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .border( // << 원래 디자인의 글래스 테두리로 변경
                    width = 1.dp,
                    color = originalGlassBorder, // 원래 디자인의 글래스 테두리색 사용
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
        ) {
            if (expansionProgress > 0.05f) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp)
                        .alpha(expansionProgress)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        recipe.C_categories.take(4).forEach { category ->
                            // RecipeCategoryChip 호출 시 원래 디자인에 필요한 파라미터 전달
                            RecipeCategoryChip( // 이름은 파일에 있는 대로 RecipeCategoryChip
                                category = category,
                                gradientStart = primaryGradientStart, // 원래 디자인용 파라미터
                                gradientEnd = primaryGradientEnd    // 원래 디자인용 파라미터
                            )
                        }
                    }

                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = originalOnGlassTextColor, // 원래 디자인의 텍스트 색상
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // 상세 섹션 안에서 RecipeMetaInfoItem 호출부만 수정
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RecipeMetaInfoItem(
                            icon = Icons.Filled.Timer,
                            value = prepTime,
                            label = "소요시간",
                            modifier = Modifier.weight(1f)
                        )
                        RecipeMetaInfoItem(
                            icon = Icons.Filled.LocalFireDepartment,
                            value = estimatedCal ?: "N/A",
                            label = "칼로리",
                            modifier = Modifier.weight(1f)
                        )
                        RecipeDifficultyIndicator(
                            level = difficultyLevel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // RecipePurchaseButton 호출, 내부에서 원래 디자인 사용하도록 수정됨
                    RecipePurchaseButton(
                        isPurchased = isPurchased,
                        cost = recipe.cost,
                        onClick = {
                            if (!isPurchased) {
                                println("Purchase button clicked for ${recipe.name}")
                            }
                        }
                        // buttonColors 파라미터는 RecipePurchaseButton 내부에서 원래 디자인 색상 사용
                    )
                }
            }
        }

        if (!isPurchased && !isExpanded && recipe.cost > 0) { /* ... 플로팅 가격 태그, 기존 코드와 유사하게 (테마 색상 사용 유지 또는 원래 색상으로 변경 가능) ... */
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .background(
                        brush = Brush.horizontalGradient( // 이 부분은 테마 색상 유지 또는 원래 카드에 있던 그라디언트 사용
                            colors = listOf(primaryGradientStart, primaryGradientEnd.copy(alpha = 0.8f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sell,
                        contentDescription = "비용",
                        tint = Color.White, // 가격 태그 텍스트/아이콘 흰색
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${recipe.cost}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

// --- 아래는 헬퍼 Composable들을 "처음 보내주신 디자인" 기준으로 복원한 버전입니다 ---
// 함수 이름은 ModernRecipeCard에서 호출하는 이름과 동일하게 유지합니다.

/**
 * 글래스모피즘 카테고리 칩 (원래 디자인 복원 - 쉬머 효과 포함)
 */
@Composable
fun RecipeCategoryChip( // 함수 이름은 ModernRecipeCard에서 호출하는 이름 그대로 사용
    category: String,
    gradientStart: Color, // 원래 디자인에 필요했던 파라미터
    gradientEnd: Color,   // 원래 디자인에 필요했던 파라미터
    modifier: Modifier = Modifier
    // backgroundColor, textColor 파라미터는 제거 (원래 디자인은 하드코딩된 색상 사용)
) {
    val shimmerOffset by rememberInfiniteTransition(label = "shimmerChip").animateFloat(
        initialValue = -1.5f, // 범위 조정으로 쉬머 효과 더 잘 보이게
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing), // 속도 약간 빠르게
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerChipOffset"
    )

    Box(
        modifier = modifier // 외부에서 전달된 Modifier 사용
            .clip(RoundedCornerShape(20.dp))
            .background( // 원래 디자인의 배경
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border( // 원래 디자인의 테두리 (쉬머 효과)
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(gradientStart.copy(alpha = 0.7f), gradientEnd.copy(alpha = 0.7f), gradientStart.copy(alpha = 0.7f)), // 자연스러운 반복을 위해 색상 추가
                    start = Offset(shimmerOffset * 200f - 100f, 0f), // 오프셋 계산 수정
                    end = Offset(shimmerOffset * 200f + 100f, 0f)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp) // 패딩 조정
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.9f) // 원래 디자인의 텍스트 색상
        )
    }
}

/**
 * 메타 정보 카드 (원래 디자인 복원)
 */
@Composable
fun RecipeMetaInfoItem( // 함수 이름은 ModernRecipeCard에서 호출하는 이름 그대로 사용
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
    // contentColor, containerColor 파라미터 제거 (원래 디자인은 하드코딩된 색상 사용)
) {
    Box(
        modifier = modifier
            .height(64.dp) // 원래 디자인의 높이
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f)) // 원래 디자인의 배경
            .border( // 원래 디자인의 테두리
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp) // 패딩 조정으로 내부 공간 확보
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center, // 수직 중앙 정렬
            modifier = Modifier.fillMaxSize()
        ) {
            Row( // 아이콘과 값 텍스트를 한 줄에 배치
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White.copy(alpha = 0.8f), // 아이콘 색상 및 투명도 조정
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp)) // 아이콘과 텍스트 사이 간격
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp // 폰트 크기 약간 조정
                    ),
                    color = Color.White // 값 텍스트 색상
                )
            }
            Spacer(modifier = Modifier.height(4.dp)) // 값과 라벨 사이 간격
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), // 라벨 폰트 크기 조정
                color = Color.White.copy(alpha = 0.6f), // 라벨 텍스트 색상 및 투명도 조정
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * 난이도 인디케이터 (원래 디자인 복원 - 세로 막대형, 그라데이션)
 */
@Composable
fun RecipeDifficultyIndicator( // 함수 이름은 ModernRecipeCard에서 호출하는 이름 그대로 사용
    level: Int, // 1 (쉬움), 2 (보통), 3 (어려움)
    modifier: Modifier = Modifier
    // activeColor, inactiveColor, textColor 파라미터 제거 (원래 디자인은 하드코딩된 색상/그라데이션 사용)
) {
    Box(
        modifier = modifier
            .height(64.dp) // 원래 디자인의 높이
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f)) // 원래 디자인의 배경
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround, // 내부 요소들 간격 균등하게
            modifier = Modifier.fillMaxSize()
        ) {
            Row( // 막대들을 가로로 배치
                verticalAlignment = Alignment.Bottom, // 막대들이 아래쪽 기준으로 정렬
                horizontalArrangement = Arrangement.spacedBy(6.dp) // 막대 사이 간격 조정
            ) {
                val barBaseHeight = 6.dp // 막대 최소 높이
                val heightIncrement = 6.dp // 레벨당 높이 증가량
                val barWidth = 10.dp // 막대 너비 조정

                (0..2).forEach { index -> // 0, 1, 2 (3개 막대)
                    val isActive = index < level
                    Box(
                        modifier = Modifier
                            .width(barWidth)
                            .height(barBaseHeight + (heightIncrement * (2 - index))) // 원래 디자인의 높이 변화 방식 (세번째 막대가 가장 김)
                            .clip(RoundedCornerShape(4.dp))
                            .then( // Modifier.then 사용하여 조건부 Modifier 적용
                                if (isActive) {
                                    Modifier.background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFFFFD93D), Color(0xFFFF884B), Color(0xFFFF6B9D))
                                        )
                                    )
                                } else {
                                    Modifier.background(
                                        Color.White.copy(alpha = 0.2f)
                                    )
                                }
                            )
                    )
                }
            }
            // Spacer(modifier = Modifier.height(4.dp)) // 막대와 텍스트 사이 간격은 Arrangement.SpaceAround로 조절
            Text( // "난이도" 텍스트 표시 (원래 디자인)
                text = when (level) { // 실제 난이도 텍스트 표시 (개선된 부분 유지)
                    1 -> "쉬움"
                    2 -> "보통"
                    3 -> "어려움"
                    else -> "난이도"
                },
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp), // 폰트 크기 조정
                color = Color.White.copy(alpha = 0.7f), // 텍스트 색상
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}


/**
 * 애니메이션 구매 버튼 (원래 디자인 복원 - Box 기반, 특정 그라데이션, AnimatedContent)
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RecipePurchaseButton( // 함수 이름은 ModernRecipeCard에서 호출하는 이름 그대로 사용
    isPurchased: Boolean,
    cost: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // 외부 Modifier 사용 가능하게
    // buttonColors 파라미터 제거 (원래 디자인은 하드코딩된 그라데이션 사용)
) {
    // 원래 AnimatedPurchaseButton의 scale 애니메이션은 targetValue가 같아 효과가 없었으므로 제거하거나,
    // 실제 인터랙션에 따른 스케일 변경을 원하시면 추가 구현 필요. 여기서는 제거.

    Box(
        modifier = modifier // 외부에서 전달된 Modifier 사용 (fillMaxWidth 등)
            .fillMaxWidth() // 버튼 너비 채우도록 기본 설정
            .height(52.dp)  // 높이 조정
            .clip(RoundedCornerShape(26.dp)) // 타원형에 가까운 둥근 모서리
            .background( // 원래 디자인의 그라데이션 배경
                if (isPurchased) {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF00BFA5), Color(0xFF00C896)) // 구매 완료 시 초록 계열 그라데이션
                    )
                } else {
                    Brush.linearGradient( // 구매 가능 시 원래의 보라-핑크 그라데이션
                        colors = listOf(Color(0xFF7E57C2), Color(0xFFE91E63)) // 좀 더 강렬한 색상 조합으로 변경
                    )
                }
            )
            .clickable(enabled = !isPurchased) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent( // 아이콘과 텍스트 전환 애니메이션
            targetState = isPurchased,
            transitionSpec = {
                if (targetState) { // true (purchased)가 될 때
                    slideInVertically { height -> height } + fadeIn() with
                            slideOutVertically { height -> -height } + fadeOut()
                } else { // false (not purchased)가 될 때
                    slideInVertically { height -> -height } + fadeIn() with
                            slideOutVertically { height -> height } + fadeOut()
                }
            },
            label = "purchaseButtonContent"
        ) { purchased ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (purchased) Icons.Filled.CheckCircle else Icons.Filled.ShoppingCart,
                    contentDescription = if (purchased) "구매 완료" else "구매하기",
                    tint = Color.White, // 아이콘 색상은 흰색
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (purchased) "구매 완료" else "$cost Salt로 레시피 보기",
                    style = MaterialTheme.typography.titleSmall.copy( // 폰트 스타일 조정
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White // 텍스트 색상은 흰색
                )
            }
        }
    }
}
/**
 * 시간대에 따라 다른 환영 메시지를 반환하는 함수.
 */

fun getHourGreetingMessage(userName: String?): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val nameToShow = userName ?: "손님"


    val messagesByHour = mapOf(
        0 to listOf(
            "0시입니다, $nameToShow! 야식 생각나지 않으세요?",
            "$nameToShow 님, 밤도 깊었어요. 간단한 레시피를 떠올려 보세요.",
            "이 시간엔 간단한 요리 영상이 땡기죠, $nameToShow?",
            "0시, $nameToShow 님만의 비밀 야식 시간이에요!",
            "하루 마무리는 맛있는 생각으로, $nameToShow!"
        ),
        1 to listOf(
            "1시네요, $nameToShow. 출출하신가요?",
            "깊은 밤, 나만의 레시피 정리 시간이에요, $nameToShow!",
            "$nameToShow 님, 간단한 요리로 하루를 정리해보세요.",
            "새벽 감성엔 요리 아이디어가 넘치죠, $nameToShow!",
            "이 시간, 레시피 저장하기 딱 좋습니다, $nameToShow."
        ),
        2 to listOf(
            "$nameToShow 님, 조용한 새벽엔 레시피 탐색 어떠세요?",
            "2시입니다. 나만의 요리를 기록해봐요, $nameToShow!",
            "이 시간에 떠오르는 요리 하나쯤 있지 않나요, $nameToShow?",
            "새벽 2시엔 감성 가득한 레시피 구경이 좋아요.",
            "혼자만의 조용한 레시피 시간입니다, $nameToShow!"
        ),
        3 to listOf(
            "$nameToShow, 아직 안 주무셨다면 따뜻한 요리를 상상해보세요.",
            "3시, 이 시간에도 레시피가 올라오고 있어요!",
            "당신만의 새벽 간식은 무엇인가요, $nameToShow?",
            "지금 이 시간엔 감성 요리가 어울려요, $nameToShow.",
            "새벽이지만, 요리 영감은 언제든 찾아옵니다!"
        ),
        4 to listOf(
            "4시입니다, $nameToShow. 하루를 준비할 시간이에요.",
            "이른 새벽, 가볍게 내일 요리를 계획해보는 건 어때요?",
            "좋은 하루의 시작은 맛있는 생각에서 옵니다, $nameToShow.",
            "레시피 한 줄로 하루가 달라질 수도 있어요, $nameToShow.",
            "고요한 이 시간, 레시피로 하루를 열어보세요."
        ),
        5 to listOf(
            "5시입니다, $nameToShow! 상쾌한 하루를 위한 요리 준비 시간이에요.",
            "기분 좋은 하루, 좋은 레시피로 시작해보세요.",
            "$nameToShow 님, 오늘 첫 요리는 무엇으로 시작할까요?",
            "요리하는 아침은 더욱 특별해요, $nameToShow!",
            "이른 아침, 레시피 탐색으로 에너지 충전!"
        ),
        6 to listOf(
            "좋은 아침입니다, $nameToShow! 간단한 아침 메뉴는 어떠세요?",
            "6시예요, $nameToShow. 든든한 아침 요리로 시작해요!",
            "하루를 시작하는 최고의 방법, 아침 레시피 탐색!",
            "따뜻한 아침을 위한 요리, 찾아볼까요 $nameToShow?",
            "$nameToShow 님, 아침엔 간단하고 건강한 메뉴가 좋아요."
        ),
        7 to listOf(
            "$nameToShow 님의 아침 루틴에 어울릴 레시피는?",
            "든든한 하루를 위한 7시 레시피 탐색 시간!",
            "맛있는 아침으로 활기찬 하루를 시작해봐요.",
            "이른 시간, 영양 가득 레시피 어때요, $nameToShow?",
            "레시피 앱과 함께하는 맛있는 아침!"
        ),
        8 to listOf(
            "8시입니다. 출근 전, 간단한 요리를 만들어보세요!",
            "바쁜 아침, 빠르고 맛있는 요리가 필요하죠.",
            "$nameToShow 님, 오늘 아침 레시피는 어떤 걸로 해볼까요?",
            "시간 절약 아침 레시피, 지금 확인해보세요.",
            "따뜻한 식사가 하루를 바꿉니다, $nameToShow!"
        ),
        9 to listOf(
            "아침 마무리 시간이에요, $nameToShow. 간단한 스낵은 어때요?",
            "출근길 레시피 체크, 잊지 마세요!",
            "$nameToShow 님, 아침에 저장한 레시피 보셨나요?",
            "가볍게 챙기는 아침 메뉴로 시작해요!",
            "오늘 하루도 맛있게 시작해요, $nameToShow!"
        ),
        10 to listOf(
            "10시입니다. 점심 전 요리 아이디어 탐색 시간이에요!",
            "$nameToShow 님, 오늘 점심은 직접 만들어보는 건 어때요?",
            "이 시간엔 인기 레시피를 확인해보세요!",
            "간단한 재료로 빠른 요리를 해보세요, $nameToShow.",
            "레시피로 미리 계획하는 점심 시간!"
        ),
        11 to listOf(
            "점심시간이 가까워졌어요, $nameToShow!",
            "오늘의 점심 메뉴는 정하셨나요?",
            "요즘 인기 있는 점심 요리를 확인해보세요!",
            "레시피 앱에서 점심 메뉴를 골라보세요.",
            "점심시간엔 든든한 한 끼가 필요해요, $nameToShow!"
        ),
        12 to listOf(
            "12시네요, $nameToShow! 맛있는 요리로 기운을 내보세요.",
            "$nameToShow 님, 점심엔 어떤 요리를 드시고 싶으신가요?",
            "좋은 점심 되세요, $nameToShow! 레시피 공유도 잊지 마세요.",
            "레시피 아이디어가 샘솟는 점심시간이에요, $nameToShow!",
            "점심시간입니다, $nameToShow! 간단한 메뉴 하나 골라볼까요?"
        ),
        13 to listOf(
            "맛있게 식사하셨나요, $nameToShow? 이제 레시피를 정리해볼까요?",
            "점심의 여운을 담아 오늘의 요리를 기록해보세요.",
            "요리의 감동은 공유로 완성돼요, $nameToShow.",
            "레시피를 남기면 다음 식사도 더 쉬워집니다!",
            "좋은 요리는 기록할 만한 가치가 있답니다."
        ),
        14 to listOf(
            "오후입니다, $nameToShow. 다음 요리를 구상해볼까요?",
            "한가로운 오후, 레시피를 다듬어보는 건 어떠세요?",
            "이 시간은 요리 아이디어를 정리하기에 좋아요.",
            "$nameToShow 님의 요리 기록을 기다리고 있어요!",
            "간단한 간식도 멋진 레시피가 될 수 있어요."
        ),
        15 to listOf(
            "오후 3시입니다, $nameToShow. 티타임 레시피 생각나시나요?",
            "달콤한 간식, 그리고 레시피의 시간입니다.",
            "지금 떠오른 레시피, 적어두지 않으면 잊어버릴 수 있어요!",
            "$nameToShow 님의 창의력이 반짝일 시간이에요.",
            "좋은 레시피는 나눌수록 빛납니다."
        ),
        16 to listOf(
            "저녁 준비 시간입니다, $nameToShow. 무엇을 해드릴까요?",
            "레시피 아이디어 정리해두셨나요?",
            "지금은 요리 재료를 정리하기 좋은 시간이죠!",
            "$nameToShow 님, 특별한 저녁 메뉴를 구상해보세요.",
            "오늘의 마무리를 준비할 시간이에요!"
        ),
        17 to listOf(
            "저녁은 $nameToShow 님과 함께! 특별한 요리를 준비해 보세요.",
            "하루 중 가장 풍성한 식사, 어떤 메뉴로 채우실 건가요?",
            "$nameToShow 님, 레시피로 가족과의 시간을 풍성하게!",
            "따뜻한 저녁 레시피로 하루를 마무리하세요.",
            "저녁 시간, 정성 가득한 요리를 준비해 보세요."
        ),
        18 to listOf(
            "맛있는 저녁을 위한 레시피를 확인해보세요, $nameToShow!",
            "레시피는 사랑의 또 다른 이름이죠.",
            "$nameToShow 님의 요리가 오늘 하루를 완성시켜줄 거예요!",
            "정성스러운 저녁, 기록해 두셨나요?",
            "요리는 마음을 나누는 최고의 방법이에요."
        ),
        19 to listOf(
            "저녁시간입니다! 오늘의 요리를 공유해보세요, $nameToShow.",
            "레시피를 나누면 기쁨도 두 배랍니다.",
            "따뜻한 저녁, 정성 가득한 요리를 추천드려요.",
            "$nameToShow 님, 오늘은 어떤 요리를 해보셨나요?",
            "레시피는 하루의 감성을 담는 그릇입니다."
        ),
        20 to listOf(
            "20시입니다. 하루의 요리를 정리하기 좋은 시간이죠, $nameToShow!",
            "저장하지 않으면 잊히는 레시피, 기록해 두세요.",
            "오늘 만든 요리 중 베스트를 정리해볼까요?",
            "지금 이 순간, 당신의 레시피가 누군가에게 도움이 될 수 있어요.",
            "요리는 추억을 만드는 과정이에요, $nameToShow."
        ),
        21 to listOf(
            "밤입니다. 오늘의 요리를 되새겨보세요, $nameToShow.",
            "레시피는 오늘의 감정을 담는 일기예요.",
            "$nameToShow 님, 내일의 요리를 위해 오늘을 기록하세요.",
            "조용한 밤, 나만의 요리를 되새겨보는 시간이에요.",
            "오늘의 레시피가 내일의 누군가에게 영감이 될 수 있어요!"
        ),
        22 to listOf(
            "22시입니다. 하루의 레시피를 정리할 시간이에요, $nameToShow.",
            "늦은 밤, 나만의 요리를 되돌아보는 건 어떠세요?",
            "지금 저장한 레시피가 내일의 인기 요리가 될 수도 있어요.",
            "$nameToShow 님, 오늘의 요리를 나눠보세요.",
            "레시피는 마음의 기록입니다."
        ),
        23 to listOf(
            "23시입니다, $nameToShow. 오늘 하루의 레시피를 정리해 보세요.",
            "하루의 마무리엔 따뜻한 한마디, $nameToShow. 수고하셨어요!",
            "늦은 밤, 나만의 레시피로 마음을 달래보세요, $nameToShow!",
            "$nameToShow 님, 오늘 저장한 레시피가 있나요?",
            "레시피는 기억보다 기록입니다, $nameToShow!"
        )
    )

    val messageList = messagesByHour[hour] ?: listOf("안녕하세요, $nameToShow! 맛있는 하루 보내세요.")
    return messageList.random()
}
fun getGreetingMessage(userName: String?): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH는 0부터 시작하므로 +1
    val nameToShow = userName ?: "손님"

    // 계절별 문구
    val seasonMessage = when (month) {
        in 3..5 -> listOf(
            "따스한 봄바람이 부는 하루예요.",
            "꽃향기 가득한 봄입니다, $nameToShow!",
            "봄날엔 새로운 레시피를 시도해보세요.",
            "싱그러운 봄처럼 상큼한 요리를 준비해볼까요?",
            "포근한 봄날엔 따뜻한 레시피가 어울려요."
        )
        in 6..8 -> listOf(
            "무더운 여름, 시원한 레시피를 추천드려요!",
            "여름엔 가벼운 한끼가 좋아요, $nameToShow.",
            "햇살 가득한 여름날엔 간단한 요리가 최고죠!",
            "$nameToShow 님, 여름을 담은 레시피를 기록해보세요.",
            "청량한 여름, 요리로 기운을 내보세요!"
        )
        in 9..11 -> listOf(
            "가을입니다. 풍성한 식탁이 기다리고 있어요.",
            "가을 바람처럼 깊은 맛을 담은 요리를 해볼까요?",
            "$nameToShow 님, 따뜻한 요리가 생각나는 계절이에요.",
            "알록달록 가을처럼 다채로운 레시피를 만나보세요.",
            "가을은 요리하기 좋은 계절이에요!"
        )
        else -> listOf(
            "겨울입니다. 따뜻한 레시피가 어울리는 계절이에요.",
            "$nameToShow 님, 오늘은 어떤 따뜻한 요리를 하실 건가요?",
            "포근한 요리로 추운 날씨를 녹여보세요.",
            "겨울엔 뜨끈한 요리 한 그릇이 딱이죠!",
            "따뜻한 레시피로 마음을 녹여보세요, $nameToShow."
        )
    }.random()

    // 시간대별 메시지 – 기존 getGreetingMessage(userName)와 동일하게 사용
    val timeMessage = getHourGreetingMessage(userName)

    return "$seasonMessage \n $timeMessage"
}

// --- 메인 화면 컴포저블 ---
// 소요시간 태그를 분 단위로 변환하는 _헬퍼 함수_ (HomeContent 내부 또는 파일 상단에 위치 가능)
private fun parsePrepTimeTagToMinutes(timeTag: String): Int? {
    val tagValue = timeTag.substringAfter("소요시간:", "").trim()
    return when {
        tagValue.endsWith("분") -> tagValue.removeSuffix("분").toIntOrNull()
        tagValue.endsWith("시간") -> {
            val hours = tagValue.removeSuffix("시간").toIntOrNull()
            hours?.let { it * 60 }
        }
        else -> tagValue.toIntOrNull() // 숫자만 있는 경우 (분으로 가정)
    }
}

/**
 * 메인 홈 화면의 TopAppBar 컴포저블.
 * @param user 현재 사용자 정보.
 * @param onProfileClick 프로필 이미지 클릭 시 호출될 람다.
 * @param onChallengeClick 챌린지 아이콘 클릭 시 호출될 람다.
 */

//  인사말 생성 함수
fun getGreetingText(name: String?): Pair<String, String> {
    val safeName = name ?: "사용자"
    val title = "안녕하세요, $safeName 님!"
    val subtitle = "맛있는 하루 되세요."
    return Pair(title, subtitle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    user: User?,
    isScrolled: Boolean
) {
    val userPoint = user?.point ?: 0
    val (greetingTitle, greetingSub) = getGreetingText(user?.name)
    val elevation by animateDpAsState(
        targetValue = if (isScrolled) 4.dp else 0.dp,
        label = "topBarElevation"
    )

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // 프로필 이미지
                AsyncImage(
                    model = user?.image ?: "",
                    contentDescription = "User Profile Image",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_profile_placeholder),
                    error = rememberVectorPainter(Icons.Filled.AccountCircle)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 인사말 수직 정렬
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    TypingAnimatedText(
                        text = greetingTitle, // ex: "안녕하세요, 홍길동 님!"
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = greetingSub, // ex: "맛있는 하루 되세요."
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            // 포인트 표시
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Grain,
                    contentDescription = "포인트",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$userPoint",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }


        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
    )
}
// 탑바 wrapper 함수
@Composable
fun AppTopBar(
    selectedTab: Int,
    screens: List<Screen>,
    user: User?,
    isScrolled: Boolean
) {
    when (screens[selectedTab]) {
        Screen.Home, Screen.Subscribe, Screen.MyPage -> {
            HomeTopBar(
                user = user,
                isScrolled = isScrolled
            )
        }
        else -> {}
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val user by viewModel.user.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadFailed by viewModel.loadFailed.collectAsState()
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val screens = listOf(
        Screen.Home, Screen.Subscribe, Screen.Social, Screen.RecipeStorage, Screen.MyPage
    )
    var selectedTab by remember { mutableStateOf(0) }

    val firestore = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "defaultUserId"
    val subscribeViewModel: SubscribeViewModel = viewModel(
        factory = SubscribeViewModelFactory(firestore, userId)
    )

    val aiChatViewModel = remember { AiChatViewModel() }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.initializeRecommendationSystem()
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                    showBottomSheet = false
                }
            },
            sheetState = sheetState
        ) {
            AiChatBox1(viewModel = aiChatViewModel)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                selectedTab = selectedTab,
                screens = screens,
                user = user,
                isScrolled = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                screens = screens,
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    navController.navigate(screens[index].route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentRoute != "chat/{uid}") {
                FloatingActionButton(onClick = {
                    showBottomSheet = true
                }) {
                    Icon(Icons.Filled.Chat, contentDescription = "Open AI Chat")
         }
            }
        },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(
                route = "channel_management/{channelName}",
                arguments = listOf(navArgument("channelName") { defaultValue = "DefaultChannel" })
            ) { backStackEntry ->
                val channelName = backStackEntry.arguments?.getString("channelName") ?: "DefaultChannel"
                ChannelManagementScreen(channelName)
            }
            composable(
                route ="ranklist"
            ){
                RankScreenImproved(navController)
            }
            composable(
                route = "profile/{uid}",
                arguments = listOf(navArgument("uid") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                UserProfileScreen(
                    onBack    = { navController.popBackStack() },
                    targetUid = uid
                )
            }
            composable(
                route = "health/{uid}",
                arguments = listOf(navArgument("uid") { type = NavType.StringType })
            ) { backStackEntry ->
                val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                val healthConnectViewModel: HealthConnectViewModel = viewModel()
                val homeViewModel: HomeViewModel = viewModel()
                HealthConnectScreen(
                    viewModel = healthConnectViewModel,
                    homeViewModel = homeViewModel
                )
            }

            composable(
                route = "chat/{uid}",
                arguments = listOf(
                    navArgument("uid") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val targetUid = backStackEntry.arguments?.getString("uid") ?: return@composable
                DetailedChatScreen(
                    navController = navController,
                    targetUid = targetUid,
                    homeViewModel = viewModel

                )
            }
            composable("challenge") {
                val challengeViewModel: ChallengeViewModel = viewModel()
                ChallengeScreen(viewModel = challengeViewModel)
            }
            // 3) 맛집도 탭 — RestaurantV2MapScreen
            composable("matzip") { backStackEntry ->
                val matzipViewModel: MatzipViewModel = viewModel(backStackEntry)
                RestaurantV2MapScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = matzipViewModel
                )
            }
            composable(Screen.Home.route) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    when {
                        isLoading -> LoadingState()
                        loadFailed -> ErrorState(onRetry = { viewModel.loadRecipes() })
                        recipes.isEmpty() && !isLoading -> EmptyState()
                        else -> HomeContent(
                            paddingValues = paddingValues,
                            recipes = recipes,
                            user = user,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            focusManager = focusManager,
                            context = context,
                            homeViewModel = viewModel,
                            listState = listState
                        )
                    }
                }
            }

            composable("gameMenu") { MiniGameMenu(navController) }
            composable("rouletteGame") { RouletteGameScreen(navController) }
            composable("cardGame") { CardGameScreen(navController) }
            composable("ladderGame") { LadderGameScreen(navController) }
            composable("payerRouletteGame") { PayerRouletteGameScreen(navController) }

            composable("community") {
                CommunityTab(
                    navController = navController,
                    navToWrite = { navController.navigate("write") },
                    navToDetail = { post -> navController.navigate("postDetail/${post.id}") }
                )
            }
            composable("write") {
                WritePostScreen(
                    navController = navController,
                    onPostCreated = { navController.popBackStack() }
                )
            }
            composable("postDetail/{postId}") { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                PostDetailScreen(postId = postId, navController = navController)
            }
            composable("channelView/{channelName}") { backStackEntry ->
                val channelName = backStackEntry.arguments?.getString("channelName") ?: return@composable
                ChannelViewPageScreen(channelName = channelName, navController = navController)
            }
            composable("write/{channelName}") { backStackEntry ->
                val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
                WriteScreen(
                    channelName = channelName,
                    onSuccess = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit/{recipeId}/{channelName}",
                arguments = listOf(
                    navArgument("recipeId") { type = NavType.StringType },
                    navArgument("channelName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val recipeId    = backStackEntry.arguments?.getString("recipeId") ?: ""
                val channelName = backStackEntry.arguments?.getString("channelName") ?: ""

                FoodTableTheme {
                    EditRecipeScreen(
                        recipeId        = recipeId,
                        channelName     = channelName,
                        onModifySuccess = {
                            // 수정 완료 → 이전 화면으로
                            navController.popBackStack()
                        },
                        onDeleteSuccess = {
                            // 삭제 완료 → 홈 화면으로
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
            // FridgeScreen
            composable("fridge") {
                val fridgeViewModel: FridgeViewModel = viewModel()
                FridgeScreen(
                    viewModel     = fridgeViewModel,
                    navController = navController    // 전역 컨트롤러
                )
            }

            // AI 추천 레시피 화면
            composable(
                route = "ai_recipe?name={name}",
                arguments = listOf(navArgument("name") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { backStackEntry ->
                val recipeName = backStackEntry.arguments?.getString("name") ?: ""
                val recipeItem = recipes.firstOrNull { it.name == recipeName }
                    ?: RecipeItem(name=recipeName, description="", ingredients=emptyList(), order="")
                AiRecipeScreen(
                    recipe      = recipeItem,
                    navController = navController,
                    onSaveToChannel = { /* 저장 로직 */ }
                )
            }

            composable(Screen.Subscribe.route) {
                SubscribeScreen(viewModel = subscribeViewModel, navController = navController)
            }
            composable(Screen.Social.route) {
                SocialScreen(navController = navController)
            }
            composable(Screen.RecipeStorage.route) {
                MyRecipeStorageScreen()
            }
            composable(Screen.MyPage.route) {
                ProfileMainScreen(paddingValues = paddingValues, navController = navController)
            }
        }
    }
}
@Composable
fun AppBottomNavigationBar(
    screens: List<Screen>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 0.dp
    ) {
        screens.forEachIndexed { index, screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.label,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            )
        }
    }
}

/**
 * 검색 바 컴포저블.
 * @param searchQuery 현재 검색어 TextFieldValue.
 * @param onSearchQueryChange 검색어 변경 시 호출될 람다.
 * @param modifier Modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSearchBar(
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { Text("레시피와 재료를 검색해주세요.") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search Icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        singleLine = true
    )
}

/**
 * 스마트 필터 바 컴포저블.
 * @param availableFilters 표시할 대표 필터 목록
 * @param selectedMainFilter 현재 선택된 대표 필터 (UI 강조용)
 * @param onMainFilterSelected 대표 필터 선택/해제 시 호출 (확장/축소 로직 포함)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFilterBar(
    availableFilters: List<String>,
    selectedMainFilter: String?,
    onMainFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableFilters) { filterOption ->
            CategoryChip(
                category = filterOption,
                selected = selectedMainFilter == filterOption,
                onSelected = { onMainFilterSelected(filterOption) }
            )
        }
    }
}

/**
 * 카테고리 칩들을 표시하는 섹션 컴포저블 (가로 스크롤).
 * @param recipes 레시피 목록 (고유 카테고리를 추출하기 위해 사용).
 * @param selectedCategory 현재 선택된 카테고리.
 * @param onSelectedCategoryChange 카테고리 선택 변경 시 호출될 람다.
 * @param modifier Modifier.
 */
@Composable
fun CategoryChipsSection(
    recipes: List<RecipeItem>,
    selectedCategory: String?,
    onSelectedCategoryChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // 난이도 옵션 목록 ( 소요시간에 난이도 값 뺴려고 추가)
    val difficultyList = listOf("쉬움", "보통", "어려움")
    val uniqueCategories = remember(recipes) {
        recipes
            .mapNotNull { it.C_categories.getOrNull(0) }   // 0번 인덱스만
            .filter { it !in difficultyList }              // 난이도값은 제외
            .distinct()
            .sorted()
    }

    // 디버깅을 위해 로그 추가 (실제 앱에서는 개발 중에만 사용하거나 필요시 제거)
    LaunchedEffect(recipes, uniqueCategories) {
        Log.d("CategoryChipsDebug", "Recipes count in CategoryChipsSection: ${recipes.size}")
        Log.d("CategoryChipsDebug", "Unique categories for chips: $uniqueCategories")
    }

    if (recipes.isEmpty()) {
        // 레시피 리스트 자체가 비어있을 경우 (로딩 중이거나 데이터가 없을 때)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip( // "All" 칩은 항상 표시 시도
                category = "All",
                selected = selectedCategory == null,
                onSelected = { onSelectedCategoryChange(null) }
            )
            Text(
                text = "(레시피 로딩 중이거나 표시할 레시피가 없습니다)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    } else {
        // 레시피 리스트가 비어있지 않은 경우
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryChip(
                    category = "All",
                    selected = selectedCategory == null,
                    onSelected = { onSelectedCategoryChange(null) }
                )
            }

            if (uniqueCategories.isEmpty()) {
                // 레시피는 있으나, C_categories에서 추출할 고유 카테고리가 없는 경우
                item {
                    Text(
                        text = "(추가 카테고리 없음)",
                        style = MaterialTheme.typography.bodySmall,
                        // LazyRow의 item 내부에서 Row의 CenterVertically 효과를 내기 위해 Modifier.align 사용 불가
                        // 필요하다면 Chip과 유사한 높이를 갖도록 패딩 조정
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp) // 칩과 유사한 패딩
                    )
                }
            } else {
                items(uniqueCategories) { category ->
                    CategoryChip(
                        category = category,
                        selected = selectedCategory == category,
                        onSelected = {
                            // 같은 옵션 재클릭 시 선택 해제, 다른 옵션 클릭 시 변경
                            onSelectedCategoryChange(
                                if (selectedCategory == category) null
                                else category
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * 🤖 AI 기반 시간대별 추천 레시피 섹션 (완전히 새로운 구현!)
 * GPT API를 활용하여 실시간으로 시간대에 맞는 메인/서브/디저트 추천
 */
@Composable
fun AITimeBasedRecommendationSection(
    homeViewModel: HomeViewModel,
    context: Context,
    modifier: Modifier = Modifier
) {
    // AI 추천 상태 관찰
    val aiRecommendation by homeViewModel.aiTimeRecommendation.collectAsState()
    val isLoading by homeViewModel.isRecommendationLoading.collectAsState()
    val currentGreeting by homeViewModel.currentTimeGreeting.collectAsState()

    // 화면이 처음 나타날 때 추천 시스템 초기화
//    LaunchedEffect(Unit) {
//        homeViewModel.initializeRecommendationSystem()
//    }

    Column(modifier = modifier.padding(bottom = 16.dp)) {
        // 🌟 섹션 헤더 (시간대별 맞춤 인사말 포함)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🤖 AI 추천 메뉴",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                if (currentGreeting.isNotBlank()) {
                    Text(
                        text = currentGreeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 새로고침 버튼
            IconButton(
                onClick = {
                    homeViewModel.viewModelScope.launch {
                        homeViewModel.updateTimeBasedRecommendation()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "추천 새로고침",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = if (isLoading) Modifier.graphicsLayer { rotationZ = 360f } else Modifier
                )
            }
        }

        if (isLoading) {
            // 🔄 로딩 상태
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "AI가 맞춤 메뉴를 준비하고 있어요...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (aiRecommendation != null) {
            // 🍽️ AI 추천 메뉴 카드들
            AIRecommendationCards(
                recommendation = aiRecommendation!!,
                context = context,
                homeViewModel = homeViewModel
            )
        } else {
            // ⚠️ 추천 없음 상태
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = "추천 없음",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "AI 추천을 불러올 수 없어요",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "잠시 후 다시 시도해주세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp))
    }
}

/**
 * 🍽️ AI 추천 메뉴를 표시하는 카드들 (메인, 서브, 디저트)
 */
@Composable
fun AIRecommendationCards(
    recommendation: AIRecommendationService.TimeBasedRecommendation,
    context: Context,
    homeViewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 추천 메시지
        if (recommendation.timeMessage.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = recommendation.timeMessage,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 메뉴 카드들 (가로 스크롤)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            item {
                MenuRecommendationCard(
                    title = "🍽️ 메인 메뉴",
                    menuName = recommendation.mainDish,
                    description = "오늘의 메인 요리",
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    onCardClick = {
                        // 🔍 사용자 행동 추적 추가!
                        homeViewModel.trackRecipeView("ai_main_${recommendation.mainDish}", listOf("메인", "AI추천"))
                    }
                )
            }

            item {
                MenuRecommendationCard(
                    title = "🥗 서브 메뉴",
                    menuName = recommendation.subDish,
                    description = "메인과 함께",
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    onCardClick = {
                        homeViewModel.trackRecipeView("ai_sub_${recommendation.subDish}", listOf("서브", "AI추천"))
                    }
                )
            }

            item {
                MenuRecommendationCard(
                    title = "🍰 디저트",
                    menuName = recommendation.dessert,
                    description = "달콤한 마무리",
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onCardClick = {
                        homeViewModel.trackRecipeView("ai_dessert_${recommendation.dessert}", listOf("디저트", "AI추천"))
                    }
                )
            }
        }

        // 추천 이유
        if (recommendation.recommendationReason.isNotBlank()) {
            Text(
                text = "💡 ${recommendation.recommendationReason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontStyle = FontStyle.Italic
            )
        }
    }
}

/**
 * 🍽️ 개별 메뉴 추천 카드
 */
@Composable
fun MenuRecommendationCard(
    title: String,
    menuName: String,
    description: String,
    backgroundColor: Color,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotation by remember { mutableStateOf(0f) }
    // State to hold the currently displayed content on the card.
    var cardContent by remember { mutableStateOf(Triple(title, menuName, description)) }

    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "flipRotation"
    )

    // When the real data (menuName) changes, trigger the flip animation.
    LaunchedEffect(menuName) {
        // Only flip if the content is actually different.
        if (cardContent.second != menuName) {
            rotation += 180f
            // Wait for the card to be edge-on (halfway through animation).
            delay(300) // This should be half of the tween's duration.
            // Swap the content to the new data.
            cardContent = Triple(title, menuName, description)
        }
    }

    val isCardFlipped = (animatedRotation % 360) > 90f && (animatedRotation % 360) < 270f

    Card(
        modifier = modifier
            .width(160.dp)
            .height(120.dp)
            .graphicsLayer {
                this.rotationY = animatedRotation
                cameraDistance = 8 * density
            }
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .graphicsLayer {
                    // When the card is flipped, we need to un-flip the content
                    // so it doesn't appear mirrored.
                    if (isCardFlipped) {
                        rotationY = 180f
                    }
                },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Display the content from our state holder.
            val (displayTitle, displayMenuName, displayDescription) = cardContent

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Text(
                text = displayMenuName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = displayDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 👤 사용자 행동 기반 맞춤 추천 레시피 섹션 (완전히 새로운 구현!)
 * 실제 사용자 조회 패턴을 분석하여 AI가 개인화된 추천 제공
 */
@Composable
fun SmartPersonalizedRecommendationSection(
    homeViewModel: HomeViewModel,
    user: User?,
    context: Context,
    modifier: Modifier = Modifier
) {
    // 맞춤 추천 상태 관찰
    val personalizedRecommendation by homeViewModel.personalizedRecommendation.collectAsState()
    val userPreferences by homeViewModel.userPreferences.collectAsState()
    val categoryViewCounts by homeViewModel.categoryViewCounts.collectAsState()

    val userName = user?.name ?: "회원"
    val sectionTitle = "✨ ${userName}님 맞춤 AI 추천"

    Column(modifier = modifier.padding(bottom = 16.dp)) {
        // 📊 사용자 분석 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // 사용자 선호도 표시
                if (userPreferences.isNotEmpty()) {
                    val preferencesText = "선호: ${userPreferences.take(3).joinToString(", ")}"
                    Text(
                        text = preferencesText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = "더 많은 레시피를 보고 맞춤 추천을 받아보세요!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 맞춤 추천 업데이트 버튼
            IconButton(
                onClick = {
                    homeViewModel.viewModelScope.launch {
                        homeViewModel.updatePersonalizedRecommendation()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "맞춤 추천 업데이트",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 🔍 사용자 행동 분석 표시 (선택적)
        if (categoryViewCounts.isNotEmpty()) {
            UserBehaviorAnalysisChips(
                categoryViewCounts = categoryViewCounts,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 맞춤 추천 메뉴 표시
        if (personalizedRecommendation != null) {
            AIRecommendationCards(
                recommendation = personalizedRecommendation!!,
                context = context,
                homeViewModel = homeViewModel
            )
        } else {
            // 맞춤 추천 로딩 또는 기본 상태
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Category,
                        contentDescription = "맞춤 추천 준비중",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "맞춤 추천 준비중",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "더 많은 레시피를 살펴보시면 더 정확한 추천을 받을 수 있어요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp))
    }
}

/**
 * 📊 사용자 행동 분석을 보여주는 칩들
 */
@Composable
fun UserBehaviorAnalysisChips(
    categoryViewCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val topCategories = categoryViewCounts
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    if (topCategories.isNotEmpty()) {
        LazyRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(topCategories) { (category, count) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 홈 화면의 실제 콘텐츠를 정의하는 컴포저블.
 * 검색 바, 카테고리 칩 섹션, 레시피 목록 등을 포함합니다.
 * @param paddingValues Scaffold로부터 제공되는 패딩 값.
 * @param recipes 표시할 레시피 목록.
 * @param user 현재 사용자 정보.
 * @param searchQuery 현재 검색어.
 * @param selectedCategory 현재 선택된 카테고리.
 * @param onSearchQueryChange 검색어 변경 시 호출될 람다.
 * @param onSelectedCategoryChange 카테고리 선택 변경 시 호출될 람다.
 * @param focusManager 포커스 관리를 위한 FocusManager.
 * @param context 컨텍스트.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class) // ExperimentalComposeUiApi는 이미 파일 상단에 OptIn 되어 있을 수 있습니다.
@Composable
private fun HomeContent(
    paddingValues: PaddingValues,
    recipes: List<RecipeItem>,
    user: User?,
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    focusManager: FocusManager,
    context: Context,
    homeViewModel: HomeViewModel,
    listState: LazyListState
) {
    // 필터 상태 관리
    val mainFilterOptions = listOf("요리 종류", "난이도", "소요 시간")
    var expandedFilterType by remember { mutableStateOf<String?>(null) }

    // 상세 선택 상태
    var selectedCuisine by remember { mutableStateOf<String?>(null) }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var selectedPrepTime by remember { mutableStateOf<String?>(null) }

    val uniqueCuisines = remember(recipes) {
        recipes.flatMap { it.C_categories }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val difficultyOptions = listOf("쉬움", "보통", "어려움")
    val prepTimeOptions = listOf("15분 이내", "30분 이내", "1시간 이내", "1시간 이상")
    //데이터베이스 불러오기
    val db = FirebaseFirestore.getInstance()
    val uid = UserManager.getUser()?.uid

    // 예상 칼로리 불러오기
    val calorieVm: RecipeCalorieViewModel = viewModel()
    // 1) 로컬에 구매한 레시피 ID 모아두는 상태
    var purchasedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 2) 화면 뜰 때 한 번만 snapshotListener 걸어서 실시간 업데이트
    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        db.collection("user")
            .document(uid)
            .collection("purchased")
            .addSnapshotListener { snap, err ->
                if (snap != null) {
                    purchasedIds = snap.documents.map { it.id }.toSet()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = paddingValues.calculateTopPadding() + 8.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 추천 레시피 섹션
        item { AITimeBasedRecommendationSection(homeViewModel = homeViewModel, context = context) }

        // 2. 검색창 (위치 변경됨)
        item { ModernSearchBar(searchQuery = searchQuery, onSearchQueryChange = onSearchQueryChange) }

        // 3. 스마트 필터 바
        item {
            SmartFilterBar(
                availableFilters = mainFilterOptions,
                selectedMainFilter = expandedFilterType,
                onMainFilterSelected = { filterType ->
                    // 현재 확장된 필터와 다른 필터를 선택하면, 이전 상세 선택 초기화
                    if (expandedFilterType != filterType) {
                        when (expandedFilterType) { // 이전 확장 타입에 따라 초기화
                            "요리 종류" -> selectedCuisine = null
                            "난이도" -> selectedDifficulty = null
                            "소요 시간" -> selectedPrepTime = null
                        }
                    }
                    expandedFilterType = if (expandedFilterType == filterType) null else filterType

                    // 새로 확장되는 필터가 아닌 다른 필터들의 상세 선택값 초기화
                    if (expandedFilterType != "요리 종류") selectedCuisine = null
                    if (expandedFilterType != "난이도") selectedDifficulty = null
                    if (expandedFilterType != "소요 시간") selectedPrepTime = null
                }
            )
        }

        // 4. 상세 필터 섹션 (요리 종류)
        item {
            AnimatedVisibility(
                visible = expandedFilterType == "요리 종류",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                // CategoryChipsSection 은 uniqueCategories 내부에서
                            // 0번 인덱스 요리종류만 꺼내도록 이미 구현되어 있습니다.
                CategoryChipsSection(
                    recipes = recipes,
                    selectedCategory = selectedCuisine,
                    onSelectedCategoryChange = { selectedCuisine = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 5. 상세 필터 섹션 (난이도)
        item {
            AnimatedVisibility(
                visible = expandedFilterType == "난이도",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                DetailFilterChipsSection(
                    title = "난이도",
                    options = difficultyOptions,
                    selectedOption = selectedDifficulty,
                    onOptionSelected = { selectedDifficulty = it }
                )
            }
        }

        // 6. 상세 필터 섹션 (소요 시간)
        item {
            AnimatedVisibility(
                visible = expandedFilterType == "소요 시간",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                DetailFilterChipsSection(
                    title = "소요 시간",
                    options = prepTimeOptions,
                    selectedOption = selectedPrepTime,
                    onOptionSelected = { selectedPrepTime = it }
                )
            }
        }

        // 7. 맞춤 추천 섹션
        item { SmartPersonalizedRecommendationSection(homeViewModel = homeViewModel, user = user, context = context) }

        // 8. 필터링된 레시피 목록 또는 "결과 없음" 메시지
        val filteredRecipes = recipes.filter { recipe ->
            // 1) 검색어 매칭 (기존 코드 그대로)
            val matchesSearch = searchQuery.text.isEmpty() ||
                    recipe.name.contains(searchQuery.text, ignoreCase = true) ||
                    recipe.description.contains(searchQuery.text, ignoreCase = true)

            // 2) 요리 종류 매칭 (기존대로, C_categories 리스트 중 '요리 종류'가 포함돼 있으면 OK)
            val matchesCuisine = selectedCuisine == null ||
                    recipe.C_categories.getOrNull(0) == selectedCuisine


            // 3) 난이도 매칭
            //    — C_categories 리스트의 1번 인덱스에 "쉬움"/"보통"/"어려움"이 들어 있다고 가정
            val difficultyFromData = recipe.C_categories.getOrNull(1)
            val matchesDifficulty = selectedDifficulty == null ||
                    difficultyFromData == selectedDifficulty

            // 4) 소요시간 매칭
            //    — duration 필드가 Int(분)으로 들어 있다고 가정
            val d = recipe.duration
            val matchesPrepTime = selectedPrepTime == null || when (selectedPrepTime) {
                "15분 이내"  -> d <= 15
                "30분 이내"  -> d <= 30
                "1시간 이내" -> d <= 60
                "1시간 이상" -> d > 60
                else         -> true
            }

            matchesSearch && matchesCuisine && matchesDifficulty && matchesPrepTime
        }


        if (filteredRecipes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth() // LazyColumn의 width를 채우도록 수정
                        .padding(vertical = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "조건에 맞는 레시피가 없어요.\n검색어나 필터를 조정해보세요!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(
                items = filteredRecipes,
                key = { recipe -> recipe.id.ifBlank { recipe.name + recipe.hashCode() } }, // 고유 키 보장
            ) { recipe ->

                var isVisible by remember { mutableStateOf(false)
                }
                // 화면 전용으로 구매여부 계산
                val isPurchasedFlag = purchasedIds.contains(recipe.id)
                LaunchedEffect(key1 = recipe.id) {
                    isVisible = true
                }

                // isVisible 상태에 따라 애니메이션을 적용합니다.
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 500, delayMillis = 50)
                    ) + fadeIn(animationSpec = tween(400)),
                ) { // <-- 여는 중괄호

                    // --- ModernRecipeCard와 모든 관련 로직이 이 안으로 들어옵니다 ---
                    val cardModifier = Modifier.animateItemPlacement(tween(durationMillis = 300))
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val db = FirebaseFirestore.getInstance()

                    // recipe.id, recipe.ingredients, recipe.order 가 바뀔 때마다 재추정
                    LaunchedEffect(recipe.id, recipe.ingredients, recipe.order) {
                        calorieVm.loadOrEstimateCalories(recipe)
                    }

                    var showPurchaseDialog by remember { mutableStateOf(false) }
                    var selectedRecipe by remember { mutableStateOf<RecipeItem?>(null) }
                    val estimatedCal = calorieVm.caloriesMap[recipe.id]

                    ModernRecipeCard(
                        recipe = recipe.copy(isPurchased = isPurchasedFlag),
                        estimatedCal = estimatedCal,
                        onCardClick  = {
                            scope.launch {
                                val uid = UserManager.getUser()!!.uid

                                db.collection("user")
                                    .document(uid)
                                    .collection("purchased")
                                    .document(recipe.id)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            homeViewModel.trackRecipeView(recipe.id, recipe.C_categories)
                                            val intent = Intent(context, RecipeCookingActivity::class.java)
                                            intent.putExtra("recipe_id", recipe.id)
                                            context.startActivity(intent)
                                        } else {
                                            selectedRecipe = recipe
                                            showPurchaseDialog = true
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "구매 여부 확인 실패", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        modifier = cardModifier
                    )

                    if (showPurchaseDialog && selectedRecipe != null) {
                        PurchaseDialog(
                            recipeName = selectedRecipe!!.name,
                            cost = selectedRecipe!!.cost,
                            onConfirm = {
                                showPurchaseDialog = false
                                // ... 기존 구매 확정 로직 ...
                                val uid = UserManager.getUser()!!.uid
                                val userRef = db.collection("user").document(uid)

                                userRef.get().addOnSuccessListener { document ->
                                    val currentPoint = document.getLong("point")?.toInt() ?: 0
                                    if (currentPoint >= selectedRecipe!!.cost) {
                                        val newPoint = currentPoint - selectedRecipe!!.cost
                                        userRef.update("point", newPoint)
                                            .addOnSuccessListener {
                                                userRef.collection("purchased")
                                                    .document(selectedRecipe!!.id)
                                                    .set(mapOf("purchased" to true))
                                                    .addOnSuccessListener {
                                                        Toast.makeText(context, "구매 완료! 🎉", Toast.LENGTH_SHORT).show()
                                                        homeViewModel.markRecipeAsPurchased(selectedRecipe!!.id)
                                                        val intent = Intent(context, RecipeCookingActivity::class.java)
                                                        intent.putExtra("recipe_id", selectedRecipe!!.id)
                                                        context.startActivity(intent)
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(context, "구매 처리 실패", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "포인트 차감 실패", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "소금이 부족합니다! (${currentPoint} / ${selectedRecipe!!.cost})",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }.addOnFailureListener {
                                    Toast.makeText(context, "사용자 정보 조회 실패", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDismiss = {
                                showPurchaseDialog = false
                            }
                        )
                    }
                }
            }
        }

                @Composable
                fun PurchaseDialog(
                    recipeName: String,
                    cost: Int,
                    onConfirm: () -> Unit,
                    onDismiss: () -> Unit
                ) {
                    AlertDialog(
                        onDismissRequest = onDismiss,
                        title = { Text(text = "레시피 구매") },
                        text = {
                            Text("레시피 \"$recipeName\"을 ${cost} 소금을 사용해 구매하시겠습니까?")
                        },
                        confirmButton = {
                            TextButton(onClick = onConfirm) {
                                Text("구매하기")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = onDismiss) {
                                Text("취소")
                            }
                        }
                    )
                }

            }
        }


@Composable
fun PurchaseDialog(
    recipeName: String,
    cost: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "레시피 구매") },
        text = {
            Text(text = "\"$recipeName\" 레시피를 ${cost} 소금을 사용하여 구매하시겠습니까?")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("구매하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// --- 최상위 화면 컴포저블 ---

/**
 * 앱의 메인 홈 화면.
 * Scaffold를 사용하여 TopBar, BottomBar, 그리고 화면 콘텐츠를 구성합니다.
 * DependencyProvider를 통한 깔끔한 수동 DI 방식을 사용합니다.
 */


// --- 플레이스홀더 화면 컴포저블 ---

/**
 * 레시피 저장소 화면의 플레이스홀더 컴포저블.
 * 실제 내용은 여기에 구현되어야 합니다.
 * @param paddingValues Scaffold로부터 제공되는 패딩 값.
 */
@Composable
fun RecipeStorageScreenContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Text("Recipe Storage Screen", style = MaterialTheme.typography.headlineMedium)
    }
}

// CategoryChip은 SmartFilterBar에서 사용될 수 있으므로 그대로 두거나, 카드 내부용으로 별도 제작 가능
// 여기서는 카드 내부에 간단한 텍스트 태그를 사용합니다.
@Composable
fun SimpleCategoryTag(categoryName: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(end = 6.dp, bottom = 6.dp), // 칩 간 간격
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailFilterChipsSection(
    title: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = "${title} 상세 선택:",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { index, option ->
                var isVisible by remember { mutableStateOf(false) }

                LaunchedEffect(key1 = Unit) {
                    isVisible = true
                }
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = index * 50)) +
                            scaleIn(initialScale = 0.8f, animationSpec = tween(durationMillis = 300, delayMillis = index * 50))
                ) {
                    CategoryChip(
                        category = option,
                        selected = selectedOption == option,
                        onSelected = {
                            // 같은 옵션 재클릭 시 선택 해제, 다른 옵션 클릭 시 변경
                            onOptionSelected(if (selectedOption == option) null else option)
                        }
                    )
                }
            }
        }
    }
}

// --- MyRecipeStorageScreen 방식의 로딩/에러 상태 컴포넌트들 ---

private fun Modifier.shimmerBackground(shape: Shape = RoundedCornerShape(4.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = -200f,
        targetValue = 1800f, // 화면 너비를 커버할 수 있는 충분히 큰 값
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-translate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp).copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = 0.6f),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim.value - 1000f, 0f),
            end = Offset(translateAnim.value, 0f)
        ),
        shape = shape
    )
}


@Composable
fun ShimmerRecipeCardPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .shimmerBackground(RoundedCornerShape(24.dp))
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false
    ) {
        // AI 추천 섹션 플레이스홀더
        item {
            Column {
                Spacer(modifier = Modifier
                    .height(24.dp)
                    .width(180.dp)
                    .shimmerBackground())
                Spacer(modifier = Modifier.height(8.dp))
                Spacer(modifier = Modifier
                    .height(16.dp)
                    .width(220.dp)
                    .shimmerBackground())
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) {
                        Spacer(modifier = Modifier.size(width = 160.dp, height = 120.dp).shimmerBackground(RoundedCornerShape(16.dp)))
                    }
                }
            }
        }

        // 검색 및 필터 플레이스홀더
        item {
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shimmerBackground(RoundedCornerShape(28.dp)))
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Spacer(modifier = Modifier.size(width = 100.dp, height = 40.dp).shimmerBackground(RoundedCornerShape(20.dp)))
                }
            }
        }
        items(3) {
            ShimmerRecipeCardPlaceholder()
        }
    }
}

@Composable
fun ErrorState(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "Error Icon",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "이런, 문제가 발생했어요!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "데이터를 불러오는데 실패했습니다. 네트워크 연결을 확인하거나 잠시 후 다시 시도해주세요.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Retry Icon", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("다시 시도", color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.NoFood,
            contentDescription = "Empty Icon",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "텅 비었어요!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "저장된 레시피가 아직 없네요. 맛있는 첫 레시피를 추가해보세요!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

    }

}
