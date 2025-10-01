@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)


package com.bcu.foodtable.ui.home


import AddIngredientScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import AiRecipeScreen
import CategoriesViewModel
import ChannelEditScreen
import ChannelEditScreenLoader
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BrunchDining
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.NightShelter
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import androidx.navigation.navDeepLink
import coil.Coil
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bcu.foodtable.JetpackCompose.HomeChannelDatil.RecipeCookingScreen
import com.bcu.foodtable.JetpackCompose.Mypage.Setting.ShakeToOpenQR
import com.bcu.foodtable.JetpackCompose.RecipeStorage.CategoryScreen
import com.bcu.foodtable.JetpackCompose.RecipeStorage.TrendRecipeScreen
import com.bcu.foodtable.JetpackCompose.RecipeStorage.TrendRecipeViewModel
import com.bcu.foodtable.JetpackCompose.Social.Appointment.AppointmentDetailScreen
import com.bcu.foodtable.JetpackCompose.Social.Appointment.AppointmentHomeScreen
import com.bcu.foodtable.JetpackCompose.Social.Openchat.CreateOpenChatScreen
import com.bcu.foodtable.JetpackCompose.Social.Openchat.OpenChatHomeScreen
import com.bcu.foodtable.JetpackCompose.Social.Openchat.OpenChatRoomScreen
import com.bcu.foodtable.JetpackCompose.Social.Openchat.RecipeByIdScreen
import com.bcu.foodtable.JetpackCompose.Social.RestaurantMapMainScreen
import com.bcu.foodtable.JetpackCompose.Social.RestaurantMapWithCustomDrawer
import com.bcu.foodtable.JetpackCompose.coach.CoachScreen
import com.bcu.foodtable.JetpackCompose.coach.CoachScrimColor
import com.bcu.foodtable.JetpackCompose.coach.CoachStep
import com.bcu.foodtable.JetpackCompose.coach.CoachTargets
import com.bcu.foodtable.JetpackCompose.coach.CoachTour
import com.bcu.foodtable.JetpackCompose.coach.CoachmarkOverlay
import com.bcu.foodtable.JetpackCompose.coach.CoachmarkStoreDataStore
import com.bcu.foodtable.JetpackCompose.coach.InteractionBlocker
import com.bcu.foodtable.JetpackCompose.coach.coachTarget
import com.bcu.foodtable.RecipePurchaseDialogExact
import com.bcu.foodtable.ui.merchant.QrPayScannerScreen

import com.bcu.foodtable.useful.PromotionItem
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import kotlinx.coroutines.isActive
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.sql.Date

// --- 데이터 모델 및 유틸리티 컴포넌트 ---

/**
 * 텍스트에 타이핑 효과를 적용하는 Composable.
 * @param text 표시할 전체 텍스트.
 * @param typingDelay 글자 사이의 지연 시간 (밀리초).
 */

private const val COACH_SCRIM_ALPHA = 0.55f
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

sealed class Screen(val route: String, val label: String, val icon: Int) {
    object Home : Screen("home", "홈", R.drawable.ic_home_black_24dp)
    object Subscribe : Screen("subscribe", "채널", R.drawable.ic_notifications_black_24dp)
    object Social : Screen("social", "소설", R.drawable.ic_dashboard_black_24dp)
    object RecipeStorage : Screen("storage", "레시피 관리", R.drawable.baseline_menu_book_24)
    object MyPage : Screen("mypage", "마이페이지", R.drawable.ic_profile_placeholder)
}

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
                    imageVector = Icons.Filled.AcUnit,
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
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val screens = listOf(
        Screen.Home, Screen.RecipeStorage, Screen.Social, Screen.Subscribe, Screen.MyPage
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

    val coachStore = remember { CoachmarkStoreDataStore(context) }
    val targets = remember { CoachTargets() }
    var showCoach by remember { mutableStateOf(true) }
    val coachBringer = remember { BringIntoViewRequester() }

    var overlayActive by remember { mutableStateOf(false) }

    // 바텀바 높이 측정
    var bottomBarHeightDp by remember { mutableStateOf(0.dp) }

    val fallback = 80.dp
    val scrimHeight = remember(bottomBarHeightDp) {
        if (bottomBarHeightDp > 0.dp) bottomBarHeightDp else fallback
    }

    val interaction = remember { MutableInteractionSource() }

    fun routeKey(route: String?): String? = when {
        route == null -> null
        route.startsWith("channelView/") -> Screen.Subscribe.route   // 채널 상세는 채널 탭으로 귀속
        route.startsWith("profile/")     -> Screen.MyPage.route      // 프로필 파생 라우트는 마이페이지로 귀속
        else -> route
    }

    /** 현재 보여지는 라우트 key (파생 라우트 포함) */
    fun currentRouteKey(): String? = routeKey(navController.currentBackStackEntry?.destination?.route)

    /** 오버레이 on/off 신호를 안전하게 소비 */
    fun consumeOverlaySignal(fromRoute: String, active: Boolean) {
        val current = currentRouteKey()
        val fromKey = routeKey(fromRoute)
        val accept = (fromKey == current)

        Log.i(
            "COACH_OVERLAY",
            "consumeOverlaySignal from=$fromKey active=$active / current=$current accept=$accept"
        )

        if (accept) {
            overlayActive = active
        } else {
            // 다른(이미 떠난) 화면에서 온 신호면 무시
            Log.i("COACH_OVERLAY", "ignored overlay signal from $fromKey (current=$current)")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeRecommendationSystem()
        CoachTour.maybeStartOnce(navController, context, coachStore)
    }
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { controller, dest, args ->
            // 위에서 만든 유틸을 그대로 사용
            Log.i(
                "NAV_TRACE",
                "[destChanged] host=${controller.hashCode().toString(16)} " +
                        "graph=${controller.graph.id} dest=${dest.route ?: "id=${dest.id}"}"
            )
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    // 구독 탭 클릭 효과
    LaunchedEffect(currentRoute) {
        // ⬇️ QR 스캐너 화면일 땐 마이페이지 탭으로 고정 표시
        if (currentRoute == "qrPayScanner") {
            selectedTab = screens.indexOf(Screen.MyPage)
            return@LaunchedEffect
        }

        // 기존 라우트 → 탭 동기화 로직 그대로 두기
        when {
            currentRoute == Screen.Subscribe.route ||
                    (currentRoute?.startsWith("channelView/") == true) -> {
                selectedTab = screens.indexOf(Screen.Subscribe)
            }
            currentRoute == Screen.Home.route -> {
                selectedTab = screens.indexOf(Screen.Home)
            }
            currentRoute == Screen.Social.route -> {
                selectedTab = screens.indexOf(Screen.Social)
            }
            currentRoute == Screen.MyPage.route ||
                    currentRoute?.startsWith("profile/") == true -> {
                selectedTab = screens.indexOf(Screen.MyPage)
            }
        }
    }
    // 홈일때만 바텀 시트 렌더
    if (currentRoute == Screen.Home.route && showBottomSheet) {
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

//  라우트 바뀌면 바텀시트 부드럽게 닫기
    LaunchedEffect(currentRoute) {
        if (currentRoute != Screen.Home.route && showBottomSheet) {
            coroutineScope.launch {
                sheetState.hide()
                showBottomSheet = false
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
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
                },
                navController = navController,
                onHeightMeasured = { h -> bottomBarHeightDp = h }
            )
        },
        floatingActionButton = {
            if (currentRoute == Screen.Home.route) {   // ← 홈에서만
                FloatingActionButton(onClick = { showBottomSheet = true },
                    modifier = Modifier.coachTarget("home_ai_fab", targets, expandPx = 10f)
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = "Open AI Chat")
                    // 제미나이 아이콘 쓰려면 painterResource(R.drawable.ic_gemini)로 교체
                }
            }
        },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }
    ) { paddingValues ->
        ShakeToOpenQR(
            navController = navController,
            routeQR = "qrPayScanner",   // 등록한 라우트와 일치시킴
            enabled = (currentRoute != "qrPayScanner") && !overlayActive // QR 화면에선 비활성화
        )

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("category/{categoryName}") { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                val categoryViewModel: CategoriesViewModel = viewModel()

                CategoryScreen(
                    categoryName = categoryName,
                    viewModel = categoryViewModel,
                    navController = navController
                )
            }

            composable(
                route = "channel_management/{channelName}",
                arguments = listOf(navArgument("channelName") { defaultValue = "DefaultChannel" })
            ) { backStackEntry ->
                val channelName = backStackEntry.arguments?.getString("channelName") ?: "DefaultChannel"
                ChannelManagementScreen(
                    channelName = channelName,
                    onEditClick = {
                        navController.navigate("edit_channel/$channelName") // 채널 documentId나 name 사용
                    }
                )
            }
            composable(
                route = "edit_channel/{channelName}",
                arguments = listOf(navArgument("channelName") { type = NavType.StringType })
            ) { backStackEntry ->
                val channelName = backStackEntry.arguments?.getString("channelName") ?: return@composable
                ChannelEditScreenLoader(
                    channelName = channelName,
                    navController = navController // NavController를 넘겨준다
                )
            }
            composable("HiddenScreen") {
                HiddenScreen()   // 당신이 만든 숨김 화면 Composable
            }
            composable("trendRecipes") {
                val trendRecipeViewModel: TrendRecipeViewModel = viewModel()
                TrendRecipeScreen(
                    viewModel = trendRecipeViewModel,
                    navController = navController
                )
            }
            composable(
                route ="recommendRecipes"
            ){
                RankScreenImproved(navController)
            }
            composable(
                route ="ranklist"
            ){
                RankScreenImproved(navController)
            }
            composable("map") {
                val viewModel = viewModel<MatzipViewModel>(it) // NavBackStackEntry로 viewModel 스코프 맞추기
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                RestaurantMapWithCustomDrawer(
                    viewModel = viewModel,
                    drawerState = drawerState,
                    scope = scope,
                    navController = navController
                )
            }
            composable("openchat_home") { OpenChatHomeScreen(navController) }
            composable("openchat_create") { CreateOpenChatScreen(navController) }
            composable(
                route = "openchat/{roomId}",
                arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "foodtable://openchat?roomId={roomId}" })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
                OpenChatRoomScreen(navController, roomId)
            }
            composable(
                route = "recipe_by_id/{rid}",
                deepLinks = listOf(navDeepLink { uriPattern = "foodtable://recipe?rid={rid}" })
            ) { backStackEntry ->
                val rid = backStackEntry.arguments?.getString("rid")!!
                RecipeByIdScreen(rid = rid, navController = navController)
            }
            composable("appointments") {
                AppointmentHomeScreen(navController)
            }
            composable("appointment/{id}") { backStackEntry ->
                val apptId = backStackEntry.arguments?.getString("id")!!
                AppointmentDetailScreen(apptId = apptId, nav = navController)
            }
            composable("qrPayScanner"){ QrPayScannerScreen(onBack = { navController.popBackStack() }) }
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
                RestaurantMapMainScreen(
                    viewModel = matzipViewModel,
                    navController = navController
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
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            homeViewModel = viewModel,
                            listState = listState,
                            nav = navController,
                            targets = targets
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
// 🔐 비밀 입력 시퀀스 설정 (아이콘 왼쪽부터 1~5라고 가정)
                val secretSequence = remember { listOf(1, 3, 2, 5, 4) }
                var clickHistory by remember { mutableStateOf(emptyList<Int>()) }

// (선택) 입력 시간 제한: 마지막 입력 후 N초 지나면 히스토리 초기화
                var lastInputAt by remember { mutableStateOf(0L) }
                val timeoutMs = 6000L  // 6초 안에 입력해야 함
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
            composable("add_ingredient?section={section}") { backStackEntry ->
                val fridgeViewModel: FridgeViewModel = viewModel()
                val section = backStackEntry.arguments?.getString("section") ?: "냉장"
                AddIngredientScreen(
                    viewModel = fridgeViewModel,
                    navController = navController,
                    section = section
                )
            }
            composable("recipe_cook/{recipeJson}") { backStackEntry ->
                val encodedJson = backStackEntry.arguments?.getString("recipeJson") ?: return@composable
                val decodedJson = URLDecoder.decode(encodedJson, StandardCharsets.UTF_8.toString())
                val recipeItem = Gson().fromJson(decodedJson, RecipeItem::class.java)

                RecipeCookingScreen(
                    recipe = recipeItem,
                    navController = navController
                )
            }

            // AI 추천 레시피 화면
            // 수정 후: recipeJson 인자 하나로 변경
            composable(
                route = "ai_recipe/{recipeJson}",
                arguments = listOf(navArgument("recipeJson") {
                    type = NavType.StringType
                })
            ) { backStackEntry ->
                val jsonEncoded = backStackEntry.arguments?.getString("recipeJson") ?: ""
                Log.d("NavRoute", "Encoded recipeJson from route: $jsonEncoded")

                val jsonDecoded = Uri.decode(jsonEncoded)
                Log.d("NavRoute", "Decoded recipeJson: $jsonDecoded")

                val recipeItem = Gson().fromJson(jsonDecoded, RecipeItem::class.java)
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                AiRecipeScreen(
                    recipe = recipeItem,
                    navController = navController,
                    onSaveToChannel = { /* … */ },
                    userId = userId
                )
            }


            composable(Screen.Subscribe.route) {
                SubscribeScreen(
                    viewModel = subscribeViewModel,
                    navController = navController,
                    onOverlayActiveChange = { isActive ->
                        consumeOverlaySignal(Screen.Subscribe.route, isActive)
                    },
                    bottomObstructionDp = bottomBarHeightDp
                )
            }

            composable(Screen.Social.route) {
                SocialScreen(
                    navController = navController,
                    onOverlayActiveChange = { isActive ->
                        consumeOverlaySignal(Screen.Social.route, isActive)
                    },
                    bottomObstructionDp = bottomBarHeightDp
                )
            }

            composable(Screen.RecipeStorage.route) {
                MyRecipeStorageScreen(
                    navController = navController,
                    onOverlayActiveChange = { isActive ->
                        consumeOverlaySignal(Screen.RecipeStorage.route, isActive)
                    },
                    bottomObstructionDp = bottomBarHeightDp
                )
            }

            composable(Screen.MyPage.route) {
                ProfileMainScreen(
                    paddingValues = paddingValues,
                    navController = navController,
                    parentOverlayActiveChange = { isActive ->
                        // 🔁 이전: overlayActive = isActive
                        consumeOverlaySignal(Screen.MyPage.route, isActive)
                    },
                    bottomObstructionDp = bottomBarHeightDp
                )
            }
        }
    }
        Log.i("BOTTOM_BLOCK", "overlayActive=$overlayActive route=$currentRoute scrimHeight=$scrimHeight bottomBarHeightDp=$bottomBarHeightDp")

        // 라우트별 시각용 스크림 표시 여부만 결정
        val showVisualScrim = overlayActive && when {
            currentRoute == Screen.Home.route -> false          // 홈만 끔
            currentRoute == Screen.Subscribe.route -> true
            currentRoute == Screen.Social.route -> true
            currentRoute == Screen.RecipeStorage.route -> true
            currentRoute == Screen.MyPage.route -> true         //  마이페이지 켬
            currentRoute?.startsWith("profile/") == true -> true// 프로필 파생 라우트도 켬
            else -> true
        }
        Log.i("BOTTOM_BLOCK", "showVisualScrim=$showVisualScrim")
        if (overlayActive) {
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(998f) // CoachmarkOverlay(999f) 아래
            ) {
                //터치 차단은 무조건 (모든 라우트 공통)
                com.bcu.foodtable.JetpackCompose.coach.InteractionBlocker(
                    visible = true,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(scrimHeight) // bottomBarHeightDp 기반
                )

                //  시각용 검은 박스는 라우트에 따라
                if (showVisualScrim) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(scrimHeight)
                            .background(CoachScrimColor) // 0.55 통일
                    )
                }
            }
        }


        if (showCoach) {
        CoachmarkOverlay(
            screen = CoachScreen.HOME,
            steps = listOf(
                CoachStep("home_banner", "이벤트/공지", "혜택과 소식을 확인해요."),
                CoachStep("home_trend", "인기 레시피", "지금 뜨는 메뉴를 봅니다."),
                CoachStep("home_reco", "맞춤 추천", "취향 기반 추천을 확인하세요."),
                CoachStep("home_ai_fab", "AI 요리 도우미", "챗으로 메뉴 추천/요리 팁 받기."),
                CoachStep("home_search", "레시피 검색", "이름·재료로 빠르게 찾아보세요.", center = true),
                CoachStep("home_card", "레시피 카드", "탭해 열고 자세히 보기.", center = true)
            ),
            targets = targets,
            store = coachStore,
            bottomObstructionDp = bottomBarHeightDp, // 이미 변수로 바꿈
            onClose = {
                showCoach = false
                if (CoachTour.running.value == true && CoachTour.currentScreen.value == CoachScreen.HOME) {
                    CoachTour.next(navController, context, coachStore )
                }
            },
            lazyListState = listState,
            onOverlayActiveChange = { isActive -> overlayActive = isActive },
            modifier = Modifier.fillMaxSize().zIndex(999f)
        )
    }
    }
}
@Composable
fun AppBottomNavigationBar(
    screens: List<Screen>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    navController: NavController, // 🔹 navController 추가
    onHeightMeasured: (Dp) -> Unit = {}
) {
    val context = LocalContext.current
    // 🔐 비밀 코드 상태
    val secretSequence = listOf(1, 3, 2, 5, 4) // 원하는 순서
    var clickHistory by remember { mutableStateOf(emptyList<Int>()) }
    var lastInputAt by remember { mutableStateOf(0L) }
    val timeoutMs = 6000L // 입력 제한 시간(6초)
    val density = LocalDensity.current
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 0.dp,
        modifier = Modifier.onSizeChanged { size ->
            onHeightMeasured(with(density) { size.height.toDp() }) // ← 실측
        }
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
                onClick = {
                    // 🔐 비밀 코드 입력 로직
                    val now = System.currentTimeMillis()
                    if (now - lastInputAt > timeoutMs) clickHistory = emptyList()
                    lastInputAt = now

                    val pressed = index + 1
                    clickHistory = (clickHistory + pressed).takeLast(secretSequence.size)

                    if (clickHistory == secretSequence) {
                        clickHistory = emptyList()
                        // ✅ 여기서 SlotActivity 실행
                        context.startActivity(Intent(context, SlotActivity::class.java))
                        return@NavigationBarItem
                    }

                    // 기본 탭 동작
                    onTabSelected(index)
                },
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


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromoBannerPagerFromFirestore(
    modifier: Modifier = Modifier,
    targets: CoachTargets,
    bringer: BringIntoViewRequester
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var promoList by remember { mutableStateOf<List<PromotionItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = db.collection("promotion").get().await()
            promoList = snapshot.documents.mapNotNull { it.toObject(PromotionItem::class.java) }
        } catch (e: Exception) {
            Log.e("Promo", "프로모션 로딩 실패", e)
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.LightGray)
            .bringIntoViewRequester(bringer)
            .coachTarget("home_banner", targets),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.trip))
                val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

                LottieAnimation(
                    composition = composition,
                    progress = progress,
                    modifier = Modifier.size(80.dp)
                )
            }

            promoList.isEmpty() -> {
                Text("이벤트가 없습니다!", color = Color.Gray)
            }

            else -> {
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    pageCount = { promoList.size }
                )

                // ✅ 전환 보장: 애니메이션 실패시 즉시 전환
                LaunchedEffect(pagerState.currentPage) {
                    delay(5000)
                    val next = (pagerState.currentPage + 1) % promoList.size

                    try {
                        if (!pagerState.isScrollInProgress) {
                            pagerState.animateScrollToPage(
                                page = next,
                                animationSpec = tween(
                                    durationMillis = 500,
                                    easing = FastOutSlowInEasing
                                )
                            )
                        }
                    } catch (e: Exception) {
                        pagerState.scrollToPage(next)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val item = promoList[page]

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(item.imageres)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    Toast
                                        .makeText(context, "링크로 이동합니다", Toast.LENGTH_SHORT)
                                        .show()
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                                    context.startActivity(intent)
                                }
                        )
                    }

                    CustomPagerIndicator(
                        totalDots = promoList.size,
                        selectedIndex = pagerState.currentPage,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun RecipePreviewCard(
    recipe: RecipeItem,
    onClick: ((String) -> Unit)? = null,
    homeViewModel: HomeViewModel
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var selectedRecipe by remember { mutableStateOf<RecipeItem?>(null) }

    val uid = UserManager.getUser()?.uid


    fun openOrPrompt(recipe: RecipeItem) {
        if (uid == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                val snap = db.collection("user").document(uid)
                    .collection("purchased").document(recipe.id).get().await()
                if (snap.getBoolean("purchased") == true) {
                    onClick?.invoke(recipe.id)
                    context.startActivity(
                        Intent(context, RecipeCookingActivity::class.java).apply {
                            putExtra("recipe_id", recipe.id)
                        }
                    )
                } else {
                    selectedRecipe = recipe
                    showPurchaseDialog = true
                }
            } catch (_: Exception) {
                Toast.makeText(context, "구매 여부 확인 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun goToRecipeCooking(recipeId: String) {
        onClick?.invoke(recipeId)
        val intent = Intent(context, RecipeCookingActivity::class.java)
        intent.putExtra("recipe_id", recipeId)
        context.startActivity(intent)
    }



    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable {
                openOrPrompt(recipe)
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = recipe.imageResId,
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = painterResource(id = R.drawable.ic_placeholder_dish),
                error = painterResource(id = R.drawable.ic_placeholder_dish)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }

    if (showPurchaseDialog && selectedRecipe != null) {
        RecipePurchaseDialogExact(
            recipe = selectedRecipe!!,
            onPurchased = {
                showPurchaseDialog = false
                homeViewModel.markRecipeAsPurchased(selectedRecipe!!.id)
                context.startActivity(
                    Intent(context, RecipeCookingActivity::class.java).apply {
                        putExtra("recipe_id", selectedRecipe!!.id)
                    }
                )
            },
            onDismiss = { showPurchaseDialog = false }
        )
    }
}


@Composable
fun CategoryGrid(
    categories: List<Pair<String, ImageVector>>,
    selectedCategory: String?,
    navController: NavController
) {
    val chunkedCategories = categories.chunked(6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chunkedCategories.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { (name, icon) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                navController.navigate("category/${name}")
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (name == selectedCategory) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = if (name == selectedCategory) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun MoreButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AddCircleOutline,
                contentDescription = "더보기",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "더보기",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeContent(
    paddingValues: PaddingValues,
    recipes: List<RecipeItem>,
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    homeViewModel: HomeViewModel,
    listState: LazyListState,
    nav: NavController,
    targets: CoachTargets
) {
    var selectedCuisine by remember { mutableStateOf<String?>(null) }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var selectedPrepTime by remember { mutableStateOf<String?>(null) }

    val db = FirebaseFirestore.getInstance()
    val uid = UserManager.getUser()?.uid

    val calorieVm: RecipeCalorieViewModel = viewModel()
    var purchasedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val coachBringer = remember { BringIntoViewRequester() }
    // 헤더/리스트 좌우 패딩 통일
    val headerHPad = 8.dp

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        db.collection("user")
            .document(uid)
            .collection("purchased")
            .addSnapshotListener { snap, _ ->
                if (snap != null) purchasedIds = snap.documents.map { it.id }.toSet()
            }
    }

    val filteredRecipes = recipes.filter { recipe ->
        val matchesSearch = searchQuery.text.isEmpty() ||
                recipe.name.contains(searchQuery.text, true) ||
                recipe.description.contains(searchQuery.text, true)
        val matchesCuisine = selectedCuisine == null || recipe.C_categories.getOrNull(0) == selectedCuisine
        val matchesDifficulty = selectedDifficulty == null || recipe.C_categories.getOrNull(1) == selectedDifficulty
        val d = recipe.duration
        val matchesPrepTime = selectedPrepTime == null || when (selectedPrepTime) {
            "15분 이내" -> d <= 15
            "30분 이내" -> d <= 30
            "1시간 이내" -> d <= 60
            "1시간 이상" -> d > 60
            else -> true
        }
        matchesSearch && matchesCuisine && matchesDifficulty && matchesPrepTime
    }

    var sectionOrder by remember {
        mutableStateOf(
            mutableListOf(
                HomeSection.TrendRecipes,
                HomeSection.RecommendRecipes,
                HomeSection.Categories,
                HomeSection.SearchBar,
                HomeSection.RecipeList // 끝에 고정
            )
        )
    }
    val topClickedRecipes by homeViewModel.topClickedRecipes.collectAsState()
    val recommendedRecipes by homeViewModel.recommendedRecipes.collectAsState()

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var offsetY by remember { mutableStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .bringIntoViewRequester(coachBringer),
    contentPadding = PaddingValues(
            top = 0.dp,
            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
            bottom = paddingValues.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 배너
        item {
            PromoBannerPagerFromFirestore(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                    targets = targets,
                    bringer = coachBringer

            )
        }

        // 섹션(RecipeList 제외)
        sectionOrder.forEachIndexed { index, section ->
            if (section is HomeSection.RecipeList) return@forEachIndexed

            item {
                val isDragging = draggedIndex == index
                val isSearchBar = section is HomeSection.SearchBar

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { if (isDragging) translationY = offsetY }
                        .then(
                            if (!isSearchBar)
                                Modifier.pointerInput(section) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedIndex = index },
                                        onDragEnd = { draggedIndex = null; offsetY = 0f },
                                        onDragCancel = { draggedIndex = null; offsetY = 0f },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetY += dragAmount.y
                                            val targetIndex = (index + (offsetY / 150).toInt())
                                                .coerceIn(0, sectionOrder.lastIndex - 1)
                                            if (targetIndex != index &&
                                                sectionOrder.getOrNull(targetIndex) !is HomeSection.RecipeList
                                            ) {
                                                sectionOrder = sectionOrder.toMutableList().apply {
                                                    add(targetIndex, removeAt(index))
                                                }
                                                draggedIndex = targetIndex
                                                offsetY = 0f
                                            }
                                        }
                                    )
                                }
                            else Modifier
                        )
                        .background(if (isDragging) Color.LightGray else Color.Transparent)
                ) {
                    Column {
                        when (section) {
                            is HomeSection.SearchBar -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = headerHPad, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "레시피 둘러보기",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Divider(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        thickness = 1.dp
                                    )
                                }
                                val searchBringer = remember { BringIntoViewRequester() }

                                ModernSearchBar(
                                    searchQuery = searchQuery,
                                    onSearchQueryChange = onSearchQueryChange,
                                    modifier = Modifier.bringIntoViewRequester(searchBringer).coachTarget("home_search", targets, bringer = searchBringer, expandPx = 8f )
                                )
                            }

                            is HomeSection.TrendRecipes -> {
                                Column(Modifier.padding(vertical = 8.dp)) { // ⬅ 수평 패딩 제거
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = headerHPad, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "인기 레시피",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.coachTarget("home_trend", targets, bringer = coachBringer, expandPx = 6f)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Divider(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            thickness = 1.dp
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    if (topClickedRecipes.isEmpty()) {
                                        Text(
                                            text = "불러오는 중...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = headerHPad)
                                        )
                                    } else {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = headerHPad),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(topClickedRecipes) { recipe ->
                                                RecipePreviewCard(
                                                    recipe = recipe,
                                                    onClick = { /* no-op */ },
                                                    homeViewModel = homeViewModel
                                                )
                                            }
                                            item {
                                                MoreButton { nav.navigate("trendRecipes") }
                                            }
                                        }
                                    }
                                }
                            }

                            is HomeSection.RecommendRecipes -> {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = headerHPad, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "추천 레시피",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.coachTarget("home_reco", targets, bringer = coachBringer, expandPx = 6f)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Divider(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            thickness = 1.dp
                                        )
                                    }
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = headerHPad, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(recommendedRecipes) { recipe ->
                                            RecipePreviewCard(
                                                recipe = recipe,
                                                homeViewModel = homeViewModel,
                                                onClick = { /* 선택 시 추가 동작 */ }
                                            )
                                        }
                                        item {
                                            MoreButton { nav.navigate("recommendRecipes") }
                                        }
                                    }
                                }
                            }

                            is HomeSection.Categories -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = headerHPad, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "카테고리",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Divider(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        thickness = 1.dp
                                    )
                                }
                                val categoryList = listOf(
                                    "한식" to Icons.Default.RiceBowl,
                                    "중식" to Icons.Default.RamenDining,
                                    "양식" to Icons.Default.LunchDining,
                                    "일식" to Icons.Default.SetMeal,
                                    "디저트" to Icons.Default.Cake,
                                    "분식" to Icons.Default.Fastfood,
                                    "샐러드" to Icons.Default.EmojiNature,
                                    "패스트푸드" to Icons.Default.LocalPizza,
                                    "야식" to Icons.Default.NightShelter,
                                    "브런치" to Icons.Default.BrunchDining,
                                    "음료" to Icons.Default.LocalCafe,
                                    "채식" to Icons.Default.Eco
                                )
                                var selectedCategory by remember { mutableStateOf<String?>(null) }
                                CategoryGrid(
                                    categories = categoryList,
                                    selectedCategory = selectedCategory,
                                    navController = nav
                                )
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }

        // RecipeList
        if (filteredRecipes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
            itemsIndexed(
                items = filteredRecipes,
                key = { idx, recipe ->
                    recipe.id.takeIf { it.isNotBlank() } ?: "recipe-$idx-${recipe.name}"
                }
            ) { index, recipe ->

                val firstCardBringer = remember(index == 0) {
                    if (index == 0) BringIntoViewRequester() else null
                }

                var isVisible by remember { mutableStateOf(false) }
                val isPurchasedFlag = purchasedIds.contains(recipe.id)
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                var showPurchaseDialog by remember { mutableStateOf(false) }
                var selectedRecipe by remember { mutableStateOf<RecipeItem?>(null) }

                val estimatedCal = calorieVm.caloriesMap[recipe.id]

                LaunchedEffect(recipe.id) {
                    try {
                        isVisible = true
                        calorieVm.loadOrEstimateCalories(recipe)
                    } catch (_: Exception) { }
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(500, delayMillis = 50)
                    ) + fadeIn(animationSpec = tween(400))
                ) {
                    val baseModifier = Modifier.animateItemPlacement(tween(300))
                    val cardModifier =
                        if (index == 0) baseModifier.then(
                            Modifier.bringIntoViewRequester(firstCardBringer!!).coachTarget("home_card", targets, bringer = firstCardBringer, expandPx = 10f)
                        ) else baseModifier
                    ModernRecipeCard(
                        recipe = recipe.copy(isPurchased = isPurchasedFlag),
                        estimatedCal = estimatedCal,
                        onCardClick = {
                            scope.launch {
                                try {
                                    val uid2 = UserManager.getUser()?.uid
                                    if (uid2 == null) {
                                        Toast.makeText(context, "유저 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    val docSnapshot = db.collection("user")
                                        .document(uid2)
                                        .collection("purchased")
                                        .document(recipe.id)
                                        .get()
                                        .await()

                                    if (!isActive) return@launch

                                    if (docSnapshot.exists()) {
                                        homeViewModel.trackRecipeView(recipe.id, recipe.C_categories)
                                        if (context.isActivity()) {
                                            val intent = Intent(context, RecipeCookingActivity::class.java)
                                            intent.putExtra("recipe_id", recipe.id)
                                            context.startActivity(intent)
                                        }
                                    } else {
                                        selectedRecipe = recipe
                                        showPurchaseDialog = true
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(context, "레시피 확인 중 오류 발생", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = cardModifier
                    )

                    if (showPurchaseDialog && selectedRecipe != null) {
                        RecipePurchaseDialogExact(
                            recipe = selectedRecipe!!,
                            onPurchased = {
                                showPurchaseDialog = false
                                homeViewModel.markRecipeAsPurchased(selectedRecipe!!.id)
                                if (context.isActivity()) {
                                    val intent = Intent(context, RecipeCookingActivity::class.java)
                                    intent.putExtra("recipe_id", selectedRecipe!!.id)
                                    context.startActivity(intent)
                                }
                            },
                            onDismiss = { showPurchaseDialog = false }
                        )
                    }
                }
            }
        }
    }
}

fun Context.isActivity(): Boolean {
    return this is Activity && !this.isFinishing && !this.isDestroyed
}

sealed class HomeSection {
    object SearchBar : HomeSection()
    object TrendRecipes : HomeSection()
    object RecommendRecipes : HomeSection()
    object Categories : HomeSection()
    object RecipeList : HomeSection()
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
@Composable
fun CustomPagerIndicator(
    totalDots: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.LightGray
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        repeat(totalDots) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (index == selectedIndex) activeColor else inactiveColor)
            )
        }
    }
}