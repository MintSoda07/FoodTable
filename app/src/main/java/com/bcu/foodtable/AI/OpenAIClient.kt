package com.bcu.foodtable.AI


import android.util.Log
import com.bcu.foodtable.useful.ApiKey
import com.bcu.foodtable.useful.FirebaseHelper
import okhttp3.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit

//  사용 예시. setAIWithAPI로 API Key를 받아 준비한 뒤, Success 시 sendMeesage 실행할 것!
//  순서 잘못될 시 오류를 반환함. (Key가 null)
//
//  val aIServiceManager = OpenAIClient()
//  aIServiceManager.setAIWithAPI(
//  onSuccess = {
//     aIServiceManager.sendMessage(
//            prompt = "안녕하세요, 쿡봇! 이것은 테스트 수신입니다. 수신하였다면 아무 농담이나 입력해 주세요!", // 사용자 입력
//            onSuccess = { response ->
//                println("ChatGPT 응답: $response")
//           },
//            onError = { error ->
//              println("오류: $error")
//
//         }
//       )
//  },
//  onError = {
//      Log.e("AI_SERVICE","An Error Occured During Setting an AI.")
//  })
class OpenAIClient() {
    // AI API 키 불러오기
    private lateinit var apiKeyInfo : ApiKey

    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS) //  타임아웃 설정
        .connectTimeout(30, TimeUnit.SECONDS) // 연결 타임아웃 설정
        .build() // HTTP 클라이언트 생성 실행

    private val gson = Gson()
    private val baseUrl = "https://api.openai.com/v1/chat/completions"

    // API 키 정보를 가져오는 함수 (콜백을 사용하여 성공 및 오류 처리)
    fun setAIWithAPI(onSuccess: (ApiKey) -> Unit, onError: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Firebase에서 API 키 정보를 가져옵니다
                apiKeyInfo = FirebaseHelper.getApiKeyInfo("QQH5lCbu52yagfWpJphQ")!!

                withContext(Dispatchers.Main) {
                    onSuccess(apiKeyInfo)
                }
            } catch (e: Exception) {
                // 예외가 발생하면 오류를 처리합니다
                withContext(Dispatchers.Main) {
                    onError("Error: ${e.message}")
                }
            }
        }
    }
    fun sendMessage(
        prompt: String,
        role:String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // API 요청에 필요한 JSON 데이터
        val requestBody = mapOf(
            "model" to "gpt-4o",  // gpt-4o 혹은 gpt-4
            "messages" to listOf(
                mapOf("role" to "system", "content" to role),
                mapOf("role" to "user", "content" to prompt)
            ),
            "max_tokens" to 8192
        )

        // RequestBody 생성 (OkHttp 4.x)
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            gson.toJson(requestBody)
        )

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer ${apiKeyInfo.KEY_VALUE}") // API 키
            .post(body) // POST 요청에 body 첨부
            .build()

        // 비동기 요청
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Unknown error")
                Log.e("AI_SERVICE","Error Occured : ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("Response not successful: ${response.code}")
                    Log.e("AI_SERVICE","Error Occured : ${response.code}")
                    return
                }

                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val jsonResponse = gson.fromJson(responseBody, Map::class.java)
                        val reply = (jsonResponse["choices"] as List<Map<String, Any>>)[0]["message"] as Map<String, String>
                        onSuccess(reply["content"] ?: "No content")
                    } catch (e: Exception) {
                        onError("Error parsing response: ${e.message}")
                        throw(e)
                    }
                } else {
                    onError("Empty response body")
                }
            }
        })
    }
}
//                        추후 사용될 레시피 생성 AI 프롬프트
//                        """
//                        당신은 조리를 도와주는 쿡봇입니다. 지켜야 할 규칙은 다음과 같습니다.
//                        1. 레시피의 모든 조리 순서의 숫자 앞에 '○' 기호를 추가하고, 조리 방법에 대한 내용을 짧게 타이틀로 정리하여 순서 뒤에 괄호로 정리. 예: ○1.(재료 준비) 신선한 소고기와 채소를 준비합니다.
//                        2. 타이머가 필요한 조리 방법에 포맷 적용: 타이머가 필요한 조리 방법은 "(조리방법,hh:mm:ss)" 형식으로 표기함. 예: ○2.(구이 시작) 신선한 소고기와 채소를 후라이팬에 올려 구워줍니다.(굽기,00:20:00).
//                        """