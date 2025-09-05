// CreateOpenChatScreen.kt
package com.bcu.foodtable.JetpackCompose.Social.Openchat

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOpenChatScreen(
    navController: NavHostController,
    vm: OpenChatViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val me = com.bcu.foodtable.useful.UserManager.getUser() ?: return
    val myUid = me.uid
    val myNick = me.name.ifBlank { "방장" }

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var passcode by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                title = { Text("오픈채팅 만들기", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("방 제목") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("방 소개(선택)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("공개 방", style = MaterialTheme.typography.titleMedium)
                Switch(checked = isPublic, onCheckedChange = { isPublic = it })
            }
            if (!isPublic) {
                OutlinedTextField(value = passcode, onValueChange = { passcode = it }, label = { Text("입장 비밀번호") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(navController.context, "방 제목을 입력하세요", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        try {
                            creating = true
                            val roomId = vm.createRoom(
                                ownerUid = myUid,
                                ownerNickname = myNick,
                                title = title.trim(),
                                desc = desc.trim(),
                                open = isPublic,
                                passcode = if (isPublic) null else passcode.ifBlank { null }
                            )
                            vm.joinRoom(roomId, myUid, myNick)
                            vm.sendSystem(roomId, "join", myNick)
                            navController.navigate("openchat/$roomId") { popUpTo("openchat_home") }
                        } catch (e: Exception) {
                            Toast.makeText(navController.context, e.message ?: "방 생성 실패", Toast.LENGTH_SHORT).show()
                        } finally {
                            creating = false
                        }
                    }
                },
                enabled = !creating,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (creating) CircularProgressIndicator(strokeWidth = 2.dp) else Text("방 만들기")
            }
        }
    }
}
