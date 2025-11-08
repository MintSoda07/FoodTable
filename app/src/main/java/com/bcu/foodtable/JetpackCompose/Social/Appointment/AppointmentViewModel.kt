package com.bcu.foodtable.JetpackCompose.Social.Appointment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.JetpackCompose.Social.ChatMessage
import com.bcu.foodtable.JetpackCompose.Social.sendMessage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AppointmentViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    /** 내 약속 리스트 실시간 (수락한 것만) */
    fun listenMyAppointments(
        uid: String,
        onChanged: (List<Appointment>) -> Unit
    ): ListenerRegistration {
        return db.collection("appointments")
            .whereArrayContains("acceptedIds", uid)
            .orderBy("startAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    // 인덱스 미구성 시 1회성 폴백 조회
                    if (e is FirebaseFirestoreException &&
                        e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    ) {
                        db.collection("appointments")
                            .whereArrayContains("acceptedIds", uid)
                            .get()
                            .addOnSuccessListener { s2 ->
                                val list = s2.documents.mapNotNull {
                                    it.toObject(Appointment::class.java)?.copy(id = it.id)
                                }.sortedBy { it.startAt }
                                onChanged(list)
                            }
                        return@addSnapshotListener
                    }
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull {
                    it.toObject(Appointment::class.java)?.copy(id = it.id)
                }.orEmpty()
                onChanged(list)
            }
    }

    /**
     * 약속 생성 + 참가자 기록 + 선택한 대상(DM/오픈채팅)으로 카드 전송
     * - 생성자는 자동 수락(acceptedIds에 포함)
     * - 전송 모두 실패 시 롤백
     */
    suspend fun createAndInvite(
        creatorUid: String,
        ap: Appointment,
        dmTargets: List<String>,
        openchatRoomId: String?
    ): String {
        //  수신자 정제: 공백 제거 + 중복 제거
        val sanitizedDmTargets = dmTargets.filter { it.isNotBlank() }.distinct()

        // 보낼 대상이 하나도 없으면 생성하지 않음
        require(sanitizedDmTargets.isNotEmpty() || !openchatRoomId.isNullOrBlank()) {
            "보낼 친구나 오픈채팅을 최소 1개 선택하세요."
        }

        val ref = db.collection("appointments").document()
        val now = System.currentTimeMillis()

        val allParticipants = (sanitizedDmTargets + creatorUid).distinct()
        val save = ap.copy(
            id = ref.id,
            creatorUid = creatorUid,
            createdAt = now,
            participantIds = allParticipants,     // 오픈채팅 멤버는 수락 시점에 participants에 추가 가능
            openchatRoomId = openchatRoomId,
            acceptedIds = listOf(creatorUid)      //  생성자 자동 수락(홈 노출)
        )

        // 1) 약속 본문 저장
        ref.set(save).await()

        // (선택) 감사 로그: 누구에게 보냈는지 기록
        ref.set(
            mapOf(
                "invitedDmIds" to sanitizedDmTargets,
                "invitedOpenRoomId" to (openchatRoomId ?: "")
            ),
            SetOptions.merge()
        ).await()

        // 2) 참가자 서브컬렉션 저장: 생성자 accepted, 초대 대상 pending
        val batch = db.batch()
        batch.set(
            ref.collection("participants").document(creatorUid),
            AppointmentParticipant(
                uid = creatorUid,
                role = "owner",
                status = "accepted",
                respondedAt = Timestamp.now()
            )
        )
        sanitizedDmTargets.forEach { uid ->
            batch.set(
                ref.collection("participants").document(uid),
                AppointmentParticipant(uid = uid, role = "member", status = "pending")
            )
        }
        batch.commit().await()

        // 3) 채팅으로 초대카드 전송
        val deeplink = "foodtable://appointment?id=${ref.id}"
        var sentCount = 0

        // 3-1) DM 전송
        for (toUid in sanitizedDmTargets) {
            try {
                android.util.Log.d("ApptInvite", "DM send -> to=$toUid, appt=${ref.id}")
                sendAppointmentDm(
                    fromUid = creatorUid,
                    toUid = toUid,
                    ap = save,
                    deeplink = deeplink
                )
                sentCount++
            } catch (e: Exception) {
                android.util.Log.e("ApptInvite", "DM 전송 실패: to=$toUid, appt=${ref.id}", e)
            }
        }

        // 3-2) 오픈채팅 전송
        if (!openchatRoomId.isNullOrBlank()) {
            try {
                android.util.Log.d("ApptInvite", "OpenChat send -> room=$openchatRoomId, appt=${ref.id}")
                sendAppointmentToOpenChat(
                    roomId = openchatRoomId,
                    senderUid = creatorUid,
                    ap = save,
                    deeplink = deeplink
                )
                sentCount++
            } catch (e: Exception) {
                android.util.Log.e("ApptInvite", "오픈채팅 전송 실패: room=$openchatRoomId, appt=${ref.id}", e)
            }
        }

        // 4)  모든 전송 실패 시 롤백 (약속/참여자/디바이스매핑 삭제)
        if (sentCount == 0) {
            suspend fun purge(path: String) {
                while (true) {
                    val s = ref.collection(path).limit(300).get().await()
                    if (s.isEmpty) break
                    val b = db.batch()
                    s.documents.forEach { b.delete(it.reference) }
                    b.commit().await()
                }
            }
            try {
                purge("participants")
                purge("deviceEvents")
            } finally {
                ref.delete().await()
            }
            throw IllegalStateException("초대 메시지 전송에 실패했습니다. 네트워크 상태를 확인한 뒤 다시 시도해주세요.")
        }

        return ref.id
    }

    /** UI에서 부를 비-suspend 래퍼: 코루틴에서 createAndInvite 호출 */
    fun createAndInviteAsync(
        creatorUid: String,
        ap: Appointment,
        dmTargets: List<String>,
        openchatRoomId: String?,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch { // 기본 Main
            try {
                // 무거운 일은 IO에서
                val id = withContext(Dispatchers.IO) {
                    createAndInvite(creatorUid, ap, dmTargets, openchatRoomId)
                }
                // 콜백은 Main
                onResult(Result.success(id))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    /** DM 쪽 약속 카드 전송 */
    private suspend fun sendAppointmentDm(
        fromUid: String,
        toUid: String,
        ap: Appointment,
        deeplink: String
    ) {
        val msg = ChatMessage(
            senderUid = fromUid,
            type = "appointment",          //  버블 분기용 타입
            text = ap.title,
            placeName = ap.placeName,
            placeUrl = ap.placeUrl,
            deeplink = deeplink,
            timestamp = System.currentTimeMillis(),
            appointmentId = ap.id          //  파서/네비용 ID
        )
        sendMessage(db, fromUid, toUid, msg)
    }

    /** 오픈채팅 약속 카드 전송 */
    private suspend fun sendAppointmentToOpenChat(
        roomId: String,
        senderUid: String,
        ap: Appointment,
        deeplink: String
    ) {
        val now = System.currentTimeMillis()
        val roomRef = db.collection("openRooms").document(roomId)
        val msgRef = roomRef.collection("messages").document()

        val payload = mapOf(
            "id" to msgRef.id,
            "senderUid" to senderUid,
            "type" to "appointment",
            "text" to ap.title,
            "placeName" to ap.placeName,
            "placeUrl" to (ap.placeUrl ?: ""),
            "deeplink" to deeplink,
            "timestamp" to now,
            "readBy" to mapOf(senderUid to true),
            "appointmentId" to ap.id
        )

        db.runBatch { b ->
            b.set(msgRef, payload)
            b.update(roomRef, mapOf("lastAt" to now, "lastMessage" to "[약속] ${ap.title}"))
        }.await()
    }

    /** 수락 → participants.status 업데이트 + acceptedIds에 추가(홈 노출) - 원자화 */
    suspend fun accept(apptId: String, uid: String) {
        val ref = db.collection("appointments").document(apptId)
        db.runBatch { b ->
            val partRef = ref.collection("participants").document(uid)
            b.set(
                partRef,
                mapOf(
                    // ✅ 문서 없던 케이스도 대비: uid/role을 함께 기록
                    "uid" to uid,
                    "role" to "member",
                    "status" to "accepted",
                    "respondedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            // ✅ 헤더 배열도 업데이트 (오픈채팅 수락 시 필수)
            b.update(
                ref,
                mapOf(
                    "acceptedIds" to FieldValue.arrayUnion(uid),
                    "participantIds" to FieldValue.arrayUnion(uid)
                )
            )
        }.await()
    }

    /** 거절 → participants upsert + acceptedIds에서 제거 (원자적) */
    suspend fun decline(apptId: String, uid: String) {
        val ref = db.collection("appointments").document(apptId)
        db.runBatch { b ->
            val partRef = ref.collection("participants").document(uid)
            b.set(
                partRef,
                mapOf(
                    "uid" to uid,
                    "role" to "member",
                    "status" to "declined",
                    "respondedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            b.update(ref, "acceptedIds", FieldValue.arrayRemove(uid))
            // participantIds는 이력 유지 목적이면 그대로 두고,
            // 완전히 제외하고 싶으면 아래 줄도 추가:
            // b.update(ref, "participantIds", FieldValue.arrayRemove(uid))
        }.await()
    }

    /** 생성자 취소(하위 컬렉션 정리) */
    suspend fun cancel(apptId: String, requesterUid: String) {
        val ref = db.collection("appointments").document(apptId)
        val snap = ref.get().await()
        require(snap.exists()) { "약속이 존재하지 않습니다." }
        require(snap.getString("creatorUid") == requesterUid) { "생성자만 삭제할 수 있습니다." }

        val title = snap.getString("title") ?: "약속"
        val participantIds = (snap.get("participantIds") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        val dmTargets = participantIds.filter { it != requesterUid } // 본인 제외
        val openRoomId = snap.getString("openchatRoomId")

        // 1) 알림 보내기(최대한 시도하되 실패해도 삭제는 진행)
        runCatching {
            val nowText = "[약속 취소] '$title' 약속이 생성자에 의해 취소되었습니다."
            val nowTs = System.currentTimeMillis()

            // 1-1) 참가자 DM 안내
            for (toUid in dmTargets) {
                val msg = ChatMessage(
                    senderUid = requesterUid,
                    text = nowText,
                    type = "text",
                    timestamp = nowTs
                )
                sendMessage(db, requesterUid, toUid, msg)
            }

            // 1-2) 오픈채팅 시스템 안내
            if (!openRoomId.isNullOrBlank()) {
                sendOpenChatSystem(openRoomId, "[약속 취소] $title")
            }
        }

        // 2) 서브컬렉션 정리 후 본문 삭제
        suspend fun purge(path: String) {
            while (true) {
                val s = ref.collection(path).limit(300).get().await()
                if (s.isEmpty) break
                val b = db.batch()
                s.documents.forEach { b.delete(it.reference) }
                b.commit().await()
            }
        }

        purge("participants")
        purge("deviceEvents") // (주의) 다른 사람 기기 캘린더 이벤트는 앱 클라이언트가 직접 지워줘야 함
        ref.delete().await()
    }

    /** 오픈채팅 시스템 메시지 간단 발송(뷰모델 외부 의존 없음) */
    private suspend fun sendOpenChatSystem(roomId: String, text: String) {
        val now = System.currentTimeMillis()
        val roomRef = db.collection("openRooms").document(roomId)
        val msgRef = roomRef.collection("messages").document()
        val payload = mapOf(
            "id" to msgRef.id,
            "type" to "system",
            "text" to text,
            "timestamp" to now,
            "readBy" to emptyMap<String, Boolean>()
        )
        db.runBatch { b ->
            b.set(msgRef, payload)
            b.update(roomRef, mapOf("lastAt" to now, "lastMessage" to text.take(50)))
        }.await()
    }

    /** 단발 조회(탐색 등): 수락한 것만 */
    suspend fun fetchMyAppointments(myUid: String): List<Appointment> {
        val snap = db.collection("appointments")
            .whereArrayContains("acceptedIds", myUid)
            .orderBy("startAt", Query.Direction.ASCENDING)
            .get().await()
        return snap.documents.mapNotNull {
            it.toObject(Appointment::class.java)?.copy(id = it.id)
        }
    }

    /** 상세 실시간(헤더 + 참가자) — 어느 쪽이 먼저 와도 즉시 콜백 */
    fun listenAppointment(
        apptId: String,
        onData: (Appointment, List<AppointmentParticipant>) -> Unit
    ): ListenerRegistration {
        val ref = db.collection("appointments").document(apptId)
        var latest: Appointment? = null
        var parts: List<AppointmentParticipant> = emptyList()

        val r1 = ref.addSnapshotListener { d, _ ->
            latest = d?.toObject(Appointment::class.java)?.copy(id = d.id)
            latest?.let { onData(it, parts) }
        }
        val r2 = ref.collection("participants").addSnapshotListener { s, _ ->
            parts = s?.documents?.mapNotNull {
                it.toObject(AppointmentParticipant::class.java)
            }.orEmpty()
            latest?.let { onData(it, parts) }
        }

        return object : ListenerRegistration {
            override fun remove() {
                r1.remove()
                r2.remove()
            }
        }
    }
}
