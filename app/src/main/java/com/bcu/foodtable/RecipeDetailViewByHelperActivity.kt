package com.bcu.foodtable

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.useful.ApiKeyManager
import com.bcu.foodtable.useful.FlexAdaptor
import com.bcu.foodtable.useful.RecipeDetailRecyclerAdaptor
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent

class RecipeDetailViewByHelperActivity : AppCompatActivity() {
    lateinit var itemOrder:RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_detail_view_by_helper_redirect)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val promptForAi = """
            당신은 조리를 도와주는 쿡봇입니다. 지켜야 할 규칙은 다음과 같습니다.
            1. 레시피의 모든 조리 순서의 숫자 앞에 '○' 기호를 추가하고, 조리 방법에 대한 내용을 짧게 타이틀로 정리하여 순서 뒤에 괄호로 정리. 예: ○1.(손질 준비) 신선한 소고기와 채소를 준비합니다.
            2. 타이머가 필요한 조리 방법에 포맷 적용: 타이머가 필요한 조리 방법은 "(조리방법,hh:mm:ss)" 형식으로 표기함. 예: ○2.(구이 시작) 신선한 소고기와 채소를 후라이팬에 올려 구워줍니다.(굽기,00:20:00).
            3. 사용자가 입력한 재료만 사용하여 레시피를 제공합니다.
            4. 레시피의 설명은 따로 레시피 제공 전 {}안에 작성합니다. 예: {소고기 감자구이는 맛있는 요리입니다.} 
        """.trimIndent()
        val itemIngredient = findViewById<RecyclerView>(R.id.itemIngredients)
        itemOrder = findViewById(R.id.itemOrder)


        var ingredientsList:List<String>;

        val description = findViewById<TextView>(R.id.recipeDescription)
        val OpenAI = OpenAIClient()
        val itemName = findViewById<TextView>(R.id.recipeName)

        val recipeName = intent.getStringExtra("RecipeName") ?: ""
        val list : List<String>? = intent.getStringExtra("Ingredients")?.removeSurrounding("{", "}")?.split(",")
        if(list!=null){
            ingredientsList =  list
            val layoutManager = FlexboxLayoutManager(this).apply {
                flexDirection = FlexDirection.ROW   // 행(row) 방향으로 아이템 배치
                justifyContent = JustifyContent.FLEX_START // 아이템을 왼쪽 정렬
                flexWrap = FlexWrap.WRAP           // 줄바꿈 허용 (자동으로 아이템 크기 맞추기)
            }
            itemIngredient.layoutManager = layoutManager
            itemIngredient.adapter = FlexAdaptor(ingredientsList)
        }
        itemName.text = recipeName



        if(list!=null) {
            if (ApiKeyManager.getGptApi() == null) {
                // 객체가 null이 아닌지 확인한 후 사용
                OpenAI.setAIWithAPI(
                    onSuccess = { info ->
                        Log.i("OpenAI", "API Name: ${info.KEY_NAME}")
                        Log.i("OpenAI", "API Key Successfully loaded.")
                        ApiKeyManager.setGptApiKey(info.KEY_NAME!!, info.KEY_VALUE!!)
                        OpenAI.apiKeyInfo = ApiKeyManager.getGptApi()!!
                    },
                    onError = {
                        Log.e("OpenAI", "Failed to Load OpenAI API Key.")
                    })

            }else{
                OpenAI.apiKeyInfo = ApiKeyManager.getGptApi()!!
            }
            OpenAI.sendMessage(
                prompt = "사용 가능한 재료 목록은 ${list}이며, 이 중 필요한 재료만을 선택해 ${recipeName}을 만들어 주세요.",
                role = promptForAi,
                onSuccess = { response ->
                    Log.i("AI_Helper","response :$response")
                    // 재료 추출: {재료} 형태 찾기
                    val descriptionRegex = """\{(.*?)\}""".toRegex()
                    val descriptionText = descriptionRegex.find(response)?.groupValues?.get(1) ?: ""

                    // 레시피 추출: 레시피 형태 찾기
                    // 정규식을 사용하여 ○ 기호로 시작하는 모든 줄을 추출
                    val regex = Regex("○\\d+\\.\\(.*?\\).*", RegexOption.MULTILINE)
                    val extractedSteps = regex.findAll(response).joinToString("\n") { it.value }
                    val items = extractedSteps.split("○").filter { it.isNotBlank() }

                    // Done 버튼 클릭 시 처리할 로직
                    fun onDoneButtonClick(position: Int) {
                        // 현재 항목을 숨기고 다음 항목을 보이게 하는 로직을 구현합니다.
                        val currentViewHolder =
                            itemOrder.findViewHolderForAdapterPosition(position) as RecipeDetailRecyclerAdaptor.ViewHolder?

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
                                itemOrder.findViewHolderForAdapterPosition(position + 1) as RecipeDetailRecyclerAdaptor.ViewHolder?
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

                    var recipeItemAdaptor = RecipeDetailRecyclerAdaptor(
                        mutableListOf(),
                        this@RecipeDetailViewByHelperActivity
                    ){ clickedPosition ->
                        onDoneButtonClick(clickedPosition)
                    }
                    Log.d("Recipe_ListChecker","Recipe String List : ${items}")
                    // 리스트 어댑터
                    runOnUiThread {

                        recipeItemAdaptor.updateItems(items)
                        itemOrder.layoutManager = LinearLayoutManager(this)
                        itemOrder.adapter = recipeItemAdaptor
                        description.text=descriptionText
                    }

                },
                onError = { errorMessage ->

                }
            )
        }
    }

}