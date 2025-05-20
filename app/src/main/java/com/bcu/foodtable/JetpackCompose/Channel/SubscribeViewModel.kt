package com.bcu.foodtable.JetpackCompose.Channel



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SubscribeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val userId = UserManager.getUser()?.uid ?: ""

    private val _subscribedChannels = MutableStateFlow<List<Channel>>(emptyList())
    val subscribedChannels: StateFlow<List<Channel>> = _subscribedChannels

    private val _myChannels = MutableStateFlow<List<Channel>>(emptyList())
    val myChannels: StateFlow<List<Channel>> = _myChannels

    private val _recommendedChannels = MutableStateFlow<List<Channel>>(emptyList())
    val recommendedChannels: StateFlow<List<Channel>> = _recommendedChannels

    fun fetchSubscribedChannels() {
        viewModelScope.launch {
            try {
                val subsSnapshot = db.collection("channel_subscribe")
                    .whereEqualTo("userId", userId)
                    .orderBy("date")
                    .limit(40)
                    .get()
                    .await()

                val channelList = mutableListOf<Channel>()

                for (doc in subsSnapshot.documents) {
                    val channelName = doc.getString("channel") ?: continue
                    val channelSnapshot = db.collection("channel")
                        .whereEqualTo("name", channelName)
                        .limit(1)
                        .get()
                        .await()

                    val channelDoc = channelSnapshot.documents.firstOrNull()
                    channelDoc?.toObject(Channel::class.java)?.let { channelList.add(it) }
                }

                _subscribedChannels.value = channelList
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun fetchMyChannels() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("channel")
                    .whereEqualTo("owner", userId)
                    .orderBy("date")
                    .get()
                    .await()

                _myChannels.value = snapshot.documents.mapNotNull { it.toObject(Channel::class.java) }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun fetchRecommendedChannels() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("channel")
                    .orderBy("subscribers", Query.Direction.DESCENDING)
                    .get()
                    .await()

                _recommendedChannels.value = snapshot.documents.mapNotNull { it.toObject(Channel::class.java) }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
