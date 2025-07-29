package com.bcu.foodtable

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bcu.foodtable.JetpackCompose.AI.AiScreen
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "메시지 수신: ${remoteMessage.data}")
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "새 메시지"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "메시지가 도착했습니다."
        showNotification(applicationContext, title, body, remoteMessage.data)
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "새 토큰: $token")
        // 유저가 로그인한 상태라면 Firestore에도 저장
        val user = UserManager.getUser()
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("user").document(user.uid)
                .update("fcmToken", token)
        }
    }
}

fun showNotification(
    context: Context,
    title: String,
    body: String,
    data: Map<String, String>? = null
) {
    val channelId = "chat_message"
    val notificationId = (System.currentTimeMillis() % 100000).toInt()
    val intent = Intent(context, AiScreen.Chat::class.java) // TODO: 채팅 액티비티로 변경
    // 추가 데이터(채팅방 이동 등)
    data?.forEach { (k, v) -> intent.putExtra(k, v) }

    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 커스텀 사운드
    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    // 또는 res/raw/my_sound.mp3 등 사용하려면:
    // val soundUri = Uri.parse("android.resource://${context.packageName}/raw/my_sound")

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setSound(soundUri)
        .setContentIntent(pendingIntent)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // Android 8.0 이상은 채널 필요!
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId, "채팅 알림", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(soundUri, null)
            description = "채팅 메시지 도착 시 알림"
        }
        notificationManager.createNotificationChannel(channel)
    }
    notificationManager.notify(notificationId, builder.build())
}