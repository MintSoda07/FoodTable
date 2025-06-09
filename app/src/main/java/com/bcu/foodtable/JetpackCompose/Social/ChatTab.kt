package com.bcu.foodtable.JetpackCompose.Social

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.bcu.foodtable.data.GlobalChatManager
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@Composable
fun ChatTab() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // 전역 chatThreads 리스트를 구독
    val threads by remember { derivedStateOf { GlobalChatManager.chatThreads } }

    // 최초에 한 번만 Firebase에서 각 친구별 마지막 메시지 로드
    LaunchedEffect(Unit) {
        try {
            val uid = UserManager.getUser()!!.uid
            GlobalChatManager.chatThreads.clear()

            // 먼저 친구 목록을 읽어서
            val friendsSnap = Firebase.firestore
                .collection("users").document(uid)
                .collection("friends")
                .get().await()

            friendsSnap.documents.forEach { friendDoc ->
                val friendUid = friendDoc.id
                // 각 친구 채팅에서 최신 메시지 하나 가져오기
                val chatSnap = Firebase.firestore
                    .collection("users").document(uid)
                    .collection("friends").document(friendUid)
                    .collection("chats")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                val lastMsg = chatSnap.documents.firstOrNull()
                    ?.getString("message")
                    ?: "<메시지 없음>"
                GlobalChatManager.chatThreads.add(friendUid to lastMsg)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "채팅 목록 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(threads) { (friendUid, lastMsg) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // 클릭 시 클립보드에 친구 UID 복사
                        clipboard.setText(AnnotatedString(friendUid))
                        Toast.makeText(context, "UID 복사됨: $friendUid", Toast.LENGTH_SHORT).show()
                    }
                    .padding(12.dp)
            ) {
                Text(text = "상대: $friendUid")
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "마지막 메시지: $lastMsg")
            }
        }
    }
}
