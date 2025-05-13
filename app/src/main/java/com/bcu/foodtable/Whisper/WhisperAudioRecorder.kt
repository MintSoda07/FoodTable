package com.bcu.foodtable.Whisper

import android.content.Context
import android.media.MediaRecorder
import java.io.File

object WhisperAudioRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(context: Context): File {
        outputFile = File(context.cacheDir, "recorded_audio.wav")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)  // Whisper는 mp3도 지원됨
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outputFile!!.absolutePath)
            prepare()
            start()
        }
        return outputFile!!
    }

    fun stop(): File? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return outputFile
    }
}
