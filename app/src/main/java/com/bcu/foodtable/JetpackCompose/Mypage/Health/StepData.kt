package com.bcu.foodtable.JetpackCompose.Mypage.Health

data class StepData(
    val date: String,  // 예: "월", "화", "05.21" 등 X축에 표시될 라벨
    val steps: Int     // 걸음 수 (Y축 데이터)
)
