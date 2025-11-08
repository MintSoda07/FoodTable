package com.bcu.foodtable.JetpackCompose.RecipeStorage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TrendRecipeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    val topRecipes = MutableStateFlow<List<RecipeItem>>(emptyList())
    val categoryTrends = MutableStateFlow<Map<String, Int>>(emptyMap())
    val channelInfo = MutableStateFlow<Map<String, Channel>>(emptyMap())
    val channelOwners = MutableStateFlow<Map<String, String>>(emptyMap())  // owner UID -> name
    private suspend fun fetchOwnerName(uid: String): String? {
        val snapshot = db.collection("user").document(uid).get().await()
        return snapshot.getString("name")
    }

    fun loadTrendData() {
        viewModelScope.launch {
            val recipeSnapshot = db.collection("recipe")
                .orderBy("clicked", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()

            val recipes = recipeSnapshot.documents.mapNotNull {
                it.toObject(RecipeItem::class.java)?.apply { id = it.id }
            }

            topRecipes.value = recipes

            // 채널 정보 로드 (Top 3)
            val channels = recipes.take(3).mapNotNull { it.contained_channel }.toSet()
            val channelMap = mutableMapOf<String, Channel>()
            val ownerMap = mutableMapOf<String, String>()  // Channel name -> ownerName

            for (channelName in channels) {
                val channelSnap = db.collection("channel")
                    .whereEqualTo("name", channelName)
                    .limit(1)
                    .get()
                    .await()

                val channel = channelSnap.documents.firstOrNull()?.toObject(Channel::class.java)
                if (channel != null) {
                    channelMap[channelName] = channel

                    val ownerName = fetchOwnerName(channel.owner)
                    ownerMap[channelName] = ownerName ?: "알 수 없음"
                }
            }
            channelInfo.value = channelMap
            channelOwners.value = ownerMap

            // 카테고리 분석
            val categoryCount = recipes.flatMap { it.C_categories }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .toMap()

            categoryTrends.value = categoryCount
        }
    }

}
