package com.bcu.foodtable.TTS

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import com.google.firebase.functions.FirebaseFunctionsException
import android.util.Log

data class CoachTurn(val role: String, val text: String) // "user" / "assistant"
private const val TAG = "CoachAPI"

data class FinalScore(
    val score: Int,
    val breakdown: Map<String, Any>,
    val summary: String
)

class CookingCoachViewModel : ViewModel() {
    // UI 상태
    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting

    val transcript = mutableListOf<CoachTurn>()           // 대화 로그
    val slots = linkedMapOf<String, Any>()                // 구조화 입력 누적 (ex: "water_ml" -> 200)
    private val _liveScore = MutableStateFlow(0)
    val liveScore: StateFlow<Int> = _liveScore

    // 현재 단계 인덱스 추적 (선택적)
    private var currentStepIndex: Int = 0

    fun startTest(recipe: RecipeItem, stepIdx: Int) {
        _isTesting.value = true
        transcript.clear()
        slots.clear()
        _liveScore.value = 0
        currentStepIndex = stepIdx
        // 첫 안내(선택)
        transcript += CoachTurn("assistant", "테스트 모드를 시작합니다. 현재 단계에 맞춰 진행해보세요!")
    }

    fun updateCurrentStep(stepIdx: Int) {
        currentStepIndex = stepIdx
    }

    fun onUserUtterance(text: String, recipe: RecipeItem) {
        if (!_isTesting.value) return
        transcript += CoachTurn("user", text)
        viewModelScope.launch {
            // 1) 레시피 컨텍스트(간단 구조화)와 사용자 발화를 CF로 보냄 → 의도/슬롯/짧은 답변 + 추천
            val coachRes = callCoachLLM(text, recipe, currentStepIndex)
            // slots 병합
            coachRes.slots?.forEach { (k, v) -> slots[k] = v }
            // 라이브 규칙 점수 갱신(로컬 간단 채점)
            _liveScore.value = ruleScore(recipe, slots)
            // 어시스턴트 답변
            coachRes.reply?.let { transcript += CoachTurn("assistant", it) }
        }
    }

    fun finishAndScore(recipe: RecipeItem, onResult: (FinalScore) -> Unit) {
        viewModelScope.launch {
            _isTesting.value = false
            val final = callFinalScorer(recipe, slots)
            transcript += CoachTurn("assistant", "최종 점수: ${final.score}/100\n${final.summary}")
            onResult(final)
        }
    }

    // ───────── LLM / CF 호출부 ─────────

    private data class CoachResult(
        val intent: String?,
        val slots: Map<String, Any>?,
        val reply: String?
    )

    private suspend fun callCoachLLM(
        userText: String,
        recipe: RecipeItem,
        stepIdx: Int
    ): CoachResult = try {
        val input = mapOf(
            "userText" to userText,
            "stepIndex" to stepIdx,
            "recipe" to toStructured(recipe)
        )

        val res = withTimeout(12_000) { // 12s 타임아웃
            Firebase.functions("us-central1")
                .getHttpsCallable("coachTurn")
                .call(input)
                .await()
        }

        // 응답이 Map 또는 JSON 문자열로 올 수도 있음
        val raw = res.getData()
        val map: Map<String, Any> = when (raw) {
            is Map<*, *> -> raw as Map<String, Any>
            is String -> runCatching { // JSON string → Map 파싱
                com.google.gson.Gson().fromJson(
                    raw,
                    object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                ) as Map<String, Any>
            }.getOrElse {
                Log.e(TAG, "coachTurn JSON parse failed: ${it.message}")
                emptyMap()
            }
            else -> emptyMap()
        }

        CoachResult(
            intent = map["intent"] as? String,
            slots  = map["slots"] as? Map<String, Any> ?: emptyMap(),
            reply  = map["reply"] as? String
        )
    } catch (e: Exception) {
        when (e) {
            is FirebaseFunctionsException -> Log.e(TAG, "coachTurn FFE ${e.code}: ${e.message}")
            is kotlinx.coroutines.TimeoutCancellationException -> Log.e(TAG, "coachTurn timeout")
            else -> Log.e(TAG, "coachTurn error: ${e.message}", e)
        }
        // 실패 시 안전한 기본값 반환 (UI는 코치 답변으로 오류 메시지 출력 가능)
        CoachResult(
            intent = null,
            slots = emptyMap(),
            reply = "코칭 서버 연결에 문제가 있어요. 잠시 후 다시 시도해주세요."
        )
    }

    private suspend fun callFinalScorer(
        recipe: RecipeItem,
        slots: Map<String, Any>
    ): FinalScore = try {
        val input = mapOf(
            "recipe" to toStructured(recipe),
            "events" to slots
        )

        val res = withTimeout(15_000) { // 15s 타임아웃
            Firebase.functions("us-central1")
                .getHttpsCallable("scoreCookingSession")
                .call(input)
                .await()
        }

        val raw = res.getData()
        val map: Map<String, Any> = when (raw) {
            is Map<*, *> -> raw as Map<String, Any>
            is String -> runCatching {
                com.google.gson.Gson().fromJson(
                    raw,
                    object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                ) as Map<String, Any>
            }.getOrElse {
                Log.e(TAG, "scoreCookingSession JSON parse failed: ${it.message}")
                emptyMap()
            }
            else -> emptyMap()
        }

        FinalScore(
            score     = (map["score"] as? Number)?.toInt() ?: 0,
            breakdown = (map["breakdown"] as? Map<String, Any>) ?: emptyMap(),
            summary   = (map["summary"] as? String).orEmpty()
        )
    } catch (e: Exception) {
        when (e) {
            is FirebaseFunctionsException -> Log.e(TAG, "score FFE ${e.code}: ${e.message}")
            is kotlinx.coroutines.TimeoutCancellationException -> Log.e(TAG, "score timeout")
            else -> Log.e(TAG, "score error: ${e.message}", e)
        }
        FinalScore(
            score = 0,
            breakdown = emptyMap(),
            summary = "채점 서버 연결에 실패했습니다. 네트워크를 확인하고 다시 시도해주세요."
        )
    }

    // 간단 규칙형 점수 (로컬)
    private fun ruleScore(recipe: RecipeItem, slots: Map<String, Any>): Int {
        // 예시: 채점 규칙을 아주 간단히 (실전은 세분화)
        var s = 0
        if (slots.isNotEmpty()) s += 20
        // TODO: 재료별 허용 범위, 단계별 타이밍, 타이머 사용 여부 등 반영
        return s.coerceIn(0, 100)
    }

    // 레시피 구조화(필요한 핵심만)
    private fun toStructured(recipe: RecipeItem): Map<String, Any> {
        val steps = recipe.order.split("○")
            .filter { it.isNotBlank() }
            .map { raw -> mapOf("text" to raw.trim()) }

        return mapOf(
            "id" to recipe.id,
            "name" to recipe.name,
            "ingredients" to recipe.ingredients, // ["물 200ml", ...] 형태면 CF에서 파싱
            "steps" to steps
        )
    }
}