package com.bcu.foodtable.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bcu.foodtable.JetpackCompose.Subscribe.SubscribeViewModel
import com.google.firebase.firestore.FirebaseFirestore

class SubscribeViewModelFactory(
    private val db: FirebaseFirestore,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscribeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscribeViewModel(db, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
