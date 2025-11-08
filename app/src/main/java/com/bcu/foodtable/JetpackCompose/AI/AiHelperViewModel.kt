package com.bcu.foodtable.JetpackCompose.AI

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.ai.OpenAIClient
import com.bcu.foodtable.useful.ApiKeyManager
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.UserManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
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

    // 생성 직후 미리보기 화면으로 바로 열고 싶을 때 사용할 콜백
    var onOpenAiRecipeScreen: ((RecipeItem) -> Unit)? = null

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

    // =======================
    // (1) AI 결과 모델
    // =======================
    data class AiStep(
        val title: String?,
        val description: String?,
        val method: String?,
        val duration: String?
    )

    data class AiRecipePayload(
        val recipe_title: String?,
        val ingredients: List<String>?,
        val steps: List<AiStep>?
    )

    // ── 정규식 & 정규화 유틸 (제목 중복/타이머 표기 깨짐 방지) ──────────────────────────────
    private val leadIndexRe     = Regex("""^\s*(?:[○\-\•\·]?\s*)?\d+[\.\)]\s*""")
    private val parenTitleRe    = Regex("""^\s*\(([^)]+)\)\s*""")
    private val noiseRe         = Regex("""[○\u25CB\u2460]\s*\d+\.\s*""")
    private val inlineTimerRe   = Regex("""\(\s*([a-zA-Z]+)\s*,\s*(\d{2}:\d{2}:\d{2})\s*\)\s*$""")

    private fun normalizeMethod(method: String?): String? {
        val m = (method ?: "").trim().lowercase()
        val allow = setOf("boil","rest","bake","fry","steam","saute","sauté","roast")
        return when {
            m in allow -> m.replace("sauté","saute")
            else -> null
        }
    }

    private fun normalizeDuration(d: String?): String? {
        val raw = (d ?: "").trim()
        return if (Regex("""^\d{2}:\d{2}:\d{2}$""").matches(raw)) raw else null
    }

    private fun sanitizeTitle(s: String?): String {
        val t = (s ?: "").replace("\n", " ").trim()
        if (t.isBlank()) return "단계"
        val noIdx = t.replace(leadIndexRe, "").trim()
        val onlyParen = parenTitleRe.matchEntire(noIdx)?.groupValues?.getOrNull(1)?.trim()
        val base = (onlyParen ?: noIdx)
        return base.takeIf { it.isNotBlank() } ?: "단계"
    }

    private fun sanitizeDesc(raw: String?, title: String): String {
        var d = (raw ?: "").replace("\n", " ").trim()
        if (d.isBlank()) return ""
        d = d.replace(noiseRe, "").replace(leadIndexRe, "").trim()

        // 맨 앞의 (제목) 만 조건부 제거 (replaceFirst(Regex, lambda) 없음 → 직접 처리)
        val m = parenTitleRe.find(d)
        if (m != null) {
            val inner = m.groupValues[1].trim()
            val replacement = if (inner.equals(title, ignoreCase = true)) "" else m.value
            d = d.replaceRange(m.range, replacement).trim()
        }

        // 앞에 동일한 제목 텍스트가 그대로 있을 때 제거
        if (d.startsWith(title)) {
            d = d.removePrefix(title).trim().removePrefix(":").trim()
        }

        // 설명 끝에 붙은 타이머 제거 (별도로 붙일 거라)
        d = d.replace(inlineTimerRe, "").trim()
        return d
    }

    /** WriteScreen 규격으로 order 문자열 생성 */
    private fun buildWriteScreenOrder(steps: List<AiStep>): String {
        return steps.mapIndexed { idx, st ->
            val title = sanitizeTitle(st.title)
            val desc  = sanitizeDesc(st.description, title)
            val method   = normalizeMethod(st.method)
            val duration = normalizeDuration(st.duration)
            val timer = if (method != null && duration != null) " ($method,$duration)" else ""
            "○${idx + 1}.(${title}) ${desc.ifBlank { title }}$timer"
                .replace(Regex("""\s+"""), " ")
                .trim()
        }.joinToString(" ")
    }

    /** ai_recipe 문서를 RecipeItem으로 변환(제목 보정 포함) — name만 신뢰 */
    fun aiDocToRecipeItem(doc: DocumentSnapshot): RecipeItem {
        val name        = (doc.getString("name") ?: "").trim()   // ★ 무조건 name 사용
        val imageUrl    = doc.getString("imageUrl") ?: ""
        val order       = doc.getString("order") ?: ""
        val ingredients = (doc.get("ingredients") as? List<String>) ?: emptyList()

        return RecipeItem(
            name = if (name.isNotBlank()) name else "새 레시피", // 1단계 제목 fallback 금지
            description = "",
            imageResId = imageUrl,
            clicked = 0,
            date = com.google.firebase.Timestamp.now(),
            order = order,
            id = "",
            authorId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
            authorName = "",
            priceInSalt = 0,
            C_categories = listOf("AI"),
            note = "",
            tags = emptyList(),
            ingredients = ingredients,
            contained_channel = "",
            estimatedCalories = null,
            likes = 0,
            likedUsers = emptyList(),
            cost = 0,
            isPurchased = false,
            duration = 0
        )
    }

    /** 코드펜스/잡텍스트가 섞여 들어오는 응답에서 JSON 오브젝트만 추출 */
    private fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start >= 0 && end > start) return raw.substring(start, end + 1).trim()
        return raw
            .replaceFirst(Regex("^```\\w*\\s*"), "")
            .replace(Regex("```\\s*$"), "")
            .trim()
    }

    // =======================
    // (2) sendMessage(context)
    // =======================
    fun sendMessage(context: Context) {
        val state = _uiState.value
        val input = state.inputText
        val point = state.userPoint

        if (input.isBlank() || point < aiUseCost || state.isSending) return

        Log.d("AiHelper", "🔄 AI 호출 시작: 입력 = $input, 포인트 = $point")
        _uiState.update { it.copy(isSending = true, showWarning = false) }

        // JSON 스키마 + 타이머 규칙 + 번호 금지
        val rule = """
당신은 요리 도우미 AI입니다. 사용자가 가진 재료로 만들 수 있는 **한 가지 요리**를 만듭니다.

출력은 **JSON 하나만** 반환하며, 추가 설명/코멘트 금지!
JSON 스키마:
{
  "recipe_title": string,
  "ingredients": [string, ...],
  "steps": [
    { "title": string, "description": string, "method": string|null, "duration": "HH:MM:SS"|null }
  ]
}

엄수 규칙:
- JSON 외의 텍스트, 머리말, 코드블록 금지
- title/description에 "1.", "1)" 같은 단계 번호 금지
- 시간 의존 단계는 method/duration 채우기, duration 포맷은 HH:MM:SS
- steps 최소 3개
- recipe_title은 간단·명확하게
""".trimIndent()

        val prompt = """
사용자 입력 재료 또는 조건:
$input

위 스키마의 **JSON**만 반환하세요.
""".trimIndent()

        apiClient.sendMessage(
            prompt = prompt,
            role = rule,
            onSuccess = { response ->
                Log.d("AiHelper", " GPT 응답 수신 완료(raw):\n$response")

                viewModelScope.launch {
                    try {
                        // JSON 파싱
                        val json = extractJsonObject(response)
                        Log.d("AiHelper", "🧩 extracted JSON = $json")
                        val payload = Gson().fromJson(json, AiRecipePayload::class.java)
                        val title = (payload.recipe_title ?: "Delicious Dish").trim()
                        val ingreds = payload.ingredients?.filter { it.isNotBlank() } ?: emptyList()
                        val steps = payload.steps?.takeIf { it.isNotEmpty() } ?: emptyList()

                        // WriteScreen 포맷 order 생성
                        val order = if (steps.isNotEmpty()) buildWriteScreenOrder(steps) else {
                            "○1.(${title}) 재료를 준비합니다 ○2.(조리) 조리합니다 ○3.(마무리) 접시에 담습니다"
                        }

                        // 포인트 차감 + UI 업데이트
                        val newPoint = point - aiUseCost
                        user?.point = newPoint
                        updateFieldById("user", user?.uid ?: "", "point", newPoint)

                        _uiState.update {
                            it.copy(
                                userPoint = newPoint,
                                inputText = "",
                                ingredients = ingreds,
                                recipes = listOf(title),
                                recipeDetails = ingreds,
                                resultText = order,
                                reasonText = ingreds.joinToString("\n"),
                            )
                        }

                        // 이미지 프롬프트 (한국어, 사진만)
                        val ingredientsText = if (ingreds.isNotEmpty()) ingreds.joinToString(", ") else input
                        val imgPrompt = """
[음식 사진 | 탑뷰(90°) | 정사각형]

요리명: "$title"
사용 가능한 재료(이것만 사용): $ingredientsText

목표: 메뉴 사진처럼 깔끔하고 현실적인 음식 사진 1장 생성
스타일: 모던, 자연광, 중립 배경, 부드러운 그림자
플레이팅: 1인분, 단정하고 식욕 돋게; 명시된 재료에 한해 최소한의 가니쉬만 허용
그릇/식기: 심플한 도자기 접시/그릇(브랜드/로고 표시 금지)

카메라: 50–85mm 상당, f/4, ISO 100–400, 높은 다이내믹 레인지
조명: 한쪽 창가 자연광 + 약한 반사판, 강한 하이라이트/난반사 금지

해야 할 것:
- 접시 전체가 프레임 안에 또렷하게 보이도록 구성
- 소스/재료의 질감·수분감이 자연스럽게 보이도록 표현

하지 말 것:
- 추가 재료, 소스, 사이드 메뉴, 글자, 로고, 워터마크 금지
- 젓가락/포크/나이프/냅킨/손/음료 등은 명시된 경우가 아니면 금지
- 과장된 판타지/비현실적 색감/추상 스타일 금지
- 포장재/브랜드 노출 금지

산출물: 사진 1장만. 캡션/텍스트/설명은 절대 포함하지 말 것.
""".trimIndent()

                        // 이미지 생성 → Storage 업로드 → ai_recipe 저장
                        _uiState.update { it.copy(isSending = true) }
                        apiClient.generateImage(
                            prompt   = imgPrompt,
                            size     = "1024x1024",
                            onSuccess = { dalleUrl ->
                                Log.i("AI ChatTest", "1차 DALL 호출")
                                viewModelScope.launch {
                                    try {
                                        val uid = user?.uid ?: return@launch
                                        val storageUrl = uploadImageToFirebaseStorage(context, dalleUrl, uid)

                                        // 1) Firestore 저장
                                        val aiRecipe = hashMapOf(
                                            "name"        to title,      // ✅ 반드시 AI가 준 요리명
                                            "imageUrl"    to storageUrl,
                                            "order"       to order,      // 타이머 포함 포맷
                                            "ingredients" to ingreds,
                                            "details"     to ingreds,
                                            "createdAt"   to System.currentTimeMillis()
                                        )
                                        val aiRecipeRef = FirebaseFirestore.getInstance()
                                            .collection("user").document(uid)
                                            .collection("ai_recipe").document()

                                        aiRecipeRef.set(aiRecipe)
                                            .addOnSuccessListener {
                                                // 2) 미리보기용 RecipeItem 즉시 전달 (제목은 무조건 AI title)
                                                val preview = RecipeItem(
                                                    name = title,
                                                    imageResId = storageUrl,
                                                    order = order,
                                                    ingredients = ingreds,
                                                    description = "",
                                                    clicked = 0,
                                                    date = com.google.firebase.Timestamp.now(),
                                                    id = aiRecipeRef.id,
                                                    authorId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                                    authorName = "",
                                                    priceInSalt = 0,
                                                    C_categories = listOf("AI"),
                                                    note = "",
                                                    tags = emptyList(),
                                                    contained_channel = "",
                                                    estimatedCalories = null,
                                                    likes = 0,
                                                    likedUsers = emptyList(),
                                                    cost = 0,
                                                    isPurchased = false,
                                                    duration = 0
                                                )

                                                onOpenAiRecipeScreen?.invoke(preview)

                                                _uiState.update {
                                                    it.copy(
                                                        imageUrl = storageUrl,
                                                        isSending = false,
                                                        done = true,
                                                        aiRecipeDocId = aiRecipeRef.id
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
                    } catch (e: Exception) {
                        // JSON 파싱 실패 → 폴백(이전 형식 대응) — 중복 방지 파서로 수정
                        Log.e("AiHelper", "JSON 파싱 실패, 폴백 적용: ${e.message}")

                        val ingredientRegex = """\{(.*?)\}""".toRegex()
                        val ingredients = ingredientRegex.findAll(response).map { it.groupValues[1] }.toList()

                        val recipeRegex = """◆(.*?)◆""".toRegex()
                        val recipes = recipeRegex.findAll(response).map { it.groupValues[1] }.toList()
                        val title = recipes.firstOrNull() ?: "Delicious Dish"

                        val lines = response.lines().map { it.trim() }.filter {
                            it.startsWith("○") || it.firstOrNull()?.isDigit() == true
                        }
                        val parsedSteps = if (lines.isNotEmpty()) {
                            lines.map { raw ->
                                val noIdx = raw.replace(leadIndexRe, "").trim()
                                val mt = parenTitleRe.find(noIdx)
                                val t = mt?.groupValues?.getOrNull(1)?.trim().orEmpty()
                                val body = noIdx.removePrefix(mt?.value ?: "").trim()
                                    .replace(inlineTimerRe, "").trim()
                                AiStep(
                                    title = t.ifBlank { "단계" },
                                    description = body,
                                    method = null,
                                    duration = null
                                )
                            }
                        } else emptyList()
                        val order = if (parsedSteps.isNotEmpty()) buildWriteScreenOrder(parsedSteps) else
                            "○1.(${title}) 재료를 준비합니다 ○2.(조리) 조리합니다 ○3.(마무리) 접시에 담습니다"

                        val newPoint = point - aiUseCost
                        user?.point = newPoint
                        updateFieldById("user", user?.uid ?: "", "point", newPoint)

                        _uiState.update {
                            it.copy(
                                userPoint = newPoint,
                                inputText = "",
                                ingredients = if (ingredients.isNotEmpty()) ingredients else state.ingredients,
                                recipes = if (recipes.isNotEmpty()) recipes else listOf(title),
                                recipeDetails = if (ingredients.isNotEmpty()) ingredients else state.recipeDetails,
                                resultText = order,
                                reasonText = (if (ingredients.isNotEmpty()) ingredients else state.recipeDetails).joinToString("\n")
                            )
                        }
                    }
                }
            },
            onError = { error ->
                Log.e("AiHelper", "❌ GPT API 오류: $error")
                _uiState.update { it.copy(isSending = false) }
            }
        )
        Log.i("AI ChatTest", "Helper 호출됨.")
    }

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

        val bitmap = withContext(Dispatchers.IO) {
            Glide.with(context).asBitmap().load(imageUrl).submit().get()
        }
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val data = baos.toByteArray()

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
        return imagePath // Storage 경로 반환
    }

    suspend fun uploadAiImageAndSaveUrl(imageBytes: ByteArray, firestoreDocRef: DocumentReference) {
        try {
            val fileName = "${System.currentTimeMillis()}.jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child("ai_recipes/$fileName")
            storageRef.putBytes(imageBytes).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            firestoreDocRef.update("imageResId", downloadUrl).await()
            Log.d("UploadAiImage", "AI image uploaded and URL saved: $downloadUrl")
        } catch (e: Exception) {
            Log.e("UploadAiImage", "Failed to upload AI image or save URL", e)
        }
    }
}
