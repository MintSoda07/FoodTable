package com.bcu.foodtable.ui.health

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord

import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.bcu.foodtable.R
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class HealthConnectActivity : AppCompatActivity() {

    private lateinit var healthConnectClient: HealthConnectClient

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class) // 심박수
    )

    private val requestPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted: Set<String> ->
        val denied = permissions - granted
        if (denied.isEmpty()) {
            Toast.makeText(this, "권한 승인됨!", Toast.LENGTH_SHORT).show()
            loadHealthData()
        } else {
            AlertDialog.Builder(this)
                .setTitle("권한이 필요합니다")
                .setMessage("걸음 수, 칼로리, 심박수 데이터를 위해 권한이 필요합니다.\nHealth Connect 설정에서 허용해주세요.")
                .setPositiveButton("설정 열기") { _, _ -> openHealthConnectSettings() }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private lateinit var txtResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_connect)

        txtResult = findViewById(R.id.txtStepResult)
        val btnPermission = findViewById<Button>(R.id.btnRequestPermission)
        val btnReadSteps = findViewById<Button>(R.id.btnReadSteps)

        if (!isHealthConnectInstalled()) {
            AlertDialog.Builder(this)
                .setTitle("Health Connect 앱 필요")
                .setMessage("이 기능을 사용하려면 Health Connect 앱이 필요합니다.\n지금 설치하시겠습니까?")
                .setPositiveButton("설치하러 가기") { _, _ ->
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                    startActivity(intent)
                }
                .setNegativeButton("취소", null)
                .show()
            return
        }

        healthConnectClient = HealthConnectClient.getOrCreate(this)

        btnPermission.setOnClickListener {
            lifecycleScope.launch {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                val needed = permissions - granted

                if (needed.isNotEmpty()) {
                    requestPermissionLauncher.launch(needed)
                } else {
                    Toast.makeText(this@HealthConnectActivity, "이미 권한이 있습니다", Toast.LENGTH_SHORT).show()
                    loadHealthData()
                }
            }
        }

        btnReadSteps.setOnClickListener {
            loadHealthData()
        }
    }

    private fun loadHealthData() {
        lifecycleScope.launch {
            try {
                val now = LocalDateTime.now()
                val startOfDay = LocalDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT)
                val startTime = startOfDay.atZone(ZoneId.systemDefault()).toInstant()
                val endTime = now.atZone(ZoneId.systemDefault()).toInstant()

                // 걸음 수
                val stepsResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(startTime, endTime))
                )
                val totalSteps = stepsResponse.records.sumOf { it.count }

                // 활동 칼로리
                val caloriesResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(startTime, endTime))
                )
                val totalCalories = caloriesResponse.records.sumOf { it.energy.inKilocalories }

                // 심박수 평균
                val heartRateResponse = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )

                val latestHeartRate = heartRateResponse.records
                    .flatMap { it.samples }
                    .maxByOrNull { it.time }
                    ?.beatsPerMinute ?: 0

                txtResult.text = """
                    오늘 걸음 수: $totalSteps
                    활동 칼로리: ${totalCalories.toInt()} kcal
                    심박수: $latestHeartRate bpm
                """.trimIndent()

                Log.d("HealthConnect", "걸음 수: $totalSteps, 칼로리: $totalCalories, 심박수 평균: $latestHeartRate")

            } catch (e: Exception) {
                Log.e("HealthConnect", "건강 데이터 불러오기 실패", e)
                Toast.makeText(this@HealthConnectActivity, "데이터 불러오기 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openHealthConnectSettings() {
        try {
            val intent = Intent("android.health.connect.action.HEALTH_CONNECT_SETTINGS")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "설정 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isHealthConnectInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
