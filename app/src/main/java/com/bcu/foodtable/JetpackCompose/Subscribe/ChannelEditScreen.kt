// --- import 구문 ---
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.bcu.foodtable.JetpackCompose.Mypage.CreateChannel.ImagePicker
import com.bcu.foodtable.ui.home.Screen
import com.bcu.foodtable.useful.Channel
import kotlinx.coroutines.tasks.await

// --- ChannelEditScreen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelEditScreen(
    initialChannel: Channel,
    navController: NavHostController,
    viewModel: ChannelEditViewModel = viewModel()
) {
    val context = LocalContext.current
    val channelName by viewModel.channelName.collectAsState()
    val channelDescription by viewModel.channelDescription.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val selectedBackgroundUri by viewModel.selectedBackgroundUri.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    // 최초 진입시 초기값 세팅
    LaunchedEffect(initialChannel) {
        viewModel.setInitialData(initialChannel)
    }

    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.setImageUri(uri) }
    val backgroundImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.setBackgroundUri(uri) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "채널 수정",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 25.dp) // 여기서만 살짝 위로!
                    )
                },
                actions = {
                    TextButton(
                        enabled = !isUploading && channelName.isNotBlank(),
                        onClick = {
                            viewModel.updateChannel(context) {
                                navController.navigate("channelView/${Uri.encode(channelName)}") {
                                    popUpTo("channelView/${Uri.encode(channelName)}") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        modifier = Modifier.padding(bottom = 25.dp) // 여기도 살짝만 위로!
                    ) {
                        Text("저장", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "채널 대표 이미지",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
            ImagePicker(
                label = "프로필 이미지",
                selectedUri = selectedImageUri,
                placeholderIcon = Icons.Rounded.AddAPhoto,
                placeholderText = "탭하여 프로필 사진 변경",
                onClick = { profileImageLauncher.launch("image/*") },
                shape = CircleShape,
                modifier = Modifier.size(120.dp).clip(CircleShape),
                iconSize = 48.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "채널 배경 이미지",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
            ImagePicker(
                label = "배경 이미지",
                selectedUri = selectedBackgroundUri,
                placeholderIcon = Icons.Filled.ImageSearch,
                placeholderText = "탭하여 배경 이미지 변경",
                onClick = { backgroundImageLauncher.launch("image/*") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
                    .clip(RoundedCornerShape(12.dp)),
                iconSize = 56.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "채널 상세 정보",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = channelName,
                onValueChange = { viewModel.channelName.value = it },
                label = { Text("채널 이름") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) },
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
                leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                maxLines = 5
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("취소")
                }

                Button(
                    onClick = {
                        viewModel.deleteChannelAndRecipes(context) {
                            // **삭제 후 → 구독화면으로 이동**
                            navController.navigate(Screen.Subscribe.route) {
                                popUpTo(Screen.Subscribe.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("삭제하기", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
@Composable
fun ChannelEditScreenLoader(
    channelName: String,
    navController: NavHostController
) {
    var channel by remember { mutableStateOf<Channel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // 최초 진입 시 Firestore에서 channelName == ... 으로 검색
    LaunchedEffect(channelName) {
        isLoading = true
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val query = db.collection("channel").whereEqualTo("name", channelName).get().await()
        val doc = query.documents.firstOrNull()
        channel = doc?.toObject(Channel::class.java)?.copy(documentId = doc.id)
        isLoading = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (channel != null) {
        ChannelEditScreen(
            initialChannel = channel!!,
            navController = navController
        )
    } else {
        // Not found UI
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("채널 정보를 찾을 수 없습니다.")
        }
    }
}
