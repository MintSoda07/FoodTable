package com.bcu.foodtable.JetpackCompose.Mypage.CreateChannel

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
// import com.bcu.foodtable.R // Assuming R is not needed for this specific redesign preview

// Dummy ViewModel for preview if not running in full app context
// class ChannelCreationViewModel : ViewModel() {
//     val channelName = MutableStateFlow("")
//     val channelDescription = MutableStateFlow("")
//     val selectedImageUri = MutableStateFlow<Uri?>(null)
//     val selectedBackgroundUri = MutableStateFlow<Uri?>(null)
//     val isUploading = MutableStateFlow(false)
//     fun setImageUri(uri: Uri?) { selectedImageUri.value = uri }
//     fun setBackgroundUri(uri: Uri?) { selectedBackgroundUri.value = uri }
//     fun createChannel(context: android.content.Context, onComplete: () -> Unit) { /* ... */ }
// }


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

    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setImageUri(uri)
    }
    val backgroundImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.setBackgroundUri(uri) }

    val primaryColor = Color(0xFFD32F2F)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("새 채널 만들기", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Text(
                text = "채널 대표 이미지",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            val profileImageShape = CircleShape // Define shape
            ImagePicker(
                label = "프로필 이미지",
                selectedUri = selectedImageUri,
                placeholderIcon = Icons.Rounded.AddAPhoto,
                placeholderText = "탭하여 프로필 사진 추가",
                onClick = { profileImageLauncher.launch("image/*") },
                shape = profileImageShape, // <-- PASSING THE SHAPE
                modifier = Modifier
                    .size(120.dp)
                    .clip(profileImageShape),
                iconSize = 48.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "채널 배경 이미지",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            val backgroundImageShape = RoundedCornerShape(12.dp) // Define shape
            ImagePicker(
                label = "배경 이미지",
                selectedUri = selectedBackgroundUri,
                placeholderIcon = Icons.Filled.ImageSearch,
                placeholderText = "탭하여 배경 이미지 추가",
                onClick = { backgroundImageLauncher.launch("image/*") },
                shape = backgroundImageShape, // <-- PASSING THE SHAPE
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(backgroundImageShape),
                iconSize = 56.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "채널 상세 정보",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            OutlinedTextField(
                value = channelName,
                onValueChange = { viewModel.channelName.value = it },
                label = { Text("채널 이름") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = "채널 이름 아이콘")
                },
                singleLine = true
            )

            OutlinedTextField(
                value = channelDescription,
                onValueChange = { viewModel.channelDescription.value = it },
                label = { Text("채널 설명") },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Outlined.Description, contentDescription = "채널 설명 아이콘")
                },
                maxLines = 5
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                ) {
                    Text("취소")
                }

                Button(
                    onClick = {
                        viewModel.createChannel(context) {
                            onFinish()
                        }
                    },
                    enabled = !isUploading && channelName.isNotBlank() && selectedImageUri != null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("생성 중...", color = Color.White)
                    } else {
                        Text("채널 생성", color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ImagePicker(
    label: String,
    selectedUri: Uri?,
    placeholderIcon: ImageVector,
    placeholderText: String,
    onClick: () -> Unit,
    shape: Shape, // Parameter is already here
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp
) {
    val lightGray = Color(0xFFF5F5F5)
    val placeholderColor = Color.DarkGray

    Box(
        modifier = modifier
            .background(
                color = if (selectedUri == null) lightGray else Color.Transparent,
            )
            .border(
                width = 1.dp,
                color = if (selectedUri == null) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else Color.Transparent,
                shape = shape // Uses the passed shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selectedUri != null) {
            Image(
                painter = rememberAsyncImagePainter(selectedUri),
                contentDescription = label,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = placeholderIcon,
                    contentDescription = label,
                    modifier = Modifier.size(iconSize),
                    tint = placeholderColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = placeholderText,
                    color = placeholderColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}