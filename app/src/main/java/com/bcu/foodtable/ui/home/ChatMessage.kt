package com.bcu.foodtable.ui.home
import java.util.UUID // id를 위해 추가

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(), // 고유 ID
    val text: String,
    val isUser: Boolean
)