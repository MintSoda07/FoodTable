package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.app.DatePickerDialog
import android.widget.Toast
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
    // --- 기존 로직은 그대로 유지합니다 ---
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var expireDate by remember { mutableStateOf("") }
    val context = LocalContext.current

    val datePickerDialog = remember {
        val today = LocalDate.now()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                // 월(month)은 0부터 시작하므로 +1 해줘야 합니다.
                val formatted = String.format("%04d-%02d-%02d", year, month + 1, day)
                expireDate = formatted
            },
            today.year,
            today.monthValue - 1, // DatePickerDialog는 월을 0부터 계산
            today.dayOfMonth
        )
    }

    // --- 여기부터 UI 디자인을 업그레이드합니다 ---
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
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            // 하단 저장 버튼
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
                    .padding(start = 16.dp, end = 16.dp, bottom = 64.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("저장하기", style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "어떤 재료를 추가할까요?",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "냉장고의 '$section' 섹션에 추가됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 재료명 입력
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("재료명") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.RestaurantMenu, contentDescription = "재료명")
                },
                singleLine = true
            )

            // 수량 입력
            OutlinedTextField(
                value = quantity,
                onValueChange = { if (it.all(Char::isDigit)) quantity = it },
                label = { Text("수량") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Numbers, contentDescription = "수량")
                },
                singleLine = true
            )

            // 유통기한 선택

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = expireDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("유통기한") },
                    modifier = Modifier
                        .fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = "유통기한")
                    },
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "날짜 선택",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )
                // 투명 클릭 레이어
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .clickable { datePickerDialog.show() }
                )
            }

        }
    }
}
