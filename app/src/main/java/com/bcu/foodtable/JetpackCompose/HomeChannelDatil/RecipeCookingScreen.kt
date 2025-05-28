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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bcu.foodtable.TTS.CookingAiViewModel
import com.bcu.foodtable.TTS.CookingAiViewModelFactory
import com.google.firebase.functions.FirebaseFunctionsException

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
fun RecipeCookingScreen(recipe: RecipeItem) {
    val application = LocalContext.current.applicationContext as Application
    val firebaseFunctionsInstance = remember { Firebase.functions("us-central1") }
    val aiViewModelFactory = remember { CookingAiViewModelFactory(application, firebaseFunctionsInstance) }
    val aiViewModel: CookingAiViewModel = viewModel(key = "aiEvaluationViewModel", factory = aiViewModelFactory) // key는 선택 사항
    val context = LocalContext.current
    // ViewModel 상태 관찰
    val isLoadingAiEval by aiViewModel.isLoading.collectAsState()
    val aiEvaluationResultText by aiViewModel.evaluationApiResult.collectAsState()
    val aiEvalToastMessage by aiViewModel.toastMessage.collectAsState()
    LaunchedEffect(aiEvalToastMessage) {
        aiEvalToastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            aiViewModel.clearToastMessage() // 메시지 표시 후 ViewModel에서 초기화
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
        userImageUriForAiEval = uri // UI 업데이트용 (선택한 이미지 미리보기 등)
        uri?.let { selectedUserImageUri ->
            // CRITICAL: recipe.imageResId가 레시피 원본 이미지의 완전한 HTTP/HTTPS URL 문자열이어야 합니다.
            val recipeImageStringUrl = recipe.imageResId // 이 변수가 실제 URL인지 확인!
            if (recipeImageStringUrl.startsWith("http")) { // 간단한 URL 형식 체크
                aiViewModel.evaluateCookingRecipe(recipeImageStringUrl, selectedUserImageUri)
            } else {
                Toast.makeText(context, "레시피 원본 이미지 URL이 유효하지 않습니다. (예: http...)", Toast.LENGTH_LONG).show()
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

                    Log.d("✅ StepParser", "🟨 raw=$raw")
                    Log.d(
                        "✅ StepParser",
                        "🟩 index=$index | title=$title | method=$method | duration=$duration | showTimer=${method.isNotEmpty() && duration.isNotEmpty()}"
                    )

                    CookingStepState(
                        text = "$title: $description",
                        showTimer = method.isNotEmpty() && duration.isNotEmpty(),
                        timerTitle = method,
                        timerDuration = duration,
                        isCurrent = index == 0,
                        timerState = if (duration.isNotEmpty()) StepTimerState(
                            parseDuration(
                                duration
                            )
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


    fun goToNextStep() {
        if (currentIndex + 1 < steps.size) {
            steps = steps.mapIndexed { index, step ->
                when (index) {
                    currentIndex -> step.copy(isDone = true, isCurrent = false)
                    currentIndex + 1 -> step.copy(isCurrent = true)
                    else -> step
                }
            }
            currentIndex++
            tts.speak(steps[currentIndex].text, TextToSpeech.QUEUE_FLUSH, null, "step")
        } else {
            steps = steps.mapIndexed { index, step ->
                if (index == currentIndex) step.copy(isDone = true, isCurrent = false) else step
            }
            isFinished = true
            tts.speak("모든 조리 과정을 완료했습니다.", TextToSpeech.QUEUE_FLUSH, null, "done")
        }
    }

    fun repeatStep() {
        tts.speak(steps[currentIndex].text, TextToSpeech.QUEUE_FLUSH, null, "repeat")
    }

    val voiceController = remember {
        VoiceCommandController(
            context = context,
            tts = tts,
            onCommand = {} // 빈 람다로 초기화
        )
    }

    LaunchedEffect(Unit) {
        voiceController.onCommand = { command: VoiceCommandController.CommandType ->
            when (command) {
                VoiceCommandController.CommandType.NEXT -> goToNextStep()
                VoiceCommandController.CommandType.REPEAT -> repeatStep()
                VoiceCommandController.CommandType.STOP -> {
                    tts.speak("음성 명령을 중지합니다.", TextToSpeech.QUEUE_FLUSH, null, "stop")
                }

                VoiceCommandController.CommandType.TIMER -> {
                    tts.speak("타이머 기능은 아직 완전히 연동되지 않았습니다.", TextToSpeech.QUEUE_FLUSH, null, "timer")
                }

                VoiceCommandController.CommandType.NONE -> {
                    tts.speak("명령을 이해하지 못했습니다.", TextToSpeech.QUEUE_FLUSH, null, "fail")
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient( // Adjusted gradient
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    ),
                    startY = 0f,
                    endY = 800f // Adjust endY for smoother transition over a larger area
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp), // Main content padding
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = 32.dp
            ) // Padding for scrollable content
        ) {
            item {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.displaySmall.copy( // Enhanced title style
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                AsyncImage(
                    model = recipe.imageResId,
                    contentDescription = recipe.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp) // Slightly taller image
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(20.dp)) // More rounded corners
                        .border( // Added subtle border
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(20.dp)
                        )
                        .shadow(6.dp, RoundedCornerShape(20.dp)) // Adjusted shadow
                        .animateContentSize()
                )
                Text(
                    "설명: ${recipe.description}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 26.sp // Increased line height for readability
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Text(
                    "예상 칼로리: ${recipe.estimatedCalories}",
                    style = MaterialTheme.typography.bodyMedium.copy( // Changed from Italic
                        color = MaterialTheme.colorScheme.onSurfaceVariant // Softer color
                    ),
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    "카테고리: ${recipe.C_categories.joinToString()}",
                    style = MaterialTheme.typography.bodyMedium.copy( // Changed from Italic
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = "태그: " + recipe.tags.joinToString(" ") { tag ->
                        if (tag.startsWith("#")) tag else "#$tag"
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy( // Changed from Italic
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(modifier = Modifier.height(20.dp)) // Increased spacer

                if (recipe.ingredients.isNotEmpty()) {
                    Text(
                        "재료",
                        style = MaterialTheme.typography.titleLarge.copy( // Enhanced style
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    Divider( // Added divider
                        modifier = Modifier.padding(bottom = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                    Column(modifier = Modifier.padding(bottom = 8.dp)) { // Added bottom padding to Column
                        recipe.ingredients.forEach { ingredient ->
                            Text(
                                text = "• $ingredient",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 22.sp // Adjusted line height
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val encodedQuery = Uri.encode(ingredient)
                                        val url =
                                            "https://search.shopping.naver.com/search/all?query=$encodedQuery"
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse(url)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "웹 브라우저를 열 수 없습니다.",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                            Log.e("RecipeCookingScreen", "네이버 쇼핑 링크 열기 오류: $e")
                                        }
                                    }
                                    .padding(vertical = 7.dp, horizontal = 8.dp) // Adjusted padding
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp)) // Increased spacer
                }
                LikeButton(recipeId = recipeId)
                Spacer(modifier = Modifier.height(16.dp)) // Spacer before step list
            }

            itemsIndexed(
                steps,
                key = { index, step -> "$index-${step.text}-${step.isCurrent}-${step.isDone}" }) { index, step ->
                CookingStepCard(
                    index = index,
                    step = step,
                    onNext = { goToNextStep() },
                    onRepeat = { repeatStep() }
                )
            }

            if (isFinished) {
                item {
                    Text(
                        "🎉 모든 조리 과정을 완료했습니다!",
                        style = MaterialTheme.typography.headlineSmall.copy( // Adjusted style
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(
                            vertical = 24.dp,
                            horizontal = 8.dp
                        ) // Adjusted padding
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (!isListening.value) {
                            voiceController.startListening()
                            isListening.value = true
                        } else {
                            voiceController.stop()
                            isListening.value = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp), // More rounded shape
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening.value) Color.Red.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp) // Standardized height
                        .padding(vertical = 8.dp)
                        .animateContentSize()
                ) {
                    Text(
                        if (isListening.value) "음성 명령 중지" else "음성 명령 시작",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }

            val html = generateRecipeHtml(recipe)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton( // Changed to OutlinedButton for variety
                    onClick = {
                        saveAsPdfWithHtml(
                            context = context,
                            html = html,
                            filename = recipe.name
                        )
                    },
                    shape = RoundedCornerShape(12.dp), // More rounded shape
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp) // Standardized height
                        .padding(vertical = 4.dp)
                ) {
                    Text("📄 PDF 저장", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                // (선택 사항) 사용자 이미지 미리보기 - 이 부분은 버튼 위에 추가할 수 있습니다.
                if (userImageUriForAiEval != null) {
                    AsyncImage( // coil.compose.AsyncImage 임포트 필요
                        model = userImageUriForAiEval,
                        contentDescription = "선택된 AI 평가용 이미지",
                        modifier = Modifier
                            .size(100.dp) // 원하는 크기로 조절
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                        // .align(Alignment.CenterHorizontally) // LazyColumn의 item 내부에서는 직접 align이 어려울 수 있음. 필요시 Box로 감싸서 정렬
                    )
                }
                Button(
                    onClick = {
                        // 이 버튼은 이제 이미지 선택기를 실행합니다.
                        // 실제 평가는 pickImageLauncherForAiEval 콜백에서 ViewModel을 통해 이루어집니다.
                        pickImageLauncherForAiEval.launch("image/*")
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        // 버튼 색상을 다른 주요 액션 버튼과 구분하기 위해 변경 가능 (예: secondary)
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    enabled = !isLoadingAiEval // ViewModel의 로딩 상태 사용
                ) {
                    Text(
                        if (isLoadingAiEval) "AI 평가 중..." else "🤖 AI 요리 사진 평가 받기", // 로딩 상태에 따라 텍스트 변경
                        color = Color.White, // 또는 MaterialTheme.colorScheme.onSecondary
                        fontSize = 16.sp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                CommentSection(recipeId = recipe.id)
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
    Log.d(
        "CookingStepCardCheck",
        "index=$index | isCurrent=${step.isCurrent} | showTimer=${step.showTimer} | duration=${step.timerDuration}"
    )

    val cardBackground = when {
        step.isCurrent -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), // Slightly more pronounced
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            )
        )
        step.isDone -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), // Adjusted for completed look
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )
        else -> Brush.horizontalGradient( // Subtle for upcoming steps
            listOf(
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                MaterialTheme.colorScheme.surfaceColorAtElevation(0.5.dp)
            )
        )
    }

    Card(
        modifier = Modifier
            .padding(vertical = 8.dp) // Consistent vertical padding
            .fillMaxWidth()
            .animateContentSize()
            .then( // Conditional border for current step
                if (step.isCurrent) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp), // More rounded corners
        elevation = CardDefaults.cardElevation(defaultElevation = if (step.isCurrent) 3.dp else 1.dp), // Subtle elevation
        colors = CardDefaults.cardColors(containerColor = Color.Transparent) // To allow modifier.background to show
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackground) // Apply dynamic background here
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (step.isDone) Icons.Default.Check else Icons.Default.Circle,
                    contentDescription = if (step.isDone) "완료된 단계" else "현재 단계 표시기",
                    tint = when { // Adjusted tint logic
                        step.isDone -> MaterialTheme.colorScheme.primary
                        step.isCurrent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(22.dp) // Slightly larger icon
                )
                Spacer(modifier = Modifier.width(10.dp)) // Adjusted spacer
                Text(
                    "단계 ${index + 1}.",
                    style = MaterialTheme.typography.titleMedium.copy( // Bolder title
                        fontWeight = FontWeight.Bold,
                        color = if (step.isCurrent || step.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp)) // Adjusted spacer

            Text(
                step.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp, // Better line height
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(start = 32.dp) // Indent text
            )

            if (step.showTimer && step.timerState != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.padding(start = 32.dp)) { // Indent Timer
                    StepTimer(
                        timerState = step.timerState,
                        onFinish = onNext
                    )
                }


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 32.dp), // Indent buttons
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { step.timerState.pause() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp), // More rounded
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    ) {
                        Text("⏸ 일시정지")
                    }

                    OutlinedButton(
                        onClick = { step.timerState.resume() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp), // More rounded
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    ) {
                        Text("▶ 다시시작")
                    }
                }
            }

            if (step.isCurrent && !step.isDone) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "현재 단계입니다.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 32.dp) // Indent text
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 32.dp) // Indent buttons
                ) {
                    OutlinedButton(
                        onClick = onRepeat,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp), // More rounded
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary) // Stronger border for primary action
                    ) {
                        Text("🔁 다시 읽기")
                    }

                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp), // More rounded
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("➡ 다음 단계", color = Color.White)
                    }
                }
            }

            if (step.isDone) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "✅ 완료됨",
                    style = MaterialTheme.typography.bodyMedium.copy( // Consistent typography
                        color = MaterialTheme.colorScheme.primary, // Use primary for positive feedback
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(start = 32.dp) // Indent text
                )
            }
        }
    }
}


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




