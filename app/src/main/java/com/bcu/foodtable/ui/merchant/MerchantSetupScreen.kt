package com.bcu.foodtable.ui.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun MerchantSetupScreen(
    onCompleted: (storeId: String) -> Unit,
    defaultStoreName: String? = null
) {
    val uid = Firebase.auth.currentUser?.uid.orEmpty()
    val db = Firebase.firestore

    var storeName by remember { mutableStateOf(defaultStoreName ?: "") }
    val roleOptions = listOf("OWNER", "MANAGER", "STAFF")
    var role by remember { mutableStateOf("OWNER") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("가맹 기본 설정", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = storeName,
            onValueChange = { storeName = it },
            singleLine = true,
            label = { Text("상호명") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("직급", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        // 간단한 선택 버튼들
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            roleOptions.forEach { opt ->
                FilterChip(
                    selected = role == opt,
                    onClick = { role = opt },
                    label = { Text(opt) }
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Button(
            enabled = storeName.isNotBlank() && !saving,
            onClick = {
                error = null
                saving = true
                val sid = "store_${uid}" // 간단 생성(원하면 Random/autoId 사용)
                val storeRef = db.collection("merchants").document(sid)
                val userRef = db.collection("user").document(uid)

                val batch = db.batch()
                batch.set(
                    storeRef,
                    mapOf(
                        "storeId" to sid,
                        "storeName" to storeName.trim(),
                        "ownerUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "status" to "ACTIVE"
                    ),
                    SetOptions.merge()
                )
                batch.set(
                    storeRef.collection("staff").document(uid),
                    mapOf(
                        "uid" to uid,
                        "role" to role,
                        "joinedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                batch.set(
                    userRef,
                    mapOf(
                        "roles" to listOf("user", "merchant"),
                        "storeId" to sid,
                        "storeName" to storeName.trim(),
                        "merchantSince" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                batch.commit()
                    .addOnSuccessListener {
                        saving = false
                        onCompleted(sid)
                    }
                    .addOnFailureListener {
                        saving = false
                        error = it.localizedMessage
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            else Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (saving) "저장 중..." else "저장")
        }

        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

/* 간단한 FlowRow: Accompanist 없이도 OK */
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    Row(modifier = modifier, horizontalArrangement = horizontalArrangement) {
        content()
    }
}
