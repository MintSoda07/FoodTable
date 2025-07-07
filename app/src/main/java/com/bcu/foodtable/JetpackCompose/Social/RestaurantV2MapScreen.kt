package com.bcu.foodtable.JetpackCompose.Social

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import androidx.tv.material3.AssistChip
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
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import kotlinx.coroutines.launch

private const val TAG = "RestaurantV2Map"
//
//// ① 클러스터 대상 아이템
//private data class ClusterItem(
//    val id: String,
//    val position: LatLng,
//    val data: Any? = null   // 추가 정보가 필요하면 MarkerData 로 바꿔도 됩니다
//)
//
//// ② 아주 간단한 ClusterManager
//private class ClusterManager(
//    private val map: KakaoMap,
//    private val layer: com.kakao.vectormap.label.LabelManager.Layer,
//    private val context: Context
//) {
//    private val items = mutableListOf<ClusterItem>()
//    private val rendered = mutableListOf<com.kakao.vectormap.label.Label>()
//
//    /** 클러스터 아이템 등록 */
//    fun addItem(item: ClusterItem) {
//        items += item
//    }
//
//    /** 클러스터링 수행 */
//    fun cluster() {
//        // 이전에 그린 라벨 제거
//        rendered.forEach { layer.removeLabel(it) }
//        rendered.clear()
//
//        val zoom = map.cameraPosition.zoomLevel
//        // zoom 레벨마다 그리드 크기(px) 결정
//        val gridSize = when {
//            zoom >= 15 -> 100
//            zoom >= 12 -> 200
//            else       -> 400
//        }
//
//        // 화면 좌표로 변환
//        val coordMap = items.map { it to map.projection.toScreenLocation(it.position) }
//
//        // 그리드 키별 그룹핑
//        val clusters = mutableMapOf<Pair<Int,Int>, MutableList<ClusterItem>>()
//        coordMap.forEach { (item, pt) ->
//            val key = (pt.x / gridSize) to (pt.y / gridSize)
//            clusters.getOrPut(key) { mutableListOf() } += item
//        }
//
//        // 그룹별 렌더링
//        clusters.values.forEach { group ->
//            if (group.size == 1) {
//                // 단일 마커
//                val ci = group[0]
//                val opts = LabelOptions.from(ci.id, ci.position)
//                    .setStyles(R.drawable.user_loc_small)
//                    .setRank(10L)
//                layer.addLabel(opts)?.let { rendered += it }
//            } else {
//                // 클러스터 노드: 그룹 중심에 카운트 표시
//                val avgLat = group.map { it.position.latitude }.average()
//                val avgLng = group.map { it.position.longitude }.average()
//                val pos = LatLng.from(avgLat, avgLng)
//
//                // 숫자를 그린 비트맵 생성
//                val text = group.size.toString()
//                val paint = android.graphics.Paint().apply {
//                    color = android.graphics.Color.WHITE
//                    textAlign = android.graphics.Paint.Align.CENTER
//                    textSize = 48f * context.resources.displayMetrics.density
//                    isAntiAlias = true
//                }
//                val size = 80  // 픽셀
//                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
//                val canvas = Canvas(bmp)
//                canvas.drawCircle((size/2).toFloat(), (size/2).toFloat(), size/2f, android.graphics.Paint().apply {
//                    color = android.graphics.Color.parseColor("#FF5722")
//                    isAntiAlias = true
//                })
//                canvas.drawText(text, size/2f, size/2f - (paint.descent()+paint.ascent())/2, paint)
//
//                val style = LabelStyle.from(bmp)
//                val opts = LabelOptions.from("cluster_${group.hashCode()}", pos)
//                    .setStyles(style)
//                    .setRank(5L)
//                layer.addLabel(opts)?.let { rendered += it }
//            }
//        }
//    }
//}
//// ① 클러스터 대상 아이템
//private data class ClusterItem(
//    val id: String,
//    val position: LatLng,
//    val data: Any? = null   // 추가 정보가 필요하면 MarkerData 로 바꿔도 됩니다
//)
//
//// ② 아주 간단한 ClusterManager
//private class ClusterManager(
//    private val map: KakaoMap,
//    private val layer: com.kakao.vectormap.label.LabelManager.Layer,
//    private val context: Context
//) {
//    private val items = mutableListOf<ClusterItem>()
//    private val rendered = mutableListOf<com.kakao.vectormap.label.Label>()
//
//    /** 클러스터 아이템 등록 */
//    fun addItem(item: ClusterItem) {
//        items += item
//    }
//
//    /** 클러스터링 수행 */
//    fun cluster() {
//        // 이전에 그린 라벨 제거
//        rendered.forEach { layer.removeLabel(it) }
//        rendered.clear()
//
//        val zoom = map.cameraPosition.zoomLevel
//        // zoom 레벨마다 그리드 크기(px) 결정
//        val gridSize = when {
//            zoom >= 15 -> 100
//            zoom >= 12 -> 200
//            else       -> 400
//        }
//
//        // 화면 좌표로 변환
//        val coordMap = items.map { it to map.projection.toScreenLocation(it.position) }
//
//        // 그리드 키별 그룹핑
//        val clusters = mutableMapOf<Pair<Int,Int>, MutableList<ClusterItem>>()
//        coordMap.forEach { (item, pt) ->
//            val key = (pt.x / gridSize) to (pt.y / gridSize)
//            clusters.getOrPut(key) { mutableListOf() } += item
//        }
//
//        // 그룹별 렌더링
//        clusters.values.forEach { group ->
//            if (group.size == 1) {
//                // 단일 마커
//                val ci = group[0]
//                val opts = LabelOptions.from(ci.id, ci.position)
//                    .setStyles(R.drawable.user_loc_small)
//                    .setRank(10L)
//                layer.addLabel(opts)?.let { rendered += it }
//            } else {
//                // 클러스터 노드: 그룹 중심에 카운트 표시
//                val avgLat = group.map { it.position.latitude }.average()
//                val avgLng = group.map { it.position.longitude }.average()
//                val pos = LatLng.from(avgLat, avgLng)
//
//                // 숫자를 그린 비트맵 생성
//                val text = group.size.toString()
//                val paint = android.graphics.Paint().apply {
//                    color = android.graphics.Color.WHITE
//                    textAlign = android.graphics.Paint.Align.CENTER
//                    textSize = 48f * context.resources.displayMetrics.density
//                    isAntiAlias = true
//                }
//                val size = 80  // 픽셀
//                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
//                val canvas = Canvas(bmp)
//                canvas.drawCircle((size/2).toFloat(), (size/2).toFloat(), size/2f, android.graphics.Paint().apply {
//                    color = android.graphics.Color.parseColor("#FF5722")
//                    isAntiAlias = true
//                })
//                canvas.drawText(text, size/2f, size/2f - (paint.descent()+paint.ascent())/2, paint)
//
//                val style = LabelStyle.from(bmp)
//                val opts = LabelOptions.from("cluster_${group.hashCode()}", pos)
//                    .setStyles(style)
//                    .setRank(5L)
//                layer.addLabel(opts)?.let { rendered += it }
//            }
//        }
//    }
//}



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
                                // ③ 지도 준비 직후
                                mapView.resume()
                                kakaoMap = mapInstance
                                val layer = mapInstance.labelManager?.layer

                                // ▶ 여기에 클러스터 매니저 생성
//                                val clusterManager = ClusterManager(mapInstance, layer)

                                // ④ (가) Firestore에서 기존 마커 불러오기
                                FirebaseFirestore.getInstance()
                                    .collection("custom_markers")
                                    .get()
                                    .addOnSuccessListener { snap ->
                                        snap.documents.forEach { doc ->
                                            val id         = doc.id
                                            val lat        = doc.getDouble("lat")       ?: return@forEach
                                            val lng        = doc.getDouble("lng")       ?: return@forEach
                                            val name       = doc.getString("name")      ?: ""
                                            val desc       = doc.getString("desc")      ?: ""
                                            val tagsList   = doc.get("tags") as? List<*> ?: emptyList<Any>()
                                            val priceRange = doc.getString("priceRange") ?: ""
                                            val hours      = doc.getString("hours")     ?: ""
                                            val rating     = doc.getDouble("rating")    ?: 0.0
                                            val userMap    = doc.get("user") as? Map<*,*>
                                            val userName   = userMap?.get("name") as? String ?: "익명"

                                            // 위치 객체
                                            val pos = LatLng.from(lat, lng)

                                            // 아이콘 스케일링 (기존 코드 그대로)
                                            val srcBmp = BitmapFactory.decodeResource(context.resources, R.drawable.user_loc_small)
                                            val targetDp = 24f
                                            val targetPx = TypedValue.applyDimension(
                                                TypedValue.COMPLEX_UNIT_DIP,
                                                targetDp,
                                                context.resources.displayMetrics
                                            ).toInt()
                                            val scaledBmp = Bitmap.createScaledBitmap(srcBmp, targetPx, targetPx, true)
                                            val style = LabelStyle.from(scaledBmp)

                                            // 지도에 라벨 추가
                                            val opts = LabelOptions.from("cust_$id", pos)
                                                .setStyles(style)
                                                .setRank(10L)
                                            layer?.addLabel(opts)

                                            // ② customMarkers 에 MarkerData 저장
                                            customMarkers["cust_$id"] = MarkerData(
                                                name, desc,
                                                tagsList.map { it.toString() },
                                                priceRange, hours,
                                                rating, userName,
                                                lat, lng
                                            )
                                        }
                                        isLoaded = true
                                    }

                                // ④ (나) 지도 터치로 새 마커 추가
                                mapInstance.setOnMapClickListener { _, position, _, _ ->
                                    newPos = position
                                    showAddDialog = true
                                    false  // 클릭 이벤트 소비
                                }


                                // (다) 라벨(마커)을 탭했을 때 설명 보기
                                mapInstance.setOnLabelClickListener { _, _, label ->
                                    if (!isLoaded) return@setOnLabelClickListener false
                                    currentMarkerId = label.getLabelId()
                                    showDescDialog = true
                                    true
                                }
                                // 1) POI 추가

                                Log.d(TAG, "layer null? ${layer == null}")
                                viewModel.restaurants.forEachIndexed { idx, rest ->
                                    val p = LatLng.from(rest.latitude, rest.longitude)
                                    val opts = LabelOptions.from("rest_$idx", p)
                                        .setStyles(R.drawable.baseline_restaurant_menu_24)
                                        .setRank(idx.toLong())
                                    layer?.addLabel(opts)
                                    Log.d(TAG, "added user label: $userLocationLabel")
                                }

                                // 2) 현위치 트래킹
                                val fusedClient = LocationServices
                                    .getFusedLocationProviderClient(context)
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
                                        // 1) 원본 비트맵 로드
                                        val srcBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.user_loc)

                                        // 2) 원하는 크기(px) 계산 (예: 가로/세로 48dp → px)
                                        val sizeDp = 24f
                                        val sizePx = TypedValue.applyDimension(
                                            TypedValue.COMPLEX_UNIT_DIP, sizeDp, context.resources.displayMetrics
                                        ).toInt()

                                        // 3) 비트맵 스케일
                                        val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, sizePx, sizePx, true)

                                        // 4) LabelStyle 생성
                                        val style = LabelStyle.from(scaledBitmap)

                                        // 5) LabelOptions 에 적용
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

                                // 권한 체크
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
    // ⑥ “맛집 설명 입력” 다이얼로그
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
                //필수 입력 값 검사
                val allFilled =
                    inputName.isNotBlank() &&
                            inputDesc.isNotBlank() &&
                            inputPriceRange.isNotBlank() &&
                            inputHours.isNotBlank()

                TextButton(onClick = {
                    //  Firestore 도큐먼트 ID 준비
                    val docRef = FirebaseFirestore.getInstance()
                        .collection("custom_markers")
                        .document()
                    val id = docRef.id
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@TextButton

                    // 2새로운 위치(newPos!!)와 context, layer 가져오기
                    val pos = newPos!!
                    val layer = kakaoMap?.labelManager?.layer

                    //  런타임에 Bitmap 스케일링
                    val srcBmp = BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.user_loc_small
                    )
                    // 원하는 dp 크기
                    val targetDp = 24f
                    val targetPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        targetDp,
                        context.resources.displayMetrics
                    ).toInt()
                    val scaledBmp = Bitmap.createScaledBitmap(srcBmp, targetPx, targetPx, true)
                    val style = LabelStyle.from(scaledBmp)

                    //  마커 추가
                    layer
                        ?.addLabel(
                            LabelOptions.from("cust_$id", pos)
                                .setStyles(style)
                                .setRank(10L)
                        )

                    //  현재 사용자 이름 가져오기
                    val user = FirebaseAuth.getInstance().currentUser


                    //  로컬 State에 저장
                    customDesc["cust_$id"] = inputDesc


                    // ① 태그 리스트 분리
                    val tagsList = inputTags
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    // ② Firestore 저장
                    docRef.set(mapOf(
                        "lat"         to pos.latitude,
                        "lng"         to pos.longitude,
                        "name"        to inputName,
                        "desc"        to inputDesc,
                        "tags"        to tagsList,
                        "priceRange"  to inputPriceRange,
                        "hours"       to inputHours,
                        "rating"      to inputRating.toDouble(),
                        "user"        to mapOf("uid" to uid, "name" to (FirebaseAuth.getInstance().currentUser?.displayName ?: "익명"))
                    ))
                    // 새 MarkerData 를 곧바로 메모리 맵에도 추가
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
                    // 다이얼로그 닫기
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
        customMarkers[currentMarkerId!!]?.let { md ->

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
                                currentMarkerId = null }) {
                                Text("확인")
                            }
                        }
                    )
                }


        }
    }

@Composable
fun RestaurantMapWithDrawerAndFab(viewModel: MatzipViewModel = viewModel()) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            DrawerContent(
                favoriteList = viewModel.favoriteRestaurants,
                onSearch = { viewModel.searchRestaurants(it) },
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

