package com.bcu.foodtable.useful

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bcu.foodtable.R

class NotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val TIMER_CHANNEL_ID = "timer_channel"
        const val TIMER_NOTIFICATION_ID = 1
    }

    init {
        createNotificationChannel()
    }

    // 알림 채널 생성
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TIMER_CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for Timer Updates"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 알림 표시
    fun showTimerNotification(timeRemaining: String,timerTitle:String) {
        val notification = NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.dish_icon) // 알림 아이콘 설정
            .setContentTitle("${timerTitle} 진행 중")
            .setContentText("남은 시간: $timeRemaining")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 사용자가 제거하지 못하도록 고정
            .build()

        notificationManager.notify(TIMER_NOTIFICATION_ID, notification)
    }

    // 알림 제거
    fun cancelNotification() {
        notificationManager.cancel(TIMER_NOTIFICATION_ID)
    }
}