package com.bcu.foodtable.JetpackCompose.Mypage.Setting

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.bcu.foodtable.ui.home.Screen

@Composable
fun ShakeToOpenQR(
    navController: NavController,
    routeQR: String = "qrScanner", // 네비게이션 라우트
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val accel = remember(sensorManager) { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    // 최신 onShake 참조 유지
    val onShake by rememberUpdatedState(newValue = {
        val currentRoute = navController.currentBackStackEntry?.destination?.route

        // 이미 QR이면 무시
        if (currentRoute == routeQR) return@rememberUpdatedState

        // 1) 마이페이지로 먼저 전환(탭 상태 복원 포함)
        navController.navigate(Screen.MyPage.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }

        // 2) 이어서 QR 스캐너로
        navController.navigate(routeQR) {
            launchSingleTop = true
        }
    })

    // 리스너 인스턴스는 remember로 1회 생성
    val listener = remember { ShakeDetector(onShake = { onShake() }) }

    // 🔑 enabled를 의존성에 포함: 값이 바뀌면 즉시 등록/해제
    DisposableEffect(lifecycleOwner, sensorManager, accel, listener, enabled) {
        fun register() {
            accel?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        }
        fun unregister() {
            sensorManager.unregisterListener(listener)
        }

        // 화면이 이미 START 상태일 수 있으므로 enabled면 즉시 등록
        if (enabled) register()

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (enabled) register()
                Lifecycle.Event.ON_STOP  -> unregister()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister() // 항상 해제해서 유출/중복 방지
        }
    }
}
