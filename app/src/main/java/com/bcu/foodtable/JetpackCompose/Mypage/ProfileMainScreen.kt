@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bcu.foodtable.JetpackCompose.Mypage

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.Mypage.Setting.SettingActivity
import com.bcu.foodtable.R
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.unit.Dp
import com.bcu.foodtable.JetpackCompose.coach.CoachScreen
import com.bcu.foodtable.JetpackCompose.coach.CoachScrimColor
import com.bcu.foodtable.JetpackCompose.coach.CoachStep
import com.bcu.foodtable.JetpackCompose.coach.CoachTargets
import com.bcu.foodtable.JetpackCompose.coach.CoachmarkOverlay
import com.bcu.foodtable.JetpackCompose.coach.CoachmarkStoreDataStore
import com.bcu.foodtable.JetpackCompose.coach.coachTarget
import com.bcu.foodtable.JetpackCompose.coach.CoachTour
import com.unity3d.player.UnityPlayerGameActivity
import kotlinx.coroutines.launch
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
    val cs = MaterialTheme.colorScheme

    val user by viewModel.user.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val editedDescription by viewModel.editedDescription.collectAsState()

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val coachBringer = remember { BringIntoViewRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadImageToFirebase(it, context) } }

    val store = remember { CoachmarkStoreDataStore(context) }
    val targets = remember { CoachTargets() }
    var showCoach by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { viewModel.checkIfChannelExists() }

    Scaffold(containerColor = cs.background) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .bringIntoViewRequester(coachBringer)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                // 상단 우측 아이콘들
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (isEditing) viewModel.cancelEdit() else viewModel.startEdit()
                            },
                            modifier = Modifier.coachTarget(
                                id = "prof_edit",
                                targets = targets,
                                bringer = coachBringer,
                                expandPx = 12f
                            )
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Rounded.Close else Icons.Rounded.Edit,
                                contentDescription = if (isEditing) "편집 취소" else "편집",
                                tint = cs.primary
                            )
                        }
                        IconButton(onClick = {
                            context.startActivity(Intent(context, SettingActivity::class.java))
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_settings_24),
                                contentDescription = "설정",
                                tint = cs.primary
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 아바타
                    AsyncImage(
                        model = imageUri ?: user.image,
                        contentDescription = "프로필 이미지",
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.baseline_person_24),
                        error = painterResource(id = R.drawable.baseline_person_24),
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(cs.primary.copy(alpha = 0.12f))
                            .clickable { imagePicker.launch("image/*") }
                            .coachTarget(
                                id = "prof_avatar",
                                targets = targets,
                                bringer = coachBringer,
                                expandPx = 16f
                            )
                    )

                    Spacer(Modifier.height(14.dp))

                    // 이름
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = cs.onSurface
                    )

                    Spacer(Modifier.height(8.dp))
                    Divider(color = cs.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(8.dp))

                    // 자기소개
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedDescription,
                            onValueChange = { viewModel.editedDescription.value = it },
                            label = { Text("자기소개") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp, max = 160.dp)
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .onFocusEvent {
                                    if (it.isFocused) scope.launch { bringIntoViewRequester.bringIntoView() }
                                },
                            maxLines = 6,
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { })
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            text = user.description.ifBlank { "자기소개가 없습니다." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(16.dp))

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
                }
            }
        }
    }

    // 코치마크 오버레이
    if (showCoach) {
        CoachmarkOverlay(
            screen = CoachScreen.PROFILE,
            steps = listOf(
                CoachStep("prof_avatar", "프로필 사진", "탭하여 변경할 수 있어요."),
                CoachStep("prof_edit", "프로필 편집", "소개를 수정하고 저장할 수 있습니다."),
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
                parentOverlayActiveChange(isActive)   // 부모(HomeScreen)로 릴레이
            },
            scrim  = CoachScrimColor
        )
    }
}

/** 소금페이 */
@Composable
private fun SaltPayCard(
    balanceText: String,
    onPayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surface,
        tonalElevation = 2.dp,
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

/** 건강 | 밥상 | 냉장 */
@Composable
private fun SegmentedTripleRow(
    onHealth: () -> Unit,
    onBapsang: (() -> Unit)? = null,
    onFridge: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cs.surface,
        tonalElevation = 1.dp,
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
                icon = { Icon(Icons.Rounded.Favorite, contentDescription = "건강", tint = cs.primary) },
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
                icon = { Icon(Icons.Rounded.Restaurant, contentDescription = "밥상", tint = cs.primary) },
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
                icon = { Icon(Icons.Rounded.Kitchen, contentDescription = "냉장", tint = cs.primary) },
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

@Composable
private fun QrPayCard(
    onQrPayClick: () -> Unit,
    qrButtonModifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surface,
        tonalElevation = 2.dp,
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
