package com.bcu.foodtable.di

import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChannelRepository(private val db: FirebaseFirestore) {

    suspend fun getChannelByName(channelName: String): Channel? {
        val result = db.collection("channel")
            .whereEqualTo("name", channelName)
            .get()
            .await()

        return result.documents.firstOrNull()?.toObject(Channel::class.java)
    }

    suspend fun getRecipesByChannel(channelName: String): List<RecipeItem> {
        val result = db.collection("recipe")
            .whereEqualTo("contained_channel", channelName)
            .get()
            .await()

        return result.documents.mapNotNull { doc ->
            doc.toObject(RecipeItem::class.java)?.apply { id = doc.id }
        }
    }

    suspend fun isUserSubscribed(channelName: String, userId: String): Boolean {
        val result = db.collection("channel_subscribe")
            .whereEqualTo("userId", userId)
            .whereEqualTo("channel", channelName)
            .get()
            .await()

        return !result.isEmpty
    }

    suspend fun toggleUserSubscription(channelName: String, userId: String): Boolean {
        val subRef = db.collection("channel_subscribe")

        val result = subRef
            .whereEqualTo("userId", userId)
            .whereEqualTo("channel", channelName)
            .get()
            .await()

        return if (!result.isEmpty) {
            val docId = result.documents.first().id
            subRef.document(docId).delete().await()
            updateChannelSubscriberCount(channelName, -1)
            false
        } else {
            val data = mapOf(
                "userId" to userId,
                "channel" to channelName,
                "date" to Timestamp.now()
            )
            subRef.add(data).await()
            updateChannelSubscriberCount(channelName, 1)
            true
        }
    }

    suspend fun updateChannelSubscriberCount(channelName: String, delta: Int): Int {
        val result = db.collection("channel")
            .whereEqualTo("name", channelName)
            .get()
            .await()

        val doc = result.documents.firstOrNull() ?: return 0
        val ref = doc.reference
        val current = doc.getLong("subscribers") ?: 0
        val updated = (current + delta).coerceAtLeast(0)
        ref.update("subscribers", updated).await()
        return updated.toInt()
    }

    suspend fun getSubscribedChannels(userId: String): List<Channel> {
        val subsSnapshot = db.collection("channel_subscribe")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        val channels = mutableListOf<Channel>()
        for (doc in subsSnapshot.documents) {
            val channelName = doc.getString("channel") ?: continue
            val channelSnapshot = db.collection("channel")
                .whereEqualTo("name", channelName)
                .limit(1)
                .get()
                .await()
            channelSnapshot.documents.firstOrNull()?.toObject(Channel::class.java)?.let {
                channels.add(it)
            }
        }
        return channels
    }

    suspend fun getMyChannels(userId: String): List<Channel> {
        val result = db.collection("channel")
            .whereEqualTo("owner", userId)
            .get()
            .await()

        return result.documents.mapNotNull { it.toObject(Channel::class.java) }
    }

    suspend fun getRecommendedChannels(): List<Channel> {
        val result = db.collection("channel")
            .orderBy("subscribers", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        return result.documents.mapNotNull { it.toObject(Channel::class.java) }
    }
}
