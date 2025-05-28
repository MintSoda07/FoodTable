package com.bcu.foodtable.TTS

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object ImageUtils { // object로 선언하여 바로 사용 가능

    fun byteArrayToBase64(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    suspend fun uriToByteArray(context: Context, uri: Uri): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
            } catch (e: Exception) {
                Log.e("ImageUtils", "URI to ByteArray conversion failed: ${e.message}", e)
                ByteArray(0)
            }
        }
    }

    suspend fun urlToByteArray(url: String): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                URL(url).openStream().use { it.readBytes() }
            } catch (e: Exception) {
                Log.e("ImageUtils", "URL to ByteArray conversion failed for $url: ${e.message}", e)
                ByteArray(0)
            }
        }
    }
}