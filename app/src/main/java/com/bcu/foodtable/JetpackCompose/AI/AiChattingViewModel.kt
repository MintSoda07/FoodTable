package com.bcu.foodtable.JetpackCompose.AI

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bcu.foodtable.AI.OpenAIClient
import com.bcu.foodtable.useful.AIChatting
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.UserManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiChattingViewModel(

    private val apiClient: OpenAIClient,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance() // ✅ Manual DI
) : ViewModel() {

    data class UiState(
        val chatList: List<AIChatting> = emptyList(),
        val inputText: String = "",
        val userPoint: Int = 0,
        val isSending: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val user = UserManager.getUser()
    private val chatCollection = db.collection("Ai_chat_Session")
    private val aiUseCost = 25

    init {
        loadChatHistory()
        _uiState.update { it.copy(userPoint = user?.point ?: 0) }
    }

    private fun loadChatHistory() {
        chatCollection
            .whereEqualTo("uid", user?.uid)
            .orderBy("chatDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    val content = doc.getString("content") ?: return@mapNotNull null
                    val chatDate = doc.getTimestamp("chatDate") ?: Timestamp.now()
                    AIChatting(content, chatDate, user?.uid ?: "")
                } ?: emptyList()

                _uiState.update { it.copy(chatList = chats.takeLast(10)) }
            }
    }

    fun onInputChange(new: String) {
        _uiState.update { it.copy(inputText = new) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val input = state.inputText
        val currentUser = user ?: return

        if (input.isBlank() || state.isSending || currentUser.point < aiUseCost) return

        val chatTime = Timestamp.now()
        val yourChat = AIChatting(content = input, chatDate = chatTime, uid = currentUser.uid)

        _uiState.update {
            it.copy(
                inputText = "",
                chatList = it.chatList + yourChat,
                isSending = true
            )
        }

        viewModelScope.launch {
            chatCollection.add(yourChat)

            val contextPrompt = (_uiState.value.chatList + yourChat)
                .takeLast(3)
                .joinToString("\n") { it.content }

            apiClient.sendMessage(
                prompt = "$contextPrompt\n$input",
                role = "당신은 요리 상담을 도와주는 친절한 AI입니다.",
                onSuccess = { response ->
                    val aiChat = AIChatting(
                        content = "Partner: $response",
                        chatDate = Timestamp.now(),
                        uid = currentUser.uid
                    )

                    chatCollection.add(aiChat)
                    val newPoint = (currentUser.point - aiUseCost).coerceAtLeast(0)
                    currentUser.point = newPoint

                    viewModelScope.launch {
                        updateFieldById("user", user.uid, "point", newPoint)
                    }

                    _uiState.update {
                        it.copy(
                            chatList = it.chatList + aiChat,
                            isSending = false,
                            userPoint = newPoint
                        )
                    }
                },
                onError = {
                    _uiState.update { it.copy(isSending = false) }
                }
            )
        }
    }
}
