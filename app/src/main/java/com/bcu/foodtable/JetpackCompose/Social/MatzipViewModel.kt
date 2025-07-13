package com.bcu.foodtable.JetpackCompose.Social

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// 1) ViewModel 클래스 정의
class MatzipViewModel : ViewModel() {
    // 아까 VectorMapViewModel 과는 별개로
    // 'matzip' 화면에서 사용할 상태/로직을 여기에 구현
    val restaurants = listOf(
        Restaurant("홍콩반점 점심특선", 37.5665, 126.9780),
        Restaurant("갈비명가",           37.5651, 126.9895),
        Restaurant("카레왕국",           37.5704, 126.9820),
    )

    companion object {
        val CATEGORY_BUTTONS = listOf(
            "FD6" to "음식점",
            "CE7" to "카페",
            "CS2" to "편의점",
            "MT1" to "대형마트"
        )
    }

//    fun fixCategoryFields() {
//        viewModelScope.launch {
//            updateMissingCategoryFields()
//        }
//    }
//    // 임시 카테고리 살리는 코드
//    suspend fun updateMissingCategoryFields() = coroutineScope {
//        val db = FirebaseFirestore.getInstance()
//        val mats = db.collection("matzip_info").get().await()
//
//        val kakaoKey = getKakaoRestApiKey() // 네가 기존에 구현한 suspend 함수 재활용
//        if (kakaoKey.isNullOrBlank()) {
//            println("카카오 REST API 키가 없습니다.")
//            return@coroutineScope
//        }
//
//        for (doc in mats.documents) {
//            val docRef = doc.reference
//            val placeId = doc.getString("placeId") ?: continue
//            val name = doc.getString("name") ?: continue
//
//            // 이미 필드가 있으면 skip (효율)
//            if (doc.contains("category_group_code") && doc.contains("category_name")) continue
//
//            // 카카오 API 재조회 (이름 기반)
//            try {
//                val response = kakaoMapApi.searchPlace(
//                    apiKey = "KakaoAK $kakaoKey",
//                    query = name
//                )
//
//                // placeId로 정확히 일치하는 place만 사용
//                val place = response.documents.firstOrNull { it.id == placeId }
//                if (place != null) {
//                    val groupCode = place.category_group_code ?: ""
//                    val catName = place.category_name ?: ""
//
//                    // Firestore 업데이트
//                    docRef.update(
//                        mapOf(
//                            "category_group_code" to groupCode,
//                            "category_name" to catName
//                        )
//                    ).addOnSuccessListener {
//                        println("업데이트 성공: $name ($groupCode, $catName)")
//                    }.addOnFailureListener { e ->
//                        println("업데이트 실패: $name")
//                        e.printStackTrace()
//                    }
//                } else {
//                    println("카카오API에서 해당 placeId로 결과 없음: $name")
//                }
//            } catch (e: Exception) {
//                println("카카오API 오류: $name")
//                e.printStackTrace()
//            }
//            // 속도 제한: 카카오 쿼터 때문에 딜레이 살짝 두는게 안전함
//            delay(200)
//        }
//    }
    suspend fun removeDuplicateMatzipDocs() {
        val db = FirebaseFirestore.getInstance()
        val allDocs = db.collection("matzip_info").get().await()
        val byPlaceId = allDocs.documents.groupBy { it.getString("placeId") ?: it.id }
        var totalDeleted = 0
        for ((placeId, docs) in byPlaceId) {
            if (docs.size <= 1) continue
            val toDelete = docs.drop(1)
            for (doc in toDelete) {
                doc.reference.delete()
                totalDeleted++
                println("중복 삭제: $placeId (${doc.id})")
            }
        }
        println("정리 끝! 삭제된 중복 도큐먼트 개수: $totalDeleted")
    }
//    // 전국 시/도, 구/군별 위도/경도 범위 리스트
//    data class Area(val name: String, val latRange: Pair<Double, Double>, val lngRange: Pair<Double, Double>)
//
//    val allAreas = listOf(
//        Area("서울", 37.40 to 37.70, 126.70 to 127.20),
//        Area("경기북부", 37.60 to 38.30, 126.70 to 127.90),
//        Area("경기남부", 36.98 to 37.60, 126.70 to 127.90),
//        Area("인천", 37.36 to 37.60, 126.50 to 126.90),
//        Area("부산", 35.05 to 35.30, 128.80 to 129.30),
//        Area("대구", 35.70 to 36.00, 128.30 to 128.80),
//        Area("광주", 35.00 to 35.25, 126.70 to 127.05),
//        Area("대전", 36.25 to 36.45, 127.30 to 127.50),
//        Area("울산", 35.32 to 35.65, 129.13 to 129.45),
//        Area("세종", 36.48 to 36.68, 127.20 to 127.45),
//        Area("강원", 37.00 to 38.60, 127.50 to 129.30),
//        Area("충남", 36.10 to 36.90, 126.60 to 127.50),
//        Area("충북", 36.20 to 37.20, 127.20 to 128.30),
//        Area("전북", 35.30 to 36.00, 126.60 to 127.60),
//        Area("전남", 34.60 to 35.40, 126.10 to 127.80),
//        Area("경북", 35.90 to 37.20, 128.00 to 129.50),
//        Area("경남", 34.80 to 35.70, 127.70 to 129.30),
//        Area("제주", 33.10 to 33.60, 126.10 to 126.90)
//    )
//    fun fetchAllNationwideMatzipTotal(
//        categoryKeyword: String = "편의점",
//        step: Double = 0.12
//    ) {
//        viewModelScope.launch {
//            val db = FirebaseFirestore.getInstance()
//            val kakaoKey = getKakaoRestApiKey()
//            if (kakaoKey.isNullOrBlank()) {
//                Log.e("전국수집", "카카오 REST API 키 없음")
//                return@launch
//            }
//
//            var totalNew = 0
//            for (area in allAreas) {
//                Log.d("전국수집", "==== ${area.name} 시작 ====")
//                for (lat in area.latRange.first..area.latRange.second step step) {
//                    for (lng in area.lngRange.first..area.lngRange.second step step) {
//                        try {
//                            val response = kakaoMapApi.searchPlace(
//                                apiKey = "KakaoAK $kakaoKey",
//                                query = categoryKeyword,
//                                longitude = lng,
//                                latitude = lat
//                            )
//                            for (place in response.documents) {
//                                // 중복 방지(이미 placeId 있으면 SKIP)
//                                val existing = db.collection("matzip_info")
//                                    .whereEqualTo("placeId", place.id)
//                                    .limit(1).get().await()
//                                if (existing.size() > 0) {
//                                    Log.d("전국수집", "중복 SKIP: ${place.place_name} (${place.x}, ${place.y})")
//                                    continue
//                                }
//                                val doc = hashMapOf(
//                                    "placeId" to place.id,
//                                    "name" to place.place_name,
//                                    "address" to place.address_name,
//                                    "roadAddress" to place.road_address_name,
//                                    "phone" to (place.phone ?: ""),
//                                    "lat" to place.y.toDoubleOrNull(),
//                                    "lng" to place.x.toDoubleOrNull(),
//                                    "placeUrl" to place.place_url,
//                                    "category_group_code" to (place.category_group_code ?: ""),
//                                    "category_name" to (place.category_name ?: "")
//                                )
//                                db.collection("matzip_info").document(place.id).set(doc)
//                                totalNew++
//                                Log.d("전국수집", "추가: ${place.place_name} (${place.x}, ${place.y})")
//                            }
//                            delay(700) // 카카오 쿼터/블록 방지
//                        } catch (e: Exception) {
//                            Log.e("전국수집", "카카오 API 오류: $lat, $lng", e)
//                            delay(1700)
//                        }
//                    }
//                }
//                Log.d("전국수집", "==== ${area.name} 끝 ====")
//            }
//            Log.d("전국수집", "== 전국 수집 완전 끝! 추가된 곳: $totalNew 개 ==")
//            loadAllRestaurants()
//        }
//    }


    // Double Progression step 확장 함수 (반복문용)
    private infix fun ClosedFloatingPointRange<Double>.step(step: Double): Iterable<Double> =
        generateSequence(start) { prev ->
            if (prev + step > endInclusive) null else prev + step
        }.asIterable()

    // 전체 맛집 리스트 (Firestore에서 읽어서 세팅)
    var allRestaurants = mutableStateListOf<MatzipData>()
        private set

    // [2] 화면에 보여줄(필터된) 리스트
    var visibleRestaurants = mutableStateListOf<MatzipData>()
        private set

    // [3] 현재 선택된 카테고리/서브카테고리 상태
    var selectedCategoryGroup by mutableStateOf("FD6")
    var selectedSubCategory by mutableStateOf("")
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

    // [6] Firestore에서 전체 장소 불러오기 (category 필드 포함!)
    fun loadAllRestaurants() {
        FirebaseFirestore.getInstance()
            .collection("matzip_info")
            .get()
            .addOnSuccessListener { snap ->
                allRestaurants.clear()
                snap.documents.forEach { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: ""
                    val desc = doc.getString("desc") ?: ""
                    val tags = (doc.get("tags") as? List<String>) ?: emptyList()
                    val priceRange = doc.getString("priceRange") ?: ""
                    val hours = doc.getString("hours") ?: ""
                    val rating = doc.getDouble("rating") ?: 0.0
                    val userName = doc.getString("userName") ?: "익명"
                    val lat = doc.getDouble("lat") ?: 0.0
                    val lng = doc.getDouble("lng") ?: 0.0
                    val phone = doc.getString("phone") ?: ""
                    val address = doc.getString("address") ?: ""
                    val roadAddress = doc.getString("roadAddress") ?: ""
                    val placeUrl = doc.getString("placeUrl") ?: ""
                    val categoryGroupCode = doc.getString("category_group_code") ?: ""
                    val categoryName = doc.getString("category_name") ?: ""

                    allRestaurants.add(
                        MatzipData(
                            id, name, desc, tags, priceRange, hours, rating, userName,
                            lat, lng, categoryGroupCode, categoryName, phone, address, roadAddress, placeUrl
                        )
                    )
                }
                filterVisibleRestaurants()
            }
    }

    // [7] 카테고리/하위카테고리 기준으로 필터
    fun filterVisibleRestaurants() {
        visibleRestaurants.clear()
        val filtered = allRestaurants.filter {
            it.categoryGroupCode == selectedCategoryGroup &&
                    (selectedSubCategory.isBlank() || it.categoryName == selectedSubCategory)
        }
        Log.d("마커DEBUG", "필터 결과: ${filtered.size}개, 카테고리: $selectedCategoryGroup, 서브: $selectedSubCategory")
        visibleRestaurants.addAll(filtered)
    }

    // [8] 상위 카테고리 변경
    fun setCategoryGroup(newGroup: String) {
        selectedCategoryGroup = newGroup
        selectedSubCategory = ""
        filterVisibleRestaurants()
    }

    // [9] 하위 카테고리 변경
    fun setSubCategory(newSub: String) {
        selectedSubCategory = newSub
        filterVisibleRestaurants()
    }

    // [10] 하위 카테고리 자동 추출
    fun getSubCategoriesForSelectedGroup(): List<String> =
        allRestaurants.filter { it.categoryGroupCode == selectedCategoryGroup }
            .map { it.categoryName }
            .distinct()
            .sorted()


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
    // Retrofit 객체 싱글톤으로 생성
    private val kakaoMapApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KakaoMapApi::class.java)
    }

    /**
     * Firestore에서 Kakao REST API 키를 읽어옴 (suspend)
     */
    private suspend fun getKakaoRestApiKey(): String? {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("API_KEY")
            .whereEqualTo("KEY_NAME", "Kakao_rest")
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.getString("KEY_VALUE")
    }

    fun fetchAllCategories() {
        // 한 번에 여러 주요 카테고리 쿼리!
        fetchAndSaveMatzipFromKakao("맛집")
        fetchAndSaveMatzipFromKakao("카페")
        fetchAndSaveMatzipFromKakao("편의점")
        fetchAndSaveMatzipFromKakao("마트")  // 또는 "대형마트", "이마트", "홈플러스" 등
    }
    /**
     * 검색어로 Kakao API에서 장소 검색 → Firestore에 저장
     */

    fun fetchAndSaveMatzipFromKakao(query: String, onFinish: (() -> Unit)? = null) {
        viewModelScope.launch {
            Log.d("디버그", "fetchAndSaveMatzipFromKakao() 호출됨")
            val kakaoKey = getKakaoRestApiKey()
            Log.d("디버그", "카카오 키: $kakaoKey")
            if (kakaoKey.isNullOrBlank()) {
                // 실패 처리
                onFinish?.invoke()
                return@launch
            }

            val response = try {
                kakaoMapApi.searchPlace(
                    apiKey = "KakaoAK $kakaoKey",
                    query = query
                )
            } catch (e: Exception) {
                // 네트워크/API 오류 처리
                Log.e("디버그", "카카오 API 호출 에러", e)
                onFinish?.invoke()
                return@launch
            }

            // Firestore에 저장
            val db = FirebaseFirestore.getInstance()
            Log.d("카카오API응답", "response.documents.size=${response.documents.size}")
            response.documents.forEach { place ->
                Log.d("카카오API응답", "category_group_code=${place.category_group_code}, category_name=${place.category_name}")
                val doc = hashMapOf(
                    "placeId" to place.id,
                    "name" to place.place_name,
                    "address" to place.address_name,
                    "roadAddress" to place.road_address_name,
                    "phone" to (place.phone ?: ""),
                    "lat" to place.y.toDoubleOrNull(),
                    "lng" to place.x.toDoubleOrNull(),
                    "placeUrl" to place.place_url,
                    "category_group_code" to (place.category_group_code ?: ""),
                    "category_name" to (place.category_name ?: "")
                )
                db.collection("matzip_info")
                    .document(place.id)  // place.id or place.placeId 등 유니크값!
                    .set(doc)            // add() 대신 set()
                    .addOnSuccessListener { Log.d("Firestore", "맛집 저장 성공: ${doc["name"]}") }
                    .addOnFailureListener { e -> Log.e("Firestore", "맛집 저장 실패", e) }
            }
            loadAllRestaurants()
            onFinish?.invoke()
        }
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
    val lng: Double = 0.0,
    val category: String = "",
    val phone: String = "",
    val address: String = "",
    val roadAddress: String = "",
    val placeUrl: String = "",
    val categoryGroupCode: String = "",   // FD6, CE7, CS2, MT1
    val categoryName: String = "",        // 한식, 중식, 베이커리, 치킨 등
)

