package com.bcu.foodtable.ui.health

import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
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


    private val stepGoals = listOf(5000, 10000, 15000, 20000)
    private val caloriesPerStep = 0.04
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

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
    }

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

                val totalCalories = healthConnectClient.readRecords(
                    ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(startTime, endTime))
                ).records.sumOf { it.energy.inKilocalories }

                val heartRate = healthConnectClient.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(startTime, endTime))
                ).records.flatMap { it.samples }.maxByOrNull { it.time }?.beatsPerMinute ?: 0

                val estimatedCalories = (steps * caloriesPerStep).toInt()

                txtResult.text = "걸음 수: $steps\n칼로리: ${totalCalories.toInt()} kcal\n추정: $estimatedCalories kcal\n심박수: $heartRate bpm"
                customStepView.setStepData(steps.toInt(), stepGoals.find { it > steps } ?: 20000)

                rewardUserIfNeeded(steps)

            } catch (e: Exception) {
                Log.e("HealthConnect", "불러오기 실패", e)
                Toast.makeText(this@HealthConnectActivity, "데이터 로딩 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rewardUserIfNeeded(currentSteps: Long) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val rewardRef = db.collection("users").document(uid)
            .collection("step_rewards").document(today)

        rewardRef.get().addOnSuccessListener { doc ->
            val currentLevel = doc.getLong("rewardStep")?.toInt() ?: 0
            val nextGoalIndex = currentLevel.coerceAtLeast(0)

            if (nextGoalIndex >= stepGoals.size) return@addOnSuccessListener

            val nextGoal = stepGoals[nextGoalIndex]
            if (currentSteps >= nextGoal) {
                db.runTransaction { transaction ->
                    val userRef = db.collection("user").document(uid)
                    val userSnap = transaction.get(userRef)
                    val currentPoint = userSnap.getLong("point") ?: 0
                    val newPoint = currentPoint + 50
                    transaction.update(userRef, "point", newPoint)
                    transaction.set(rewardRef, mapOf("rewardStep" to nextGoalIndex + 1))
                    newPoint
                }.addOnSuccessListener { newPoint ->
                    Toast.makeText(this, "${nextGoal}걸음 달성!  50포인트 지급!", Toast.LENGTH_SHORT).show()
                    animatePointReward(newPoint - 50, newPoint)
                }
            }
        }
    }

    private fun animatePointReward(from: Long, to: Long) {
        val animator = ValueAnimator.ofInt(from.toInt(), to.toInt())
        animator.duration = 1000
        animator.addUpdateListener {

        }
        animator.start()
    }

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

    private fun openHealthConnectSettings() {
        try {
            startActivity(Intent("android.health.connect.action.HEALTH_CONNECT_SETTINGS"))
        } catch (e: Exception) {
            Toast.makeText(this, "설정 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isHealthConnectInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
