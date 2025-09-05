package com.bcu.foodtable.JetpackCompose.Social.Openchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import com.google.firebase.firestore.FieldPath

class OpenChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val rooms = db.collection("openRooms")
    private val users = db.collection("user")
    private val dms   = db.collection("dm") // 개인채팅(초대 메시지 전송용) – 프로젝트 구조에 맞게 경로만 맞추면 됨

    private val _discover = MutableStateFlow<List<OpenChatRoom>>(emptyList())
    val discover: StateFlow<List<OpenChatRoom>> = _discover

    private val _myRooms = MutableStateFlow<List<OpenChatRoom>>(emptyList())
    val myRooms: StateFlow<List<OpenChatRoom>> = _myRooms

    private val _messages = MutableStateFlow<List<RoomMessage>>(emptyList())
    val messages: StateFlow<List<RoomMessage>> = _messages

    private var msgListener: ListenerRegistration? = null
    private var currentRoomId: String? = null
    private var myRoomsListener: ListenerRegistration? = null
    private var discoverListener: ListenerRegistration? = null

    /** 공개방(탐색) 실시간 구독 */
    fun observeDiscover() {
        discoverListener?.remove()
        discoverListener = rooms
            .whereEqualTo("open", true)
            .orderBy("lastAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                _discover.value = snap?.documents?.mapNotNull { d ->
                    d.toObject(OpenChatRoom::class.java)?.copy(id = d.id)
                }.orEmpty()
            }
    }

    /** 내 방 실시간 구독 (배열 인덱스) */
    fun observeMyRooms(myUid: String) {
        myRoomsListener?.remove()
        myRoomsListener = rooms
            .whereArrayContains("memberIds", myUid)
            .orderBy("lastAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { d ->
                    d.toObject(OpenChatRoom::class.java)?.copy(id = d.id)
                }.orEmpty()
                _myRooms.value = list
            }
    }


    /** 방 만들기 */
    suspend fun createRoom(
        ownerUid: String,
        ownerNickname: String,
        title: String,
        desc: String,
        open: Boolean,
        passcode: String?,
        thumbUrl: String? = null
    ): String {
        val ref = rooms.document()
        val now = System.currentTimeMillis()
        val room = OpenChatRoom(
            id = ref.id,
            title = title,
            desc = desc,
            ownerUid = ownerUid,
            open = open,
            passcode = passcode,
            memberCount = 1,
            lastAt = now,
            lastMessage = null,
            memberIds = listOf(ownerUid),
            thumbUrl = thumbUrl.orEmpty()
        )
        ref.set(room).await()
        val ownerMember = OpenChatMember(
            uid = ownerUid, nickname = ownerNickname.ifBlank { "방장" },
            role = "owner", colorSeed = ownerUid.hashCode(), joinedAt = now
        )
        ref.collection("members").document(ownerUid).set(ownerMember).await()
        return ref.id
    }

    /** 공개/비공개 전환 (방장 전용) */
    suspend fun setRoomVisibility(roomId: String, isOpen: Boolean, passcode: String?) {
        val updates = mutableMapOf<String, Any>("open" to isOpen)
        updates["passcode"] = if (isOpen) FieldValue.delete() else (passcode ?: "")
        rooms.document(roomId).update(updates).await()
        sendSystem(roomId, "notice",
            if (isOpen) "방이 공개로 전환되었습니다." else "방이 비공개로 전환되었습니다."
        )
    }

    /** 참가 */
    suspend fun joinRoom(roomId: String, uid: String, nickname: String): Boolean {
        val roomDoc = rooms.document(roomId).get().await()
        if (!roomDoc.exists()) return false
        val banned = rooms.document(roomId).collection("bans").document(uid).get().await().exists()
        if (banned) throw IllegalStateException("접근이 제한된 방입니다.")

        val role = if (roomDoc.getString("ownerUid") == uid) "owner" else "member"
        val member = OpenChatMember(
            uid = uid, nickname = nickname, role = role,
            colorSeed = nickname.hashCode().xor(uid.hashCode()).xor(Random.nextInt()),
            joinedAt = System.currentTimeMillis()
        )
        val roomRef = rooms.document(roomId)
        roomRef.collection("members").document(uid).set(member, SetOptions.merge()).await()
        roomRef.update("memberIds", FieldValue.arrayUnion(uid)).await()
        return true
    }

    /** 나가기: 방장은 바로 나갈 수 없도록 금지(양도 후에만) */
    suspend fun leaveRoom(roomId: String, uid: String) {
        val roomSnap = rooms.document(roomId).get().await()
        require(roomSnap.exists()) { "방이 존재하지 않습니다." }
        val ownerUid = roomSnap.getString("ownerUid")
        if (ownerUid == uid) {
            throw IllegalStateException("방장은 나가기 전에 방장을 다른 멤버에게 양도해야 합니다.")
        }
        rooms.document(roomId).collection("members").document(uid).delete().await()
        rooms.document(roomId).update("memberIds", FieldValue.arrayRemove(uid)).await()
    }

    /** 방장 양도: oldOwner -> member, newOwner -> owner */
    suspend fun transferOwnership(roomId: String, newOwnerUid: String) {
        val roomRef = rooms.document(roomId)
        db.runTransaction { tx ->
            val roomDoc = tx.get(roomRef)
            require(roomDoc.exists()) { "방이 존재하지 않습니다." }
            val oldOwnerUid = roomDoc.getString("ownerUid") ?: error("방장 정보 없음")
            require(oldOwnerUid != newOwnerUid) { "이미 해당 사용자가 방장입니다." }

            val newMemberRef = roomRef.collection("members").document(newOwnerUid)
            val oldMemberRef = roomRef.collection("members").document(oldOwnerUid)
            require(tx.get(newMemberRef).exists()) { "대상 사용자가 멤버가 아닙니다." }

            tx.update(roomRef, "ownerUid", newOwnerUid)
            tx.set(newMemberRef, mapOf("role" to "owner"), SetOptions.merge())
            tx.set(oldMemberRef, mapOf("role" to "member"), SetOptions.merge())
            null
        }.await()

        // 시스템 메시지: 새 방장 이름 표시
        val newOwnerName = users.document(newOwnerUid).get().await().getString("name") ?: "새 방장"
        sendSystem(roomId, "owner_transfer", newOwnerName)
    }

    /** 방 삭제: 오너만 가능. 모든 하위 컬렉션 삭제 후 룸 문서 삭제 */
    suspend fun deleteRoom(roomId: String, requesterUid: String) {
        val roomRef = rooms.document(roomId)
        val doc = roomRef.get().await()
        require(doc.exists()) { "방이 존재하지 않습니다." }
        val ownerUid = doc.getString("ownerUid")
        require(ownerUid == requesterUid) { "방장만 삭제할 수 있습니다." }

        // 하위 컬렉션 순차 삭제
        suspend fun deleteCollection(path: String, batchSize: Int = 300) {
            val coll = roomRef.collection(path)
            while (true) {
                val snap = coll.limit(batchSize.toLong()).get().await()
                if (snap.isEmpty) break
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }

        // 존재할 수 있는 모든 서브컬렉션 정리
        listOf("messages", "members", "bans", "events").forEach { deleteCollection(it) }

        // 마지막에 방 자체 삭제
        roomRef.delete().await()
    }

    /** 메시지 리스너 */
    fun listenMessages(roomId: String) {
        if (currentRoomId == roomId) return
        currentRoomId = roomId
        msgListener?.remove()
        msgListener = rooms.document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                _messages.value = snap?.documents?.mapNotNull { d ->
                    d.toObject(RoomMessage::class.java)?.copy(id = d.id)
                }.orEmpty()
            }
    }

    fun removeMessageListener() {
        msgListener?.remove()
        msgListener = null
        currentRoomId = null
    }

    /** 텍스트/이미지 메시지 */
    suspend fun sendText(roomId: String, uid: String, text: String) {
        val nickname = rooms.document(roomId).collection("members").document(uid).get().await()
            .getString("nickname") ?: "익명"
        val msgRef = rooms.document(roomId).collection("messages").document()
        val now = System.currentTimeMillis()
        val msg = RoomMessage(
            id = msgRef.id, senderUid = uid, senderNickname = nickname,
            text = text, type = "text", readBy = mapOf(uid to true), timestamp = now
        )
        msgRef.set(msg).await()
        rooms.document(roomId).update(mapOf("lastAt" to now, "lastMessage" to text.take(50))).await()
    }

    suspend fun sendImage(roomId: String, uid: String, imageUrl: String) {
        val nickname = rooms.document(roomId).collection("members").document(uid).get().await()
            .getString("nickname") ?: "익명"
        val msgRef = rooms.document(roomId).collection("messages").document()
        val now = System.currentTimeMillis()
        val msg = RoomMessage(
            id = msgRef.id, senderUid = uid, senderNickname = nickname,
            imageUrl = imageUrl, type = "image", readBy = mapOf(uid to true), timestamp = now
        )
        msgRef.set(msg).await()
        rooms.document(roomId).update(mapOf("lastAt" to now, "lastMessage" to "사진")).await()
    }

    /** 시스템 메시지 */
    suspend fun sendSystem(roomId: String, code: String, nickname: String?) {
        val text = when (code) {
            "join"   -> "${nickname ?: "누군가"}님이 입장했어요."
            "leave"  -> "${nickname ?: "누군가"}님이 퇴장했어요."
            "kick"   -> "${nickname ?: "사용자"}님이 강퇴되었습니다."
            "invite" -> "${nickname ?: "누군가"}님을 초대했어요."
            "notice" -> nickname ?: "공지사항이 업데이트되었습니다."
            "owner_transfer" -> "방장이 ${nickname ?: "새 방장"}님으로 변경되었어요."
            else     -> nickname ?: "시스템 알림"
        }
        val now = System.currentTimeMillis()
        val msgRef = rooms.document(roomId).collection("messages").document()
        msgRef.set(RoomMessage(id = msgRef.id, type = "system", systemCode = code, text = text, timestamp = now)).await()
        rooms.document(roomId).update(mapOf("lastAt" to now, "lastMessage" to text.take(50))).await()
    }

    /** 읽음 표시 */
    suspend fun markRead(roomId: String, uid: String, msgId: String) {
        rooms.document(roomId).collection("messages").document(msgId)
            .set(mapOf("readBy.$uid" to true), SetOptions.merge()).await()
    }

    fun markAllRead(roomId: String, uid: String, list: List<RoomMessage>) {
        viewModelScope.launch {
            val batch = db.batch()
            list.forEach { m ->
                if (m.type != "system" && m.readBy[uid] != true) {
                    val ref = rooms.document(roomId).collection("messages").document(m.id)
                    batch.set(ref, mapOf("readBy.$uid" to true), SetOptions.merge())
                }
            }
            batch.commit().await()
        }
    }

    /** 강퇴/밴 */
    suspend fun kickMember(roomId: String, targetUid: String) {
        val roomRef = rooms.document(roomId)
        roomRef.collection("members").document(targetUid).delete().await()
        roomRef.update("memberIds", FieldValue.arrayRemove(targetUid)).await()
        val targetName = users.document(targetUid).get().await().getString("name")
        sendSystem(roomId, "kick", targetName)
    }

    suspend fun banMember(roomId: String, targetUid: String, reason: String?) {
        val roomRef = rooms.document(roomId)
        roomRef.collection("bans").document(targetUid)
            .set(mapOf("reason" to (reason ?: ""), "bannedAt" to com.google.firebase.Timestamp.now()))
            .await()
        kickMember(roomId, targetUid) // ← 내부에서 system 처리
    }
    /** ---- 초대 플로우 (친구 채팅으로 초대 전송) ---- */

    // (1) id list로 유저들 이름 조회 (whereIn chunked)
    private suspend fun fetchUsersByIds(ids: List<String>): List<Pair<String, String>> {
        if (ids.isEmpty()) return emptyList()
        val result = mutableListOf<Pair<String, String>>()
        ids.chunked(10).forEach { chunk ->
            val snap = users.whereIn(FieldPath.documentId(), chunk).get().await()
            snap.documents.forEach { d ->
                val name = d.getString("name") ?: d.id.take(6)
                result += d.id to name
            }
        }
        return result
    }

    /** 친구 목록 */
    suspend fun fetchFriends(me: String): List<Friend> {
        val snap = users.document(me).collection("friends").get().await()
        val ids = snap.documents.map { it.id }
        val pairs = fetchUsersByIds(ids)
        return pairs.map { (uid, name) -> Friend(uid, name) }
    }

    /** DM 스레드ID(양쪽 동일) */
    private fun dmThreadId(a: String, b: String): String =
        if (a < b) "${a}_$b" else "${b}_$a"

    /** (2) 친구 채팅으로 오픈채팅 초대 메시지 전송 (멤버 추가는 하지 않음) */
    suspend fun sendInvitesAsDm(
        roomId: String,
        roomTitle: String,
        inviterUid: String,
        targetUids: List<String>
    ) {
        if (targetUids.isEmpty()) return

        val inviterName = users.document(inviterUid).get().await().getString("name") ?: "사용자"
        val deepLink = "foodtable://openchat?roomId=$roomId"
        val now = System.currentTimeMillis()

        targetUids.forEach { toUid ->
            val senderMsgRef = db.collection("user").document(inviterUid)
                .collection("chats").document(toUid)
                .collection("messages").document()
            val receiverMsgRef = db.collection("user").document(toUid)
                .collection("chats").document(inviterUid)
                .collection("messages").document(senderMsgRef.id)

            val payload = mapOf(
                "id" to senderMsgRef.id,
                "senderUid" to inviterUid,
                "type" to "openchat_invite",
                "text" to "${inviterName}님이 '$roomTitle' 오픈채팅에 초대했어요.",
                "timestamp" to now,
                "openchatRoomId" to roomId,
                "openchatTitle" to roomTitle,
                "deeplink" to deepLink,
                "read" to false
            )

            db.runBatch { b ->
                b.set(senderMsgRef, payload)
                b.set(receiverMsgRef, payload)
                // (선택) 채팅방 메타 업데이트 (최근 메시지/시간)
                val senderChatRef = senderMsgRef.parent.parent!! // user/inviterUid/chats/toUid
                val receiverChatRef = receiverMsgRef.parent.parent!! // user/toUid/chats/inviterUid
                b.set(senderChatRef, mapOf("lastAt" to now, "lastMessage" to payload["text"]), SetOptions.merge())
                b.set(receiverChatRef, mapOf("lastAt" to now, "lastMessage" to payload["text"]), SetOptions.merge())
            }.await()

            // (선택) 푸시 발송: 너의 DM 코드에 있는 callSendChat() 재사용 가능
            // callSendChat(toUid = toUid, chatUid = inviterUid, title = roomTitle, body = payload["text"] as String?)
        }

        // 방 내부 공지(“A, B님을 초대했어요.”)
        val targetNames = fetchUsersByIds(targetUids).joinToString(", ") { it.second }
        sendSystem(roomId, "invite", targetNames)
    }
    override fun onCleared() {
        super.onCleared()
        discoverListener?.remove()
        myRoomsListener?.remove()
        msgListener?.remove()
    }


}
