package com.bcu.foodtable.ui.myPage.myFridge


data class Ingredient(
    val name: String = "",
    val quantity: Int = 1,
    val expireDate: String = "", // "2025-05-21"
    val section: String = "냉장"
)


