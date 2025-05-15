package com.bcu.foodtable.Whisper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bcu.foodtable.AI.OpenAIClient

object WhisperRecorder {
    fun start(
        context: Context,
        durationMillis: Long = 4000,
        onTranscriptionReady: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // 1. 녹음 시작
        val recordedFile = WhisperAudioRecorder.start(context)

        // 2. 4초 후 녹음 종료 + Whisper API 호출
        Handler(Looper.getMainLooper()).postDelayed({
            val audioFile = WhisperAudioRecorder.stop()
            if (audioFile != null) {
                val whisperClient = OpenAIClient()
                whisperClient.setAIWithAPI(
                    onSuccess = {
                        whisperClient.transcribeAudio(
                            audioFile,
                            onResult = { resultText ->
                                Log.d("WhisperRecorder", "Whisper 결과: $resultText")
                                onTranscriptionReady(resultText)
                            },
                            onError = { errorMsg ->
                                Log.e("WhisperRecorder", "API 오류: $errorMsg")
                                onError(errorMsg)
                            }
                        )
                    },
                    onError = { keyError ->
                        onError("API 키 오류: $keyError")
                    }
                )
            } else {
                onError("녹음 파일이 존재하지 않습니다.")
            }
        }, 4000) // 4초 후 자동 호출
    }
}

