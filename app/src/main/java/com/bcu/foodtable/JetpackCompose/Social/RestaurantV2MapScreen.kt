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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bcu.foodtable.R
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

private const val TAG = "RestaurantV2Map"

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


                                // ④ (가) Firestore에서 기존 마커 불러오기
                                FirebaseFirestore.getInstance()
                                    .collection("custom_markers")
                                    .get()
                                    .addOnSuccessListener { snap ->
                                        snap.documents.forEach { doc ->
                                            val id   = doc.id
                                            val lat  = doc.getDouble("lat") ?: return@forEach
                                            val lng  = doc.getDouble("lng") ?: return@forEach
                                            val desc = doc.getString("desc") ?: ""
                                            val pos  = LatLng.from(lat, lng)

                                            // 1) 원본 Bitmap 로드
                                            val srcBmp = BitmapFactory.decodeResource(
                                                context.resources,
                                                R.drawable.user_loc_small
                                            )
                                            // 2) 원하는 크기(dp) → px로 변환
                                            val targetDp = 24f  // 예: 32dp 크기
                                            val targetPx = TypedValue.applyDimension(
                                                TypedValue.COMPLEX_UNIT_DIP,
                                                targetDp,
                                                context.resources.displayMetrics
                                            ).toInt()
                                            // 3) Bitmap 스케일
                                            val scaledBmp = Bitmap.createScaledBitmap(srcBmp, targetPx, targetPx, true)
                                            // 4) LabelStyle 생성
                                            val style = LabelStyle.from(scaledBmp)
                                            // ────────────────────────
                                            // Label 추가
                                            val opts = LabelOptions.from("cust_$id", pos)
                                                .setStyles(style)
                                                .setRank(10L)
                                            layer?.addLabel(opts)
                                            customDesc["cust_$id"] = desc
                                        }
                                    }

                                // ④ (나) 지도 터치로 새 마커 추가
                                mapInstance.setOnMapClickListener { _, position, _, _ ->
                                    newPos = position
                                    showAddDialog = true
                                }

                                // ④ (다) 라벨 클릭 시 설명 보기
                                mapInstance.setOnLabelClickListener { _, layer, label ->
                                    // 1) 클릭된 Label의 ID 얻기
                                    val labelId = label.getLabelId()
                                    // 2) Description이 있으면 대화상자 띄우기
                                    customDesc[labelId]?.let { text ->
                                        currentDesc = text
                                        showDescDialog = true
                                    }
                                    // 3) 이벤트 소비했음을 알리기 위해 Boolean 반환
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
                    Text("설명을 입력하세요:")
                    TextField(
                        value = inputDesc,
                        onValueChange = { inputDesc = it },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 1) Firestore 도큐먼트 ID 준비
                    val docRef = FirebaseFirestore.getInstance()
                        .collection("custom_markers")
                        .document()
                    val id = docRef.id
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@TextButton

                    // 2) 새로운 위치(newPos!!)와 context, layer 가져오기
                    val pos = newPos!!
                    val layer = kakaoMap?.labelManager?.layer

                    // 3) 런타임에 Bitmap 스케일링
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

                    // 4) 마커 추가
                    layer
                        ?.addLabel(
                            LabelOptions.from("cust_$id", pos)
                                .setStyles(style)
                                .setRank(10L)
                        )

                    // 5) 현재 사용자 이름 가져오기
                    val user = FirebaseAuth.getInstance().currentUser


                    // 5) 로컬 State에 저장
                    customDesc["cust_$id"] = inputDesc

                    // 6) Firestore에 저장
                    docRef.set(mapOf(
                        "lat"  to pos.latitude,
                        "lng"  to pos.longitude,
                        "desc" to inputDesc,
                        "user" to mapOf( "uid" to uid)
                    ))

                    // 7) 다이얼로그 닫기
                    showAddDialog = false
                    inputDesc = ""
                }) {
                    Text("추가")
                }
            }
        )
    }

    // ⑦ “설명 보기” 다이얼로그
    if (showDescDialog) {
        AlertDialog(
            onDismissRequest = { showDescDialog = false },
            title = { Text("맛집 설명") },
            text = { Text(currentDesc) },
            confirmButton = {
                TextButton(onClick = { showDescDialog = false }) {
                    Text("확인")
                }
            }
        )
    }
}
