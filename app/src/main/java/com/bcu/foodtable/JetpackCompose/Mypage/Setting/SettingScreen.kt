package com.bcu.foodtable.JetpackCompose.Mypage.Setting

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcu.foodtable.R

@Composable
fun SettingScreen(
    context: Context,
    viewModel: SettingViewModel,
    onRequestPermissions: () -> Unit
) {
    val healthGranted by viewModel.healthPermissionGranted.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 상단 타이틀
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "설정",
                    fontSize = 20.sp,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Red, RoundedCornerShape(8.dp))
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.baseline_settings_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingItemSwitch(
                label = "헬스 권한",
                checked = healthGranted,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        if (!viewModel.isHealthConnectInstalled()) {
                            viewModel.openPlayStoreForHealthConnect(context)
                        } else {
                            onRequestPermissions()
                        }
                    } else {
                        viewModel.revokeHealthPermissions()
                    }
                }
            )

            SettingItemSwitch(
                label = "푸시 알림",
                checked = false,
                onCheckedChange = {}
            )

            SettingItemSwitch(
                label = "다크 모드",
                checked = false,
                onCheckedChange = {}
            )

            SettingItemText(label = "앱 버전 1.0.0")
        }

        Button(
            onClick = { viewModel.logoutAndNavigate(context) },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("로그아웃", color = Color.White)
        }
    }
}

@Composable
fun SettingItemSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingItemText(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp)
    }
}
