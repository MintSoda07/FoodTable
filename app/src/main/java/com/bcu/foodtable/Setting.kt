package com.bcu.foodtable

import android.content.Intent
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
        try{
            healthClient = HealthConnectClient.getOrCreate(this)
        }catch(E:Exception){
            Log.e("health","NO HC Detected.")
        }

        healthPermissionSwitch = findViewById(R.id.switch_health_permission)

        //  요청할 권한 명시
        permissions = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class)
        )

        // 권한 런처 등록
        permissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions: Set<String> ->
            lifecycleScope.launch {
                val alreadyGranted = healthClient.permissionController.getGrantedPermissions()
                val allPermissions = grantedPermissions + alreadyGranted

                val allGranted = permissions.all { it in allPermissions }

                if (allGranted) {
                    Toast.makeText(this@Setting, " 모든 권한이 승인되었습니다", Toast.LENGTH_SHORT).show()
                    healthPermissionSwitch.isChecked = true
                } else {
                    Toast.makeText(this@Setting, "️ 일부 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
                    healthPermissionSwitch.isChecked = false
                }
            }
        }
        try{
            healthPermissionSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    lifecycleScope.launch {
                        val granted = healthClient.permissionController.getGrantedPermissions()
                        val needed = permissions - granted
                        if (needed.isNotEmpty()) {
                            permissionLauncher.launch(needed)
                        } else {
                            Toast.makeText(this@Setting, "이미 권한이 부여되어 있습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }catch(E:Exception){
            Log.e("health","NO HC Detected.")
        }
        //  스위치 ON -> 권한 요청


        // 로그아웃 처리
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
}
