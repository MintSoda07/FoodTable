package com.bcu.foodtable.JetpackCompose.Channel

import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem

data class ChannelUiState(
    val channel: Channel? = null,
    val recipes: List<RecipeItem> = emptyList(),
    val isSubscribed: Boolean = false,
    val subscriberCount: Int = 0,
    val isLoading: Boolean = false
) {

}
