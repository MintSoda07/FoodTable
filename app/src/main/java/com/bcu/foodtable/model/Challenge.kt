package com.bcu.foodtable.model

import kotlinx.serialization.Serializable

@Serializable
data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ChallengeType = ChallengeType.DAILY,
    val reward: Int = 0,
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val isCompleted: Boolean = false,
    val progress: Int = 0,
    val targetValue: Int = 0
)
@Serializable
enum class ChallengeType {
    DAILY,
    WEEKLY,
    ACHIEVEMENT
}