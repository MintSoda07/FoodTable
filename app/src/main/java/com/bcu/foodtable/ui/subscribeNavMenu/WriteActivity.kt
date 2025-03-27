package com.bcu.foodtable.ui.subscribeNavMenu

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.IngredientAdapter
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.FlexAdaptor
import com.bcu.foodtable.useful.RecipeDetailRecyclerAdaptor
import com.bcu.foodtable.useful.RecipeItem
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
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
    private lateinit var addpageStageButton :Button

    private lateinit var addpageCookingMethod : TextInputEditText
    private lateinit var addpageTimer1 : TextInputEditText
    private lateinit var addpageTimer2 : TextInputEditText
    private lateinit var addpageTimer3 : TextInputEditText

    private lateinit var addpageTimerBox : View
    private lateinit var addpageIngredientsBtn : Button
    private lateinit var addpageIngredientsText : TextInputEditText
    private lateinit var addpageIngredientsRecyclerView: RecyclerView

    private lateinit var addpageTagsListFlexBoxRecyclerView :RecyclerView
    private lateinit var addpageTagsInputTextField : TextInputEditText
    private lateinit var addpageTagsButton : Button

    private lateinit var note : TextView
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
        addpageStageButton = findViewById(R.id.AddPageStageAddBtn)
        addpageCookingMethod = findViewById(R.id.addpageCookingMethod)
        addpageTimer1 = findViewById(R.id.AddPageStageTimerHour)
        addpageTimer2 = findViewById(R.id.AddPageStageTimerMinute)
        addpageTimer3 = findViewById(R.id.AddPageStageTimerSecond)

        addpageIngredientsBtn = findViewById(R.id.ingre_btn)
        addpageIngredientsText = findViewById(R.id.ingre_textField)
        addpageIngredientsRecyclerView = findViewById(R.id.AddPageIngredientsItemList)

        note = findViewById(R.id.note_des)

        addpageTagsListFlexBoxRecyclerView = findViewById(R.id.AddPageItemTags)
        addpageTagsInputTextField = findViewById(R.id.tags_inputTextField)
        addpageTagsButton = findViewById(R.id.tags_inputButton)

        addpageTimerBox = findViewById(R.id.timerStage)
        val channelName= intent.getStringExtra("channel_name")
        fun setupNumberLimit(editText: TextInputEditText, maxValue: Int) {
            editText.inputType = InputType.TYPE_CLASS_NUMBER // 숫자 입력만 허용

            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.toString()?.let { text ->
                        if (text.isNotEmpty()) {
                            val num = text.toIntOrNull() ?: 0
                            if (num > maxValue) {
                                editText.setText(maxValue.toString()) // 최대값으로 설정
                                editText.setSelection(editText.text!!.length) // 커서 이동
                            }
                        }
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }

        setupNumberLimit(addpageTimer1, 23)  // 최대 23
        setupNumberLimit(addpageTimer2, 59)  // 최대 59
        setupNumberLimit(addpageTimer3, 59)  // 최대 59

        val layoutManager2 = FlexboxLayoutManager(this@WriteActivity).apply {
            flexDirection = FlexDirection.ROW   // 행(row) 방향으로 아이템 배치
            justifyContent = JustifyContent.FLEX_START // 아이템을 왼쪽 정렬
            flexWrap = FlexWrap.WRAP           // 줄바꿈 허용 (자동으로 아이템 크기 맞추기)
        }
        val tagList = mutableListOf<String>()
        val ingredientsList = mutableListOf<String>()
        val adapter = FlexAdaptor(tagList)
        val adapter2 = IngredientAdapter(ingredientsList)
        addpageTagsListFlexBoxRecyclerView.layoutManager = layoutManager2
        addpageTagsListFlexBoxRecyclerView.adapter = adapter

        addpageIngredientsRecyclerView.layoutManager  = LinearLayoutManager(this@WriteActivity)
        addpageIngredientsRecyclerView.adapter = adapter2

        addpageIngredientsBtn.setOnClickListener{
            if(!addpageIngredientsText.text.isNullOrEmpty()){
                adapter2.addItem(addpageIngredientsText.text.toString())
                addpageIngredientsText.setText("")
            }
        }
        addpageTagsButton.setOnClickListener{
            if(!addpageTagsInputTextField.text.isNullOrEmpty()){
                adapter.addItem("#${addpageTagsInputTextField.text.toString()}")
                addpageTagsInputTextField.setText("")
            }
        }
        fun getFormattedTime(): String {
            val hour = addpageTimer1.text?.toString()?.padStart(2, '0') ?: "00"
            val minute = addpageTimer2.text?.toString()?.padStart(2, '0') ?: "00"
            val second = addpageTimer3.text?.toString()?.padStart(2, '0') ?: "00"

            return "$hour:$minute:$second"
        }
        fun formatCookingStep(stepNumber: Int, title: String, description: String, cookingMethod: String?, time: String?): String {
            val timePart = if (cookingMethod != null && time != null) " ($cookingMethod,$time)" else ""
            return "$stepNumber.($title) $description$timePart"
        }
        val recipeSteps = mutableListOf<String>()
        var indexOfStage = 1
        val layoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW   // 행(row) 방향으로 아이템 배치
            justifyContent = JustifyContent.FLEX_START // 아이템을 왼쪽 정렬
            flexWrap = FlexWrap.WRAP           // 줄바꿈 허용 (자동으로 아이템 크기 맞추기)
        }

        addpageRecyclerViewStage.layoutManager = layoutManager
        var recipeItemAdaptor = RecipeDetailRecyclerAdaptor(
            mutableListOf(),
            this@WriteActivity
        ){ clickedPosition ->
        }
        addpageRecyclerViewStage.adapter = recipeItemAdaptor
        // Switch 상태 변경 리스너 설정
        addpageSwitchTimer.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                addpageTimerBox.visibility = View.VISIBLE  // Switch가 켜지면 보이기
            } else {
                addpageTimerBox.visibility = View.GONE     // Switch가 꺼지면 숨기기
            }
        }
        addpageStageButton.setOnClickListener{
            if(!addpageTitleText.text.isNullOrEmpty() && !addpageDescription.text.isNullOrEmpty() ){
                addpageStageNumber.text="$indexOfStage"
                if(addpageSwitchTimer.isChecked)
                {
                    recipeSteps.add(formatCookingStep(
                        stepNumber = indexOfStage,
                        title = sanitizeInput(addpageTitleText.text.toString()),
                        description = sanitizeInput(addpageDescription.text.toString()),
                        cookingMethod = sanitizeInput(addpageCookingMethod.text.toString()),
                        time = getFormattedTime()
                    ))
                    Log.d("RecipeStage",recipeSteps[indexOfStage-1])
                    recipeItemAdaptor.updateItems(recipeSteps)
                    Log.d("RecipeStage","Adaptor items  : ${recipeItemAdaptor.itemCount}, ItemList : $recipeSteps")
                }else{
                    recipeSteps.add(formatCookingStep(
                        stepNumber = indexOfStage,
                        title = sanitizeInput(addpageTitleText.text.toString()),
                        description = sanitizeInput(addpageDescription.text.toString()),
                        cookingMethod = null,
                        time = null
                    ))
                    Log.d("RecipeStage",recipeSteps[indexOfStage-1])
                    recipeItemAdaptor.updateItems(recipeSteps)
                    Log.d("RecipeStage","Adaptor items  : ${recipeItemAdaptor.itemCount}, ItemList : $recipeSteps")
                }
                addpageTimer1.setText("")
                addpageTimer2.setText("")
                addpageTimer3.setText("")
                addpageCookingMethod.setText("")
                addpageDescription.setText("")
                addpageTitleText.setText("")
                addpageSwitchTimer.isChecked=false
            }
        }

        // 갤러리로 이동
        buttonSelectImage.setOnClickListener {
            openGallery()
        }
        fun saveRecipeToFirestore(imageUrl: String) {
            val title = editTextTitle.text.toString().trim() // 레시피 제목
            val description = editTextDescription.text.toString().trim() // 레시피 설명

            val category1 = spinner1.selectedItem.toString()
            val category2 = spinner2.selectedItem.toString()

            val categoryList = listOf(category1, category2)
                    if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "제목과 설명을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return
            }
            val orderString = "○"+recipeSteps.joinToString("○")
            val recipeItem = RecipeItem(
                name = title,
                description = description,
                imageResId = imageUrl,
                clicked = 0, // 기본값 0
                date = Timestamp.now(),
                order = orderString, // ○가 포함된 문자열로 저장
                id = "", // Firestore 저장 후 자동 설정 가능
                C_categories = categoryList, // 카테고리는 비어있는 리스트로 초기화
                note = note.text.toString(), // 무시됨
                tags = tagList, // 태그는 비어있는 리스트로 초기화
                ingredients = ingredientsList, // 재료 리스트도 비어있는 리스트로 초기화
                contained_channel = channelName!!
            )

            val firestore = FirebaseFirestore.getInstance()
            val recipeRef = firestore.collection("recipe").document()
            recipeRef.set(recipeItem)
                .addOnSuccessListener {
                    Toast.makeText(this, "레시피가 업로드되었습니다!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ChannelViewPage::class.java)
                    intent.putExtra("channel_name", channelName)  // Firestore 문서 ID 전달
                    this.startActivity(intent)  // 새로운 액티비티로 전환
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "업로드 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }


        // 업로드 버튼 클릭 시 실행
        buttonUpload.setOnClickListener {
            buttonUpload.isActivated=false
            selectedImageUri?.let { uri ->
                FireStoreHelper.uploadImage(
                    imageUri = uri,
                    imageName = "",  // 원하는 이미지 파일명 설정
                    folderName = "recipe_image", // Firebase Storage 폴더명
                    onSuccess = { imageUrl ->
                        saveRecipeToFirestore(imageUrl) // 이미지 업로드 이후 레시피를 저장
                    },
                    onFailure = { exception ->
                        Toast.makeText(this, "업로드 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                        buttonUpload.isActivated=true
                    }
                )
            } ?: run {
                Toast.makeText(this, "이미지를 선택해 주세요.", Toast.LENGTH_SHORT).show()
                buttonUpload.isActivated=true
            }
        }
        
        // 카테고리 데이터 Firestore에서 가져오기
        getCategoriesFromFirestore()  // 여기서 호출 추가
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }
    fun sanitizeInput(inputStr: String): String {
        return inputStr
        //return inputStr.replace(Regex("[()○]"), "")
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
}