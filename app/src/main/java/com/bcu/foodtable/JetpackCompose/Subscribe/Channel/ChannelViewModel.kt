package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
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

    private val _isSubscribed = MutableStateFlow<Boolean?>(null)
    val isSubscribed: StateFlow<Boolean?> = _isSubscribed

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var channelDocId: String? = null

    fun loadAll(channelName: String, userId: String) {
        viewModelScope.launch {
            if (channelName.isBlank()) {
                Log.e("ChannelViewModel", "channelName이 비어있어 로딩 중단")
                _channel.value = null
                _isLoading.value = false
                return@launch
            }

            _isLoading.value = true
            try {
                val snapshot = Firebase.firestore
                    .collection("channel")
                    .whereEqualTo("name", channelName)
                    .limit(1)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    val channel = doc.toObject(Channel::class.java)
                    _channel.value = channel
                    _subscriberCount.value = channel?.subscribers ?: 0
                    channelDocId = doc.id

                    channel?.name?.let { loadRecipes(it) }

                    if (userId.isNotBlank()) {
                        checkSubscription(channelName, userId)
                    }

                    loadSubscriberCount(channelName)
                } else {
                    Log.e("ChannelViewModel", "채널 name=$channelName 문서 없음")
                    _channel.value = null
//
                }
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "loadAll 오류: ${e.message}")
            } finally {
                _isLoading.value = false
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
        val db = Firebase.firestore
        val subscriptionRef = db.collection("user")
            .document(userId)
            .collection("subscriptions")
            .document(channelName)

        viewModelScope.launch {
            try {
                val snapshot = subscriptionRef.get().await()
                _isSubscribed.value = snapshot.exists()
            } catch (e: Exception) {
                Log.e("checkSubscription", "구독 확인 실패: ${e.message}")
                _isSubscribed.value = false
            }
        }
    }



    // 구독자 수 실시간 로딩
    fun loadSubscriberCount(channelName: String) {
        viewModelScope.launch {
            val snapshot = Firebase.firestore
                .collection("channel")
                .whereEqualTo("name", channelName)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val docId = snapshot.documents.first().id
                Firebase.firestore.collection("channel")
                    .document(docId)
                    .addSnapshotListener { docSnapshot, _ ->
                        val count = docSnapshot?.getLong("subscribers")?.toInt() ?: 0
                        _subscriberCount.value = count
                    }
            }
        }
    }


    // 구독/구독취소 토글
    fun toggleSubscription(channelName: String, userId: String) {
        val db = Firebase.firestore
        val subscriptionRef = db.collection("user")
            .document(userId)
            .collection("subscriptions")
            .document(channelName)

        viewModelScope.launch {
            val now = isSubscribed.value
            val currentCount = subscriberCount.value

            // UI: 로딩 표시
            _isSubscribed.value = null
            val newValue = !(now ?: false)

            // 미리 UI 반영
            _isSubscribed.value = newValue
            _subscriberCount.value = if (newValue) currentCount + 1 else currentCount - 1

            try {
                if (newValue) {
                    subscriptionRef.set(
                        mapOf("subscribedAt" to Timestamp.now())
                    ).await()
                } else {
                    subscriptionRef.delete().await()
                }

                //  채널 구독자 수 업데이트
                val channelQuery = db.collection("channel")
                    .whereEqualTo("name", channelName)
                    .limit(1)
                    .get()
                    .await()

                channelQuery.documents.firstOrNull()?.reference?.update(
                    "subscribers", FieldValue.increment(if (newValue) 1 else -1)
                )

            } catch (e: Exception) {
                // 실패 시 복구
                Log.e("ChannelViewModel", "구독 토글 실패: ${e.message}")
                _isSubscribed.value = now
                _subscriberCount.value = currentCount
            }
        }
    }




}
