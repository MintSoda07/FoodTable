package com.bcu.foodtable.JetpackCompose.Social

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
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
import com.google.accompanist.permissions.*
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

private const val TAG = "RestaurantMapScreen"

@Composable
fun RestaurantMapWithCustomDrawer(viewModel: MatzipViewModel = viewModel()) {
    // drawer 오픈 상태 State
    var drawerOpened by remember { mutableStateOf(false) }

    // 최초 데이터 로딩
    LaunchedEffect(Unit) {
        viewModel.loadCustomMarkers()
        viewModel.loadFavoriteRestaurants()
    }

    Box(Modifier.fillMaxSize()) {
        // 지도 always 아래 깔림
        RestaurantKakaoMap(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel
        )

        // drawer 오픈 버튼 (오른쪽 하단/상단 등)
        FloatingActionButton(
            onClick = { drawerOpened = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 24.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "메뉴")
        }

        // 커스텀 drawer & overlay
        if (drawerOpened) {
            // 1. 검은색 반투명 오버레이 (drawer 바깥)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        drawerOpened = false // 오버레이 클릭하면 닫힘!
                    }
            )

            // 2. drawer 패널 (오른쪽에서 슬라이드됨)
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .align(Alignment.CenterEnd),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
            ) {
                Column {
                    // 상단 닫기 버튼
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(end = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { drawerOpened = false }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = Color.Black
                            )
                        }
                    }
                    // drawer 내용
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp)
                    ) {
                        DrawerContent(
                            favoriteList = viewModel.favoriteRestaurants.map { place ->
                                CustomMarkerData(
                                    id = place.id,
                                    name = place.place_name,
                                    desc = place.category_name ?: "",
                                    tags = place.category_name?.let { listOf(it) } ?: emptyList(),
                                    lat = place.y.toDoubleOrNull() ?: 0.0,
                                    lng = place.x.toDoubleOrNull() ?: 0.0
                                )
                            },
                            onSearch = { query ->
                                if (query.isNotBlank()) {
                                    viewModel.fetchRestaurantsFromKakao(
                                        centerLat = 37.554722,
                                        centerLng = 126.970833,
                                        keyword = query
                                    )
                                }
                            },
                            nearbyList = viewModel.customMarkers,
                            searchResults = viewModel.visibleRestaurants.map { place ->
                                CustomMarkerData(
                                    id = place.id,
                                    name = place.place_name,
                                    desc = place.category_name ?: "",
                                    tags = place.category_name?.let { listOf(it) } ?: emptyList(),
                                    lat = place.y.toDoubleOrNull() ?: 0.0,
                                    lng = place.x.toDoubleOrNull() ?: 0.0
                                )
                            },
                            // drawer 내부 리스트 클릭시에도 닫기 원하면 아래처럼
                            onItemClicked = { drawerOpened = false }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RestaurantMapMainScreen(
    viewModel: MatzipViewModel = viewModel()
) {
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var drawerOpened by remember { mutableStateOf(false) }

    // 위치 권한 요청
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    if (!permissionState.status.isGranted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("위치 권한이 필요합니다.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        // 지도
        RestaurantKakaoMap(
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel
        )

        // 햄버거(메뉴) 버튼 - 왼쪽 위!
        FloatingActionButton(
            onClick = { drawerOpened = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 24.dp, start = 24.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "메뉴")
        }

        // 검은 오버레이 (drawer 열렸을 때만)
        if (drawerOpened) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { drawerOpened = false }
            )
        }

        // ✨ 왼쪽 → 오른쪽 슬라이드 Drawer (부드러운 애니)
        AnimatedVisibility(
            visible = drawerOpened,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth },  // 왼쪽에서 등장
                animationSpec = tween(durationMillis = 320)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },  // 왼쪽으로 퇴장
                animationSpec = tween(durationMillis = 250)
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .align(Alignment.CenterStart),   // ← 왼쪽 정렬!
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Column {
                    // 닫기 버튼 (오른쪽 상단)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(end = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { drawerOpened = false }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = Color.Black
                            )
                        }
                    }
                    // Drawer 내용
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 8.dp)
                    ) {
                        DrawerContent(
                            favoriteList = viewModel.favoriteRestaurants.map { place ->
                                CustomMarkerData(
                                    id = place.id,
                                    name = place.place_name,
                                    desc = place.category_name ?: "",
                                    tags = place.category_name?.let { listOf(it) } ?: emptyList(),
                                    lat = place.y.toDoubleOrNull() ?: 0.0,
                                    lng = place.x.toDoubleOrNull() ?: 0.0
                                )
                            },
                            onSearch = { query ->
                                if (query.isNotBlank()) {
                                    viewModel.fetchRestaurantsFromKakao(
                                        centerLat = 37.554722,
                                        centerLng = 126.970833,
                                        keyword = query
                                    )
                                }
                            },
                            nearbyList = viewModel.customMarkers,
                            searchResults = viewModel.visibleRestaurants.map { place ->
                                CustomMarkerData(
                                    id = place.id,
                                    name = place.place_name,
                                    desc = place.category_name ?: "",
                                    tags = place.category_name?.let { listOf(it) } ?: emptyList(),
                                    lat = place.y.toDoubleOrNull() ?: 0.0,
                                    lng = place.x.toDoubleOrNull() ?: 0.0
                                )
                            },
                            onItemClicked = { drawerOpened = false }
                        )
                    }
                }
            }
        }
    }
}

// 지도, 마커, 트래킹, 마커 클릭 상세보기 다 여기에!
@Composable
fun RestaurantKakaoMap(
    modifier: Modifier = Modifier,
    viewModel: MatzipViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. State
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var isLoaded by remember { mutableStateOf(false) }
    val customMarkers = remember { mutableStateMapOf<String, CustomMarkerData>() }
    var userLocationLatLng by remember { mutableStateOf(LatLng.from(37.554722, 126.970833)) }
    var trackingEnabled by remember { mutableStateOf(true) }
    var userLocationLabel by remember { mutableStateOf<Label?>(null) }
    var selectedPlace by remember { mutableStateOf<KakaoPlace?>(null) }
    var selectedCustomMarker by remember { mutableStateOf<CustomMarkerData?>(null) }

    // 2. custom_markers Firestore fetch 1회
    LaunchedEffect(Unit) {
        customMarkers.clear()
        FirebaseFirestore.getInstance()
            .collection("custom_markers")
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { doc ->
                    doc.toObject(CustomMarkerData::class.java)?.let { marker ->
                        customMarkers["cust_${marker.id}"] = marker
                    }
                }
            }
    }

    // 3. 지도 컴포넌트
    val mapView = remember {
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    KakaoMapSdk.init(context, "b0402134c35d48a50145988a7727b74b")
                    mapView.start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {
                                // 필요하다면 정리 코드 작t성
                            }
                            override fun onMapError(e: Exception?) {
                                Log.e(TAG, "KakaoMap Error", e)
                            }
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(map: KakaoMap) {
                                kakaoMap = map
                                isLoaded = true

                                val layer = map.labelManager?.layer

                                // 현위치 마커 추가
                                fun updateUserMarker(userPos: LatLng) {
                                    val srcBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.user_loc)
                                    val sizePx = TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_DIP, 24f, context.resources.displayMetrics
                                    ).toInt()
                                    val scaledBitmap = Bitmap.createScaledBitmap(srcBitmap, sizePx, sizePx, true)
                                    val style = LabelStyle.from(scaledBitmap)
                                    val opts = LabelOptions.from("user_loc", userPos)
                                        .setStyles(style)
                                        .setRank(10000L)
                                    userLocationLabel = layer?.addLabel(opts)
                                }

                                // 현위치 트래킹
                                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                                val req = LocationRequest.create().apply {
                                    interval = 5000L
                                    fastestInterval = 2000L
                                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                                }
                                val cb = object : LocationCallback() {
                                    override fun onLocationResult(result: LocationResult) {
                                        val loc = result.lastLocation ?: return
                                        val pos = LatLng.from(loc.latitude, loc.longitude)
                                        userLocationLatLng = pos
                                        if (userLocationLabel == null) {
                                            updateUserMarker(pos)
                                        } else {
                                            userLocationLabel?.moveTo(pos)
                                        }
                                        if (trackingEnabled) {
                                            kakaoMap?.moveCamera(
                                                CameraUpdateFactory.newCenterPosition(pos),
                                                CameraAnimation.from(500, true, true)
                                            )
                                            trackingEnabled = false
                                        }
                                    }
                                }
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    try {
                                        fusedClient.requestLocationUpdates(req, cb, Looper.getMainLooper())
                                    } catch (_: Exception) { }
                                }

                                // 최초 fetch (지도 중심 음식점)
                                val center = map.cameraPosition?.getPosition() ?: userLocationLatLng
                                viewModel.fetchRestaurantsFromKakao(center.latitude, center.longitude)

                                // 마커 표시 함수
                                fun showMarkers() {
                                    layer?.removeAll()
                                    // 현위치 마커
                                    updateUserMarker(userLocationLatLng)
                                    // 1. 카카오 맛집 마커 (visibleRestaurants)
                                    viewModel.visibleRestaurants.forEach { place ->
                                        val pos = LatLng.from(place.y.toDoubleOrNull() ?: 0.0, place.x.toDoubleOrNull() ?: 0.0)
                                        val opts = LabelOptions.from("matzip_${place.id}", pos)
                                            .setStyles(R.drawable.user_loc_small)
                                            .setRank(10L)
                                        layer?.addLabel(opts)
                                    }
                                    // 2. custom_markers
                                    customMarkers.values.forEach { marker ->
                                        val pos = LatLng.from(marker.lat, marker.lng)
                                        val opts = LabelOptions.from("cust_${marker.id}", pos)
                                            .setStyles(R.drawable.user_loc_small)
                                            .setRank(20L)
                                        layer?.addLabel(opts)
                                    }
                                }
                                showMarkers()

                                // 카메라 이동 시 카카오맵 API로 음식점 fetch + 마커 갱신
                                map.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                                    val center = cameraPosition?.getPosition() ?: userLocationLatLng
                                    viewModel.fetchRestaurantsFromKakao(center.latitude, center.longitude)
                                    showMarkers()
                                }

                                // 마커 클릭 시 상세 다이얼로그 표시
                                map.setOnLabelClickListener { _, _, label ->
                                    val id = label.labelId
                                    if (id.startsWith("matzip_")) {
                                        val placeId = id.removePrefix("matzip_")
                                        val place = viewModel.visibleRestaurants.find { it.id == placeId }
                                        if (place != null) {
                                            selectedPlace = place
                                            selectedCustomMarker = null
                                        }
                                    } else if (id.startsWith("cust_")) {
                                        selectedCustomMarker = customMarkers[id]
                                        selectedPlace = null
                                    }
                                    true
                                }
                            }
                        }
                    )
                }
                Lifecycle.Event.ON_START -> mapView.resume()
                Lifecycle.Event.ON_STOP -> mapView.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(factory = { mapView }, modifier = modifier)

    // 상세 다이얼로그
    selectedPlace?.let { place ->
        KakaoPlaceDetailDialog(
            place = place,
            onClose = { selectedPlace = null },
            onFavorite = { viewModel.saveRestaurantToFavorites(place) }
        )
    }
    selectedCustomMarker?.let { marker ->
        CustomMarkerDetailDialog(
            marker = marker,
            onClose = { selectedCustomMarker = null }
        )
    }
}

// 카카오 API place 상세 다이얼로그
@Composable
fun KakaoPlaceDetailDialog(
    place: KakaoPlace,
    onClose: () -> Unit,
    onFavorite: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(place.place_name, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                if (!place.category_name.isNullOrBlank()) {
                    Text(place.category_name!!, style = MaterialTheme.typography.bodySmall)
                }
                if (!place.address_name.isNullOrBlank()) {
                    Text(place.address_name, style = MaterialTheme.typography.bodySmall)
                }
                if (!place.phone.isNullOrBlank()) {
                    Text("☎ ${place.phone}", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    if (!place.road_address_name.isNullOrBlank()) {
                        Text(place.road_address_name, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onFavorite) {
                        Icon(Icons.Default.Favorite, contentDescription = "찜하기", tint = Color.Red)
                        Spacer(Modifier.width(4.dp))
                        Text("찜하기")
                    }
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(place.place_url))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Map, contentDescription = "지도")
                        Spacer(Modifier.width(4.dp))
                        Text("카카오맵")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("확인") } }
    )
}

// 커스텀 마커 상세 다이얼로그
@Composable
fun CustomMarkerDetailDialog(
    marker: CustomMarkerData,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(marker.name, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                if (marker.desc.isNotBlank()) {
                    Text(marker.desc, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { i ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i < marker.rating.toInt()) Color(0xFFFFD700) else Color.Gray
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("%.1f".format(marker.rating), style = MaterialTheme.typography.bodyMedium)
                }
                Text("위치: (%.5f, %.5f)".format(marker.lat, marker.lng), style = MaterialTheme.typography.bodySmall)
                Text("등록자: ${marker.userName}", style = MaterialTheme.typography.labelLarge)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("확인") } }
    )
}
@Composable
fun DrawerContent(
    favoriteList: List<CustomMarkerData>,
    onSearch: (String) -> Unit,
    nearbyList: List<CustomMarkerData>,
    searchResults: List<CustomMarkerData>,
    onItemClicked: () -> Unit   // ← 추가!
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .padding(16.dp)
    ) {
        Text("찜한 맛집", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (favoriteList.isEmpty()) {
            Text("아직 찜한 맛집이 없습니다.", color = Color.Gray)
        } else {
            favoriteList.forEach { marker ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Log.d("DrawerDebug", "Row 클릭됨!")
                            onItemClicked() } // ← 여기!
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(marker.name, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            marker.tags.take(2).forEach { tag ->
                                Text("#$tag ", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("${marker.rating}★", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("주변 맛집", style = MaterialTheme.typography.titleLarge)
        if (nearbyList.isEmpty()) {
            Text("주변 맛집이 없습니다.", color = Color.Gray)
        } else {
            nearbyList.forEach { marker ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClicked() } // ← 여기!
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(marker.name, style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            marker.tags.take(2).forEach { tag ->
                                Text("#$tag ", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("${marker.rating}★", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

data class CustomMarkerData(
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
