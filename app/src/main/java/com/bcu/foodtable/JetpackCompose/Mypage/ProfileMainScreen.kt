package com.bcu.foodtable.JetpackCompose.Mypage

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.Mypage.Setting.SettingActivity
import com.bcu.foodtable.R
import com.bcu.foodtable.ui.health.HealthConnectActivity

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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadImageToFirebase(it, context) } }

    LaunchedEffect(Unit) { viewModel.checkIfChannelExists() }

    Scaffold(
        topBar = {
            ProfileTopBar(
                user = user,
                onChallengeClick = {
                    context.startActivity(Intent(context, HealthConnectActivity::class.java))
                }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to colorScheme.primaryContainer.copy(alpha = 0.6f),
                        0.4f to colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        1f to colorScheme.background
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
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
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(2.dp, colorScheme.primary, CircleShape)
                                .background(colorScheme.surface)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            placeholder = painterResource(id = R.drawable.baseline_person_24),
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            user.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = editedDescription,
                                onValueChange = { viewModel.editedDescription.value = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("자기소개") }
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.saveChanges() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("저장") }

                                OutlinedButton(
                                    onClick = { viewModel.cancelEdit() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("취소") }
                            }
                        } else {
                            Text(
                                user.description.ifBlank { "자기소개가 없습니다." },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(onClick = { viewModel.startEdit() }) {
                                Text("편집")
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            "소금 보유량: ${user.point}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.tertiary
                            )
                        )

                        Spacer(Modifier.height(24.dp))

                        /*** 버튼 그룹화 ***/
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.navigateToPurchase(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("소금 구매")
                            }

                            if (!hasChannel) {
                                OutlinedButton(
                                    onClick = { viewModel.navigateToChannelCreation(context) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = colorScheme.primary
                                    ),
                                    border = BorderStroke(1.dp, colorScheme.primary)
                                ) {
                                    Text("채널 생성하기")
                                }
                            }

                            Button(
                                onClick = { viewModel.navigateToHealth(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("건강 확인하기")
                            }

                            OutlinedButton(
                                onClick = { viewModel.navigateToFridge(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = colorScheme.primary
                                ),
                                border = BorderStroke(1.dp, colorScheme.primary)
                            ) {
                                Text("나의 냉장고")
                            }
                        }

                    }
                }
            }
        }
    }
}
