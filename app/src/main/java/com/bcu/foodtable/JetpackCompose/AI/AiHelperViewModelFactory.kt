package com.bcu.foodtable.JetpackCompose.AI

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bcu.foodtable.ai.OpenAIClient

class AiHelperViewModelFactory(
    private val apiClient: OpenAIClient
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiHelperViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiHelperViewModel(apiClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
