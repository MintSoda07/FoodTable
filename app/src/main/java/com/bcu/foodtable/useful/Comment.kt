package com.bcu.foodtable.useful

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val userId: String = "",
    val nickname: String = "",
    val content: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val likes: Int = 0,
    val pinned: Boolean = false,

)
data class Reply(
    val id: String = "",
    val userId: String = "",
    val nickname: String = "",
    val content: String = "",
    val createdAt: Timestamp = Timestamp.now()
)