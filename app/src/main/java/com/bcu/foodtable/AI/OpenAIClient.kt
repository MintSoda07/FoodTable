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
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // API 요청에 필요한 JSON 데이터
        val requestBody = mapOf(
            "model" to "gpt-4o",  // gpt-4o 혹은 gpt-4
            "messages" to listOf(
                mapOf("role" to "system", "content" to "당신은 조리 도우미 쿡봇입니다.조리 방법, 대안, 또는 요리 정보를 제공해 주세요.레시피를 제공할 때에는 요리 제목, 재료, 난이도, 순서를 제공합니다.2048 토큰 내로 답변하세요."),
                mapOf("role" to "user", "content" to prompt)
            ),
            "max_tokens" to 2048
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
                    }
                } else {
                    onError("Empty response body")
                }
            }
        })
    }
}