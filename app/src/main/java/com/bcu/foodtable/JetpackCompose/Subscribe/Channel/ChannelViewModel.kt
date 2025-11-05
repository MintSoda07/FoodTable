package com.bcu.foodtable.JetpackCompose.Subscribe.Channel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChannelViewModel : ViewModel() {

    // 채널 정보
    private val _channel = MutableStateFlow<Channel?>(null)
    val channel: StateFlow<Channel?> = _channel

    // 레시피 리스트
    private val _recipes = MutableStateFlow<List<RecipeItem>>(emptyList())
    val recipes: StateFlow<List<RecipeItem>> = _recipes

    // 구독자 수
    private val _subscriberCount = MutableStateFlow(0)
    val subscriberCount: StateFlow<Int> = _subscriberCount

    // 현재 사용자가 구독 중인지
    private val _isSubscribed = MutableStateFlow<Boolean?>(null)
    val isSubscribed: StateFlow<Boolean?> = _isSubscribed

    // 전체 로딩 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 내부에서 다시 로딩할 때 사용할 채널 이름 저장
    private var currentChannelName: String = ""

    // Firestore 문서 ID (필요시)
    private var channelDocId: String? = null

    /**
     * 채널과 관련된 모든 데이터를 로드합니다.
     */
    fun loadAll(channelName: String, userId: String) {
        currentChannelName = channelName

        viewModelScope.launch {
            if (channelName.isBlank()) {
                Log.e("ChannelViewModel", "channelName이 비어있어 로딩 중단")
                _channel.value = null
                _isLoading.value = false
                return@launch
            }

            _isLoading.value = true
            try {
                // 1) 채널 문서 조회
                val snapshot = Firebase.firestore
                    .collection("channel")
                    .whereEqualTo("name", channelName)
                    .limit(1)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val doc = snapshot.documents.first()
                    val channelObj = doc.toObject(Channel::class.java)
                    _channel.value = channelObj
                    _subscriberCount.value = channelObj?.subscribers ?: 0
                    channelDocId = doc.id

                    // 2) 레시피 로드
                    channelObj?.name?.let { loadRecipes(it) }

                    // 3) 사용자 구독 상태 확인
                    if (userId.isNotBlank()) {
                        checkSubscription(channelName, userId)
                    }

                    // 4) 실시간 구독자 수 리스너
                    loadSubscriberCount(channelName)
                } else {
                    Log.e("ChannelViewModel", "채널(name=$channelName) 문서 없음")
                    _channel.value = null
                }
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "loadAll 오류: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 해당 채널의 레시피 목록을 가져와서 StateFlow에 반영합니다.
     */
    fun loadRecipes(channelName: String) {
        viewModelScope.launch {
            val list = FireStoreHelper.getRecipesForChannel(channelName)
            _recipes.value = list
        }
    }

    /**
     * 레시피 구매 처리 후, 다시 loadAll을 호출해 UI를 갱신합니다.
     */
    fun purchaseRecipe(item: RecipeItem, userId: String) {
        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("user")
                    .document(userId)
                    .collection("purchased")
                    .document(item.id)
                    .set(
                        mapOf(
                            "purchased" to true,
                            "price" to (item.cost ?: 0),
                            "ts" to FieldValue.serverTimestamp(),  // 스샷의 ts 필드
                            // 선택: 조회 편의를 위한 추가 메타
                            "recipeName" to item.name,
                            "channel" to item.contained_channel,
                            "image" to item.imageResId
                        )
                    )
                    .await()

                // 필요하면 기존처럼 리로드
                loadAll(currentChannelName, userId)
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "purchaseRecipe 실패: ${e.message}")
            }
        }
    }

    /**
     * 사용자의 해당 채널 구독 여부를 확인합니다.
     */
    fun checkSubscription(channelName: String, userId: String) {
        val subscriptionRef = Firebase.firestore
            .collection("user")
            .document(userId)
            .collection("subscriptions")
            .document(channelName)

        viewModelScope.launch {
            try {
                val snapshot = subscriptionRef.get().await()
                _isSubscribed.value = snapshot.exists()
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "checkSubscription 오류: ${e.message}")
                _isSubscribed.value = false
            }
        }
    }

    /**
     * 채널 문서에 실시간 리스너를 걸어 구독자 수를 업데이트합니다.
     */
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
                    .addSnapshotListener { docSnap, _ ->
                        val count = docSnap?.getLong("subscribers")?.toInt() ?: 0
                        _subscriberCount.value = count
                    }
            }
        }
    }

    /**
     * 구독/구독취소를 토글하고, UI에 미리 반영한 뒤 Firestore 업데이트를 수행합니다.
     */
    fun toggleSubscription(channelName: String, userId: String) {
        val subscriptionRef = Firebase.firestore
            .collection("user")
            .document(userId)
            .collection("subscriptions")
            .document(channelName)

        viewModelScope.launch {
            val wasSubscribed = isSubscribed.value ?: false
            val currentCount = subscriberCount.value

            // 로딩 상태 표시
            _isSubscribed.value = null

            // 반전된 구독 상태
            val newState = !wasSubscribed
            _isSubscribed.value = newState
            _subscriberCount.value = if (newState) currentCount + 1 else currentCount - 1

            try {
                if (newState) {
                    subscriptionRef.set(mapOf("subscribedAt" to Timestamp.now())).await()
                } else {
                    subscriptionRef.delete().await()
                }

                // 채널 컬렉션의 subscribers 필드 업데이트
                val channelQuery = Firebase.firestore
                    .collection("channel")
                    .whereEqualTo("name", channelName)
                    .limit(1)
                    .get()
                    .await()
                channelQuery.documents.firstOrNull()
                    ?.reference
                    ?.update("subscribers", FieldValue.increment(if (newState) 1 else -1))
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "toggleSubscription 실패: ${e.message}")
                // 실패 시 복구
                _isSubscribed.value = wasSubscribed
                _subscriberCount.value = currentCount
            }
        }
    }
}
