package com.bcu.foodtable.ui.health

import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.bcu.foodtable.R
import com.bcu.foodtable.ui.myPage.FoodItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HealthConnectActivity : AppCompatActivity() {

    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var txtResult: TextView
    private lateinit var customStepView: StepProgressView
    private lateinit var btnRewardBox: Button

    private val stepGoals = listOf(5000, 10000, 15000, 20000)
    private val caloriesPerStep = 0.04
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var rewardableCount = 0

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_connect)

        txtResult = findViewById(R.id.txtStepResult)
        customStepView = findViewById(R.id.stepProgressView)
        val btnPermission = findViewById<Button>(R.id.btnRequestPermission)
        val btnReadSteps = findViewById<Button>(R.id.btnReadSteps)
        btnRewardBox = findViewById(R.id.btnRewardBox)

        btnRewardBox.setOnClickListener {
            claimReward()
        }

        if (!isHealthConnectInstalled()) {
            showInstallDialog()
            return
        }

        healthConnectClient = HealthConnectClient.getOrCreate(this)

        btnPermission.setOnClickListener {
            lifecycleScope.launch {
                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                val needed = permissions - granted
                if (needed.isNotEmpty()) {
                    registerForActivityResult(PermissionController.createRequestPermissionResultContract()) {}.launch(needed)
                } else {
                    loadHealthData()
                }
            }
        }

        btnReadSteps.setOnClickListener {
            loadHealthData()
        }

        loadHealthData()
    }
    // 헬스 데이터 로드
    private fun loadHealthData() {
        lifecycleScope.launch {
            try {
                val now = LocalDateTime.now()
                val startOfDay = LocalDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT)
                val startTime = startOfDay.atZone(ZoneId.systemDefault()).toInstant()
                val endTime = now.atZone(ZoneId.systemDefault()).toInstant()

                val steps = healthConnectClient.readRecords(
                    ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(startTime, endTime))
                ).records.sumOf { it.count }
//                val steps = 25000L
                val totalCalories = healthConnectClient.readRecords(
                    ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(startTime, endTime))
                ).records.sumOf { it.energy.inKilocalories }

                val heartRate = healthConnectClient.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(startTime, endTime))
                ).records.flatMap { it.samples }.maxByOrNull { it.time }?.beatsPerMinute ?: 0

                // 예상 칼로리 바탕으로 음식 출력
                val estimatedCalories = (steps * caloriesPerStep).toInt()
                val foodItem = getFoodByCalories(estimatedCalories)
                val particle = getJosa(foodItem.name, "을", "를")
                val foodImageView = findViewById<ImageView>(R.id.foodImageView)
                val foodTextView = findViewById<TextView>(R.id.foodEquivalentTextView)

                foodImageView.setImageResource(foodItem.imageResId)
                foodTextView.text = "오늘 ${foodItem.name}$particle 불태웠어요!"


                txtResult.text = "걸음 수: $steps\n칼로리: ${totalCalories.toInt()} kcal\n추정: $estimatedCalories kcal\n심박수: $heartRate bpm"
                customStepView.setStepData(steps.toInt(), stepGoals.find { it > steps } ?: 20000)

                rewardUserIfNeeded(steps)

            } catch (e: Exception) {
                Log.e("HealthConnect", "불러오기 실패", e)
                Toast.makeText(this@HealthConnectActivity, "데이터 로딩 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // 리워드 횟수 체크해주는거?
    private fun rewardUserIfNeeded(currentSteps: Long) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val rewardRef = db.collection("user").document(uid)
            .collection("step_rewards").document(today)

        rewardRef.get().addOnSuccessListener { doc ->
            val receivedCount = doc.getLong("rewardStep")?.toInt() ?: 0
            rewardableCount = stepGoals.count { currentSteps >= it } - receivedCount
            updateRewardBoxUI()
        }
    }
    // 리워드 알고리즘
    private fun claimReward() {
        if (rewardableCount <= 0) return
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
        }.addOnSuccessListener { newPoint ->
            rewardableCount--
            updateRewardBoxUI()
            Toast.makeText(this, " 보상 수령! +50P", Toast.LENGTH_SHORT).show()
            animatePointReward(newPoint - 50, newPoint)
        }
    }
    // 리워드 ui 업데이트
    private fun updateRewardBoxUI() {
        if (rewardableCount > 0) {
            btnRewardBox.visibility = View.VISIBLE
            btnRewardBox.text = "🎁 $rewardableCount"
        } else {
            btnRewardBox.visibility = View.GONE
        }
    }


    private fun animatePointReward(from: Long, to: Long) {
        val animator = ValueAnimator.ofInt(from.toInt(), to.toInt())
        animator.duration = 1000
        animator.addUpdateListener {}
        animator.start()
    }
    //예상 칼로리로 태운 음식 칼로리
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
    // 헬스 커넥터 다운로드 다이어로그
    private fun showInstallDialog() {
        AlertDialog.Builder(this)
            .setTitle("Health Connect 필요")
            .setMessage("Health Connect 앱을 설치해야 합니다.")
            .setPositiveButton("설치") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                })
            }
            .setNegativeButton("취소", null)
            .show()
    }
    // 헬스 커넥트 세팅 오픈
    private fun openHealthConnectSettings() {
        try {
            startActivity(Intent("android.health.connect.action.HEALTH_CONNECT_SETTINGS"))
        } catch (e: Exception) {
            Toast.makeText(this, "설정 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 헬스 커넥터 다운 확인
    private fun isHealthConnectInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
