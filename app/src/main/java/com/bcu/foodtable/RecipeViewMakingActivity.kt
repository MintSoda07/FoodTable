package com.bcu.foodtable

import android.app.Activity
import android.content.Intent
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.useful.ApiKey
import com.bcu.foodtable.useful.ApiKeyManager
import com.bcu.foodtable.useful.FireStoreHelper
import com.bcu.foodtable.useful.FlexAdaptor
import com.bumptech.glide.Glide
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.textfield.TextInputEditText

class RecipeViewMakingActivity : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 1 // 갤러리 요청 코드

    var recipeName = ""
    var type="New"

    lateinit var itemImageView : ImageView
    lateinit var ingredientsList:List<String>
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_making_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recipeName = intent.getStringExtra("RecipeName") ?: ""
        val list : List<String>? = intent.getStringExtra("Ingredients")?.removeSurrounding("{", "}")?.split(",")
        if(list!=null){
            ingredientsList =  list
            type=intent.getStringExtra("type")?:""
        }

        type = intent.getStringExtra("RecipeName") ?: ""
        intent.putExtra("Type","New")

        val OpenAI = OpenAIClient()
        if (ApiKeyManager.getGptApi() == null) {
            // 객체가 null이 아닌지 확인한 후 사용
            OpenAI.setAIWithAPI(
                onSuccess = { info ->
                    Log.i("OpenAI", "API Name: ${info.KEY_NAME}")
                    Log.i("OpenAI", "API Key Successfully loaded.")
                    ApiKeyManager.setGptApiKey(info.KEY_NAME!!, info.KEY_VALUE!!)
                },
                onError = {
                    Log.e("OpenAI", "Failed to Load OpenAI API Key.")
                })
        }
        val AddPageRecipeNameText = findViewById<TextView>(R.id.AddPageRecipeNameText)
        itemImageView = findViewById<ImageView>(R.id.itemImageView)
        val AddPageImageUploadBtn = findViewById<Button>(R.id.AddPageImageUploadBtn)
        val AddPageDescriptionText = findViewById<TextView>(R.id.AddPageDescriptionText)

        val AddPageIngredientsItemList = findViewById<RecyclerView>(R.id.AddPageIngredientsItemList)
        val AddPageStageListRecyclerView = findViewById<RecyclerView>(R.id.AddPageStageListRecyclerView)

        val AddPageStageNum = findViewById<TextView>(R.id.AddPageStageNum)
        val AddPageStageTitleText = findViewById<TextView>(R.id.AddPageStageTitleText)
        val AddPageStageDescriptionText = findViewById<TextView>(R.id.AddPageStageDescriptionText)
        val AddPageStageAddBtn = findViewById<Button>(R.id.AddPageStageAddBtn)

        val AddPageStageTimerHour = findViewById<TextView>(R.id.AddPageStageTimerHour)
        val AddPageStageTimerMinute = findViewById<TextView>(R.id.AddPageStageTimerMinute)
        val AddPageStageTimerSecond = findViewById<TextView>(R.id.AddPageStageTimerSecond)
        val AddPageStageTimerBtn = findViewById<Button>(R.id.AddPageStageTimerBtn)

        val AddPageItemTags = findViewById<RecyclerView>(R.id.AddPageItemTags)

        val layoutManager = FlexboxLayoutManager(this).apply {
            flexDirection = FlexDirection.ROW   // 행(row) 방향으로 아이템 배치
            justifyContent = JustifyContent.FLEX_START // 아이템을 왼쪽 정렬
            flexWrap = FlexWrap.WRAP           // 줄바꿈 허용 (자동으로 아이템 크기 맞추기)
        }
        AddPageIngredientsItemList.layoutManager = layoutManager

        AddPageRecipeNameText.text = recipeName


        AddPageImageUploadBtn.setOnClickListener{
            openGallery()
        }
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    Glide.with(itemImageView.context)
                        .load(imageUri) // 매개변수로 전달받은 URL을 그대로 사용
                        .centerCrop()
                        .override(itemImageView.width, itemImageView.height)
                        .placeholder(R.drawable.baseline_menu_book_24) // 로딩 중 표시할 이미지
                        .error(R.drawable.dish_icon) // 실패 시 표시할 이미지
                        .into(itemImageView) // ImageView에 로드
                } else {
                    Toast.makeText(this, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
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