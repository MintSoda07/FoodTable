package com.bcu.foodtable.JetpackCompose.Channel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.di.ChannelRepository
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class ChannelViewModel(
    private val repository: ChannelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState: StateFlow<ChannelUiState> = _uiState

    fun loadChannel(channelName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val channel = repository.getChannelByName(channelName)
                channel?.let {
                    _uiState.update { state ->
                        state.copy(
                            channel = it,
                            subscriberCount = it.subscribers ?: 0
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "loadChannel error: ${e.message}", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadRecipes(channelName: String) {
        viewModelScope.launch {
            try {
                val items = repository.getRecipesByChannel(channelName)
                _uiState.update { it.copy(recipes = items) }
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "loadRecipes error: ${e.message}", e)
            }
        }
    }

    fun checkSubscription(channelName: String, userId: String) {
        viewModelScope.launch {
            try {
                val isSubscribed = repository.isUserSubscribed(channelName, userId)
                _uiState.update { it.copy(isSubscribed = isSubscribed) }
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "checkSubscription error: ${e.message}", e)
            }
        }
    }

    fun toggleSubscription(channelName: String, userId: String) {
        viewModelScope.launch {
            try {
                val subscribed = repository.toggleUserSubscription(channelName, userId)
                val delta = if (subscribed) 1 else -1
                val updatedCount = repository.updateChannelSubscriberCount(channelName, delta)
                _uiState.update {
                    it.copy(
                        isSubscribed = subscribed,
                        subscriberCount = updatedCount
                    )
                }
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "toggleSubscription error: ${e.message}", e)
            }
        }
    }
}
