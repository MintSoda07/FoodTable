package com.bcu.foodtable.JetpackCompose.AI

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.useful.ApiKeyManager
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiHelperViewModel(
    private val apiClient: OpenAIClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState

    private val user = UserManager.getUser()
    private val aiUseCost = 40

    init {
        user?.let {
            _uiState.update { state -> state.copy(userPoint = it.point) }
        }

        val apiKey = ApiKeyManager.getGptApi()
        if (apiKey == null) {
            apiClient.setAIWithAPI(
                onSuccess = {
                    ApiKeyManager.setGptApiKey(it.KEY_NAME!!, it.KEY_VALUE!!)
                    Log.i("AiHelper", "GPT API Key Loaded")
                },
                onError = {
                    Log.e("AiHelper", "GPT API Key Load Failed")
                }
            )
        } else {
            apiClient.apiKeyInfo = apiKey
        }
    }

    fun onInputChange(newInput: String) {
        _uiState.update { it.copy(inputText = newInput) }
    }

    fun hideWarning() {
        _uiState.update { it.copy(showWarning = false) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val input = state.inputText
        val point = state.userPoint

        if (input.isBlank() || point < aiUseCost || state.isSending) return

        Log.d("AiHelper", "🔄 AI 호출 시작: 입력 = $input, 포인트 = $point")

        _uiState.update { it.copy(isSending = true, showWarning = false) }

        val rule = """
        당신은 조리를 도와주는 쿡봇입니다. 지켜야 할 규칙은 다음과 같습니다.
        1. 사용자가 입력한 재료만 사용하여 만들 수 있는 레시피를 최소 4개 이상 제공합니다.
        2. 재료 목록은 따로 레시피 제공 전 {}안에 작성합니다. 예: {소고기}{감자}{소금}{후추} 
        3. 제공되는 레시피의 앞과 뒤에는 정규식 구분을 위해 ◆을 붙여 주세요. 예: ◆감자 소금구이◆
        4. 또한, 레시피에 사용되는 재료를 자세한 용량과 함께 레시피 이름 뒤에 괄호로 넣어 주세요. 예: ◆감자 소금구이◆(감자 2개,소금 5g)
        """.trimIndent()

        apiClient.sendMessage(
            prompt = "사용자 입력:$input",
            role = rule,
            onSuccess = { response ->
                Log.d("AiHelper", "✅ GPT 응답 수신 완료:\n$response")

                viewModelScope.launch {
                    val ingredientRegex = """\{(.*?)\}""".toRegex()
                    val ingredients = ingredientRegex.findAll(response).map { it.groupValues[1] }.toList()
                    Log.d("AiHelper", "🟢 추출된 재료: $ingredients")

                    val recipeRegex = """◆(.*?)◆""".toRegex()
                    val recipes = recipeRegex.findAll(response).map { it.groupValues[1] }.toList()
                    Log.d("AiHelper", "🟢 추출된 레시피 제목: $recipes")

                    val recipeDetailsRegex = """◆.*?◆\((.*?)\)""".toRegex()
                    val details = recipeDetailsRegex.findAll(response).map { it.groupValues[1] }.toList()
                    Log.d("AiHelper", "🟢 추출된 레시피 상세: $details")

                    val newPoint = point - aiUseCost
                    user?.point = newPoint
                    updateFieldById("user", user?.uid ?: "", "point", newPoint)

                    _uiState.update {
                        it.copy(
                            userPoint = newPoint,
                            inputText = "",
                            ingredients = ingredients,
                            recipes = recipes,
                            recipeDetails = details,
                            resultText = recipes.joinToString("\n"),
                            reasonText = details.joinToString("\n"),
                            isSending = false
                        )
                    }

                    Log.d("AiHelper", "✅ UI 상태 업데이트 완료")
                }
            },
            onError = { error ->
                Log.e("AiHelper", "❌ GPT API 오류: $error")
                _uiState.update { it.copy(isSending = false) }
            }
        )
    }
}
