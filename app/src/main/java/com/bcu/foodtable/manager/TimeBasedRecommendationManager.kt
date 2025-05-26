package com.bcu.foodtable.manager

import android.util.Log
import com.bcu.foodtable.ai.AIRecommendationService
import com.bcu.foodtable.data.UserBehaviorTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.*

/**
 * 한국 시간 기반 시간대별 추천 관리자
 * AI 서비스와 사용자 행동 분석을 통합하여 맞춤형 추천을 제공합니다.
 */
class TimeBasedRecommendationManager(
    private val aiService: AIRecommendationService,
    private val behaviorTracker: UserBehaviorTracker
) {
    companion object {
        private const val TAG = "TimeRecommendationMgr"
        private const val KOREA_TIMEZONE = "Asia/Seoul"
    }

    // 현재 한국 시간 정보
    private val _currentKoreanTime = MutableStateFlow(getCurrentKoreanTime())
    val currentKoreanTime: StateFlow<KoreanTimeInfo> = _currentKoreanTime.asStateFlow()

    // AI 기반 시간대별 추천 정보
    private val _aiRecommendation = MutableStateFlow<AIRecommendationService.TimeBasedRecommendation?>(null)
    val aiRecommendation: StateFlow<AIRecommendationService.TimeBasedRecommendation?> = _aiRecommendation.asStateFlow()

    // 시간대별 맞춤 인사말
    private val _timeBasedGreeting = MutableStateFlow("")
    val timeBasedGreeting: StateFlow<String> = _timeBasedGreeting.asStateFlow()

    // 추천 로딩 상태
    private val _isLoadingRecommendation = MutableStateFlow(false)
    val isLoadingRecommendation: StateFlow<Boolean> = _isLoadingRecommendation.asStateFlow()

    /**
     * 한국 시간 정보 데이터 클래스
     */
    data class KoreanTimeInfo(
        val hour: Int,                  // 시간 (0-23)
        val minute: Int,               // 분 (0-59)
        val timePhase: TimePhase,      // 시간대 구분
        val greeting: String,          // 기본 인사말
        val emoji: String             // 시간대별 이모지
    )

    /**
     * 시간대 구분 enum
     */
    enum class TimePhase(val displayName: String) {
        DAWN("새벽"),           // 0-5시
        MORNING("아침"),        // 6-9시  
        LATE_MORNING("오전"),   // 10-11시
        LUNCH("점심"),          // 12-14시
        AFTERNOON("오후"),      // 15-17시
        EVENING("저녁"),        // 18-21시
        NIGHT("밤")            // 22-23시
    }

    init {
        updateCurrentTime()
        updateTimeBasedGreeting()
    }

    /**
     * 현재 한국 시간 정보를 업데이트합니다.
     */
    fun updateCurrentTime() {
        val newTimeInfo = getCurrentKoreanTime()
        _currentKoreanTime.value = newTimeInfo
        updateTimeBasedGreeting()
        
        Log.d(TAG, "⏰ 한국 시간 업데이트: ${newTimeInfo.hour}:${newTimeInfo.minute.toString().padStart(2, '0')} (${newTimeInfo.timePhase.displayName})")
    }

    /**
     * AI 기반 시간대별 추천을 업데이트합니다.
     * @param userName 사용자 이름 (인사말에 사용)
     */
    suspend fun updateAIRecommendation(userName: String = "회원") {
        try {
            _isLoadingRecommendation.value = true
            Log.d(TAG, "🤖 AI 추천 업데이트 시작")

            val currentTime = _currentKoreanTime.value
            val userPreferences = behaviorTracker.preferredCategories.first()
            
            // AI 서비스를 통해 시간대별 추천 가져오기
            val recommendation = aiService.getTimeBasedRecommendation(
                currentHour = currentTime.hour,
                userPreferences = userPreferences
            )
            
            _aiRecommendation.value = recommendation
            
            Log.d(TAG, "✅ AI 추천 업데이트 완료: ${recommendation.mainDish}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ AI 추천 업데이트 실패", e)
            // 실패 시 기본 추천 설정
            _aiRecommendation.value = createFallbackRecommendation()
        } finally {
            _isLoadingRecommendation.value = false
        }
    }

    /**
     * 사용자 맞춤 추천을 가져옵니다.
     */
    suspend fun getPersonalizedRecommendation(): AIRecommendationService.TimeBasedRecommendation {
        return try {
            Log.d(TAG, "👤 사용자 맞춤 추천 요청")
            
            val userPreferences = behaviorTracker.preferredCategories.first()
            
            if (userPreferences.isNotEmpty()) {
                // 사용자 행동 데이터가 있는 경우 AI 맞춤 추천
                aiService.getPersonalizedRecommendation(userPreferences)
            } else {
                // 행동 데이터가 없는 경우 시간대별 기본 추천
                val currentTime = _currentKoreanTime.value
                aiService.getTimeBasedRecommendation(currentTime.hour, emptyList())
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 맞춤 추천 실패", e)
            createPersonalizedFallbackRecommendation()
        }
    }

    /**
     * 현재 한국 시간 정보를 가져옵니다.
     */
    private fun getCurrentKoreanTime(): KoreanTimeInfo {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(KOREA_TIMEZONE))
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val timePhase = when (hour) {
            in 0..5 -> TimePhase.DAWN
            in 6..9 -> TimePhase.MORNING
            in 10..11 -> TimePhase.LATE_MORNING
            in 12..14 -> TimePhase.LUNCH
            in 15..17 -> TimePhase.AFTERNOON
            in 18..21 -> TimePhase.EVENING
            else -> TimePhase.NIGHT
        }
        
        val (greeting, emoji) = getTimePhaseInfo(timePhase, hour)
        
        return KoreanTimeInfo(
            hour = hour,
            minute = minute,
            timePhase = timePhase,
            greeting = greeting,
            emoji = emoji
        )
    }

    /**
     * 시간대별 인사말과 이모지를 반환합니다.
     */
    private fun getTimePhaseInfo(timePhase: TimePhase, hour: Int): Pair<String, String> {
        return when (timePhase) {
            TimePhase.DAWN -> {
                if (hour <= 2) "아직 깨어계세요? 늦은 밤입니다" to "🌙"
                else "이른 새벽이네요! 좋은 하루 준비하세요" to "🌅"
            }
            TimePhase.MORNING -> "좋은 아침입니다! 상쾌한 하루 시작하세요" to "☀️"
            TimePhase.LATE_MORNING -> "활기찬 오전입니다! 오늘도 화이팅하세요" to "🌤️"
            TimePhase.LUNCH -> "점심시간이네요! 맛있는 한끼 드세요" to "🍽️"
            TimePhase.AFTERNOON -> "따뜻한 오후입니다! 달콤한 휴식은 어떠세요" to "🌞"
            TimePhase.EVENING -> "저녁시간입니다! 하루 수고하셨어요" to "🌇"
            TimePhase.NIGHT -> "편안한 밤입니다! 맛있는 저녁 드세요" to "🌃"
        }
    }

    /**
     * 시간대별 인사말을 업데이트합니다.
     */
    private fun updateTimeBasedGreeting() {
        val timeInfo = _currentKoreanTime.value
        val fullGreeting = "${timeInfo.greeting} ${timeInfo.emoji}"
        _timeBasedGreeting.value = fullGreeting
        
        Log.d(TAG, "💬 인사말 업데이트: $fullGreeting")
    }

    /**
     * AI 서비스 실패 시 기본 추천을 생성합니다.
     */
    private fun createFallbackRecommendation(): AIRecommendationService.TimeBasedRecommendation {
        val currentTime = _currentKoreanTime.value
        
        return when (currentTime.timePhase) {
            TimePhase.DAWN, TimePhase.NIGHT -> AIRecommendationService.TimeBasedRecommendation(
                mainDish = "따뜻한 우동",
                subDish = "김치전",
                dessert = "호떡",
                timeMessage = "${currentTime.greeting} ${currentTime.emoji}",
                recommendationReason = "늦은 시간에는 부담 없고 따뜻한 음식이 좋습니다."
            )
            TimePhase.MORNING -> AIRecommendationService.TimeBasedRecommendation(
                mainDish = "계란말이덮밥",
                subDish = "된장국",
                dessert = "바나나",
                timeMessage = "${currentTime.greeting} ${currentTime.emoji}",
                recommendationReason = "아침에는 영양가 있고 소화 잘 되는 음식이 좋습니다."
            )
            TimePhase.LATE_MORNING, TimePhase.AFTERNOON -> AIRecommendationService.TimeBasedRecommendation(
                mainDish = "샌드위치",
                subDish = "과일 샐러드",
                dessert = "아이스커피",
                timeMessage = "${currentTime.greeting} ${currentTime.emoji}",
                recommendationReason = "간식 시간에는 가볍고 맛있는 메뉴가 좋습니다."
            )
            TimePhase.LUNCH -> AIRecommendationService.TimeBasedRecommendation(
                mainDish = "김치찌개",
                subDish = "계란후라이",
                dessert = "수정과",
                timeMessage = "${currentTime.greeting} ${currentTime.emoji}",
                recommendationReason = "점심에는 든든하고 맛있는 한식이 최고입니다."
            )
            TimePhase.EVENING -> AIRecommendationService.TimeBasedRecommendation(
                mainDish = "제육볶음",
                subDish = "콩나물무침",
                dessert = "식혜",
                timeMessage = "${currentTime.greeting} ${currentTime.emoji}",
                recommendationReason = "저녁에는 풍성하고 맛있는 메뉴로 하루를 마무리하세요."
            )
        }
    }

    /**
     * 맞춤 추천 실패 시 기본 추천을 생성합니다.
     */
    private suspend fun createPersonalizedFallbackRecommendation(): AIRecommendationService.TimeBasedRecommendation {
        val preferences = behaviorTracker.preferredCategories.first()
        val mainCategory = preferences.firstOrNull() ?: "한식"
        
        return AIRecommendationService.TimeBasedRecommendation(
            mainDish = when (mainCategory) {
                "한식" -> "비빔밥"
                "양식" -> "스파게티"
                "일식" -> "돈카츠"
                "중식" -> "짜장면"
                else -> "볶음밥"
            },
            subDish = when (mainCategory) {
                "한식" -> "미역국"
                "양식" -> "시저샐러드"
                "일식" -> "미소시루"
                "중식" -> "단무지"
                else -> "계란스프"
            },
            dessert = when (mainCategory) {
                "한식" -> "누룽지"
                "양식" -> "티라미수"
                "일식" -> "모찌"
                "중식" -> "망고푸딩"
                else -> "아이스크림"
            },
            timeMessage = "당신의 취향을 고려한 특별한 추천입니다! ✨",
            recommendationReason = "${mainCategory} 선호도를 바탕으로 선별한 맞춤 메뉴입니다."
        )
    }

    /**
     * 특정 시간대의 추천 메뉴를 미리 가져옵니다. (캐싱 목적)
     */
    suspend fun preloadRecommendationForHour(targetHour: Int) {
        try {
            Log.d(TAG, "📦 ${targetHour}시 추천 미리 로드")
            
            val userPreferences = behaviorTracker.preferredCategories.first()
            aiService.getTimeBasedRecommendation(targetHour, userPreferences)
            
            Log.d(TAG, "✅ ${targetHour}시 추천 미리 로드 완료")
        } catch (e: Exception) {
            Log.e(TAG, "❌ ${targetHour}시 추천 미리 로드 실패", e)
        }
    }

    /**
     * 사용자의 선호도 점수를 기반으로 추천 품질을 평가합니다.
     * @param categories 평가할 카테고리 리스트
     * @return 추천 적합도 점수 (0-100)
     */
    suspend fun evaluateRecommendationScore(categories: List<String>): Int {
        return behaviorTracker.calculatePreferenceScore(categories)
    }

    /**
     * 현재 시간대에 가장 적합한 카테고리를 추천합니다.
     */
    fun getRecommendedCategoriesForCurrentTime(): List<String> {
        val timePhase = _currentKoreanTime.value.timePhase
        
        return when (timePhase) {
            TimePhase.DAWN, TimePhase.NIGHT -> listOf("간단", "따뜻한", "야식")
            TimePhase.MORNING -> listOf("아침", "간단", "영양", "건강")
            TimePhase.LATE_MORNING -> listOf("간식", "가벼운", "달콤한")
            TimePhase.LUNCH -> listOf("점심", "한식", "든든한", "메인")
            TimePhase.AFTERNOON -> listOf("디저트", "음료", "간식", "달콤한")
            TimePhase.EVENING -> listOf("저녁", "메인", "풍성한", "맛있는")
        }
    }
} 