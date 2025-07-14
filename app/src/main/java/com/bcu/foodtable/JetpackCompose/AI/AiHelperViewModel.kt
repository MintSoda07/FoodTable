package com.bcu.foodtable.JetpackCompose.AI

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.ai.OpenAIClient
import com.bcu.foodtable.useful.ApiKeyManager
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// 나의 냉장고 관련 ai 호출
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
        당신은 요리 도우미 AI입니다. 사용자가 가진 재료로 만들 수 있는 요리 하나를 아래 형식에 따라 제공합니다.

        1. 제목은 ◆로 감싸고, 괄호 안에 정확한 재료 용량을 작성하세요.
        예: ◆계란 볶음밥◆(계란 2개, 밥 1공기, 간장 1스푼)

        2. 조리 단계는 반드시 `○숫자.`로 시작해야 하며, 한 줄씩 나열합니다.
        예:
        ○1. 계란을 풀어 팬에 볶는다.
        ○2. 밥과 간장을 넣고 볶는다.

        ❗꼭 한 가지 요리만 제공하세요.
        ❗형식을 반드시 지키고, 여는 멘트나 설명은 넣지 마세요.
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
                            // ★ 여기를 recipes.joinToString("\n") 대신 response로 변경 ★
                            resultText = response.trim(),
                            reasonText = details.joinToString("\n"),

                        )
                    }

                    Log.d("AiHelper", "✅ UI 상태 업데이트 완료")

                    // 3) 이미지 생성 프롬프트 구성
                    val title = recipes.firstOrNull() ?: "Delicious Dish"
                    val imgPrompt = """
                        A hyper-realistic, top-down shot of a beautifully plated gourmet dish called "$title", 
                        with glistening sauce, fresh herbs and microgreens garnish, steam gently rising 
                        from the center, soft natural window light casting delicate shadows, 
                        shallow depth of field to blur the background, vibrant colors highlighting texture 
                            and freshness, styled on a rustic wooden table.
                        """.trimIndent()
                    // 이미지 생성 시작 직전에

                    _uiState.update { it.copy(/* 이미 isSending=true */) }

                    // 4) DALL·E 3 호출
                    apiClient.generateImage(
                        prompt   = imgPrompt,
                        size     = "1024x1024",
                        onSuccess = { url ->
                            Log.i("AI ChatTest","1차 DALL 호출")
                            // 성공 시 UI 상태에 URL 반영
                            _uiState.update {
                                Log.i("AI ChatTest","2차 DALL 호출")
                                it.copy(
                                    imageUrl = url,
                                    isSending = false      // 텍스트+이미지 모두 끝나면 꺼주기
                                )
                            }
                        },
                        onError = { err ->
                            Log.e("AiHelper", "Image gen failed: $err")
                            _uiState.update { it.copy(isSending = false) }
                        }
                    )
                }
            },
            onError = { error ->
                Log.e("AiHelper", "❌ GPT API 오류: $error")
                _uiState.update { it.copy(isSending = false) }
            }
        )
        Log.i("AI ChatTest","Helper 호출됨.")
    }
}

