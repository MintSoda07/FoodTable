package com.bcu.foodtable.useful

import com.google.firebase.Timestamp

data class AIChatting(
    val content: String="",
    val chatDate: Timestamp?=null,
    val uid:String=""
)
