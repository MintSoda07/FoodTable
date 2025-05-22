package com.bcu.foodtable.JetpackCompose.Mypage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.bcu.foodtable.PuchasePage
import com.bcu.foodtable.JetpackCompose.Mypage.Health.HealthConnectActivity
import com.bcu.foodtable.ui.myPage.ChannelCreationActivity
import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.FridgeActivity
import com.bcu.foodtable.useful.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    var editedDescription = MutableStateFlow("")
    // 바로 호출
    init {
        fetchUserData()
    }
    private val _user = MutableStateFlow(
        User(
            uid = auth.currentUser?.uid.orEmpty(),
            name = "이름 없음",
            description = "",
            image = "",
            point = 0
        )
    )
    val user: StateFlow<User> = _user

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri

    private val _hasChannel = MutableStateFlow(false)
    val hasChannel: StateFlow<Boolean> = _hasChannel
    // 파이어베이스로 이미지 업로드
    fun uploadImageToFirebase(uri: Uri, context: Context) {
        val uid = auth.currentUser?.uid ?: return
        val ref = storage.reference.child("user_profile_image/$uid.jpg")

        _imageUri.value = uri

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                    firestore.collection("user").document(uid)
                        .update("image", downloadUrl.toString())
                        .addOnSuccessListener {
                            _user.value = _user.value.copy(image = downloadUrl.toString())
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
    // 채널 생성할때 확인해주는 함수
    fun checkIfChannelExists() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("channel")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                _hasChannel.value = !snapshot.isEmpty
            }
            .addOnFailureListener {
                _hasChannel.value = false
            }
    }
    //수정 시작
    fun startEdit() {
        editedDescription.value = _user.value.description
        _isEditing.value = true
    }
    //수정 취소
    fun cancelEdit() {
        _isEditing.value = false
    }
    // 수정 바뀐거 저장
    fun saveChanges() {
        val uid = auth.currentUser?.uid ?: return
        val newDesc = editedDescription.value

        firestore.collection("user").document(uid)
            .update("description", newDesc)
            .addOnSuccessListener {
                _user.value = _user.value.copy(description = newDesc)
                _isEditing.value = false
            }
    }
    // 결제하기로 이동하는 함수...
    fun navigateToPurchase(context: Context) {
        context.startActivity(Intent(context, PuchasePage::class.java))
    }
    // 채널 생성으로 이동하는 함수...
    fun navigateToChannelCreation(context: Context) {
        context.startActivity(Intent(context, ChannelCreationActivity::class.java))
    }
    // 건강 확인으로 이동하는 함수...
    fun navigateToHealth(context: Context) {
        context.startActivity(Intent(context, HealthConnectActivity::class.java))
    }
    // 나의 냉장고로 이동하는 함수...
    fun navigateToFridge(context: Context) {
        context.startActivity(Intent(context, FridgeActivity::class.java))
    }
    // 유저 데이터 가져오는 함수...
    fun fetchUserData() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("user").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "이름 없음"
                    val description = document.getString("description") ?: ""
                    val image = document.getString("image") ?: ""
                    val point = document.getLong("point")?.toInt() ?: 0

                    _user.value = User(
                        uid = uid,
                        name = name,
                        description = description,
                        image = image,
                        point = point
                    )
                }
            }
            .addOnFailureListener {
                // 필요 시 에러 메시지 출력
            }
    }

}
