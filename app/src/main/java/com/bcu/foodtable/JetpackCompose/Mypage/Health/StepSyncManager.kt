
package com.bcu.foodtable.JetpackCompose.Mypage.Health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import com.bcu.foodtable.JetpackCompose.Mypage.Health.HealthConnectViewModel
import java.time.LocalDate

class StepSyncManager(
    private val context: Context,
    private val healthClient: HealthConnectClient,
    private val viewModel: HealthConnectViewModel
) {
    private val preferences = context.getSharedPreferences("health_prefs", Context.MODE_PRIVATE)

    fun syncIfNewDay() {
        val today = LocalDate.now().toString()
        val lastSavedDate = preferences.getString("last_saved_date", null)

        if (lastSavedDate != today) {
            Log.d("StepSyncManager", "새로운 날짜: 어제 걸음 수 저장 시도")
            viewModel.uploadYesterdaySteps(healthClient)
            preferences.edit().putString("last_saved_date", today).apply()
        } else {
            Log.d("StepSyncManager", "오늘 이미 저장 완료됨: $today")
        }
    }
}
