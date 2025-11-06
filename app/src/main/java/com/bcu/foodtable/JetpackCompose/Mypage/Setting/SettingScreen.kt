package com.bcu.foodtable.JetpackCompose.Mypage.Setting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.Mypage.ProfileViewModel
import com.bcu.foodtable.JetpackCompose.Mypage.Setting.MyRecipe.MyRecipesActivity
import com.bcu.foodtable.R
import com.bcu.foodtable.ui.theme.WarmLightColorScheme
import kotlinx.coroutines.launch

@Composable
fun SettingScreen(
    context: Context,
    viewModel: SettingViewModel,
    onRequestPermissions: () -> Unit
) {
    MaterialTheme(
        colorScheme = WarmLightColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        SettingScreenContent(
            context = context,
            viewModel = viewModel,
            onRequestPermissions = onRequestPermissions
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingScreenContent(
    context: Context,
    viewModel: SettingViewModel,
    onRequestPermissions: () -> Unit,
    profileVM: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val color = MaterialTheme.colorScheme
    val healthGranted by viewModel.healthPermissionGranted.collectAsState()
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pushEnabled by rememberSaveable { mutableStateOf(false) }
    var darkEnabled by rememberSaveable { mutableStateOf(false) }

    val user by profileVM.user.collectAsState()
    val imageUri by profileVM.imageUri.collectAsState()
    val isEditing by profileVM.isEditing.collectAsState()
    val editedDescription by profileVM.editedDescription.collectAsState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { profileVM.uploadImageToFirebase(it, context) } }

    Scaffold(
        containerColor = color.primaryContainer, // TOP 영역과 동일
        snackbarHost = { SnackbarHost(host) },
        topBar = {
            TopAppBar(
                title = { Text("마이페이지", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color.onPrimaryContainer.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = color.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = color.primaryContainer,
                    titleContentColor = color.onPrimaryContainer,
                    navigationIconContentColor = color.onPrimaryContainer,
                    actionIconContentColor = color.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 본문 배경
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(color.surface, color.surfaceColorAtElevation(2.dp))
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                /* -------------------- 내 정보 (실제 동작) -------------------- */
                item {
                    SectionCard(title = "내 정보") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = imageUri ?: user.image,
                                contentDescription = "프로필 이미지",
                                placeholder = painterResource(id = R.drawable.baseline_person_24),
                                error = painterResource(id = R.drawable.baseline_person_24),
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePicker.launch("image/*") }
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = user.name.ifBlank { "이름 없음" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                // UID 숨김
                            }
                            TextButton(onClick = {
                                if (isEditing) profileVM.cancelEdit() else profileVM.startEdit()
                            }) { Text(if (isEditing) "취소" else "수정") }
                        }

                        Spacer(Modifier.height(10.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = editedDescription,
                                onValueChange = { profileVM.editedDescription.value = it },
                                label = { Text("소개글") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp, max = 160.dp),
                                maxLines = 6,
                                singleLine = false
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { profileVM.saveChanges() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("저장") }
                        } else {
                            // 편집 전에도 라벨 "소개글"이 보이도록
                            Text(
                                text = "소개글",
                                style = MaterialTheme.typography.labelLarge,
                                color = color.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = user.description.ifBlank { "소개글이 없습니다. 상단의 ‘수정’을 눌러 작성해 보세요." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = color.onSurface
                            )
                        }
                    }
                }

                /* -------------------- 내 활동 (더미 항목) -------------------- */
                item {
                    SectionCard(title = "내 활동") {
                        SettingClickableRow(
                            leadingIcon = Icons.Default.ListAlt,
                            title = "내가 등록한 레시피",
                            subtitle = "올린 레시피 목록 보기",
                            trailing = { Icon(Icons.Outlined.NavigateNext, null, tint = color.onSurfaceVariant) }
                        ) {
                            context.startActivity(Intent(context, MyRecipesActivity::class.java))
                        }

                        Divider(thickness = 0.8.dp, color = color.outlineVariant)

                        SettingClickableRow(
                            leadingIcon = Icons.Default.Event,
                            title = "약속 확인",
                            subtitle = "내 일정/약속 모아보기",
                            trailing = { Icon(Icons.Outlined.NavigateNext, null, tint = color.onSurfaceVariant) }
                        ) { scope.launch { host.showSnackbar("약속 확인(더미)") } }

                        Divider(thickness = 0.8.dp, color = color.outlineVariant)

                        SettingClickableRow(
                            leadingIcon = Icons.Default.Receipt,
                            title = "결제 내역",
                            subtitle = "소금페이 결제 기록",
                            trailing = { Icon(Icons.Outlined.NavigateNext, null, tint = color.onSurfaceVariant) }
                        ) { scope.launch { host.showSnackbar("결제 내역(더미)") } }

                        Divider(thickness = 0.8.dp, color = color.outlineVariant)

                        SettingClickableRow(
                            leadingIcon = Icons.Default.QuestionAnswer,
                            title = "문의",
                            subtitle = "문제 신고 및 피드백",
                            trailing = { Icon(Icons.Outlined.NavigateNext, null, tint = color.onSurfaceVariant) }
                        ) { scope.launch { host.showSnackbar("문의(더미)") } }
                    }
                }

                /* -------------------- 연동 -------------------- */
                item {
                    SectionCard(title = "연동") {
                        SettingSwitchRow(
                            leadingIcon = Icons.Default.SportsGymnastics,
                            title = "헬스 권한",
                            subtitle = "Health Connect 연동으로 건강 데이터 사용",
                            checked = healthGranted,
                            onCheckedChange = { on ->
                                if (on) {
                                    HealthPrefs.setEnabled(context, true)
                                    if (!viewModel.isHealthConnectInstalled()) {
                                        viewModel.openPlayStoreForHealthConnect(context)
                                    } else {
                                        onRequestPermissions()
                                    }
                                } else {
                                    HealthPrefs.setEnabled(context, false)
                                    viewModel.revokeHealthPermissions()
                                }
                            }
                        )

                        Divider(thickness = 0.8.dp, color = color.outlineVariant)

                        SettingClickableRow(
                            leadingIcon = Icons.Default.PrivacyTip,
                            title = "개인정보 처리방침",
                            subtitle = "서비스 이용 전에 확인하세요",
                            trailing = { Icon(Icons.Outlined.NavigateNext, null, tint = color.onSurfaceVariant) }
                        ) { openWeb(context, "https://your.privacy.policy.link") }
                    }
                }

                /* -------------------- 환경설정 -------------------- */
                item {
                    SectionCard(title = "환경설정") {
                        SettingSwitchRow(
                            leadingIcon = Icons.Default.NotificationsActive,
                            title = "푸시 알림",
                            subtitle = "소식·이벤트·약속 알림 받기",
                            checked = pushEnabled,
                            onCheckedChange = { on ->
                                pushEnabled = on
                                scope.launch {
                                    host.showSnackbar(
                                        if (on) "알림을 켰어요." else "알림을 껐어요.",
                                        withDismissAction = true
                                    )
                                }
                            },
                            trailingAccessory = {
                                TextButton(onClick = { openAppNotificationSettings(context) }) {
                                    Text("시스템 설정", color = color.primary)
                                }
                            }
                        )

                        Divider(thickness = 0.8.dp, color = color.outlineVariant)

                        SettingSwitchRow(
                            leadingIcon = Icons.Default.Brightness4,
                            title = "다크 모드",
                            subtitle = "어두운 테마 사용",
                            checked = darkEnabled,
                            onCheckedChange = { on ->
                                darkEnabled = on
                                scope.launch {
                                    host.showSnackbar(
                                        if (on) "다크 모드를 켰어요." else "다크 모드를 껐어요.",
                                        withDismissAction = true
                                    )
                                }
                            }
                        )
                    }
                }

                /* -------------------- 앱 정보 -------------------- */
                item {
                    SectionCard(title = "앱 정보") {
                        SettingClickableRow(
                            leadingIcon = Icons.Default.Info,
                            title = "버전 정보",
                            subtitle = "현재 설치된 앱 버전",
                            trailing = {
                                Text(
                                    text = getAppVersion(context),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = color.onSurfaceVariant
                                )
                            },
                            onClick = { /* no-op */ }
                        )

                        Divider(thickness = 0.8.dp, color = color.outlineVariant)

                        SettingClickableRow(
                            leadingIconPainter = painterResource(R.drawable.baseline_settings_24),
                            title = "앱 설정(시스템)",
                            subtitle = "권한·저장공간·배터리 최적화",
                            trailing = { Icon(Icons.Outlined.ChevronRight, null, tint = color.onSurfaceVariant) }
                        ) { openAppDetailsSettings(context) }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

/* ===================== 공용 컴포넌트 ===================== */

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val color = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = color.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color.onSurface
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    leadingIcon: ImageVector? = null,
    leadingIconPainter: Painter? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    trailingAccessory: @Composable (() -> Unit)? = null
) {
    val color = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null || leadingIconPainter != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = color.onSurfaceVariant)
                } else if (leadingIconPainter != null) {
                    Image(painter = leadingIconPainter, contentDescription = null)
                }
            }
            Spacer(Modifier.width(12.dp))
        }

        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color.onSurface)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = color.onSurfaceVariant)
            }
        }

        if (trailingAccessory != null) {
            Spacer(Modifier.width(8.dp))
            trailingAccessory()
            Spacer(Modifier.width(4.dp))
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = color.onPrimary,
                checkedTrackColor = color.primary,
                uncheckedThumbColor = color.onSurface,
                uncheckedTrackColor = color.outlineVariant
            )
        )
    }
}

@Composable
private fun SettingClickableRow(
    leadingIcon: ImageVector? = null,
    leadingIconPainter: Painter? = null,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val color = MaterialTheme.colorScheme
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(containerColor = color.surfaceColorAtElevation(1.dp)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (leadingIcon != null || leadingIconPainter != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, contentDescription = null, tint = color.onSurfaceVariant)
                    } else if (leadingIconPainter != null) {
                        Image(painter = leadingIconPainter, contentDescription = null)
                    }
                }
                Spacer(Modifier.width(12.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color.onSurface)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = color.onSurfaceVariant)
                }
            }

            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/* ===================== Utils ===================== */

private fun getAppVersion(context: Context): String =
    try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val vn = if (Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode.toString() else pInfo.versionCode.toString()
        "${pInfo.versionName} ($vn)"
    } catch (_: Exception) { "1.0.0" }

private fun openAppNotificationSettings(context: Context) {
    try {
        val intent = Intent().apply {
            action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Settings.ACTION_APP_NOTIFICATION_SETTINGS
            } else {
                "android.settings.APP_NOTIFICATION_SETTINGS"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                putExtra("app_package", context.packageName)
                putExtra("app_uid", context.applicationInfo.uid)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppDetailsSettings(context)
    }
}

private fun openAppDetailsSettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    } catch (_: Exception) { /* no-op */ }
}

private fun openWeb(context: Context, url: String) {
    try {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(i)
    } catch (_: Exception) { /* no-op */ }
}
