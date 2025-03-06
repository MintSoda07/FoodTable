package com.bcu.foodtable.ui.subscribeNavMenu

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bcu.foodtable.R

class WriteActivity : AppCompatActivity() {
    class MainActivity : AppCompatActivity() {

        companion object {
            private const val IMAGE_REQUEST_CODE = 1000
        }

        private lateinit var editTextPost: EditText
        private lateinit var imageView: ImageView
        private lateinit var buttonSelectImage: Button
        private lateinit var buttonUpload: Button

        private var selectedImageUri: Uri? = null
        private var selectedBitmap: Bitmap? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_write)

            // UI 요소 초기화
            editTextPost = findViewById(R.id.editTextPost)
            imageView = findViewById(R.id.imageView)
            buttonSelectImage = findViewById(R.id.buttonSelectImage)
            buttonUpload = findViewById(R.id.buttonUpload)

            // 이미지 선택 버튼 클릭 이벤트
            buttonSelectImage.setOnClickListener {
                openImageSelector()
            }

            // 업로드 버튼 클릭 이벤트
            buttonUpload.setOnClickListener {
                uploadPost()
            }
        }

        // 갤러리에서 이미지 선택하기
        private fun openImageSelector() {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_REQUEST_CODE)
        }

        // 이미지 선택 결과 처리
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_REQUEST_CODE) {
                // 선택된 이미지 URI를 받아옴
                selectedImageUri = data?.data
                selectedImageUri?.let {
                    // ImageView에 이미지 표시
                    imageView.setImageURI(it)

                    // URI로부터 비트맵 객체 생성
                    selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                }
            }
        }

        // 글과 이미지를 서버에 업로드하는 기능 (예시)
        private fun uploadPost() {
            val postText = editTextPost.text.toString()

            if (postText.isEmpty()) {
                Toast.makeText(this, "글을 작성해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            if (selectedImageUri == null) {
                Toast.makeText(this, "이미지를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            // 업로드 로직 구현 (예: 서버로 텍스트와 이미지를 전송)
            // 여기에서는 임시로 Toast로 업로드 메시지 표시
            Toast.makeText(this, "업로드 완료", Toast.LENGTH_SHORT).show()

            // 실제 서버에 데이터를 전송하는 로직은 여기에 추가
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_write)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
    }
}