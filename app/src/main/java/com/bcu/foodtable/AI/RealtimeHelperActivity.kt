package com.bcu.foodtable.AI

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.RecipeItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class RealtimeHelperActivity : AppCompatActivity() {

    private lateinit var startHelpButton: Button
    private lateinit var statusText: TextView
    private lateinit var micStatusText: TextView

    private var webSocketClient: WebSocketClient? = null
    private var isConnected = false

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val SAMPLE_RATE = 24000 // Whisper나 Gemini 기준 16000Hz가 일반적
    private val executor = Executors.newSingleThreadExecutor()
    private val gson = Gson()

    companion object {
        private const val TAG = "VoiceHelper"
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_realtime_helper)

        startHelpButton = findViewById(R.id.startHelpButton)
        statusText = findViewById(R.id.statusText)
        micStatusText = findViewById(R.id.micStatusText)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startHelpButton.setOnClickListener {
            if (isConnected) disconnectWebSocket()
            else checkMicrophonePermissionAndConnect()
        }
    }

    private fun checkMicrophonePermissionAndConnect() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_RECORD_AUDIO
            )
        } else {
            fetchApiKeyFromFirestoreAndConnect()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // ✅ 반드시 호출

        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fetchApiKeyFromFirestoreAndConnect()
        } else {
            statusText.text = "마이크 권한이 필요합니다."
        }
    }


    private fun fetchApiKeyFromFirestoreAndConnect() {
        statusText.text = "API 키 불러오는 중..."
        startHelpButton.isEnabled = false

        FirebaseFirestore.getInstance()
            .collection("API_KEY")
            .document("QQH5lCbu52yagfWpJphQ")
            .get()
            .addOnSuccessListener { document ->
                val apiKey = document.getString("KEY_VALUE")
                if (!apiKey.isNullOrBlank()) {
                    connectWebSocket(apiKey)
                } else {
                    updateStatus("API 키가 비어 있습니다.")
                }
            }
            .addOnFailureListener {
                updateStatus("API 키 로드 실패: ${it.message}")
            }
    }

    private fun connectWebSocket(apiKey: String) {
        val uri = URI("wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-12-17")

        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d(TAG, "WebSocket 연결 성공")
                runOnUiThread {
                    isConnected = true
                    updateStatus("연결됨!")
                    startHelpButton.text = "연결 종료"
                    startHelpButton.isEnabled = true
                    startAudioRecording()
                }
            }
            val recipeItem = RecipeItem(
                name = "치킨 알프레도 파스타",
                description = "부드럽고 크리미한 알프레도 소스에 구운 치킨을 더한 파스타.",
                imageResId = "https://example.com/images/chicken_alfredo.jpg", // Firestore에서 이미지 URL을 불러옴
                clicked = 120,
                date = Timestamp.now(),
                order = "○1. (준비하기) 오븐을 180도로 예열하세요. ○2. (재료 준비) 감자 4개는 껍질을 벗기고, 얇게 썰어주세요. 양파 1개는 다지고, 다진 고기 200g을 준비하세요. ○3. (감자 삶기) 끓는 물에 감자를 넣고 약 10분 동안 삶아 부드럽게 만든 후 물기를 빼세요. (삶기,00:10:00) ○4. (칠리 소스 만들기) 팬에 기름을 두르고, 다진 양파와 고기를 넣어 볶습니다. 고기가 익으면, 토마토 페이스트와 칠리 파우더를 넣고 5분간 더 볶습니다. (볶기,00:05:00) ○5. (베샤멜 소스 만들기) 다른 팬에 버터를 녹이고 밀가루 2큰술을 넣어 볶은 후, 우유를 조금씩 넣으며 끓여서 걸쭉한 소스를 만듭니다. 소금과 후추로 간을 맞추세요. ○6. (층 쌓기) 오븐용 그라탕 접시에 삶은 감자, 칠리 소스, 베샤멜 소스를 번갈아 가며 올려주세요. 그 위에 체다 치즈를 듬뿍 뿌립니다. ○7. (구우기) 180도 오븐에서 20분 정도 구워 치즈가 녹고 황금빛이 돌도록 합니다. (굽기,00:20:00) ○8. (서빙하기) 오븐에서 꺼낸 후 잠시 식힌 후, 뜨겁게 서빙하세요.",
                id = "recipe_12345",
                C_categories = listOf("주요리", "이탈리안", "치킨"),
                note = "알프레도 소스를 너무 오래 끓이지 않도록 주의하세요.",
                tags = listOf("알프레도", "치킨", "파스타", "크리미"),
                ingredients = listOf("파스타 면", "치킨 가슴살", "알프레도 소스", "파마산 치즈", "마늘", "버터", "크림"),
                contained_channel = "recipe_channel_1",
                estimatedCalories = "850"
            )

            fun orderSplit(orderString: String): List<String> {
                val regex = "○(\\d+)\\.\\s\\((.*?)\\)\\s([^\\(]+)(?:\\s\\(([^)]+),([^)]*)\\))?".toRegex()
                val steps = regex.findAll(orderString).map {
                    val stepNumber = it.groupValues[1]  // 단계 번호
                    val title = it.groupValues[2]  // 제목
                    val description = it.groupValues[3]  // 설명

                    // 각 단계 정보를 문자열로 반환
                    "순서: $stepNumber, 제목: $title, 설명: $description"
                }.toList()

                return steps
            }

            override fun onMessage(message: String?) {
                Log.d(TAG, "메시지 수신됨: $message")
                runOnUiThread {
                    statusText.text = "서버 응답: $message"
                }

                val gson = Gson()

                try {
                    val jsonObject = gson.fromJson(message, JsonObject::class.java)
                    val type = jsonObject.get("type")?.asString

                    when (type) {
                        "session.created" -> {
                            sendSessionUpdate(recipeItem)
                            startStreamingAudio()
                        }

                        "tool_calls" -> {
                            val toolCalls = jsonObject.getAsJsonArray("tool_calls")
                            toolCalls?.forEach { element ->
                                val call = element.asJsonObject
                                val functionName = call["function"]?.asJsonObject?.get("name")?.asString
                                val argumentsStr = call["function"]?.asJsonObject?.get("arguments")?.asString
                                val toolCallId = call["id"]?.asString ?: return@forEach

                                try {
                                    val argsObj = gson.fromJson(argumentsStr, JsonObject::class.java)

                                    val toolResponse = when (functionName) {
                                        "get_recipe_info" -> {
                                            val recipeName = argsObj.get("name")?.asString ?: ""
                                            // 단일 레시피에 대한 정보만 처리
                                            if (recipeItem.name == recipeName) {
                                                // 레시피 정보를 반환 (RecipeItem의 모든 요소 포함)
                                                val recipeInfo = mapOf(
                                                    "name" to recipeItem.name,
                                                    "description" to recipeItem.description,
                                                    "imageResId" to recipeItem.imageResId,
                                                    "clicked" to recipeItem.clicked,
                                                    "date" to recipeItem.date,
                                                    "order" to recipeItem.order,
                                                    "id" to recipeItem.id,
                                                    "C_categories" to recipeItem.C_categories,
                                                    "note" to recipeItem.note,
                                                    "tags" to recipeItem.tags,
                                                    "ingredients" to recipeItem.ingredients,
                                                    "contained_channel" to recipeItem.contained_channel,
                                                    "estimatedCalories" to recipeItem.estimatedCalories
                                                )
                                                gson.toJson(mapOf("recipe" to recipeInfo))
                                            } else {
                                                // 레시피가 없거나 이름이 일치하지 않으면 에러 반환
                                                Log.e(TAG, "해당 이름의 레시피를 찾을 수 없습니다.")
                                                gson.toJson(mapOf("error" to "해당 이름의 레시피를 찾을 수 없습니다."))
                                            }
                                        }

                                        "get_next_step" -> {
                                            val currentStepIndex = argsObj.get("current_step_index")?.asInt ?: -1
                                            // 이미 나누어진 order로부터 단계 정보 가져오기
                                            val steps = orderSplit(recipeItem.order)
                                            val nextStep = if (currentStepIndex + 1 < steps.size) {
                                                val nextStepText = steps[currentStepIndex + 1]
                                                nextStepText
                                            } else {
                                                // 더 이상 단계가 없으면
                                                "더 이상 다음 단계가 없습니다."
                                            }
                                            gson.toJson(mapOf("next_step" to nextStep))
                                        }

                                        else -> {
                                            Log.e(TAG, "알 수 없는 함수 이름입니다: $functionName")
                                            gson.toJson(mapOf("error" to "알 수 없는 함수 이름입니다: $functionName"))
                                        }
                                    }

                                    val responseMessage = mapOf(
                                        "event_id" to "event_${System.currentTimeMillis()}",
                                        "type" to "tool_response",
                                        "tool_response" to mapOf(
                                            "tool_call_id" to toolCallId,
                                            "role" to "tool",
                                            "content" to toolResponse
                                        )
                                    )

                                    webSocketClient?.send(gson.toJson(responseMessage))

                                } catch (e: Exception) {
                                    Log.e(TAG, "툴 응답 처리 오류", e)
                                }
                            }
                        }


                        else -> {
                            Log.e(TAG, "알 수 없는 메시지 타입: $type")
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "메시지 파싱 오류", e)
                }
            }







            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG, "WebSocket 연결 종료: $reason")
                runOnUiThread {
                    isConnected = false
                    startHelpButton.text = "연결"
                    updateStatus("연결 종료됨")
                    stopAudioRecording()
                }
            }

            override fun onError(ex: Exception?) {
                Log.e(TAG, "WebSocket 오류 발생", ex)
                runOnUiThread {
                    isConnected = false
                    startHelpButton.text = "연결"
                    updateStatus("오류 발생: ${ex?.message}")
                    stopAudioRecording()
                }
            }
        }

        webSocketClient?.apply {
            addHeader("Authorization", "Bearer $apiKey")
            addHeader("OpenAI-Beta", "realtime=v1")
            connect()
        }
    }

    private fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread {
                micStatusText.text = "마이크 권한 없음"
                updateStatus("마이크 권한이 없어 녹음을 시작할 수 없습니다.")
            }
            return
        }

        micStatusText.text = "마이크 입력 중..."
        isRecording = true
    }

    private fun startStreamingAudio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread {
                micStatusText.text = "마이크 권한 없음"
                updateStatus("마이크 권한이 없어 오디오 스트리밍을 시작할 수 없습니다.")
            }
            Log.w(TAG, "마이크 권한 없음. 스트리밍 불가.")
            return
        }

        Log.d(TAG, "마이크 권한 확인됨. 오디오 스트리밍 초기화 시작.")

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true
        runOnUiThread {
            micStatusText.text = "마이크 스트리밍 중"
        }

        Log.d(TAG, "AudioRecord 시작됨. bufferSize: $bufferSize")

        executor.execute {
            val buffer = ByteArray(bufferSize)
            val gson = Gson()
            var accumulatedData = ByteArray(0)

            try {
                var lastSaveTime = System.currentTimeMillis()

                while (isRecording && webSocketClient?.isOpen == true) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // 오디오 데이터를 임시로 누적
                        accumulatedData += buffer.copyOf(read)

                        // 10초 간격으로 저장
                        if (System.currentTimeMillis() - lastSaveTime >= 10000) {
                            val base64Audio = Base64.encodeToString(accumulatedData, Base64.NO_WRAP)
                            saveBase64ToWavFile(base64Audio, "audio_${System.currentTimeMillis()}")
                            lastSaveTime = System.currentTimeMillis()
                            accumulatedData = ByteArray(0) // 저장 후 데이터를 초기화
                        }

                        try {
                            // 오디오 데이터를 Base64로 인코딩
                            val base64Audio = Base64.encodeToString(buffer.copyOf(read), Base64.NO_WRAP)

                            // WebSocket 이벤트 생성
                            val audioEvent = mapOf(
                                "event_id" to "event_${System.currentTimeMillis()}",
                                "type" to "input_audio_buffer.append",
                                "audio" to base64Audio
                            )

                            // JSON 직렬화
                            val json = gson.toJson(audioEvent)

                            // 서버로 전송
                            webSocketClient?.send(json)

                            Log.d(TAG, "오디오 패킷 전송 완료: ${base64Audio.take(30)}... (length: ${base64Audio.length})")

                            // 딜레이
                            Thread.sleep(20)

                        } catch (e: Exception) {
                            Log.e(TAG, "오디오 전송 중 오류 발생", e)
                        }
                    } else {
                        Log.w(TAG, "오디오 버퍼 읽기 실패 또는 0 바이트")
                    }
                }

                Log.d(TAG, "오디오 스트리밍 루프 종료됨. isRecording: $isRecording, webSocket open: ${webSocketClient?.isOpen}")
            } catch (e: Exception) {
                Log.e(TAG, "오디오 스트리밍 스레드 오류", e)
            }
        }
    }
    private fun saveBase64ToWavFile(base64Audio: String, fileName: String) {
        try {
            // Base64 디코딩
            val audioData = Base64.decode(base64Audio, Base64.NO_WRAP)

            // WAV 파일의 전체 길이를 구하기 위해 PCM 데이터의 길이를 사용
            val dataSize = audioData.size
            val byteRate = SAMPLE_RATE * 2 * 1 // 16-bit mono => 2 바이트 per 샘플
            val headerSize = 44 // WAV 파일의 기본 헤더 크기
            val fileSize = headerSize + dataSize

            // 파일을 저장할 디렉토리 선택 (외부 저장소 예시)
            val externalStoragePath = File(getExternalFilesDir(null), "audio")
            if (!externalStoragePath.exists()) {
                externalStoragePath.mkdir()  // 디렉토리가 없으면 생성
            }

            val wavFile = File(externalStoragePath, "$fileName.wav")
            val fos = FileOutputStream(wavFile)

            // "RIFF" 헤더
            fos.write("RIFF".toByteArray())
            fos.write(intToByteArray(fileSize - 8), 0, 4)
            fos.write("WAVE".toByteArray())

            // fmt 헤더
            fos.write("fmt ".toByteArray())
            fos.write(intToByteArray(16), 0, 4)  // fmt chunk size
            fos.write(shortToByteArray(1), 0, 2)  // PCM 형식 (1은 PCM)
            fos.write(shortToByteArray(1), 0, 2)  // Mono 채널 수
            fos.write(intToByteArray(SAMPLE_RATE), 0, 4)  // 샘플링 주파수
            fos.write(intToByteArray(byteRate), 0, 4)  // Byte rate
            fos.write(shortToByteArray(2), 0, 2)  // Block align
            fos.write(shortToByteArray(16), 0, 2)  // 비트 깊이 (16비트)

            // data 헤더
            fos.write("data".toByteArray())
            fos.write(intToByteArray(dataSize), 0, 4)

            // PCM 데이터 기록
            fos.write(audioData)

            fos.close()
            Log.d(TAG, "WAV 파일 저장 완료: ${wavFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "WAV 파일 저장 중 오류 발생", e)
        }
    }


    // Helper functions to convert integers to byte arrays
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            (value shr 8 and 0xFF).toByte(),
            (value shr 16 and 0xFF).toByte(),
            (value shr 24 and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            (value.toInt() shr 8 and 0xFF).toByte()
        )
    }


    private fun stopAudioRecording() {
        isRecording = false
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
        micStatusText.text = "마이크 중지됨"
    }

    private fun disconnectWebSocket() {
        webSocketClient?.close()
        updateStatus("연결 종료 중...")
    }

    private fun updateStatus(message: String) {
        statusText.text = message
        startHelpButton.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioRecording()
        webSocketClient?.close()
    }
    private fun sendSessionUpdate(recipeItem: RecipeItem) {
        val gson = Gson()
        val sessionUpdate = mapOf(
            "event_id" to "event_${System.currentTimeMillis()}",
            "type" to "session.update",
            "session" to mapOf(
                "modalities" to listOf("text", "audio"),
                "instructions" to "당신은 조리를 도와주는 쿡봇입니다.",
                "voice" to "sage",
                "input_audio_format" to "pcm16",
                "output_audio_format" to "pcm16",
                "input_audio_transcription" to mapOf("model" to "whisper-1"),
                "turn_detection" to mapOf(
                    "type" to "server_vad",
                    "threshold" to 0.5,
                    "prefix_padding_ms" to 300,
                    "silence_duration_ms" to 500,
                    "create_response" to true
                ),
                "tools" to listOf(
                    mapOf(
                        "type" to "function",
                        "name" to "get_recipe_info",
                        "description" to "레시피 정보를 불러줍니다.",
                        "parameters" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "name" to mapOf("type" to "string")
                            ),
                            "required" to listOf("name")
                        )
                    ),
                    mapOf(
                        "type" to "function",
                        "name" to "get_next_step",
                        "description" to "현재 조리 순서에서 다음 단계를 반환합니다.",
                        "parameters" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "current_step_index" to mapOf("type" to "integer")
                            ),
                            "required" to listOf("current_step_index")
                        )
                    )
                ),
                "tool_choice" to "auto",
                "temperature" to 0.8,
                "max_response_output_tokens" to "inf"
            )
        )

        val json = gson.toJson(sessionUpdate)
        webSocketClient?.send(json)
    }
}
