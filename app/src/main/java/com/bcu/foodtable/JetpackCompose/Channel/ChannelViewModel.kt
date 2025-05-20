package com.bcu.foodtable.JetpackCompose.Channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChannelViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState: StateFlow<ChannelUiState> = _uiState

    fun loadChannel(channelName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val result = db.collection("channel")
                    .whereEqualTo("name", channelName)
                    .get().await()

                result.documents.firstOrNull()?.toObject(Channel::class.java)?.let { channel ->
                    _uiState.update {
                        it.copy(
                            channel = channel,
                            subscriberCount = channel.subscribers ?: 0
                        )
                    }
                }
            } catch (e: Exception) {
                // 예외 로깅 등
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadRecipes(channelName: String) {
        viewModelScope.launch {
            try {
                val result = db.collection("recipe")
                    .whereEqualTo("contained_channel", channelName)
                    .get().await()

                val items = result.documents.mapNotNull { doc ->
                    doc.toObject(RecipeItem::class.java)?.apply { id = doc.id }
                }

                _uiState.update { it.copy(recipes = items) }
            } catch (e: Exception) {
                // 예외 로깅 등
            }
        }
    }

    fun checkSubscription(channelName: String, userId: String) {
        db.collection("channel_subscribe")
            .whereEqualTo("userId", userId)
            .whereEqualTo("channel", channelName)
            .get()
            .addOnSuccessListener { snapshot ->
                _uiState.update { it.copy(isSubscribed = !snapshot.isEmpty) }
            }
    }

    fun toggleSubscription(channelName: String, userId: String) {
        val subRef = db.collection("channel_subscribe")

        subRef.whereEqualTo("userId", userId)
            .whereEqualTo("channel", channelName)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val docId = snapshot.documents[0].id
                    subRef.document(docId).delete().addOnSuccessListener {
                        updateSubscriberCount(channelName, -1)
                        _uiState.update { it.copy(isSubscribed = false) }
                    }
                } else {
                    val data = mapOf(
                        "userId" to userId,
                        "channel" to channelName,
                        "date" to Timestamp.now()
                    )
                    subRef.add(data).addOnSuccessListener {
                        updateSubscriberCount(channelName, 1)
                        _uiState.update { it.copy(isSubscribed = true) }
                    }
                }
            }
    }

    private fun updateSubscriberCount(channelName: String, delta: Int) {
        db.collection("channel")
            .whereEqualTo("name", channelName)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.firstOrNull()?.let { doc ->
                    val ref = doc.reference
                    val current = doc.getLong("subscribers") ?: 0
                    val updated = (current + delta).coerceAtLeast(0)
                    ref.update("subscribers", updated)
                    _uiState.update { it.copy(subscriberCount = updated.toInt()) }
                }
            }
    }

}
