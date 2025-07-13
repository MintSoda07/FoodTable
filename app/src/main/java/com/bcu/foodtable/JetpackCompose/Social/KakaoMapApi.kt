package com.bcu.foodtable.JetpackCompose.Social

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoMapApi {
    @GET("v2/local/search/keyword.json")
    suspend fun searchPlace(
        @Header("Authorization") apiKey: String, // "KakaoAK {REST_API_KEY}"
        @Query("query") query: String,
        @Query("x") longitude: Double? = null,
        @Query("y") latitude: Double? = null,
        @Query("page") page: Int? = null
    ): KakaoPlaceResponse
}

data class KakaoPlaceResponse(
    val documents: List<KakaoPlace>
)

data class KakaoPlace(
    val id: String,
    val place_name: String,
    val category_name: String?,
    val category_group_code: String?,
    val phone: String?,
    val address_name: String,
    val road_address_name: String,
    val place_url: String,
    val x: String, // 경도
    val y: String  // 위도
)
