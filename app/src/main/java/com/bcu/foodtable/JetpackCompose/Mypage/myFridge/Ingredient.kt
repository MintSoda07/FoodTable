package com.bcu.foodtable.JetpackCompose.Mypage.myFridge


data class Ingredient(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 1,
    val expireDate: String = "", // "2025-05-21"
    val section: String = "냉장",
    val shelfIndex: Int = 0, // 선반 위치 구분
    val imageResId: String = "" // 혹은 Int
)


