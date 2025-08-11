package com.bcu.foodtable.JetpackCompose.Social

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kakao.vectormap.LatLng
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MatzipViewModel : ViewModel() {
    // 1. 커스텀 마커는 기존처럼 Firestore에서 불러와서 지도에 표시
    var customMarkers = mutableStateMapOf<String, CustomMarkerData>()
        private set


    // 2. 카카오맵 API에서 지도 중심/영역 기준 실시간으로 불러온 음식점들
    var visibleRestaurants = mutableStateListOf<KakaoPlace>()
        private set

    // 3. 마커 클릭시 상세보기 정보
    var selectedPlace by mutableStateOf<KakaoPlace?>(null)
        private set

    // 4. (옵션) 사용자가 찜한 맛집 목록(Firestore에 저장된 것)
    var favoriteRestaurants = mutableStateListOf<KakaoPlace>()
        private set

    var cameraMoveTarget by mutableStateOf<LatLng?>(null)
        private set
    var pendingCustomMarker by mutableStateOf<CustomMarkerData?>(null)
        private set
    var selectedCustomMarker by mutableStateOf<CustomMarkerData?>(null)
        private set

    private var customMarkersListener: ListenerRegistration? = null

    private var favoritesListener: ListenerRegistration? = null

    var customMarkerToCreate by mutableStateOf<LatLng?>(null)
        private set

    fun showCustomMarkerCreationDialog(lat: Double, lng: Double) {
        customMarkerToCreate = LatLng.from(lat, lng)
    }

    fun dismissCustomMarkerCreationDialog() {
        customMarkerToCreate = null
    }

    fun moveToLocation(lat: Double, lng: Double) {
        cameraMoveTarget = LatLng.from(lat, lng)
    }
    fun resetCameraMoveTarget() { cameraMoveTarget = null }
    fun setPendingCustomMarkerValue(marker: CustomMarkerData) {
        pendingCustomMarker = marker
    }
    fun clearPendingCustomMarker() { pendingCustomMarker = null }
    fun showCustomMarkerDialog(marker: CustomMarkerData) {
        selectedCustomMarker = marker
    }
    fun dismissCustomMarkerDialog() { selectedCustomMarker = null }

    private val favoriteInFlight = mutableStateMapOf<String, Boolean>()
    private val unFavoriteInFlight = mutableStateMapOf<String, Boolean>()

    fun isFavoriteInFlight(id: String) = favoriteInFlight.containsKey(id)
    fun isUnFavoriteInFlight(id: String) = unFavoriteInFlight.containsKey(id)
    // 카카오맵 API 연동 객체
    private val kakaoMapApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KakaoMapApi::class.java)
    }

    /** Firestore에서 REST API 키 가져오기 */
    private suspend fun getKakaoRestApiKey(): String? {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("API_KEY")
            .whereEqualTo("KEY_NAME", "Kakao_rest")
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.getString("KEY_VALUE")
    }

    /** custom_markers 불러오기 */
    fun loadCustomMarkers() {
        val db = FirebaseFirestore.getInstance()
        customMarkersListener?.remove() // 중복 방지
        customMarkersListener = db.collection("custom_markers")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    Log.e("Firestore", "custom_markers 실시간 로딩 실패", e)
                    return@addSnapshotListener
                }

                customMarkers.clear() // 기존 마커 초기화

                for (doc in snap.documents) {
                    val marker = doc.toObject(CustomMarkerData::class.java)
                    if (marker != null) {
                        //  key-value 구조로 추가
                        customMarkers["cust_${marker.id}"] = marker
                    }
                }

                Log.d("Firestore", "커스텀 마커 ${customMarkers.size}개 로드 완료")
            }
    }



    /**
     * 지도 드래그/이동/줌 등에서 호출:
     * 현재 중심/반경/영역의 음식점 카카오맵 API로 fetch
     */
    fun fetchRestaurantsFromKakao(
        centerLat: Double,
        centerLng: Double,
        keyword: String = "음식점"
    ) {
        viewModelScope.launch {
            val kakaoKey = getKakaoRestApiKey() ?: return@launch
            try {
                val response = kakaoMapApi.searchPlace(
                    apiKey = "KakaoAK $kakaoKey",
                    query = keyword,
                    longitude = centerLng,
                    latitude = centerLat
                )
                visibleRestaurants.clear()
                visibleRestaurants.addAll(response.documents)
            } catch (e: Exception) {
                Log.e("카카오API", "지도 영역 맛집 불러오기 실패", e)
            }
        }
    }

    /** 마커 클릭시 상세보기용 데이터 세팅 */
    fun onMarkerClicked(place: KakaoPlace) {
        selectedPlace = place
    }

    fun dismissPlaceDialog() {
        selectedPlace = null
    }

    /**
     * (옵션) 사용자가 '찜'하거나 '상세보기'에서 저장을 원할 때 Firestore에 해당 음식점 정보 저장
     * - 추후 검색, 개인화, 찜리스트 등에 활용
     */
    fun saveRestaurantToFavorites(place: KakaoPlace) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val userMatzipRef = db.collection("user").document(userId).collection("matzip")
        val globalMatzipRef = db.collection("matzip_info")

        //  중복 클릭/중복 호출 가드
        if (favoriteInFlight.containsKey(place.id)) {
            Log.i("찜저장", "이미 진행중: ${place.id}")
            return
        }
        favoriteInFlight[place.id] = true

        // 이미 찜되어 있으면 바로 종료
        if (favoriteRestaurants.any { it.id == place.id }) {
            favoriteInFlight.remove(place.id)
            return
        }

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

        userMatzipRef.document(place.id).set(doc)
            .addOnSuccessListener { Log.i("찜저장", "user/$userId/matzip 저장 완료") }
            .addOnCompleteListener { favoriteInFlight.remove(place.id) }  // ✅ 해제

        globalMatzipRef.document(place.id).set(doc)
            .addOnSuccessListener { Log.i("공용찜저장", "matzip_info에도 저장됨") }
    }


    fun saveCustomMarker(name: String, desc: String, rating: Double, latLng: LatLng) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val db = FirebaseFirestore.getInstance()
        val id = "${System.currentTimeMillis()}"

        db.collection("user").document(userId).get()
            .addOnSuccessListener { snapshot ->
                val userName = snapshot.getString("name") ?: "익명"

                val marker = CustomMarkerData(
                    id = id,
                    name = name,
                    desc = desc,
                    rating = rating,
                    lat = latLng.latitude,
                    lng = latLng.longitude,
                    userName = userName
                )

                db.collection("custom_markers")
                    .document(id)
                    .set(marker)
                    .addOnSuccessListener {
                        Log.i("커스텀 마커", "저장 완료: $name")

                        // ✅ 즉시 지도에 반영되도록 직접 추가
                        customMarkers["cust_$id"] = marker

                        // ❌ showMarkers()는 자동 호출 안 되지만,
                        // 지도는 customMarkers 가 바뀌면 recomposition 으로 다시 그림
                    }
            }
    }




    /** Firestore에 저장된 찜한 음식점(추후 필요시 사용) */
    fun listenFavoriteRestaurants() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        favoritesListener?.remove()
        favoritesListener = db.collection("user").document(userId).collection("matzip")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener
                favoriteRestaurants.clear()
                for (doc in snap.documents) {
                    val place = KakaoPlace(
                        id = doc.getString("placeId") ?: doc.id,
                        place_name = doc.getString("name") ?: "",
                        category_name = doc.getString("category_name"),
                        category_group_code = doc.getString("category_group_code"),
                        phone = doc.getString("phone"),
                        address_name = doc.getString("address") ?: "",
                        road_address_name = doc.getString("roadAddress") ?: "",
                        place_url = doc.getString("placeUrl") ?: "",
                        x = doc.getDouble("lng")?.toString() ?: "",
                        y = doc.getDouble("lat")?.toString() ?: ""
                    )
                    favoriteRestaurants.add(place)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        favoritesListener?.remove()
        customMarkersListener?.remove()
    }

    fun removeRestaurantFromFavorites(placeId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        if (unFavoriteInFlight.containsKey(placeId)) {
            Log.i("찜해제", "이미 진행중: $placeId")
            return
        }
        unFavoriteInFlight[placeId] = true

        db.collection("user").document(userId).collection("matzip")
            .document(placeId)
            .delete()
            .addOnSuccessListener { Log.i("찜해제", "$placeId 해제됨") }
            .addOnCompleteListener { unFavoriteInFlight.remove(placeId) }  //
    }

    // 메시지 전송
    fun sendPlaceToChat(friendUid: String, place: KakaoPlace) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val message = ChatMessage(
            senderUid = myUid,
            type = "place",
            placeName = place.place_name,
            category = place.category_name,
            placeUrl = place.place_url,
            timestamp = System.currentTimeMillis()
        )

        // 기존 텍스트/이미지 전송과 동일하게 sender/receiver 모두에 저장
        viewModelScope.launch {
            sendMessage(db, myUid, friendUid, message)
        }
    }

}
