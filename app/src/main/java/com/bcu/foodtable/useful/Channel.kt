package com.bcu.foodtable.useful

import com.google.firebase.Timestamp

data class Channel(
    val name:String="",
    var description:String="",
    var imageResId:String="",
    var subscribers:Int=0,
    var BackgroundResId:String="",
    val date: Timestamp = Timestamp.now(),
)
