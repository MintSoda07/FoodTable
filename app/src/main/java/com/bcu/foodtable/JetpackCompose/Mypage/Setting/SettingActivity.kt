package com.bcu.foodtable.JetpackCompose.Mypage.Setting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.runtime.rememberCoroutineScope
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingActivity : ComponentActivity() {

    private val viewModel: SettingViewModel by viewModels()

    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  여기서 등록
        permissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { grantedPermissions ->
            viewModel.checkHealthPermissions()
        }

        setContent {
            SettingScreen(
                context = this,
                viewModel = viewModel,
                onRequestPermissions = {
                    viewModel.requestHealthPermissions { neededPermissions ->
                        permissionLauncher.launch(neededPermissions)
                    }
                }
            )
        }

    }
}

