package com.bcu.foodtable.Whisper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bcu.foodtable.AI.OpenAIClient
import android.Manifest
import android.speech.tts.TextToSpeech
import com.bcu.foodtable.R
import com.bcu.foodtable.RecipeViewActivity
import com.bcu.foodtable.useful.ApiKeyManager
import com.bcu.foodtable.useful.RecipeDetailRecyclerAdaptor

class VoiceCookingManager(
    private val context: Context,
    private val micButton: ImageButton,
    private val onNextStep: () -> Unit,
    private val onRepeat: () -> Unit,
    private val onStop: () -> Unit,
    private val onStartTimer: () -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // 음성 보이스 시작
    fun start() {
        Log.d("VoiceCooking", "start() 호출됨1")

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
            Log.e("VoiceCooking", "마이크 권한 없음 — 요청 보냄")
            return
        }

        Log.d("VoiceCooking", "start() 호출됨2")

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "음성 인식을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            Log.e("VoiceCooking", "음성 인식 불가 환경")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    micButton.setImageResource(R.drawable.ic_mic_pro)
                    Log.d("VoiceCooking", "음성 인식 준비 완료")
                }

                override fun onResults(results: Bundle?) {
                    micButton.setImageResource(R.drawable.ic_mic)
                    val spokenText = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    spokenText?.let {
                        Log.d("VoiceCooking", "음성 인식 결과: $it")
                        analyzeIntent(it)
                    }
                    if (isListening) restart()
                }

                override fun onError(error: Int) {
                    micButton.setImageResource(R.drawable.ic_mic)
                    Log.e("VoiceCooking", "에러 발생: $error")

                    // ERROR_CLIENT(5)는 반복 방지
                    if (isListening && error != SpeechRecognizer.ERROR_CLIENT) {
                        restart()
                    } else {
                        stop()
                        Toast.makeText(context, "음성 인식에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    }
                }


                override fun onEndOfSpeech() {}
                override fun onBeginningOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onRmsChanged(rmsdB: Float) {}
            })
        }

        isListening = true
        //tts로 첫 단계 말하기
        if (context is RecipeViewActivity) {
            val recycler = context.getAdaptorViewList()
            var newIndex = 0

            //  완료된 마지막 단계 이후로 currentStepIndex 갱신
            for (i in 0 until recycler.childCount) {
                val holder =
                    recycler.findViewHolderForAdapterPosition(i) as? RecipeDetailRecyclerAdaptor.ViewHolder
                if (holder?.checkBox?.isChecked == true) {
                    newIndex = i + 1
                }
            }

            context.setCurrentStepIndex(newIndex)
            Log.d("VoiceCooking", "현재 단계로 설정된 인덱스: $newIndex")
            Log.d("VoiceCooking", "getCurrentStepIndex() 값 확인: ${context.getCurrentStepIndex()}")
            //  새 index의 단계만 TTS로 설명

            if (newIndex < recycler.adapter?.itemCount ?: 0) {
                val tts = (context as? TextToSpeechProvider)?.getTTS()
                if (tts != null) {
                    val actualIndex = context.getCurrentStepIndex()
                    if (actualIndex == 0) {
                        tts.speak("요리를 시작하겠습니다.", TextToSpeech.QUEUE_FLUSH, null, "START")
                    }
                    Log.d("VoiceCooking", "실제 speakStep 호출 인덱스: $actualIndex")
                    context.speakStep(actualIndex)
                }
            }
        }
        restart()
    }
    // TTS 가져오기
    private val tts: TextToSpeech by lazy {
        (context as? TextToSpeechProvider)?.getTTS()
            ?: throw IllegalStateException("TextToSpeechProvider를 구현한 Activity가 필요합니다.")
    }

    // 음성 보이스 다시 시작
    private fun restart() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        Log.d("VoiceCooking", "startListening() 호출")
        speechRecognizer?.startListening(intent)
    }
    // 음성 보이스 정지
    fun stop() {
        isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        micButton.setImageResource(R.drawable.ic_mic)
        tts.stop()

    }

    // 음성으로 다음 단계 넘어 가기 기능
    fun analyzeIntent(text: String) {
        val normalized = text.lowercase()
        when {
            normalized.contains("다음") || normalized.contains("계속") || normalized.contains("스킵") -> {
                onNextStep(); return // 음성 다음
            }
            normalized.contains("반복") || normalized.contains("다시") -> {
                onRepeat(); return // 음성 반복
            }
            normalized.contains("그만") || normalized.contains("종료") || normalized.contains("멈춰")-> {
                onStop(); stop(); return // 음성 정지
            }
            normalized.contains("타이머") || normalized.contains("시간") || normalized.contains("재줘") || normalized.contains("시작")-> {
                // ⬇ 여기에서 타이머 버튼 클릭 시뮬레이션
                val activity = context as? RecipeViewActivity
                activity?.let {
                    val index = it.getCurrentStepIndex()
                    val recycler = it.getAdaptorViewList()
                    val holder = recycler.findViewHolderForAdapterPosition(index) as? RecipeDetailRecyclerAdaptor.ViewHolder
                    holder?.startButton?.performClick()  // 타이머 시작
                }
                return
            }
        }

        val apiKey = ApiKeyManager.getGptApi()
        if (apiKey == null) {
            Toast.makeText(context, "GPT API 키가 없습니다.", Toast.LENGTH_SHORT).show()
            stop()
            return
        }

        val prompt = """
            다음 사용자 발화의 의도를 판단하세요. 선택지는 아래와 같습니다:
            - next: 다음 단계
            - repeat: 반복
            - stop: 조리 종료
            - timer: 타이머 시작
            - none: 명령 아님
            
            사용자 발화: "$text"
            위 중 하나만 소문자로 답하세요.
        """.trimIndent()

        val client = OpenAIClient().apply { apiKeyInfo = apiKey }

        client.sendMessage(
            prompt = prompt,
            role = "음성 명령 분석기",
            onSuccess = { intent ->
                when (intent.trim()) {
                    "next" -> onNextStep()
                    "repeat" -> onRepeat()
                    "stop" -> {
                        if (context is RecipeViewActivity) {
                           /* val tts = context.getTTS()
                            tts.speak("요리를 종료합니다.", TextToSpeech.QUEUE_FLUSH, null, "STOP_DONE")*/

                            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                                override fun onStart(utteranceId: String?) {}

                                override fun onDone(utteranceId: String?) {
                                    if (utteranceId == "STOP_DONE") {
                                        onStop()
                                        stop()
                                    }
                                }

                                override fun onError(utteranceId: String?) {}
                            })
                        } else {
                            onStop()
                            stop()
                        }
                    }

                    "timer" -> {
                        if (context is RecipeViewActivity) {
                            val viewHolder = context.getAdaptorViewList()
                                .findViewHolderForAdapterPosition(context.getCurrentStepIndex())
                                    as? RecipeDetailRecyclerAdaptor.ViewHolder
                            viewHolder?.startButton?.performClick()
                            Log.d("VoiceCooking", "타이머 시작 버튼 눌림")
                        }
                    }
                    else -> {
                        Log.d("VoiceCooking", "명령 아님: $text")

                        if (context is RecipeViewActivity) {
                            val currentIndex = context.getCurrentStepIndex()
                            val tts = (context as? TextToSpeechProvider)?.getTTS()
                            tts?.speak("이건 명령이 아니에요. 다시 말해 주세요.", TextToSpeech.QUEUE_FLUSH, null, "UNKNOWN")
                            context.speakStep(currentIndex)  // 현재 단계 다시 설명
                        }
                    }
                }
            },
            onError = {
                Log.e("VoiceCooking", "GPT 분석 오류: $it")
            }
        )
    }
    fun isRunning(): Boolean = isListening

    fun listen() = restart()

}
