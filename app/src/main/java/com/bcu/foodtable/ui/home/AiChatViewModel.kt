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
// import java.util.UUID // ChatMessage에 ID를 사용하고 싶다면 이 import가 필요합니다.
// ChatMessage 클래스는 별도 파일에 정의되어 있거나 이 파일의 다른 곳에 정의되어 있다고 가정합니다.
// 예: import com.bcu.foodtable.ui.home.ChatMessage 또는 같은 파일 내 정의

class AiChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> get() = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val functions = FirebaseFunctions.getInstance()

    fun sendMessage(userMessage: String) {
        val trimmed = userMessage.trim()

        if (trimmed.isEmpty()) {
            // 'isUser' 파라미터 이름으로 수정
            _messages.value = _messages.value + ChatMessage(text = "질문을 입력해주세요.", isUser = false)
            return
        }

        // 'isUser' 파라미터 이름으로 수정
        _messages.value = _messages.value + ChatMessage(text = trimmed, isUser = true)

        viewModelScope.launch {
            _isLoading.value = true // 로딩 시작
            try {
                val payload = hashMapOf<String, Any>("text" to trimmed)
                Log.d("AiChatViewModel", "Firebase Functions 호출 payload: $payload")

                val result = functions
                    .getHttpsCallable("askRecipe")
                    .call(payload)
                    .await()

                val dataMap = result.getData() as? Map<*, *>
                if (dataMap == null) {
                    Log.e("AiChatViewModel", "AI 응답 데이터가 Map 형식이 아닙니다. 응답 데이터: ${result.getData()}")
                    // 'isUser' 파라미터 이름으로 수정
                    _messages.value = _messages.value + ChatMessage(text = "AI 응답 형식 오류입니다.", isUser = false)
                    return@launch // 코루틴 종료
                }

                val aiReply = dataMap["reply"] as? String
                if (aiReply == null) {
                    Log.e("AiChatViewModel", "AI 응답에서 'reply' 키를 찾을 수 없거나 문자열이 아닙니다. 응답 Map: $dataMap")
                    // 'isUser' 파라미터 이름으로 수정
                    _messages.value = _messages.value + ChatMessage(text = "AI 응답 내용이 없습니다.", isUser = false)
                    return@launch // 코루틴 종료
                }
                // AI 응답이 성공적으로 왔을 때
                // 'isUser' 파라미터 이름으로 수정
                _messages.value = _messages.value + ChatMessage(text = aiReply, isUser = false)

            } catch (e: Exception) {
                Log.e("AiChatViewModel", "askRecipe 함수 호출 중 오류 발생", e)

                val errorMessage = if (e is FirebaseFunctionsException) {
                    "AI 호출 실패 (코드: ${e.code}): ${e.message}"
                } else {
                    "AI 호출 실패: ${e.localizedMessage ?: "알 수 없는 오류가 발생했습니다."}"
                }
                // 'isUser' 파라미터 이름으로 수정
                _messages.value = _messages.value + ChatMessage(text = errorMessage, isUser = false)
            } finally {
                _isLoading.value = false // 로딩 종료 (성공/실패 모든 경우)
            }
        }
    }
}