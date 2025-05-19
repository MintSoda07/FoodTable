package com.bcu.foodtable.JetpackCompose.AI

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiRecommendationViewModel @Inject constructor(
    private val apiClient: OpenAIClient
) : ViewModel() {

    data class UiState(
        val inputText: String = "",
        val isSending: Boolean = false,
        val userPoint: Int = 0,
        val resultText: String = "",
        val reasonText: String = "",
        val showWarning: Boolean = true
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val user = UserManager.getUser()
    private val aiUseCost = 40

    init {
        _uiState.update { it.copy(userPoint = user?.point ?: 0) }
    }

    fun onInputChange(new: String) {
        _uiState.update { it.copy(inputText = new) }
    }

    fun hideWarning() {
        _uiState.update { it.copy(showWarning = false) }
    }

    fun sendRecommendation() {
        val state = _uiState.value
        val input = state.inputText
        val currentUser = user ?: return

        if (input.isBlank() || state.isSending || currentUser.point < aiUseCost) return

        _uiState.update {
            it.copy(isSending = true, showWarning = false, resultText = "", reasonText = "")
        }

        val prompt1 = "사용자 입력:${input}"
        val role1 = "당신은 사용자에게 알맞은 레시피를 추천하는 친절한 도우미입니다. 추천 이유는 대답해주면 안 됩니다."

        apiClient.sendMessage(
            prompt = prompt1,
            role = role1,
            onSuccess = { result ->

                val prompt2 = """다음은 당신의 추천해준 레시피 내용입니다: "$result" 이상으로, 사용자의 질문 "$input"에 대해 왜 추천했는지 설명해 주세요."""
                val role2 = "당신은 추천 이유를 재료나 조리 특성을 바탕으로 친절하게 설명하는 도우미입니다."

                apiClient.sendMessage(
                    prompt = prompt2,
                    role = role2,
                    onSuccess = { reason ->
                        val newPoint = (currentUser.point - aiUseCost).coerceAtLeast(0)

                        viewModelScope.launch {
                            updateFieldById("user", currentUser.uid, "point", newPoint)
                        }

                        currentUser.point = newPoint

                        _uiState.update {
                            it.copy(
                                isSending = false,
                                resultText = result,
                                reasonText = reason,
                                userPoint = newPoint
                            )
                        }
                    },
                    onError = {
                        _uiState.update { it.copy(isSending = false) }
                    }
                )
            },
            onError = {
                _uiState.update { it.copy(isSending = false) }
            }
        )
    }
}
