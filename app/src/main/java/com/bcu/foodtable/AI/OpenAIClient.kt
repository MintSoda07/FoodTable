package com.bcu.foodtable.ai


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
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
class OpenAIClient @Inject constructor() {
    // AI API 키 불러오기
    lateinit var apiKeyInfo : ApiKey

    private val client = OkHttpClient.Builder()
        .readTimeout(60, TimeUnit.SECONDS) //  타임아웃 설정
        .connectTimeout(60, TimeUnit.SECONDS) // 연결 타임아웃 설정
        .build() // HTTP 클라이언트 생성 실행

    private val gson = Gson()
    private val baseUrl = "https://api.openai.com/v1/chat/completions"

    private val imgUrl  = "https://api.openai.com/v1/images/generations"

    // API 키 정보를 가져오는 함수 (콜백을 사용하여 성공 및 오류 처리)
    suspend fun setAIWithAPIAsync(): ApiKey = suspendCoroutine { continuation ->
        val client = OpenAIClient()
        client.setAIWithAPI(
            onSuccess = { apiKey ->
                continuation.resume(apiKey)
            },
            onError = { errorMsg ->
                continuation.resumeWithException(Exception(errorMsg))
            }
        )
    }
    fun setAIWithAPI(onSuccess: (ApiKey) -> Unit, onError: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Firebase에서 API 키 정보를 가져옵니다
                apiKeyInfo = FirebaseHelper.getApiKeyInfo("QQH5lCbu52yagfWpJphQ")!!
                if(apiKeyInfo.toString().length>5)  Log.i("AI_KEY","Set AI KEY, and api Key is not null")
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
    /** DALL·E 3 이미지 생성 */
    fun generateImage(
        prompt: String,
        size: String = "1024x1024",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // 1) 요청 로그
        Log.d("OpenAIClient", "🖼️ generateImage() prompt=\"$prompt\", size=$size")

        // 2) 바디 JSON 문자열로 미리 생성하고 로그
        val bodyMap = mapOf(
            "model"           to "dall-e-3",
            "prompt"          to prompt,
            "n"               to 1,
            "size"            to size,
            "response_format" to "url"
        )
        val jsonBody = gson.toJson(bodyMap)
        Log.v("OpenAIClient", "🔤 Request JSON: $jsonBody")

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            jsonBody
        )

        val req = Request.Builder()
            .url(imgUrl)
            .addHeader("Authorization", "Bearer ${apiKeyInfo.KEY_VALUE}")
            .post(body)
            .build()

        // 3) 네트워크 호출
        client.newCall(req).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OpenAIClient", "❌ generateImage onFailure: ${e.localizedMessage}", e)
                onError(e.localizedMessage ?: "Unknown error")
            }
            override fun onResponse(call: Call, res: Response) {
                val code = res.code
                val respBody = res.body?.string().orEmpty()
                Log.d("OpenAIClient", "📨 generateImage response code=$code, body=$respBody")

                if (code != 200) {
                    onError("HTTP $code")
                    return
                }
                try {
                    val root = gson.fromJson(respBody, Map::class.java)
                    val data = root["data"] as List<Map<String,Any>>
                    val url = data[0]["url"] as String
                    Log.d("OpenAIClient", "🎉 Image URL -> $url")
                    onSuccess(url)
                } catch (e: Exception) {
                    Log.e("OpenAIClient", "⚠️ generateImage parse error: ${e.localizedMessage}", e)
                    onError("Parse error: ${e.localizedMessage}")
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