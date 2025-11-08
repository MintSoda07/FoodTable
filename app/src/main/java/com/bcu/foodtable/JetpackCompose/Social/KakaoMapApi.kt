package com.bcu.foodtable.JetpackCompose.Social

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * 카카오맵 장소검색 API Retrofit 인터페이스
 */
interface KakaoMapApi {
    @GET("v2/local/search/keyword.json")
    suspend fun searchPlace(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("x") longitude: Double? = null,
        @Query("y") latitude: Double? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,     // ← 추가 (최대 15)
        @Query("rect") rect: String? = null,  // ← 선택: 화면 bounds로 쿼리하고 싶을 때
        @Query("radius") radius: Int? = null  // ← 선택
    ): KakaoPlaceResponse
}

/**
 * 카카오맵 장소 검색 결과(루트 응답)
 */
data class KakaoPlaceResponse(
    val meta: KakaoMeta,
    val documents: List<KakaoPlace>
)

data class KakaoMeta(
    val total_count: Int,
    val pageable_count: Int,
    val is_end: Boolean
)

/**
 * 카카오맵 장소 상세 데이터
 */
data class KakaoPlace(
    val id: String,
    val place_name: String,
    val category_name: String?,
    val category_group_code: String?,
    val phone: String?,
    val address_name: String,
    val road_address_name: String,
    val place_url: String,
    val x: String, // 경도(longitude)
    val y: String,  // 위도(latitude)
    val rating: Double = 0.0
)
