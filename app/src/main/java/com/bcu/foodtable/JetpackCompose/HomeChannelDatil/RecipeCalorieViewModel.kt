package com.bcu.foodtable.JetpackCompose.HomeChannelDatil

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.bcu.foodtable.ai.OpenAIClient
import com.bcu.foodtable.useful.ApiKeyManager
import com.google.firebase.firestore.FirebaseFirestore
import com.bcu.foodtable.useful.RecipeItem


class RecipeCalorieViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    // ① 마지막으로 본 (ingredients, order) 페어를 저장해 두는 맵
    private val _lastDataMap = mutableMapOf<String, Pair<List<String>, String>>()

    // ② recipe.id 별 AI 추정 칼로리 캐시
    private val _caloriesMap = mutableStateMapOf<String, String?>()
    val caloriesMap: Map<String, String?> = _caloriesMap

    // OpenAIClient 인스턴스 + API 키 설정
    private val openAI: OpenAIClient = OpenAIClient().apply {
        ApiKeyManager.getGptApi()?.let { apiKeyInfo = it }
    }
    /** 화면을 떠날 때나 강제로 캐시를 비우고 싶으면 호출 */
    fun clearCache(recipeId: String) {
        _lastDataMap.remove(recipeId)
        _caloriesMap.remove(recipeId)
    }


    /** 재료나 순서가 바뀌면 캐시 삭제 후, 없으면 Firestore/AI 호출 */
    fun loadOrEstimateCalories(recipe: RecipeItem) {
        val id = recipe.id
        val currentPair = recipe.ingredients to recipe.order

        // 입력 정보가 바뀌었으면 이전 캐시 제거
        if (_lastDataMap[id] != currentPair) {
            _lastDataMap[id] = currentPair
            _caloriesMap.remove(id)
        }

        // 캐시에 없으면 Firestore 확인 → 없으면 AI 호출
        if (_caloriesMap[id].isNullOrBlank()) {
            firestore.collection("recipe").document(id).get()
                .addOnSuccessListener { doc ->
                    val existing = doc.getString("estimatedCalories")
                    if (!existing.isNullOrBlank()) {
                        _caloriesMap[id] = existing
                    } else {
                        estimateAndSave(recipe)
                    }
                }
                .addOnFailureListener {
                    estimateAndSave(recipe)
                }
        }
    }

    /** AI에 프롬프트 보내서 추정 후, 로컬/Firestore에 저장 */
    private fun estimateAndSave(recipe: RecipeItem) {
        val prompt = """
            재료: ${recipe.ingredients.joinToString(", ")}
            순서: ${recipe.order}
            이 레시피의 예상 칼로리를 숫자+단위만 알려주세요. 예: "350 kcal"
        """.trimIndent()

        openAI.sendMessage(
            prompt   = prompt,
            role     = "칼로리 추정 AI",
            onSuccess = { resp ->
                val cal = resp.trim()
                _caloriesMap[recipe.id] = cal
                firestore.collection("recipe")
                    .document(recipe.id)
                    .update("estimatedCalories", cal)
            },
            onError = {
                _caloriesMap[recipe.id] = "0 kcal"
            }
        )
    }
}
