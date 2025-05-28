package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChannelViewModel : ViewModel() {

    private val _channel = MutableStateFlow<Channel?>(null)
    val channel: StateFlow<Channel?> = _channel

    private val _recipes = MutableStateFlow<List<RecipeItem>>(emptyList())
    val recipes: StateFlow<List<RecipeItem>> = _recipes

    private val _subscriberCount = MutableStateFlow(0)
    val subscriberCount: StateFlow<Int> = _subscriberCount

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed

    // 채널 정보 로드
    fun loadChannel(channelName: String) {
        viewModelScope.launch {
            try {
                val snapshot = Firebase.firestore
                    .collection("channel")
                    .document(channelName)
                    .get()
                    .await()

                if (snapshot.exists()) {
                    val channel = snapshot.toObject(Channel::class.java)
                    channel?.let {
                        _channel.value = it
                        _subscriberCount.value = it.subscribers
                    }
                }
            } catch (e: Exception) {
                // 예외 처리 (옵션)
                e.printStackTrace()
            }
        }
    }

    // 채널의 레시피 리스트 로드
    fun loadRecipes(channelName: String) {
        viewModelScope.launch {
            val recipes = FireStoreHelper.getRecipesForChannel(channelName)
            _recipes.value = recipes
        }
    }

    // 구독 상태 확인
    fun checkSubscription(channelName: String, userId: String) {
        val subscriptionRef = Firebase.firestore
            .collection("users")
            .document(userId)
            .collection("subscriptions")
            .document(channelName)

        viewModelScope.launch {
            val snapshot = subscriptionRef.get().await()
            _isSubscribed.value = snapshot.exists()
        }
    }

    // 구독자 수 실시간 로딩
    fun loadSubscriberCount(channelName: String) {
        Firebase.firestore.collection("channels")
            .document(channelName)
            .addSnapshotListener { snapshot, _ ->
                val count = snapshot?.getLong("subscriberCount")?.toInt() ?: 0
                _subscriberCount.value = count
            }
    }

    // 구독/구독취소 토글
    fun toggleSubscription(channelName: String, userId: String) {
        val subscriptionRef = Firebase.firestore
            .collection("users")
            .document(userId)
            .collection("subscriptions")
            .document(channelName)

        viewModelScope.launch {
            val snapshot = subscriptionRef.get().await()
            if (snapshot.exists()) {
                // 구독 해제
                subscriptionRef.delete().await()
                _isSubscribed.value = false
                Firebase.firestore.collection("channels")
                    .document(channelName)
                    .update("subscriberCount", FieldValue.increment(-1))
                _subscriberCount.value -= 1
            } else {
                // 구독 등록
                subscriptionRef.set(mapOf("subscribedAt" to Timestamp.now())).await()
                _isSubscribed.value = true
                Firebase.firestore.collection("channels")
                    .document(channelName)
                    .update("subscriberCount", FieldValue.increment(1))
                _subscriberCount.value += 1
            }
        }
    }
}
