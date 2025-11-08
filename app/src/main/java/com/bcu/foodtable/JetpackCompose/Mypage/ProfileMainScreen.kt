@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bcu.foodtable.JetpackCompose.Mypage

import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bcu.foodtable.JetpackCompose.Mypage.Setting.SettingActivity
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.bcu.foodtable.JetpackCompose.coach.CoachScreen
import com.bcu.foodtable.JetpackCompose.coach.CoachScrimColor
import com.bcu.foodtable.JetpackCompose.coach.CoachStep
import com.bcu.foodtable.JetpackCompose.coach.CoachTargets
import com.bcu.foodtable.JetpackCompose.coach.CoachTour
import com.bcu.foodtable.JetpackCompose.coach.CoachmarkOverlay
import com.bcu.foodtable.JetpackCompose.coach.CoachmarkStoreDataStore
import com.bcu.foodtable.JetpackCompose.coach.coachTarget
import com.unity3d.player.UnityPlayerGameActivity

// ZXing (QR 생성)
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileMainScreen(
    paddingValues: PaddingValues,
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(),
    parentOverlayActiveChange: (Boolean) -> Unit = {},
    bottomObstructionDp: Dp = 0.dp
) {
    val context = LocalContext.current
    val color = MaterialTheme.colorScheme
    val user by viewModel.user.collectAsState()

    val scrollState = rememberScrollState()
    val coachBringer = remember { BringIntoViewRequester() }

    val store = remember { CoachmarkStoreDataStore(context) }
    val targets = remember { CoachTargets() }
    var showCoach by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { viewModel.checkIfChannelExists() }

    Scaffold(containerColor = color.background) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .bringIntoViewRequester(coachBringer)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 부모 카드 배경: 살구톤(스샷 느낌)
            val cardBg = color.primary.copy(alpha = 0.12f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                // 상단 우측: "MY" 버튼
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            context.startActivity(Intent(context, SettingActivity::class.java))
                        }) {
                            Text(
                                "MY",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 소금페이
                    SaltPayCard(
                        balanceText = "%,d G".format((user.point ?: 0)),
                        onPayClick = { viewModel.navigateToPurchase(context) },
                        modifier = Modifier.coachTarget(
                            id = "prof_salt",
                            targets = targets,
                            bringer = coachBringer,
                            expandPx = 8f
                        )
                    )
                    Spacer(Modifier.height(12.dp))

                    // QR 결제
                    QrPayCard(
                        onQrPayClick = { navController.navigate("qrPayScanner") },
                        qrButtonModifier = Modifier.coachTarget(
                            id = "prof_qr",
                            targets = targets,
                            bringer = coachBringer,
                            expandPx = 10f
                        )
                    )
                    Spacer(Modifier.height(16.dp))

                    // 건강/밥상/냉장
                    SegmentedTripleRow(
                        onHealth = { viewModel.navigateToHealth(navController) },
                        onBapsang = {
                            val intent = Intent(context, UnityPlayerGameActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            context.startActivity(intent)
                        },
                        onFridge = { navController.navigate("fridge") }
                    )

                    Spacer(Modifier.height(12.dp))

                    // ▼▼▼ 더미 QR 미리보기 추가 (요청 섹션) ▼▼▼
                    Spacer(Modifier.height(20.dp))
                    Divider(color = color.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))

                    // 고정 더미 페이로드 (데모용)
                    val demoQrPayload = remember { "foodtable://demo/pay?uid=guest" }

                    QRPreviewCard(
                        title = "내 결제 QR",
                        subtitle = "매장에서 이 코드를 스캔해보는 테스트용",
                        qrData = demoQrPayload,
                        sizeDp = 200.dp
                    )
                    // ▲▲▲ 더미 QR 끝 ▲▲▲
                }
            }
        }
    }

    // 코치마크: prof_salt & prof_qr만 유지
    if (showCoach) {
        CoachmarkOverlay(
            screen = CoachScreen.PROFILE,
            steps = listOf(
                CoachStep("prof_salt", "소금페이", "충전하고 콘텐츠를 구매해 보세요."),
                CoachStep("prof_qr", "QR 결제", "코드를 스캔해 간편 결제!")
            ),
            targets = targets,
            store = store,
            bottomObstructionDp = bottomObstructionDp,
            onClose = {
                showCoach = false
                if (CoachTour.running.value == true && CoachTour.currentScreen.value == CoachScreen.PROFILE) {
                    CoachTour.next(navController, context, store)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(999f),
            onOverlayActiveChange = { isActive ->
                Log.i("COACH_OVERLAY", "Profile onOverlayActiveChange=$isActive")
                parentOverlayActiveChange(isActive)
            },
            scrim = CoachScrimColor
        )
    }
}

/** ───────────────────────── 소금페이 카드 ───────────────────────── */
@Composable
private fun SaltPayCard(
    balanceText: String,
    onPayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent, // ← 부모(살구톤) 그대로 보이게
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("소금페이", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(cs.outline.copy(alpha = 0.35f))
                )
                Spacer(Modifier.width(8.dp))
                val amount = balanceText.removeSuffix(" G")
                Text(
                    text = buildAnnotatedString {
                        append(amount)
                        withStyle(SpanStyle(fontSize = 14.sp, color = cs.onSurfaceVariant)) {
                            append(" G")
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                    color = cs.primary,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onPayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary
                )
            ) { Text("충전") }
        }
    }
}

/** ───────────────────────── QR 결제 카드 ───────────────────────── */
@Composable
private fun QrPayCard(
    onQrPayClick: () -> Unit,
    qrButtonModifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent, // ← 부모(살구톤) 그대로 보이게
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("QR 결제", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(cs.outline.copy(alpha = 0.35f))
                )
                Spacer(Modifier.width(8.dp))
                Text("스캔하여 결제", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onQrPayClick,
                modifier = qrButtonModifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Filled.QrCode2, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("QR 결제하기")
            }
        }
    }
}

/** ──────────────────────── 건강 | 밥상 | 냉장 ─────────────────────── */
@Composable
private fun SegmentedTripleRow(
    onHealth: () -> Unit,
    onBapsang: (() -> Unit)? = null,
    onFridge: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent, // ← 부모(살구톤) 그대로 보이게
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .height(68.dp)
                .fillMaxWidth()
        ) {
            SegmentCell(
                title = "건강",
                icon = { Icon(Icons.Filled.Favorite, contentDescription = "건강", tint = cs.primary) },
                onClick = onHealth,
                enabled = true,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(cs.outline.copy(alpha = 0.25f))
            )
            val bapsangEnabled = onBapsang != null
            SegmentCell(
                title = "밥상",
                icon = { Icon(Icons.Filled.Restaurant, contentDescription = "밥상", tint = cs.primary) },
                onClick = { onBapsang?.invoke() },
                enabled = bapsangEnabled,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(cs.outline.copy(alpha = 0.25f))
            )
            SegmentCell(
                title = "냉장",
                icon = { Icon(Icons.Filled.Kitchen, contentDescription = "냉장", tint = cs.primary) },
                onClick = onFridge,
                enabled = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegmentCell(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val alpha = if (enabled) 1f else 0.4f
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalContentColor provides cs.onSurface.copy(alpha = alpha)) {
            icon()
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurface.copy(alpha = alpha)
            )
        }
    }
}

/** ──────────────────────── 보이는 QR 카드 (더미) ─────────────────────── */
@Composable
private fun QRPreviewCard(
    title: String,
    subtitle: String? = null,
    qrData: String,
    sizeDp: Dp = 200.dp
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))

            val bitmap by remember(qrData, sizeDp) {
                mutableStateOf(makeQrBitmap(qrData, sizeDp))
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(sizeDp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(sizeDp)
                        .background(cs.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("QR 생성 실패", color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

private fun makeQrBitmap(data: String, sizeDp: Dp): Bitmap? {
    return try {
        val density = android.content.res.Resources.getSystem().displayMetrics.density
        val sizePx = (sizeDp.value * density).toInt()
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        bmp
    } catch (_: Exception) {
        null
    }
}
