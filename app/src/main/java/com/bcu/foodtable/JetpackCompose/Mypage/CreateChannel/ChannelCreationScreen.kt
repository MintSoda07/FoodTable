package com.bcu.foodtable.JetpackCompose.Mypage.CreateChannel

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.bcu.foodtable.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelCreationScreen(
    viewModel: ChannelCreationViewModel = viewModel(),
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val channelName by viewModel.channelName.collectAsState()
    val channelDescription by viewModel.channelDescription.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val selectedBackgroundUri by viewModel.selectedBackgroundUri.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setImageUri(uri)
    }
    val launcherForBackground = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.setBackgroundUri(uri) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("채널 생성하기") })
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //  대표 이미지

            Text("대표 이미지")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(10.dp)
                    .background(
                        color = Color(0x00FFFFFF),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFC62828),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "대표 이미지",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("대표 이미지를 선택하세요", color = Color(0xFFC62828))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            //  배경 이미지
            Text("배경 이미지")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(10.dp)
                    .background(
                        color = Color(0x00FFFFFF),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFC62828),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .clickable { launcherForBackground.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedBackgroundUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedBackgroundUri),
                        contentDescription = "배경 이미지",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("배경 이미지를 선택하세요", color = Color(0xFFC62828))
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = channelName,
                onValueChange = { viewModel.channelName.value = it },
                label = { Text("채널 이름") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = channelDescription,
                onValueChange = { viewModel.channelDescription.value = it },
                label = { Text("채널 설명") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        viewModel.createChannel(context) {
                            (context as? Activity)?.finish()
                            onFinish()
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("채널 생성", color = Color.White)
                }

                Button(
                    onClick = {
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("취소", color = Color.White)
                }
            }
        }
    }

}