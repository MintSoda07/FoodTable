package com.bcu.foodtable.useful

// API 데이터 클래스
data class ApiKey(
    val KEY_NAME: String? = null,
    val KEY_VALUE: String? = null
)

// 상세 API 데이터 클래스
data class ApiKeyWithDetails(
    val KEY_NAME: String? = null,
    val KEY_VALUE: String? = null,
    val API_URL: String? = null,
    val API_SECRET: String? = null
)