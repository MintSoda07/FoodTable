package com.bcu.foodtable.JetpackCompose.AI

import UserPromptInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bcu.foodtable.JetpackCompose.AI.AiHelperViewModel
import com.bcu.foodtable.JetpackCompose.AI.* // AiWarningCard, UserPromptInput 등 정의한 위치에 맞게 수정
import com.bcu.foodtable.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiHelperScreen(
    viewModel: AiHelperViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD32F2F))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.ai_assistant),
                fontSize = 24.sp,
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.the_jamsil))
            )
            Text(
                text = "${uiState.userPoint} 소금",
                fontSize = 20.sp,
                color = Color(0xFFFFF176),
                fontFamily = FontFamily(Font(R.font.the_jamsil_bold))
            )
        }

        // 경고 메시지
        if (uiState.showWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFA000))
            ) {
                Text(
                    text = stringResource(R.string.ai_cost, 40), // 포맷 값 전달!
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // 재료 및 레시피 리스트
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.ingredients),
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.the_jamsil))
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.ingredients.forEach { ingredient ->
                    AssistChip(
                        onClick = {},
                        label = { Text(ingredient) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.recipes),
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.the_jamsil))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.recipes.zip(uiState.recipeDetails).forEach { (title, detail) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = title, fontWeight = FontWeight.Bold)
                            Text(text = detail, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // 입력 및 전송
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD32F2F))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = viewModel::onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("재료를 입력하세요 (예: 감자, 소고기...)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White
                )
            )
            IconButton(
                onClick = { viewModel.sendMessage() },
                enabled = !uiState.isSending,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "전송",
                    tint = Color.White
                )
            }
        }
    }
}
