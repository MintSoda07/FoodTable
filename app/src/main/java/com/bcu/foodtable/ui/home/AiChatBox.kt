package com.bcu.foodtable.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // items(messages, key = { it.id }) 를 위해 id가 ChatMessage에 있다면 좋습니다.
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
// import androidx.lifecycle.viewmodel.compose.viewModel // viewModel 주입 방식에 따라 필요
import kotlinx.coroutines.launch

// ChatMessage 데이터 클래스는 다음과 같은 형태를 가정합니다.
// data class ChatMessage(val id: String = UUID.randomUUID().toString(), val text: String, val isUser: Boolean)
// ViewModel에서 messages StateFlow의 타입이 List<ChatMessage>이라고 가정합니다.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatBox(viewModel: AiChatViewModel) { // viewModel을 파라미터로 받거나 Hilt/Koin 등으로 주입
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState() // ViewModel에 isLoading StateFlow가 있다고 가정
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 새 메시지가 추가될 때 자동으로 스크롤
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // 전체 배경색 적용
            .padding(8.dp) // 전체적인 패딩 감소
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp), // 입력창과의 간격
            verticalArrangement = Arrangement.spacedBy(8.dp) // 메시지 간 간격
        ) {
            // ChatMessage에 id가 있다면 items(messages, key = { it.id }) 로 사용하는 것이 좋습니다.
            items(messages) { message ->
                MessageBubble(message = message)
            }
        }

        ChatInputRow(
            userInput = userInput,
            onUserInputChange = { userInput = it },
            onSendMessage = {
                if (userInput.text.isNotBlank()) {
                    viewModel.sendMessage(userInput.text.trim()) // ViewModel의 sendMessage 호출
                    userInput = TextFieldValue("") // 입력 필드 초기화
                }
            },
            isLoading = isLoading
        )
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val bubbleColor = if (message.isUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (message.isUser)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = if (message.isUser) 16.dp else 4.dp,
                topEnd = if (message.isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = bubbleColor,
            shadowElevation = 4.dp,
            tonalElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.75f)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputRow(
    userInput: TextFieldValue,
    onUserInputChange: (TextFieldValue) -> Unit,
    onSendMessage: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            TextField(
                value = userInput,
                onValueChange = onUserInputChange,
                placeholder = { Text("메시지를 입력하세요...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4,
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendMessage,
                enabled = userInput.text.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (userInput.text.isNotBlank() && !isLoading)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray,
                        shape = RoundedCornerShape(50)
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}