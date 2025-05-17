package com.bcu.foodtable.ui.subscribeNavMenu

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChannelViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _channel = MutableLiveData<Channel>()
    val channel: LiveData<Channel> = _channel

    private val _recipes = MutableLiveData<List<RecipeItem>>()
    val recipes: LiveData<List<RecipeItem>> = _recipes

    private val _isSubscribed = MutableLiveData<Boolean>()
    val isSubscribed: LiveData<Boolean> = _isSubscribed

    private val _subscriberCount = MutableLiveData<Int>()
    val subscriberCount: LiveData<Int> = _subscriberCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    suspend fun loadChannel(channelName: String) {
        _isLoading.postValue(true)
        try {
            val result = db.collection("channel")
                .whereEqualTo("name", channelName)
                .get().await()

            result.documents.firstOrNull()?.toObject(Channel::class.java)?.let { channel ->
                _channel.postValue(channel)
                _subscriberCount.postValue(channel.subscribers ?: 0)
            }
        } catch (e: Exception) {
            Log.e("ViewModel", "채널 로딩 실패: ${e.message}")
        } finally {
            _isLoading.postValue(false)
        }
    }

    suspend fun loadRecipes(channelName: String) {
        try {
            val result = db.collection("recipe")
                .whereEqualTo("contained_channel", channelName)
                .get().await()

            val items = result.documents.mapNotNull { doc ->
                doc.toObject(RecipeItem::class.java)?.apply { id = doc.id }
            }
            _recipes.postValue(items)
        } catch (e: Exception) {
            Log.e("ViewModel", "레시피 로딩 실패: ${e.message}")
        }
    }

    fun checkSubscription(channelName: String, userId: String) {
        db.collection("channel_subscribe")
            .whereEqualTo("userId", userId)
            .whereEqualTo("channel", channelName)
            .get()
            .addOnSuccessListener { snapshot ->
                _isSubscribed.value = !snapshot.isEmpty
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
                        _isSubscribed.value = false
                    }
                } else {
                    val data = mapOf(
                        "userId" to userId,
                        "channel" to channelName,
                        "date" to Timestamp.now()
                    )
                    subRef.add(data).addOnSuccessListener {
                        updateSubscriberCount(channelName, 1)
                        _isSubscribed.value = true
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
                    _subscriberCount.value = updated.toInt()
                }
            }
    }
}
