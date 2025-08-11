package com.bcu.foodtable

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.LocusIdCompat

object Notifications {

    private const val CHANNEL_ID_CHAT = "chat_message_v1" // 사운드/설정 바꾸면 ID 버전업
    private const val GROUP_KEY_CHAT = "chat_group"

    private fun ensureChatChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID_CHAT) != null) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ch = NotificationChannel(
            CHANNEL_ID_CHAT,
            "채팅 알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "채팅 메시지 도착 시 알림"
            setSound(soundUri, Notification.AUDIO_ATTRIBUTES_DEFAULT)
        }
        nm.createNotificationChannel(ch)
    }

    fun showChatNotification(
        context: Context,
        title: String,
        body: String,
        chatUid: String,
        messageId: String
    ) {
        ensureChatChannel(context)

        // 알림 클릭 시 MainActivity 열고 Compose에서 chat/{chatUid}로 네비게이션
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "OPEN_CHAT"
            putExtra("chatUid", chatUid)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val requestCode = chatUid.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val person = Person.Builder()
            .setName(title) // 상대/방 이름
            .build()

        val style = NotificationCompat.MessagingStyle(person)
            .setConversationTitle(null) // 단체방이면 방 제목을 넣어도 됨
            .addMessage(body, System.currentTimeMillis(), person)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_CHAT)
            .setSmallIcon(R.drawable.hangover_soup) // 흰색 단색 아이콘 권장
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(style)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setGroup(GROUP_KEY_CHAT)
            .setShortcutId(chatUid)
            .setLocusId(LocusIdCompat(chatUid))

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(chatUid.hashCode(), builder.build())
    }
}
