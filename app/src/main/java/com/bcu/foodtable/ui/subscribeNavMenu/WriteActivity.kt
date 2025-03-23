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
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FirebaseFirestore

class WriteActivity : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var buttonSelectImage: Button
    private lateinit var spinner1: Spinner // Spinner 추가
    private lateinit var spinner2: Spinner
    private lateinit var buttonUpload: Button
    private lateinit var itemImageView: ImageView

    private lateinit var addpageRecyclerViewStage : RecyclerView
    private lateinit var addpageStageNumber : TextView
    private lateinit var addpageTitleText : TextInputEditText
    private lateinit var addpageDescription : EditText
    private lateinit var addpageSwitchTimer : Switch

    private lateinit var addpageTimer1 : TextInputEditText
    private lateinit var addpageTimer2 : TextInputEditText
    private lateinit var addpageTimer3 : TextInputEditText

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
        editTextDescription = findViewById(R.id.making_des) // 상세 설명 입력칸
        buttonSelectImage = findViewById(R.id.buttonSelectImage)

        // 스피너 ID 가져오기
        spinner1 = findViewById(R.id.categorySpinner) // Food Types
        spinner2 = findViewById(R.id.categorySpinner2) // Cooking Methods
        buttonUpload = findViewById(R.id.buttonUpload)
        itemImageView = findViewById(R.id.imageView22)
        // 버튼 활성화
        buttonSelectImage.isEnabled = true

        addpageRecyclerViewStage = findViewById(R.id.AddPageStageListRecyclerView)
        addpageStageNumber = findViewById(R.id.AddPageStageNum)
        addpageDescription = findViewById(R.id.AddPageStageDescriptionText)

        addpageSwitchTimer = findViewById(R.id.timerSwitch)
        addpageTitleText = findViewById(R.id.AddPageStageTitleText)

        addpageTimer1 = findViewById(R.id.AddPageStageTimerHour)
        addpageTimer2 = findViewById(R.id.AddPageStageTimerMinute)
        addpageTimer3 = findViewById(R.id.AddPageStageTimerSecond)


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

        // 가져올 문서 목록 (문서 이름과 해당 데이터를 적용할 스피너 매핑)
        val categoryMapping = mapOf(
            "C_food_types" to spinner1,      // Food Types → Spinner 1
            "C_cooking_methods" to spinner2  // Cooking Methods → Spinner 2
        )

        for ((doc, spinner) in categoryMapping) {
            firestore.collection("C_categories")
                .document(doc)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val list = document.get("list") as? List<String>
                        if (!list.isNullOrEmpty()) {
                            // ArrayAdapter 생성 및 스피너에 적용
                            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list)
                            spinner.adapter = adapter
                        } else {
                            Toast.makeText(this, "$doc 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "$doc 로드 실패", exception)
                    Toast.makeText(this, "$doc 로드 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveRecipeToFirestore(imageUrl: String) {
        val firestore = FirebaseFirestore.getInstance()

        val title = editTextTitle.text.toString().trim() // 사용자가 입력한 제목
        val description = editTextDescription.text.toString().trim() // 사용자가 입력한 상세 설명
        val category1 = spinner1.selectedItem.toString() // 선택한 카테고리
        val category2 = spinner2.selectedItem.toString() // 선택한 카테고리

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
            "category" to category1, // 선택한 카테고리
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