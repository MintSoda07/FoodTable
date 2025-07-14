package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.util.Log
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
                    Log.i("AI ChatTest","채널 내채널 띄워주는중 , ${doc} 호출됨")
                    val channel = doc.toObject(Channel::class.java)
                    channel?.copy(documentId = doc.id)
                }.filterNotNull()
                _myChannels.value = channels
            }
    }

    fun saveRecipeToChannel(recipe: RecipeItem, selectedChannel: Channel) {
        val data = recipe.copy(contained_channel = selectedChannel.name)
        Log.i("AI ChatTest","saveRecipeToChannel 호출됨")
        db.collection("recipe")
            .add(data)
            .addOnSuccessListener { _saveSuccess.value = true }
            .addOnFailureListener { _saveSuccess.value = false }
    }

    fun resetSaveSuccess() {
        Log.i("AI ChatTest","resetSaveSuccess 호출됨")
        _saveSuccess.value = null
    }
}
