package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bcu.foodtable.ui.home.FoodTableTheme
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

// ────────────────────────────────────────────────────────────────────────────
// ★ EditRecipeScreen: 기존에 업로드된 레시피를 불러와 수정하는 화면 ★
//   • recipeId: Firestore "recipe" 컬렉션의 문서 ID
//   • channelName: 해당 레시피가 속한 채널 이름(name) (권한 검증 시 사용)
//   • onSuccess: 수정 완료 후 호출할 콜백 (예: 뒤로 돌아가기, 새로 고침 등)
// ────────────────────────────────────────────────────────────────────────────


@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class  // material3 API도 실험적이면 추가
)
@Composable
fun EditRecipeScreen(
    recipeId: String,
    channelName: String,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    // 삭제 확인 다이얼로그 노출 상태
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // ──────────────────────────────────────────────────────────────────────
    // 1) 초기 로딩 상태: Firestore에서 기존 Recipe를 불러오고, 에러 처리
    // ──────────────────────────────────────────────────────────────────────
    var isLoadingData by remember { mutableStateOf(true) }
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }
    var originalRecipe by remember { mutableStateOf<RecipeItem?>(null) }

    // 문서를 비동기 호출하여 originalRecipe에 저장
    LaunchedEffect(recipeId) {
        try {
            val docSnap = firestore.collection("recipe").document(recipeId).get().await()
            if (docSnap.exists()) {
                originalRecipe = docSnap.toObject(RecipeItem::class.java)
            } else {
                loadErrorMessage = "존재하지 않는 레시피입니다."
            }
        } catch (e: Exception) {
            loadErrorMessage = "레시피 로드 실패: ${e.localizedMessage}"
        } finally {
            isLoadingData = false
        }
    }

    // 로딩 중 표시
    if (isLoadingData) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    // 로드 오류 시 메시지 표시 후 종료
    if (loadErrorMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = loadErrorMessage!!,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    // ──────────────────────────────────────────────────────────────────────
    // 2) originalRecipe를 바탕으로 “수정 폼” 상태값 초기화
    // ──────────────────────────────────────────────────────────────────────
    val scrollState = rememberScrollState()

    // 2-0) 드롭다운/입력용 초기값
    var categoryList by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val difficulties = listOf("쉬움", "보통", "어려움")
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var difficultyExpanded by remember { mutableStateOf(false) }

    // 2-1) 기본 텍스트/이미지/리스트 상태
    var title by remember { mutableStateOf(TextFieldValue()) }
    var description by remember { mutableStateOf(TextFieldValue()) }
    var note by remember { mutableStateOf(TextFieldValue()) }

    var originalImageUrl by remember { mutableStateOf<String?>(null) } // Firestore에 저장된 기존 URL
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }    // 사용자가 새로 고른 이미지

    val recipeSteps = remember { mutableStateListOf<Step>() }
    var ingredients by remember { mutableStateOf(mutableListOf<String>()) }
    var tags by remember { mutableStateOf(mutableListOf<String>()) }

    // 2-2) 가격, 소요시간(분)
    var priceInput by remember { mutableStateOf(TextFieldValue()) }
    var durationInput by remember { mutableStateOf(TextFieldValue()) }

    // 2-3) “단계 입력”용 상태
    var stepTitle by remember { mutableStateOf(TextFieldValue()) }
    var stepDescription by remember { mutableStateOf(TextFieldValue()) }
    var cookingMethod by remember { mutableStateOf(TextFieldValue()) }
    var hour by remember { mutableStateOf(TextFieldValue()) }
    var minute by remember { mutableStateOf(TextFieldValue()) }
    var second by remember { mutableStateOf(TextFieldValue()) }
    var useTimer by remember { mutableStateOf(false) }

    // 2-4) “태그/재료 입력”용 상태
    var tagInput by remember { mutableStateOf(TextFieldValue()) }
    var ingredientInput by remember { mutableStateOf(TextFieldValue()) }

    // 2-5) 폼을 제출할 때 업로드 상태
    var isUploading by remember { mutableStateOf(false) }

    // Image Picker Launcher
    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
            }
        }

    // 2-6) Firestore에서 카테고리 목록 불러오기 (WriteScreen과 동일)
    LaunchedEffect(Unit) {
        try {
            val doc = firestore
                .collection("C_categories")
                .document("C_food_types")
                .get()
                .await()
            val list = doc.get("list") as? List<String>
            if (!list.isNullOrEmpty()) {
                categoryList = list
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "카테고리 로드 실패: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 2-7) originalRecipe 값을 기준으로 상태 초기화
    LaunchedEffect(originalRecipe) {
        originalRecipe?.let { r ->
            title = TextFieldValue(r.name)
            description = TextFieldValue(r.description)
            note = TextFieldValue(r.note ?: "")

            originalImageUrl = r.imageResId

            priceInput = TextFieldValue(r.priceInSalt.toString())
            durationInput = TextFieldValue(r.duration.toString())

            // 카테고리/난이도
            if (r.C_categories.isNotEmpty()) {
                selectedCategory = r.C_categories.getOrNull(0)
                selectedDifficulty = r.C_categories.getOrNull(1)
            }

            // 단계(order 필드 예시: "○1. 제목○2. 제목..." 형태)
            recipeSteps.clear()
            r.order.split("○")
                .filter { it.isNotBlank() }
                .forEach { raw ->
                    // “1. 제목(방법) (HH:MM:SS)” 형태 파싱 식 예시
                    val regex =
                        Regex("""^\s*○?\s*\d+\.\s*\(([^)]+)\)\s*(.*?)(?:\s*\(([^()]+?),\s*([0-9]{2}:[0-9]{2}:[0-9]{2})\))?$""")
                    val match = regex.find(raw.trim())
                    val titleText = match?.groupValues?.getOrNull(1) ?: ""
                    val descText = match?.groupValues?.getOrNull(2) ?: raw
                    val methodText = match?.groupValues?.getOrNull(3)
                    val timeText = match?.groupValues?.getOrNull(4)
                    recipeSteps.add(
                        Step(
                            title = titleText,
                            description = descText,
                            method = methodText,
                            time = timeText
                        )
                    )
                }

            // 태그, 재료
            tags = r.tags.toMutableList()
            ingredients = r.ingredients.toMutableList()
        }
    }

    // “HH시 MM분 SS초” 형태로 포맷
    fun getFormattedTime(): String {
        val h = hour.text.padStart(2, '0')
        val m = minute.text.padStart(2, '0')
        val s = second.text.padStart(2, '0')
        return "${h}시 ${m}분 ${s}초"
    }

    // ──────────────────────────────────────────────────────────────────────
    // 3) EditRecipeScreen UI 렌더링
    // ──────────────────────────────────────────────────────────────────────
    FoodTableTheme {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // 3-1) 화면 제목
        Text(
            text = "레시피 수정",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(12.dp))

        // 3-2) 제목 / 설명 / 비고
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("제목") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("설명") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("비고 (note)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // 3-3) 소요 시간 입력 (분 단위 Int)
        OutlinedTextField(
            value = durationInput,
            onValueChange = { newValue ->
                val filtered = newValue.text.filter { it.isDigit() }
                val cursorPos = filtered.length
                durationInput = TextFieldValue(
                    text = filtered,
                    selection = TextRange(cursorPos)
                )
            },
            label = { Text("소요 시간 (분 단위 정수)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.height(12.dp))

        // 3-4) 가격 입력(숫자만)
        OutlinedTextField(
            value = priceInput,
            onValueChange = { newValue ->
                val filtered = newValue.text.filter { it.isDigit() }
                val cursorPos = filtered.length
                priceInput = TextFieldValue(
                    text = filtered,
                    selection = TextRange(cursorPos)
                )
            },
            label = { Text("가격 (숫자만 입력)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.height(12.dp))

        // 3-5) 카테고리 드롭다운
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = selectedCategory ?: "",
                onValueChange = { },
                label = { Text("카테고리 선택") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categoryList.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = {
                            selectedCategory = category
                            categoryExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // 3-6) 난이도 드롭다운
        ExposedDropdownMenuBox(
            expanded = difficultyExpanded,
            onExpandedChange = { difficultyExpanded = !difficultyExpanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = selectedDifficulty ?: "",
                onValueChange = { },
                label = { Text("난이도 선택") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = difficultyExpanded,
                onDismissRequest = { difficultyExpanded = false }
            ) {
                difficulties.forEach { diff ->
                    DropdownMenuItem(
                        text = { Text(diff) },
                        onClick = {
                            selectedDifficulty = diff
                            difficultyExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // 3-7) 이미지 선택 (기존 URL 또는 새 Uri)
        if (selectedImageUri != null) {
            // 새 이미지를 고른 경우
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "새로 선택된 이미지",
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .clickable { pickImageLauncher.launch("image/*") },
                contentScale = ContentScale.Crop
            )
        } else {
            // 새 이미지를 고르지 않은 경우, 기존 Firestore URL 보여줌
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray, shape = MaterialTheme.shapes.medium)
                    .clickable { pickImageLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (!originalImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = originalImageUrl,
                        contentDescription = "기존 이미지",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("이미지 선택", color = Color.DarkGray)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // 3-8) 조리 단계 입력
        Text("조리 단계 추가", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = stepTitle,
            onValueChange = { stepTitle = it },
            label = { Text("단계 제목") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = stepDescription,
            onValueChange = { stepDescription = it },
            label = { Text("단계 설명") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = cookingMethod,
            onValueChange = { cookingMethod = it },
            label = { Text("조리 방법") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = useTimer,
                onCheckedChange = { useTimer = it }
            )
            Text("타이머 사용")
        }
        Spacer(Modifier.height(4.dp))

        if (useTimer) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = hour,
                    onValueChange = { hour = it },
                    label = { Text("시") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = minute,
                    onValueChange = { minute = it },
                    label = { Text("분") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = second,
                    onValueChange = { second = it },
                    label = { Text("초") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }
            Spacer(Modifier.height(4.dp))

            Text(
                text = "(${getFormattedTime()})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                // 단계 추가
                val step = Step(
                    title = stepTitle.text,
                    description = stepDescription.text,
                    method = if (useTimer) cookingMethod.text else null,
                    time = if (useTimer) getFormattedTime() else null
                )
                recipeSteps.add(step)

                // 입력 초기화
                stepTitle = TextFieldValue("")
                stepDescription = TextFieldValue("")
                cookingMethod = TextFieldValue("")
                hour = TextFieldValue("")
                minute = TextFieldValue("")
                second = TextFieldValue("")
                useTimer = false
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("단계 추가")
        }
        Spacer(Modifier.height(12.dp))

        if (recipeSteps.isNotEmpty()) {
            Text("추가된 조리 단계", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))

            recipeSteps.forEachIndexed { i, step ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            onClick = { /* 클릭 시 별 동작 없음 */ },
                            onLongClick = { recipeSteps.removeAt(i) }
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${i + 1}. ${step.title}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "ㄴ ${step.description}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        step.time?.let { timeString ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "($timeString)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 3-9) 태그/재료 입력
        Text("태그 및 재료 입력", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                label = { Text("태그") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (tagInput.text.isNotBlank()) {
                    tags.add("#${tagInput.text}")
                    tagInput = TextFieldValue("")
                }
            }) {
                Text("추가")
            }
        }
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    AssistChip(
                        modifier = Modifier.combinedClickable(
                            onClick = { /* 클릭 시 별 동작 없음 */ },
                            onLongClick = { tags.remove(tag) }
                        ),
                        onClick = { /* 클릭 시 별 동작 없음 */ },
                        label = { Text(tag) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = ingredientInput,
                onValueChange = { ingredientInput = it },
                label = { Text("재료") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (ingredientInput.text.isNotBlank()) {
                    ingredients.add(ingredientInput.text)
                    ingredientInput = TextFieldValue("")
                }
            }) {
                Text("추가")
            }
        }
        if (ingredients.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                ingredients.forEach { ing ->
                    Text(
                        text = "- $ing",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .combinedClickable(
                                onClick = { /* 클릭 시 별 동작 없음 */ },
                                onLongClick = { ingredients.remove(ing) }
                            )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 3-10) 수정 완료 버튼
        Button(
            onClick = {
                // 필수 입력값 체크
                if (title.text.isBlank() ||
                    description.text.isBlank() ||
                    selectedCategory.isNullOrBlank() ||
                    selectedDifficulty.isNullOrBlank() ||
                    priceInput.text.isBlank() ||
                    durationInput.text.isBlank()
                ) {
                    Toast.makeText(context, "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isUploading = true

                // 1) 새 이미지를 골랐는지 확인
                if (selectedImageUri != null) {
                    // 이미지 변경 필요: Storage에 업로드 → URL을 받아서 update
                    FireStoreHelper.uploadImage(
                        imageUri = selectedImageUri!!,
                        imageName = UUID.randomUUID().toString(),
                        folderName = "recipe_image",
                        onSuccess = { newImageUrl ->
                            // Firestore 문서 update
                            val updatedMap = mutableMapOf<String, Any>(
                                "name" to title.text,
                                "description" to description.text,
                                "note" to note.text,
                                "imageResId" to newImageUrl,
                                "priceInSalt" to priceInput.text.toInt(),
                                "duration" to durationInput.text.toInt(),
                                "C_categories" to listOfNotNull(selectedCategory, selectedDifficulty),
                                "tags" to tags,
                                "ingredients" to ingredients,
                                "order" to "○" + recipeSteps.mapIndexed { idx, s -> "${idx + 1}. ${s.title}" }
                                    .joinToString("○"),
                                "contained_channel" to channelName,
                                "authorId" to (currentUser?.uid ?: ""),
                                "authorName" to (originalRecipe?.authorName ?: ""),
                                "likes" to (originalRecipe?.likes ?: 0),
                                "likedUsers" to (originalRecipe?.likedUsers ?: listOf<String>()),
                                "cost" to priceInput.text.toInt(),
                                "estimatedCalories" to (originalRecipe?.estimatedCalories ?: ""),
                                "date" to Timestamp.now()
                            )

                            firestore.collection("recipe").document(recipeId)
                                .update(updatedMap)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "레시피 수정 완료", Toast.LENGTH_SHORT).show()
                                    isUploading = false
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        "레시피 수정 실패: ${e.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    isUploading = false
                                }
                        },
                        onFailure = {
                            isUploading = false
                            Toast.makeText(context, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    // 이미지 변경 없음: 기존 originalImageUrl 유지
                    val updatedMap = mutableMapOf<String, Any>(
                        "name" to title.text,
                        "description" to description.text,
                        "note" to note.text,
                        "imageResId" to (originalImageUrl ?: ""),
                        "priceInSalt" to priceInput.text.toInt(),
                        "duration" to durationInput.text.toInt(),
                        "C_categories" to listOfNotNull(selectedCategory, selectedDifficulty),
                        "tags" to tags,
                        "ingredients" to ingredients,
                        "order" to "○" + recipeSteps.mapIndexed { idx, s -> "${idx + 1}. ${s.title}" }
                            .joinToString("○"),
                        "contained_channel" to channelName,
                        "authorId" to (currentUser?.uid ?: ""),
                        "authorName" to (originalRecipe?.authorName ?: ""),
                        "likes" to (originalRecipe?.likes ?: 0),
                        "likedUsers" to (originalRecipe?.likedUsers ?: listOf<String>()),
                        "cost" to priceInput.text.toInt(),
                        "estimatedCalories" to (originalRecipe?.estimatedCalories ?: ""),
                        "date" to Timestamp.now()
                    )

                    firestore.collection("recipe").document(recipeId)
                        .update(updatedMap)
                        .addOnSuccessListener {
                            Toast.makeText(context, "레시피 수정 완료", Toast.LENGTH_SHORT).show()
                            isUploading = false
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "레시피 수정 실패: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                            isUploading = false
                        }
                }
            },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isUploading) "수정 중..." else "레시피 수정 완료")
        }
// ───────────── 삭제 버튼 ─────────────
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { showDeleteDialog = true },
            enabled = !isUploading && !isDeleting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F),  // Material red 700
                contentColor = Color.White
            )
        ) {
            Text(if (isDeleting) "삭제 중..." else "레시피 삭제")
        }

// 삭제 확인 다이얼로그
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("레시피 삭제") },
                text = { Text("정말 이 레시피를 삭제하시겠습니까? 삭제된 레시피는 복구할 수 없습니다.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        isDeleting = true
                        // Firestore에서 문서 삭제
                        firestore.collection("recipe")
                            .document(recipeId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "레시피가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                isDeleting = false
                                onSuccess()  // 뒤로 이동 또는 새로고침 콜백
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "삭제 실패: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                isDeleting = false
                            }
                    }) {
                        Text("삭제")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                    }) {
                        Text("취소")
                    }
                }
            )
        }
        Spacer(Modifier.height(40.dp))
    }
}}

//// ────────────────────────────────────────────────────────────────────────────
//// Step 데이터 클래스: 조리 단계 정보를 담습니다
//// ────────────────────────────────────────────────────────────────────────────
//data class Step(
//    val title: String,
//    val description: String,
//    val method: String?,
//    val time: String?
//)
