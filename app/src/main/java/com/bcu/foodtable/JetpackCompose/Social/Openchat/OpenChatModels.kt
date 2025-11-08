// OpenChatModels.kt
package com.bcu.foodtable.JetpackCompose.Social.Openchat

data class OpenChatRoom(
    val id: String = "",
    val title: String = "",
    val desc: String = "",
    val ownerUid: String = "",
    val open: Boolean = true,
    val passcode: String? = null,
    val memberCount: Long = 1,
    val lastAt: Long = System.currentTimeMillis(),   // 정렬/탐색용(ms)
    val lastMessage: String? = null,                 // 리스트 미리보기
    val memberIds: List<String> = emptyList(),
    val thumbUrl: String = ""
)

data class OpenChatMember(
    val uid: String = "",
    val nickname: String = "",
    val role: String = "member",                     // "owner" | "member"
    val colorSeed: Int = 0,
    val joinedAt: Long = System.currentTimeMillis()
)

data class RoomMessage(
    val id: String = "",
    val type: String = "text",                       // "text" | "image" | "system"
    val text: String? = null,
    val imageUrl: String? = null,
    val senderUid: String = "",
    val senderNickname: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val systemCode: String? = null,
    val readBy: Map<String, Boolean> = emptyMap(),
    val recipeTitle: String? = null,
    val recipeThumb: String? = null,
    val deeplink: String? = null,
    // 약속 공유
    val placeName: String? = null,
    val placeUrl: String? = null,
    val appointmentId: String? = null

)

/** 친구 초대용 간단 모델 */
data class Friend(
    val uid: String = "",
    val name: String = ""
)
