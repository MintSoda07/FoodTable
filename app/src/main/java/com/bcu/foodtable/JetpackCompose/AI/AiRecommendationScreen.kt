package com.bcu.foodtable.JetpackCompose.AI

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bcu.foodtable.R

@Composable
fun AiRecommendationScreen(
    viewModel: AiRecommendationViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    AiRecommendationScreenContent(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onSendClick = viewModel::sendRecommendation
    )
}
@Composable
fun AiRecommendationScreenContent(
    uiState: AiUiState,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
){

    Column(modifier = modifier.fillMaxSize()) {

        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD32F2F))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ai_recipe),
                fontSize = 24.sp,
                fontFamily = FontFamily(Font(R.font.the_jamsil)),
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.salt_value, uiState.userPoint),
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.the_jamsil_bold)),
                color = Color(0xFFFFF176),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        // 경고
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

        // 결과 카드
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (uiState.resultText.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        shape = RoundedCornerShape(15.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(Modifier.background(Color(0xFFD32F2F))) {
                            Text(
                                text = stringResource(R.string.recommend_AI_output),
                                color = Color.White,
                                fontFamily = FontFamily(Font(R.font.the_jamsil_bold)),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                            Text(
                                text = uiState.resultText,
                                modifier = Modifier
                                    .background(Color.White)
                                    .padding(6.dp),
                                fontFamily = FontFamily(Font(R.font.the_jamsil))
                            )
                        }
                    }
                }
            }

            if (uiState.reasonText.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        shape = RoundedCornerShape(15.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(Modifier.background(Color(0xFFD32F2F))) {
                            Text(
                                text = stringResource(R.string.recommend_AI_reason),
                                color = Color.White,
                                fontFamily = FontFamily(Font(R.font.the_jamsil_bold)),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(6.dp)
                            )
                            Text(
                                text = uiState.reasonText,
                                modifier = Modifier
                                    .background(Color.White)
                                    .padding(6.dp),
                                fontFamily = FontFamily(Font(R.font.the_jamsil))
                            )
                        }
                    }
                }
            }
        }

        // 입력창 하단
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFD32F2F))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = stringResource(R.string.recommend_sample)) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily(Font(R.font.the_jamsil)))
            )
            IconButton(
                onClick = onSendClick,
                enabled = uiState.inputText.isNotBlank()
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