package com.bcu.foodtable.ui.myPage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.UserManager
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ChannelCreationActivity : AppCompatActivity() {

    private lateinit var channelNameEditText: EditText
    private lateinit var channelDescriptionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var selectImageButton: Button
    private lateinit var imageView: ImageView
    private var selectedImageUri: Uri? = null

    private val imageRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_channel) // 레이아웃 파일 설정

        channelNameEditText = findViewById(R.id.channelNameEditText)
        channelDescriptionEditText = findViewById(R.id.channelDescriptionEditText)
        saveButton = findViewById(R.id.createChannelButton)
        selectImageButton = findViewById(R.id.buttonSelectImage)
        imageView = findViewById(R.id.imageView22)

        // 이미지 선택 버튼 클릭 시 갤러리 열기
        selectImageButton.setOnClickListener {
            openGallery()
        }

        // 채널 생성 버튼 클릭 시, 이미지가 있으면 이미지 업로드 후 채널 생성
        saveButton.setOnClickListener {
            val name = channelNameEditText.text.toString()
            val description = channelDescriptionEditText.text.toString()
            if (selectedImageUri != null) {
                uploadImageAndCreateChannel(name, description)
            } else {
                createChannel(name, description, null)
            }
        }
    }

    // 갤러리 열기
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, imageRequestCode)
    }

    // 갤러리에서 선택된 이미지 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imageRequestCode && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            imageView.setImageURI(selectedImageUri) // 이미지 미리보기
        }
    }

    // 이미지 업로드 후 채널 생성
    private fun uploadImageAndCreateChannel(channelName: String, channelDescription: String) {
        if (selectedImageUri != null) {
            val storage = FirebaseStorage.getInstance()
            val storageRef: StorageReference = storage.reference
            val imageRef = storageRef.child("channel_images/${System.currentTimeMillis()}.jpg")

            imageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        createChannel(channelName, channelDescription, imageUrl)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CreateChannel", "Error uploading image", e)
                    Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 채널 생성 함수
    private fun createChannel(channelName: String, channelDescription: String, imageUrl: String?) {
        val firestore = FirebaseFirestore.getInstance()
        val userId = UserManager.getUser()!!.uid

        val channel = hashMapOf(
            "name" to channelName,
            "description" to channelDescription,
            "owner" to userId,
            "subscribers" to 0,  // 구독자 수는 처음에 0으로 설정
            "date" to FieldValue.serverTimestamp(),
            "channel_main_image" to imageUrl // 이미지 URL 추가
        )

        firestore.collection("channel")
            .add(channel)
            .addOnSuccessListener { documentReference ->
                Log.d("CreateChannel", "Channel created with ID: ${documentReference.id}")
                finish()  // 채널 생성 후, 이전 페이지로 돌아가기
            }
            .addOnFailureListener { e ->
                Log.w("CreateChannel", "Error adding channel", e)
                Toast.makeText(this, "채널 생성 실패", Toast.LENGTH_SHORT).show()
            }
    }
}