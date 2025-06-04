
package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bcu.foodtable.JetpackCompose.AI.AiHelperViewModel
import com.bcu.foodtable.ai.OpenAIClient
import com.bcu.foodtable.useful.RecipeItem
import com.google.gson.Gson
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FridgeScreen(viewModel: FridgeViewModel, navController: NavController) {
    val allIngredients = viewModel.ingredientList


    var selectedTabIndex by remember { mutableStateOf(0) }


    val scope = rememberCoroutineScope()
    val showDialog = remember { mutableStateOf<Ingredient?>(null) }
    val fridgeSections = listOf("냉장", "냉동", "문칸")
    val fridgeMap = remember { fridgeSections.associateWith { mutableStateListOf<Ingredient>() }.toMutableMap() }
    val outsideFridge = remember { mutableStateListOf<Ingredient>() }
    LaunchedEffect(Unit) {
        viewModel.loadIngredients() //  단 한 번만 호출됨
    }
    // 초기 분류 (한 번만 실행)
    LaunchedEffect(allIngredients) {
        fridgeSections.forEach { fridgeMap[it]?.clear() }

        outsideFridge.clear() // ← 초기화 안 하면 duplication 발생 가능

        allIngredients.forEach { ingredient ->
            val alreadyInFridge = fridgeMap[ingredient.section]?.any { it.id == ingredient.id } ?: false
            val alreadyOutside = outsideFridge.any { it.id == ingredient.id }

            if (!alreadyInFridge && !alreadyOutside) {
                fridgeMap[ingredient.section]?.add(ingredient)
            }
        }
    }


    fun moveIngredientToOutside(
        ingredient: Ingredient,
        fromSection: String,
        fridgeMap: MutableMap<String, SnapshotStateList<Ingredient>>,
        outsideFridge: SnapshotStateList<Ingredient>
    ) {
        println(" [OUT] 시도: ${ingredient.name} / $fromSection")

        val removed = fridgeMap[fromSection]?.removeIf { it.id == ingredient.id } == true
        val existsOutside = outsideFridge.any { it.id == ingredient.id }

        println(" [OUT] removed: $removed / already exists outside: $existsOutside")

        if (removed) {
            // 중복 제거 후 추가 (안전하게)
            outsideFridge.removeAll { it.id == ingredient.id }
            outsideFridge.add(ingredient)
            println(" [OUT] 이동 완료: ${ingredient.name}")
        }
    }


    fun moveIngredientToFridge(
        ingredient: Ingredient,
        toSection: String,
        fridgeMap: MutableMap<String, SnapshotStateList<Ingredient>>,
        outsideFridge: SnapshotStateList<Ingredient>
    ) {
        println("⬅ [IN] 시도: ${ingredient.name} / $toSection")

        val removed = outsideFridge.removeIf { it.id == ingredient.id }
        val exists = fridgeMap[toSection]?.any { it.id == ingredient.id } == true

        println("⬅ [IN] removed: $removed / already exists in section: $exists")

        if (removed && !exists) {
            val updated = ingredient.copy(section = toSection)
            fridgeMap[toSection]?.add(updated)

            // // 파이어베이스 반영
            viewModel.updateIngredientSection(ingredient.id, toSection)

            println(" [IN] 이동 완료: ${ingredient.name}")
        }
    }



    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

        val fridgeDropThreshold = constraints.maxHeight * 0.5f
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    fridgeSections.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            Text(
                " ${fridgeSections[selectedTabIndex]}칸",
                style = MaterialTheme.typography.titleLarge
            )


            // LazyVerticalGrid 그대로 유지
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
                items(fridgeMap[fridgeSections[selectedTabIndex]] ?: emptyList(), key = { it.id }) { ingredient ->
                    val currentSection = fridgeSections[selectedTabIndex]
                    IngredientCard(
                        ingredient = ingredient,
                        onClick = {
                            moveIngredientToOutside(
                                ingredient, currentSection, fridgeMap, outsideFridge
                            )
                        },
                        onLongClick = { showDialog.value = ingredient },
                        draggable = true,
                        onDragEnd = {
                            moveIngredientToOutside(
                                ingredient, currentSection, fridgeMap, outsideFridge
                            )
                        }
                    )
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            Text("\uD83E\uDDF5 꺼낸 재료", style = MaterialTheme.typography.titleLarge)

            Row(modifier = Modifier.fillMaxWidth().height(100.dp).padding(top = 8.dp)) {
                outsideFridge.forEach { ingredient ->
                    key(ingredient.id) {
                    var offset by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(80.dp)
                            .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offset += dragAmount
                                    },
                                    onDragEnd = {
                                        if (offset.y < fridgeDropThreshold) {
                                            moveIngredientToFridge(
                                                ingredient,
                                                toSection = fridgeSections[selectedTabIndex],
                                                fridgeMap = fridgeMap,
                                                outsideFridge = outsideFridge
                                            )
                                        }
                                        offset = Offset.Zero
                                    }
                                )
                            }
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ingredient.name)
                    }
                    }
                }

            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(onClick = {
                    val selectedSection = fridgeSections[selectedTabIndex]
                    navController.navigate("add_ingredient?section=$selectedSection")
                }) {
                    Icon(Icons.Default.Add, contentDescription = "재료 추가")
                }
            }

        }
    }

    // 팝업: 이걸로 만들 수 있는 요리
    showDialog.value?.let { selectedIngredient ->
        AlertDialog(
            onDismissRequest = { showDialog.value = null },
            title = { Text("추천 요리") },
            text = {
                val recipes = viewModel.findRecipesByIngredient(selectedIngredient.name)
                Column {

                    recipes.forEach { Text("• $it") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog.value = null }) {
                    Text("닫기")
                }
            }
        )
    }
    // 상단에 추가
    val aiViewModel: AiHelperViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AiHelperViewModel(OpenAIClient()) as T
            }
        }
    )
    val aiState by aiViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val gson = remember { Gson() }

// 버튼 추가
    // AI 추천 버튼
    Button(
        onClick = {
            val takenOut = outsideFridge.map { it.name }.joinToString(", ")
            aiViewModel.onInputChange(takenOut)
            aiViewModel.sendMessage()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text("🍳 꺼낸 재료로 AI 요리 추천")
    }


// 응답이 도착하면 AiRecipeScreen으로 이동
    LaunchedEffect(aiState.resultText) {
        if (aiState.resultText.isNotBlank()) {
            Log.d("AI_RAW", aiState.resultText)  // 이제 실제 조리 단계가 포함된 원문이 출력됩니다.

            val recipeName = Regex("""◆(.*?)◆""")
                .find(aiState.resultText)
                ?.groupValues?.getOrNull(1)
                ?: "AI 추천 요리"

            val ingredients = Regex("""◆.*?◆\((.*?)\)""")
                .find(aiState.resultText)
                ?.groupValues?.getOrNull(1)
                ?.split(",")?.map { it.trim() }
                ?: emptyList()

            // “○없음” 또는 숫자+마침표만 있는 케이스 모두를 포괄하도록
            val stepRegex = Regex(
                """^[\u0020\u00A0\u3000]*[○\u25CB\u2460]?\s*\d+\..*""",
                RegexOption.MULTILINE
            )

            val matches = stepRegex.findAll(aiState.resultText).toList()
            Log.d("AI_REGEX_MATCH_COUNT", "match 개수 = ${matches.size}")
            matches.forEach { match ->
                Log.d("AI_REGEX_MATCH_LINE", "[${match.value}]")
            }

            val order = matches
                .map { it.value.trim() }
                .joinToString(" ")
            Log.d("AI_ORDER_STRING", "order = \"$order\"")

            if (order.isBlank()) {
                Log.e("AI_ORDER", " 조리 단계 없음\n${aiState.resultText}")
                Toast.makeText(context, "AI가 조리 단계를 반환하지 않았어요", Toast.LENGTH_LONG).show()
                return@LaunchedEffect
            }

            val recipe = RecipeItem(
                id = UUID.randomUUID().toString(),
                name = recipeName,
                description = "AI가 추천한 요리입니다.",
                imageResId = "",
                ingredients = ingredients,
                order = order,
                tags = listOf("AI추천"),
                C_categories = listOf("AI")
            )

            val encodedRecipeJson = Uri.encode(Gson().toJson(recipe))
            Log.d("AI_NAV", "🔁 페이지 전환: recipe=${recipe.name}")
            navController.navigate("ai_recipe/$encodedRecipeJson")

            aiViewModel.hideWarning()
        }
    }






}
fun getEmojiForIngredient(name: String): String {
    return when (name) {
        "계란" -> "\uD83E\uDD5A"
        "당근" -> "\uD83E\uDD55"
        "상추" -> "\uD83C\uDF3F"
        "소고기" -> "\uD83E\uDD69"
        "양파" -> "\uD83E\uDDC5"
        "감자" -> "\uD83E\uDD54"
        else -> "\uD83C\uDF72" // 기본 요리 이모지
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IngredientCard(
    ingredient: Ingredient,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    draggable: Boolean = false,
    onDragEnd: (() -> Unit)? = null
) {
    var offset by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = Modifier
            .padding(6.dp)
            .size(110.dp)
            .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
            .then(
                if (draggable) Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            offset = Offset.Zero
                            onDragEnd?.invoke()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offset += dragAmount
                        }
                    )
                } else Modifier
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.6f) // 반투명한 유리 느낌
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            // 이모지
            Text(
                text = getEmojiForIngredient(ingredient.name),
                style = MaterialTheme.typography.headlineMedium
            )

            // 이름
            Text(
                text = ingredient.name,
                style = MaterialTheme.typography.titleSmall,
                color = Color.Black
            )

            // 수량
            Text(
                text = "${ingredient.quantity}개",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray
            )
        }
    }
}

