package com.bcu.foodtable.ui.subscribeNavMenu


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.UserManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class SubscribeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val userId = UserManager.getUser()?.uid ?: ""

    private val _subscribedChannels = MutableLiveData<List<Channel>>()
    val subscribedChannels: LiveData<List<Channel>> = _subscribedChannels

    private val _myChannels = MutableLiveData<List<Channel>>()
    val myChannels: LiveData<List<Channel>> = _myChannels

    private val _recommendedChannels = MutableLiveData<List<Channel>>()
    val recommendedChannels: LiveData<List<Channel>> = _recommendedChannels

    fun fetchSubscribedChannels() {
        db.collection("channel_subscribe")
            .whereEqualTo("userId", userId)
            .orderBy("date")
            .limit(40)
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

