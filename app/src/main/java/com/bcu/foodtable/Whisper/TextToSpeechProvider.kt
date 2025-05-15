package com.bcu.foodtable.Whisper

import android.speech.tts.TextToSpeech

 //TTS 받는 인터페이스
interface TextToSpeechProvider {
    fun getTTS(): TextToSpeech
}
