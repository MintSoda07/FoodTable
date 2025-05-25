package com.bcu.foodtable.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI 기반 레시피 추천 서비스
 * GPT API를 활용하여 시간대별로 메인, 서브, 디저트 메뉴를 추천합니다.
 */
class AIRecommendationService {
    
    companion object {
        private const val TAG = "AIRecommendationService"
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        
        // TODO: 실제 운영 시에는 BuildConfig나 안전한 저장소에서 API 키를 가져와야 합니다
        private const val OPENAI_API_KEY = "YOUR_OPENAI_API_KEY_HERE" // 실제 API 키로 교체 필요
    }

    /**
     * 시간대별 추천 레시피 데이터 클래스
     */
    data class TimeBasedRecommendation(
        val mainDish: String,           // 메인 메뉴
        val subDish: String,            // 서브 메뉴  
        val dessert: String,            // 디저트
        val timeMessage: String,        // 시간대별 맞춤 메시지
        val recommendationReason: String // 추천 이유
    )

    /**
     * 현재 시간대에 맞는 AI 추천을 받습니다.
     * @param currentHour 현재 시간 (0-23)
     * @param userPreferences 사용자 선호 카테고리 (옵션)
     * @return 시간대별 추천 메뉴 정보
     */
    suspend fun getTimeBasedRecommendation(
        currentHour: Int,
        userPreferences: List<String> = emptyList()
    ): TimeBasedRecommendation {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🤖 AI 추천 요청 시작 - 시간: ${currentHour}시, 선호도: $userPreferences")
                
                // 시간대별 맞춤 프롬프트 생성
                val prompt = createTimeBasedPrompt(currentHour, userPreferences)
                
                // GPT API 호출
                val gptResponse = callGPTAPI(prompt)
                
                // 응답 파싱
                val recommendation = parseGPTResponse(gptResponse, currentHour)
                
                Log.d(TAG, "✅ AI 추천 완료: ${recommendation.mainDish}, ${recommendation.subDish}, ${recommendation.dessert}")
                recommendation
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ AI 추천 실패, 기본 추천 반환", e)
                // API 실패 시 기본 추천 반환
                getDefaultRecommendation(currentHour)
            }
        }
    }

    /**
     * 시간대별 맞춤 프롬프트를 생성합니다.
     */
    private fun createTimeBasedPrompt(currentHour: Int, userPreferences: List<String>): String {
        val timeContext = when (currentHour) {
            in 6..9 -> "아침 시간대 (6-9시)"
            in 10..11 -> "오전 간식 시간대 (10-11시)"
            in 12..14 -> "점심 시간대 (12-14시)"
            in 15..17 -> "오후 간식 시간대 (15-17시)"
            in 18..21 -> "저녁 시간대 (18-21시)"
            in 22..23, in 0..5 -> "야식/늦은 시간대 (22시-새벽5시)"
            else -> "일반 시간대"
        }
        
        val preferenceText = if (userPreferences.isNotEmpty()) {
            "사용자 선호 음식: ${userPreferences.joinToString(", ")}"
        } else {
            "사용자 선호도 정보 없음"
        }

        return """
            현재 한국 시간 ${currentHour}시 ($timeContext)에 맞는 한국인 취향의 메뉴를 추천해주세요.
            $preferenceText
            
            다음 형식으로 정확히 응답해주세요:
            {
                "mainDish": "메인 요리명",
                "subDish": "서브 요리명 또는 반찬",
                "dessert": "디저트명",
                "timeMessage": "시간대에 맞는 한국어 인사말 (예: 좋은 아침입니다! 든든한 아침식사로 하루를 시작하세요)",
                "recommendationReason": "이 시간대에 이 메뉴들을 추천하는 이유"
            }
            
            조건:
            1. 한국인이 해당 시간대에 실제로 먹는 음식 위주로 추천
            2. 메인, 서브, 디저트는 서로 조화를 이루어야 함
            3. 시간대 특성을 고려 (아침: 간단하고 영양가, 저녁: 든든하고 맛있는 등)
            4. 사용자 선호도가 있다면 반영
            5. 모든 응답은 한국어로
            6. JSON 형식을 정확히 지켜주세요
        """.trimIndent()
    }

    /**
     * GPT API를 호출합니다.
     */
    private suspend fun callGPTAPI(prompt: String): String {
        // ====== ✅ 실제 GPT API 호출 코드 (나중에 사용하려면 주석 해제하세요) ======
        /*
        return withContext(Dispatchers.IO) {
            val url = URL(OPENAI_API_URL)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $OPENAI_API_KEY")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val requestBody = JSONObject().apply {
                    put("model", "gpt-3.5-turbo")
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("max_tokens", 500)
                    put("temperature", 0.7)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                        reader.readText()
                    }
                    Log.e(TAG, "GPT API 오류 (코드: $responseCode): $errorResponse")
                    throw Exception("GPT API 호출 실패: $responseCode")
                }

            } finally {
                connection.disconnect()
            }
        }
        */

        // ====== ✅ 현재는 테스트용 더미 응답 사용 중 ======
        Log.w(TAG, "⚠️ GPT API 우회 모드: 더미 응답 반환 중")

        return """
    {
        "choices": [{
            "message": {
                "content": "{ 
                    \"mainDish\": \"라면\", 
                    \"subDish\": \"김치\", 
                    \"dessert\": \"아이스크림\", 
                    \"timeMessage\": \"간단한 야식 추천입니다 🍜\", 
                    \"recommendationReason\": \"가볍게 먹기 좋고 간편해서 추천합니다.\" 
                }"
            }
        }]
    }
    """.trimIndent()
    }

    /**
     * GPT API 응답을 파싱합니다.
     */
    private fun parseGPTResponse(response: String, currentHour: Int): TimeBasedRecommendation {
        return try {
            Log.d(TAG, "🔍 GPT 응답 파싱 중: $response")
            
            val jsonResponse = JSONObject(response)
            val choices = jsonResponse.getJSONArray("choices")
            val message = choices.getJSONObject(0).getJSONObject("message")
            val content = message.getString("content")
            
            // GPT 응답에서 JSON 부분 추출
            val cleanedContent = content.trim()
            val recommendationJson = JSONObject(cleanedContent)
            
            TimeBasedRecommendation(
                mainDish = recommendationJson.getString("mainDish"),
                subDish = recommendationJson.getString("subDish"),
                dessert = recommendationJson.getString("dessert"),
                timeMessage = recommendationJson.getString("timeMessage"),
                recommendationReason = recommendationJson.getString("recommendationReason")
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "GPT 응답 파싱 실패", e)
            // 파싱 실패 시 기본 추천 반환
            getDefaultRecommendation(currentHour)
        }
    }

    /**
     * API 실패 시 사용할 기본 추천을 생성합니다.
     */
    private fun getDefaultRecommendation(currentHour: Int): TimeBasedRecommendation {
        return when (currentHour) {
            in 6..9 -> TimeBasedRecommendation(
                mainDish = "계란후라이덮밥",
                subDish = "김치",
                dessert = "요구르트",
                timeMessage = "좋은 아침입니다! 든든한 아침식사로 하루를 시작하세요 ☀️",
                recommendationReason = "아침에는 간단하면서도 영양가 있는 메뉴가 좋습니다."
            )
            in 12..14 -> TimeBasedRecommendation(
                mainDish = "김치찌개",
                subDish = "계란말이",
                dessert = "식혜",
                timeMessage = "점심시간이네요! 맛있는 한끼로 에너지를 충전하세요 🍽️",
                recommendationReason = "점심에는 든든하고 맛있는 한식이 제격입니다."
            )
            in 18..21 -> TimeBasedRecommendation(
                mainDish = "불고기",
                subDish = "나물 3종",
                dessert = "수정과",
                timeMessage = "저녁 시간입니다! 하루의 피로를 맛있는 저녁으로 달래보세요 🌙",
                recommendationReason = "저녁에는 풍성하고 영양 균형이 잡힌 메뉴가 좋습니다."
            )
            else -> TimeBasedRecommendation(
                mainDish = "라면",
                subDish = "김치",
                dessert = "아이스크림",
                timeMessage = "안녕하세요! 간단하고 맛있는 메뉴는 어떠세요? 😊",
                recommendationReason = "간단하면서도 만족도 높은 메뉴입니다."
            )
        }
    }

    /**
     * 사용자 맞춤 AI 추천을 받습니다.
     * @param userPreferences 사용자의 선호 카테고리
     * @param avoidCategories 피하고 싶은 카테고리
     * @return 맞춤 추천 메뉴
     */
    suspend fun getPersonalizedRecommendation(
        userPreferences: List<String>,
        avoidCategories: List<String> = emptyList()
    ): TimeBasedRecommendation {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "👤 맞춤 추천 요청: 선호-$userPreferences, 회피-$avoidCategories")
                
                val prompt = """
                    사용자의 취향을 분석해서 맞춤 메뉴를 추천해주세요.
                    
                    선호하는 음식: ${userPreferences.joinToString(", ")}
                    피하고 싶은 음식: ${if (avoidCategories.isNotEmpty()) avoidCategories.joinToString(", ") else "없음"}
                    
                    다음 형식으로 정확히 응답해주세요:
                    {
                        "mainDish": "메인 요리명",
                        "subDish": "서브 요리명",
                        "dessert": "디저트명",
                        "timeMessage": "사용자님만을 위한 특별한 추천입니다! ✨",
                        "recommendationReason": "사용자 취향 분석 결과와 추천 이유"
                    }
                    
                    조건:
                    1. 사용자 선호 카테고리를 최대한 반영
                    2. 피하고 싶은 카테고리는 절대 포함하지 않기
                    3. 메뉴들이 서로 잘 어울리도록
                    4. 한국어로 응답
                    5. JSON 형식 정확히 지키기
                """.trimIndent()
                
                val gptResponse = callGPTAPI(prompt)
                parseGPTResponse(gptResponse, 12) // 기본 시간으로 12시 사용
                
            } catch (e: Exception) {
                Log.e(TAG, "맞춤 추천 실패", e)
                // 선호도 기반 기본 추천
                TimeBasedRecommendation(
                    mainDish = if (userPreferences.contains("한식")) "된장찌개" else "파스타",
                    subDish = if (userPreferences.contains("한식")) "무생채" else "샐러드",
                    dessert = if (userPreferences.contains("전통")) "식혜" else "케이크",
                    timeMessage = "${userPreferences.firstOrNull() ?: "맛있는"} 요리로 특별한 식사 시간 되세요! ✨",
                    recommendationReason = "사용자님의 선호도를 바탕으로 선별한 맞춤 메뉴입니다."
                )
            }
        }
    }
} 