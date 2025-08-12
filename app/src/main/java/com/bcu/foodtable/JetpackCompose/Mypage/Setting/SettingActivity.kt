package com.bcu.foodtable.JetpackCompose.Mypage.Setting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.runtime.rememberCoroutineScope
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import com.bcu.foodtable.JetpackCompose.Mypage.Setting.SettingScreen
import com.bcu.foodtable.JetpackCompose.Mypage.Setting.SettingViewModel
import kotlinx.coroutines.launch
class SettingActivity : ComponentActivity() {

    private val viewModel: SettingViewModel by viewModels()
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { _ ->
            lifecycleScope.launch {
                val client = viewModel.getHealthClient()
                val granted = client?.permissionController?.getGrantedPermissions() ?: emptySet()
                val minimal = setOf(HealthPermission.getReadPermission(StepsRecord::class))
                val enabled = minimal.all { it in granted }

                HealthPrefs.setEnabled(this@SettingActivity, enabled) //  Pref 정합
                viewModel.checkHealthPermissions()                     // UI 상태 갱신
            }
        }

        setContent {
            SettingScreen(
                context = this,
                viewModel = viewModel,
                onRequestPermissions = {
                    viewModel.requestHealthPermissions { needed ->
                        permissionLauncher.launch(needed)
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkHealthPermissions() // 설정/시트 다녀온 뒤 반영
    }
}

