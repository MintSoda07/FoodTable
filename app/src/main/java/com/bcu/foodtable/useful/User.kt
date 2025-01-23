package com.bcu.foodtable.useful

// 유저의 정보를 담을 User 데이터 클래스
// !!! 주의 !!!
// DB에 유저 필드 추가시 아래에 위치한 User 내용물도 추가해 주어야 하며, UserManager.kt 또한 알맞게 수정해 주어야 함.
// RecipeItem 또한 DB 수정 시 해당 data class 수정 필요.

data class User(
    val Name: String = "",
    val email: String = "",
    val image: String = "",
    val phoneNumber: String = "",
    var point: Int = 0,
    var uid: String=""
)