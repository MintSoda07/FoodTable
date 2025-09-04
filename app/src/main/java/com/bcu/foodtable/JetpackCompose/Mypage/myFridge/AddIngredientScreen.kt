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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIngredientScreen(
    viewModel: FridgeViewModel,
    navController: NavController,
    section: String
) {
    val context = LocalContext.current

    // 입력값
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var expireDate by rememberSaveable { mutableStateOf("") }

    val qtyInt = quantity.toIntOrNull() ?: 0
    val isValid = name.isNotBlank() && qtyInt > 0 && expireDate.isNotBlank()

    // 스캔 상태
    var isScanning by remember { mutableStateOf(false) }
    var scannedIngredients by remember { mutableStateOf<List<IngredientExtracted>?>(null) }

    // 날짜
    val datePickerDialog = remember {
        val today = LocalDate.now()
        DatePickerDialog(
            context,
            { _, y, m, d -> expireDate = "%04d-%02d-%02d".format(y, m + 1, d) },
            today.year, today.monthValue - 1, today.dayOfMonth
        )
    }

    // GMS Document Scanner
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build()
    }
    val documentScanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val docScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages.orEmpty()
            val firstUri = pages.firstOrNull()?.getImageUri()
            if (firstUri != null) {
                isScanning = true
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, firstUri)
                val base64 = encodeImageToBase64(bitmap)
                viewModel.sendToClovaOCR(
                    base64Image = base64,
                    onSuccess = { ocrText ->
                        viewModel.extractIngredientsWithQuantityUsingAI(
                            ocrText = ocrText,
                            onResult = { ai ->
                                isScanning = false
                                if (ai.isEmpty()) {
                                    Toast.makeText(context, "AI가 유효한 재료를 찾지 못했습니다.", Toast.LENGTH_LONG).show()
                                } else {
                                    scannedIngredients = ai
                                }
                            },
                            onError = { msg ->
                                isScanning = false
                                Toast.makeText(context, "AI 호출 실패: $msg", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    onError = { msg ->
                        isScanning = false
                        Toast.makeText(context, "Clova OCR 실패: $msg", Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                Toast.makeText(context, "유효한 스캔 결과가 없습니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isScanning = true
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            val base64 = encodeImageToBase64(bitmap)
            viewModel.sendToClovaOCR(
                base64Image = base64,
                onSuccess = { ocrText ->
                    viewModel.extractIngredientsWithQuantityUsingAI(
                        ocrText = ocrText,
                        onResult = { ai ->
                            isScanning = false
                            if (ai.isEmpty()) {
                                Toast.makeText(context, "AI가 유효한 재료를 찾지 못했습니다.", Toast.LENGTH_LONG).show()
                            } else {
                                scannedIngredients = ai
                            }
                        },
                        onError = { msg ->
                            isScanning = false
                            Toast.makeText(context, "AI 호출 실패: $msg", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onError = { msg ->
                    isScanning = false
                    Toast.makeText(context, "Clova OCR 실패: $msg", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Scaffold에서 topBar 제거 → 내부 헤더 사용
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // 우리가 직접 statusBar 패딩 적용
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Button(
                    onClick = {
                        val item = Ingredient(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            quantity = qtyInt,
                            expireDate = expireDate,
                            section = section
                        )
                        viewModel.addIngredient(item, section) {
                            Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("저장하기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.statusBars) // 상태바만큼 안전패딩
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                // 뒤로가기: 배경색 제거, 좌측 고정
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기"

                    )
                }

                // 제목: 정확히 중앙 정렬
                Text(
                    text = "새로운 재료 추가",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center),
                    maxLines = 1
                )
            }

            // 섹션 배지
            AssistChip(
                onClick = {},
                label = { Text("'$section' 섹션") },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )

            // 입력 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // 재료명
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("재료명") },
                        leadingIcon = { Icon(Icons.Default.RestaurantMenu, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = name.isBlank(),
                        supportingText = {
                            if (name.isBlank()) Text("재료명을 입력하세요.", color = MaterialTheme.colorScheme.error)
                        }
                    )

                    // 수량
                    Column {
                        Text("수량", style = MaterialTheme.typography.labelLarge)
                        QuantityStepper(
                            value = qtyInt.coerceAtLeast(0),
                            onChange = { new -> quantity = new.coerceAtLeast(0).toString() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 유통기한
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("유통기한", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = expireDate.isNotBlank(),
                                onClick = { datePickerDialog.show() },
                                label = { Text(if (expireDate.isBlank()) "날짜 선택" else expireDate) },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (expireDate.isNotBlank()) {
                                TextButton(onClick = { expireDate = "" }) { Text("지우기") }
                            }
                        }
                        if (expireDate.isBlank()) {
                            Text(
                                "유통기한을 선택하세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // OCR / 갤러리
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("영수증에서 자동추출", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                (context as? Activity)?.let { act ->
                                    documentScanner
                                        .getStartScanIntent(act)
                                        .addOnSuccessListener { sender ->
                                            docScanLauncher.launch(IntentSenderRequest.Builder(sender).build())
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "스캐너 실행 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                }
                            },
                            enabled = !isScanning
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("영수증 촬영")
                        }
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            enabled = !isScanning
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("갤러리에서 선택")
                        }
                    }
                    if (isScanning) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("인식 중… 잠시만요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    // 스캔 결과 다이얼로그
    scannedIngredients?.let { list ->
        ScannedIngredientsDialog(
            section = section,
            items = list,
            onConfirmAdd = { itemsToAdd, bulkExpire ->
                val finalExpire = bulkExpire.ifBlank {
                    expireDate.ifBlank { LocalDate.now().plusDays(7).toString() }
                }
                itemsToAdd.forEach { ing ->
                    viewModel.addIngredient(
                        Ingredient(
                            id = UUID.randomUUID().toString(),
                            name = ing.name,
                            quantity = ing.quantity,
                            expireDate = finalExpire,
                            section = section
                        ),
                        section
                    ) {}
                }
                Toast.makeText(context, "${itemsToAdd.size}개 추가됨", Toast.LENGTH_SHORT).show()
                scannedIngredients = null
                navController.popBackStack()
            },
            onDismiss = { scannedIngredients = null }
        )
    }
}

@Composable
private fun QuantityStepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onChange(value - 1) },
            enabled = value > 0,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Icon(Icons.Default.Remove, contentDescription = "감소")
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = value.toString(), style = MaterialTheme.typography.titleSmall)
        }
        IconButton(
            onClick = { onChange(value + 1) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Icon(Icons.Default.Add, contentDescription = "증가")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannedIngredientsDialog(
    section: String,
    items: List<IngredientExtracted>,
    onConfirmAdd: (selected: List<IngredientExtracted>, bulkExpireDate: String) -> Unit,
    onDismiss: () -> Unit
) {
    // 체크 상태: 최초 한 번만 생성, 이후 유지
    val selected = remember(items) {
        mutableStateListOf<Boolean>().apply { addAll(List(items.size) { true }) }
    }
    var bulkExpireDate by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("스캔된 재료 • $section") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                if (items.isEmpty()) {
                    Text("감지된 재료가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    // 항목 리스트
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items.forEachIndexed { idx, ing ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .clickable { selected[idx] = !selected[idx] } // ✅ 한 곳만 토글
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected[idx],
                                    onCheckedChange = { checked -> selected[idx] = checked } // ✅ 정상 토글
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("• ${ing.name} (${ ing.quantity }개)")
                            }
                        }
                    }
                }

                // 일괄 유통기한 (지우기 우측 정렬로 밀림 방지)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { showPicker = true },
                        label = { Text(if (bulkExpireDate.isBlank()) "일괄 유통기한 선택" else bulkExpireDate) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                    )
                    Spacer(Modifier.weight(1f))
                    if (bulkExpireDate.isNotBlank()) {
                        TextButton(onClick = { bulkExpireDate = "" }) {
                            Text("지우기")
                        }
                    }
                }

                if (showPicker) {
                    val context = LocalContext.current
                    val today = LocalDate.now()
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            bulkExpireDate = "%04d-%02d-%02d".format(y, m + 1, d)
                            showPicker = false
                        },
                        today.year, today.monthValue - 1, today.dayOfMonth
                    ).show()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val toAdd = items.filterIndexed { i, _ -> selected.getOrNull(i) == true } //
                onConfirmAdd(toAdd, bulkExpireDate)
            }) { Text("선택 추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}