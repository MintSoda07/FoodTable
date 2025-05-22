package com.bcu.foodtable.JetpackCompose.Mypage.Setting

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val healthClient = try {
        HealthConnectClient.getOrCreate(context)
    } catch (e: Exception) {
        null
    }

    private val _healthPermissionGranted = MutableStateFlow(false)
    val healthPermissionGranted: StateFlow<Boolean> = _healthPermissionGranted

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    init {
        checkHealthPermissions()
    }

    fun checkHealthPermissions() {
        viewModelScope.launch {
            try {
                val granted = healthClient?.permissionController?.getGrantedPermissions() ?: emptySet()
                _healthPermissionGranted.value = permissions.all { it in granted }
            } catch (e: Exception) {
                Log.e("SettingViewModel", "권한 확인 오류: ${e.message}")
            }
        }
    }

    fun requestHealthPermissions(launchPermissionRequest: (Set<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val granted = healthClient?.permissionController?.getGrantedPermissions() ?: emptySet()
                val needed = permissions - granted
                if (needed.isNotEmpty()) {
                    launchPermissionRequest(needed)
                } else {
                    Toast.makeText(context, "이미 모든 권한이 부여되어 있습니다", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SettingViewModel", "권한 요청 오류: ${e.message}")
            }
        }
    }


    fun revokeHealthPermissions() {
        viewModelScope.launch {
            try {
                healthClient?.permissionController?.revokeAllPermissions()
                _healthPermissionGranted.value = false
                Toast.makeText(context, "권한이 모두 철회되었습니다", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("SettingViewModel", "권한 철회 오류: ${e.message}")
                Toast.makeText(context, "권한 철회 실패", Toast.LENGTH_SHORT).show()
                checkHealthPermissions()
            }
        }
    }

    fun logoutAndNavigate(context: Context) {
        auth.signOut()
        context.startActivity(Intent(context, com.bcu.foodtable.LoginActivity::class.java))
    }

    fun isHealthConnectInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openPlayStoreForHealthConnect(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            setPackage("com.android.vending")
        }
        context.startActivity(intent)
    }

    fun getHealthClient(): HealthConnectClient? = healthClient

    fun getPermissionLauncherContract() =
        PermissionController.createRequestPermissionResultContract()
}
