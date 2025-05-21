package com.bcu.foodtable.JetpackCompose.Mypage

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.bcu.foodtable.R
import com.bcu.foodtable.ui.myPage.FoodItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HealthConnectViewModel : ViewModel() {

    data class HealthUiState(
        val steps: Int = 0,
        val goal: Int = 20000,
        val resultText: String = "",
        val rewardCount: Int = 0,
        val foodItem: FoodItem? = null
    )

    private val stepGoals = listOf(5000, 10000, 15000, 20000)
    private val caloriesPerStep = 0.04
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState

    private val _stepDataList = MutableStateFlow<List<StepData>>(emptyList())
    val stepDataList: StateFlow<List<StepData>> = _stepDataList

    fun getRequiredPermissions(): Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    fun loadHealthData(client: HealthConnectClient) {
        viewModelScope.launch {
            try {
                val now = LocalDateTime.now()
                val startOfDay = LocalDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT)
                val startTime = startOfDay.atZone(ZoneId.systemDefault()).toInstant()
                val endTime = now.atZone(ZoneId.systemDefault()).toInstant()

                val steps = client.readRecords(
                    ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(startTime, endTime))
                ).records.sumOf { it.count }

                val totalCalories = client.readRecords(
                    ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(startTime, endTime))
                ).records.sumOf { it.energy.inKilocalories }

                val heartRate = client.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(startTime, endTime))
                ).records.flatMap { it.samples }.maxByOrNull { it.time }?.beatsPerMinute ?: 0

                val estimatedCalories = (steps * caloriesPerStep).toInt()
                val foodItem = getFoodByCalories(estimatedCalories)
                val resultText = "걸음 수: $steps\n칼로리: ${totalCalories.toInt()} kcal\n추정: $estimatedCalories kcal\n심박수: $heartRate bpm"

                val nextGoal = stepGoals.find { it > steps } ?: 20000

                _uiState.value = _uiState.value.copy(
                    steps = steps.toInt(),
                    goal = nextGoal,
                    resultText = resultText,
                    foodItem = foodItem
                )

                rewardUserIfNeeded(steps)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(resultText = "데이터 로딩 실패: ${e.message}")
            }
        }
    }

    private fun rewardUserIfNeeded(currentSteps: Long) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val rewardRef = db.collection("user").document(uid)
            .collection("step_rewards").document(today)

        rewardRef.get().addOnSuccessListener { doc ->
            val receivedCount = doc.getLong("rewardStep")?.toInt() ?: 0
            val rewardableCount = stepGoals.count { currentSteps >= it } - receivedCount
            _uiState.value = _uiState.value.copy(rewardCount = rewardableCount)
        }
    }

    fun claimReward() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val rewardRef = db.collection("user").document(uid)
            .collection("step_rewards").document(today)

        db.runTransaction { transaction ->
            val userRef = db.collection("user").document(uid)
            val userSnap = transaction.get(userRef)
            val currentPoint = userSnap.getLong("point") ?: 0
            val newPoint = currentPoint + 50
            val prevReward = transaction.get(rewardRef).getLong("rewardStep")?.toInt() ?: 0

            transaction.update(userRef, "point", newPoint)
            transaction.set(rewardRef, mapOf("rewardStep" to prevReward + 1))
            newPoint
        }.addOnSuccessListener {
            val updatedCount = _uiState.value.rewardCount - 1
            _uiState.value = _uiState.value.copy(rewardCount = updatedCount)
        }
    }

    private fun getFoodByCalories(calories: Int): FoodItem {
        return when (calories) {
            in 0..30 -> FoodItem("블랙커피", R.drawable.black_coffee)
            in 31..80 -> FoodItem("미역국", R.drawable.seaweed_soup)
            in 81..150 -> FoodItem("계란찜", R.drawable.steamed_egg)
            in 151..200 -> FoodItem("김밥", R.drawable.kimbap)
            in 201..300 -> FoodItem("된장찌개", R.drawable.soybean_paste_stew)
            in 301..400 -> FoodItem("순두부찌개", R.drawable.soft_tofu_stew)
            in 401..500 -> FoodItem("냉면", R.drawable.cold_noodles)
            in 501..600 -> FoodItem("라면", R.drawable.ramen)
            in 601..700 -> FoodItem("떡볶이", R.drawable.tteokbokki)
            in 701..800 -> FoodItem("돈까스", R.drawable.pork_cutlet)
            in 801..900 -> FoodItem("햄버거", R.drawable.hamburger)
            in 901..1000 -> FoodItem("해장국", R.drawable.hangover_soup)
            else -> FoodItem("국밥", R.drawable.rice_soup)
        }
    }

    fun getJosa(word: String, josaWithBatchim: String, josaWithoutBatchim: String): String {
        val lastChar = word.last()
        val hasBatchim = (lastChar.code - 0xAC00) % 28 != 0
        return if (hasBatchim) josaWithBatchim else josaWithoutBatchim
    }
}

