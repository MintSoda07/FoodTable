package com.bcu.foodtable.JetpackCompose.Social.Appointment

data class Appointment(
    val id: String = "",
    val title: String = "약속",
    val creatorUid: String = "",
    val placeName: String = "",
    val placeUrl: String? = null,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val note: String = "",
    val openchatRoomId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val participantIds: List<String> = emptyList(),
    val acceptedIds: List<String> = emptyList(),
    val paidBy: List<String> = emptyList()
)

data class AppointmentParticipant(
    val uid: String = "",
    val role: String = "member",
    val status: String = "pending",
    val respondedAt: com.google.firebase.Timestamp? = null,
    val paidAt: com.google.firebase.Timestamp? = null
)

data class DeviceEventMapping(
    val calendarId: Long = -1,
    val eventId: Long = -1,
    val lastSyncAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
)