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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.AIChatting
import com.bcu.foodtable.useful.ApiKeyManager
import com.bcu.foodtable.useful.ChattingAdaptor
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.useful.UserManager
import com.bcu.foodtable.useful.ViewAnimator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AiChattingActivity : AppCompatActivity() {
    private var isSending = false
    private val aiUseCost = 25 // 한 번 AI 프롬프트를 전송할 때 필요한 소금(값)
    private lateinit var userData: User
    val chattingAdapter = ChattingAdaptor()

    // FB 불러오기
    val db = FirebaseFirestore.getInstance()
    val chatCollection = db.collection("Ai_chat_Session")

    val chattingList : MutableList<AIChatting>  = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_chatting)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val saltView = findViewById<TextView>(R.id.AiChatSaltView)
        val submitBtn = findViewById<Button>(R.id.submitAiSendButtonInChat)

        val userInputBox = findViewById<TextInputEditText>(R.id.userAIPromptInputChat)
        val userWarningBox = findViewById<CardView>(R.id.warningCardChat)
        var userWarningText = findViewById<TextView>(R.id.warningTextCredit)

        val userSendingArea = findViewById<View>(R.id.Sending)

        val chatRecyclerView: RecyclerView = findViewById(R.id.AIChattingView)
        chatRecyclerView.adapter = chattingAdapter
        chatRecyclerView.layoutManager = LinearLayoutManager(this)


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
        userData = UserManager.getUser()!!
        saltView.text = "${userData.point} 소금"

        // Firestore에서 데이터 가져오기

        chatCollection
            .whereEqualTo("uid", userData.uid) // 특정 UID로 필터링
            .orderBy("chatDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error fetching data: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val chats = snapshot.documents.map { doc ->
                        val content = doc.getString("content") ?: ""
                        val chatDate = doc.getTimestamp("chatDate") ?: Timestamp.now()
                        AIChatting(content = content, chatDate = chatDate)
                    }
                    val latestChats = chats.takeLast(3) // 마지막 3개 항목
                    chattingList.clear()
                    chattingList.addAll(latestChats)

                    chattingAdapter.apply {
                        chatList.clear()
                        chatList.addAll(chats)
                        notifyDataSetChanged()
                    }
                }
            }
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
                return@setOnClickListener
            }
            if (!isSending) {
                val chatTime = Timestamp.now()
                val promptInput = userInputBox.getText().toString()
                val yourChat = AIChatting(
                    content = promptInput,
                    chatDate = chatTime,
                    uid = userData.uid
                )

                // addChat을 통해 채팅 추가
                chattingAdapter.addChat(yourChat)
                addChat(yourChat)
                chattingList.add(yourChat)

                isSending = true

                userInputBox.clearFocus()
                userInputBox.setText("")
                ViewAnimator.moveYPos(userSendingArea, 0f, 150f, 400, AccelerateInterpolator(3.0f))
                    .start()
                Toast.makeText(
                    applicationContext,
                    getString(R.string.ai_working),
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("OpenAI", "UserPrompt :${promptInput} \n Set Prompt to \"${chattingList[0].content}\\n ${chattingList[1].content}\\n ${chattingList[2].content}\\n${promptInput}")
                aIServiceAgent.sendMessage(
                    prompt = "${chattingList[0].content}\n ${chattingList[1].content}\n ${chattingList[2].content}\n${promptInput}",
                    role = "당신은 사용자에게 요리에 대해 이야기를 나누는 친절한 전문가입니다. partner은 당신입니다.",
                    onSuccess = { response ->
                        isSending = false
                        runOnUiThread {
                            ViewAnimator.moveYPos(
                                userSendingArea,
                                150f,
                                0f,
                                400,
                                DecelerateInterpolator(3.0f)
                            )
                                .start()

                            // 시간 생성 (예: 현재 시간)
                            val currentTime = Timestamp.now()

                            // AIChatting 데이터 생성
                            val aiChatResponse = AIChatting(
                                content = "Partner:${response}",
                                chatDate = currentTime,
                                uid = userData.uid
                            )

                            // addChat을 통해 채팅 추가
                            chattingAdapter.addChat(aiChatResponse)
                            addChat(aiChatResponse)
                            chattingList.add(aiChatResponse)
                            Log.i(
                                "OpenAI",
                                "UserPromt sent successful. Return String : ${response}"
                            )

                        }
                        var userSalt = userData.point
                        userSalt -= aiUseCost
                        userData.point -= aiUseCost
                        CoroutineScope(Dispatchers.IO).launch {
                            updateFieldById(
                                collectionPath = "user",
                                documentId = userData.uid,
                                fieldName = "point",
                                newValue = userSalt
                            )
                        }
                        runOnUiThread {
                            saltView.text = "${userSalt} 소금"
                            if (userData.point <= aiUseCost) {
                                val lock =
                                    ContextCompat.getDrawable(
                                        this@AiChattingActivity,
                                        R.drawable.baseline_lock_24
                                    )
                                submitBtn.background = lock
                            }
                        }
                    },
                    onError = { response ->
                        runOnUiThread {
                            isSending = false
                            ViewAnimator.moveYPos(
                                submitBtn,
                                150f,
                                0f,
                                400,
                                DecelerateInterpolator(3.0f)
                            )
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.ai_error),
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("OpenAI", response)
                        }
                    }
                )
            }
        }
    }

    fun updateChatList(newChats: List<AIChatting>) {
        chattingAdapter.apply {
            chatList.clear()
            chatList.addAll(newChats)
            notifyDataSetChanged()
        }
    }

    // 채팅 수가 8개가 넘어가면 가장 오래된 데이터 삭제
    fun checkAndDeleteOldChats() {
        chatCollection
            .whereEqualTo("uid", userData.uid) // 특정 UID로 필터링
            .orderBy("chatDate", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.size() > 8) {
                    val oldestChat = snapshot.documents.first()
                    oldestChat.reference.delete()
                        .addOnSuccessListener {
                            if (chattingList.size >= 3) {
                                chattingList.removeAt(0) // 맨 앞 값을 제거
                            }
                            Log.d("Firestore", "Oldest chat deleted successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error deleting chat: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching chats: ${e.message}")
            }
    }

    fun addChat(chat: AIChatting) {
        if (chat.uid.isEmpty()) {
            Log.e("Firestore", "Chat UID is missing")
            return
        }
        chatCollection.add(chat)
            .addOnSuccessListener {
                Log.d("Firestore", "Chat added successfully")
                // 오래된 채팅 삭제 확인
                checkAndDeleteOldChats()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding chat: ${e.message}")
            }
    }
}