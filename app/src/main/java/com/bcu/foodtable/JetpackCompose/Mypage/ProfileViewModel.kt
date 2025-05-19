package com.bcu.foodtable.JetpackCompose.Mypage

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    var user by mutableStateOf(User())
        private set

    var isEditing by mutableStateOf(false)
        private set

    private var originalImageUrl: String = ""
    private var tempImageUri: Uri? = null

    init {
        loadUserData()
    }

    fun loadUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("user").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val loadedUser = document.toObject(User::class.java)
                    loadedUser?.let {
                        user = it
                        originalImageUrl = it.image ?: ""
                    }
                }
            }
            .addOnFailureListener {
                Log.e("ProfileViewModel", "사용자 정보 불러오기 실패", it)
            }
    }

    fun startEdit() {
        isEditing = true
    }

    fun cancelEdit() {
        isEditing = false
        tempImageUri = null
        user = user.copy(image = originalImageUrl)
    }

    fun saveChanges() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            FirebaseFirestore.getInstance().collection("user").document(uid)
                .update("description", user.description ?: "")
                .addOnSuccessListener {
                    Log.d("ProfileViewModel", "프로필 저장 성공")
                    isEditing = false
                }
                .addOnFailureListener {
                    Log.e("ProfileViewModel", "프로필 저장 실패", it)
                }
        }
    }

    fun pickImageFromGallery(activity: Activity) {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        activity.startActivityForResult(intent, 1000)
    }

    fun handleImageResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1000 && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            tempImageUri = uri
            uploadImageToFirebase(uri)
        }
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("user_profile_image/$uid.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateUserProfileImage(uid, downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Log.e("ProfileViewModel", "이미지 업로드 실패", it)
            }
    }

    private fun updateUserProfileImage(userId: String, imageUrl: String) {
        FirebaseFirestore.getInstance().collection("user").document(userId)
            .update("image", imageUrl)
            .addOnSuccessListener {
                originalImageUrl = imageUrl
                user = user.copy(image = imageUrl)
                Log.d("ProfileViewModel", "프로필 이미지 URL 저장 완료")
            }
            .addOnFailureListener {
                Log.e("ProfileViewModel", "이미지 URL 저장 실패", it)
            }
    }
}
