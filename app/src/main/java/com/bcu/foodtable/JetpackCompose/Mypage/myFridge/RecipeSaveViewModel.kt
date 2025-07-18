package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.util.Log
import androidx.lifecycle.ViewModel
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

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
                    Log.i("AI ChatTest", "채널 내채널 띄워주는중 , ${doc} 호출됨")
                    val channel = doc.toObject(Channel::class.java)
                    channel?.copy(documentId = doc.id)
                }.filterNotNull()
                _myChannels.value = channels
            }
    }

    suspend fun saveRecipeToChannel(recipe: RecipeItem, selectedChannel: Channel, userId: String) {
        // 1. imageResId가 이미 downloadUrl이면 그대로, 아니면 downloadUrl을 받아오기
        val imageResId = recipe.imageResId
        val isUrl = imageResId.startsWith("https://")
        val finalImageUrl = if (isUrl) {
            imageResId
        } else {
            // Storage에서 downloadUrl 얻어오기
            val storageRef = FirebaseStorage.getInstance().reference.child(imageResId)
            storageRef.downloadUrl.await().toString()
        }

        // 2. downloadUrl로 imageResId를 대체하여 복사
        val data = recipe.copy(
            contained_channel = selectedChannel.name,
            imageResId = finalImageUrl
        )
        Log.i("AI ChatTest", "saveRecipeToChannel 호출됨, 저장될 imageResId: $finalImageUrl")

        // 3. Firestore 저장
        db.collection("recipe")
            .add(data)
            .addOnSuccessListener { docRef ->
                // purchased 서브컬렉션 기록
                db.collection("user")
                    .document(userId)
                    .collection("purchased")
                    .document(docRef.id)
                    .set(mapOf("purchased" to true))
                    .addOnSuccessListener {
                        _saveSuccess.value = true
                    }
                    .addOnFailureListener {
                        _saveSuccess.value = true
                    }
            }
            .addOnFailureListener {
                _saveSuccess.value = false
            }
    }


    fun resetSaveSuccess() {
        Log.i("AI ChatTest", "resetSaveSuccess 호출됨")
        _saveSuccess.value = null
    }
}
