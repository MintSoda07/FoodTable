package com.bcu.foodtable

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.useful.*
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipeViewActivity : AppCompatActivity() {
    private lateinit var recipeId: String
    private lateinit var adaptorViewList: RecyclerView
    private lateinit var RecipeAdaptor: RecipeDetailRecyclerAdaptor
    private lateinit var items: List<String>

    private var isClickedUpdated = false
    private lateinit var notificationPermissionManager: NotificationPermissionManager

    //    private val regex = Regex("(.*)\\s*\\((.*),(\\d{2}:\\d{2}:\\d{2})\\)") // 타이머가 포함된 형식
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        adaptorViewList = findViewById(R.id.RecipeList)
        adaptorViewList.layoutManager = LinearLayoutManager(this)
        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    // 알림 승인
                    Toast.makeText(this, "알림이 승인되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 알림 거부
                }
            }

        notificationPermissionManager = NotificationPermissionManager(this, permissionLauncher)

        // 알림 권한 요청
        notificationPermissionManager.requestPermissionIfNeeded()

        // Intent로 전달된 데이터 받기
        recipeId = intent.getStringExtra("recipe_id") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            val recipe = FirebaseHelper.getDocumentById("recipe", recipeId, RecipeItem::class.java)
            recipe?.let {
                it.id = recipeId  // Firestore 문서의 ID를 recipe.id에 할당

                // 해당 아이템의 클릭 수 +1
                if (!isClickedUpdated) {
                    updateFieldById(
                        collectionPath = "recipe", // 컬렉션 이름
                        documentId = recipeId, // 문서 ID
                        fieldName = "clicked", // 수정할 필드 이름
                        newValue = it.clicked + 1 // 새 값
                    )
                }
                // UI 업데이트
                val placeholder_name = findViewById<TextView>(R.id.itemName_recipe)
                val placeholder_description = findViewById<TextView>(R.id.BasicDescription)
                val placeholder_image = findViewById<ImageView>(R.id.itemImageView)
                val inputString = it.order

                // ○를 기준으로 문자열을 나눔 (
                items = inputString.split("○").filter { it.isNotBlank() }
                // 리스트 어댑터
                RecipeAdaptor = RecipeDetailRecyclerAdaptor(
                    mutableListOf(),
                    this@RecipeViewActivity
                ) { position ->
                    onDoneButtonClick(position)
                }
                RecipeAdaptor.updateItems(items)
                adaptorViewList.adapter = RecipeAdaptor
                FireStoreHelper.loadImageFromUrl(it.imageResId, placeholder_image)
                placeholder_name.text = it.name
                placeholder_description.text = it.description

            } ?: run {
                Log.d("Recipe", "No recipe found for the provided ID.")
            }
        }

    }

    // Done 버튼 클릭 시 처리할 로직
    private fun onDoneButtonClick(position: Int) {
        // 현재 항목을 숨기고 다음 항목을 보이게 하는 로직을 구현합니다.
        val currentViewHolder =
            adaptorViewList.findViewHolderForAdapterPosition(position) as RecipeDetailRecyclerAdaptor.ViewHolder?

        // Done 버튼 숨기기
        currentViewHolder?.doneButton?.visibility = View.GONE

        // CheckBox 체크 상태 변경
        currentViewHolder?.checkBox?.isChecked = true

        // 배경색을 회색으로 흐리게 하기 (배경색 변경)
        currentViewHolder?.itemView?.setBackgroundColor(Color.parseColor("#D3D3D3"))  // 회색으로 배경 변경
        try {
            val item = items[position + 1]
            val regex = Regex("(.*)\\s*\\((.*),(\\d{2}:\\d{2}:\\d{2})\\)") // 타이머가 포함된 형식
            val matchResult = regex.find(item)
            val nextViewHolder =
                adaptorViewList.findViewHolderForAdapterPosition(position + 1) as RecipeDetailRecyclerAdaptor.ViewHolder?
            // 타이머가 존재하면, 다음 항목의 타이머를 보이게 설정
            if (matchResult != null) {
                nextViewHolder?.timerFrame?.visibility = View.VISIBLE
                val method = matchResult.groupValues[2].trim() // 조리방식 문자열
                val timeStr = matchResult.groupValues[3].trim() // "hh:mm:ss" 의 시간 비슷한 문자열
                nextViewHolder?.timerTitle?.text = method
                nextViewHolder?.timerTime?.text = timeStr
            } else {
                // 다음 항목 버튼 바로 보이기
                nextViewHolder?.doneButton?.visibility = View.VISIBLE
            }
        } catch (error: Exception) {
            Log.e("RecipeCooking", "Error Occured.. :${error.message}") // Index 오류일 것이다. 그대로 둔다.
        }

    }

}

