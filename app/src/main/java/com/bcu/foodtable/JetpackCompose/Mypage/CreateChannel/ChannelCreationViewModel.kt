package com.bcu.foodtable.JetpackCompose.Mypage.CreateChannel
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChannelCreationViewModel : ViewModel() {
    val channelName = MutableStateFlow("")
    val channelDescription = MutableStateFlow("")
    val selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedBackgroundUri = MutableStateFlow<Uri?>(null)
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    fun setImageUri(uri: Uri?) {
        selectedImageUri.value = uri
    }
    fun setBackgroundUri(uri: Uri?) { selectedBackgroundUri.value = uri }

    fun createChannel(context: Context, onSuccess: () -> Unit) {
        _isUploading.value = true

        val name = channelName.value
        val description = channelDescription.value
        val userId = UserManager.getUser()?.uid ?: return

        if (selectedImageUri.value != null) {
            val imageRef = storage.reference.child("channel_images/${System.currentTimeMillis()}.jpg")
            imageRef.putFile(selectedImageUri.value!!)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        uploadChannelData(name, description, userId, uri.toString(), uri.toString(), context, onSuccess)
                    }
                }
                .addOnFailureListener {
                    _isUploading.value = false
                    Toast.makeText(context, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
        } else {
            uploadChannelData(name, description, userId, null, null, context, onSuccess)
        }
    }

    private fun uploadChannelData(
        name: String,
        description: String,
        owner: String,
        imageUrl: String?,
        backgroundUrl: String?,
        context: Context,
        onSuccess: () -> Unit
    ) {
        val channel = hashMapOf(
            "name" to name,
            "description" to description,
            "owner" to owner,
            "subscribers" to 0,
            "date" to FieldValue.serverTimestamp(),
            "imageResId" to imageUrl,
            "backgroundResId" to backgroundUrl
        )

        firestore.collection("channel")
            .add(channel)
            .addOnSuccessListener {
                Toast.makeText(context, "채널이 생성되었습니다", Toast.LENGTH_SHORT).show()
                _isUploading.value = false
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(context, "채널 생성 실패", Toast.LENGTH_SHORT).show()
                _isUploading.value = false
            }
    }
}