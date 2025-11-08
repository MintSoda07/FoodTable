package com.bcu.foodtable.manager

import android.util.Log
import com.bcu.foodtable.ai.AIRecommendationService
import com.bcu.foodtable.data.UserBehaviorTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.*

class TimeBasedRecommendationManager(
    private val aiService: AIRecommendationService,
    private val behaviorTracker: UserBehaviorTracker
) {
    companion object {
        private const val TAG = "TimeRecommendationMgr"
        private const val KOREA_TIMEZONE = "Asia/Seoul"
    }

    data class KoreanTimeInfo(
        val hour: Int,
        val minute: Int,
        val greeting: String,
        val emoji: String
    )

    private val _currentKoreanTime = MutableStateFlow(getCurrentKoreanTime())
    val currentKoreanTime: StateFlow<KoreanTimeInfo> = _currentKoreanTime.asStateFlow()

    private val _aiRecommendation = MutableStateFlow<AIRecommendationService.TimeBasedRecommendation?>(null)
    val aiRecommendation: StateFlow<AIRecommendationService.TimeBasedRecommendation?> = _aiRecommendation.asStateFlow()

    private val _timeBasedGreeting = MutableStateFlow("")
    val timeBasedGreeting: StateFlow<String> = _timeBasedGreeting.asStateFlow()

    private val _isLoadingRecommendation = MutableStateFlow(false)
    val isLoadingRecommendation: StateFlow<Boolean> = _isLoadingRecommendation.asStateFlow()

    init {
        updateCurrentTime()
        updateTimeBasedGreeting("손님")
    }

    fun updateCurrentTime() {
        val newTimeInfo = getCurrentKoreanTime()
        _currentKoreanTime.value = newTimeInfo
        updateTimeBasedGreeting("손님")
        Log.d(TAG, "⏰ 시간 업데이트: ${newTimeInfo.hour}:${newTimeInfo.minute.toString().padStart(2, '0')}")
    }

    suspend fun updateAIRecommendation(userName: String = "회원") {
        try {
            _isLoadingRecommendation.value = true
            val currentTime = _currentKoreanTime.value
            val userPreferences = behaviorTracker.preferredCategories.first()

            val recommendation = aiService.getTimeBasedRecommendation(
                currentHour = currentTime.hour,
                userPreferences = userPreferences
            )

            _aiRecommendation.value = recommendation
            updateTimeBasedGreeting(userName)
            Log.d(TAG, "✅ AI 추천 업데이트 완료: ${recommendation.mainDish}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ AI 추천 업데이트 실패", e)
            _aiRecommendation.value = createFallbackRecommendation()
        } finally {
            _isLoadingRecommendation.value = false
        }
    }

    suspend fun getPersonalizedRecommendation(): AIRecommendationService.TimeBasedRecommendation {
        return try {
            val userPreferences = behaviorTracker.preferredCategories.first()
            if (userPreferences.isNotEmpty()) {
                aiService.getPersonalizedRecommendation(userPreferences)
            } else {
                val currentTime = _currentKoreanTime.value
                aiService.getTimeBasedRecommendation(currentTime.hour, emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 맞춤 추천 실패", e)
            createPersonalizedFallbackRecommendation()
        }
    }

    private fun getCurrentKoreanTime(): KoreanTimeInfo {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(KOREA_TIMEZONE))
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val (greeting, emoji) = getHourGreetingEmoji(hour)

        return KoreanTimeInfo(
            hour = hour,
            minute = minute,
            greeting = greeting,
            emoji = emoji
        )
    }

    private fun getHourGreetingEmoji(hour: Int): Pair<String, String> {
        return when (hour) {
            in 0..1 -> "깊은 밤입니다. 출출하신가요?" to "🌙"
            in 2..4 -> "조용한 새벽, 요리 아이디어를 떠올려보세요!" to "🌌"
            5 -> "이른 아침, 하루를 요리로 시작해봐요!" to "🌄"
            in 6..8 -> "좋은 아침입니다! 아침 메뉴를 골라보세요." to "☀️"
            9 -> "느긋한 아침, 간단한 요리는 어때요?" to "🌤️"
            in 10..11 -> "점심 준비 전 레시피 탐색 시간!" to "🌞"
            in 12..13 -> "점심시간이에요! 든든하게 드세요." to "🍽️"
            14 -> "가벼운 디저트로 오후를 준비해요!" to "🍩"
            in 15..16 -> "오후 간식, 레시피로 기운을 내보세요!" to "☕"
            17 -> "저녁 준비 시간입니다. 어떤 요리를 해볼까요?" to "🍳"
            in 18..19 -> "저녁시간입니다. 정성 가득 요리를 해봐요!" to "🌇"
            20 -> "하루 마무리엔 따뜻한 한 끼!" to "🍛"
            21 -> "늦은 밤, 요리 일기 남겨보세요." to "📝"
            in 22..23 -> "오늘의 레시피를 정리해봐요." to "🌃"
            else -> "안녕하세요!" to "👋"
        }
    }

    fun updateTimeBasedGreeting(userName: String? = null) {
        _timeBasedGreeting.value = getGreetingMessage(userName)
    }

    private fun createFallbackRecommendation(): AIRecommendationService.TimeBasedRecommendation {
        val currentTime = _currentKoreanTime.value
        return AIRecommendationService.TimeBasedRecommendation(
            mainDish = "비빔밥",
            subDish = "계란국",
            dessert = "수정과",
            timeMessage = "${currentTime.greeting} ${currentTime.emoji}",
            recommendationReason = "시간대에 맞는 기본 한식 추천입니다."
        )
    }

    private suspend fun createPersonalizedFallbackRecommendation(): AIRecommendationService.TimeBasedRecommendation {
        val preferences = behaviorTracker.preferredCategories.first()
        val mainCategory = preferences.firstOrNull() ?: "한식"

        return AIRecommendationService.TimeBasedRecommendation(
            mainDish = when (mainCategory) {
                "한식" -> "된장찌개"
                "양식" -> "까르보나라"
                "일식" -> "돈부리"
                "중식" -> "짬뽕"
                else -> "볶음밥"
            },
            subDish = "김치",
            dessert = "아이스크림",
            timeMessage = "개인 취향 기반 추천입니다. 🍽️",
            recommendationReason = "$mainCategory 선호도를 반영한 맞춤 메뉴입니다."
        )
    }

    suspend fun preloadRecommendationForHour(targetHour: Int) {
        try {
            val userPreferences = behaviorTracker.preferredCategories.first()
            aiService.getTimeBasedRecommendation(targetHour, userPreferences)
        } catch (e: Exception) {
            Log.e(TAG, "❌ ${targetHour}시 추천 미리 로드 실패", e)
        }
    }

    suspend fun evaluateRecommendationScore(categories: List<String>): Int {
        return behaviorTracker.calculatePreferenceScore(categories)
    }

    fun getRecommendedCategoriesForHour(hour: Int): List<String> {
        return when (hour) {
            in 0..5, in 22..23 -> listOf("야식", "간단", "따뜻한")
            in 6..9 -> listOf("아침", "간단", "건강")
            in 10..11 -> listOf("간식", "가벼운", "브런치")
            in 12..13 -> listOf("점심", "든든한", "한식")
            in 14..16 -> listOf("디저트", "간식", "달콤한")
            in 17..19 -> listOf("저녁", "풍성한", "가정식")
            else -> listOf("요리기록", "추천", "개인레시피")
        }
    }

/** 계절 + 시간대 인사말을 합친 최종 메시지 */
    private fun getGreetingMessage(userName: String?): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(KOREA_TIMEZONE))
        val month = calendar.get(Calendar.MONTH) + 1     // 1~12
        val nameToShow = userName ?: "사용자"

        // ① 계절 문구
        val seasonMessage = when (month) {
            in 3..5   -> listOf(
                "따스한 봄바람이 부는 하루예요.",
                "꽃향기 가득한 봄입니다, $nameToShow!",
                "봄날엔 새로운 레시피를 시도해보세요.",
                "싱그러운 봄처럼 상큼한 요리를 준비해볼까요?",
                "포근한 봄날엔 따뜻한 레시피가 어울려요."
            )
            in 6..8   -> listOf(
                "무더운 여름, 시원한 레시피를 추천드려요!",
                "여름엔 가벼운 한끼가 좋아요, $nameToShow.",
                "햇살 가득한 여름날엔 간단한 요리가 최고죠!",
                "$nameToShow 님, 여름을 담은 레시피를 기록해보세요.",
                "청량한 여름, 요리로 기운을 내보세요!"
            )
            in 9..11  -> listOf(
                "가을입니다. 풍성한 식탁이 기다리고 있어요.",
                "가을 바람처럼 깊은 맛을 담은 요리를 해볼까요?",
                "$nameToShow 님, 따뜻한 요리가 생각나는 계절이에요.",
                "알록달록 가을처럼 다채로운 레시피를 만나보세요.",
                "가을은 요리하기 좋은 계절이에요!"
            )
            else      -> listOf(
                "겨울입니다. 따뜻한 레시피가 어울리는 계절이에요.",
                "$nameToShow 님, 오늘은 어떤 따뜻한 요리를 하실 건가요?",
                "포근한 요리로 추운 날씨를 녹여보세요.",
                "겨울엔 뜨끈한 요리 한 그릇이 딱이죠!",
                "따뜻한 레시피로 마음을 녹여보세요, $nameToShow."
            )
        }.random()

        // ② 시간대 문구
        val timeMessage = getHourGreetingMessage(nameToShow)

        return "$seasonMessage\n$timeMessage"
    }
    fun getHourGreetingMessage(userName: String?): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val nameToShow = userName ?: "사용자"


        val messagesByHour = mapOf(
            0 to listOf(
                "0시입니다, $nameToShow! 야식 생각나지 않으세요?",
                "$nameToShow 님, 밤도 깊었어요. 간단한 레시피를 떠올려 보세요.",
                "이 시간엔 간단한 요리 영상이 땡기죠, $nameToShow?",
                "0시, $nameToShow 님만의 비밀 야식 시간이에요!",
                "하루 마무리는 맛있는 생각으로, $nameToShow!"
            ),
            1 to listOf(
                "1시네요, $nameToShow. 출출하신가요?",
                "깊은 밤, 나만의 레시피 정리 시간이에요, $nameToShow!",
                "$nameToShow 님, 간단한 요리로 하루를 정리해보세요.",
                "새벽 감성엔 요리 아이디어가 넘치죠, $nameToShow!",
                "이 시간, 레시피 저장하기 딱 좋습니다, $nameToShow."
            ),
            2 to listOf(
                "$nameToShow 님, 조용한 새벽엔 레시피 탐색 어떠세요?",
                "2시입니다. 나만의 요리를 기록해봐요, $nameToShow!",
                "이 시간에 떠오르는 요리 하나쯤 있지 않나요, $nameToShow?",
                "새벽 2시엔 감성 가득한 레시피 구경이 좋아요.",
                "혼자만의 조용한 레시피 시간입니다, $nameToShow!"
            ),
            3 to listOf(
                "$nameToShow, 아직 안 주무셨다면 따뜻한 요리를 상상해보세요.",
                "3시, 이 시간에도 레시피가 올라오고 있어요!",
                "당신만의 새벽 간식은 무엇인가요, $nameToShow?",
                "지금 이 시간엔 감성 요리가 어울려요, $nameToShow.",
                "새벽이지만, 요리 영감은 언제든 찾아옵니다!"
            ),
            4 to listOf(
                "4시입니다, $nameToShow. 하루를 준비할 시간이에요.",
                "이른 새벽, 가볍게 내일 요리를 계획해보는 건 어때요?",
                "좋은 하루의 시작은 맛있는 생각에서 옵니다, $nameToShow.",
                "레시피 한 줄로 하루가 달라질 수도 있어요, $nameToShow.",
                "고요한 이 시간, 레시피로 하루를 열어보세요."
            ),
            5 to listOf(
                "5시입니다, $nameToShow! 상쾌한 하루를 위한 요리 준비 시간이에요.",
                "기분 좋은 하루, 좋은 레시피로 시작해보세요.",
                "$nameToShow 님, 오늘 첫 요리는 무엇으로 시작할까요?",
                "요리하는 아침은 더욱 특별해요, $nameToShow!",
                "이른 아침, 레시피 탐색으로 에너지 충전!"
            ),
            6 to listOf(
                "좋은 아침입니다, $nameToShow! 간단한 아침 메뉴는 어떠세요?",
                "6시예요, $nameToShow. 든든한 아침 요리로 시작해요!",
                "하루를 시작하는 최고의 방법, 아침 레시피 탐색!",
                "따뜻한 아침을 위한 요리, 찾아볼까요 $nameToShow?",
                "$nameToShow 님, 아침엔 간단하고 건강한 메뉴가 좋아요."
            ),
            7 to listOf(
                "$nameToShow 님의 아침 루틴에 어울릴 레시피는?",
                "든든한 하루를 위한 7시 레시피 탐색 시간!",
                "맛있는 아침으로 활기찬 하루를 시작해봐요.",
                "이른 시간, 영양 가득 레시피 어때요, $nameToShow?",
                "레시피 앱과 함께하는 맛있는 아침!"
            ),
            8 to listOf(
                "8시입니다. 출근 전, 간단한 요리를 만들어보세요!",
                "바쁜 아침, 빠르고 맛있는 요리가 필요하죠.",
                "$nameToShow 님, 오늘 아침 레시피는 어떤 걸로 해볼까요?",
                "시간 절약 아침 레시피, 지금 확인해보세요.",
                "따뜻한 식사가 하루를 바꿉니다, $nameToShow!"
            ),
            9 to listOf(
                "아침 마무리 시간이에요, $nameToShow. 간단한 스낵은 어때요?",
                "출근길 레시피 체크, 잊지 마세요!",
                "$nameToShow 님, 아침에 저장한 레시피 보셨나요?",
                "가볍게 챙기는 아침 메뉴로 시작해요!",
                "오늘 하루도 맛있게 시작해요, $nameToShow!"
            ),
            10 to listOf(
                "10시입니다. 점심 전 요리 아이디어 탐색 시간이에요!",
                "$nameToShow 님, 오늘 점심은 직접 만들어보는 건 어때요?",
                "이 시간엔 인기 레시피를 확인해보세요!",
                "간단한 재료로 빠른 요리를 해보세요, $nameToShow.",
                "레시피로 미리 계획하는 점심 시간!"
            ),
            11 to listOf(
                "점심시간이 가까워졌어요, $nameToShow!",
                "오늘의 점심 메뉴는 정하셨나요?",
                "요즘 인기 있는 점심 요리를 확인해보세요!",
                "레시피 앱에서 점심 메뉴를 골라보세요.",
                "점심시간엔 든든한 한 끼가 필요해요, $nameToShow!"
            ),
            12 to listOf(
                "12시네요, $nameToShow! 맛있는 요리로 기운을 내보세요.",
                "$nameToShow 님, 점심엔 어떤 요리를 드시고 싶으신가요?",
                "좋은 점심 되세요, $nameToShow! 레시피 공유도 잊지 마세요.",
                "레시피 아이디어가 샘솟는 점심시간이에요, $nameToShow!",
                "점심시간입니다, $nameToShow! 간단한 메뉴 하나 골라볼까요?"
            ),
            13 to listOf(
                "맛있게 식사하셨나요, $nameToShow? 이제 레시피를 정리해볼까요?",
                "점심의 여운을 담아 오늘의 요리를 기록해보세요.",
                "요리의 감동은 공유로 완성돼요, $nameToShow.",
                "레시피를 남기면 다음 식사도 더 쉬워집니다!",
                "좋은 요리는 기록할 만한 가치가 있답니다."
            ),
            14 to listOf(
                "오후입니다, $nameToShow. 다음 요리를 구상해볼까요?",
                "한가로운 오후, 레시피를 다듬어보는 건 어떠세요?",
                "이 시간은 요리 아이디어를 정리하기에 좋아요.",
                "$nameToShow 님의 요리 기록을 기다리고 있어요!",
                "간단한 간식도 멋진 레시피가 될 수 있어요."
            ),
            15 to listOf(
                "오후 3시입니다, $nameToShow. 티타임 레시피 생각나시나요?",
                "달콤한 간식, 그리고 레시피의 시간입니다.",
                "지금 떠오른 레시피, 적어두지 않으면 잊어버릴 수 있어요!",
                "$nameToShow 님의 창의력이 반짝일 시간이에요.",
                "좋은 레시피는 나눌수록 빛납니다."
            ),
            16 to listOf(
                "저녁 준비 시간입니다, $nameToShow. 무엇을 해드릴까요?",
                "레시피 아이디어 정리해두셨나요?",
                "지금은 요리 재료를 정리하기 좋은 시간이죠!",
                "$nameToShow 님, 특별한 저녁 메뉴를 구상해보세요.",
                "오늘의 마무리를 준비할 시간이에요!"
            ),
            17 to listOf(
                "저녁은 $nameToShow 님과 함께! 특별한 요리를 준비해 보세요.",
                "하루 중 가장 풍성한 식사, 어떤 메뉴로 채우실 건가요?",
                "$nameToShow 님, 레시피로 가족과의 시간을 풍성하게!",
                "따뜻한 저녁 레시피로 하루를 마무리하세요.",
                "저녁 시간, 정성 가득한 요리를 준비해 보세요."
            ),
            18 to listOf(
                "맛있는 저녁을 위한 레시피를 확인해보세요, $nameToShow!",
                "레시피는 사랑의 또 다른 이름이죠.",
                "$nameToShow 님의 요리가 오늘 하루를 완성시켜줄 거예요!",
                "정성스러운 저녁, 기록해 두셨나요?",
                "요리는 마음을 나누는 최고의 방법이에요."
            ),
            19 to listOf(
                "저녁시간입니다! 오늘의 요리를 공유해보세요, $nameToShow.",
                "레시피를 나누면 기쁨도 두 배랍니다.",
                "따뜻한 저녁, 정성 가득한 요리를 추천드려요.",
                "$nameToShow 님, 오늘은 어떤 요리를 해보셨나요?",
                "레시피는 하루의 감성을 담는 그릇입니다."
            ),
            20 to listOf(
                "20시입니다. 하루의 요리를 정리하기 좋은 시간이죠, $nameToShow!",
                "저장하지 않으면 잊히는 레시피, 기록해 두세요.",
                "오늘 만든 요리 중 베스트를 정리해볼까요?",
                "지금 이 순간, 당신의 레시피가 누군가에게 도움이 될 수 있어요.",
                "요리는 추억을 만드는 과정이에요, $nameToShow."
            ),
            21 to listOf(
                "밤입니다. 오늘의 요리를 되새겨보세요, $nameToShow.",
                "레시피는 오늘의 감정을 담는 일기예요.",
                "$nameToShow 님, 내일의 요리를 위해 오늘을 기록하세요.",
                "조용한 밤, 나만의 요리를 되새겨보는 시간이에요.",
                "오늘의 레시피가 내일의 누군가에게 영감이 될 수 있어요!"
            ),
            22 to listOf(
                "22시입니다. 하루의 레시피를 정리할 시간이에요, $nameToShow.",
                "늦은 밤, 나만의 요리를 되돌아보는 건 어떠세요?",
                "지금 저장한 레시피가 내일의 인기 요리가 될 수도 있어요.",
                "$nameToShow 님, 오늘의 요리를 나눠보세요.",
                "레시피는 마음의 기록입니다."
            ),
            23 to listOf(
                "23시입니다, $nameToShow. 오늘 하루의 레시피를 정리해 보세요.",
                "하루의 마무리엔 따뜻한 한마디, $nameToShow. 수고하셨어요!",
                "늦은 밤, 나만의 레시피로 마음을 달래보세요, $nameToShow!",
                "$nameToShow 님, 오늘 저장한 레시피가 있나요?",
                "레시피는 기억보다 기록입니다, $nameToShow!"
            )
        )

        val messageList = messagesByHour[hour] ?: listOf("안녕하세요, $nameToShow! 맛있는 하루 보내세요.")
        return messageList.random()
    }

} 