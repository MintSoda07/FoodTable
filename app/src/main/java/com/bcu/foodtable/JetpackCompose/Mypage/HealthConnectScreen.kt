package com.bcu.foodtable.JetpackCompose.Mypage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.activity.compose.rememberLauncherForActivityResult
import com.bcu.foodtable.JetpackCompose.Mypage.StepBarChart.StepBarChart

// StepBarChart가 같은 패키지 내 함수라면 import 생략 가능
// import com.bcu.foodtable.JetpackCompose.Mypage.StepBarChart

@Composable
fun HealthConnectScreen(viewModel: HealthConnectViewModel) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val stepData by viewModel.stepDataList.collectAsState()
    val client = remember { HealthConnectClient.getOrCreate(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions: Set<String> ->
        if (grantedPermissions.containsAll(viewModel.getRequiredPermissions())) {
            viewModel.loadHealthData(client)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = state.resultText, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(12.dp))

        StepProgressBar(current = state.steps, goal = state.goal)

        Spacer(modifier = Modifier.height(12.dp))

        // StepBarChart가 stepData를 List<StepData> 타입으로 받는지 반드시 확인
        StepBarChart(
            stepData = stepData,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)  // 여기서 높이 지정
        )

        Spacer(modifier = Modifier.height(12.dp))
        state.foodItem?.let { foodItem ->
            Image(
                painter = painterResource(id = foodItem.imageResId),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
            Text("오늘 ${foodItem.name}${viewModel.getJosa(foodItem.name, "을", "를")} 불태웠어요!")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.rewardCount > 0) {
            Button(onClick = viewModel::claimReward) {
                Text("🎁 ${state.rewardCount} 보상 수령")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                val neededPermissions = viewModel.getRequiredPermissions()
                permissionLauncher.launch(neededPermissions)
            }) {
                Text("권한 요청")
            }

            Button(onClick = { viewModel.loadHealthData(client) }) {
                Text("걸음 수 불러오기")
            }
        }
    }
}
