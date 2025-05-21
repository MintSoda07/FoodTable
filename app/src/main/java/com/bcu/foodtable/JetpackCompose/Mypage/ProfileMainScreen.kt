package com.bcu.foodtable.JetpackCompose.Mypage

import com.bcu.foodtable.JetpackCompose.Mypage.Setting.SettingActivity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.User
import com.bcu.foodtable.ui.health.HealthConnectActivity
import com.bcu.foodtable.ui.myPage.myFridge.FridgeActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMainScreen(
    paddingValues: PaddingValues,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val user by viewModel.user.collectAsState()
    val hasChannel by viewModel.hasChannel.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val editedDescription by viewModel.editedDescription.collectAsState()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadImageToFirebase(it, context) }
    }

    LaunchedEffect(Unit) {
        viewModel.checkIfChannelExists()
    }

    Scaffold(
        topBar = {
            ProfileTopBar(user = user, onChallengeClick = {
                context.startActivity(Intent(context, HealthConnectActivity::class.java))
            })
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {

                    //  설정 버튼 - 오른쪽 상단
                    IconButton(
                        onClick = {
                            context.startActivity(Intent(context, SettingActivity::class.java))
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_settings_24),
                            contentDescription = "설정",
                            tint = colorScheme.primary
                        )
                    }

                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = imageUri ?: user.image,
                            contentDescription = "프로필 이미지",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary.copy(alpha = 0.2f))
                                .clickable { launcher.launch("image/*") },
                            placeholder = painterResource(id = R.drawable.baseline_person_24),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = colorScheme.onSurface
                            )
                        )

                        if (isEditing) {
                            OutlinedTextField(
                                value = editedDescription,
                                onValueChange = { viewModel.editedDescription.value = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("자기소개") }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(onClick = { viewModel.saveChanges() }) {
                                    Text("저장")
                                }
                                OutlinedButton(onClick = { viewModel.cancelEdit() }) {
                                    Text("취소")
                                }
                            }
                        } else {
                            Text(
                                text = user.description.ifBlank { "자기소개가 없습니다." },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colorScheme.onSurfaceVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { viewModel.startEdit() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = colorScheme.primary
                                )
                            ) {
                                Text("편집")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "소금 보유량: ${user.point}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {

                            Button(
                                onClick = { viewModel.navigateToPurchase(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = colorScheme.onPrimary
                                )
                            ) {
                                Text("소금 구매")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!hasChannel) {
                            Button(
                                onClick = { viewModel.navigateToChannelCreation(context) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.secondary,
                                    contentColor = colorScheme.onSecondary
                                )
                            ) {
                                Text("채널 생성하기")
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = { viewModel.navigateToHealth(context) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.tertiary,
                                contentColor = colorScheme.onTertiary
                            )
                        ) {
                            Text("건강 확인하기")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.navigateToFridge(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("나의 냉장고")
                        }
                    }
                }
            }
        }
    }
}
