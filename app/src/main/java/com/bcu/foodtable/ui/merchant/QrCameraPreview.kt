package com.bcu.foodtable.ui.merchant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.material3.*

@Composable
fun QrCameraPreview(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,                 // ← on/off 컨트롤
    onBarcode: (String) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // ── 권한 ─────────────────────────────────────────────────────────────
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted) launcher.launch(Manifest.permission.CAMERA)
    }

    if (!permissionGranted) {
        Surface(tonalElevation = 2.dp) {
            Text("카메라 권한이 필요합니다.", modifier = modifier.padding(16.dp))
        }
        return
    }

    // ── CameraX 준비물 (remember 로 재사용) ───────────────────────────────
    val previewView = remember {
        PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    val previewUseCase = remember { Preview.Builder().build() }
    val analysisUseCase = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    // 분석기 세팅은 한 번만
    LaunchedEffect(Unit) {
        analysisUseCase.setAnalyzer(
            ContextCompat.getMainExecutor(ctx),
            BarcodeAnalyser(onBarcode)
        )
    }
    // SurfaceProvider 연결
    LaunchedEffect(previewView) {
        previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
    }

    // 카메라 프로바이더
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(ctx) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(cameraProviderFuture) {
        cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull()
    }

    AndroidView(modifier = modifier, factory = { previewView })

    // ── enabled/수명주기 따라 bind/unbind ────────────────────────────────
    LaunchedEffect(enabled, permissionGranted, cameraProvider, lifecycleOwner) {
        val provider = cameraProvider ?: return@LaunchedEffect
        // 항상 먼저 해제(안전)
        runCatching { provider.unbindAll() }

        if (enabled && permissionGranted) {
            runCatching {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase,
                    analysisUseCase
                )
            }
        } else {
            // 비활성일 때 분석도 멈추게(콜백 억제)
            runCatching { analysisUseCase.clearAnalyzer() }
            runCatching {
                // 다시 켤 때 재설정 되도록 analyzer 재세팅
                analysisUseCase.setAnalyzer(
                    ContextCompat.getMainExecutor(ctx),
                    BarcodeAnalyser(onBarcode)
                )
            }
        }
    }

    // 화면이 STOP 되거나 사라질 때 반드시 해제
    DisposableEffect(lifecycleOwner, cameraProvider) {
        val provider = cameraProvider
        val obs = LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            if (event == Lifecycle.Event.ON_STOP) {
                runCatching { provider?.unbindAll() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            runCatching { provider?.unbindAll() }
        }
    }
}
