
package com.bcu.foodtable.ui.myPage.myFridge

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

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
            TabRow(selectedTabIndex = selectedTabIndex) {
                fridgeSections.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Text(
                " ${fridgeSections[selectedTabIndex]}칸",
                style = MaterialTheme.typography.titleLarge
            )


            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
                items(fridgeMap[fridgeSections[selectedTabIndex]] ?: emptyList(), key = {it.id} ){ ingredient ->
                    IngredientCard(
                        ingredient = ingredient,
                        onClick = {
                            moveIngredientToOutside(
                                ingredient,
                                fromSection = fridgeSections[selectedTabIndex],
                                fridgeMap = fridgeMap,
                                outsideFridge = outsideFridge
                            )
                        },
                        onLongClick = { showDialog.value = ingredient },
                        draggable = true,
                        onDragEnd = {
                            moveIngredientToOutside(
                                ingredient,
                                fromSection = fridgeSections[selectedTabIndex],
                                fridgeMap = fridgeMap,
                                outsideFridge = outsideFridge
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
            .padding(8.dp)
            .size(100.dp)
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = "${getEmojiForIngredient(ingredient.name)} ${ingredient.name}")
        }
    }
}
