package com.bcu.foodtable.AI

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.ApiKeyManager
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.FlexAdaptor
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.useful.ViewAnimator
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AiHelperActivity : AppCompatActivity() {
    private var isSending = false
    private val aiUseCost = 40 // 한 번 AI 프롬프트를 전송할 때 필요한 소금(값)




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_helper)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val promptRule = """
            당신은 조리를 도와주는 쿡봇입니다. 지켜야 할 규칙은 다음과 같습니다.
            1. 레시피의 모든 조리 순서의 숫자 앞에 '○' 기호를 추가하고, 조리 방법에 대한 내용을 짧게 타이틀로 정리하여 순서 뒤에 괄호로 정리. 예: ○1.(손질 준비) 신선한 소고기와 채소를 준비합니다.
            2. 타이머가 필요한 조리 방법에 포맷 적용: 타이머가 필요한 조리 방법은 "(조리방법,hh:mm:ss)" 형식으로 표기함. 예: ○2.(구이 시작) 신선한 소고기와 채소를 후라이팬에 올려 구워줍니다.(굽기,00:20:00).
            3. 사용자가 입력한 재료만 사용하여 레시피를 제공합니다.
            4. 재료 목록은 따로 레시피 제공 전 {}안에 작성합니다. 예: {소고기}{감자}{소금}{후추} 
        """.trimIndent()

        val promptRuleFirst ="""
            당신은 조리를 도와주는 쿡봇입니다. 지켜야 할 규칙은 다음과 같습니다.
            1. 사용자가 입력한 재료만 사용하여 만들 수 있는 레시피를 최소 4개 이상 제공합니다.
            2. 재료 목록은 따로 레시피 제공 전 {}안에 작성합니다. 예: {소고기}{감자}{소금}{후추} 
            3. 제공되는 레시피의 앞과 뒤에는 정규식 구분을 위해 ◆을 붙여 주세요. 예: ◆감자 소금구이◆ 
            4. 최종 출력은 이런 형식이 되어야 합니다. 예: {소고기}{감자}{소금}{후추} ◆감자 소금구이◆ ◆감자 고기구이◆ ◆소고기 구이◆ ◆소고기 감자볶음◆
        """.trimIndent()


        // 테스트용 예시로 입력할 사용자 입력 :: 사과,고구마,강력분,중력분,박력분,소금,백설탕,흑설탕,베이킹파우더,드라이이스트,우유,버터,계란,바닐라 익스트렉트, 소고기,닭고기,감자,고구마 등이 있습니다.
        val saltTextView = findViewById<TextView>(R.id.AiHelperSalt)

        val userWarningText = findViewById<TextView>(R.id.warningTextCredit)
        val userWarningBox = findViewById<CardView>(R.id.warningCard)

        val submitBtn = findViewById<Button>(R.id.submitAiSendButton)
        val BackgroundText = findViewById<TextView>(R.id.backgroundText)

        val userSendingArea = findViewById<View>(R.id.Sending)
        val userInputBox = findViewById<TextInputEditText>(R.id.userAIPromptInput)

        var finalIngredientList : List<String>
        var finalRecipeList : List<String>
        // 만약 저장된 GPT_ApiKey가 없다면

        // Ai 불러오기
        val aIServiceAgent = OpenAIClient()

        if (ApiKeyManager.getGptApi() == null) {
            // 객체가 null이 아닌지 확인한 후 사용
            aIServiceAgent.setAIWithAPI(
                onSuccess = { info ->
                    Log.i("OpenAI", "API Name: ${info.KEY_NAME}")
                    Log.i("OpenAI", "API Key Successfully loaded.")
                    ApiKeyManager.setGptApiKey(info.KEY_NAME!!, info.KEY_VALUE!!)
                },
                onError = {
                    Log.e("OpenAI", "Failed to Load OpenAI API Key.")
                })
        }

        ViewAnimator.moveYPos(userWarningBox, 400f, 0f, 600, DecelerateInterpolator(3.0f)).start()
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            ViewAnimator.moveYPos(userWarningBox, 0f, 400f, 600, AccelerateInterpolator(3.0f))
                .start()
        }, 7000) // 7초 후에 실행되는 것

        // 소금 계산을 위해 유저 설정 불러오기
        var userData = UserManager.getUser()!!
        saltTextView.text = "${userData.point} 소금"

        userWarningText.text = getString(R.string.ai_cost, aiUseCost)
        if (userData.point <= aiUseCost) {
            val lock = ContextCompat.getDrawable(this, R.drawable.baseline_lock_24)
            submitBtn.background = lock
        }


        submitBtn.setOnClickListener {
            if (userData.point <= aiUseCost) {
                Toast.makeText(
                    applicationContext,
                    "${getString(R.string.ai_no_salt)} ${getString(R.string.ai_cost, aiUseCost)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            if (!isSending && userData.point >= aiUseCost) {
                isSending = true
                ViewAnimator.alphaChange(BackgroundText, 200, false, AccelerateInterpolator(2.0f), {
                    BackgroundText.visibility = View.VISIBLE
                    BackgroundText.text = getString(R.string.ai_working)
                    ViewAnimator.alphaChange(
                        BackgroundText,
                        200,
                        true,
                        AccelerateInterpolator(2.0f)
                    ).start()
                }).start()
                val promptInput = userInputBox.getText().toString()
                var aiResponse = ""
                userInputBox.clearFocus()
                userInputBox.setText("")
                ViewAnimator.moveYPos(userSendingArea, 0f, 150f, 400, AccelerateInterpolator(3.0f))
                    .start()
                Toast.makeText(
                    applicationContext,
                    getString(R.string.ai_working),
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("OpenAI", "UserPrompt :${promptInput}")
                aIServiceAgent.sendMessage(
                    prompt = "사용자 입력:${promptInput}",
                    role = promptRuleFirst,
                    onSuccess = { response ->
                        aiResponse = response
                        runOnUiThread {
                            BackgroundText.visibility = View.INVISIBLE
                            Log.i(
                                "OpenAI",
                                "UserPromt sent successful. Return String : ${response}"
                            )
                            ViewAnimator.moveYPos(
                                userSendingArea,
                                150f,
                                0f,
                                400,
                                DecelerateInterpolator(3.0f)
                            ).start()
                        }
                        isSending = false

                        var userSalt = userData.point
                        userSalt -= 40
                        userData.point -= 40
                        CoroutineScope(Dispatchers.IO).launch {
                            updateFieldById(
                                collectionPath = "user",
                                documentId = userData.uid,
                                fieldName = "point",
                                newValue = userSalt
                            )
                        }
                        runOnUiThread {
                            saltTextView.text = "${userSalt} 소금"
                            if (userData.point <= aiUseCost) {
                                val lock =
                                    ContextCompat.getDrawable(
                                        this@AiHelperActivity,
                                        R.drawable.baseline_lock_24
                                    )
                                submitBtn.background = lock
                            }
                        }

                        // 정규식 작성 부분
                        // AI의 아웃풋 형태가 {감자}{고구마}{버터}{소금}{후추}{파슬리}{칠리 소스}{케첩}{돼지고기}{닭고기}{치킨 스톡}{굴소스}
                        // ◆감자 버터구이◆ ◆고구마 케첩구이◆ ◆칠리 돼지고기 볶음◆ ◆굴소스 닭고기 볶음◆ ◆파슬리 버터 감자◆ ◆치킨 스톡 닭고기 조림◆ ◆소금 후추 돼지고기 구이◆ ◆고구마 칠리 소스 볶음◆
                        // 이런 느낌으로 나오도록 한 상태이므로 이를 두 가지의 리스트로 분리하는 정규식

                        // 재료 추출: {재료} 형태 찾기
                        val ingredientRegex = """\{(.*?)\}""".toRegex()
                        val ingredients = ingredientRegex.findAll(aiResponse).map { it.groupValues[1] }.toList()

                        // 레시피 추출: ◆레시피◆ 형태 찾기
                        val recipeRegex = """◆(.*?)◆""".toRegex()
                        val recipes = recipeRegex.findAll(aiResponse).map { it.groupValues[1] }.toList()

                        println("재료 목록: $ingredients")
                        println("레시피 목록: $recipes")

                        finalIngredientList = ingredients
                        finalRecipeList = recipes

                        runOnUiThread {
                            val ingredientsItemRecyclerView: RecyclerView = findViewById(R.id.IngredientsBox)
                            val RecipessItemRecyclerView: RecyclerView = findViewById(R.id.RecipeBox)

                            val layoutManager = FlexboxLayoutManager(this).apply {
                                flexDirection = FlexDirection.ROW   // 행(row) 방향으로 아이템 배치
                                justifyContent = JustifyContent.FLEX_START // 아이템을 왼쪽 정렬
                                flexWrap = FlexWrap.WRAP           // 줄바꿈 허용 (자동으로 아이템 크기 맞추기)
                            }
                            val layoutManager2 = FlexboxLayoutManager(this).apply {
                                flexDirection = FlexDirection.ROW   // 행(row) 방향으로 아이템 배치
                                justifyContent = JustifyContent.FLEX_START // 아이템을 왼쪽 정렬
                                flexWrap = FlexWrap.WRAP           // 줄바꿈 허용 (자동으로 아이템 크기 맞추기)
                            }
                            ingredientsItemRecyclerView.layoutManager = layoutManager
                            ingredientsItemRecyclerView.adapter = FlexAdaptor(ingredients)

                            RecipessItemRecyclerView.layoutManager = layoutManager2
                            RecipessItemRecyclerView.adapter = FlexAdaptor(recipes)
                        }
                    },
                    onError = { response ->
                        runOnUiThread {
                            aiResponse = response
                            Log.e("OpenAI", response)
                            isSending = false
                            ViewAnimator.moveYPos(
                                submitBtn,
                                150f,
                                0f,
                                400,
                                DecelerateInterpolator(3.0f)
                            )
                        }
                    }
                )
            }
        }
        

    }

}