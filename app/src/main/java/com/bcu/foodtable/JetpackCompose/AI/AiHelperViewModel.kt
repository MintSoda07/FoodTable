package com.bcu.foodtable.JetpackCompose.AI

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.waitUntilImageIsAvailable
import com.bcu.foodtable.ai.OpenAIClient
import com.bcu.foodtable.useful.ApiKeyManager
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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

    fun sendMessage(context: Context) {
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
                    val recipeRegex = """◆(.*?)◆""".toRegex()
                    val recipes = recipeRegex.findAll(response).map { it.groupValues[1] }.toList()
                    val recipeDetailsRegex = """◆.*?◆\((.*?)\)""".toRegex()
                    val details = recipeDetailsRegex.findAll(response).map { it.groupValues[1] }.toList()
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
                            resultText = response.trim(),
                            reasonText = details.joinToString("\n"),
                        )
                    }

                    val title = recipes.firstOrNull() ?: "Delicious Dish"
                    val ingredientsText = details.firstOrNull() ?: ingredients.joinToString(", ")

                    val imgPrompt = """
                    A realistic, top-down food photo of a dish called "$title", made **only** using these ingredients: $ingredientsText.
                    The dish should look exactly like a real "$title" as served at home or in a restaurant, using the listed ingredients, with no missing or extra items.
                    Present the dish authentically, with all ingredients accurately prepared and incorporated.
                    No fantasy, no additional decorations, no unrelated foods.
                    Simple background, focus on the food, natural lighting.
                """.trimIndent()

                    // 이미지 생성
                    _uiState.update { it.copy(isSending = true) }

                    apiClient.generateImage(
                        prompt   = imgPrompt,
                        size     = "1024x1024",
                        onSuccess = { dalleUrl ->
                            Log.i("AI ChatTest", "1차 DALL 호출")
                            viewModelScope.launch {
                                try {
                                    // 1. DALL-E 이미지 Firebase Storage에 업로드
                                    val uid = user?.uid ?: return@launch
                                    val storageUrl = uploadImageToFirebaseStorage(context, dalleUrl, uid)

                                    // 2. Firestore에 user/{uid}/ai_recipe 저장
                                    val aiRecipe = hashMapOf(
                                        "name"        to title,
                                        "imageUrl"    to storageUrl,
                                        "order"       to response.trim(),
                                        "ingredients" to ingredients,
                                        "details"     to details,
                                        "createdAt"   to System.currentTimeMillis()
                                    )
                                    val aiRecipeRef = FirebaseFirestore.getInstance()
                                        .collection("user").document(uid)
                                        .collection("ai_recipe").document()
                                    aiRecipeRef.set(aiRecipe)
                                        .addOnSuccessListener {
                                            _uiState.update {
                                                it.copy(
                                                    imageUrl = storageUrl,
                                                    isSending = false,
                                                    done = true,
                                                    aiRecipeDocId = aiRecipeRef.id // 이 필드를 FuturisticDialog에서 사용!
                                                )
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("AiHelper", "Firestore 저장 실패: ${e.message}")
                                            _uiState.update { it.copy(isSending = false) }
                                        }
                                } catch (e: Exception) {
                                    Log.e("AiHelper", "이미지 Storage 업로드 실패: ${e.message}")
                                    _uiState.update { it.copy(isSending = false) }
                                }
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
        Log.i("AI ChatTest", "Helper 호출됨.")
    }
    // 초기화?
    fun resetDone() {
        _uiState.update { it.copy(done = false) }
    }

    suspend fun uploadImageToFirebaseStorage(
        context: Context,
        imageUrl: String,
        userId: String
    ): String {
        val imagePath = "ai_recipes/${userId}/${System.currentTimeMillis()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child(imagePath)

        // Glide로 비트맵 받아오기 (네트워크 작업)
        val bitmap = withContext(Dispatchers.IO) {
            Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .submit()
                .get()
        }
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val data = baos.toByteArray()

        // Storage에 업로드 (downloadUrl X)
        suspendCoroutine<Unit> { cont ->
            storageRef.putBytes(data)
                .addOnSuccessListener {
                    Log.d("IMAGE_UPLOAD", "업로드 성공! path=$imagePath")
                    cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    Log.e("IMAGE_UPLOAD", "업로드 실패! message: ${e.message}", e)
                    cont.resumeWithException(e)
                }
        }

        // 다운로드 URL 대신 **Storage 경로를 반환**
        return imagePath
    }

    suspend fun uploadAiImageAndSaveUrl(imageBytes: ByteArray, firestoreDocRef: DocumentReference) {
        try {
            // 1. 스토리지 경로 생성 (예: ai_recipes/UUID.jpg)
            val fileName = "${System.currentTimeMillis()}.jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child("ai_recipes/$fileName")

            // 2. 이미지 업로드
            storageRef.putBytes(imageBytes).await()

            // 3. 업로드 후 다운로드 URL 가져오기
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // 4. Firestore에 imageResId를 다운로드 URL로 저장 (통일된 URL 형태)
            firestoreDocRef.update("imageResId", downloadUrl).await()

            Log.d("UploadAiImage", "AI image uploaded and URL saved: $downloadUrl")

        } catch (e: Exception) {
            Log.e("UploadAiImage", "Failed to upload AI image or save URL", e)
        }
    }




}

