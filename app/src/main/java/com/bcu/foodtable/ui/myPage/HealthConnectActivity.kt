package com.bcu.foodtable.ui.health

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.PermissionController
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

    // 1. 권한 집합 생성
    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    // 2. 권한 요청 런처 생성
    private val requestPermissionsLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        if (grantedPermissions.containsAll(permissions)) {
            Toast.makeText(this, " 권한이 승인되었습니다!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, " 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_connect)

        // 3. 클라이언트 초기화
        healthConnectClient = HealthConnectClient.getOrCreate(this)

        val txtStepResult = findViewById<TextView>(R.id.txtStepResult)
        val btnPermission = findViewById<Button>(R.id.btnRequestPermission)
        val btnReadSteps = findViewById<Button>(R.id.btnReadSteps)

        // 4. 권한 요청 버튼 클릭 시
        btnPermission.setOnClickListener {
            lifecycleScope.launch {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                val needed = permissions - granted
                if (needed.isNotEmpty()) {
                    requestPermissionsLauncher.launch(needed)
                } else {
                    Toast.makeText(this@HealthConnectActivity, " 이미 권한이 있습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5. 걸음 수 읽기
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
                    Toast.makeText(this@HealthConnectActivity, " 걸음 수 읽기 실패", Toast.LENGTH_SHORT).show()
                    Log.e("HealthConnect", "걸음 수 오류", e)
                }
            }
        }
    }
}
