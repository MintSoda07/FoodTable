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
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bcu.foodtable.JetpackCompose.Social.DetailedChatScreen // ← 실제 채팅 메인 또는 상세로 연결
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "메시지 수신: notification=${remoteMessage.notification}, data=${remoteMessage.data}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "새 메시지"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "메시지가 도착했습니다."

        // 채팅방 id (상대 UID 등), type 등 다양한 정보 전달 가능
        val chatUid = remoteMessage.data["chatUid"] ?: remoteMessage.data["fromUid"] ?: ""
        showNotification(applicationContext, title, body, chatUid, remoteMessage.data)
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "새 토큰: $token")
        // 유저가 로그인한 상태라면 Firestore에도 저장
        val user = UserManager.getUser()
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("user").document(user.uid)
                .update("fcmToken", token)
                .addOnSuccessListener { Log.d("FCM", "토큰 저장 성공") }
                .addOnFailureListener { e -> Log.e("FCM", "토큰 저장 실패: ${e.message}", e) }
        }
    }
}

// 알림 표시 함수 (채팅방으로 이동 포함)
fun showNotification(
    context: Context,
    title: String,
    body: String,
    chatUid: String?,
    data: Map<String, String>? = null
) {
    val channelId = "chat_message"
    val notificationId = (System.currentTimeMillis() % 100000).toInt()

    //  실제 채팅 메인 or 상세 Activity로 변경
    val intent = Intent(context, HomeActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        chatUid?.let { putExtra("chatUid", it) }
        // 필요하다면 추가 데이터도
    }

    val pendingIntent = PendingIntent.getActivity(
        context, notificationId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 🔵 커스텀 사운드 예시 (raw/my_sound.mp3)
    val soundUri: Uri = try {
        // 파일 추가 후 사용: val soundUri = Uri.parse("android.resource://${context.packageName}/raw/my_sound")
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    } catch (e: Exception) {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setSound(soundUri)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // 채널 중복 생성 방지
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId, "채팅 알림", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(soundUri, null)
                description = "채팅 메시지 도착 시 알림"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    notificationManager.notify(notificationId, builder.build())
}
