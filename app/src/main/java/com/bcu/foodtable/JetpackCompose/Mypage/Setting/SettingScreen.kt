package com.bcu.foodtable.JetpackCompose.Mypage.Setting

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsGymnastics
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

/* ──────────────────────────────────────────────────────────
 * 실제 화면 본문: 기존 내용은 여기로 이동
 * ────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingScreenContent(
    context: Context,
    viewModel: SettingViewModel,
    onRequestPermissions: () -> Unit
) {
    val color = MaterialTheme.colorScheme
    val healthGranted by viewModel.healthPermissionGranted.collectAsState()
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 데모 상태(실사용에선 DataStore/VM과 연결)
    var pushEnabled by rememberSaveable { mutableStateOf(false) }
    var darkEnabled by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(host) },
        topBar = {
            TopAppBar(
                title = { Text("설정", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = color.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = color.surface,
                    titleContentColor = color.onSurface
                )
            )
        }
    ) { padding ->
        // 부드러운 그라데이션 배경 (테마 기반)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            color.surface,
                            color.surfaceColorAtElevation(2.dp)
                        )
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // -------- 섹션: 연동 --------
                item {
                    SectionCard(title = "연동") {
                        SettingSwitchRow(
                            leadingIcon = Icons.Default.SportsGymnastics,
                            title = "헬스 권한",
                            subtitle = "Health Connect 연동으로 건강 데이터 사용",
                            checked = healthGranted,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
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
                            trailing = { Icon(Icons.Outlined.NavigateNext, null, tint = color.onSurfaceVariant) },
                        ) {
                            openWeb(context, "https://your.privacy.policy.link")
                        }
                    }
                }

                // -------- 섹션: 환경설정 --------
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
                                // 실제 테마 토글은 App Theme 래퍼와 DataStore 연동 권장
                            }
                        )
                    }
                }

                // -------- 섹션: 앱 정보 --------
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
                            trailing = { Icon(Icons.Outlined.ChevronRight, null, tint = color.onSurfaceVariant) },
                        ) { openAppDetailsSettings(context) }
                    }
                }

                // -------- 로그아웃 --------
                item {
                    DestructiveActionCard(
                        text = "로그아웃",
                        onClick = { viewModel.logoutAndNavigate(context) }
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

/* ===================== Composables ===================== */

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

@Composable
private fun DestructiveActionCard(
    text: String,
    onClick: () -> Unit
) {
    val color = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = color.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(color.error, color.error.copy(alpha = 0.75f))
                    ),
                    RoundedCornerShape(14.dp)
                ),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = color.onError)
            Spacer(Modifier.width(8.dp))
            Text(text, color = color.onError, fontWeight = FontWeight.SemiBold)
        }
    }
}

/* ===================== Utils ===================== */

private fun getAppVersion(context: Context): String =
    try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val vn = if (Build.VERSION.SDK_INT >= 28) pInfo.longVersionCode.toString() else pInfo.versionCode.toString()
        "${pInfo.versionName} ($vn)"
    } catch (_: Exception) {
        "1.0.0"
    }

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
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(i)
    } catch (_: Exception) { /* no-op */ }
}
