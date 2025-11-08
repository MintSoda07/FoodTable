// OpenChatRepository.kt  (선택: 쓰고 있다면 배열 방식으로 업데이트)
package com.bcu.foodtable.JetpackCompose.Social.Openchat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class OpenChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // 강퇴: 멤버 삭제 + memberIds 제거 + 로그
    suspend fun kickMember(roomId: String, targetUid: String) {
        val roomRef = db.collection("openRooms").document(roomId)
        val memberRef = roomRef.collection("members").document(targetUid)
        val logRef = roomRef.collection("events").document()

        db.runTransaction { tx ->
            tx.delete(memberRef)
            tx.update(roomRef, "memberIds", FieldValue.arrayRemove(targetUid))
            tx.set(logRef, mapOf(
                "type" to "kick",
                "targetUid" to targetUid,
                "ts" to Timestamp.now()
            ))
            null
        }.await()
    }

    // 밴: bans 추가 + 위와 동일
    suspend fun banMember(roomId: String, targetUid: String, reason: String?) {
        val roomRef = db.collection("openRooms").document(roomId)
        val banRef = roomRef.collection("bans").document(targetUid)
        val memberRef = roomRef.collection("members").document(targetUid)
        val logRef = roomRef.collection("events").document()

        db.runBatch { b ->
            b.set(banRef, mapOf("reason" to (reason ?: ""), "bannedAt" to Timestamp.now()))
            b.delete(memberRef)
            b.update(roomRef, "memberIds", FieldValue.arrayRemove(targetUid))
            b.set(logRef, mapOf(
                "type" to "ban",
                "targetUid" to targetUid,
                "reason" to (reason ?: ""),
                "ts" to Timestamp.now()
            ))
        }.await()
    }

    suspend fun isBanned(roomId: String, uid: String): Boolean {
        val snap = db.collection("openRooms")
            .document(roomId).collection("bans").document(uid)
            .get().await()
        return snap.exists()
    }

    suspend fun unbanMember(roomId: String, targetUid: String) {
        db.collection("openRooms").document(roomId)
            .collection("bans").document(targetUid)
            .delete().await()
    }
}
