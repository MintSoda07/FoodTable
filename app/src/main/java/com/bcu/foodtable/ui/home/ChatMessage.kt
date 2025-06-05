package com.bcu.foodtable.ui.home
import java.util.UUID // id를 위해 추가

/**
 * ChatMessage 데이터 클래스 예시
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)