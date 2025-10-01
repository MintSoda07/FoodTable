import android.app.Activity
import android.app.DatePickerDialog
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.FridgeViewModel
import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.Ingredient
import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.IngredientExtracted
import com.bcu.foodtable.JetpackCompose.Mypage.myFridge.encodeImageToBase64
import com.bcu.foodtable.R
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.*

// ▼▼ 필요: encodeImageToBase64, Ingredient, IngredientExtracted, FridgeViewModel 등 기존 타입/함수는 그대로 사용 ▼▼

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

    // ===== UI =====
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 10.dp,
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
                    enabled = isValid && !isScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .height(52.dp),
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
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "새로운 재료 추가",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("재료명") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = name.isBlank(),
                        supportingText = {
                            if (name.isBlank()) Text("재료명을 입력하세요.", color = MaterialTheme.colorScheme.error)
                        }
                    )

                    Column {
                        Text("수량", style = MaterialTheme.typography.labelLarge)
                        QuantityStepper(
                            value = qtyInt.coerceAtLeast(0),
                            onChange = { new -> quantity = new.coerceAtLeast(0).toString() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

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

                    if (isScanning) {
                        // ✅ 인식 중: scan.lottie 표시
                        ScanLottiePanel()
                    } else {
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
                        Text(
                            "영수증 촬영 또는 갤러리에서 선택하세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    // 스캔 결과 다이얼로그 (기존 그대로)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannedIngredientsDialog(
    section: String,
    items: List<IngredientExtracted>,
    onConfirmAdd: (selected: List<IngredientExtracted>, bulkExpireDate: String) -> Unit,
    onDismiss: () -> Unit
) {
    // 선택/수량 상태
    val selected = remember(items) {
        mutableStateMapOf<Int, Boolean>().apply { items.indices.forEach { put(it, true) } }
    }
    val qtyMap = remember(items) {
        mutableStateMapOf<Int, Int>().apply { items.forEachIndexed { i, ing -> put(i, ing.quantity.coerceAtLeast(1)) } }
    }

    // 일괄 유통기한
    var bulkExpireDate by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    // 전체선택 상태
    val allChecked = selected.values.all { it }
    val anyChecked = selected.values.any { it }

    fun toggleAll(check: Boolean) {
        items.indices.forEach { idx -> selected[idx] = check }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("스캔된 재료 • $section", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${selected.values.count { it }}개 선택됨 / 총 ${items.size}개",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // 상단 툴바: 전체선택 / 일괄 유통기한 / 프리셋
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = allChecked,
                        onClick = { toggleAll(!allChecked) },
                        label = { Text(if (allChecked) "전체해제" else "전체선택") }
                    )
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = { showPicker = true },
                        label = { Text(if (bulkExpireDate.isBlank()) "일괄 유통기한" else bulkExpireDate) },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                    )
                }
                Row {
                    // 빠른 프리셋: +3, +7, +14일
                    fun setPreset(days: Long) {
                        val d = LocalDate.now().plusDays(days)
                        bulkExpireDate = "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
                    }
                    listOf(3L to "+3일", 7L to "+7일", 14L to "+14일").forEachIndexed { i, (d, label) ->
                        AssistChip(onClick = { setPreset(d) }, label = { Text(label) })
                        if (i != 2) Spacer(Modifier.width(6.dp))
                    }
                }

                // 항목 리스트
                if (items.isEmpty()) {
                    Text("감지된 재료가 없습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    // 길어질 수 있으니 LazyColumn
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items.size, key = { it }) { idx ->
                            val ing = items[idx]
                            val checked = selected[idx] == true
                            val q = qtyMap[idx] ?: 1

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { selected[idx] = it }
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(ing.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "추천 수량: ${ing.quantity}개",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    QuantityChip(value = q, enabled = checked, onMinus = {
                                        qtyMap[idx] = (q - 1).coerceAtLeast(1)
                                    }, onPlus = {
                                        qtyMap[idx] = (q + 1).coerceAtLeast(1)
                                    })
                                }
                            }
                        }
                    }
                }

                if (showPicker) {
                    val context = LocalContext.current
                    val today = LocalDate.now()
                    // DatePickerDialog는 Compose 바깥 UI라서 즉시 show
                    DisposableEffect(Unit) {
                        val dlg = DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                bulkExpireDate = "%04d-%02d-%02d".format(y, m + 1, d)
                                showPicker = false
                            },
                            today.year, today.monthValue - 1, today.dayOfMonth
                        )
                        dlg.setOnDismissListener { showPicker = false }
                        dlg.show()
                        onDispose { dlg.setOnDismissListener(null) }
                    }
                }
            }
        },
        confirmButton = {
            val canConfirm = anyChecked
            val count = selected.values.count { it }
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val out = buildList {
                        selected.forEach { (idx, ok) ->
                            if (ok) {
                                val src = items[idx]
                                add(src.copy(quantity = (qtyMap[idx] ?: src.quantity).coerceAtLeast(1)))
                            }
                        }
                    }
                    onConfirmAdd(out, bulkExpireDate)
                }
            ) {
                Text(if (canConfirm) "선택 추가 ($count)" else "선택 추가")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

/* --- 수량 칩(작은 스텝퍼) --- */
@Composable
private fun QuantityChip(
    value: Int,
    enabled: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val bg = if (enabled) MaterialTheme.colorScheme.surface else
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    val content = if (enabled) MaterialTheme.colorScheme.onSurface else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMinus, enabled = enabled && value > 1) {
                Icon(Icons.Default.Remove, contentDescription = "감소", tint = content)
            }
            Text(
                "$value",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = content
            )
            IconButton(onClick = onPlus, enabled = enabled) {
                Icon(Icons.Default.Add, contentDescription = "증가", tint = content)
            }
        }
    }
}


/* ---------- Lottie UI ---------- */

@Composable
private fun ScanLottiePanel() {
    val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.scan))
    val progress by animateLottieCompositionAsState(
        composition = comp,
        iterations = LottieConstants.IterateForever,
        speed = 1.0f
    )
    val listBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(listBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(
                composition = comp,
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1.6f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "영수증 인식 중… 잠시만 기다려주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ---------- 수량 스텝퍼 (기존) ---------- */

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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onChange(value - 1) }, enabled = value > 0, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Remove, contentDescription = "감소")
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(text = value.toString(), style = MaterialTheme.typography.titleSmall)
        }
        IconButton(onClick = { onChange(value + 1) }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Add, contentDescription = "증가")
        }
    }
}

/* ---------- 스캔 결과 다이얼로그 (사용자 코드 그대로 유지) ---------- */
// ScannedIngredientsDialog(...) 기존 코드 사용
