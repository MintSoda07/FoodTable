package com.bcu.foodtable.ui.subscribeNavMenu

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.FireStoreHelper
import com.bumptech.glide.Glide

class WriteActivity : AppCompatActivity() {

    private lateinit var editTextPost: EditText
    private lateinit var buttonSelectImage: Button
    private lateinit var buttonUpload: Button
    private lateinit var itemImageView: ImageView

    private var selectedImageUri: Uri? = null
    var isMainImageUploaded = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(it)
                .centerCrop()
                .placeholder(R.drawable.baseline_menu_book_24)
                .error(R.drawable.dish_icon)
                .into(itemImageView)
            isMainImageUploaded = true
        } ?: run {
            Toast.makeText(this, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        // UI 요소 초기화
        editTextPost = findViewById(R.id.editTextPost)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        buttonUpload = findViewById(R.id.buttonUpload)
        itemImageView = findViewById(R.id.imageView22)

        // 버튼 활성화
        buttonSelectImage.isEnabled = true

        // 버튼 클릭 시 갤러리 이동
        buttonSelectImage.setOnClickListener {
            openGallery()
        }

        // 업로드 버튼 클릭 시 실행
        buttonUpload.setOnClickListener {
            selectedImageUri?.let { uri ->
                uploadImage(uri)
            } ?: run {
                Toast.makeText(this, "이미지를 선택해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun uploadImage(imageUri: Uri) {
        val imageName = "uploaded_${System.currentTimeMillis()}.jpg"
        val collectionName = "images"
        val folderName = "uploads"

        FireStoreHelper.uploadImage(
            imageUri = imageUri,
            imageName = imageName,
            collectionName = collectionName,
            folderName = folderName,
            onSuccess = { imageUrl ->
                Log.d("Upload", "업로드 성공: $imageUrl")
                Toast.makeText(this, "업로드 성공!", Toast.LENGTH_SHORT).show()
            },
            onFailure = { exception ->
                Log.e("Upload", "업로드 실패", exception)
                Toast.makeText(this, "업로드 실패", Toast.LENGTH_SHORT).show()
            }
        )
    }
}