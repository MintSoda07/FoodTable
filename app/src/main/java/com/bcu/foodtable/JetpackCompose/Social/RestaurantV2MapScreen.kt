package com.bcu.foodtable.JetpackCompose.Social

import android.util.Log
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bcu.foodtable.R
import com.bcu.foodtable.data.KakaoApiKeyProvider
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapType
import com.kakao.vectormap.MapViewInfo
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import kotlinx.coroutines.runBlocking

private const val TAG = "RestaurantV2Map"



@Composable
fun RestaurantV2MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MatzipViewModel = viewModel()
) {
    Log.d(TAG, "▶ Entered RestaurantV2MapScreen")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1) MapView 인스턴스 한 번만 생성
    val mapView = remember {
        Log.d(TAG, "▶ remember: create MapView")
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // 2) Lifecycle 이벤트에 따라 start/resume/pause 처리
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    Log.d(TAG, "▶ ON_CREATE → init & start")
                    // (a) API 키 가져오고 초기화
                    val key = "aa"
                    KakaoMapSdk.init(context, key)
                    // (b) 지도 시작
                    mapView.start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {
                                Log.d(TAG, "▶ onMapDestroy")
                            }
                            override fun onMapError(e: Exception?) {
                                Log.e(TAG, "▶ onMapError", e)
                            }
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(kakaoMap: KakaoMap) {
                                Log.d(TAG, "▶ onMapReady")
                                // 맵 준비 즉시 resume
                                mapView.resume()
                                // 카메라 이동+줌
                                viewModel.restaurants.firstOrNull()?.let { rest ->
                                    val pos = LatLng.from(rest.latitude, rest.longitude)
                                    Log.d(TAG, "▶ moveCamera to ${rest.name}")
                                    kakaoMap.moveCamera(
                                        CameraUpdateFactory.newCenterPosition(pos),
                                        CameraAnimation.from(500, true, true)
                                    )
                                    kakaoMap.moveCamera(
                                        CameraUpdateFactory.zoomTo(14),
                                        CameraAnimation.from(500, true, true)
                                    )
                                }
                                // POI 추가
                                val layer = kakaoMap.labelManager?.layer
                                viewModel.restaurants.forEachIndexed { idx: Int, rest: Restaurant ->
                                    Log.d(TAG, "   • add label #$idx → ${rest.name}")
                                    val p = LatLng.from(rest.latitude, rest.longitude)
                                    val opts = LabelOptions.from("rest_$idx", p)
                                        .setStyles(R.drawable.baseline_restaurant_menu_24)
                                        .setRank(idx.toLong())
                                    layer?.addLabel(opts)
                                }
                            }
                        }
                    )
                }
                Lifecycle.Event.ON_START -> {
                    Log.d(TAG, "▶ ON_START → mapView.resume()")
                    mapView.resume()
                }
                Lifecycle.Event.ON_STOP -> {
                    Log.d(TAG, "▶ ON_STOP → mapView.pause()")
                    mapView.pause()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    Log.d(TAG, "▶ ON_DESTROY → mapView.stop()")
                    // V2에는 stop()이 없으므로 onMapDestroy 콜백 안에서 처리하세요
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 3) Compose 트리에 배치
    Log.d(TAG, "▶ AndroidView render")
    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
    Log.d(TAG, "▶ End RestaurantV2MapScreen")
}