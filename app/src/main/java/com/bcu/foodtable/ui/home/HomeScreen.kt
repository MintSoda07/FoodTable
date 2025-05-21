package com.bcu.foodtable.ui.home

import com.bcu.foodtable.useful.RecipeItem
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.AI.AiMainActivity
import com.bcu.foodtable.JetpackCompose.Channel.SubscribeScreen
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.ui.ChallengeActivity

import com.bcu.foodtable.JetpackCompose.HomeViewModel  // ✅ 수정된 ViewModel import 경로
import com.bcu.foodtable.JetpackCompose.HomeTopSection
import com.bcu.foodtable.JetpackCompose.Mypage.HealthConnectActivity
import com.bcu.foodtable.JetpackCompose.Mypage.ProfileMainScreen
import com.bcu.foodtable.JetpackCompose.RecipeCard
import com.bcu.foodtable.JetpackCompose.screens.MyChannelScreen
import com.bcu.foodtable.JetpackCompose.screens.AIServiceScreen
import com.bcu.foodtable.JetpackCompose.screens.RecipeStorageScreen
import com.bcu.foodtable.JetpackCompose.screens.ProfileScreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    navController: NavHostController
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedTab by remember { mutableStateOf(0) }
    val user by viewModel.user.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showSearchBar by remember { mutableStateOf(true) }

    val screens = listOf(
        Screen.Home, Screen.Subscribe, Screen.AIService, Screen.RecipeStorage, Screen.MyPage
    )

    LaunchedEffect(Unit) {
        if (recipes.isEmpty()) viewModel.loadRecipes()
        viewModel.loadUserInfo()
    }

    Scaffold(
        topBar = {
            HomeTopBar(user = user) {
                context.startActivity(Intent(context, ChallengeActivity::class.java))
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.primary) {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = screen.icon),
                                contentDescription = null
                            )
                        },
                        label = { Text(text = screen.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.White.copy(alpha = 0.7f),
                            selectedTextColor = Color.White,
                            unselectedTextColor = Color.White.copy(alpha = 0.7f),
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> HomeContent(
                paddingValues = paddingValues,
                recipes = recipes,
                user = user,
                searchQuery = searchQuery,
                showSearchBar = showSearchBar,
                selectedCategory = selectedCategory,
                onSearchQueryChange = { searchQuery = it },
                onSelectedCategoryChange = { selectedCategory = it },
                focusManager = focusManager,
                context = context
            )

            1 -> {
                SubscribeScreen(navController = navController)
            }

            2 -> {
                LaunchedEffect(Unit) {
                    context.startActivity(Intent(context, AiMainActivity::class.java))
                }
            }
            3 -> Text("Recipe Storage Screen")
            4 -> {
                ProfileMainScreen(
                    paddingValues = paddingValues
                )
            }

        }


        LaunchedEffect(searchQuery.text) {
            if (searchQuery.text.isNotBlank()) {
                delay(5000)
                showSearchBar = false
            }
        }
    }
}
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun HomeTopBar(user: User?, onChallengeClick: () -> Unit) { // ✅ nullable User 처리
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (user != null) {
                        AsyncImage(
                            model = user.image,
                            contentDescription = "프로필 이미지",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            error = rememberVectorPainter(Icons.Outlined.AccountCircle)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "소금: ${user.point}")
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("소금: \\0")
                    }
                }
            },
            actions = {
                IconButton(onClick = onChallengeClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_emoji_events_24),
                        contentDescription = "챌린지"
                    )
                }
            }
        )
    }

@Composable
private fun HomeContent(
    paddingValues: PaddingValues,
    recipes: List<RecipeItem>,
    user: User?, // ✅ nullable User
    searchQuery: TextFieldValue,
    showSearchBar: Boolean,
    selectedCategory: String?,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    onSelectedCategoryChange: (String?) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
            .background(MaterialTheme.colorScheme.background)
    ) {
        HomeTopSection(
            searchQuery = searchQuery.text,
            onSearchChange = { onSearchQueryChange(TextFieldValue(it)) },
            onSearchClick = {},
            selectedCategory = selectedCategory ?: "전체",
            onCategorySelect = { category ->
                onSelectedCategoryChange(if (category == "전체") null else category)
            }
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                CategoryChip(
                    category = "전체",
                    selected = selectedCategory == null,
                    onSelected = { onSelectedCategoryChange(null) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            val uniqueCategories = recipes.flatMap { it.C_categories }.distinct()
            items(uniqueCategories, key = { category -> category }) { category ->
                CategoryChip(
                    category = category,
                    selected = selectedCategory == category,
                    onSelected = { onSelectedCategoryChange(category) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = recipes.filter { recipe ->
                    val matchesCategory = selectedCategory == null || recipe.C_categories.contains(selectedCategory)
                    val matchesSearch = searchQuery.text.isEmpty() ||
                            recipe.name.contains(searchQuery.text, ignoreCase = true) ||
                            recipe.description.contains(searchQuery.text, ignoreCase = true)
                    matchesCategory && matchesSearch
                },
                key = { recipe ->
                    if (recipe.id.isNotBlank()) recipe.id else (recipe.name + recipe.hashCode())
                }
            ) { recipe ->
                RecipeCard(
                    title = recipe.name,
                    description = recipe.description,
                    imageUrl = recipe.imageResId,
                    saltReward = recipe.clicked
                )
            }
        }
    }
}

private sealed class Screen(val label: String, val icon: Int) {
    object Home : Screen("Home", R.drawable.ic_home_black_24dp)
    object Subscribe : Screen("Subscribe", R.drawable.ic_notifications_black_24dp)
    object AIService : Screen("AI Service", R.drawable.ic_dashboard_black_24dp)
    object RecipeStorage : Screen("Recipe Storage", R.drawable.baseline_menu_book_24)
    object MyPage : Screen("My Page", R.drawable.baseline_person_24)
}

@Composable
fun CategoryChip(category: String, selected: Boolean, onSelected: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onSelected,
        label = { Text(category) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray
        )
    )
}
