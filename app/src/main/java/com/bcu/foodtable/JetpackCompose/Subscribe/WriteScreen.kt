package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

/** 조리 단계 하나를 나타내는 데이터 클래스 **/
data class Step(
    val title: String,
    val description: String,
    val method: String?,
    val time: String?
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun WriteScreen(channelName: String, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val firestore = FirebaseFirestore.getInstance()

    // ────────────────────────────────────────────────────────
    // 0) 드롭다운 및 입력 상태
    // ────────────────────────────────────────────────────────
    var categoryList by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val difficulties = listOf("쉬움", "보통", "어려움")
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var difficultyExpanded by remember { mutableStateOf(false) }

    // 1) 일반 텍스트/이미지/리스트 상태
    var title by remember { mutableStateOf(TextFieldValue()) }
    var description by remember { mutableStateOf(TextFieldValue()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val recipeSteps = remember { mutableStateListOf<Step>() }
    var ingredients by remember { mutableStateOf(mutableListOf<String>()) }
    var tags by remember { mutableStateOf(mutableListOf<String>()) }
    var note by remember { mutableStateOf(TextFieldValue()) }

    /** 새로운 필드: 소요 시간 (분 단위로 입력받을 Int) **/
    var durationInput by remember { mutableStateOf(TextFieldValue()) }

    // 2) 조리 단계 입력 필드
    var stepTitle by remember { mutableStateOf(TextFieldValue()) }
    var stepDescription by remember { mutableStateOf(TextFieldValue()) }
    var cookingMethod by remember { mutableStateOf(TextFieldValue()) }
    var hour by remember { mutableStateOf(TextFieldValue()) }
    var minute by remember { mutableStateOf(TextFieldValue()) }
    var second by remember { mutableStateOf(TextFieldValue()) }
    var useTimer by remember { mutableStateOf(false) }

    // 3) 태그/재료 입력 필드
    var tagInput by remember { mutableStateOf(TextFieldValue()) }
    var ingredientInput by remember { mutableStateOf(TextFieldValue()) }

    // 4) 가격 입력(숫자만)
    var priceInput by remember { mutableStateOf(TextFieldValue()) }

    var isUploading by remember { mutableStateOf(false) }

    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            selectedImageUri = uri
        }

    // Firestore에서 카테고리 목록을 불러옵니다.
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
            Toast.makeText(context, "카테고리 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // “01시 01분 01초” 형태로 포맷
    fun getFormattedTime(): String {
        val h = hour.text.padStart(2, '0')
        val m = minute.text.padStart(2, '0')
        val s = second.text.padStart(2, '0')
        return "${h}시 ${m}분 ${s}초"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        //--------------------------------------------------------
        // 1. 제목 / 설명 / 비고
        //--------------------------------------------------------
        Text("레시피 작성", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

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

        //--------------------------------------------------------
        // 1-1. 소요 시간 입력 (분 단위로 Int)
        //--------------------------------------------------------
        OutlinedTextField(
            value = durationInput,
            onValueChange = { newValue ->
                // 숫자만 필터링
                val filtered = newValue.text.filter { ch -> ch.isDigit() }
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

        //--------------------------------------------------------
        // 1-2. 가격 입력 (Int)
        //--------------------------------------------------------
        OutlinedTextField(
            value = priceInput,
            onValueChange = { newValue ->
                val filtered = newValue.text.filter { ch -> ch.isDigit() }
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

        //--------------------------------------------------------
        // 1-3. 카테고리 드롭다운 (Firebase → C_food_types/list)
        //--------------------------------------------------------
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

        //--------------------------------------------------------
        // 1-4. 난이도 드롭다운 (쉬움/보통/어려움)
        //--------------------------------------------------------
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

        //--------------------------------------------------------
        // 2. 이미지 선택
        //--------------------------------------------------------
        if (selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = null,
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .clickable { pickImageLauncher.launch("image/*") },
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
                    .background(Color.LightGray, shape = MaterialTheme.shapes.medium)
                    .clickable { pickImageLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Text("이미지 선택", color = Color.DarkGray)
            }
        }
        Spacer(Modifier.height(16.dp))

        //--------------------------------------------------------
        // 3. 조리 단계 입력
        //--------------------------------------------------------
        Text("조리 단계 추가", style = MaterialTheme.typography.titleMedium)
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

        // 타이머 입력란 (보이기/숨기기)
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

            // 실제 표시용 타이머 텍스트
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
                // 단계 추가 로직
                val step = Step(
                    title = stepTitle.text,
                    description = stepDescription.text,
                    method = if (useTimer) cookingMethod.text else null,
                    time = if (useTimer) getFormattedTime() else null
                )
                recipeSteps.add(step)

                // 입력란 초기화
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

        //--------------------------------------------------------
        // 4. 추가된 단계 표시 (별도 디자인, 길게 눌러 삭제)
        //--------------------------------------------------------
        if (recipeSteps.isNotEmpty()) {
            Text("추가된 조리 단계", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            recipeSteps.forEachIndexed { i, step ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        // 길게 누르면 해당 인덱스 삭제
                        .combinedClickable(
                            onClick = { /* 클릭 시 별 동작 없음 */ },
                            onLongClick = {
                                recipeSteps.removeAt(i)
                            }
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 1. 제목
                        Text(
                            text = "${i + 1}. ${step.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        // ㄴ 설명
                        Text(
                            text = "ㄴ ${step.description}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        // 타이머가 있는 경우
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

        //--------------------------------------------------------
        // 5. 태그 및 재료 입력
        //--------------------------------------------------------
        Text("태그 및 재료 입력", style = MaterialTheme.typography.titleMedium)
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
        // 태그 목록 (길게 눌러 삭제)
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ){
                tags.forEach { tag ->
                    AssistChip(
                        modifier = Modifier.combinedClickable(
                            onClick = { /* 클릭 시 별 동작 없음 */ },
                            onLongClick = {
                                tags.remove(tag)
                            }
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
        // 재료 목록(불릿 리스트, 길게 눌러 삭제)
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
                                onLongClick = {
                                    ingredients.remove(ing)
                                }
                            )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        //--------------------------------------------------------
        // 6. 업로드 버튼
        //--------------------------------------------------------
        Button(
            onClick = {
                // 필수 입력값 체크
                if (selectedImageUri == null ||
                    title.text.isBlank() ||
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
                FireStoreHelper.uploadImage(
                    imageUri = selectedImageUri!!,
                    imageName = UUID.randomUUID().toString(),
                    folderName = "recipe_image",
                    onSuccess = { imageUrl ->
                        val recipe = RecipeItem(
                            name = title.text,
                            description = description.text,
                            imageResId = imageUrl,
                            clicked = 0,
                            date = Timestamp.now(),
                            order = "○" + recipeSteps.mapIndexed { idx, s -> "${idx + 1}. ${s.title}" }
                                .joinToString("○"),
                            id = "",
                            authorId = "",    // 실제 사용자는 로그인된 유저 ID로 설정
                            authorName = "",  // 실제 사용자는 사용자명으로 설정
                            priceInSalt = priceInput.text.toInt(),
                            C_categories = listOfNotNull(selectedCategory, selectedDifficulty),
                            note = note.text,
                            tags = tags,
                            ingredients = ingredients,
                            contained_channel = channelName,
                            estimatedCalories = null,
                            likes = 0,
                            likedUsers = listOf(),
                            cost = priceInput.text.toInt(),
                            duration = durationInput.text.toInt()
                        )
                        val db = FirebaseFirestore.getInstance()
                        val ref = db.collection("recipe").document()
                        recipe.id = ref.id
                        ref.set(recipe).addOnSuccessListener {
                            Toast.makeText(context, "레시피 업로드 완료", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }.addOnFailureListener {
                            Toast.makeText(context, "레시피 업로드 실패", Toast.LENGTH_SHORT).show()
                        }
                        isUploading = false
                    },
                    onFailure = {
                        isUploading = false
                        Toast.makeText(context, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isUploading) "업로드 중..." else "레시피 업로드")
        }
    }
}
