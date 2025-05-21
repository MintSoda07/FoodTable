package com.bcu.foodtable.JetpackCompose.Mypage.Health

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import com.bcu.foodtable.JetpackCompose.Mypage.StepBarChart.StepBarChart
import com.bcu.foodtable.ui.myPage.StepProgressView

@Composable
fun HealthConnectScreen(viewModel: HealthConnectViewModel) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    val client = remember { HealthConnectClient.getOrCreate(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions: Set<String> ->
        if (grantedPermissions.containsAll(viewModel.getRequiredPermissions())) {
            viewModel.loadHealthData(client)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.setHealthClient(client)

        StepSyncManager(context, client, viewModel).syncIfNewDay()

        val granted = viewModel.getHealthClient()?.permissionController?.getGrantedPermissions() ?: emptySet()
        val needed = viewModel.getRequiredPermissions() - granted
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed)
        } else {
            viewModel.loadHealthData(client)
        }
        viewModel.fetchWeeklySteps()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        // 반원형 걸음 수 ProgressView
        AndroidView(
            factory = { StepProgressView(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            update = { it.setStepData(state.steps, state.goal) }
        )

        Spacer(modifier = Modifier.height(6.dp))
        if (state.rewardCount > 0) {
            Button(
                onClick = viewModel::claimReward,
                modifier = Modifier
                    .padding(bottom = 8.dp)
            ) {
                Text("🎁 ${state.rewardCount} 보상 수령")
            }
        }
        Text("걸음 수: ${state.steps}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("추정 칼로리: ${(state.steps * 0.04).toInt()} kcal", fontSize = 18.sp)

        Spacer(modifier = Modifier.height(16.dp))

        state.foodItem?.let { foodItem ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Image(
                    painter = painterResource(id = foodItem.imageResId),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text("오늘 ${foodItem.name}${viewModel.getJosa(foodItem.name, "을", "를")} 불태웠어요!", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        val stepData by viewModel.stepDataList.collectAsState()
        Log.d("StepChart", "Compose에서 받은 데이터: $stepData")
        StepBarChart(
            stepData = stepData,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))


    }


}