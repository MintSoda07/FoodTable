package com.bcu.foodtable.JetpackCompose.HomeChannelDatil
import com.google.firebase.auth.FirebaseAuth
import StepTimerState
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent // 추가: URL을 열기 위함
import android.net.Uri // 추가: URL을 파싱하기 위함
import android.print.PrintAttributes
import android.print.PrintManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // 추가: 클릭 가능한 UI를 만들기 위함
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.RecipeItem // RecipeItem에 ingredients: List<String> 필드가 있다고 가정
import com.bcu.foodtable.voice.VoiceCommandController
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.google.firebase.functions.ktx.functions
import android.util.Base64
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bcu.foodtable.TTS.CookingAiViewModel
import com.bcu.foodtable.TTS.CookingAiViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

fun byteArrayToBase64(byteArray: ByteArray): String {
    return Base64.encodeToString(byteArray, Base64.NO_WRAP)
}

// 상태 데이터 클래스 (StepTimerState는 별도 파일에 정의되어 있다고 가정)
data class CookingStepState(
    val text: String,
    val isDone: Boolean = false,
    val showTimer: Boolean = false,
    val timerTitle: String = "",
    val timerDuration: String = "",
    val isCurrent: Boolean = false,
    val timerState: StepTimerState? = null // ← 여기까지가 맞습니다!


)


@Composable
fun RecipeCookingScreen(
    recipe: RecipeItem,
    navController: NavController  // Compose 내비게이션 사용 시
) {
    val application = LocalContext.current.applicationContext as Application
    val firebaseFunctionsInstance = remember { Firebase.functions("us-central1") }
    val aiViewModelFactory =
        remember { CookingAiViewModelFactory(application, firebaseFunctionsInstance) }
    val aiViewModel: CookingAiViewModel =
        viewModel(key = "aiEvaluationViewModel", factory = aiViewModelFactory)
    val context = LocalContext.current

    // ViewModel 상태 관찰
    val isLoadingAiEval by aiViewModel.isLoading.collectAsState()
    val aiEvaluationResultText by aiViewModel.evaluationApiResult.collectAsState()
    val aiEvalToastMessage by aiViewModel.toastMessage.collectAsState()

    LaunchedEffect(aiEvalToastMessage) {
        aiEvalToastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            aiViewModel.clearToastMessage()
        }
    }

    var userImageUriForAiEval by remember { mutableStateOf<Uri?>(null) }

    val tts = remember {
        TextToSpeech(context, null).apply {
            language = Locale.KOREAN
        }
    }
    var userImageUri by remember { mutableStateOf<Uri?>(null) }

    val pickImageLauncherForAiEval = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        userImageUriForAiEval = uri
        uri?.let { selectedUserImageUri ->
            val recipeImageStringUrl = recipe.imageResId
            if (recipeImageStringUrl.startsWith("http")) {
                aiViewModel.evaluateCookingRecipe(recipeImageStringUrl, selectedUserImageUri)
            } else {
                Toast.makeText(
                    context,
                    "레시피 원본 이미지 URL이 유효하지 않습니다. (예: http...)",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("RecipeCooking_AI", "잘못된 레시피 이미지 URL: $recipeImageStringUrl. 전체 URL이어야 합니다.")
            }
        }
    }

    Log.d("RecipeOrderRaw", recipe.order)

    val recipeId = recipe.id.ifBlank { UUID.randomUUID().toString() }
    var steps by remember {
        mutableStateOf(
            recipe.order.split("○")
                .filter { it.isNotBlank() }
                .mapIndexed { index, raw ->
                    val regex =
                        Regex("""^\s*○?\s*\d+\.\s*\(([^)]+)\)\s*(.*?)(?:\s*\(([^()]+?),\s*([0-9]{2}:[0-9]{2}:[0-9]{2})\))?$""")
                    val match = regex.find(raw.trim())

                    val title = match?.groupValues?.getOrNull(1) ?: ""
                    val description = match?.groupValues?.getOrNull(2) ?: raw
                    val method = match?.groupValues?.getOrNull(3) ?: ""
                    val duration = match?.groupValues?.getOrNull(4) ?: ""

                    CookingStepState(
                        text = "$title: $description",
                        showTimer = method.isNotEmpty() && duration.isNotEmpty(),
                        timerTitle = method,
                        timerDuration = duration,
                        isCurrent = index == 0,
                        timerState = if (duration.isNotEmpty()) StepTimerState(
                            parseDuration(duration)
                        ) else null
                    )
                }
        )
    }

    if (steps.isEmpty()) {
        Log.e("RecipeCookingScreen", "레시피 단계가 없습니다. order: ${recipe.order}")
        Text("유효한 조리 단계가 없습니다.")
        return
    }

    var currentIndex by remember { mutableStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    val isListening = remember { mutableStateOf(false) }

     fun goToNextStepWithoutTTS() {
        if (currentIndex + 1 < steps.size) {
            steps = steps.mapIndexed { idx, step ->
                when (idx) {
                    currentIndex -> step.copy(isDone = true, isCurrent = false)
                    currentIndex + 1 -> step.copy(isCurrent = true)
                    else -> step
                }
            }
            currentIndex += 1
        } else {
            steps = steps.mapIndexed { idx, step ->
                if (idx == currentIndex) step.copy(isDone = true, isCurrent = false) else step
            }
            isFinished = true
        }
    }

    fun repeatStep() {
        tts.speak(steps[currentIndex].text, TextToSpeech.QUEUE_FLUSH, null, "repeat")
    }
    // 보이스 컨트롤러
    val voiceController = remember {
        VoiceCommandController(
            context = context,
            tts = tts,
            onCommand = {}
        )
    }
    //  “현재 단계의 StepTimerState”를 컨트롤러에 바인딩
    //    currentIndex나 steps가 바뀔 때마다 실행됩니다.
    LaunchedEffect(currentIndex, steps) {
        voiceController.stepTimerState = steps.getOrNull(currentIndex)?.timerState
    }


    // onCommand 핸들러 등록
    LaunchedEffect(Unit) {
        voiceController.onCommand = { command: VoiceCommandController.CommandType ->
            when (command) {
                VoiceCommandController.CommandType.NEXT -> {
                    // 상태만 업데이트 (버튼/음성 모두)
                    goToNextStepWithoutTTS()
                    // 음성 명령일 때만 TTS로 안내
                    if (currentIndex < steps.size) {
                        tts.speak(steps[currentIndex].text, TextToSpeech.QUEUE_FLUSH, null, "step")
                    } else {
                        tts.speak("모든 조리 과정을 완료했습니다.", TextToSpeech.QUEUE_FLUSH, null, "done")
                    }
                }
                VoiceCommandController.CommandType.REPEAT -> {
                    repeatStep()
                }
                VoiceCommandController.CommandType.STOP -> {
                    voiceController.stop()
                    isListening.value = false
                    tts.speak("음성 인식을 중지합니다.", TextToSpeech.QUEUE_FLUSH, null, "stop")
                }

                // ───────────────────────────────────────────────
                //  TIMER 분기는 컨트롤러 내부에서 이미 처리되므로,
                //    이곳에서는 별도 TTS 안내만(또는 아무것도 하지 않음) 해 줍니다.
                VoiceCommandController.CommandType.TIMER -> {
                    // (컨트롤러에서 이미 start/pause/resume 을 처리함)
                    // 혹시 “현재 단계에 타이머가 없을 때” 안내하고 싶다면 추가 가능:
                    if (steps.getOrNull(currentIndex)?.timerState == null) {
                        tts.speak("현재 단계에 타이머가 없습니다.", TextToSpeech.QUEUE_FLUSH, null, "no_timer")
                    }
                }

                VoiceCommandController.CommandType.NONE -> {
                    tts.speak("명령을 이해하지 못했습니다.", TextToSpeech.QUEUE_FLUSH, null, "fail")
                }
                // ───────────────────────────────────────────────
            }
        }
    }

    // 화면이 사라질 때 음성 인식이 꺼지도록
    DisposableEffect(Unit) {
        onDispose {
            if (isListening.value) {
                voiceController.stop()
                isListening.value = false
            }
        }
    }


    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // ───────────────────────────────────────────────────────
    // 1) 채널 소유 여부를 담을 상태
    // ───────────────────────────────────────────────────────
    var isChannelOwner by remember { mutableStateOf(false) }
    var channelCheckFinished by remember { mutableStateOf(false) } // 로딩 완료 체크

    // ───────────────────────────────────────────────────────
    // 2) Firestore에서 "채널 이름(name) == recipe.contained_channel" 으로 조회하여 owner 비교
    // ───────────────────────────────────────────────────────
    LaunchedEffect(recipe.contained_channel) {
        if (currentUser == null) {
            // 비로그인 상태면 무조건 false 처리
            isChannelOwner = false
            channelCheckFinished = true
            return@LaunchedEffect
        }

        try {
            // 방법: whereEqualTo("name", recipe.contained_channel) → 문서가 단건이라고 가정
            val querySnapshot = firestore
                .collection("channel")
                .whereEqualTo("name", recipe.contained_channel)
                .limit(1)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                // 첫 번째 문서만 가져와서 owner 필드를 읽는다
                val doc = querySnapshot.documents[0]
                val ownerUid = doc.getString("owner")

                isChannelOwner = (ownerUid == currentUser.uid)
            } else {
                // 채널 이름이 존재하지 않으면, false 처리
                isChannelOwner = false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "채널 소유자 확인 중 오류: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            isChannelOwner = false
        } finally {
            channelCheckFinished = true
        }
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
        // LazyColumn을 쓰되, channelCheckFinished가 true가 되어야 본문을 노출
        if (!channelCheckFinished) {
            // 채널 소유 여부 로딩 중에는 프로그레스 표시
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
        ) {
            item {
                // ─────────────────────────────────────────────────
                //   3-1) 레시피 토퍼: 제목 + 수정 버튼 (소유자 여부에 따라 노출)
                // ─────────────────────────────────────────────────
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 레시피 이름
                            Text(
                                recipe.name,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE25532),
                                    letterSpacing = (-0.5).sp
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // “수정” 아이콘: isChannelOwner이 true일 때만 나타내기
                            if (isChannelOwner) {
                                IconButton(
                                    onClick = {
                                        // EditRecipeScreen으로 이동: recipeId와 contained_channel을 인자로 전달
                                        navController.navigate("edit/${recipe.id}/${recipe.contained_channel}")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "레시피 수정",
                                        tint = Color(0xFFE25532)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 레시피 대표 이미지
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(24.dp))
                        ) {
                            AsyncImage(
                                model = recipe.imageResId,
                                contentDescription = recipe.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.1f)
                                            )
                                        )
                                    )
                            )

                            // Floating Info Cards
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recipe.estimatedCalories?.let {
                                    InfoChip(
                                        text = it,
                                        icon = "🔥",
                                        backgroundColor = Color(0xFFE25532).copy(alpha = 0.9f) // primary 색상
                                    )
                                }
                                InfoChip(
                                    text = "${steps.size}단계",
                                    icon = "👨‍🍳",
                                    backgroundColor = Color(0xFFB9806D).copy(alpha = 0.9f) // tertiary 색상
                                )
                            }
                        }
                    }
                }

                // Recipe Info Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFBE7DF) // surfaceVariant 색상
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
                                color = Color(0xFFE25532) // primary 색상
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            recipe.description,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0xFF3A2C28), // onBackground 색상
                                lineHeight = 28.sp
                            ),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Category and Tags with Modern Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryChip(
                                text = recipe.C_categories.joinToString(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tags with Hashtag Style
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recipe.tags) { tag ->
                                TagChip(tag = if (tag.startsWith("#")) tag else "#$tag")
                            }
                        }
                    }
                }

                // Ingredients Section
                if (recipe.ingredients.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFE2D6) // primaryContainer 색상
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
                                        color = Color(0xFFE25532) // primary 색상
                                    )
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recipe.ingredients.forEach { ingredient ->
                                    IngredientItem(
                                        ingredient = ingredient,
                                        onClick = {
                                            val encodedQuery = Uri.encode(ingredient)
                                            val url =
                                                "https://search.shopping.naver.com/search/all?query=$encodedQuery"
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                data = Uri.parse(url)
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "웹 브라우저를 열 수 없습니다.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                Log.e("RecipeCookingScreen", "네이버 쇼핑 링크 열기 오류: $e")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                LikeButton(recipeId = recipeId)
                Spacer(modifier = Modifier.height(24.dp))
            }

            itemsIndexed(
                steps,
                key = { index, step -> "$index-${step.text}-${step.isCurrent}-${step.isDone}" }
            ) { index, step ->
                CookingStepCard(
                    index = index,
                    step = step,
                    onNext = { goToNextStepWithoutTTS() },
                    onRepeat = { repeatStep() }
                )
            }

            if (isFinished) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFB9806D).copy(alpha = 0.1f) // tertiary 색상
                        ),
                        border = BorderStroke(
                            2.dp,
                            Color(0xFFB9806D).copy(alpha = 0.3f)
                        ) // tertiary 색상
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "🎉",
                                style = MaterialTheme.typography.displayMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                "조리 완료!",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = Color(0xFFB9806D), // tertiary 색상
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "모든 단계를 성공적으로 완료했습니다",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF5F5F5F) // onSurfaceVariant 색상
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Action Buttons Section
            item {
                Spacer(modifier = Modifier.height(20.dp))

                // Voice Control Button
                ModernActionButton(
                    text = if (isListening.value) "음성 명령 중지" else "🎤 음성 명령 시작",
                    backgroundColor = if (isListening.value) Color(0xFFD32F2F) else Color(0xFFE25532), // error와 primary 색상
                    onClick = {
                        if (!isListening.value) {
                            val started = voiceController.startListening()
                            if (started) {
                                //  버튼을 눌러 음성 인식이 켜질 때, 현재 단계 설명을 바로 TTS로 읽어 줌
                                steps.getOrNull(currentIndex)?.let { stepState ->
                                    tts.speak(stepState.text, TextToSpeech.QUEUE_FLUSH, null, "speak_step")
                                }
                                isListening.value = true
                            }
                        } else {
                            voiceController.stop()
                            isListening.value = false
                        }
                    },
                    isLoading = false
                )


                Spacer(modifier = Modifier.height(12.dp))

                // PDF Save Button
                ModernActionButton(
                    text = "📄 PDF로 저장",
                    backgroundColor = Color(0xFFB9806D), // tertiary 색상
                    onClick = {
                        val html = generateRecipeHtml(recipe)
                        saveAsPdfWithHtml(
                            context = context,
                            html = html,
                            filename = recipe.name
                        )
                    },
                    isOutlined = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // AI Evaluation Section
                if (userImageUriForAiEval != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFBE7DF) // surfaceVariant 색상
                        )
                    ) {
                        AsyncImage(
                            model = userImageUriForAiEval,
                            contentDescription = "선택된 AI 평가용 이미지",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                ModernActionButton(
                    text = if (isLoadingAiEval) "AI 분석 중..." else "🤖 AI 요리 평가 받기",
                    backgroundColor = Color(0xFF5C2B1B), // onPrimaryContainer 색상
                    onClick = {
                        pickImageLauncherForAiEval.launch("image/*")
                    },
                    isLoading = isLoadingAiEval,
                    enabled = !isLoadingAiEval
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
                CommentSection(recipeId = recipe.id)
            }
        }
    }
}

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
                    color = Color.White, // 배경색이 진한 색상이므로 흰색 유지
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
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // Color(0xFFE25532).copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) // Color(0xFFE25532).copy(alpha = 0.3f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary, // Color(0xFFE25532)
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
            containerColor = Color(0xFFB9806D).copy(alpha = 0.1f) // tertiary 색상
        ),
        border = BorderStroke(1.dp, Color(0xFFB9806D).copy(alpha = 0.2f)) // tertiary 색상
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFFB9806D), // tertiary 색상
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
            containerColor = Color.White // surface 색상 (그대로 유지)
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
                        Color(0xFFE25532), // primary 색상으로 변경
                        CircleShape
                    )
            )
            Text(
                text = ingredient,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface, // 이미 테마 색상 사용
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), // 이미 테마 색상 사용
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ModernActionButton(
    text: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    isOutlined: Boolean = false
) {
    if (isOutlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, backgroundColor),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = backgroundColor
            ),
            enabled = enabled
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = backgroundColor
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    fontSize = 16.sp
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = Color.White // 진한 배경색에 흰색 텍스트
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            ),
            enabled = enabled
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White // 진한 배경색에 흰색 로딩 표시
                )
            } else {
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
}



@Composable
fun CookingStepCard(
    index: Int,
    step: CookingStepState,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (step.isCurrent) 14.dp else if (step.isDone) 6.dp else 2.dp,
        animationSpec = tween(300)
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (step.isCurrent) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )

    val cardColor by animateColorAsState(
        targetValue = when {
            step.isCurrent -> Color(0xFFFFE2D6) // primaryContainer - 현재 단계
            step.isDone -> Color(0xFFF3E0DC) // tertiaryContainer - 완료된 단계
            else -> Color(0xFFFFFBF8) // background - 대기 중인 단계
        },
        animationSpec = tween(300)
    )

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            step.isCurrent -> Color(0xFFE25532) // primary - 현재 단계
            step.isDone -> Color(0xFFB9806D) // tertiary - 완료된 단계
            else -> Color(0xFFDDC7BD) // outline - 대기 중인 단계
        },
        animationSpec = tween(300)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .scale(animatedScale)
            .shadow(elevation = animatedElevation, shape = RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, cardBorderColor)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // ⏺ 상단 라벨
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "STEP ${index + 1}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = cardBorderColor
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                StatusBadge(isCurrent = step.isCurrent, isDone = step.isDone)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 설명 텍스트
            Text(
                text = step.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = if (step.isCurrent) FontWeight.Medium else FontWeight.Normal,
                    lineHeight = 26.sp,
                    color = Color(0xFF3A2C28) // onBackground 색상
                ),
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // 타이머
            if (step.showTimer && step.timerState != null && step.isCurrent) {
                Spacer(modifier = Modifier.height(20.dp))
                TimerSection(
                    timerState = step.timerState,
                    timerTitle = step.timerTitle,
                    onFinish = onNext,
                    onNext = onNext
                )
            }

            // 버튼
            if (step.isCurrent && !step.isDone) {
                Spacer(modifier = Modifier.height(24.dp))
                CurrentStepActions(
                    onRepeat = onRepeat,
                    onNext = onNext
                )
            }

            // 완료 상태
            if (step.isDone) {
                Spacer(modifier = Modifier.height(18.dp))
                CompletionStatus()
            }
        }
    }
}

@Composable
private fun StepIndicator(
    stepNumber: Int,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    val backgroundColor = when {
        isCompleted -> Color(0xFFB9806D) // tertiary 색상 - 완료된 단계
        isCurrent -> Color(0xFFE25532) // primary 색상 - 현재 단계
        else -> MaterialTheme.colorScheme.surfaceVariant // 대기 중인 단계
    }

    val contentColor = when {
        isCompleted || isCurrent -> Color.White // 진한 배경에 흰색 텍스트
        else -> MaterialTheme.colorScheme.onSurfaceVariant // 테마 색상 사용
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                backgroundColor,
                CircleShape
            )
            .border(
                width = if (isCurrent) 3.dp else 0.dp,
                color = if (isCurrent) Color(0xFFE25532).copy(alpha = 0.3f) else Color.Transparent, // primary 색상
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "완료됨",
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = stepNumber.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            )
        }
    }
}

@Composable
private fun StatusBadge(
    isCurrent: Boolean,
    isDone: Boolean
) {
    if (isCurrent || isDone) {
        val (backgroundColor, textColor, text, icon) = when {
            isCurrent -> Tuple4(
                Color(0xFFE25532).copy(alpha = 0.1f), // primary 색상의 10% 투명도
                Color(0xFFE25532), // primary 색상
                "진행중",
                "🔥"
            )
            isDone -> Tuple4(
                Color(0xFFB9806D).copy(alpha = 0.1f), // tertiary 색상의 10% 투명도
                Color(0xFFB9806D), // tertiary 색상
                "완료",
                "✅"
            )
            else -> Tuple4(Color.Transparent, Color.Transparent, "", "")
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
@Composable
private fun TimerSection(
    timerState: StepTimerState,
    timerTitle: String,
    onFinish: () -> Unit,
    onNext: () -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE1D5)), // secondaryContainer 색상
        border = BorderStroke(1.dp, Color(0xFFE25532).copy(alpha = 0.3f)) // primary 색상
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 타이틀
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⏰", fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
                Text(
                    timerTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE25532) // primary 색상
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 타이머 바 UI
            StepTimer(timerState = timerState, onFinish = {
                isRunning = false
                isPaused = false
                onFinish()
            })

            Spacer(modifier = Modifier.height(12.dp))

            // 버튼 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 시작 또는 재시작 버튼
                ModernTimerButton(
                    text = if (!isRunning && !isPaused) "⏵ 시작" else "▶ 재시작",
                    onClick = {
                        if (!isRunning && !isPaused) {
                            // 시작
                            timerState.start {
                                isRunning = false
                                isPaused = false
                                onFinish()
                            }
                        } else {
                            // 재시작
                            timerState.resume()
                        }
                        isRunning = true
                        isPaused = false
                    },
                    backgroundColor = Color(0xFFB9806D), // tertiary 색상
                    modifier = Modifier.weight(1f)
                )

                // 일시정지 버튼
                ModernTimerButton(
                    text = "⏸ 일시정지",
                    onClick = {
                        timerState.pause()
                        isRunning = false
                        isPaused = true
                    },
                    backgroundColor = Color(0xFF5D4037), // onSecondaryContainer 색상
                    modifier = Modifier.weight(1f),
                    enabled = isRunning
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ModernActionButton(
                text = "➡ 다음 단계",
                onClick = onNext,
                backgroundColor = Color(0xFFE25532), // primary 색상
                textColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@Composable
private fun CurrentStepActions(
    onRepeat: () -> Unit,
    onNext: () -> Unit
) {
    Column {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE25532).copy(alpha = 0.05f) // primary 색상의 5% 투명도
            ),
            border = BorderStroke(1.dp, Color(0xFFE25532).copy(alpha = 0.2f)) // primary 색상의 20% 투명도
        ) {
            Text(
                "🎯 현재 단계를 진행 중입니다",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFE25532), // primary 색상
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ModernActionButton(
                text = "🔁 다시 읽기",
                onClick = onRepeat,
                backgroundColor = Color(0xFFE25532).copy(alpha = 0.1f), // primary 색상의 10% 투명도
                textColor = Color(0xFFE25532), // primary 색상
                isOutlined = true,
                modifier = Modifier.weight(1f)
            )

            ModernActionButton(
                text = "➡ 다음 단계",
                onClick = onNext,
                backgroundColor = Color(0xFFE25532), // primary 색상
                textColor = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompletionStatus() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFB9806D).copy(alpha = 0.1f) // tertiary 색상의 10% 투명도
        ),
        border = BorderStroke(1.dp, Color(0xFFB9806D).copy(alpha = 0.3f)) // tertiary 색상의 30% 투명도
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                "✅",
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                "단계 완료됨",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFB9806D), // tertiary 색상
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun ModernTimerButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White // 진한 배경색에 흰색 텍스트
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            fontSize = 13.sp
        )
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
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(2.dp, backgroundColor),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = textColor
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                fontSize = 14.sp
            )
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = textColor
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 1.dp
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                fontSize = 14.sp
            )
        }
    }
}

// Helper data class for multiple return values
private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)


fun saveAsPdfWithHtml(context: Context, html: String, filename: String = "recipe") {
    val webView = WebView(context)
    webView.settings.javaScriptEnabled = false
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            val activity = context as? Activity
            if (activity == null) {
                Toast.makeText(context, "PDF 저장 실패: Activity context가 아닙니다.", Toast.LENGTH_SHORT).show()
                Log.e("PDFSave", "Context is not an Activity context.")
                return
            }

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val pdfFileName = "${filename.replace(" ", "_")}_${sdf.format(Date())}.pdf"

            val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "PDF 저장 실패: PrintManager를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                Log.e("PDFSave", "PrintManager is null.")
                return
            }

            try {
                val printAdapter = webView.createPrintDocumentAdapter(pdfFileName)
                val jobName = "${context.packageName}_RecipeDocument"
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                Log.i("PDFSave", "Print job initiated for $pdfFileName")
            } catch (e: Exception) {
                Toast.makeText(context, "PDF 저장 중 오류가 발생했습니다: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("PDFSave", "Error starting print job", e)
            }
        }

        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            Log.e("PDFSave", "WebView error while creating PDF: $errorCode - $description on $failingUrl")
            Toast.makeText(context, "PDF 생성 중 WebView 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}




// evaluateCookingAI 함수 전체를 아래 코드로 교체해주세요.
// (다른 함수들: uriToByteArray, urlToByteArray, byteArrayToBase64 등은 기존 코드 그대로 둡니다.)

fun evaluateCookingAI(context: Context, recipeImageUrl: String, userImageUri: Uri) {
    val uiScope = CoroutineScope(Dispatchers.Main) // UI 작업을 위한 스코프

    Log.d("AI_Eval_Flow", "evaluateCookingAI 함수 시작. 원본 이미지 URL: $recipeImageUrl, 사용자 이미지 URI: $userImageUri")

    if (recipeImageUrl.isBlank()) {
        Log.e("AI_Eval_Input", "원본 레시피 이미지 URL이 비어있습니다.")
        Toast.makeText(context, "원본 레시피 이미지 정보가 없습니다.", Toast.LENGTH_LONG).show()
        return
    }

    uiScope.launch {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e("AI_Eval_Auth", "사용자가 로그인되어 있지 않습니다.")
                Toast.makeText(context, "로그인이 필요한 기능입니다.", Toast.LENGTH_LONG).show()
                return@launch
            }
            Log.d("AI_Eval_Auth", "User UID: ${currentUser.uid}. ID 토큰 새로고침 시도...")

            currentUser.getIdToken(true)
                .addOnCompleteListener { tokenTask ->
                    if (tokenTask.isSuccessful) {
                        val idToken = tokenTask.result?.token
                        Log.d("AI_Eval_Auth", "ID 토큰 새로고침 성공. 토큰(앞 20자): ${idToken?.take(20)}")

                        uiScope.launch { // 새로운 코루틴 시작 (suspend 함수 호출용)
                            var referenceImageBase64 = ""
                            var userImageBase64 = ""
                            var successLoadingImages = false

                            try {
                                // --- 실제 이미지 로딩 및 변환 로직으로 복원 ---
                                Log.d("AI_Eval_Image", "원본 레시피 이미지 로드 시도: $recipeImageUrl")
                                val referenceBytes = urlToByteArray(recipeImageUrl) // suspend 함수
                                if (referenceBytes.isEmpty()) {
                                    Log.e("AI_Eval_Image", "원본 레시피 이미지 -> 바이트 배열 변환 실패 (결과 비어있음). URL: $recipeImageUrl")
                                    Toast.makeText(context, "원본 레시피 이미지를 불러오는 데 실패했습니다.", Toast.LENGTH_LONG).show()
                                    return@launch // 현재 코루틴 종료
                                }
                                referenceImageBase64 = byteArrayToBase64(referenceBytes)
                                Log.d("AI_Eval_Image_Content", "원본 레시피 이미지 Base64 변환 완료. 길이: ${referenceImageBase64.length}")
                                if (referenceImageBase64.isBlank()) {
                                    Log.e("AI_Eval_Image_Content", "원본 레시피 이미지 Base64 문자열이 비어있거나 공백입니다.")
                                    Toast.makeText(context, "원본 레시피 이미지 데이터 변환에 실패했습니다.", Toast.LENGTH_LONG).show()
                                    return@launch
                                }

                                Log.d("AI_Eval_Image", "사용자 이미지 로드 시도: $userImageUri")
                                val userBytes = uriToByteArray(context, userImageUri) // suspend 함수
                                if (userBytes.isEmpty()) {
                                    Log.e("AI_Eval_Image", "사용자 이미지 -> 바이트 배열 변환 실패 (결과 비어있음). URI: $userImageUri")
                                    Toast.makeText(context, "선택한 사용자 이미지를 불러오는 데 실패했습니다.", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                userImageBase64 = byteArrayToBase64(userBytes)
                                Log.d("AI_Eval_Image_Content", "사용자 이미지 Base64 변환 완료. 길이: ${userImageBase64.length}")
                                if (userImageBase64.isBlank()) {
                                    Log.e("AI_Eval_Image_Content", "사용자 이미지 Base64 문자열이 비어있거나 공백입니다.")
                                    Toast.makeText(context, "사용자 이미지 데이터 변환에 실패했습니다.", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                // --- 실제 이미지 로딩 및 변환 로직 복원 끝 ---

                                successLoadingImages = true // 모든 이미지 로드 및 변환 성공

                            } catch (e: Exception) {
                                Log.e("AI_Eval_Image_Exception", "이미지 로딩/변환 중 예외 발생", e)
                                Toast.makeText(context, "이미지 처리 중 오류: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                return@launch // 예외 발생 시 현재 코루틴 종료
                            }

                            if (successLoadingImages) {
                                val commonMimeType = "image/jpeg" // 실제 MIME 타입 감지 로직 추가 권장

                                // --- 원래 필드명으로 inputData 구성 ---
                                val inputData = hashMapOf(
                                    "userImageBase64" to userImageBase64,
                                    "referenceImageBase64" to referenceImageBase64,
                                    "mimeTypeUser" to commonMimeType, // 필요하다면 실제 이미지에서 MIME 타입 추출
                                    "mimeTypeReference" to commonMimeType // 필요하다면 실제 이미지에서 MIME 타입 추출
                                )
                                Log.d("AI_Eval_Params", "Cloud Function 입력 데이터 준비 완료 (실제 데이터). UserImgLen: ${userImageBase64.length}, RefImgLen: ${referenceImageBase64.length}")
                                // --- 원래 필드명으로 inputData 구성 끝 ---

                                Log.d("AI_Eval_Call", "Cloud Function 'evaluateDish' 호출 시작 (실제 데이터)...")
                                Firebase.functions("us-central1")
                                    .getHttpsCallable("evaluateDish")
                                    .call(inputData) // 원래 inputData 사용
                                    .addOnSuccessListener { result ->
                                        val resultData = result.getData()
                                        Log.d("AI_Result_Success_Raw", "Raw result data (실제 테스트): $resultData")
                                        val evaluationData = resultData as? Map<String, Any>
                                        val evaluationText = evaluationData?.get("evaluation") as? String
                                        Log.d("AI_Result_Success", "AI 평가 결과 텍스트 (실제 테스트): $evaluationText")

                                        if (!evaluationText.isNullOrBlank()) {
                                            Toast.makeText(context, "AI 요리 평가:\n$evaluationText", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "AI 평가 결과를 받았지만 내용이 비어있습니다.", Toast.LENGTH_LONG).show()
                                            Log.w("AI_Result_Success", "평가 결과 텍스트가 null이거나 비어있습니다.")
                                        }
                                    }
                                    .addOnFailureListener { ex ->
                                        Log.e("AI_Result_Failure", "Cloud Function 호출 실패 (실제 테스트)", ex)
                                        val errorMessage = if (ex is FirebaseFunctionsException) {
                                            "AI 평가 오류 (Code: ${ex.code}): ${ex.message}"
                                        } else {
                                            "AI 평가 중 알 수 없는 오류: ${ex.localizedMessage}"
                                        }
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    }
                            } else {
                                Log.e("AI_Eval_Flow", "이미지 로드/변환 실패로 Cloud Function 호출을 진행하지 않습니다.")
                            }
                        }
                    } else {
                        Log.e("AI_Eval_Auth", "ID 토큰 새로고침 실패.", tokenTask.exception)
                        Toast.makeText(context, "인증 정보 갱신에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: Exception) {
            Log.e("AI_Eval_Outer_Exception", "evaluateCookingAI 함수 로직 외부에서 예외 발생", e)
            Toast.makeText(context, "AI 평가 준비 중 오류: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

suspend fun uriToByteArray(context: Context, uri: Uri): ByteArray {
    return withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.readBytes() ?: ByteArray(0)
    }
}

suspend fun urlToByteArray(url: String): ByteArray {
    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            inputStream.readBytes()
        } catch (e: Exception) {
            Log.e("ByteArrayError", "URL 변환 실패: ${e.message}")
            ByteArray(0)
        }
    }
}




