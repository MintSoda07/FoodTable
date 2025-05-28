package com.bcu.foodtable.useful

import com.google.firebase.Timestamp


data class CommunityPost(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val location: String = "",
    val visited: Int = 0,
    val likes: Int = 0,
    val bookmarks: Int = 0,
    val comments: Int = 0,
    val imageUrl: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
