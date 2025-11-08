package com.bcu.foodtable.JetpackCompose.Mypage.Setting

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
    private val onShake: () -> Unit,
    private val thresholdG: Float = 3.5f,   // 흔들림 민감도(2.3~2.8 사이 권장)
    private val debounceMs: Long = 1200L    // 연속 트리거 방지 쿨다운
) : SensorEventListener {

    private var lastTriggerTime = 0L

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 중력으로 정규화된 gForce
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX*gX + gY*gY + gZ*gZ)

        if (gForce > thresholdG) {
            val now = System.currentTimeMillis()
            if (now - lastTriggerTime > debounceMs) {
                lastTriggerTime = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}