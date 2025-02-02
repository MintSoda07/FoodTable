package com.bcu.foodtable.useful

import com.google.firebase.Timestamp

data class SubscribeItem(
    val userId:String="",
    val channel:String="",
    val date:Timestamp = Timestamp.now()
)
