package com.bcu.foodtable.JetpackCompose.Social

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bcu.foodtable.R
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.LatLngBounds
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import kotlinx.coroutines.launch
import com.kakao.vectormap.camera.CameraPosition
import kotlinx.coroutines.tasks.await


private const val TAG = "RestaurantV2Map"
//카테고리 띄우는 스크린
@Composable
fun RestaurantMapMainScreen(
    modifier: Modifier = Modifier,
    viewModel: MatzipViewModel = viewModel()
) {
    Box(modifier = modifier) {
        // 지도 자체(항상 바닥)
        RestaurantV2MapScreen(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel
        )
        // 카테고리바/칩 Overlay! (Top에 겹침)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)   // 지도 위 약간 띄우기(선택)
                .align(Alignment.TopCenter)
        ) {
            CategorySelectorBar(
                selected = viewModel.selectedCategoryGroup,
                onSelect = { viewModel.setCategoryGroup(it) }
            )
            SubCategoryChips(
                subCategories = viewModel.getSubCategoriesForSelectedGroup(),
                selected = viewModel.selectedSubCategory,
                onSelect = { viewModel.setSubCategory(it) }
            )
        }
    }
}
// 지도 띄우는 맵 스크린
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RestaurantV2MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MatzipViewModel = viewModel()
) {
    // ① 권한 상태
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // ② 최초 진입 시 권한 요청
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    // ③ 승인 여부에 따라 분기
    if (permissionState.status.isGranted) {

        MapWithTracking(modifier, viewModel)
    } else {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("위치 권한이 필요합니다.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MapWithTracking(
    modifier: Modifier,
    viewModel: MatzipViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    val customDesc = remember { mutableStateMapOf<String, String>() }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDescDialog by remember { mutableStateOf(false) }
    var newPos by remember { mutableStateOf<LatLng?>(null) }
    var inputDesc by remember { mutableStateOf("") }
    var currentDesc by remember { mutableStateOf("") }

    var inputName by remember { mutableStateOf("") }
    var inputRating by remember { mutableStateOf(3f) }
    var inputPriceRange by remember { mutableStateOf("") }
    var inputHours by remember { mutableStateOf("") }
    var inputTags by remember { mutableStateOf("") }
    var isLoaded by remember { mutableStateOf(false) }

    val DEFAULT_LATLNG = LatLng.from(37.554722, 126.970833) // 서울역

// (개발/테스트용, 실제 배포시엔 주석 처리)
    LaunchedEffect(Unit) {
//        viewModel.removeDuplicateMatzipDocs()
//        viewModel.fetchAllNationwideMatzipTotal("편의점")
        //viewModel.fixCategoryFields()
        // 최초 1회만 수동 실행 (또는 관리자 버튼으로!)
        // viewModel.fetchAndSaveMatzipFromKakao("맛집")
        // viewModel.fetchAndSaveMatzipFromKakao("카페")
        // viewModel.fetchAndSaveMatzipFromKakao("편의점")
    }

    // ----- 여기에 추가 -----
    // 1) 상세 정보를 담을 데이터 클래스
    data class MarkerData(
        val name: String,
        val desc: String,
        val tags: List<String>,
        val priceRange: String,
        val hours: String,
        val rating: Double,
        val userName: String,
        val lat: Double,
        val lng: Double
    )
    // 2) ID → MarkerData 매핑
    val customMarkers = remember { mutableStateMapOf<String, MarkerData>() }
    // 3) 클릭한 마커 ID 저장
    var currentMarkerId by remember { mutableStateOf<String?>(null) }


    // --- 앱 시작/지도 준비 시점에서 customMarkers Map을 Firestore에서 초기화!
    LaunchedEffect(Unit) {
        viewModel.loadAllRestaurants()
        // (1) custom_markers
        val customSnap = FirebaseFirestore.getInstance()
            .collection("custom_markers")
            .get()
            .await()
        customSnap.documents.forEach { doc ->
            val id = doc.id
            val name = doc.getString("name") ?: ""
            val desc = doc.getString("desc") ?: ""
            val tags = (doc.get("tags") as? List<String>) ?: emptyList()
            val priceRange = doc.getString("priceRange") ?: ""
            val hours = doc.getString("hours") ?: ""
            val rating = doc.getDouble("rating") ?: 0.0
            val userName = doc.getString("userName") ?: "익명"
            val lat = doc.getDouble("lat") ?: 0.0
            val lng = doc.getDouble("lng") ?: 0.0

            customMarkers["cust_$id"] = MarkerData(
                name, desc, tags, priceRange, hours, rating, userName, lat, lng
            )
        }

        // (2) matzip_info
        val matzipSnap = FirebaseFirestore.getInstance()
            .collection("matzip_info")
            .get()
            .await()
        viewModel.allRestaurants.clear()
        matzipSnap.documents.forEach { doc ->
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
            val category = doc.getString("category") ?: ""
            val phone = doc.getString("phone") ?: ""
            val address = doc.getString("address") ?: ""
            val roadAddress = doc.getString("roadAddress") ?: ""
            val placeUrl = doc.getString("placeUrl") ?: ""
            val groupCode = doc.getString("category_group_code") ?: ""
            val catName = doc.getString("category_name") ?: ""

            viewModel.allRestaurants.add(
                MatzipData(id, name, desc, tags, priceRange, hours, rating, userName, lat, lng, category, phone, address, roadAddress, placeUrl, categoryGroupCode = groupCode, categoryName = catName)
            )
        }
    }


    // MapView
    val mapView = remember {
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }



    // 트래킹 상태
    var userLocationLabel by remember { mutableStateOf<Label?>(null) }
    var trackingEnabled by remember { mutableStateOf(true) }
    var userLocationLatLng by remember { mutableStateOf(DEFAULT_LATLNG) }

    fun addOrUpdateUserLocationMarker(
        context: Context,
        layer: LabelLayer?,
        userPos: LatLng
    ) {
        val srcBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.user_loc)
        val sizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics
        ).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, sizePx, sizePx, true)
        val style = LabelStyle.from(scaledBitmap)
        val opts = LabelOptions.from("user_loc", userPos)
            .setStyles(style)
            .setRank(10L)
        userLocationLabel = layer?.addLabel(opts)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    KakaoMapSdk.init(context, "b0402134c35d48a50145988a7727b74b")
                    mapView.start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() { /*…*/ }
                            override fun onMapError(e: Exception?) { Log.e(TAG, "MapError", e) }
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(mapInstance: KakaoMap) {
                                // 지도 준비 직후
                                isLoaded = true
                                mapView.resume()
                                kakaoMap = mapInstance
                                val layer = mapInstance.labelManager?.layer

                                // (1) 기존 마커 표시 부분 모두 삭제! (이벤트로만 표시)


                                // (2) 마커 추가 함수 추출
                                fun showMarkersNearCenter(
                                    layer: LabelLayer?,
                                    center: LatLng,
                                    zoomLevel: Float,
                                    userPos: LatLng,           // <- 현위치(lat, lng)를 추가 파라미터로 전달!
                                    context: Context           // <- 리소스 접근용(외부에서 넣어줘야 함)
                                ) {
                                    // 1. 모든 마커 삭제 (removeAll하면 user_loc도 사라지므로 반드시 아래에서 새로 추가!)
                                    layer?.removeAll()

                                    // --- (1) 현위치 마커 새로 등록 ---
                                    val userBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.user_loc)
                                    val sizePx = TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics
                                    ).toInt()
                                    val scaledUserBitmap = Bitmap.createScaledBitmap(userBitmap, sizePx, sizePx, true)
                                    val userStyle = LabelStyle.from(scaledUserBitmap)
                                    val userLabelOpts = LabelOptions.from("user_loc", userPos)
                                        .setStyles(userStyle)
                                        .setRank(9999L) // 다른 마커 위에 보이게 rank 크게
                                    userLocationLabel = layer?.addLabel(userLabelOpts) // 최신 핸들 할당

                                    // 2. 파라미터 계산
                                    val radius = when {
                                        zoomLevel < 12f -> 0.1
                                        zoomLevel < 15f -> 0.03
                                        else -> 0.01
                                    }
                                    val latMin = center.latitude - radius
                                    val latMax = center.latitude + radius
                                    val lngMin = center.longitude - radius
                                    val lngMax = center.longitude + radius

                                    val markerLimit = when {
                                        zoomLevel < 12f -> 20
                                        zoomLevel < 15f -> 50
                                        else -> 100
                                    }

                                    Log.d(TAG, "== allRestaurants: ${viewModel.allRestaurants.size}")
                                    Log.d(TAG, "== visibleRestaurants: ${viewModel.visibleRestaurants.size}")

                                    viewModel.visibleRestaurants.forEach { matzip ->
                                        Log.d("마커DEBUG", "지도에 추가: ${matzip.name}, ${matzip.lat}, ${matzip.lng}")
                                        val pos = LatLng.from(matzip.lat, matzip.lng)
                                        val opts = LabelOptions.from("matzip_${matzip.id}", pos)
                                            .setStyles(R.drawable.user_loc_small) // 원하는 마커 아이콘
                                            .setRank(10L)
                                        layer?.addLabel(opts)
                                        Log.d(TAG, "마커 추가: ${matzip.name}, ${matzip.lat}, ${matzip.lng}")

                                    }

                                    // 4. custom_markers 마커 표시
                                    FirebaseFirestore.getInstance()
                                        .collection("custom_markers")
                                        .whereGreaterThanOrEqualTo("lat", latMin)
                                        .whereLessThanOrEqualTo("lat", latMax)
                                        .whereGreaterThanOrEqualTo("lng", lngMin)
                                        .whereLessThanOrEqualTo("lng", lngMax)
                                        .limit(markerLimit.toLong())
                                        .get()
                                        .addOnSuccessListener { snap ->
                                            Log.d("마커", "custom_markers: ${snap.size()}개, center=${center.latitude},${center.longitude}")
                                            val srcBmp = BitmapFactory.decodeResource(context.resources, R.drawable.user_loc_small)
                                            val targetPx = TypedValue.applyDimension(
                                                TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics
                                            ).toInt()
                                            val scaledBmp = Bitmap.createScaledBitmap(srcBmp, targetPx, targetPx, true)
                                            val style = LabelStyle.from(scaledBmp)
                                            snap.documents.forEach { doc ->
                                                val id = doc.id
                                                val lat = doc.getDouble("lat") ?: return@forEach
                                                val lng = doc.getDouble("lng") ?: return@forEach
                                                Log.d("마커", "add cust: id=$id, lat=$lat, lng=$lng")
                                                val pos = LatLng.from(lat, lng)
                                                val opts = LabelOptions.from("cust_$id", pos)
                                                    .setStyles(style)
                                                    .setRank(10L)
                                                layer?.addLabel(opts)
                                            }
                                        }
                                }

                                // 카메라 이동 종료(Idle과 유사)

                                // ---- 최초 진입
                                val cameraPos = mapInstance.cameraPosition
                                val center: LatLng = cameraPos?.getPosition() ?: DEFAULT_LATLNG
                                val zoomLevel = cameraPos?.zoomLevel?.toFloat() ?: 15f
                                showMarkersNearCenter(layer, center, zoomLevel, userLocationLatLng, context) // <- 파라미터 추가!
                                addOrUpdateUserLocationMarker(context, layer, userLocationLatLng)            // <- 파라미터 추가!

                                // ---- 카메라 이동 이벤트
                                mapInstance.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                                    val center = cameraPosition?.getPosition() ?: DEFAULT_LATLNG
                                    val zoomLevel = cameraPosition?.zoomLevel?.toFloat() ?: 15f
                                    showMarkersNearCenter(layer, center, zoomLevel, userLocationLatLng, context) // <- 파라미터 추가!
                                    addOrUpdateUserLocationMarker(context, layer, userLocationLatLng)            // <- 파라미터 추가!
                                }







                                // --- 지도 터치로 새 마커 추가 ---
                                mapInstance.setOnMapClickListener { _, position, _, _ ->
                                    newPos = position
                                    showAddDialog = true
                                    false
                                }

                                //--- 라벨(마커) 탭 시 설명 다이얼로그 ---
                                mapInstance.setOnLabelClickListener { kakaoMap, layer, label ->
                                    if (!isLoaded) return@setOnLabelClickListener false
                                    Log.d("마커클릭", "labelId: ${label.labelId}") // 로그!
                                    currentMarkerId = label.labelId  // label.id 또는 label.labelId (SDK에 따라 다름)
                                    showDescDialog = true
                                    true
                                }


                                // --- 샘플 POI 표시 (viewModel.restaurants 등) ---
                                Log.d(TAG, "layer null? ${layer == null}")
                                viewModel.restaurants.forEachIndexed { idx, rest ->
                                    val p = LatLng.from(rest.latitude, rest.longitude)
                                    val opts = LabelOptions.from("rest_$idx", p)
                                        .setStyles(R.drawable.baseline_restaurant_menu_24)
                                        .setRank(idx.toLong())
                                    layer?.addLabel(opts)
                                    Log.d(TAG, "added user label: $userLocationLabel")
                                }

                                // --- 현위치 트래킹 ---
                                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                                val req = LocationRequest.create().apply {
                                    interval = 5_000L
                                    fastestInterval = 2_000L
                                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                                }
                                val cb = object : LocationCallback() {
                                    override fun onLocationResult(result: LocationResult) {
                                        Log.d(TAG, "onLocationResult ▶ ${result.lastLocation}")
                                        val loc = result.lastLocation ?: return
                                        val pos = LatLng.from(loc.latitude, loc.longitude)
                                        userLocationLatLng = pos
                                        val srcBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.user_loc)
                                        val sizeDp = 24f
                                        val sizePx = TypedValue.applyDimension(
                                            TypedValue.COMPLEX_UNIT_DIP, sizeDp, context.resources.displayMetrics
                                        ).toInt()
                                        val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, sizePx, sizePx, true)
                                        val style = LabelStyle.from(scaledBitmap)
                                        val opts = LabelOptions.from("user_loc", pos)
                                            .setStyles(style)
                                            .setRank(10L)

                                        if (userLocationLabel == null) {
                                            userLocationLabel = layer?.addLabel(opts)
                                        } else {
                                            userLocationLabel?.moveTo(pos)
                                        }

                                        if (trackingEnabled) {
                                            kakaoMap?.let { map ->
                                                try {
                                                    map.moveCamera(
                                                        CameraUpdateFactory.newCenterPosition(pos),
                                                        CameraAnimation.from(500, true, true)
                                                    )
                                                } catch (e: RuntimeException) {
                                                    Log.e(TAG, "moveCamera 실패, 무시합니다", e)
                                                }
                                            }
                                            trackingEnabled = false
                                        }
                                    }
                                }

                                // --- 위치 권한 체크 ---
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    try {
                                        fusedClient.requestLocationUpdates(req, cb, Looper.getMainLooper())
                                    } catch (e: SecurityException) {
                                        Log.e(TAG, "위치 권한이 없어서 위치 업데이트를 요청할 수 없습니다.", e)
                                    }
                                } else {
                                    // 권한 요청 로직(Compose에서 Accompanist 권한 요청 등)
                                }
                            }


                        }
                    )
                }
                Lifecycle.Event.ON_START -> mapView.resume()
                Lifecycle.Event.ON_STOP  -> mapView.pause()
                Lifecycle.Event.ON_DESTROY -> { /* V2엔 stop() 없음 */ }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(factory = { mapView }, modifier = modifier)

    //  “맛집 설명 입력” 다이얼로그
    if (showAddDialog && newPos != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; inputDesc = "" },
            title = { Text("맛집 설명 추가") },
            text = {
                Column {
                    // ① 식당 이름
                    TextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text("식당 이름") },
                        singleLine = true
                    )
                    // ② 한 줄 설명
                    TextField(
                        value = inputDesc,
                        onValueChange = { inputDesc = it },
                        placeholder = { Text("간단 설명") },
                        singleLine = true
                    )
                    // ③ 태그(콤마 구분)
                    TextField(
                        value = inputTags,
                        onValueChange = { inputTags = it },
                        placeholder = { Text("태그(예: 중식,분식)") },
                        singleLine = true
                    )
                    // ④ 가격대
                    TextField(
                        value = inputPriceRange,
                        onValueChange = { inputPriceRange = it },
                        placeholder = { Text("가격대(₩,₩₩₩)") },
                        singleLine = true
                    )
                    // ⑤ 영업시간
                    TextField(
                        value = inputHours,
                        onValueChange = { inputHours = it },
                        placeholder = { Text("영업시간(예: 10:00-21:00)") },
                        singleLine = true
                    )
                    // ⑥ 별점 슬라이더
                    Text("별점: ${"%.1f".format(inputRating)}")
                    Slider(
                        value = inputRating,
                        onValueChange = { inputRating = it },
                        valueRange = 0f..5f,
                        steps = 9
                    )
                }
            },
            confirmButton = {
                // 필수 입력 값 검사
                val allFilled =
                    inputName.isNotBlank() &&
                            inputDesc.isNotBlank() &&
                            inputPriceRange.isNotBlank() &&
                            inputHours.isNotBlank()

                TextButton(
                    onClick = {
                        // 1. Firestore 도큐먼트 ID 준비
                        val docRef = FirebaseFirestore.getInstance()
                            .collection("custom_markers")
                            .document()
                        val id = docRef.id
                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@TextButton

                        // 2. 위치(newPos!!)와 context, layer 가져오기
                        val pos = newPos!!
                        val layer = kakaoMap?.labelManager?.layer

                        // 3. Bitmap 스케일링
                        val srcBmp = BitmapFactory.decodeResource(
                            context.resources,
                            R.drawable.user_loc_small
                        )
                        val targetDp = 24f
                        val targetPx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            targetDp,
                            context.resources.displayMetrics
                        ).toInt()
                        val scaledBmp = Bitmap.createScaledBitmap(srcBmp, targetPx, targetPx, true)
                        val style = LabelStyle.from(scaledBmp)

                        // 4. 마커 추가
                        layer
                            ?.addLabel(
                                LabelOptions.from("cust_$id", pos)
                                    .setStyles(style)
                                    .setRank(10L)
                            )

                        // 5. 태그 리스트 분리
                        val tagsList = inputTags
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        // 6. Firestore 저장 —  id 필드 꼭 포함!
                        docRef.set(
                            mapOf(
                                "id"         to id,
                                "lat"        to pos.latitude,
                                "lng"        to pos.longitude,
                                "name"       to inputName,
                                "desc"       to inputDesc,
                                "tags"       to tagsList,
                                "priceRange" to inputPriceRange,
                                "hours"      to inputHours,
                                "rating"     to inputRating.toDouble(),
                                "userName"   to (FirebaseAuth.getInstance().currentUser?.displayName ?: "익명"),
                                "user"       to mapOf(
                                    "uid" to uid,
                                    "name" to (FirebaseAuth.getInstance().currentUser?.displayName ?: "익명")
                                )
                            )
                        )

                        // 7. 로컬 State에 저장
                        customMarkers["cust_$id"] = MarkerData(
                            name       = inputName,
                            desc       = inputDesc,
                            tags       = tagsList,
                            priceRange = inputPriceRange,
                            hours      = inputHours,
                            rating     = inputRating.toDouble(),
                            userName   = FirebaseAuth.getInstance().currentUser?.displayName ?: "익명",
                            lat        = pos.latitude,
                            lng        = pos.longitude
                        )

                        // 8. 입력값 초기화 & 다이얼로그 닫기
                        showAddDialog = false
                        inputDesc = ""
                        inputName = ""
                        inputTags = ""
                        inputPriceRange = ""
                        inputHours = ""
                        inputRating = 3f
                    },
                    enabled = allFilled
                ) {
                    Text("추가")
                }
            }
        )
    }



    // ⑦ “설명 보기” 다이얼로그
    if (showDescDialog && currentMarkerId != null) {
        val markerId = currentMarkerId!!
        when {
            markerId.startsWith("cust_") -> {
                customMarkers[markerId]?.let { md ->
                    AlertDialog(
                        onDismissRequest = { showDescDialog = false },
                        title = {
                            Text(
                                text = md.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 별점
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(5) { i ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i < md.rating.toInt()) Color(0xFFFFD700) else Color.Gray
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = String.format("%.1f", md.rating),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Divider()
                                Spacer(Modifier.height(12.dp))
                                // 간단 설명
                                Text(
                                    text = md.desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                // 태그
                                FlowRow(
                                    mainAxisSpacing = 8.dp,
                                    crossAxisSpacing = 4.dp,
                                ) {
                                    md.tags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = tag,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                // 가격대 · 영업시간
                                Row {
                                    Text(
                                        text = "💰 ${md.priceRange}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = "⏰ ${md.hours}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Divider()
                                Spacer(Modifier.height(12.dp))
                                // 등록자 · 위치
                                Text(
                                    text = "등록자: ${md.userName}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "위치: (%.5f, %.5f)".format(md.lat, md.lng),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // 찜하기 버튼
                                    TextButton(onClick = { /* 찜하기 구현 */ }) {
                                        Icon(Icons.Default.Favorite, contentDescription = "찜하기", tint = Color.Red)
                                        Spacer(Modifier.width(4.dp))
                                        Text("찜하기")
                                    }
                                    // 공유하기 버튼
                                    TextButton(onClick = { /* 공유 구현 */ }) {
                                        Icon(Icons.Default.Share, contentDescription = "공유")
                                        Spacer(Modifier.width(4.dp))
                                        Text("공유")
                                    }
                                    // 메뉴 보기 버튼
                                    TextButton(onClick = { /* 메뉴 보기 구현 */ }) {
                                        Icon(Icons.Default.MenuBook, contentDescription = "메뉴")
                                        Spacer(Modifier.width(4.dp))
                                        Text("메뉴")
                                    }
                                    // 후기 보기 버튼
                                    TextButton(onClick = { /* 후기 보기 구현 */ }) {
                                        Icon(Icons.Default.RateReview, contentDescription = "후기")
                                        Spacer(Modifier.width(4.dp))
                                        Text("후기")
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showDescDialog = false
                                currentMarkerId = null
                            }) {
                                Text("확인")
                            }
                        }
                    )
                }
            }
            markerId.startsWith("matzip_") -> {
                val matzipId = markerId.removePrefix("matzip_")
                val matzip = viewModel.allRestaurants.find { it.id == matzipId }
                if (matzip != null) {
                    AlertDialog(
                        onDismissRequest = { showDescDialog = false },
                        title = {
                            Text(
                                text = matzip.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (matzip.categoryGroupCode.isNotBlank() && matzip.categoryName.isNotBlank()) {
                                    // 카테고리명 변환: FD6 → 음식점 등
                                    val groupName = MatzipViewModel.CATEGORY_BUTTONS
                                        .firstOrNull { it.first == matzip.categoryGroupCode }
                                        ?.second ?: matzip.categoryGroupCode

                                    Text(
                                        text = "카테고리: $groupName > ${matzip.categoryName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2196F3) // 파란색 강조
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                if (matzip.desc.isNotBlank()) {
                                    Text(matzip.desc, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(4.dp))
                                }
                                if (matzip.tags.isNotEmpty()) {
                                    FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 4.dp) {
                                        matzip.tags.forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = tag,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                                if (matzip.phone.isNotBlank()) {
                                    Text("전화: ${matzip.phone}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (matzip.address.isNotBlank()) {
                                    Text("지번: ${matzip.address}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (matzip.roadAddress.isNotBlank()) {
                                    Text("도로명: ${matzip.roadAddress}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    if (matzip.priceRange.isNotBlank()) {
                                        Text("💰 ${matzip.priceRange}", style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    if (matzip.hours.isNotBlank()) {
                                        Text("⏰ ${matzip.hours}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(5) { i ->
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i < matzip.rating.toInt()) Color(0xFFFFD700) else Color.Gray
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(String.format("%.1f", matzip.rating), style = MaterialTheme.typography.bodyMedium)
                                }
                                if (matzip.placeUrl.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(matzip.placeUrl))
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = "지도보기")
                                        Spacer(Modifier.width(4.dp))
                                        Text("카카오맵에서 보기")
                                    }
                                }
                                Divider(Modifier.padding(vertical = 8.dp))
                                Text("등록자: ${matzip.userName}", style = MaterialTheme.typography.labelLarge)
                                Text("위치: (%.5f, %.5f)".format(matzip.lat, matzip.lng), style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showDescDialog = false
                                currentMarkerId = null
                            }) { Text("확인") }
                        }
                    )
                }
            }

        }
    }
}

@Composable
fun RestaurantMapWithDrawerAndFab(viewModel: MatzipViewModel = viewModel()) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        viewModel.fetchAllCategories()
        Log.d("디버그", "맵 화면 첫 진입: 카카오 fetchAndSaveMatzipFromKakao 자동 호출")
        viewModel.fetchAndSaveMatzipFromKakao("맛집") {
            Log.d("디버그", "카카오 호출 후 콜백(맵 첫 진입)")
        }
    }
    ModalNavigationDrawer(
        drawerContent = {
            DrawerContent(
                favoriteList = viewModel.favoriteRestaurants,
                onSearch = {
                    Log.d("디버그", "onSearch 호출됨: $it")
                    searchQuery = it
                    viewModel.searchRestaurants(it)
                    // "카카오에서 신규 가게 받아오기" 예시
                    if (it.isNotBlank()) {
                        Log.d("디버그", "카카오 fetchAndSaveMatzipFromKakao 호출!")
                        viewModel.fetchAndSaveMatzipFromKakao(it) {
                            Log.d("디버그", "카카오 호출 후 콜백")
                        }
                    }
                },
                nearbyList = viewModel.nearbyRestaurants,
                searchResults = viewModel.searchResults
            )
        },
        drawerState = drawerState
    ) {
        Box(Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = { scope.launch { drawerState.open() } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 24.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "메뉴")
            }
            RestaurantV2MapScreen(viewModel = viewModel)
        }
    }
}


@Composable
fun DrawerContent(
    favoriteList: List<MatzipData>,
    onSearch: (String) -> Unit,
    nearbyList: List<MatzipData>,
    searchResults: List<MatzipData>,
    onItemClick: (MatzipData) -> Unit = {}
)  {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .padding(20.dp)
    ) {
        // --- 검색창 ---
        Text(
            text = "맛집 탐색",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearch(it)
            },
            placeholder = { Text("이름/태그/설명 검색") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))

        // --- 검색 결과 표시 ---
        if (searchQuery.isNotBlank()) {
            Text("🔍 검색 결과", style = MaterialTheme.typography.titleMedium)
            if (searchResults.isEmpty()) {
                Text("검색 결과가 없습니다.", color = Color.Gray)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 120.dp)
                        .fillMaxWidth()
                ) {
                    items(searchResults) { matzip ->
                        DrawerRestaurantItem(matzip, onItemClick)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // --- 찜한 맛집 ---
        Text("⭐ 찜한 맛집", style = MaterialTheme.typography.titleMedium)
        if (favoriteList.isEmpty()) {
            Text("아직 찜한 맛집이 없어요.", color = Color.Gray)
        } else {
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 120.dp)
                    .fillMaxWidth()
            ) {
                items(favoriteList) { matzip ->
                    DrawerRestaurantItem(matzip, onItemClick)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // --- 주변 맛집 ---
        Text("📍 주변 맛집", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 120.dp)
                .fillMaxWidth()
        ) {
            items(nearbyList) { matzip ->
                DrawerRestaurantItem(matzip, onItemClick)
            }
        }

        Spacer(Modifier.weight(1f))
        Divider(Modifier.padding(vertical = 12.dp))
        Text("FoodTable v1.0", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }
}

// 맛집 리스트 아이템 컴포저블
@Composable
fun DrawerRestaurantItem(
    matzip: MatzipData,
    onClick: (MatzipData) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(matzip) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(matzip.name, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                matzip.tags.take(2).forEach { tag ->
                    Text("#$tag ", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(6.dp))
                Text("${matzip.rating}★", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// --- 샘플 데이터 (실제 구현 시 ViewModel에서 가져오세요!) ---
val sampleFavorites = listOf(
    MatzipData(
        id = "1",
        name = "감성라멘",
        desc = "진한 국물 맛집",
        tags = listOf("라멘", "일식"),
        priceRange = "₩₩",
        hours = "11:00-21:00",
        rating = 4.7,
        userName = "유저1",
        lat = 37.0, lng = 127.0
    )
)
val sampleNearby = listOf(
    MatzipData(
        id = "2",
        name = "고기굽는집",
        desc = "숯불구이 전문",
        tags = listOf("고기", "한식"),
        priceRange = "₩₩₩",
        hours = "16:00-22:00",
        rating = 4.3,
        userName = "유저2",
        lat = 37.0, lng = 127.01
    ),
    MatzipData(
        id = "3",
        name = "미미분식",
        desc = "떡볶이, 튀김, 김밥",
        tags = listOf("분식", "떡볶이"),
        priceRange = "₩",
        hours = "09:00-20:00",
        rating = 4.1,
        userName = "유저3",
        lat = 37.01, lng = 127.0
    )
)


// Float 소수 자리 포맷 헬퍼
private fun Float.format(digits: Int) = "%.${digits}f".format(this)

//카테고리 선택바
@Composable
fun CategorySelectorBar(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MatzipViewModel.CATEGORY_BUTTONS.forEach { (code, label) ->
            Button(
                onClick = { onSelect(code) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == code) Color(0xFFFFC107) else Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(label, color = if (selected == code) Color.Black else Color.DarkGray)
            }
        }
    }
}
// 하위 카테고리 선택바
@Composable
fun SubCategoryChips(
    subCategories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        item {
            AssistChip(
                onClick = { onSelect("") },
                label = { Text("전체") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected.isBlank()) Color(0xFFFFF59D) else Color(0xFFF0F0F0)
                ),
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        items(subCategories) { subCat ->
            AssistChip(
                onClick = { onSelect(subCat) },
                label = { Text(subCat) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected == subCat) Color(0xFFFFF59D) else Color(0xFFF0F0F0)
                ),
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}
