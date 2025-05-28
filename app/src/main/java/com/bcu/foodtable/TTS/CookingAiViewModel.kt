package com.bcu.foodtable.TTS // 사용자님의 패키지명인지 확인해주세요

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.bcu.foodtable.TTS.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CookingAiViewModel(
    private val application: Application,
    private val functions: FirebaseFunctions
) : ViewModel() {

    private var ttsHelper: TtsHelper? = null
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _evaluationApiResult = MutableStateFlow<String?>(null)
    val evaluationApiResult: StateFlow<String?> = _evaluationApiResult
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage
    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady
    init {
        Log.d("AI_TTS_VM_Lifecycle", "CookingAiViewModel init 시작됨.")
        ttsHelper = TtsHelper(application) { isSuccess ->
            Log.d("AI_TTS_VM_Lifecycle", "TtsHelper onInitialized 콜백 호출됨. 성공여부: $isSuccess")
            _isTtsReady.value = isSuccess
            if (!isSuccess) {
                _toastMessage.value = "음성 안내 기능을 시작하지 못했습니다."
                Log.w("AI_TTS_VM", "TTS Helper 초기화 실패 (콜백에서 확인).")
            } else {
                Log.i("AI_TTS_VM", "TTS Helper 초기화 성공 (콜백에서 확인).")
            }
        }
        Log.d("AI_TTS_VM_Lifecycle", "CookingAiViewModel init: TtsHelper 객체 생성 시도 완료.")
    }

    fun evaluateCookingRecipe(recipeImageUrl: String, userImageUri: Uri) {
        Log.d("AI_Eval_VM", "evaluateCookingRecipe 시작. 원본 URL: $recipeImageUrl, 사용자 URI: $userImageUri")
        _isLoading.value = true
        _evaluationApiResult.value = null
        _toastMessage.value = null

        if (recipeImageUrl.isBlank()) {
            Log.e("AI_Eval_VM_Input", "원본 레시피 이미지 URL 비어있음.")
            _toastMessage.value = "원본 레시피 이미지 정보가 없습니다."
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.e("AI_Eval_VM_Auth", "사용자 로그인 안됨.")
                _toastMessage.value = "로그인이 필요한 기능입니다."
                _isLoading.value = false
                return@launch
            }

            try {
                currentUser.getIdToken(true).addOnCompleteListener { tokenTask ->
                    if (tokenTask.isSuccessful) {
                        val idToken = tokenTask.result?.token
                        Log.d("AI_Eval_VM_Auth", "ID 토큰 새로고침 성공. 토큰(앞 20자): ${idToken?.take(20)}")

                        viewModelScope.launch {
                            var referenceImageBase64 = ""
                            var userImageBase64 = ""

                            try {
                                val referenceBytes = ImageUtils.urlToByteArray(recipeImageUrl)
                                if (referenceBytes.isEmpty()) {
                                    handleImageLoadingError("원본 레시피 이미지를 불러오는 데 실패했습니다.", "원본 이미지 바이트 변환 실패")
                                    return@launch
                                }
                                referenceImageBase64 = ImageUtils.byteArrayToBase64(referenceBytes)
                                if (referenceImageBase64.isBlank()) {
                                    handleImageLoadingError("원본 레시피 이미지 데이터 변환 실패.", "원본 Base64 비어있음")
                                    return@launch
                                }

                                val userBytes = ImageUtils.uriToByteArray(application, userImageUri)
                                if (userBytes.isEmpty()) {
                                    handleImageLoadingError("선택한 사용자 이미지를 불러오는 데 실패했습니다.", "사용자 이미지 바이트 변환 실패")
                                    return@launch
                                }
                                userImageBase64 = ImageUtils.byteArrayToBase64(userBytes)
                                if (userImageBase64.isBlank()) {
                                    handleImageLoadingError("사용자 이미지 데이터 변환 실패.", "사용자 Base64 비어있음")
                                    return@launch
                                }

                                val commonMimeType = "image/jpeg"
                                val inputData = hashMapOf(
                                    "userImageBase64" to userImageBase64,
                                    "referenceImageBase64" to referenceImageBase64,
                                    "mimeTypeUser" to commonMimeType,
                                    "mimeTypeReference" to commonMimeType
                                )
                                callEvaluateDishFunction(inputData)

                            } catch (e: Exception) {
                                Log.e("AI_Eval_VM_ImgEx", "이미지 로딩/변환 중 예외", e)
                                _toastMessage.value = "이미지 처리 중 오류: ${e.localizedMessage}"
                                _isLoading.value = false
                            }
                        }
                    } else {
                        Log.e("AI_Eval_VM_Auth", "ID 토큰 새로고침 실패.", tokenTask.exception)
                        _toastMessage.value = "인증 정보 갱신에 실패했습니다. 다시 시도해주세요."
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("AI_Eval_VM_OuterEx", "AI 평가 준비 중 외부 예외", e)
                _toastMessage.value = "AI 평가 준비 중 오류: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    private fun handleImageLoadingError(toastMsg: String, logMsg: String) {
        Log.e("AI_Eval_VM_Image", logMsg)
        _toastMessage.value = toastMsg
        _isLoading.value = false
    }

    private fun callEvaluateDishFunction(inputData: HashMap<String, String>) {
        Log.d("AI_Eval_VM_Call", "Cloud Function 'evaluateDish' 호출 시작...")
        functions.getHttpsCallable("evaluateDish")
            .call(inputData)
            .addOnSuccessListener { result ->
                _isLoading.value = false
                val resultData = result.getData()
                Log.d("AI_Result_VM_Raw", "Raw result data: $resultData")
                val evaluationData = resultData as? Map<String, Any>
                val evaluationText = evaluationData?.get("evaluation") as? String
                Log.d("AI_Result_VM_Success", "AI 평가 결과 텍스트 (원본): $evaluationText")

                if (!evaluationText.isNullOrBlank()) {
                    _evaluationApiResult.value = evaluationText // UI에는 원본 또는 약간 가공된 텍스트 표시

                    // --- TTS용 텍스트 가공 (이전과 동일) ---
                    var cleanedText = evaluationText
                    cleanedText = cleanedText.replace("**", "")
                    cleanedText = cleanedText.lines()
                        .map { line -> line.trim().removePrefix("*").trim() }
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                    cleanedText = cleanedText.replace(Regex("\\s{2,}"), " ").trim()
                    // --- TTS용 텍스트 가공 끝 ---

                    // --- "AI 셰프의 한마디" 추가 ---
                    val chefComments = listOf(
                        "자, AI 셰프가 당신의 요리를 꼼꼼히 살펴봤어요! 결과는 다음과 같습니다. ",
                        "AI 셰프의 평가 시간입니다! 당신의 요리는 과연 어떻게 평가되었을까요? ",
                        "흥미로운 요리군요! AI 셰프가 내린 평가는 이렇습니다. "
                    )
                    val randomChefComment = chefComments.random() // 여러 멘트 중 하나를 랜덤으로 선택
                    val ttsFriendlyText = randomChefComment + cleanedText // 셰프 코멘트 + 가공된 평가 내용
                    // --- "AI 셰프의 한마디" 추가 끝 ---

                    _toastMessage.value = "AI 요리 평가 완료!"

                    if (_isTtsReady.value && ttsHelper != null) {
                        Log.d("AI_TTS_VM", "TTS 준비됨. 음성 출력 시도 (셰프 코멘트 + 가공된 텍스트): $ttsFriendlyText")
                        ttsHelper?.speak(ttsFriendlyText)
                    } else {
                        Log.w("AI_TTS_VM", "TTS 준비 안됨 또는 ttsHelper null. 음성 출력 불가. 가공된 텍스트: $cleanedText")
                    }
                } else {
                    _evaluationApiResult.value = null
                    _toastMessage.value = "AI 평가 결과를 받았지만 내용이 비어있습니다."
                    Log.w("AI_Result_VM_Success", "평가 결과 텍스트가 null이거나 비어있습니다.")
                }
            }
            .addOnFailureListener { ex ->
                _isLoading.value = false
                Log.e("AI_Result_VM_Fail", "Cloud Function 호출 실패", ex)
                val errorMessage = if (ex is FirebaseFunctionsException) {
                    "AI 평가 오류 (Code: ${ex.code}): ${ex.message}"
                } else {
                    "AI 평가 중 알 수 없는 오류: ${ex.localizedMessage}"
                }
                _toastMessage.value = errorMessage
                _evaluationApiResult.value = null
            }
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        ttsHelper?.shutdown()
        super.onCleared()
        Log.d("AI_Eval_VM", "ViewModel onCleared, TTS shutdown.")
    }
}