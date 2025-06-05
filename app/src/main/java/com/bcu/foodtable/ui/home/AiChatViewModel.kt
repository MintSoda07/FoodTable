// ui/home/AiChatViewModel.kt

package com.bcu.foodtable.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ChatMessage는 별도 파일(ui/home/ChatMessage.kt)에 정의되어야 합니다.
 * 예:
 * data class ChatMessage(
 *     val id: String = java.util.UUID.randomUUID().toString(),
 *     val text: String,
 *     val isUser: Boolean
 * )
 */

class AiChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> get() = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val functions = FirebaseFunctions.getInstance()

    /**
     * 사용자가 입력한 메시지를 AI(askRecipe Cloud Function)에게 보내고,
     * 응답을 ChatMessage 목록에 추가합니다.
     */
    fun sendMessage(userMessage: String) {
        val trimmed = userMessage.trim()
        if (trimmed.isEmpty()) {
            _messages.value = _messages.value + ChatMessage(
                text = "질문을 입력해주세요.",
                isUser = false
            )
            return
        }

        // 1) 사용자가 보낸 메시지를 목록에 추가
        _messages.value = _messages.value + ChatMessage(
            text = trimmed,
            isUser = true
        )

        // 2) 백그라운드로 Cloud Function 호출
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 호출 payload 준비
                val payload = hashMapOf<String, Any>("text" to trimmed)
                Log.d("AiChatViewModel", "Firebase Functions 호출 payload: $payload")

                // askRecipe 이름의 HTTPS 호출
                val result = functions
                    .getHttpsCallable("askRecipe")
                    .call(payload)
                    .await()

                // result.getData() 메서드를 통해 응답 내용을 꺼냅니다.
                val dataMap = result.getData() as? Map<*, *>
                if (dataMap == null) {
                    Log.e(
                        "AiChatViewModel",
                        "AI 응답 데이터가 Map 형식이 아닙니다. 응답 전체: ${result.getData()}"
                    )
                    _messages.value = _messages.value + ChatMessage(
                        text = "AI 응답 형식 오류입니다.",
                        isUser = false
                    )
                    return@launch
                }

                // "reply" 키로 AI 응답 문자열을 가져옵니다.
                val aiReply = dataMap["reply"] as? String
                if (aiReply == null) {
                    Log.e(
                        "AiChatViewModel",
                        "AI 응답에서 'reply' 키가 없거나 문자열이 아닙니다. Map 내용: $dataMap"
                    )
                    _messages.value = _messages.value + ChatMessage(
                        text = "AI 응답 내용이 없습니다.",
                        isUser = false
                    )
                    return@launch
                }

                // 3) AI 응답 메시지 추가
                _messages.value = _messages.value + ChatMessage(
                    text = aiReply,
                    isUser = false
                )

            } catch (e: Exception) {
                Log.e("AiChatViewModel", "askRecipe 호출 중 오류 발생", e)

                val errorMessage = if (e is FirebaseFunctionsException) {
                    "AI 호출 실패 (코드: ${e.code}): ${e.message}"
                } else {
                    "AI 호출 실패: ${e.localizedMessage ?: "알 수 없는 오류"}"
                }
                _messages.value = _messages.value + ChatMessage(
                    text = errorMessage,
                    isUser = false
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}
