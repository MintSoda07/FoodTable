package com.bcu.foodtable

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.bcu.foodtable.useful.UserManager

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(msg: RemoteMessage) {
        val data = msg.data
        val title = data["title"] ?: "새 메시지"
        val body  = data["body"] ?: "메시지가 도착했습니다."
        val chatUid = data["chatUid"] ?: return
        val messageId = data["messageId"] ?: System.currentTimeMillis().toString()

        Log.d("FCM", "onMessageReceived: data=$data, notification=${msg.notification}")

        // 현재 열람 중인 채팅방이면 시스템 알림 생략 -> 인앱 이벤트만
        if (AppState.isAppInForeground() && AppState.currentChatUid.value == chatUid) {
            InAppEvents.emitNewMessage(chatUid, title, body, messageId)
            return
        }

        // 시스템 알림
        Notifications.showChatNotification(
            context = applicationContext,
            title = title,
            body = body,
            chatUid = chatUid,
            messageId = messageId
        )
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "새 토큰: $token")
        val user = UserManager.getUser() ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("user").document(user.uid)
            .set(mapOf("fcmTokens" to FieldValue.arrayUnion(token)), SetOptions.merge())
            .addOnSuccessListener { Log.d("FCM", "토큰 저장 성공") }
            .addOnFailureListener { e -> Log.e("FCM", "토큰 저장 실패: ${e.message}", e) }
    }
}
