// ChallengeUtils.kt
package com.bcu.foodtable.util

import com.bcu.foodtable.model.Challenge
import com.bcu.foodtable.model.ChallengeType
import java.util.*

object ChallengeUtils {
    fun generateRandomChallenges(type: ChallengeType, count: Int): List<Challenge> {
        val pool = listOf(
            Challenge(id = UUID.randomUUID().toString(), title = "🚰 물 8잔 마시기", description = "건강을 위해 수분을 충분히 섭취해요.", targetValue = 1, progress = 0, reward = 100, type = type),
            Challenge(id = UUID.randomUUID().toString(), title = "🍱 건강 도시락 챙기기", description = "식단을 챙기는 하루!", targetValue = 1, progress = 0, reward = 120, type = type),
            Challenge(id = UUID.randomUUID().toString(), title = "📝 식사 일기 작성", description = "오늘 먹은 걸 기록해요.", targetValue = 1, progress = 0, reward = 90, type = type),
            Challenge(id = UUID.randomUUID().toString(), title = "🍎 아침 과일 먹기", description = "하루를 상큼하게 시작해보세요!", targetValue = 1, progress = 0, reward = 80, type = type),
            Challenge(id = UUID.randomUUID().toString(), title = "🚶‍♂️ 산책 30분", description = "가볍게 걷기만 해도 몸이 달라져요.", targetValue = 1, progress = 0, reward = 150, type = type)
        )
        return pool.shuffled().take(count)
    }
}
