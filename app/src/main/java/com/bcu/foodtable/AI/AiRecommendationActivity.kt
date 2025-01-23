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
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.ApiKey
import com.bcu.foodtable.useful.ApiKeyManager
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.useful.ViewAnimator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AiRecommendationActivity : AppCompatActivity() {
    private var isSending = false
    private var apiKeyItem: ApiKey? = null
    private val aiUseCost = 40 // 한 번 AI 프롬프트를 전송할 때 필요한 소금(값)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_recommendation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 소금 텍스트뷰
        val saltView = findViewById<TextView>(R.id.AIRecommendSalt)
        val submitBtn = findViewById<Button>(R.id.submitAiSendButton)

        val responseBodyBox = findViewById<CardView>(R.id.AIResponseRecommendationCard)
        var responseViewBody = findViewById<TextView>(R.id.AiResponseResult)
        val responseReasonBox = findViewById<CardView>(R.id.AiResponseRecommendationReasonCard)
        var responseViewReason = findViewById<TextView>(R.id.AiResponseReason)

        val userInputBox = findViewById<TextInputEditText>(R.id.userAIPromptInput)
        val userWarningBox = findViewById<CardView>(R.id.warningCard)
        var userWarningText = findViewById<TextView>(R.id.warningTextCredit)

        val userSendingArea = findViewById<View>(R.id.Sending)
        val BackgroundText = findViewById<TextView>(R.id.BackgroundText)
        // Ai 불러오기
        val aIServiceAgent = OpenAIClient()

        // 만약 저장된 GPT_ApiKey가 없다면
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
        saltView.text = "${userData.point} 소금"

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
                ViewAnimator.alphaChange(BackgroundText, 200, false, AccelerateInterpolator(2.0f),{
                    BackgroundText.visibility=View.VISIBLE
                    BackgroundText.text=getString(R.string.ai_working)
                    ViewAnimator.alphaChange(BackgroundText, 200, true, AccelerateInterpolator(2.0f)).start()
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
                    role = "당신은 사용자에게 알맞은 레시피를 추천하는 친절한 도우미입니다.사용자의 질문에 대해 해당하는 음식과 그 레시피를 추천해 주세요. 추천 이유는 대답해주면 안 됩니다.",
                    onSuccess = { response ->
                        aiResponse = response
                        runOnUiThread {
                            BackgroundText.visibility=View.INVISIBLE
                            responseViewBody.text = response
                            responseBodyBox.visibility = View.VISIBLE
                            Log.i(
                                "OpenAI",
                                "UserPromt sent successful. Return String : ${response}"
                            )
                        }
                        aIServiceAgent.sendMessage(
                            prompt = "다음은 당신의 추천해준 레시피 내용입니다. \"${aiResponse}\" 이상으로, 사용자의 질문인 \"${promptInput}\"과 그에 대한 당신의 대답을 바탕으로 해당 레시피를 추찬해준 이유에 대해 재료의 특징,레시피의 특성을 이유로 들어 자세하게 설명해 주세요.",
                            role = "당신은 사용자에게 알맞은 레시피를 추천해준 친절한 도우미입니다. 왜 해당 레시피를 추천해 주었는지 친절하게 설명해 주세요.",
                            onSuccess = { response ->
                                runOnUiThread {
                                    responseViewReason.text = response
                                    responseReasonBox.visibility = View.VISIBLE
                                    Log.i(
                                        "OpenAI",
                                        "reasonRequestPrompt sent successful. Return String : ${response}"
                                    )
                                    isSending = false
                                    ViewAnimator.moveYPos(
                                        userSendingArea,
                                        150f,
                                        0f,
                                        400,
                                        DecelerateInterpolator(3.0f)
                                    ).start()
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
                                    saltView.text = "${userSalt} 소금"
                                    runOnUiThread {
                                        if (userData.point <= aiUseCost) {
                                            val lock =
                                                ContextCompat.getDrawable(
                                                    this@AiRecommendationActivity,
                                                    R.drawable.baseline_lock_24
                                                )
                                            submitBtn.background = lock
                                        }
                                    }
                                }
                            },
                            onError = { response ->
                                runOnUiThread {
                                    responseViewReason.text = getString(R.string.ai_error)
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
                    },
                    onError = { response ->
                        runOnUiThread {
                            responseViewBody.text = getString(R.string.ai_error)
                            aiResponse = response
                            Log.e("OpenAI", response)
                        }
                    }
                )


            }
        }
    }
}
