package com.bcu.foodtable

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.lifecycle.lifecycleScope
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class Setting : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>
    private lateinit var permissions: Set<String>
    private lateinit var healthClient:HealthConnectClient
    private lateinit var healthPermissionSwitch : Switch
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        auth = FirebaseAuth.getInstance()
        val btnLogout = findViewById<Button>(R.id.btn_logout)

        try {
            healthClient = HealthConnectClient.getOrCreate(this)
        } catch (e: Exception) {
            Log.e("health", "NO HC Detected.")
        }

        healthPermissionSwitch = findViewById(R.id.switch_health_permission)

        // 권한 명시
        permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class)
        )

        //  [1] 앱 실행 시 권한 상태 확인 → 스위치 상태 반영
        lifecycleScope.launch {
            try {
                val granted = healthClient.permissionController.getGrantedPermissions()
                val allGranted = permissions.all { it in granted }
                healthPermissionSwitch.isChecked = allGranted
            } catch (e: Exception) {
                Log.e("health", "권한 확인 중 오류: ${e.message}")
            }
        }

        //  [2] 권한 요청 런처 등록
        permissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions: Set<String> ->
            lifecycleScope.launch {
                val alreadyGranted = healthClient.permissionController.getGrantedPermissions()
                val allPermissions = grantedPermissions + alreadyGranted
                val allGranted = permissions.all { it in allPermissions }

                healthPermissionSwitch.isChecked = allGranted
                val msg = if (allGranted) "모든 권한이 승인되었습니다" else "일부 권한이 거부되었습니다"
                Toast.makeText(this@Setting, msg, Toast.LENGTH_SHORT).show()
            }
        }

        //  [3] 스위치 ON → 권한 요청
        healthPermissionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isHealthConnectInstalled()) {
                    // Health Connect 앱이 없으면 PlayStore로 이동
                    Toast.makeText(this, "Health Connect 앱이 필요합니다", Toast.LENGTH_SHORT).show()
                    healthPermissionSwitch.isChecked = false  // 스위치 다시 OFF
                    openPlayStoreForHealthConnect()
                    return@setOnCheckedChangeListener
                }

                // 권한 요청
                lifecycleScope.launch {
                    val granted = healthClient.permissionController.getGrantedPermissions()
                    val needed = permissions - granted
                    if (needed.isNotEmpty()) {
                        permissionLauncher.launch(needed)
                    } else {
                        Toast.makeText(this@Setting, "이미 권한이 부여되어 있습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // 스위치 OFF 시 권한 철회
                lifecycleScope.launch {
                    try {
                        healthClient.permissionController.revokeAllPermissions()
                        Toast.makeText(this@Setting, "모든 Health Connect 권한이 취소되었습니다", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("health", "권한 철회 실패: ${e.message}")
                        Toast.makeText(this@Setting, "권한 철회 실패", Toast.LENGTH_SHORT).show()
                        healthPermissionSwitch.isChecked = true // 실패 시 다시 ON
                    }
                }
            }
        }



        // 로그아웃
        btnLogout.setOnClickListener {
            auth.currentUser?.let { auth.signOut() }
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                val granted = healthClient.permissionController.getGrantedPermissions()
                healthPermissionSwitch.isChecked = permissions.all { it in granted }
            } catch (e: Exception) {
                Log.e("health", "권한 재확인 중 오류: ${e.message}")
            }
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

    private fun openPlayStoreForHealthConnect() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            setPackage("com.android.vending")
        }
        startActivity(intent)
    }



}
