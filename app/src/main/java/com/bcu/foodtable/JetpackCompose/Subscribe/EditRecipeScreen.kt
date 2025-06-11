package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.bcu.foodtable.R
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

// ────────────────────────────────────────────────────────────────────────────
// ★ EditRecipeScreen: 기존에 업로드된 레시피를 불러와 수정하는 화면 ★
//   • recipeId: Firestore "recipe" 컬렉션의 문서 ID
//   • channelName: 해당 레시피가 속한 채널 이름(name) (권한 검증 시 사용)
//   • onSuccess: 수정 완료 후 호출할 콜백 (예: 뒤로 돌아가기, 새로 고침 등)
// ────────────────────────────────────────────────────────────────────────────

// 데이터 클래스 (기존 코드와 동일)
//data class Step(
//    val title: String,
//    val description: String,
//    val method: String?,
//    val time: String?
//)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun EditRecipeScreen(
    recipeId: String,
    channelName: String,
    onModifySuccess: () -> Unit,
    onDeleteSuccess: () -> Unit
) {

    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // 1) 초기 로딩 상태
    var isLoadingData by remember { mutableStateOf(true) }
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }
    var originalRecipe by remember { mutableStateOf<RecipeItem?>(null) }

    // 2) 수정 폼 상태값
    var title by remember { mutableStateOf(TextFieldValue()) }
    var description by remember { mutableStateOf(TextFieldValue()) }
    var note by remember { mutableStateOf(TextFieldValue()) }
    var originalImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val recipeSteps = remember { mutableStateListOf<Step>() }
    var ingredients by remember { mutableStateOf(mutableListOf<String>()) }
    var tags by remember { mutableStateOf(mutableListOf<String>()) }
    var priceInput by remember { mutableStateOf(TextFieldValue()) }
    var durationInput by remember { mutableStateOf(TextFieldValue()) }
    var categoryList by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val difficulties = listOf("쉬움", "보통", "어려움")
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var difficultyExpanded by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    var stepTitle by remember { mutableStateOf(TextFieldValue()) }
    var stepDescription by remember { mutableStateOf(TextFieldValue()) }
    var cookingMethod by remember { mutableStateOf(TextFieldValue()) }
    var hour by remember { mutableStateOf(TextFieldValue()) }
    var minute by remember { mutableStateOf(TextFieldValue()) }
    var second by remember { mutableStateOf(TextFieldValue()) }
    var useTimer by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf(TextFieldValue()) }
    var ingredientInput by remember { mutableStateOf(TextFieldValue()) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedImageUri = uri }

    // 데이터 로딩 및 상태 초기화 로직
    LaunchedEffect(recipeId) {
        isLoadingData = true
        try {
            val docSnap = firestore.collection("recipe").document(recipeId).get().await()
            if (docSnap.exists()) {
                originalRecipe = docSnap.toObject(RecipeItem::class.java)
                originalRecipe?.let { r ->
                    title = TextFieldValue(r.name)
                    description = TextFieldValue(r.description)
                    note = TextFieldValue(r.note ?: "")
                    originalImageUrl = r.imageResId
                    priceInput = TextFieldValue(r.priceInSalt.toString())
                    durationInput = TextFieldValue(r.duration.toString())
                    selectedCategory = r.C_categories.getOrNull(0)
                    selectedDifficulty = r.C_categories.getOrNull(1)
                    recipeSteps.clear()
                    // 정규식 개선: 타이머 없는 경우도 고려
                    val stepRegex = Regex("""^\s*(\d+)\.\(([^)]+)\)\s*([^○]*)""")
                    val timerRegex = Regex("""\(([^,]+),\s*(\d{2}:\d{2}:\d{2})\)""")

                    r.order.split("○").filter { it.isNotBlank() }.forEach { raw ->
                        stepRegex.find(raw)?.let { match ->
                            val fullDescription = match.groupValues[3].trim()
                            val timerMatch = timerRegex.find(fullDescription)

                            val stepDescriptionText = if(timerMatch != null) {
                                fullDescription.substringBefore(timerMatch.value).trim()
                            } else {
                                fullDescription
                            }

                            recipeSteps.add(Step(
                                title = match.groupValues[2],
                                description = stepDescriptionText,
                                method = timerMatch?.groupValues?.getOrNull(1),
                                time = timerMatch?.groupValues?.getOrNull(2)
                            ))
                        }
                    }
                    tags = r.tags.toMutableList()
                    ingredients = r.ingredients.toMutableList()
                }
            } else {
                loadErrorMessage = "존재하지 않는 레시피입니다."
            }
        } catch (e: Exception) {
            loadErrorMessage = "레시피 로드 실패: ${e.localizedMessage}"
        } finally {
            isLoadingData = false
        }
    }
    LaunchedEffect(Unit) {
        try {
            val doc = firestore.collection("C_categories").document("C_food_types").get().await()
            (doc.get("list") as? List<String>)?.let { categoryList = it }
        } catch (e: Exception) { Toast.makeText(context, "카테고리 로드 실패", Toast.LENGTH_SHORT).show() }
    }

    // “HH시 MM분 SS초” 형태로 포맷 (첫 번째 코드와 동일한 로직)
    fun getFormattedTime(): String {
        val h = hour.text.padStart(2, '0')
        val m = minute.text.padStart(2, '0')
        val s = second.text.padStart(2, '0')
        return "$h:$m:$s"
    }

    // --- UI 디자인 업그레이드 ---

    FoodTableTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("레시피 수정", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onModifySuccess) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                if (title.text.isBlank() || description.text.isBlank() || selectedCategory.isNullOrBlank()) {
                                    Toast.makeText(context, "필수 항목(제목, 설명, 카테고리)을 입력해주세요.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isUploading = true
                                val updateRecipeAction = { imageUrl: String ->
                                    val orderString = recipeSteps.mapIndexed { idx, s ->
                                        val mainPart = "○${idx + 1}.(${s.title})${s.description}"
                                        val timerPart = if (!s.method.isNullOrBlank() && !s.time.isNullOrBlank()) " (${s.method},${s.time})" else ""
                                        mainPart + timerPart
                                    }.joinToString("")

                                    val updatedMap = mutableMapOf<String, Any?>(
                                        "name" to title.text,
                                        "description" to description.text,
                                        "note" to note.text,
                                        "imageResId" to imageUrl,
                                        "priceInSalt" to (priceInput.text.toIntOrNull() ?: 0),
                                        "duration" to (durationInput.text.toIntOrNull() ?: 0),
                                        "C_categories" to listOfNotNull(selectedCategory, selectedDifficulty),
                                        "tags" to tags,
                                        "ingredients" to ingredients,
                                        "order" to orderString,
                                        "contained_channel" to channelName,
                                        "authorId" to currentUser?.uid,
                                        "authorName" to originalRecipe?.authorName,
                                        "likes" to originalRecipe?.likes,
                                        "likedUsers" to originalRecipe?.likedUsers,
                                        "cost" to (priceInput.text.toIntOrNull() ?: 0),
                                        "estimatedCalories" to originalRecipe?.estimatedCalories,
                                        "date" to Timestamp.now()
                                    )
                                    firestore.collection("recipe").document(recipeId).update(updatedMap.filterValues { it != null } as Map<String, Any>)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "레시피 수정 완료", Toast.LENGTH_SHORT).show()
                                            onModifySuccess()
                                        }
                                        .addOnFailureListener { e -> Toast.makeText(context, "수정 실패: ${e.message}", Toast.LENGTH_LONG).show() }
                                        .addOnCompleteListener { isUploading = false }
                                }

                                if (selectedImageUri != null) {
                                    FireStoreHelper.uploadImage(selectedImageUri!!, UUID.randomUUID().toString(), "recipe_image",
                                        onSuccess = { newImageUrl -> updateRecipeAction(newImageUrl) },
                                        onFailure = { isUploading = false; Toast.makeText(context, "이미지 업로드 실패", Toast.LENGTH_SHORT).show() }
                                    )
                                } else {
                                    updateRecipeAction(originalImageUrl ?: "")
                                }
                            },
                            enabled = !isUploading && !isDeleting
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text("저장")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            }
        ) { paddingValues ->
            if (isLoadingData) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Scaffold
            }
            if (loadErrorMessage != null) {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text(loadErrorMessage ?: "알 수 없는 오류", color = MaterialTheme.colorScheme.error)
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    EditSectionCard(title = "기본 정보", icon = Icons.Default.Article) {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("레시피 제목") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("한 줄 설명") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("나만의 비고 (선택)") }, modifier = Modifier.fillMaxWidth())
                    }
                }

                item {
                    EditSectionCard(title = "대표 이미지", icon = Icons.Default.Image) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { pickImageLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = selectedImageUri ?: originalImageUrl,
                                contentDescription = "대표 이미지",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                error = rememberAsyncImagePainter(model = R.drawable.ic_placeholder_dish_error),
                                placeholder = rememberAsyncImagePainter(model = R.drawable.ic_placeholder_dish)
                            )
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                                    Text("이미지를 변경하려면 터치하세요", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                item {
                    EditSectionCard(title = "상세 정보", icon = Icons.Default.Tune) {
                        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                            OutlinedTextField(readOnly = true, value = selectedCategory ?: "", onValueChange = {}, label = { Text("카테고리") }, modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) })
                            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                categoryList.forEach { category -> DropdownMenuItem(text = { Text(category) }, onClick = { selectedCategory = category; categoryExpanded = false }) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        ExposedDropdownMenuBox(expanded = difficultyExpanded, onExpandedChange = { difficultyExpanded = !difficultyExpanded }) {
                            OutlinedTextField(readOnly = true, value = selectedDifficulty ?: "", onValueChange = {}, label = { Text("난이도") }, modifier = Modifier.fillMaxWidth().menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = difficultyExpanded) })
                            ExposedDropdownMenu(expanded = difficultyExpanded, onDismissRequest = { difficultyExpanded = false }) {
                                difficulties.forEach { diff -> DropdownMenuItem(text = { Text(diff) }, onClick = { selectedDifficulty = diff; difficultyExpanded = false }) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // 숫자 필터링 로직 개선
                        OutlinedTextField(
                            value = durationInput,
                            onValueChange = { newValue ->
                                val filtered = newValue.text.filter { it.isDigit() }
                                durationInput = TextFieldValue(text = filtered, selection = TextRange(filtered.length))
                            },
                            label = { Text("소요 시간 (분)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(Modifier.height(12.dp))
                        // 숫자 필터링 로직 개선
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { newValue ->
                                val filtered = newValue.text.filter { it.isDigit() }
                                priceInput = TextFieldValue(text = filtered, selection = TextRange(filtered.length))
                            },
                            label = { Text("가격 (Salt)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                item {
                    EditSectionCard(title = "태그 (${tags.size})", icon = Icons.Default.Style) {
                        val focusManager = LocalFocusManager.current
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { tagInput = it },
                                label = { Text("태그 추가") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (tagInput.text.isNotBlank() && !tags.contains("#${tagInput.text.trim()}")) {
                                        tags.add("#${tagInput.text.trim()}")
                                        tagInput = TextFieldValue("")
                                        focusManager.clearFocus()
                                    }
                                })
                            )
                            IconButton(onClick = {
                                if (tagInput.text.isNotBlank() && !tags.contains("#${tagInput.text.trim()}")) {
                                    tags.add("#${tagInput.text.trim()}")
                                    tagInput = TextFieldValue("")
                                }
                            }) { Icon(Icons.Default.Add, contentDescription = "태그 추가") }
                        }
                        if (tags.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                tags.forEach { tag ->
                                    InputChip(selected = false, onClick = { tags.remove(tag) }, label = { Text(tag) }, trailingIcon = { Icon(Icons.Default.Close, contentDescription = "삭제", modifier = Modifier.size(18.dp)) })
                                }
                            }
                        }
                    }
                }

                item {
                    EditSectionCard(title = "재료 (${ingredients.size})", icon = Icons.Default.Kitchen) {
                        val focusManager = LocalFocusManager.current
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = ingredientInput,
                                onValueChange = { ingredientInput = it },
                                label = { Text("재료 추가") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (ingredientInput.text.isNotBlank() && !ingredients.contains(ingredientInput.text.trim())) {
                                        ingredients.add(ingredientInput.text.trim())
                                        ingredientInput = TextFieldValue("")
                                        focusManager.clearFocus()
                                    }
                                })
                            )
                            IconButton(onClick = {
                                if (ingredientInput.text.isNotBlank() && !ingredients.contains(ingredientInput.text.trim())) {
                                    ingredients.add(ingredientInput.text.trim())
                                    ingredientInput = TextFieldValue("")
                                }
                            }) { Icon(Icons.Default.Add, contentDescription = "재료 추가") }
                        }
                        if (ingredients.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ingredients.forEach { ing ->
                                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { ingredients.remove(ing) }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Remove, contentDescription = "삭제", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("- $ing", modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    EditSectionCard(title = "조리 단계 (${recipeSteps.size})", icon = Icons.Default.List) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = stepTitle, onValueChange = { stepTitle = it }, label = { Text("단계 제목") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = stepDescription, onValueChange = { stepDescription = it }, label = { Text("단계 설명") }, modifier = Modifier.fillMaxWidth())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = useTimer, onCheckedChange = { useTimer = it })
                                Text("타이머 사용")
                            }
                            // =================================================================
                            // ★★★ 타이머 UI를 첫 번째 코드의 형태로 수정 ★★★
                            // =================================================================
                            AnimatedVisibility(visible = useTimer) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)){
                                    OutlinedTextField(
                                        value = cookingMethod,
                                        onValueChange = { cookingMethod = it },
                                        label = { Text("조리 방법 (예: 굽기)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = hour,
                                            onValueChange = { newV ->
                                                val num = newV.text.filter { it.isDigit() }
                                                    .toIntOrNull()
                                                    ?.coerceIn(0, 59)
                                                    ?.toString() ?: ""
                                                hour = TextFieldValue(num, TextRange(num.length))
                                            },
                                            label = { Text("시") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                                        )
                                        OutlinedTextField(
                                            value = minute,
                                            onValueChange = { newV ->
                                                val num = newV.text.filter { it.isDigit() }
                                                    .toIntOrNull()
                                                    ?.coerceIn(0, 59)
                                                    ?.toString() ?: ""
                                                minute = TextFieldValue(num, TextRange(num.length))
                                            },
                                            label = { Text("분") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                                        )
                                        OutlinedTextField(
                                            value = second,
                                            onValueChange = { newV ->
                                                val num = newV.text.filter { it.isDigit() }
                                                    .toIntOrNull()
                                                    ?.coerceIn(0, 59)
                                                    ?.toString() ?: ""
                                                second = TextFieldValue(num, TextRange(num.length))
                                            },
                                            label = { Text("초") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                                        )
                                    }
                                    Text(
                                        text = "설정된 시간: ${getFormattedTime()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    val newStep = Step(
                                        title = stepTitle.text,
                                        description = stepDescription.text,
                                        method = if (useTimer) cookingMethod.text else null,
                                        time = if (useTimer) getFormattedTime() else null
                                    )
                                    recipeSteps.add(newStep)
                                    // 입력 필드 초기화
                                    stepTitle = TextFieldValue()
                                    stepDescription = TextFieldValue()
                                    cookingMethod = TextFieldValue()
                                    hour = TextFieldValue()
                                    minute = TextFieldValue()
                                    second = TextFieldValue()
                                    useTimer = false
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("단계 추가") }
                        }
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recipeSteps.forEachIndexed { i, step ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = { recipeSteps.removeAt(i) }),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${i + 1}.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(step.title, fontWeight = FontWeight.SemiBold)
                                            Text(step.description, style = MaterialTheme.typography.bodyMedium)
                                            step.time?.let { timeString ->
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)){
                                                    Icon(Icons.Default.Timer, contentDescription = "타이머", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("타이머: ${step.method} - $timeString", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                                }
                                            }
                                        }
                                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("레시피 삭제", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "삭제")
                                Spacer(Modifier.width(8.dp))
                                Text("삭제하기")
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("레시피 삭제") },
                text = { Text("정말 이 레시피를 삭제하시겠습니까? 삭제된 레시피는 복구할 수 없습니다.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isDeleting = true
                            firestore.collection("recipe").document(recipeId).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "레시피가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                    onModifySuccess()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "삭제 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                                .addOnCompleteListener { isDeleting = false; showDeleteDialog = false }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("삭제")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
                }
            )
        }
    }
}


@Composable
private fun EditSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            content()
        }
    }
}