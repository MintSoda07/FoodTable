package com.bcu.foodtable.di


import com.google.firebase.firestore.FirebaseFirestore

class AppContainer {
    private val firestore = FirebaseFirestore.getInstance()
    val channelRepository = ChannelRepository(firestore)
}