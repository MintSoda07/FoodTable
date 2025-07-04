package com.bcu.foodtable.JetpackCompose.Social

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

// 1) ViewModel 클래스 정의
class MatzipViewModel : ViewModel() {
    // 아까 VectorMapViewModel 과는 별개로
    // 'matzip' 화면에서 사용할 상태/로직을 여기에 구현
    val restaurants = listOf(
        Restaurant("홍콩반점 점심특선", 37.5665, 126.9780),
        Restaurant("갈비명가",           37.5651, 126.9895),
        Restaurant("카레왕국",           37.5704, 126.9820),
    )

    // 전체 맛집 리스트 (Firestore에서 읽어서 세팅)
    var allRestaurants = mutableStateListOf<MatzipData>()
        private set

    // 찜한 맛집
    var favoriteRestaurants = mutableStateListOf<MatzipData>()
        private set

    // 주변 맛집 (ex: 3km 이내, 예시는 전체 목록 중 일부 랜덤으로)
    var nearbyRestaurants = mutableStateListOf<MatzipData>()
        private set

    // 검색 결과 (검색창에 입력시 갱신)
    var searchResults = mutableStateListOf<MatzipData>()
        private set

    init {
        // 샘플 데이터로 초기화 (실제로는 Firestore에서 불러오면 됨)
        val sample = listOf(
            MatzipData("1", "동네 김밥집", "싸고 맛있음", listOf("분식", "김밥"), "₩", "08:00-21:00", 4.5, "홍길동", 37.1, 127.0),
            MatzipData("2", "코끼리 카레", "인도커리 전문", listOf("커리", "인도음식"), "₩₩", "11:00-22:00", 4.3, "나나", 37.12, 127.01)
        )
        allRestaurants.addAll(sample)
        favoriteRestaurants.add(sample[0])
        nearbyRestaurants.addAll(sample)
        searchResults.addAll(sample)
    }

    // 검색 로직 (간단히 name/desc/tags에 query가 포함되면 추가)
    fun searchRestaurants(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            searchResults.clear()
            searchResults.addAll(allRestaurants)
        } else {
            val result = allRestaurants.filter {
                it.name.lowercase().contains(q)
                        || it.desc.lowercase().contains(q)
                        || it.tags.any { tag -> tag.lowercase().contains(q) }
            }
            searchResults.clear()
            searchResults.addAll(result)
        }
    }

    // 찜 추가/삭제 예시 (실제 Firestore 연동시에는 그에 맞게 처리)
    fun toggleFavorite(mat: MatzipData) {
        if (favoriteRestaurants.any { it.id == mat.id }) {
            favoriteRestaurants.removeAll { it.id == mat.id }
        } else {
            favoriteRestaurants.add(mat)
        }
    }

    // 주변 맛집 업데이트 (현위치 기반, 실제 구현시 거리 계산 필요)
    fun updateNearbyRestaurants(centerLat: Double, centerLng: Double, radiusKm: Double = 3.0) {
        // 예시: 모든 맛집을 다 넣음. 실제로는 위경도 거리 계산
        nearbyRestaurants.clear()
        nearbyRestaurants.addAll(allRestaurants)
    }
}

data class Restaurant(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class MatzipData(
    val id: String = "",
    val name: String = "",
    val desc: String = "",
    val tags: List<String> = emptyList(),
    val priceRange: String = "",
    val hours: String = "",
    val rating: Double = 0.0,
    val userName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)
