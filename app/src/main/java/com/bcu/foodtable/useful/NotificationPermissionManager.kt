package com.bcu.foodtable.useful
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

class NotificationPermissionManager(
    private val context: Context,
    private val permissionLauncher: ActivityResultLauncher<String>
) {

    fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12 이하에서는 별도의 알림 권한이 필요 없음
        }
    }

    fun requestPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isPermissionGranted()) {
                if (shouldShowRationale()) {
                    showPermissionRationale()
                } else {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun shouldShowRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (context as? androidx.appcompat.app.AppCompatActivity)?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } ?: false
        } else {
            false
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(context)
            .setTitle("알림 권한 요청")
            .setMessage("앱에서 알림을 표시하려면 알림 권한이 필요합니다.")
            .setPositiveButton("확인") { _, _ ->
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("취소", null)
            .show()
    }
}