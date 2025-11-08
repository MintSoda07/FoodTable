// ui/home/AiChatBox.kt

package com.bcu.foodtable.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items           // 반드시 이 items 임포트
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow

/**
 * ChatMessage 데이터 클래스는 별도 파일(ui/home/ChatMessage.kt)에 정의되어 있어야 합니다.
 * 예:
 * data class ChatMessage(
 *     val id: String = java.util.UUID.randomUUID().toString(),
 *     val text: String,
 *     val isUser: Boolean
 * )
 */

/**
 * AiChatViewModel 인터페이스는 별도 파일에 정의되어 있어야 합니다.
 * StateFlow<List<ChatMessage>>와 StateFlow<Boolean>을 제공하는 구현체가 필요합니다.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatBox(viewModel: AiChatViewModel) {
    // StateFlow → Compose State 변환
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    val listState = rememberLazyListState()

    // 새 메시지가 추가되면 리스트 하단으로 스크롤
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        // ───────────────────────────────────────────────────
        // 0) 최상단 안내 카드
        // ───────────────────────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "메시지를 입력해 상담을\n시작해 보세요. “도움말” 입력 시 기능 안내!",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // ───────────────────────────────────────────────────
        // 1) 메시지 목록
        // ───────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { message ->
                MessageBubble(message = message)
            }
        }

        // ───────────────────────────────────────────────────
        // 2) 입력창
        // ───────────────────────────────────────────────────
        ChatInputRow(
            userInput = userInput,
            onUserInputChange = { userInput = it },
            onSendMessage = {
                if (userInput.text.isNotBlank()) {
                    viewModel.sendMessage(userInput.text.trim())
                    userInput = TextFieldValue("")
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
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.75f)
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(12.dp)
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
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(30.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(30.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TextField(
                value = userInput,
                onValueChange = onUserInputChange,
                placeholder = { Text("메시지를 입력해주세요…") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
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
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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
                        contentDescription = "보내기",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
