package com.bcu.foodtable.ui.health

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.bcu.foodtable.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectActivity : AppCompatActivity() {

    private lateinit var healthConnectClient: HealthConnectClient

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    private val requestPermissionsLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions: Set<String> ->
        if (grantedPermissions.containsAll(permissions)) {
            Toast.makeText(this, "✅ 권한 승인됨!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❗ 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_connect)

        healthConnectClient = HealthConnectClient.getOrCreate(this)

        val txtStepResult = findViewById<TextView>(R.id.txtStepResult)
        val btnPermission = findViewById<Button>(R.id.btnRequestPermission)
        val btnReadSteps = findViewById<Button>(R.id.btnReadSteps)

        // 권한 요청
        btnPermission.setOnClickListener {
            lifecycleScope.launch {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                val needed = permissions - granted
                if (needed.isNotEmpty()) {
                    requestPermissionsLauncher.launch(needed)
                } else {
                    Toast.makeText(this@HealthConnectActivity, "✅ 이미 권한 있음", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 걸음 수 읽기
        btnReadSteps.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val endTime = Instant.now()
                    val startTime = endTime.minus(1, ChronoUnit.DAYS)

                    val response = healthConnectClient.readRecords(
                        ReadRecordsRequest(
                            recordType = StepsRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                        )
                    )

                    val totalSteps = response.records.sumOf { it.count }
                    txtStepResult.text = "오늘 걸음 수: $totalSteps"

                } catch (e: Exception) {
                    Toast.makeText(this@HealthConnectActivity, "❌ 걸음 수 불러오기 실패", Toast.LENGTH_SHORT).show()
                    Log.e("HealthConnect", "걸음 수 오류", e)
                }
            }
        }
    }
}
