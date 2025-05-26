package com.bcu.foodtable.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * 사용자 행동 추적 시스템
 * 사용자가 어떤 카테고리의 레시피를 얼마나 자주 보는지 추적하고 분석합니다.
 */
class UserBehaviorTracker(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "UserBehaviorTracker"
        private const val PREFS_NAME = "user_behavior_prefs"
        private const val KEY_VIEW_COUNTS = "category_view_counts"
        private const val COLLECTION_USER_BEHAVIOR = "user_behavior"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 사용자의 카테고리별 조회 횟수를 저장하는 StateFlow
    private val _categoryViewCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryViewCounts: StateFlow<Map<String, Int>> = _categoryViewCounts.asStateFlow()

    // 사용자의 선호 카테고리 리스트 (조회 빈도 기준 상위 카테고리들)
    private val _preferredCategories = MutableStateFlow<List<String>>(emptyList())
    val preferredCategories: StateFlow<List<String>> = _preferredCategories.asStateFlow()

    init {
        // 앱 시작 시 로컬 저장된 행동 데이터 로드
        loadLocalBehaviorData()

        // Firebase에서 사용자 행동 데이터 동기화 (코루틴으로 처리)
        CoroutineScope(Dispatchers.IO).launch {
            syncBehaviorDataFromFirebase()
        }
    }

    /**
     * 레시피 조회 행동을 기록합니다.
     * @param recipeId 조회한 레시피 ID
     * @param categories 해당 레시피의 카테고리 리스트
     * @param userId 현재 사용자 ID
     */
    suspend fun trackRecipeView(recipeId: String, categories: List<String>, userId: String?) {
        try {
            Log.d(TAG, "🔍 레시피 조회 추적: $recipeId, 카테고리: $categories")
            
            // 1. 로컬 카테고리별 조회 횟수 업데이트
            val currentCounts = _categoryViewCounts.value.toMutableMap()
            categories.forEach { category ->
                if (category.isNotBlank()) {
                    currentCounts[category] = (currentCounts[category] ?: 0) + 1
                }
            }
            _categoryViewCounts.value = currentCounts
            
            // 2. 선호 카테고리 업데이트 (상위 5개 카테고리)
            updatePreferredCategories(currentCounts)
            
            // 3. 로컬 저장소에 저장
            saveLocalBehaviorData(currentCounts)
            
            // 4. Firebase에 비동기적으로 업로드
            if (userId != null) {
                saveBehaviorDataToFirebase(userId, recipeId, categories, currentCounts)
            }
            
            Log.d(TAG, "✅ 사용자 행동 추적 완료. 현재 선호 카테고리: ${_preferredCategories.value}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 레시피 조회 추적 중 오류 발생", e)
        }
    }

    /**
     * 현재 사용자의 선호 카테고리를 반환합니다.
     * @param limit 반환할 선호 카테고리 개수 (기본값: 5개)
     * @return 조회 빈도 기준 상위 카테고리 리스트
     */
    fun getPreferredCategories(limit: Int = 5): List<String> {
        return _preferredCategories.value.take(limit)
    }

    /**
     * 특정 카테고리의 조회 횟수를 반환합니다.
     * @param category 확인할 카테고리명
     * @return 해당 카테고리의 조회 횟수
     */
    fun getCategoryViewCount(category: String): Int {
        return _categoryViewCounts.value[category] ?: 0
    }

    /**
     * 사용자의 선호도 점수를 계산합니다.
     * @param categories 평가할 카테고리 리스트
     * @return 선호도 점수 (0-100)
     */
    fun calculatePreferenceScore(categories: List<String>): Int {
        val totalViews = _categoryViewCounts.value.values.sum()
        if (totalViews == 0) return 0
        
        val categoryScore = categories.sumOf { category ->
            getCategoryViewCount(category)
        }
        
        // 정규화된 점수 계산 (0-100)
        return minOf(100, (categoryScore.toFloat() / totalViews * 100).toInt())
    }

    /**
     * 선호 카테고리를 업데이트합니다.
     */
    private fun updatePreferredCategories(categoryCounts: Map<String, Int>) {
        val sortedCategories = categoryCounts
            .filter { it.value > 0 }
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }
            .take(5)
        
        _preferredCategories.value = sortedCategories
        Log.d(TAG, "📊 선호 카테고리 업데이트: $sortedCategories")
    }

    /**
     * 로컬 저장소에서 행동 데이터를 로드합니다.
     */
    private fun loadLocalBehaviorData() {
        try {
            val savedData = prefs.getString(KEY_VIEW_COUNTS, "{}")
            val categoryCounts = parseCategoryCountsFromJson(savedData ?: "{}")
            _categoryViewCounts.value = categoryCounts
            updatePreferredCategories(categoryCounts)
            
            Log.d(TAG, "📱 로컬 행동 데이터 로드 완료: ${categoryCounts.size}개 카테고리")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 로컬 행동 데이터 로드 실패", e)
        }
    }

    /**
     * 로컬 저장소에 행동 데이터를 저장합니다.
     */
    private fun saveLocalBehaviorData(categoryCounts: Map<String, Int>) {
        try {
            val jsonData = convertCategoryCountsToJson(categoryCounts)
            prefs.edit().putString(KEY_VIEW_COUNTS, jsonData).apply()
            Log.d(TAG, "💾 로컬 행동 데이터 저장 완료")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 로컬 행동 데이터 저장 실패", e)
        }
    }

    /**
     * Firebase에서 사용자 행동 데이터를 동기화합니다.
     */
    private suspend fun syncBehaviorDataFromFirebase() {
        // TODO: 실제 사용자 ID를 가져오는 로직 구현 필요
        // 현재는 임시로 생략하고, 필요시 구현
    }

    /**
     * Firebase에 행동 데이터를 저장합니다.
     */
    private suspend fun saveBehaviorDataToFirebase(
        userId: String, 
        recipeId: String, 
        categories: List<String>,
        categoryCounts: Map<String, Int>
    ) {
        try {
            val behaviorData = hashMapOf(
                "userId" to userId,
                "recipeId" to recipeId,
                "categories" to categories,
                "viewedAt" to FieldValue.serverTimestamp(),
                "categoryCounts" to categoryCounts
            )
            
            firestore.collection(COLLECTION_USER_BEHAVIOR)
                .document(userId)
                .set(behaviorData)
                .await()
                
            Log.d(TAG, "☁️ Firebase 행동 데이터 저장 완료")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase 행동 데이터 저장 실패", e)
        }
    }

    /**
     * JSON 문자열을 카테고리 카운트 맵으로 파싱합니다.
     */
    private fun parseCategoryCountsFromJson(json: String): Map<String, Int> {
        return try {
            // 간단한 JSON 파싱 (실제로는 Gson이나 kotlinx.serialization 사용 권장)
            val map = mutableMapOf<String, Int>()
            if (json.length > 2) { // "{}" 보다 긴 경우에만 파싱
                val content = json.removeSurrounding("{", "}")
                if (content.isNotBlank()) {
                    content.split(",").forEach { pair ->
                        val keyValue = pair.split(":")
                        if (keyValue.size == 2) {
                            val key = keyValue[0].trim().removeSurrounding("\"")
                            val value = keyValue[1].trim().toIntOrNull() ?: 0
                            map[key] = value
                        }
                    }
                }
            }
            map
        } catch (e: Exception) {
            Log.e(TAG, "JSON 파싱 오류", e)
            emptyMap()
        }
    }

    /**
     * 카테고리 카운트 맵을 JSON 문자열로 변환합니다.
     */
    private fun convertCategoryCountsToJson(categoryCounts: Map<String, Int>): String {
        return try {
            // 간단한 JSON 직렬화
            val pairs = categoryCounts.map { "\"${it.key}\":${it.value}" }
            "{${pairs.joinToString(",")}}"
        } catch (e: Exception) {
            Log.e(TAG, "JSON 변환 오류", e)
            "{}"
        }
    }

    /**
     * 행동 데이터를 초기화합니다. (테스트나 사용자 요청 시 사용)
     */
    fun clearBehaviorData() {
        _categoryViewCounts.value = emptyMap()
        _preferredCategories.value = emptyList()
        prefs.edit().clear().apply()
        Log.d(TAG, "🗑️ 사용자 행동 데이터 초기화 완료")
    }
} 