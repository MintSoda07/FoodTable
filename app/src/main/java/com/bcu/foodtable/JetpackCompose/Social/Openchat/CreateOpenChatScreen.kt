// CreateOpenChatScreen.kt
package com.bcu.foodtable.JetpackCompose.Social.Openchat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.bcu.foodtable.useful.UserManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOpenChatScreen(
    navController: NavHostController,
    vm: OpenChatViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val me = UserManager.getUser() ?: return
    val myUid = me.uid
    val myNick = me.name.ifBlank { "방장" }

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var passcode by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    //
    var thumbUrl by remember { mutableStateOf<String?>(null) }
    var uploading by remember { mutableStateOf(false) }
    val storage = remember { com.google.firebase.storage.FirebaseStorage.getInstance().reference }
    val pickThumb = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                uploading = true
                val ref = storage.child("openRoomThumbs/$myUid/${System.currentTimeMillis()}.jpg")
                ref.putFile(uri).await()
                thumbUrl = ref.downloadUrl.await().toString()
            } catch (e: Exception) {
                Toast.makeText(navController.context, "썸네일 업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally { uploading = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("오픈채팅 만들기", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            //
            ElevatedCard(onClick = { pickThumb.launch("image/*") }) {
                Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (thumbUrl.isNullOrBlank()) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Text(if (uploading) "업로드 중..." else "방 사진 추가 (선택)")
                    } else {
                        coil.compose.AsyncImage(
                            model = thumbUrl,
                            contentDescription = "방 썸네일",
                            modifier = Modifier.height(140.dp).fillMaxWidth()
                        )
                        TextButton(onClick = { pickThumb.launch("image/*") }) { Text("변경") }
                    }
                }
            }

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("방 제목") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("방 소개(선택)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                                title = title.trim(),
                                desc = desc.trim(),
                                open = isPublic,
                                passcode = if (isPublic) null else passcode.ifBlank { null },
                                thumbUrl = thumbUrl,
                                ownerNickname = myNick
                            )
                            vm.joinRoom(roomId, myUid, myNick)
                            vm.sendSystem(roomId, "join", myNick)
                            navController.navigate("openchat/$roomId") { popUpTo("openchat_home") }
                        } catch (e: Exception) {
                            Toast.makeText(navController.context, e.message ?: "방 생성 실패", Toast.LENGTH_SHORT).show()
                        } finally { creating = false }
                    }
                },
                enabled = !creating && !uploading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (creating) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text("방 만들기")
            }
        }
    }
}
