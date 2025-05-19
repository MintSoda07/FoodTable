package com.bcu.foodtable.JetpackCompose.Mypage

data class HealthConnectState(
    val steps: Int = 0,
    val goal: Int = 20000,
    val resultText: String = "",
    val rewardCount: Int = 0,
    val foodItem: FoodItem? = null
)
