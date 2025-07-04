package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.time.LocalDate
import java.util.*
// --- 기존 함수 시그니처를 그대로 유지합니다 ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIngredientScreen(
    viewModel: FridgeViewModel,
    navController: NavController,
    section: String
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var expireDate by remember { mutableStateOf("") }
    val context = LocalContext.current


    // OCR 스캔된 이름 임시 저장(null 이면 다이얼로그 미표시)
    var scannedIngredients by remember { mutableStateOf<List<IngredientExtracted>?>(null) }

    // OCR 텍스트를 재료명 리스트로 걸러내는 로직
//    fun handleOcrResult(text: String) {
//        // 1) 줄 단위로 분리
//        val lines = text.lines().map { it.trim() }
//
//        // 2) “수량 금액 상품” 헤더 찾기
//        val headerIdx = lines.indexOfFirst {
//            it.contains("수량") && it.contains("금액") && it.contains("상품")
//        }
//        if (headerIdx == -1) {
//            Toast.makeText(context, "항목 헤더를 찾지 못했습니다.", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        // 3) 헤더 아래부터 패턴 매칭
//        val itemPattern = Regex("""^(.+?)\s+\d+\s+[\d,]+$""")
//        val blacklist = setOf("선택안함", "합계", "판매금액", "부가", "신용승인")
//        val items = mutableListOf<String>()
//
//        for (i in headerIdx + 1 until lines.size) {
//            val line = lines[i]
//            // 빈 줄이거나 블랙리스트 키워드로 시작하면 끝
//            if (line.isBlank() || blacklist.any { line.startsWith(it) }) break
//
//            itemPattern.find(line)?.let { m ->
//                val name = m.groupValues[1]
//                items += name
//            }
//        }
//
//        val unique = items.distinct()
//        if (unique.isEmpty()) {
//            Toast.makeText(context, "유효한 재료명을 찾지 못했습니다.", Toast.LENGTH_LONG).show()
//        } else {
//            scannedNames = unique
//        }
//    }

    val recognizer = TextRecognition
        .getClient(KoreanTextRecognizerOptions.Builder().build())

    val datePickerDialog = remember {
        val today = LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                expireDate = String.format("%04d-%02d-%02d", year, month + 1, day)
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        )
    }
    // ① Document Scanner 옵션
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)   // 카메라 촬영 전용
            .setPageLimit(1)                   // 최대 1페이지
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
            )
            .build()
    }
    // ② Document Scanner 클라이언트
    val documentScanner = GmsDocumentScanning.getClient(scannerOptions)


    // ③ IntentSender 런처
    val docScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // ML Kit 대신 Clova OCR 호출로 대체
            val pages = GmsDocumentScanningResult
                .fromActivityResultIntent(result.data)
                ?.pages
                .orEmpty()

            val firstUri = pages.firstOrNull()?.getImageUri()
            if (firstUri != null) {
                // 이미지 → Base64 변환
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, firstUri)
                val base64Image = encodeImageToBase64(bitmap)

                // Clova OCR 직접 호출
                viewModel.sendToClovaOCR(
                    base64Image = base64Image,
                    onSuccess = { extractedText ->
                        viewModel.extractIngredientsWithQuantityUsingAI(
                            ocrText = extractedText,
                            onResult = { aiIngredients ->
                                if (aiIngredients.isEmpty()) {
                                    Toast.makeText(context, "AI가 유효한 재료를 찾지 못했습니다.", Toast.LENGTH_LONG).show()
                                } else {
                                    scannedIngredients = aiIngredients
                                }
                            },
                            onError = { errMsg ->
                                Toast.makeText(context, "AI 호출 실패: $errMsg", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    onError = { errorMsg ->
                        Toast.makeText(context, "Clova OCR 실패: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                Toast.makeText(context, "유효한 스캔 결과가 없습니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 갤러리에서도 OCR 할 거면 기존 galleryLauncher 유지
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            val base64Image = encodeImageToBase64(bitmap)
            viewModel.sendToClovaOCR(
                base64Image = base64Image,
                onSuccess = { extractedText ->
                    viewModel.extractIngredientsWithQuantityUsingAI(
                        ocrText = extractedText,
                        onResult = { aiIngredients ->
                            if (aiIngredients.isEmpty()) {
                                Toast.makeText(context, "AI가 유효한 재료를 찾지 못했습니다.", Toast.LENGTH_LONG).show()
                            } else {
                                scannedIngredients = aiIngredients
                            }
                        },
                        onError = { errMsg ->
                            Toast.makeText(context, "AI 호출 실패: $errMsg", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onError = { errorMsg ->
                    Toast.makeText(context, "Clova OCR 실패: $errorMsg", Toast.LENGTH_LONG).show()
                }
            )
        }
    }



    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("새로운 재료 추가", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        val quantityInt = quantity.toIntOrNull()
                        if (name.isNotBlank() && quantityInt != null && quantityInt > 0 && expireDate.isNotBlank()) {
                            val item = Ingredient(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                quantity = quantity.toInt(),
                                expireDate = expireDate,
                                section = section
                            )
                            viewModel.addIngredient(item, section) {
                                navController.popBackStack()
                            }
                        } else {
                            Toast.makeText(context, "모든 정보를 올바르게 입력해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("저장하기", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "어떤 재료를 추가할까요?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "'$section' 섹션에 새로운 재료를 등록합니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                        alpha = 0.5f
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("재료명") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.RestaurantMenu,
                                contentDescription = null
                            )
                        },
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.all(Char::isDigit)) quantity = it },
                        label = { Text("수량") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                        singleLine = true,
                        colors = outlinedTextFieldColors()
                    )

                    Box {
                        OutlinedTextField(
                            value = expireDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("유통기한") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "날짜 선택"
                                )
                            },
                            colors = outlinedTextFieldColors()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { datePickerDialog.show() }
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 문서 스캐너 호출 (카메라 촬영 전용)
                        Button(
                            onClick = {
                            // ▶ 올바른 문서 스캔 호출
                            documentScanner
                                .getStartScanIntent(context as Activity)
                                .addOnSuccessListener { intentSender ->
                                    val req = IntentSenderRequest.Builder(intentSender).build()
                                    docScanLauncher.launch(req)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "스캐너 실행 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }) {
                            Text("영수증 촬영")
                        }

                        // 갤러리에서 기존 영수증 이미지 선택
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("갤러리에서 선택")
                        }
                    }
                }
            }
        }
    }
    // ⑥ 스캔 결과 다이얼로그
    scannedIngredients?.let { ingredients ->
        AlertDialog(
            onDismissRequest = { scannedIngredients = null },
            title = { Text("스캔된 재료 및 수량") },
            text = {
                Column {
                    Text("다음 재료들이 감지되었습니다:")
                    Spacer(Modifier.height(8.dp))
                    ingredients.forEach { ingredient ->
                        Text("• ${ingredient.name} (${ingredient.quantity}개)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    ingredients.forEach { ingredient ->
                        viewModel.addIngredient(
                            Ingredient(
                                id = UUID.randomUUID().toString(),
                                name = ingredient.name,
                                quantity = ingredient.quantity,
                                expireDate = expireDate.ifBlank {
                                    LocalDate.now().plusDays(7).toString()
                                },
                                section = section
                            ), section
                        ){}
                    }
                    Toast.makeText(context, "${ingredients.size}개 추가됨", Toast.LENGTH_SHORT).show()
                    scannedIngredients = null
                    navController.popBackStack()
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { scannedIngredients = null }) { Text("취소") }
            }
        )
    }

}
@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.error,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.error,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLeadingIconColor = MaterialTheme.colorScheme.error,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.error
)
