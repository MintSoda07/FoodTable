package com.bcu.foodtable.JetpackCompose

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.RecipeItem
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.data.UserBehaviorTracker
import com.bcu.foodtable.ai.AIRecommendationService
import com.bcu.foodtable.manager.TimeBasedRecommendationManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class HomeViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val behaviorTracker: UserBehaviorTracker? = null,
    private val aiService: AIRecommendationService? = null,
    private val timeManager: TimeBasedRecommendationManager? = null
) : ViewModel() {

    private val _recipes = MutableStateFlow<List<RecipeItem>>(emptyList())
    val recipes: StateFlow<List<RecipeItem>> = _recipes

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed

    // 🤖 AI 기반 추천 상태들
    private val _aiTimeRecommendation = MutableStateFlow<AIRecommendationService.TimeBasedRecommendation?>(null)
    val aiTimeRecommendation: StateFlow<AIRecommendationService.TimeBasedRecommendation?> = _aiTimeRecommendation.asStateFlow()

    private val _personalizedRecommendation = MutableStateFlow<AIRecommendationService.TimeBasedRecommendation?>(null)
    val personalizedRecommendation: StateFlow<AIRecommendationService.TimeBasedRecommendation?> = _personalizedRecommendation.asStateFlow()

    // ⏰ 시간 관리 상태들
    private val _currentTimeGreeting = MutableStateFlow("")
    val currentTimeGreeting: StateFlow<String> = _currentTimeGreeting.asStateFlow()

    private val _isRecommendationLoading = MutableStateFlow(false)
    val isRecommendationLoading: StateFlow<Boolean> = _isRecommendationLoading.asStateFlow()

    // 📊 사용자 행동 분석 상태들
    private val _userPreferences = MutableStateFlow<List<String>>(emptyList())
    val userPreferences: StateFlow<List<String>> = _userPreferences.asStateFlow()

    private val _categoryViewCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val categoryViewCounts: StateFlow<Map<String, Int>> = _categoryViewCounts.asStateFlow()

    // 🔄 추천 업데이트 타이머
    private var recommendationUpdateTimer: kotlinx.coroutines.Job? = null

    init {
        // 초기화: 시간 관리자와 행동 추적기 설정
        setupTimeManager()
        setupBehaviorTracker()
        startRecommendationUpdateTimer()
        FirebaseFirestore.getInstance()
            .collection("recipe")
            .addSnapshotListener { snaps, e ->
                if (e != null) return@addSnapshotListener
                val list = snaps?.documents
                    ?.mapNotNull { it.toObject(RecipeItem::class.java)?.apply { id = it.id } }
                    ?: emptyList()
                _recipes.value = list
            }
    }



    /**
     * 🤖 AI 기반 시간대별 추천을 업데이트합니다.
     */
    suspend fun updateTimeBasedRecommendation() {
        try {
            _isRecommendationLoading.value = true
            Log.d("HomeViewModel", "🤖 AI 시간대별 추천 업데이트 시작")

            val userName = _user.value?.name ?: "회원"
            timeManager?.updateAIRecommendation(userName)
            
            // 시간 관리자에서 추천 가져오기
            val recommendation = timeManager?.aiRecommendation?.value
            _aiTimeRecommendation.value = recommendation

            Log.d("HomeViewModel", "✅ AI 시간대별 추천 업데이트 완료: ${recommendation?.mainDish}")

        } catch (e: Exception) {
            Log.e("HomeViewModel", "❌ AI 시간대별 추천 업데이트 실패", e)
        } finally {
            _isRecommendationLoading.value = false
        }
    }

    /**
     * 👤 사용자 맞춤 AI 추천을 업데이트합니다.
     */
    suspend fun updatePersonalizedRecommendation() {
        try {
            Log.d("HomeViewModel", "👤 사용자 맞춤 추천 업데이트 시작")

            val recommendation = timeManager?.getPersonalizedRecommendation()
            _personalizedRecommendation.value = recommendation

            Log.d("HomeViewModel", "✅ 사용자 맞춤 추천 업데이트 완료: ${recommendation?.mainDish}")

        } catch (e: Exception) {
            Log.e("HomeViewModel", "❌ 사용자 맞춤 추천 업데이트 실패", e)
        }
    }

    /**
     * 🔍 레시피 조회 행동을 추적합니다.
     * @param recipeId 조회한 레시피 ID
     * @param categories 해당 레시피의 카테고리들
     */
    fun trackRecipeView(recipeId: String, categories: List<String>) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                behaviorTracker?.trackRecipeView(recipeId, categories, userId)
                
                // 행동 추적 후 맞춤 추천 업데이트
                updatePersonalizedRecommendation()
                
                Log.d("HomeViewModel", "🔍 레시피 조회 추적 완료: $recipeId")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ 레시피 조회 추적 실패", e)
            }
        }
    }

    /**
     * ⏰ 현재 시간과 인사말을 업데이트합니다.
     */
    fun updateCurrentTime() {
        timeManager?.updateCurrentTime()
        
        // 인사말 업데이트
        val greeting = timeManager?.timeBasedGreeting?.value ?: ""
        _currentTimeGreeting.value = greeting
        
        Log.d("HomeViewModel", "⏰ 시간 업데이트: $greeting")
    }

    /**
     * 📊 사용자 선호도 점수를 계산합니다.
     * @param categories 평가할 카테고리들
     * @return 선호도 점수 (0-100)
     */

    /**
     * 📱 시간 관리자 설정
     */
    private fun setupTimeManager() {
        timeManager?.let { manager ->
            // 시간대별 인사말 관찰
            viewModelScope.launch {
                manager.timeBasedGreeting.collect { greeting ->
                    _currentTimeGreeting.value = greeting
                }
            }

            // AI 추천 관찰
            viewModelScope.launch {
                manager.aiRecommendation.collect { recommendation ->
                    _aiTimeRecommendation.value = recommendation
                }
            }
        }
    }

    /**
     * 📊 행동 추적기 설정
     */
    private fun setupBehaviorTracker() {
        behaviorTracker?.let { tracker ->
            // 사용자 선호 카테고리 관찰
            viewModelScope.launch {
                tracker.preferredCategories.collect { preferences ->
                    _userPreferences.value = preferences
                }
            }

            // 카테고리별 조회 횟수 관찰
            viewModelScope.launch {
                tracker.categoryViewCounts.collect { counts ->
                    _categoryViewCounts.value = counts
                }
            }
        }
    }

    /**
     * 🔄 추천 자동 업데이트 타이머 시작
     * 5분마다 시간을 체크하고 필요시 추천을 업데이트합니다.
     */
    private fun startRecommendationUpdateTimer() {
        recommendationUpdateTimer = viewModelScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000L) // 5분마다 실행
                
                try {
                    updateCurrentTime()
                    
                    // 매 시간 정각에 추천 업데이트
                    val currentMinute = timeManager?.currentKoreanTime?.value?.minute ?: 0
                    if (currentMinute in 0..4) { // 정각 근처 5분 내
                        Log.d("HomeViewModel", "🔄 정시 추천 업데이트 실행")
                        updateTimeBasedRecommendation()
                    }
                    
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "❌ 자동 업데이트 실패", e)
                }
            }
        }
    }

    /**
     * 🚀 전체 시스템 초기화 및 데이터 로딩
     */
    fun initializeRecommendationSystem() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "🚀 추천 시스템 초기화 시작")

                // 1. 기본 데이터 로딩
                loadUserInfo()
                loadRecipes()

                // 2. 시간 정보 업데이트
                updateCurrentTime()

                // 3. AI 추천 초기화
                delay(1000) // 사용자 정보 로딩 대기
                updateTimeBasedRecommendation()
                updatePersonalizedRecommendation()

                Log.d("HomeViewModel", "✅ 추천 시스템 초기화 완료")

            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ 추천 시스템 초기화 실패", e)
            }
        }
    }


    // ✅ 기존 레시피 로딩 로직 유지 (MyRecipeStorageScreen 방식)
    fun loadRecipes() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadFailed.value = false
            try {
                _recipes.value = fetchRecipes()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "레시피 로딩 실패", e)
                _recipes.value = emptyList()
                _loadFailed.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

//    private suspend fun fetchRecipes(): List<RecipeItem> = kotlinx.coroutines.coroutineScope {
//        try {
//            Log.d("HomeViewModel", "Firestore에서 recipe 컬렉션 조회 시작")
//
//            val snapshot = db.collection("recipe").get().await()
//
//            val recipeList = snapshot.documents.map { document ->
//                async {
//                    try {
//                        val recipe = document.toObject(RecipeItem::class.java)
//                        if (recipe != null) {
//                            if (recipe.id.isBlank()) {
//                                recipe.copy(id = document.id)
//                            } else {
//                                recipe
//                            }
//                        } else {
//                            Log.w("HomeViewModel", "레시피 변환 실패: ${document.id}")
//                            null
//                        }
//                    } catch (e: Exception) {
//                        Log.e("HomeViewModel", "개별 레시피 로드 실패: ${document.id}", e)
//                        null
//                    }
//                }
//            }.awaitAll().filterNotNull()
//
//            Log.d("HomeViewModel", "총 불러온 레시피 수: ${recipeList.size}")
//            recipeList
//
//        } catch (e: Exception) {
//            Log.e("HomeViewModel", "레시피 컬렉션 조회 실패", e)
//            emptyList()
//        }
//    }
private suspend fun fetchRecipes(): List<RecipeItem> = kotlinx.coroutines.coroutineScope {
    try {
        Log.d("HomeViewModel", "🚀 Firestore에서 recipe 컬렉션 조회 시작")

        val snapshot = db.collection("recipe").get().await()
        Log.d("HomeViewModel", "📥 recipe 문서 수: ${snapshot.documents.size}")

        val recipeList = snapshot.documents.mapIndexed { index, document ->
            async {
                try {
                    val recipe = document.toObject(RecipeItem::class.java)
                    Log.d("HomeViewModel", "✅ [$index] 레시피 파싱 성공: ${recipe?.name ?: "null"}")

                    if (recipe != null) {
                        if (recipe.id.isBlank()) {
                            recipe.copy(id = document.id)
                        } else {
                            recipe
                        }
                    } else {
                        Log.w("HomeViewModel", "⚠️ [$index] toObject 결과 null: ${document.id}")
                        null
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "❌ [$index] 개별 레시피 로드 실패: ${document.id}", e)
                    null
                }
            }
        }.awaitAll().filterNotNull()

        Log.d("HomeViewModel", "✅ 총 레시피 수: ${recipeList.size}")
        recipeList

    } catch (e: Exception) {
        Log.e("HomeViewModel", "❌ 레시피 컬렉션 전체 조회 실패", e)
        emptyList()
    }.also {
        Log.d("HomeViewModel", "🎯 fetchRecipes 완료. 결과 레시피 수: ${it.size}")
    }
}


    fun loadUserInfo() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "Firestore에서 사용자 정보 조회 시작")

                val snapshot = db.collection("user").document(uid).get().await()
                val userData = snapshot.toObject(User::class.java)

                if (userData != null) {
                    Log.d("HomeViewModel", "유저 정보 로드 성공: ${userData.name}, 소금: ${userData.point}")
                    _user.value = userData
                } else {
                    Log.e("HomeViewModel", "유저 정보 변환 실패")
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "유저 정보 로드 실패: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 타이머 정리
        recommendationUpdateTimer?.cancel()
        Log.d("HomeViewModel", "🧹 HomeViewModel 정리 완료")
    }
}
