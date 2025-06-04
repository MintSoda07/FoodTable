package com.bcu.foodtable.JetpackCompose.Mypage.Health

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.bcu.foodtable.JetpackCompose.AI.AiMainActivity
import com.bcu.foodtable.JetpackCompose.Subscribe.SubscribeActivity
import com.bcu.foodtable.JetpackCompose.Mypage.StepBarChart.StepBarChart
import com.bcu.foodtable.JetpackCompose.RecipeStorage.RecipeStorageActivity
import com.bcu.foodtable.ui.ChallengeActivity
import com.bcu.foodtable.ui.home.AppBottomNavigationBar
import com.bcu.foodtable.ui.home.HomeTopBar
import com.bcu.foodtable.ui.home.Screen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bcu.foodtable.JetpackCompose.HomeViewModel

@Composable
fun HealthConnectScreen(viewModel: HealthConnectViewModel, homeViewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val user by homeViewModel.user.collectAsState()

    val client = remember { HealthConnectClient.getOrCreate(context) }
    val rawData by viewModel.stepDataList.collectAsState()

    Log.d("StepDebug", "original dates = ${rawData.map { it.date }}")
    val weeklyData = fillWeeklyStepData(rawData)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions: Set<String> ->
        if (grantedPermissions.containsAll(viewModel.getRequiredPermissions())) {
            viewModel.loadHealthData(client)
        }
    }

    var selectedTab by remember { mutableStateOf(4) } // Health tab index
    val screens = listOf(
        Screen.Home, Screen.Subscribe, Screen.Social, Screen.RecipeStorage, Screen.MyPage
    )

    LaunchedEffect(Unit) {
        viewModel.setHealthClient(client)
        homeViewModel.loadUserInfo()

        StepSyncManager(context, client, viewModel).syncIfNewDay()

        val granted =
            viewModel.getHealthClient()?.permissionController?.getGrantedPermissions() ?: emptySet()
        val needed = viewModel.getRequiredPermissions() - granted
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed)
        } else {
            viewModel.loadHealthData(client)
        }

        viewModel.fetchWeeklySteps()
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                user = user
            )
        },
        bottomBar = {
            AppBottomNavigationBar(
                screens = screens,
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    when (newTab) {
                        0 -> (context as? androidx.activity.ComponentActivity)?.finish()
                        1 -> context.startActivity(Intent(context, SubscribeActivity::class.java))
                        2 -> context.startActivity(Intent(context, AiMainActivity::class.java))
                        3 -> context.startActivity(Intent(context, RecipeStorageActivity::class.java))
                        4 -> selectedTab = newTab
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // 반원형 그래프
            AndroidView(
                factory = { StepProgressView(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                it.setStepData(state.steps, state.goal)
                it.setArcColor("#FF935C")
            }

            // 보상 수령 버튼
            if (state.rewardCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = viewModel::claimReward,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7043) // 홈 화면 계열의 오렌지톤
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("🎁 ${state.rewardCount} 보상 수령", fontSize = 16.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 걸음 수 및 칼로리 정보
            Text(
                text = "걸음 수: ${state.steps}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "추정 칼로리: ${(state.steps * 0.04).toInt()} kcal",
                fontSize = 18.sp,
                color = Color(0xFFEF6C00), // 톤다운된 주황 강조
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 음식 아이템 표시
            state.foodItem?.let { foodItem ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Image(
                        painter = painterResource(id = foodItem.imageResId),
                        contentDescription = null,
                        modifier = Modifier.size(70.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "오늘 ${foodItem.name}${viewModel.getJosa(foodItem.name, "을", "를")} 불태웠어요!",
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(0.dp))

            // 주간 걸음 그래프
            val stepData by viewModel.stepDataList.collectAsState()
            Log.d("StepChart", "Compose에서 받은 데이터: $stepData")
            StepBarChart(
                stepData = weeklyData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

}
