package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import androidx.lifecycle.ViewModel
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RecipeSaveViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _myChannels = MutableStateFlow<List<Channel>>(emptyList())
    val myChannels: StateFlow<List<Channel>> = _myChannels

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess: StateFlow<Boolean?> = _saveSuccess

    fun loadMyChannels(userId: String) {
        db.collection("channel")
            .whereEqualTo("owner", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val channels = snapshot.documents.map { doc ->
                    val channel = doc.toObject(Channel::class.java)
                    channel?.copy(documentId = doc.id)
                }.filterNotNull()
                _myChannels.value = channels
            }
    }

    fun saveRecipeToChannel(recipe: RecipeItem, selectedChannel: Channel) {
        val data = recipe.copy(contained_channel = selectedChannel.name)
        db.collection("recipe")
            .add(data)
            .addOnSuccessListener { _saveSuccess.value = true }
            .addOnFailureListener { _saveSuccess.value = false }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = null
    }
}
