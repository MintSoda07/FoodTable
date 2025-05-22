package com.bcu.foodtable.JetpackCompose.Mypage.myFridge

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.LocalDate
import androidx.compose.ui.text.input.KeyboardType
import java.util.UUID

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "재료 추가",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("재료명") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("수량") },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            val today = LocalDate.now()
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val formatted = String.format("%04d-%02d-%02d", year, month + 1, day)
                    expireDate = formatted
                },
                today.year, today.monthValue - 1, today.dayOfMonth
            ).show()
        }) {
            Text(if (expireDate.isEmpty()) "유통기한 선택" else "유통기한: $expireDate")
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("저장")
        }
    }
}
