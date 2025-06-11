// com.bcu.foodtable.data.GlobalChatManager.kt
package com.bcu.foodtable.data

import androidx.compose.runtime.mutableStateListOf

/**
 * 앱 전체에서 친구 리스트와 채팅 스레드를 전역으로 관리.
 * mutableStateListOf 로 선언하면 Compose 가 변경을 감지합니다.
 */
object GlobalChatManager {
    // 친구 리스트 (친구 UID 목록)
    val friends = mutableStateListOf<String>()

    // 채팅 스레드: Pair<상대방UID, 마지막메시지>
    val chatThreads = mutableStateListOf<Pair<String, String>>()
}
