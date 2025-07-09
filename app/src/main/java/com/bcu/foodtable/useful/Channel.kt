package com.bcu.foodtable.useful

import com.google.firebase.Timestamp

data class Channel(
    val documentId: String = "",
    val name:String="",
    var description:String="",
    var imageResId:String="",
    var subscribers:Int=0,
    var backgroundResId:String="",
    val date: Timestamp = Timestamp.now(),
    val owner : String = "",
)
