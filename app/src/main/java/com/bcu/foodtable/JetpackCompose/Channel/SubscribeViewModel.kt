package com.bcu.foodtable.JetpackCompose.Channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.UserManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
        db.collection("channel_subscribe")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { subsSnapshot ->
                val channelList = mutableListOf<Channel>()
                val tasks = mutableListOf<Task<QuerySnapshot>>()

                for (doc in subsSnapshot.documents) {
                    val channelName = doc.getString("channel") ?: continue

                    val task = db.collection("channel")
                        .whereEqualTo("name", channelName)
                        .limit(1)
                        .get()

                    tasks.add(task)

                    task.addOnSuccessListener { query ->
                        val chDoc = query.documents.firstOrNull()
                        chDoc?.toObject(Channel::class.java)?.let { channelList.add(it) }
                    }
                }

                Tasks.whenAllComplete(tasks).addOnSuccessListener {
                    _subscribedChannels.value = channelList
                }
            }
    }

    fun fetchMyChannels() {
        db.collection("channel")
            .whereEqualTo("owner", userId)
            .orderBy("date")
            .get()
            .addOnSuccessListener { snapshot ->
                _myChannels.value = snapshot.documents.mapNotNull { it.toObject(Channel::class.java) }
            }
    }

    fun fetchRecommendedChannels() {
        db.collection("channel")
            .orderBy("subscribers", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                _recommendedChannels.value = snapshot.documents.mapNotNull { it.toObject(Channel::class.java) }
            }
    }
}
