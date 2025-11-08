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
import java.util.*

class VoiceCommandController(
    private val context: Context,
    private val tts: TextToSpeech,
    onCommand: (CommandType) -> Unit
) {

    enum class CommandType { NEXT, REPEAT, STOP, TIMER, NONE }

    var onCommand: (CommandType) -> Unit = onCommand

    // 타이머 상태를 외부에서 세팅
    var stepTimerState: StepTimerState? = null

    private var keepListening = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private var originalSystemVolume: Int = 0
    private var originalNotificationVolume: Int = 0
    private var isBeepMuted = false
    private var hasRestoredBeep = false

    // 메인 스레드 보장용
    private val mainHandler = Handler(Looper.getMainLooper())
    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post { block() }
    }

    // ====== 🔎 TTS 디버그 로깅 유틸 ======
    // utteranceId -> 전체 텍스트 매핑 (콜백에서 프리뷰 찍기 용)
    private val ttsLogMap = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun logTTS(level: Int = Log.DEBUG, msg: String) {
        val thread = Thread.currentThread().name
        val stamp  = System.currentTimeMillis()
        Log.println(level, "TTS", "[$stamp][$thread] $msg")
    }

    /**
     * 공통 Listener 설정: onStart/onDone/onError 모두 로그 남김.
     * onDoneExtra가 있으면 onDone 후 메인쓰레드에서 추가 동작 실행.
     */
    private fun setTtsListener(onDoneExtra: (() -> Unit)? = null) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                val preview = id?.let { ttsLogMap[it] }?.let { txt ->
                    if (txt.length > 120) txt.take(120) + "…" else txt
                } ?: ""
                logTTS(msg = "onStart: id=$id, preview=\"$preview\"")
            }
            override fun onError(id: String?) {
                val preview = id?.let { ttsLogMap[it] }?.let { txt ->
                    if (txt.length > 120) txt.take(120) + "…" else txt
                } ?: ""
                logTTS(Log.ERROR, "onError: id=$id, preview=\"$preview\"")
                if (id != null) ttsLogMap.remove(id)
            }
            override fun onDone(id: String?) {
                logTTS(msg = "onDone: id=$id")
                if (id != null) ttsLogMap.remove(id)
                if (onDoneExtra != null) {
                    mainHandler.post { onDoneExtra.invoke() }
                }
            }
        })
    }

    /**
     * 공통 speak 래퍼: 항상 로깅 + listener 세팅 + speak까지 한 번에.
     * onDoneExtra로 후행 작업(예: 자동 재청취)도 가능.
     */
    private fun speakWithLogging(
        text: String,
        utteranceId: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        onDoneExtra: (() -> Unit)? = null
    ) {
        runOnMain {
            val preview = if (text.length > 120) text.take(120) + "…" else text
            ttsLogMap[utteranceId] = text
            logTTS(msg = "speak() 호출: id=$utteranceId, len=${text.length}, preview=\"$preview\", queue=${if (queueMode == TextToSpeech.QUEUE_FLUSH) "FLUSH" else "ADD"}")

            setTtsListener(onDoneExtra)
            tts.speak(text, queueMode, null, utteranceId)
        }
    }
    // ==================================

    // 테스트 모드일 때 자유 발화 캡처 여부
    var shouldCaptureFreeSpeech: () -> Boolean = { false }

    // 자유 발화 콜백(명령 아님)
    var onFreeSpeech: (String) -> Unit = {}

    /**
     * 음성 인식을 실제로 시작했으면 true
     * (메인 스레드로 디스패치하므로 호출 즉시 true 반환)
     */
    fun startListening(): Boolean {
        runOnMain {
            // 1) 인식 가능 기기인지 확인
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Toast.makeText(context, "음성 인식이 지원되지 않습니다", Toast.LENGTH_SHORT).show()
                return@runOnMain
            }

            // 2) 권한 체크
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
                return@runOnMain
            }

            if (!isBeepMuted) muteSystemBeep()

            // 3) 플래그
            keepListening = true

            // 4) 기존 recognizer 해제 후 재생성
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
                        restoreSystemBeep()
                        muteSystemBeep()

                        val text = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()

                        if (text.isNullOrEmpty()) {
                            if (keepListening) startListening()
                            return
                        }

                        Log.d("VoiceCommand", "인식된 텍스트: $text")
                        analyzeIntent(text)

                        if (keepListening) startListening()
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        restoreSystemBeep()
                        muteSystemBeep()
                        if (keepListening) startListening()
                    }
                })
            }

            // 6) 인텐트 설정
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN)
            }

            // 7) 시작
            speechRecognizer?.startListening(intent)
            isListening = true
        }
        return true
    }

    fun stop() {
        runOnMain {
            keepListening = false
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            if (!hasRestoredBeep) {
                restoreSystemBeep()
                hasRestoredBeep = true
            }
        }
    }

    // 🔹 Q&A 등에서 "잠깐 멈춤 → 답 TTS → 자동 재개"를 쉽게 쓰라고 제공
    fun stopListeningOnly() {
        runOnMain { speechRecognizer?.stopListening() }
    }

    fun restartListeningIfNeeded() {
        runOnMain { if (keepListening) startListening() }
    }

    fun speakWithAutoResume(
        text: String,
        utteranceId: String = "qa_answer"
    ) {
        runOnMain {
            logTTS(msg = "speakWithAutoResume 진입: id=$utteranceId")
            // 듣기 잠깐 멈춤
            stopListeningOnly()
            logTTS(msg = "STT stopListeningOnly() 완료")

            // 말이 끝나면 자동 재청취
            speakWithLogging(
                text = text,
                utteranceId = utteranceId,
                queueMode = TextToSpeech.QUEUE_FLUSH,
                onDoneExtra = { restartListeningIfNeeded() }
            )
        }
    }

    fun isRunning(): Boolean = isListening

    private fun analyzeIntent(userText: String) {
        val local = userText.lowercase(Locale.KOREAN)
        val timer = stepTimerState

        // —— 1) 타이머 우선 처리 ——
        if (timer != null && listOf("일시정지","정지","멈춰","멈춰줘","중지","휴식").any { it in local }) {
            runOnMain {
                speechRecognizer?.stopListening()
                timer.pause()
                speakWithLogging(
                    text = "타이머를 일시정지합니다.",
                    utteranceId = "timer_pause",
                    onDoneExtra = { if (keepListening) startListening() }
                )
            }
            return
        }

        if (timer != null && listOf("재시작","다시시작","계속","이어","이어줘","다시 해줘").any { it in local }) {
            runOnMain {
                speechRecognizer?.stopListening()
                timer.resume()
                speakWithLogging(
                    text = "타이머를 재개합니다.",
                    utteranceId = "timer_resume",
                    onDoneExtra = { if (keepListening) startListening() }
                )
            }
            return
        }

        if (timer != null
            && "타이머" in local
            && listOf("시작","켜줘","틀어줘","돌려","재줘").any { it in local }
        ) {
            if (timer.isRunning) return
            runOnMain {
                speechRecognizer?.stopListening()
                timer.start { /* 필요시 타이머 종료 TTS 추가 가능 */ }
                speakWithLogging(
                    text = "타이머를 시작합니다.",
                    utteranceId = "timer_start",
                    onDoneExtra = { if (keepListening) startListening() }
                )
            }
            return
        }

        val keyword = when {
            "다음" in local || "다음으로" in local || "다음 단계" in local || "넘어가" in local || "이동" in local ->
                CommandType.NEXT
            "반복" in local || "다시" in local || "한번 더" in local || "repeat" in local || "리핏" in local ->
                CommandType.REPEAT
            "그만" in local || "멈춰" in local || "멈춰라" in local || "끝내" in local || "취소" in local || "종료" in local || "음성 종료" in local ->
                CommandType.STOP
            else -> null
        }

        if (keyword != null) {
            if (keyword == CommandType.STOP) {
                onCommand(CommandType.STOP) // 상위에서 stop() 처리
                return
            }
            onCommand(keyword)
            return
        }

        if (shouldCaptureFreeSpeech()) {
            onFreeSpeech(userText)
            return
        }

        // ——— OpenAIClient 백업 분류기 ———
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
                            speakWithLogging(
                                text = "명령을 이해하지 못했습니다. 다시 말해주세요.",
                                utteranceId = "UNKNOWN"
                            )
                        } else {
                            onCommand(command)
                        }
                    },
                    onError = { errMsg ->
                        Log.e("VoiceCommand", "GPT 분석 실패: $errMsg")
                        speakWithLogging(
                            text = "음성 명령 분석에 실패했습니다.",
                            utteranceId = "FAIL"
                        )
                    }
                )
            },
            onError = { errorMsg ->
                runOnMain {
                    Toast.makeText(context, "API 키 로드 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                    speakWithLogging(
                        text = "API 키를 불러오는 데 실패했습니다.",
                        utteranceId = "KEY_FAIL"
                    )
                    stop()
                }
            }
        )
    }

    private fun muteSystemBeep() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalSystemVolume = am.getStreamVolume(AudioManager.STREAM_SYSTEM)
        originalNotificationVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
        am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        isBeepMuted = true
        hasRestoredBeep = false
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
