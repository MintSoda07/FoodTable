package com.bcu.foodtable.useful

import com.google.firebase.Timestamp

// 레시피의 정보를 저장하기 위한 data class.
data class RecipeItem(
    val name: String = "",
    val description: String = "",
    val imageResId: String = "", // Firestore에서 이미지 URL을 불러옴
    val clicked: Int = 0,
    val date : Timestamp = Timestamp.now(),
    val order : String ="",
    var id: String = "",
    var C_categories : List<String> = listOf(),
    var note : String="",
    var tags : List<String> = listOf(),
    var ingredients : List<String> = listOf(),
    val contained_channel : String="",
    var estimatedCalories: String? = null
)