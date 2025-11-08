package com.bcu.foodtable.TTS // 사용자님의 패키지명으로 되어 있는지 확인해주세요

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult // 추가됨
import androidx.activity.result.contract.ActivityResultContracts // 추가됨
import androidx.compose.foundation.Image // 추가됨 (이미지 미리보기용)
import androidx.compose.foundation.border // 추가됨 (이미지 미리보기용)
import androidx.compose.foundation.clickable // 추가됨 (이미지 미리보기용)
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // 추가됨 (스크롤용)
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll // 추가됨 (스크롤용)
import androidx.compose.material.icons.Icons // 추가됨 (아이콘용)
import androidx.compose.material.icons.filled.PhotoCamera // 추가됨 (아이콘용)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale // 추가됨 (이미지 미리보기용)
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter // 추가됨 (Coil 라이브러리 사용 시)
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions

// ViewModel 및 Factory import 경로가 올바른지 확인해주세요.
// CookingAiViewModel과 CookingAiViewModelFactory가 이 파일과 같은 패키지(com.bcu.foodtable.TTS)에 있다면
// 별도의 import문이 필요 없을 수 있습니다. 다른 패키지에 있다면 정확한 import 경로를 추가해야 합니다.
// 예: import com.bcu.foodtable.yourpackage.CookingAiViewModel
// 예: import com.bcu.foodtable.yourpackage.CookingAiViewModelFactory


@OptIn(ExperimentalMaterial3Api::class) // Material3 최신 API 사용 시
@Composable
fun CookingEvaluationScreen() {
    val application = LocalContext.current.applicationContext as Application
    val firebaseFunctionsInstance = remember { Firebase.functions("us-central1") }
    val viewModelFactory = remember { CookingAiViewModelFactory(application, firebaseFunctionsInstance) }
    val viewModel: CookingAiViewModel = viewModel(factory = viewModelFactory)

    val isLoading by viewModel.isLoading.collectAsState()
    val evaluationResult by viewModel.evaluationApiResult.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val context = LocalContext.current

    // 입력 상태 변수들
    var userImageUriState by remember { mutableStateOf<Uri?>(null) }
    var recipeImageUrlState by remember { mutableStateOf("") }

    // 이미지 피커 런처 정의
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        userImageUriState = uri
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearToastMessage() // 메시지 표시 후 초기화
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // 내용이 길어질 경우 스크롤 가능하도록 추가
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // UI 요소들이 위에서부터 쌓이도록 변경
    ) {
        Text("AI 요리 평가", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // 1. 레시피 이미지 URL 입력창
        OutlinedTextField(
            value = recipeImageUrlState,
            onValueChange = { recipeImageUrlState = it },
            label = { Text("레시피 이미지 URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 2. 사용자 요리 이미지 선택 UI
        Text("내 요리 사진 선택:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.CenterHorizontally) // 가운데 정렬
                .clickable { imagePickerLauncher.launch("image/*") } // 클릭 시 이미지 피커 실행
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)), // 테두리 모양 변경
            contentAlignment = Alignment.Center
        ) {
            if (userImageUriState != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = userImageUriState), // Coil 라이브러리 사용
                    contentDescription = "선택된 내 요리 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // 이미지 비율 유지하며 채우기
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "이미지 선택 아이콘",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant // 테마 색상 적용
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp)) // "AI 평가 받기" 버튼과의 간격

        // 3. "AI 평가 받기" 버튼
        Button(
            onClick = {
                val currentRecipeUrl = recipeImageUrlState
                val currentUserImageUri = userImageUriState

                if (currentUserImageUri != null && currentRecipeUrl.isNotBlank()) {
                    viewModel.evaluateCookingRecipe(currentRecipeUrl, currentUserImageUri)
                } else {
                    Toast.makeText(context, "레시피 URL과 내 요리 사진을 모두 입력/선택해주세요.", Toast.LENGTH_LONG).show()
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = !isLoading
        ) {
            Text(
                if (isLoading) "평가 요청 중..." else "🤖 AI 평가 받기",
                color = Color.White,
                fontSize = 16.sp
            )
        }

        // 4. 로딩 인디케이터
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        // 5. 평가 결과 표시
        evaluationResult?.let { resultText ->
            Spacer(modifier = Modifier.height(24.dp))
            Text("AI 평가 결과:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(resultText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}