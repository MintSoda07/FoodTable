package com.bcu.foodtable.ui.subscribeNavMenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.RecipeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChannelViewModel : ViewModel() {

    private val _channel = MutableStateFlow<Channel?>(null)
    val channel: StateFlow<Channel?> = _channel

    private val _recipes = MutableStateFlow<List<RecipeItem>>(emptyList())
    val recipes: StateFlow<List<RecipeItem>> = _recipes

    private val _subscriberCount = MutableStateFlow(0)
    val subscriberCount: StateFlow<Int> = _subscriberCount

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed

    fun loadChannel(channelName: String) {
        viewModelScope.launch {
            FireStoreHelper.getChannel(channelName)?.let {
                _channel.value = it
                _subscriberCount.value = it.subscribers
            }
        }
    }

    fun loadRecipes(channelName: String) {
        viewModelScope.launch {
            val recipes = FireStoreHelper.getRecipesForChannel(channelName)
            _recipes.value = recipes
        }
    }

    fun checkSubscription(channelName: String, userId: String) {
        viewModelScope.launch {
            val result = FireStoreHelper.isUserSubscribed(channelName, userId)
            _isSubscribed.value = result
        }
    }

    fun toggleSubscription(channelName: String, userId: String) {
        viewModelScope.launch {
            val nowSubscribed = _isSubscribed.value
            if (nowSubscribed) {
                FireStoreHelper.unsubscribeChannel(channelName, userId)
                _subscriberCount.value = (_subscriberCount.value - 1).coerceAtLeast(0)
            } else {
                FireStoreHelper.subscribeChannel(channelName, userId)
                _subscriberCount.value = _subscriberCount.value + 1
            }
            _isSubscribed.value = !nowSubscribed
        }
    }
}
