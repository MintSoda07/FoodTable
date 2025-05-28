package com.bcu.foodtable.TTS // 사용자님의 패키지명인지 확인해주세요

import android.content.Context
import android.os.Build // Build.VERSION 확인을 위해 추가
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice // Voice 객체 사용을 위해 추가 (API 21+)
import android.util.Log
import java.util.Locale

class TtsHelper(
    context: Context,
    private val onInitialized: (isSuccess: Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var currentLocale: Locale = Locale.KOREAN

    init {
        Log.d("TtsHelper_Lifecycle", "TtsHelper init 호출됨. Context: $context")
        try {
            Log.d("TtsHelper_Lifecycle", "TtsHelper: TextToSpeech 객체 생성 시도 중...")
            tts = TextToSpeech(context.applicationContext, this)
            Log.d("TtsHelper_Lifecycle", "TtsHelper: TextToSpeech 객체 생성 요청 완료 (또는 대기열에 추가됨).")
        } catch (e: Exception) {
            Log.e("TtsHelper_Lifecycle", "TextToSpeech 생성자 또는 TtsHelper init 중 예외 발생", e)
            onInitialized(false)
        }
    }

    override fun onInit(status: Int) {
        Log.d("TtsHelper_Lifecycle", "TtsHelper onInit 콜백 수신됨. Status: $status")
        if (status == TextToSpeech.SUCCESS) {
            val langResult = tts?.setLanguage(currentLocale)

            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsHelper", "기본 언어(${currentLocale.displayLanguage}) 지원 안됨 또는 데이터 없음.")
                isInitialized = false
            } else {
                Log.i("TtsHelper", "TTS 초기화 및 언어 설정 성공 (${currentLocale.displayLanguage}).")
                isInitialized = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        val availableVoices = tts?.voices
                        // --- ▼▼▼ 추가된 디버깅 로그 (1) ▼▼▼ ---
                        Log.i("TtsHelper_Voices", "====== 현재 TTS 엔진에서 사용 가능한 전체 목소리 목록 ======")
                        availableVoices?.forEach { voice ->
                            Log.i("TtsHelper_Voices", "이름: ${voice.name}, 로케일: ${voice.locale}, 특징: ${voice.features}")
                        }
                        Log.i("TtsHelper_Voices", "======================================================")
                        // --- ▲▲▲ 추가된 디버깅 로그 (1) 끝 ▲▲▲ ---

                        val voiceNameToTry = "ko-kr-x-koc-network"
                        Log.d("TtsHelper_DebugFind", "찾으려는 목소리 이름(voiceNameToTry): '$voiceNameToTry' (길이: ${voiceNameToTry.length})")

                        // --- ▼▼▼ 추가된 디버깅 로그 (2) ▼▼▼ ---
                        Log.d("TtsHelper_DebugFind", "availableVoices 객체는 null인가?: ${availableVoices == null}")
                        if (availableVoices != null) {
                            Log.d("TtsHelper_DebugFind", "availableVoices 목록의 크기: ${availableVoices.size}")
                            Log.d("TtsHelper_DebugFind", "find 실행 시 currentLocale: $currentLocale (예상: ko_KR)")
                        }
                        // --- ▲▲▲ 추가된 디버깅 로그 (2) 끝 ▲▲▲ ---
                        val desiredKoreanVoice = availableVoices?.find { voice ->
                            val localeMatches = (voice.locale == currentLocale)
                            // voice.name이 null일 수도 있으므로 안전하게 처리
                            val currentVoiceName = voice.name ?: "NULL_VOICE_NAME"

                            // 로케일이 한국어인 경우에만 상세 비교 로그를 남깁니다.
                            if (localeMatches) {
                                val nameEqualsResult = currentVoiceName.equals(voiceNameToTry, ignoreCase = true)
                                Log.d("TtsHelper_DebugFind", "검사 중 (로케일: ${voice.locale}): 이름='${currentVoiceName}' (길이:${currentVoiceName.length}), 이름 일치 시도 결과($voiceNameToTry): $nameEqualsResult")
                                // 만약 위 로그로도 원인 파악이 안되면, 아래 바이트 배열 비교 로그의 주석을 해제하여 테스트해볼 수 있습니다.
                                // 이는 눈에 보이지 않는 특수문자가 있는지 확인하는 데 도움이 될 수 있습니다.
                                // val currentVoiceNameBytes = currentVoiceName.toByteArray().joinToString("") { "%02x".format(it) }
                                // val voiceNameToTryBytes = voiceNameToTry.toByteArray().joinToString("") { "%02x".format(it) }
                                // Log.d("TtsHelper_DebugFind", "  ㄴ 현재 목소리 이름 (Hex): $currentVoiceNameBytes")
                                // Log.d("TtsHelper_DebugFind", "  ㄴ 찾으려는 목소리 이름 (Hex): $voiceNameToTryBytes")
                            }
                            localeMatches && currentVoiceName.equals(voiceNameToTry, ignoreCase = true)
                        }
                        // --- ▲▲▲ 목소리 선택 로직 디버깅 로그 강화 끝 ▲▲▲ ---

                        if (desiredKoreanVoice != null) {
                            val setResult = tts?.setVoice(desiredKoreanVoice)
                            if (setResult == TextToSpeech.SUCCESS) {
                                Log.i("TtsHelper_Voices", "프로그램에서 목소리 변경 성공: ${desiredKoreanVoice.name}")
                            } else {
                                Log.w("TtsHelper_Voices", "프로그램에서 목소리 변경 실패: ${desiredKoreanVoice.name}, 결과 코드: $setResult. 기기 기본 목소리가 사용됩니다.")
                            }
                        } else {
                            Log.w("TtsHelper_Voices", "'${voiceNameToTry}' 목소리를 목록에서 찾을 수 없음. 사용 가능한 목록을 확인하세요. 기기 기본 목소리가 사용됩니다.")
                        }
                    } catch (e: Exception) {
                        Log.e("TtsHelper_Voices", "목소리 조회 또는 설정 중 예외 발생", e)
                    }
                }
            }
        } else {
            Log.e("TtsHelper", "TTS 초기화 실패, status: $status")
            isInitialized = false
        }
        onInitialized(isInitialized)
    }

    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (isInitialized && tts != null) {
            tts?.speak(text, queueMode, null, null)
            Log.i("TtsHelper", "Speaking: $text")
        } else {
            Log.w("TtsHelper", "TTS not initialized or null. Cannot speak: $text. isInitialized=$isInitialized, ttsInstanceNotNull=${tts!=null}")
        }
    }

    fun setSpeechRate(rate: Float) {
        if (isInitialized && tts != null) {
            val result = tts?.setSpeechRate(rate)
            if (result == TextToSpeech.SUCCESS) {
                Log.i("TtsHelper", "음성 속도 변경: $rate")
            } else {
                Log.w("TtsHelper", "음성 속도 변경 실패.")
            }
        }
    }

    fun setPitch(pitch: Float) {
        if (isInitialized && tts != null) {
            val result = tts?.setPitch(pitch)
            if (result == TextToSpeech.SUCCESS) {
                Log.i("TtsHelper", "음성 높이 변경: $pitch")
            } else {
                Log.w("TtsHelper", "음성 높이 변경 실패.")
            }
        }
    }

    fun setLanguage(locale: Locale): Boolean {
        currentLocale = locale
        if (isInitialized && tts != null) {
            val result = tts?.setLanguage(currentLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsHelper", "Failed to set language to ${currentLocale.displayLanguage}")
                return false
            }
            Log.i("TtsHelper", "Language set to ${currentLocale.displayLanguage}")
            return true
        }
        Log.w("TtsHelper", "Cannot set language, TTS not initialized or null.")
        return false
    }

    fun shutdown() {
        Log.d("TtsHelper_Lifecycle", "TtsHelper shutdown() 호출됨.")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        Log.i("TtsHelper", "TTS Shutdown.")
    }
}