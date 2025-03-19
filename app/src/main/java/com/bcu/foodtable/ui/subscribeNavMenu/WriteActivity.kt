package com.bcu.foodtable.ui.subscribeNavMenu

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bcu.foodtable.R
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FirebaseFirestore

class WriteActivity : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var buttonSelectImage: Button
    private lateinit var categorySpinner: Spinner // Spinner 추가
    private lateinit var buttonUpload: Button
    private lateinit var itemImageView: ImageView
    private lateinit var buttonBack: Button

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
        editTextTitle = findViewById(R.id.editTextTitle) // 제목 입력칸
        editTextDescription = findViewById(R.id.editTextDescription) // 상세 설명 입력칸
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        categorySpinner = findViewById(R.id.categorySpinner) // Spinner 초기화
        buttonUpload = findViewById(R.id.buttonUpload)
        itemImageView = findViewById(R.id.imageView22)
        buttonBack = findViewById(R.id.buttonBack)

        // 뒤로 가기 버튼 클릭 이벤트 (제대로 닫음)
        buttonBack.setOnClickListener {
            finish() // 현재 액티비티 종료하고 이전 화면으로 돌아감
        }

        // 버튼 활성화
        buttonSelectImage.isEnabled = true

        // 갤러리로 이동
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

        // 카테고리 데이터 Firestore에서 가져오기
        getCategoriesFromFirestore()  // 여기서 호출 추가
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun uploadImage(imageUri: Uri) {
        val storageReference = FirebaseStorage.getInstance().reference
        val imageName = "uploaded_${System.currentTimeMillis()}.jpg"
        val imageRef = storageReference.child("recipe_image/$imageName")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveRecipeToFirestore(uri.toString())  // 이미지 URL을 Firestore에 저장
                }
                Toast.makeText(this, "이미지 업로드 성공!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("Upload", "업로드 실패", exception)
                Toast.makeText(this, "업로드 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCategoriesFromFirestore() {
        val firestore = FirebaseFirestore.getInstance()

        // "C_categories" 컬렉션에서 카테고리 목록 가져오기
        firestore.collection("C_categories")
            .get()
            .addOnSuccessListener { result ->
                val categoriesList = mutableListOf<String>()

                // Firestore에서 가져온 데이터로 카테고리 목록 생성
                for (document in result) {
                    // 'name' 필드가 있는지 확인
                    val category = document.getString("name")
                    category?.let {
                        categoriesList.add(it)
                    }
                }

                // 카테고리가 있으면 Spinner에 설정
                if (categoriesList.isNotEmpty()) {
                    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categoriesList)
                    categorySpinner.adapter = adapter
                } else {
                    Toast.makeText(this, "카테고리 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "카테고리 로드 실패", exception)
                Toast.makeText(this, "카테고리 로드 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveRecipeToFirestore(imageUrl: String) {
        val firestore = FirebaseFirestore.getInstance()

        val title = editTextTitle.text.toString().trim() // 사용자가 입력한 제목
        val description = editTextDescription.text.toString().trim() // 사용자가 입력한 상세 설명
        val category = categorySpinner.selectedItem.toString() // 선택한 카테고리

        if (title.isEmpty()) {
            Toast.makeText(this, "레시피 제목을 입력해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "레시피 상세 설명을 입력해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        val recipeData = hashMapOf(
            "title" to title,  // 입력한 제목
            "description" to description, // 입력한 상세 설명
            "category" to category, // 선택한 카테고리
            "imageUrl" to imageUrl, // 업로드된 이미지 URL
            "timestamp" to System.currentTimeMillis() // 시간 기록
        )

        firestore.collection("recipes")
            .add(recipeData)
            .addOnSuccessListener {
                Toast.makeText(this, "레시피 Firestore 저장 완료!", Toast.LENGTH_SHORT).show()
                finish() // ✅ Firestore 저장도 성공하면 이전 화면으로 이동
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Firestore 저장 실패", exception)
                Toast.makeText(this, "Firestore 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }
}