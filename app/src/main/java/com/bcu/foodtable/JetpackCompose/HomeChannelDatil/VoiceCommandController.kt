package com.bcu.foodtable.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.useful.ApiKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class VoiceCommandController(
    private val context: Context,
    private val tts: TextToSpeech,
     onCommand: (CommandType) -> Unit
) {

    enum class CommandType {
        NEXT, REPEAT, STOP, TIMER, NONE
    }
    var onCommand: (CommandType) -> Unit = onCommand
    private var currentStepIndex = 0
    private val steps = listOf("1단계: 재료 준비", "2단계: 요리 시작", "3단계: 마무리")
    private var isTimerRunning = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "음성 인식이 지원되지 않습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(context as Activity, arrayOf(permission), 100)
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!text.isNullOrEmpty()) {
                        Log.d("VoiceCommand", "인식된 텍스트: $text")
                        analyzeIntent(text)
                    }
                    isListening = false
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                        SpeechRecognizer.ERROR_CLIENT -> "클라이언트 오류"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 부족"
                        SpeechRecognizer.ERROR_NETWORK -> "네트워크 오류"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                        SpeechRecognizer.ERROR_NO_MATCH -> "일치하는 결과 없음"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "인식기 바쁨"
                        SpeechRecognizer.ERROR_SERVER -> "서버 오류"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "사용자 발화 없음"
                        else -> "알 수 없는 오류 ($error)"
                    }
                    Log.e("VoiceCommand", "음성 인식 오류: $message")
                    Toast.makeText(context, "음성 인식 오류: $message", Toast.LENGTH_SHORT).show()
                    isListening = false
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onEndOfSpeech() {}
                override fun onBeginningOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onRmsChanged(rmsdB: Float) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN)
        }

        speechRecognizer?.startListening(intent)
        isListening = true
    }

    fun stop() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        isListening = false
    }

    fun isRunning(): Boolean = isListening

    private fun analyzeIntent(userText: String) {
        val local = userText.lowercase(Locale.ROOT)

        val keyword = when {
            "다음" in local || "계속" in local -> CommandType.NEXT
            "반복" in local || "다시" in local -> CommandType.REPEAT
            "그만" in local || "멈춰" in local -> CommandType.STOP
            "타이머" in local || "시간" in local || "재줘" in local || "시작" in local -> CommandType.TIMER
            else -> null
        }

        if (keyword != null) {
            onCommand(keyword)
            return
        }

        val apiKey = ApiKeyManager.getGptApi()
        if (apiKey == null) {
            Toast.makeText(context, "GPT API 키가 없습니다.", Toast.LENGTH_SHORT).show()
            stop()
            return
        }

        val prompt = """
            다음 발화는 요리 도우미 앱에서 사용된 음성 명령입니다. 
            의도를 아래 중 하나로 분류하세요 (소문자 단어만): 
            next, repeat, stop, timer, none

            발화: "$userText"
        """.trimIndent()

        val client = OpenAIClient().apply { apiKeyInfo = apiKey }

        CoroutineScope(Dispatchers.IO).launch {
            client.sendMessage(
                prompt = prompt,
                role = "음성 명령 분류기",
                onSuccess = { result ->
                    val command = when (result.trim()) {
                        "next" -> CommandType.NEXT
                        "repeat" -> CommandType.REPEAT
                        "stop" -> CommandType.STOP
                        "timer" -> CommandType.TIMER
                        else -> CommandType.NONE
                    }

                    if (command == CommandType.NONE) {
                        tts.speak("명령을 이해하지 못했습니다. 다시 말해주세요.", TextToSpeech.QUEUE_FLUSH, null, "UNKNOWN")
                    } else {
                        onCommand(command)
                    }
                },
                onError = {
                    Log.e("VoiceCommand", "GPT 분석 실패: $it")
                    tts.speak("음성 명령 분석에 실패했습니다.", TextToSpeech.QUEUE_FLUSH, null, "FAIL")
                }
            )
        }
    }

}
