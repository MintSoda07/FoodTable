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
import com.bcu.foodtable.ai.OpenAIClient
import com.bcu.foodtable.useful.ApiKey

data class CoachTurn(val role: String, val text: String) // "user" / "assistant"
private const val TAG = "CoachAPI"
private const val MEM_TAG = "CoachMemory"

data class FinalScore(
    val score: Int,
    val breakdown: Map<String, Any>,
    val summary: String
)

// 🔸 메모리 큐에 담길 한 턴
data class MemoryTurn(
    val user: String,
    val assistant: String,
    val ts: Long = System.currentTimeMillis()
)

class CookingCoachViewModel : ViewModel() {
    // UI 상태
    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting

    val transcript = mutableListOf<CoachTurn>()           // 대화 로그(전체 표시용)
    val slots = linkedMapOf<String, Any>()                // 구조화 입력 누적 (ex: "water_ml" -> 200)

    private val _liveScore = MutableStateFlow(0)
    val liveScore: StateFlow<Int> = _liveScore

    // 현재 단계 인덱스 추적
    private var currentStepIndex: Int = 0

    // ✅ 단계 완수 기반 점수 계산용
    private var totalSteps: Int = 0
    private var completedSteps: Int = 0

    // ✅ 롤링 메모리(최근 Q/A만 유지)
    private val memoryQueue: ArrayDeque<MemoryTurn> = ArrayDeque()
    private val MEMORY_MAX_ITEMS = 15
    private val MEMORY_MAX_CHARS = 4000

    /** 현재 메모리 통계 문자열 (로그용) */
    private fun memoryStatsString(prefix: String = ""): String {
        val totalChars = memoryTotalChars()
        val head = memoryQueue.firstOrNull()
        val tail = memoryQueue.lastOrNull()
        val headPreview = head?.let { it.user.take(15) + " / " + it.assistant.take(15) } ?: "-"
        val tailPreview = tail?.let { it.user.take(15) + " / " + it.assistant.take(15) } ?: "-"
        return buildString {
            if (prefix.isNotBlank()) append("$prefix | ")
            append("turns=${memoryQueue.size}, chars=$totalChars, ")
            append("head=[$headPreview], tail=[$tailPreview]")
        }
    }

    private fun memoryTotalChars(): Int =
        memoryQueue.sumOf { it.user.length + it.assistant.length }

    private fun rememberUser(user: String) {
        Log.d(MEM_TAG, "rememberUser() BEFORE  -> ${memoryStatsString()}")
        memoryQueue.addLast(MemoryTurn(user = user, assistant = ""))
        trimMemory()
        Log.d(MEM_TAG, "rememberUser() AFTER   -> ${memoryStatsString()}")
    }

    private fun fillLastAssistant(assistant: String) {
        if (memoryQueue.isEmpty()) {
            Log.w(MEM_TAG, "fillLastAssistant() but memory was empty, creating new turn")
            rememberTurn(user = "", assistant = assistant)
            return
        }
        Log.d(MEM_TAG, "fillLastAssistant() BEFORE -> ${memoryStatsString()}")
        val last = memoryQueue.removeLast()
        val updated = last.copy(assistant = assistant)
        memoryQueue.addLast(updated)
        trimMemory()
        Log.d(MEM_TAG, "fillLastAssistant() AFTER  -> ${memoryStatsString()}")
    }

    private fun rememberTurn(user: String, assistant: String) {
        Log.d(MEM_TAG, "rememberTurn() BEFORE  -> ${memoryStatsString()}")
        memoryQueue.addLast(MemoryTurn(user, assistant))
        trimMemory()
        Log.d(MEM_TAG, "rememberTurn() AFTER   -> ${memoryStatsString()}")
    }

    private fun trimMemory() {
        var trimmedByCount = 0
        var trimmedByChars = 0

        while (memoryQueue.size > MEMORY_MAX_ITEMS) {
            memoryQueue.removeFirst(); trimmedByCount++
        }
        if (trimmedByCount > 0) {
            Log.d(MEM_TAG, "trimMemory() by COUNT -> removed=$trimmedByCount, ${memoryStatsString()}")
        }

        while (memoryQueue.isNotEmpty() && memoryTotalChars() > MEMORY_MAX_CHARS) {
            memoryQueue.removeFirst(); trimmedByChars++
        }
        if (trimmedByChars > 0) {
            Log.d(MEM_TAG, "trimMemory() by CHARS -> removed=$trimmedByChars, ${memoryStatsString()}")
        }
    }

    private fun clearMemory() {
        val before = memoryStatsString("clearMemory() BEFORE")
        memoryQueue.clear()
        Log.d(MEM_TAG, "$before | AFTER -> turns=0, chars=0")
    }

    private fun memoryPayload(): List<Map<String, String>> =
        memoryQueue.map { mapOf("user" to it.user, "assistant" to it.assistant) }

    fun startTest(recipe: RecipeItem, stepIdx: Int) {
        _isTesting.value = true
        transcript.clear()
        slots.clear()
        _liveScore.value = 0
        currentStepIndex = stepIdx

        // ✅ 단계 점수 초기화
        totalSteps = recipe.order.split("○").count { it.isNotBlank() }
        completedSteps = 0

        clearMemory() // ✅ 세션 시작 시 메모리 초기화
        transcript += CoachTurn("assistant", "레시피 AI 도우미를 시작합니다. 현재 단계에 맞춰 진행해보세요!")
        Log.d(MEM_TAG, "startTest() -> ${memoryStatsString()}")
    }

    fun updateCurrentStep(stepIdx: Int) {
        currentStepIndex = stepIdx
    }

    // ✅ NEXT 시 호출되어 완료 카운트/가점 반영
    fun onStepCompleted(stepIdx: Int, recipe: RecipeItem) {
        // 슬롯에 완료 이벤트 누적 (예: step_1_done = true)
        slots["step_${stepIdx + 1}_done"] = true
        // 즉시 점수 갱신
        _liveScore.value = ruleScore(recipe, slots)
    }

    /** 타이머 사용을 기록 (시작/일시정지/재시작 등 한 번이라도 쓰면 true) */
    fun markTimerUsed(recipe: RecipeItem) {
        slots["used_timer"] = true
        _liveScore.value = ruleScore(recipe, slots)
    }

    fun onUserUtterance(text: String, recipe: RecipeItem) {
        if (!_isTesting.value) return
        transcript += CoachTurn("user", text)
        rememberUser(text)

        if (isQuestion(text)) {
            viewModelScope.launch {
                answerQuestion(
                    userText = text,
                    recipe = recipe,
                    currentStepIndex = currentStepIndex,
                    onAnswer = { reply ->
                        transcript += CoachTurn("assistant", reply)
                        fillLastAssistant(reply)
                    },
                    onError = { msg ->
                        val m = msg ?: "지금은 잘 모르겠어요. 잠시 후 다시 시도해 주세요."
                        transcript += CoachTurn("assistant", m)
                        fillLastAssistant(m)
                    }
                )
            }
        } else {
            viewModelScope.launch {
                val coachRes = callCoachLLM(text, recipe, currentStepIndex)
                coachRes.slots?.forEach { (k, v) -> slots[k] = v }
                _liveScore.value = ruleScore(recipe, slots).coerceAtMost(100)
                coachRes.reply?.let {
                    transcript += CoachTurn("assistant", it)
                    fillLastAssistant(it)
                }
            }
        }
    }

    fun finishAndScore(recipe: RecipeItem, onResult: (FinalScore) -> Unit) {
        viewModelScope.launch {
            _isTesting.value = false

            // 로컬 가중치 예시: 완료율 70 + 실시간가점 30
            val completionRatio = if (totalSteps > 0) completedSteps.toFloat() / totalSteps else 0f
            val stepScore = (completionRatio * 70).toInt()
            val live = _liveScore.value.coerceAtMost(30)
            val localFinal = (stepScore + live).coerceIn(0, 100)

            // 서버 점수(있으면) 시도 → 실패 시 로컬 점수 사용
            val serverFinal = runCatching { callFinalScorer(recipe, slots) }.getOrNull()

            val final = serverFinal ?: FinalScore(
                score = localFinal,
                breakdown = mapOf(
                    "completedSteps" to completedSteps,
                    "totalSteps" to totalSteps,
                    "completionScore(70%)" to stepScore,
                    "liveBonus(≤30)" to live
                ),
                summary = if (serverFinal == null)
                    "서버 점수 산출에 실패하여 로컬 규칙 기반으로 산출했습니다."
                else
                    "서버 점수 산출 결과입니다."
            )

            transcript += CoachTurn("assistant", "최종 점수: ${final.score}/100\n${final.summary}")
            onResult(final)

            Log.d(MEM_TAG, "finishAndScore() -> ${memoryStatsString("BEFORE CLEAR")}")
            clearMemory()
        }
    }

    // ───────── 질문 판별 & Q&A 본체 ─────────

    fun isQuestion(text: String): Boolean {
        val t = text.trim()
        if (t.endsWith("?")) return true
        val kw = listOf("맞나요", "괜찮을까요", "어때요", "얼마", "몇", "대체", "바꿔", "대신", "가능", "넣어도", "말고")
        return kw.any { t.contains(it) }
    }

    fun answerQuestion(
        userText: String,
        recipe: RecipeItem,
        currentStepIndex: Int,
        onAnswer: (String) -> Unit,
        onError: (String?) -> Unit
    ) {
        quickRuleAnswer(userText, recipe, currentStepIndex)?.let { a ->
            onAnswer(a); return
        }
        askAIWithContext(
            userText = userText,
            recipe = recipe,
            currentStepIndex = currentStepIndex,
            onAnswer = onAnswer,
            onError = onError
        )
    }


    private fun quickRuleAnswer(
        q: String,
        recipe: RecipeItem,
        stepIdx: Int
    ): String? {
        val lower = q.lowercase()
        if ((listOf("물", "워터", "water").any { lower.contains(it) })
            && (lower.contains("ml") || lower.contains("몇") || lower.contains("얼마"))
        ) {
            recipe.ingredients.firstOrNull { it.contains("물") || it.contains("water", ignoreCase = true) }?.let { row ->
                val m = Regex("""(\d+)\s*ml""", RegexOption.IGNORE_CASE).find(row)
                if (m != null) return "레시피 기준 권장량은 ${m.groupValues[1]}ml 입니다."
            }
            val around = recipe.order.split("○").filter { it.isNotBlank() }
            listOf(stepIdx - 1, stepIdx, stepIdx + 1).forEach { i ->
                if (i in around.indices) {
                    val t = around[i]
                    val m = Regex("""(\d+)\s*ml""", RegexOption.IGNORE_CASE).find(t)
                    if (m != null) return "현재 단계 기준 권장량은 ${m.groupValues[1]}ml 정도예요."
                }
            }
        }
        return null
    }

    private fun askAIWithContext(
        userText: String,
        recipe: RecipeItem,
        currentStepIndex: Int,
        onAnswer: (String) -> Unit,
        onError: (String?) -> Unit
    ) {
        val client = OpenAIClient()
        client.setAIWithAPI(
            onSuccess = { apiKey: ApiKey ->
                client.apiKeyInfo = apiKey

                val sys = """
너는 한국어 요리 코치야. 답변은 1~2문장, 결론부터 말해.
- 양/시간/비율 질문이면 수치 1개만 제시(필요 시 ±범위 짧게).
- 대체 재료는 가능/불가 → 가능 시 기본 비율 1개와 간단 보정 1개.
- 안전/위생/과다염분 등 위험 소지는 한 문장 경고.
- 확실치 않으면 필요한 추가 정보 1가지만 요청.
- 최근 대화와 현재 단계 맥락을 활용하되, 내부 데이터는 절대 드러내지 마.
- “참고 태그/데이터에 따르면/메모리에 따르면” 같은 표현 금지.
- 말투는 상냥하지만 군더더기 없이, 마침표로 끝내.
""".trimIndent()

                val memText = buildString {
                    if (memoryQueue.isNotEmpty()) {
                        appendLine("최근 대화 요약:")
                        memoryQueue.takeLast(8).forEach { m ->
                            appendLine("- 사용자: ${m.user}")
                            appendLine("  코치: ${m.assistant}")
                        }
                    }
                }.trim()

                val steps = recipe.order.split("○").filter { it.isNotBlank() }
                val stepBrief = steps.getOrNull(currentStepIndex)?.trim()?.take(140) ?: ""

                val ctx = """
[레시피] ${recipe.name}
[현재 단계 #${currentStepIndex + 1}] $stepBrief
[핵심 재료] ${recipe.ingredients.joinToString()}

${if (memText.isNotBlank()) memText else ""}
""".trimIndent()

                val prompt = """
$sys

사용자 질문: "$userText"

컨텍스트:
$ctx

출력 형식:
- 한글 1~2문장.
- 필요 시 수치/비율/시간 1개.
- 위험/주의 1개까지만.
""".trimIndent()

                client.sendMessage(
                    prompt = prompt,
                    role = "요리 코치",
                    onSuccess = { answer ->
                        val a = answer.trim().ifBlank { "아직 확답이 어려워요. 조금 더 구체적으로 물어봐 주세요." }
                        onAnswer(a)
                    },
                    onError = { e ->
                        Log.e(TAG, "OpenAI Q&A error: $e"); onError(e)
                    }
                )
            },
            onError = { e ->
                Log.e(TAG, "OpenAI key load fail: $e"); onError(e)
            }
        )
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
            "recipe" to toStructured(recipe),
            "memory" to memoryPayload(),
            "slots" to slots
        )

        Log.d(MEM_TAG, "callCoachLLM() send memory -> turns=${memoryQueue.size}, chars=${memoryTotalChars()}")

        val res = withTimeout(12_000) {
            Firebase.functions("us-central1")
                .getHttpsCallable("coachTurn")
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
                Log.e(TAG, "coachTurn JSON parse failed: ${it.message}"); emptyMap()
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
        CoachResult(
            intent = null,
            slots = emptyMap(),
            reply = "코칭 서버 연결에 문제가 있어요. 잠시 후 다시 시도해주세요."
        )
    }

    private suspend fun callFinalScorer(
        recipe: RecipeItem,
        slots: Map<String, Any>
    ): FinalScore {
        val input = mapOf("recipe" to toStructured(recipe), "events" to slots)

        val res = withTimeout(15_000) {
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

        return FinalScore(
            score     = (map["score"] as? Number)?.toInt() ?: 0,
            breakdown = (map["breakdown"] as? Map<String, Any>) ?: emptyMap(),
            summary   = (map["summary"] as? String).orEmpty()
        )
    }


    // 간단 규칙형 점수 (로컬 예시)
    private fun ruleScore(recipe: RecipeItem, slots: Map<String, Any>): Int {
        // 전체 단계 수
        val totalSteps = recipe.order.split("○").count { it.isNotBlank() }.coerceAtLeast(1)
        // 완료된 단계 수
        val completed = slots.keys.count { it.startsWith("step_") && it.endsWith("_done") }

        // 60점: 단계 진행률
        val stepScore = ((completed.toFloat() / totalSteps) * 60f).toInt()

        // 10점: 타이머 한 번이라도 사용
        val timerScore = if ((slots["used_timer"] as? Boolean) == true) 10 else 0

        // 30점: 기타 슬롯 이벤트(코칭 LLM이 채운 값 등). 과도 상승 방지를 위해 상한 둠
        val otherEvents = slots.size - completed - (if (slots.containsKey("used_timer")) 1 else 0)
        val eventScore = (otherEvents.coerceAtLeast(0) * 5).coerceAtMost(30)

        return (stepScore + timerScore + eventScore).coerceIn(0, 100)
    }

    private fun toStructured(recipe: RecipeItem): Map<String, Any> {
        val steps = recipe.order.split("○")
            .filter { it.isNotBlank() }
            .map { raw -> mapOf("text" to raw.trim()) }

        return mapOf(
            "id" to recipe.id,
            "name" to recipe.name,
            "ingredients" to recipe.ingredients,
            "steps" to steps
        )
    }
}
