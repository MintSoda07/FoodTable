package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Long = 0,
    val category: String = "",
    val available: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    storeId: String,
    onBack: () -> Unit = {}
) {
    val db = Firebase.firestore
    val products = remember { mutableStateListOf<Product>() }

    var query by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("name") } // name | price
    var sortAsc by remember { mutableStateOf(true) }

    var showEditor by remember { mutableStateOf(false) }
    var editing: Product? by remember { mutableStateOf(null) }
    var defaultCategoryForNew by remember { mutableStateOf<String?>(null) }

    val expandedMap = remember { mutableStateMapOf<String, Boolean>() }

    // 실시간 구독
    DisposableEffect(storeId) {
        if (storeId.isBlank()) return@DisposableEffect onDispose { }
        val ref = db.collection("merchants").document(storeId).collection("products")
        val reg = ref.addSnapshotListener { snap, _ ->
            snap ?: return@addSnapshotListener
            for (dc in snap.documentChanges) {
                val p = dc.document.toObject(Product::class.java).copy(id = dc.document.id)
                when (dc.type) {
                    DocumentChange.Type.ADDED -> if (products.none { it.id == p.id }) products.add(p)
                    DocumentChange.Type.MODIFIED -> products.indexOfFirst { it.id == p.id }.takeIf { it >= 0 }?.let { idx -> products[idx] = p }
                    DocumentChange.Type.REMOVED -> products.removeAll { it.id == p.id }
                }
            }
        }
        onDispose { reg.remove() }
    }

    // 검색/정렬 적용
    val filtered = remember(products, query, sortBy, sortAsc) {
        products
            .filter {
                val q = query.trim()
                if (q.isBlank()) true
                else it.name.contains(q, true) || it.category.contains(q, true)
            }.sortedWith(
                when (sortBy) {
                    "price" -> compareBy<Product> { it.price }.let { if (sortAsc) it else it.reversed() }
                    else -> compareBy<Product> { it.name.lowercase() }.let { if (sortAsc) it else it.reversed() }
                }
            )
    }

    val grouped = remember(filtered) {
        filtered.groupBy { it.category.ifBlank { "미분류" } }
            .toSortedMap(compareBy<String> { if (it == "미분류") "zzz" else it.lowercase() })
    }

    LaunchedEffect(grouped.keys) {
        grouped.keys.forEach { cat -> if (expandedMap[cat] == null) expandedMap[cat] = true }
        expandedMap.keys.filter { it !in grouped.keys }.forEach { expandedMap.remove(it) }
    }

    fun toggleAll(expand: Boolean) {
        grouped.keys.forEach { expandedMap[it] = expand }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("메뉴/상품 관리") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "뒤로") } },
                actions = {
                    IconButton(onClick = { toggleAll(true) }) { Icon(Icons.Default.UnfoldMore, contentDescription = "전체 펼치기") }
                    IconButton(onClick = { toggleAll(false) }) { Icon(Icons.Default.UnfoldLess, contentDescription = "전체 접기") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; defaultCategoryForNew = null; showEditor = true }) {
                Icon(Icons.Default.Add, contentDescription = "추가")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 검색/정렬 바
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("상품명/카테고리 검색") },
                    singleLine = true, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = { sortBy = if (sortBy == "name") "price" else "name" }) {
                    Icon(if (sortBy == "name") Icons.Default.SortByAlpha else Icons.Default.AttachMoney, null)
                    Spacer(Modifier.width(6.dp)); Text(if (sortBy == "name") "이름" else "가격")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { sortAsc = !sortAsc }) {
                    Icon(if (sortAsc) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null)
                }
            }

            // 리스트
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                grouped.forEach { (category, list) ->
                    item(key = "header_$category") {
                        CategoryHeader(
                            category = category,
                            count = list.size,
                            expanded = expandedMap[category] == true,
                            onToggle = { expandedMap[category] = !(expandedMap[category] ?: true) },
                            onAddInCategory = {
                                editing = null
                                defaultCategoryForNew = category
                                showEditor = true
                            }
                        )
                    }
                    if (expandedMap[category] == true) {
                        items(list, key = { it.id }) { p ->
                            ProductRow(
                                product = p,
                                onEdit = { editing = p; defaultCategoryForNew = p.category.ifBlank { "미분류" }; showEditor = true },
                                onDelete = {
                                    db.collection("merchants").document(storeId)
                                        .collection("products").document(p.id).delete()
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        ProductEditorDialog(
            initial = editing,
            defaultCategory = defaultCategoryForNew,
            onDismiss = { showEditor = false },
            onSave = { data ->
                val col = db.collection("merchants").document(storeId).collection("products")
                if (data.id.isBlank()) {
                    col.add(
                        mapOf(
                            "name" to data.name.trim(),
                            "price" to data.price,
                            "category" to data.category.trim(),
                            "available" to data.available
                        )
                    ).addOnSuccessListener { showEditor = false }
                } else {
                    col.document(data.id).set(
                        mapOf(
                            "name" to data.name.trim(),
                            "price" to data.price,
                            "category" to data.category.trim(),
                            "available" to data.available
                        )
                    ).addOnSuccessListener { showEditor = false }
                }
            }
        )
    }
}

@Composable
private fun CategoryHeader(
    category: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddInCategory: () -> Unit
) {
    ElevatedCard(onClick = onToggle) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            Spacer(Modifier.width(6.dp))
            Text(category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            AssistChip(onClick = {}, label = { Text("$count 개") }, leadingIcon = { Icon(Icons.Default.Inventory2, null) })
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onAddInCategory) { Icon(Icons.Default.Add, contentDescription = "이 카테고리에 추가") }
        }
    }
}

@Composable
private fun ProductRow(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text("${product.price}원 · ${product.category.ifBlank { "미분류" }} · ${if (product.available) "판매중" else "품절"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "수정") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "삭제") }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    initial: Product? = null,
    defaultCategory: String? = null,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var priceText by remember { mutableStateOf((initial?.price ?: 0).toString()) }
    var category by remember {
        mutableStateOf(initial?.category?.takeIf { it.isNotBlank() } ?: defaultCategory.orEmpty())
    }
    var available by remember { mutableStateOf(initial?.available ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "상품 추가" else "상품 수정") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("상품명") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = priceText, onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("가격(원)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("카테고리") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = available, onCheckedChange = { available = it })
                    Text("판매중")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && priceText.toLongOrNull() != null,
                onClick = {
                    onSave(Product(id = initial?.id ?: "", name = name, price = priceText.toLongOrNull() ?: 0, category = category, available = available))
                }
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
