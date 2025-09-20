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
    if (!enabled) return

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
    val listener = remember {
        ShakeDetector(onShake = { onShake() })
    }

    // 생명주기에 따라 등록/해제
    DisposableEffect(lifecycleOwner, sensorManager, accel, listener) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    accel?.let {
                        sensorManager.registerListener(
                            listener, it, SensorManager.SENSOR_DELAY_GAME
                        )
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    sensorManager.unregisterListener(listener)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(listener)
        }
    }
}