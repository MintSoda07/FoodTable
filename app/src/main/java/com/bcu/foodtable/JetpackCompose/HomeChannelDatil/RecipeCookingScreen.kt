package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.voice.VoiceCommandController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// 상태 데이터 클래스
data class CookingStepState(
    val text: String,
    val isDone: Boolean = false,
    val showTimer: Boolean = false,
    val timerTitle: String = "",
    val timerDuration: String = "",
    val isCurrent: Boolean = false,
    val timerState: StepTimerState? = null
)


@Composable
fun RecipeCookingScreen(recipe: RecipeItem) {
    val context = LocalContext.current
    val tts = remember {
        TextToSpeech(context, null).apply {
            language = Locale.KOREAN
        }
    }

    val recipeId = recipe.id.ifBlank { UUID.randomUUID().toString() }
    var steps by remember {
        mutableStateOf(
            recipe.order.split("○")
                .filter { it.isNotBlank() }
                .mapIndexed { index, raw ->
                    val regex = Regex("""\d+\.\s*\((.*?)\)\s*(.*?)(?:\(([^,]+),([^)]+)\))?$""")
                    val match = regex.find(raw)
                    val title = match?.groupValues?.get(1) ?: ""
                    val description = match?.groupValues?.get(2) ?: raw
                    val method = match?.groupValues?.getOrNull(3) ?: ""
                    val duration = match?.groupValues?.getOrNull(4) ?: ""
                    CookingStepState(
                        text = "$title: $description",
                        showTimer = method.isNotEmpty() && duration.isNotEmpty(),
                        timerTitle = method,
                        timerDuration = duration,
                        isCurrent = index == 0
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
                    tts.speak("타이머 기능은 아직 구현되지 않았습니다.", TextToSpeech.QUEUE_FLUSH, null, "timer")
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
                    contentDescription = null,
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
                LikeButton(recipeId = recipeId)
            }

            itemsIndexed(steps, key = { index, step -> "$index-${step.isCurrent}" }) { index, step ->
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
                        )
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
                CommentSection(recipeId = recipeId)
            }
        }
    }
}

@Composable
fun CookingStepCard(index: Int, step: CookingStepState, onNext: () -> Unit, onRepeat: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .animateContentSize()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (step.isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (step.isDone) Icons.Default.Check else Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (step.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("○${index + 1}. ${step.text}", style = MaterialTheme.typography.bodyLarge)
            }
            if (step.showTimer) {
                StepTimer(durationString = step.timerDuration)
            }
            if (step.isCurrent) {
                Text("✅ 현재 단계입니다", color = MaterialTheme.colorScheme.primary)
                Row {
                    Button(onClick = onRepeat) { Text("읽기") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onNext) { Text("다음") }
                }
            } else if (step.isDone) {
                Text("✅ 완료됨", color = MaterialTheme.colorScheme.secondary)
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
                Toast.makeText(context, "PDF 저장 실패: Activity context 아님", Toast.LENGTH_SHORT).show()
                return
            }

            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val pdfFileName = "$filename-${sdf.format(Date())}.pdf"

            val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val printAdapter = webView.createPrintDocumentAdapter(pdfFileName)
            val jobName = "Recipe PDF"

            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            Toast.makeText(context, "PDF 저장 요청이 시작되었습니다", Toast.LENGTH_SHORT).show()
        }
    }
}
