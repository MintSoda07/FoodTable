package com.bcu.foodtable.voice

import StepTimerState
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bcu.foodtable.ai.OpenAIClient
import com.bcu.foodtable.useful.ApiKey
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

    // 타이머 상태를 외부에서 세팅해 주도록 함
    var stepTimerState: StepTimerState? = null

    private var keepListening = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private var originalSystemVolume: Int = 0
    private var originalNotificationVolume: Int = 0
    private var isBeepMuted = false         // ① 삑 소음 음소거 여부 확인 플래그
    private var hasRestoredBeep = false     // ② 한 번이라도 복원했는지 확인
    /**
     * 음성 인식을 실제로 시작했으면 true,
     * 권한 요청만 했거나 실패했으면 false를 반환
     */
    fun startListening(): Boolean {

        // 1) 실제로 인식 가능한 기기인지 확인
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "음성 인식이 지원되지 않습니다", Toast.LENGTH_SHORT).show()
            return false
        }

        // 2) 권한이 없으면 요청만 하고 false 반환
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(context, permission)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(permission),
                REQUEST_RECORD_AUDIO
            )
            return false
        }
        if (!isBeepMuted) {
            muteSystemBeep()
        }
        // 3) 권한이 있고, 음성 인식을 시작할 준비가 되었으므로 keepListening=true
        keepListening = true

        // 4) 기존 recognizer가 있으면 해제
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onResults(results: Bundle?) {
                    isListening = false

                    // 비프음 복원 후 다시 음소거 (필요시 사용)
                    restoreSystemBeep()
                    muteSystemBeep()

                    // 1) 인식된 문자열 가져오기
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()   // 공백 제거

                    // 2) 만약 text가 null 이거나 빈 문자열("") 이라면
                    if (text.isNullOrEmpty()) {
                        // --> 아무 말도 인식되지 않은 상태이므로, analyzeIntent를 호출하지 않고 리턴
                        if (keepListening) {
                            // 재시작만 하고 TTS는 호출하지 않음
                            startListening()
                        }
                        return
                    }

                    // 3) 그 외 정상적인 텍스트가 들어왔을 때만 analyzeIntent()
                    Log.d("VoiceCommand", "인식된 텍스트: $text")
                    analyzeIntent(text)

                    // 4) 계속 듣기 모드 유지
                    if (keepListening) {
                        startListening()
                    }
                }

                override fun onError(error: Int) {
                    isListening = false

                    // 에러 시 비프음 복원 후 다시 음소거
                    restoreSystemBeep()
                    muteSystemBeep()

                    // ERROR_NO_MATCH, ERROR_SPEECH_TIMEOUT 등에도 재시작하여 듣기를 계속 유지
                    if (keepListening) {
                        startListening()
                    }
                }
            })
        }

        // 6) RecognizerIntent 설정
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN)
        }

        // 7) 실제 듣기 시작
        speechRecognizer?.startListening(intent)
        isListening = true

        return true
    }

    fun stop() {
        // 8) 버튼에서 손 뗐을 때 keepListening=false
        keepListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        isListening = false
        // 사용자가 버튼으로 중단했을 때 한 번만 볼륨 복원
        if (!hasRestoredBeep) {
            restoreSystemBeep()
            hasRestoredBeep = true
        }
    }

    fun isRunning(): Boolean = isListening

    private fun analyzeIntent(userText: String) {
        val local = userText.lowercase(Locale.KOREAN)

        // 현재 단계의 StepTimerState
        val timer = stepTimerState

        // —— 1) 타이머 우선 처리 ——

        // 1) 일시정지 분기
        if (timer != null && listOf("일시정지","정지","멈춰","멈춰줘","중지","휴식").any { it in local }) {
            // ★ 인식만 잠깐 멈추기
            speechRecognizer?.stopListening()

            // 실제 일시정지
            timer.pause()

            // 안내 후 다시 듣기 재개
            tts.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).post {
                        if (keepListening) startListening()
                    }
                }
            })
            // ★ Utterance ID는 고유하게
            tts.speak("타이머를 일시정지합니다.", TextToSpeech.QUEUE_FLUSH, null, "timer_pause")
            return
        }

            // 2) 재시작 분기
        if (timer != null && listOf("재시작","다시시작","계속","이어","이어줘","다시 해줘").any { it in local }) {
            speechRecognizer?.stopListening()
            timer.resume()
            tts.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).post {
                        if (keepListening) startListening()
                    }
                }
            })
            tts.speak("타이머를 재개합니다.", TextToSpeech.QUEUE_FLUSH, null, "timer_resume")
            return
        }
        // “타이머” 또는 “시간” 또는 “재줘” 또는 “시작” 또는 “돌려” 또는 “켜줘” 또는 “틀어줘” 또는 “돌려줘” 또는 “때려줘” → 타이머 시작
        if (timer != null && (
                    "타이머" in local ||
                            "시간" in local ||
                            "재줘" in local ||
                            "시작" in local ||
                            "돌려" in local ||
                            "켜줘" in local ||
                            "틀어줘" in local ||
                            "돌려줘" in local ||
                            "때려줘" in local
                    )) {
            // 1) 현재 듣기만 멈춥니다
            speechRecognizer?.stopListening()

            timer.start {
                // tts.speak("타이머가 종료되었습니다.", TextToSpeech.QUEUE_FLUSH, null, "timer_finish")
            }
            tts.setOnUtteranceProgressListener(object: UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onError(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).post {
                        if (keepListening) {
                            startListening()
                        }
                    }
                }
            })

            // 4) 안내 멘트 (Utterance ID를 동일하게 설정)
            tts.speak(
                "타이머를 시작합니다.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "timer_start"
            )
            return
        }
        val keyword = when {
            "다음" in local || "다음으로" in local || "다음 단계" in local || "넘어가" in local || "이동" in local ->
                CommandType.NEXT

            "반복" in local || "다시" in local || "한번 더" in local || "repeat" in local || "리핏" in local ->
                CommandType.REPEAT

            // “그만”, “멈춰” 기존 키워드에 “종료”, “음성 종료” 추가
            "그만" in local ||
                    "멈춰" in local ||
                    "멈춰라" in local ||
                    "끝내" in local ||
                    "취소" in local ||
                    "종료" in local ||
                    "음성 종료" in local ->
                CommandType.STOP

            else -> null
        }

        if (keyword != null) {
            if (keyword == CommandType.STOP) {
                // 음성 인식 즉시 중지
                stop()
            }
            onCommand(keyword)
            return
        }

        // ————————— 여기부터 OpenAIClient 관련 부분 (수정 없음) —————————
        val aiClient = OpenAIClient()
        aiClient.setAIWithAPI(
            onSuccess = { apiKeyObj: ApiKey ->
                aiClient.apiKeyInfo = apiKeyObj

                val prompt = """
                    다음 발화는 요리 도우미 앱에서 사용된 음성 명령입니다. 
                    의도를 아래 중 하나로 분류하세요 (소문자 단어만): 
                    next, repeat, stop, timer, none

                    발화: "$userText"
                """.trimIndent()

                aiClient.sendMessage(
                    prompt = prompt,
                    role = "음성 명령 분류기",
                    onSuccess = { result ->
                        val command = when (result.trim()) {
                            "next" -> CommandType.NEXT
                            "repeat" -> CommandType.REPEAT
                            "stop" -> {
                                stop()
                                CommandType.STOP
                            }
                            "timer" -> CommandType.TIMER
                            else -> CommandType.NONE
                        }

                        if (command == CommandType.NONE) {
                            tts.speak(
                                "명령을 이해하지 못했습니다. 다시 말해주세요.",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "UNKNOWN"
                            )
                        } else {
                            onCommand(command)
                        }
                    },
                    onError = { errMsg ->
                        Log.e("VoiceCommand", "GPT 분석 실패: $errMsg")
                        tts.speak(
                            "음성 명령 분석에 실패했습니다.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "FAIL"
                        )
                    }
                )
            },
            onError = { errorMsg ->
                Toast.makeText(context, "API 키 로드 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                tts.speak(
                    "API 키를 불러오는 데 실패했습니다.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "KEY_FAIL"
                )
                stop()
            }
        )
        // ————————— 여기까지 OpenAIClient 관련 부분 —————————
    }
    private fun muteSystemBeep() {
        if (isBeepMuted) return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalSystemVolume = am.getStreamVolume(AudioManager.STREAM_SYSTEM)
        originalNotificationVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        isBeepMuted = true
        hasRestoredBeep = false  // 볼륨을 복원한 적 없으므로 false로 초기화
    }

    private fun restoreSystemBeep() {
        if (!isBeepMuted || hasRestoredBeep) return
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_SYSTEM, originalSystemVolume, 0)
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0)
        isBeepMuted = false
    }

    companion object {
        private const val TAG = "VoiceCommandController"
        const val REQUEST_RECORD_AUDIO = 1001
    }
}
