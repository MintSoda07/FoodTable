package com.bcu.foodtable

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class InAppMessageEvent(
    val chatUid: String,
    val title: String,
    val body: String,
    val messageId: String
)

object InAppEvents {
    private val _newMessageFlow = MutableSharedFlow<InAppMessageEvent>(
        replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val newMessageFlow = _newMessageFlow.asSharedFlow()

    fun emitNewMessage(chatUid: String, title: String, body: String, messageId: String) {
        _newMessageFlow.tryEmit(InAppMessageEvent(chatUid, title, body, messageId))
    }
}
