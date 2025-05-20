package com.bcu.foodtable.JetpackCompose.Channel

import kotlinx.coroutines.tasks.await


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CategorySelector(
    selectedCategory1: String,
    selectedCategory2: String,
    onCategory1Change: (String) -> Unit,
    onCategory2Change: (String) -> Unit
) {
    var category1Options by remember { mutableStateOf<List<String>>(emptyList()) }
    var category2Options by remember { mutableStateOf<List<String>>(emptyList()) }

    // Firestore에서 카테고리 불러오기
    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()

        val doc1 = firestore.collection("C_categories").document("C_food_types").get()
        val doc2 = firestore.collection("C_categories").document("C_cooking_methods").get()

        val snapshot1 = doc1.await()
        val snapshot2 = doc2.await()

        val list1 = snapshot1.get("list") as? List<String>
        val list2 = snapshot2.get("list") as? List<String>

        if (!list1.isNullOrEmpty()) category1Options = list1
        if (!list2.isNullOrEmpty()) category2Options = list2
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        CategoryDropdown(
            label = "음식 종류",
            options = category1Options,
            selected = selectedCategory1,
            onSelectedChange = onCategory1Change
        )

        Spacer(modifier = Modifier.height(8.dp))

        CategoryDropdown(
            label = "조리 방식",
            options = category2Options,
            selected = selectedCategory2,
            onSelectedChange = onCategory2Change
        )
    }
}

@Composable
fun CategoryDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelectedChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)

                }
            }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectedChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
