package com.bcu.foodtable.data

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.database.ktx.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object KakaoApiKeyProvider {
    private const val TAG = "KakaoApiKeyProvider"

    // 한 번 읽어오면 여기에 저장됨
    private var cachedKey: String? = null

    /**
     * 실제 키가 필요할 때 호출.
     * 이미 캐싱되어 있으면 즉시 반환하고, 아니면 Firebase에서 가져와서 캐싱 후 반환.
     */
    @SuppressLint("RestrictedApi")
    suspend fun getApiKey(): String {
        Log.d(TAG, "getApiKey() called. cachedKey=${if (cachedKey != null) "YES" else "NO"}")

        // 캐시된 키가 있으면 바로 리턴
        cachedKey?.let {
            Log.d(TAG, "Returning API key from cache.")
            return it
        }

        Log.d(TAG, "Cache miss. Fetching API key from Firebase Realtime Database.")

        // Firebase Realtime DB 경로
        val ref = Firebase.database
            .getReference("API_KEY")
            .child("E3dzNAnZELMML8G1Hwxx")

        return try {
            // 단일 조회 (await)
            Log.d(TAG, "Requesting snapshot at ${ref.path}.")
            val snapshot = ref.get().await()
            Log.d(TAG, "Snapshot retrieved. exists=${snapshot.exists()}")

            if (!snapshot.exists()) {
                val message = "API 키 문서를 찾을 수 없습니다."
                Log.e(TAG, message)
                throw IllegalStateException(message)
            }

            // KEY_VALUE 필드에서 읽어오기
            val key = snapshot.child("KEY_VALUE").getValue<String>()
            Log.d(TAG, "Snapshot child 'KEY_VALUE' = ${key?.let { "[REDACTED]" } ?: "null"}")

            if (key.isNullOrBlank()) {
                val message = "KEY_VALUE가 비어 있습니다."
                Log.e(TAG, message)
                throw IllegalStateException(message)
            }

            // 캐싱 후 리턴
            cachedKey = key
            Log.d(TAG, "API key cached successfully.")
            key
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch API key from Firebase", e)
            throw e
        }
    }
}
