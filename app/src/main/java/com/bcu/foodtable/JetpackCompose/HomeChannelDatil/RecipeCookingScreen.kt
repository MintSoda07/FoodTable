package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import StepTimerState
import android.app.Activity
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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bcu.foodtable.useful.RecipeItem // RecipeItem에 ingredients: List<String> 필드가 있다고 가정
import com.bcu.foodtable.voice.VoiceCommandController
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    val context = LocalContext.current
    val tts = remember {
        TextToSpeech(context, null).apply {
            language = Locale.KOREAN
        }
    }
    Log.d("RecipeOrderRaw", recipe.order)


    val recipeId = recipe.id.ifBlank { UUID.randomUUID().toString() }
    var steps by remember {
        mutableStateOf(
            recipe.order.split("○")
                .filter { it.isNotBlank() }
                .mapIndexed { index, raw ->
                    val regex = Regex("""^\s*○?\s*\d+\.\s*\(([^)]+)\)\s*(.*?)(?:\s*\(([^()]+?),\s*([0-9]{2}:[0-9]{2}:[0-9]{2})\))?$""")
                    val match = regex.find(raw.trim())

                    val title = match?.groupValues?.getOrNull(1) ?: ""
                    val description = match?.groupValues?.getOrNull(2) ?: raw
                    val method = match?.groupValues?.getOrNull(3) ?: ""
                    val duration = match?.groupValues?.getOrNull(4) ?: ""

                    Log.d("✅ StepParser", "🟨 raw=$raw")
                    Log.d("✅ StepParser", "🟩 index=$index | title=$title | method=$method | duration=$duration | showTimer=${method.isNotEmpty() && duration.isNotEmpty()}")

                    CookingStepState(
                        text = "$title: $description",
                        showTimer = method.isNotEmpty() && duration.isNotEmpty(),
                        timerTitle = method,
                        timerDuration = duration,
                        isCurrent = index == 0,
                                timerState = if (duration.isNotEmpty()) StepTimerState(parseDuration(duration)) else null
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
                    // 실제 타이머 기능 연동 시 이 부분 수정 필요
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
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 28.sp
                    )
                )
                AsyncImage(
                    model = recipe.imageResId,
                    contentDescription = recipe.name, // contentDescription에 레시피 이름 추가
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shadow(8.dp)
                        .padding(vertical = 8.dp)
                        .animateContentSize()
                )
                Text(
                    "설명: ${recipe.description}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 24.sp
                    )
                )
                Text(
                    "예상 칼로리: ${recipe.estimatedCalories}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontStyle = FontStyle.Italic
                    )
                )
                Text(
                    "카테고리: ${recipe.C_categories.joinToString()}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontStyle = FontStyle.Italic
                    )
                )
                Text(
                    text = "태그: " + recipe.tags.joinToString(" ") { tag ->
                        if (tag.startsWith("#")) tag else "#$tag"
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontStyle = FontStyle.Italic
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- START: 재료 목록 섹션 ---
                // RecipeItem에 ingredients: List<String> 필드가 있다고 가정합니다.
                // 실제 RecipeItem의 재료 필드명으로 'recipe.ingredients'를 사용하거나 맞게 수정해주세요.
                if (recipe.ingredients.isNotEmpty()) {
                    Text(
                        "재료",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Column {
                        recipe.ingredients.forEach { ingredient ->
                            Text(
                                text = "• $ingredient",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val encodedQuery = Uri.encode(ingredient)
                                        val url = "https://search.shopping.naver.com/search/all?query=$encodedQuery"
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse(url)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast
                                                .makeText(context, "웹 브라우저를 열 수 없습니다.", Toast.LENGTH_SHORT)
                                                .show()
                                            Log.e("RecipeCookingScreen", "네이버 쇼핑 링크 열기 오류: $e")
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // --- END: 재료 목록 섹션 ---

                LikeButton(recipeId = recipeId) // LikeButton은 별도 파일에 정의되어 있다고 가정
            }

            itemsIndexed(steps, key = { index, step -> "$index-${step.text}-${step.isCurrent}-${step.isDone}" }) { index, step -> // key를 좀 더 고유하게 변경
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
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 24.sp
                        ),
                        modifier = Modifier.padding(vertical = 16.dp)
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening.value) Color.Red else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .animateContentSize()
                ) {
                    Text(if (isListening.value) "음성 명령 중지" else "음성 명령 시작", color = Color.White)
                }
            }

            // generateRecipeHtml 함수는 별도 파일에 정의되어 있다고 가정
            val html = generateRecipeHtml(recipe)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        saveAsPdfWithHtml(
                            context = context,
                            html = html,
                            filename = recipe.name
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📄 PDF 저장")
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                CommentSection(recipeId = recipeId) // CommentSection은 별도 파일에 정의되어 있다고 가정
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
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            )
        )
        step.isDone -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.surfaceVariant
            )
        )
        else -> Brush.horizontalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            )
        )
    }

    Card(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .background(cardBackground)
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (step.isDone) Icons.Default.Check else Icons.Default.Circle,
                    contentDescription = if (step.isDone) "완료된 단계" else "현재 단계 표시기",
                    tint = if (step.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "단계 ${index + 1}.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                step.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            if (step.showTimer && step.timerState != null) {
                Spacer(modifier = Modifier.height(12.dp))
                StepTimer(
                    timerState = step.timerState,
                    onFinish = onNext
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { step.timerState.pause() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⏸ 일시정지")
                    }

                    OutlinedButton(
                        onClick = { step.timerState.resume() },
                        modifier = Modifier.weight(1f)
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
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onRepeat,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🔁 다시 읽기")
                    }

                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
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
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
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
            val pdfFileName = "${filename.replace(" ", "_")}_${sdf.format(Date())}.pdf" // 파일 이름 형식 약간 변경

            val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "PDF 저장 실패: PrintManager를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                Log.e("PDFSave", "PrintManager is null.")
                return
            }

            try {
                val printAdapter = webView.createPrintDocumentAdapter(pdfFileName)
                val jobName = "${context.packageName}_RecipeDocument" // Job 이름 구체화
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                // 성공 메시지는 시스템에서 처리하므로 앱 토스트는 생략 가능
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

