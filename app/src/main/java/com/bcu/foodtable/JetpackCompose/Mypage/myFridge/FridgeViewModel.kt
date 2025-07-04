package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.bcu.foodtable.ai.OpenAIClient
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID


//데이터 받을 데이터 클래스
data class IngredientExtracted(
    val name: String,
    val quantity: Int
)

class FridgeViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val uid = Firebase.auth.currentUser?.uid ?: ""

    private val openAIClient = OpenAIClient().apply {
        setAIWithAPI(
            onSuccess = { println("OpenAI API ready") },
            onError = { error -> println("OpenAI API failed: $error") }
        )
    }
    var ingredientList by mutableStateOf<List<Ingredient>>(emptyList())
        private set

    // 재료 로딩
    fun loadIngredients() {
        if (uid == null) return

        db.collection("user").document(uid).collection("fridge")
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { doc ->
                    val item = doc.toObject(Ingredient::class.java)
                    // 문서 ID를 item에 추가로 저장하려면 여기에 처리 (예: item.id = doc.id)
                    item?.copy(docId = doc.id)
                }
                ingredientList = items
            }
            .addOnFailureListener {
                println(" loadIngredients 실패: ${it.message}")
            }
    }
    // 재료 추가
    fun addIngredient(item: Ingredient, section: String = "냉장", onSuccess: () -> Unit = {}) {
        if (uid == null) return

        val itemWithSection = item.copy(section = section)

        db.collection("user").document(uid).collection("fridge")
            .add(itemWithSection)
            .addOnSuccessListener {
                loadIngredients()
                onSuccess()
            }
            .addOnFailureListener {
                println(" addIngredient 실패: ${it.message}")
            }
    }
    // 재료 이동
    fun updateIngredientSection(id: String, newSection: String) {
        val docRef = db.collection("user").document(uid).collection("fridge")
            .whereEqualTo("id", id)

        docRef.get().addOnSuccessListener { documents ->
            for (doc in documents) {
                doc.reference.update("section", newSection)
            }
        }
    }

    // 레시피 찾기 예시
    fun findRecipesByIngredient(name: String): List<String> {
        // TODO: 여기는 추후 Firestore에서 레시피 ingredients 검색으로 바꿀 수 있음
        val normalized = name.trim().lowercase()
        val dummyRecipes = mapOf(
            "계란" to listOf("계란말이", "스크램블 에그", "계란국"),
            "당근" to listOf("당근라페", "당근볶음", "야채주먹밥"),
            "소고기" to listOf("불고기", "소고기무국"),
            "상추" to listOf("쌈밥", "상추겉절이"),
            "양파" to listOf("양파볶음", "카레")
        )
        return dummyRecipes.entries.firstOrNull {
            it.key == normalized
        }?.value ?: listOf("추천 레시피 없음")
    }
    fun extractIngredientsWithQuantityUsingAI(
        ocrText: String,
        onResult: (List<IngredientExtracted>) -> Unit,
        onError: (String) -> Unit
    ) {
        val rolePrompt = "당신은 영수증에서 구매한 음식의 메뉴명 또는 음식 재료명과 해당 수량만 정확히 추출하는 AI입니다."

        val userPrompt = """
아래는 음식점 또는 마트에서 받은 영수증의 OCR 텍스트입니다. 이 텍스트에서 구매한 음식의 메뉴명 또는 음식 재료명과 각 항목의 수량만 정확하게 추출하여 JSON 배열 형태로 알려주세요.

[반드시 지켜야 할 규칙]
1. 메뉴명, 음식 재료명, 상품명으로 명시된 항목만 추출합니다.
2. "단가", "금액", "부가세", "판매금액", "과세공급가액", "신용카드", "주문번호", 날짜, 주소, 사업자번호 등 메뉴명이나 재료명이 아닌 정보는 절대 포함하지 않습니다.
3. 메뉴명이나 음식 재료가 아닌 항목은 절대 반환하지 않습니다.
4. 숫자만 있고 이름이 없는 항목은 절대 포함하지 않습니다.

[응답 형식 예시]
[
    {"name": "부대찌개", "quantity": 1},
    {"name": "계란", "quantity": 10},
    {"name": "돼지고기", "quantity": 2}
]

[영수증 텍스트 시작]
$ocrText
[영수증 텍스트 끝]

위의 규칙을 엄격히 지켜 JSON 배열 형태로 메뉴명 또는 음식 재료명과 수량만 반환하세요.
""".trimIndent()


        openAIClient.sendMessage(
            prompt = userPrompt,
            role = rolePrompt,
            onSuccess = { response ->
                Log.d("AI_RESPONSE", response) // AI의 원본 응답 로그로 출력
                val ingredients = parseIngredientsWithQuantity(response)
                CoroutineScope(Dispatchers.Main).launch {
                    if (ingredients.isNotEmpty()) {
                        onResult(ingredients)
                    } else {
                        onError("AI가 유효한 메뉴를 찾지 못했습니다.")
                    }
                }
            },
            onError = { error ->
                CoroutineScope(Dispatchers.Main).launch {
                    onError(error)
                }
            }
        )
    }


    // API 응답 결과를 IngredientExtracted 객체로 변환
    private fun parseIngredientsWithQuantity(response: String): List<IngredientExtracted> {
        return try {
            // JSON 부분만 정확히 추출하기 위한 정규표현식
            val jsonRegex = "\\[.*\\]".toRegex(RegexOption.DOT_MATCHES_ALL)
            val jsonMatch = jsonRegex.find(response)

            jsonMatch?.let {
                Gson().fromJson(it.value, Array<IngredientExtracted>::class.java).toList()
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("AI_PARSE_ERROR", "Parsing failed: ${e.message}")
            emptyList()
        }
    }
    fun sendToClovaOCR(
        base64Image: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val client = OkHttpClient()

        // 1) JSON 페이로드 구성
        val json = JSONObject().apply {
            put("version", "V2")
            put("requestId", UUID.randomUUID().toString())
            put("timestamp", System.currentTimeMillis())
            put("images", JSONArray().apply {
                put(JSONObject().apply {
                    put("format", "jpg")
                    put("name", "receipt")
                    put("data", base64Image)
                })
            })
        }
        Log.d("ClovaOCR", "페이로드 준비: ${json.toString().take(200)}...")

        // 2) 요청 빌드
        val url = "https://clovaocr-api-kr.ncloud.com/external/v1/43752/deb068eed76ffce2572f356a69cb8fc8f2b0eda008f48d9d2cb0502c0f11bc45"
        val secret = "enNXc0d3SGh5ZEtQWHVPQUVwT2d2UFhHcXNJa3p0Z2E="
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-OCR-SECRET", secret)
            .post(RequestBody.create("application/json".toMediaType(), json.toString()))
            .build()
        Log.d("ClovaOCR", "요청 생성: URL=$url, SECRET=${secret.takeLast(4).padStart(secret.length, '*')}")

        // 3) 네트워크 호출
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ClovaOCR", "네트워크 호출 실패", e)
                Handler(Looper.getMainLooper()).post {
                    onError("네트워크 오류: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("ClovaOCR", "응답 도착: code=${response.code}")
                val body = response.body?.string()
                Log.d("ClovaOCR", "응답 바디: ${body?.take(500)}")

                if (!response.isSuccessful || body == null) {
                    Log.e("ClovaOCR", "비정상 응답: code=${response.code}")
                    Handler(Looper.getMainLooper()).post {
                        onError("응답 실패: ${response.code}")
                    }
                    return
                }

                try {
                    Log.d("ClovaOCR", "파싱 시작")
                    val images = JSONObject(body).getJSONArray("images")
                    val fields = images.getJSONObject(0).getJSONArray("fields")
                    val lines = List(fields.length()) { i ->
                        fields.getJSONObject(i).getString("inferText")
                    }
                    val text = lines.joinToString("\n")
                    Log.d("ClovaOCR", "파싱 완료, 텍스트 길이=${text.length}")
                    Handler(Looper.getMainLooper()).post {
                        onSuccess(text)
                    }
                } catch (e: Exception) {
                    Log.e("ClovaOCR", "파싱 중 오류", e)
                    Handler(Looper.getMainLooper()).post {
                        onError("파싱 오류: ${e.message}")
                    }
                }
            }
        })
    }







}

fun encodeImageToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

object GlobalTray {
    val items = mutableStateListOf<Ingredient>()
}