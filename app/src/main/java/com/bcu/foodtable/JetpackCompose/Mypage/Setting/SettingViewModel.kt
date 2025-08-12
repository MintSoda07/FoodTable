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
    private val healthClient = runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()

    private val _healthPermissionGranted = MutableStateFlow(false)
    val healthPermissionGranted: StateFlow<Boolean> = _healthPermissionGranted

    //  최소 권한(스위치 ON 판단 기준)
    private val minimalPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )
    //  선택 권한(있으면 더 풍부한 기능)
    private val optionalPermissions = setOf(
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )
    //  요청에 사용할 전체(읽기만)
    private val allRequested = minimalPermissions + optionalPermissions

    init {
        checkHealthPermissions()
    }

    fun checkHealthPermissions() {
        viewModelScope.launch {
            try {
                val granted = healthClient?.permissionController?.getGrantedPermissions() ?: emptySet()
                //  최소 권한만 있어도 스위치 ON
                _healthPermissionGranted.value = minimalPermissions.all { it in granted }
            } catch (e: Exception) {
                Log.e("SettingViewModel", "권한 확인 오류: ${e.message}")
            }
        }
    }

    /** 스위치 ON -> 권한 요청 */
    fun requestHealthPermissions(launchPermissionRequest: (Set<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val granted = healthClient?.permissionController?.getGrantedPermissions() ?: emptySet()
                val needed = allRequested - granted
                if (needed.isNotEmpty()) {
                    launchPermissionRequest(needed)   // Health Connect 시트 표시
                } else {
                    Toast.makeText(context, "이미 권한이 허용되어 있어요.", Toast.LENGTH_SHORT).show()
                    _healthPermissionGranted.value = true
                }
            } catch (e: Exception) {
                Log.e("SettingViewModel", "권한 요청 오류: ${e.message}")
            }
        }
    }

    /** 스위치 OFF -> 모든 헬스 권한 철회 */
    fun revokeHealthPermissions() {
        viewModelScope.launch {
            try {
                healthClient?.permissionController?.revokeAllPermissions()
                // 철회 직후 실제 권한 상태 확인
                val granted = healthClient?.permissionController?.getGrantedPermissions() ?: emptySet()
                android.util.Log.d("SettingVM", "After revoke, granted=$granted")

                // Pref와 UI 상태 즉시 OFF로 맞춤
                HealthPrefs.setEnabled(getApplication(), false)
                _healthPermissionGranted.value = false
                Toast.makeText(getApplication(), "헬스 권한이 모두 철회되었습니다", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("SettingVM", "권한 철회 오류: ${e.message}")
                // 실패 시 실제 상태 재확인
                checkHealthPermissions()
                Toast.makeText(getApplication(), "권한 철회 실패", Toast.LENGTH_SHORT).show()
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
