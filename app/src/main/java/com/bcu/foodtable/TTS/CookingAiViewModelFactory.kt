package com.bcu.foodtable.TTS

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.functions.FirebaseFunctions

class CookingAiViewModelFactory(
    private val application: Application,
    private val firebaseFunctions: FirebaseFunctions
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CookingAiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CookingAiViewModel(application, firebaseFunctions) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}