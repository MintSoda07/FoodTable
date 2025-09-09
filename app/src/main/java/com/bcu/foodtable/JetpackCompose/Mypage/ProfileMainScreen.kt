package com.bcu.foodtable.JetpackCompose.Mypage

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bcu.foodtable.JetpackCompose.Mypage.Setting.SettingActivity
import com.bcu.foodtable.R
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import com.unity3d.player.UnityPlayerGameActivity
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileMainScreen(
    paddingValues: PaddingValues,
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme


    val user by viewModel.user.collectAsState()

    val imageUri by viewModel.imageUri.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val editedDescription by viewModel.editedDescription.collectAsState()
    val scrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.uploadImageToFirebase(it, context) } }

    LaunchedEffect(Unit) { viewModel.checkIfChannelExists() }

    Scaffold(containerColor = cs.background) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scrollState)  //
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                // 카드 우상단 아이콘(편집 ← 설정)
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (isEditing) viewModel.cancelEdit() else viewModel.startEdit()
                        }) {
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

                    // 자기소개 (편집 가능)
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedDescription,
                            onValueChange = { viewModel.editedDescription.value = it },
                            label = { Text("자기소개") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp, max = 160.dp) // ⬅ 너무 커지지 않도록 상한
                                .bringIntoViewRequester(bringIntoViewRequester)
                                .onFocusEvent { if (it.isFocused) scope.launch { bringIntoViewRequester.bringIntoView() } },
                            maxLines = 6,          // ⬅ 줄 수 제한
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // 필요 시 저장/포커스 해제 등
                                    // viewModel.saveChanges()
                                    // LocalFocusManager.current.clearFocus()
                                }
                            )
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

                    // ───────── 소금페이 영역 ─────────
                    SaltPayCard(
                        // 기존 user.point
                        // 사용, null 안전 처리 및 천단위 포맷
                        balanceText = "%,d G".format((user.point ?: 0)),
                        onPayClick = { viewModel.navigateToPurchase(context) }
                    )

                    Spacer(Modifier.height(16.dp))

                    // ───────── 건강 | 밥상 | 냉장 (연결된 바 + 실선 구분) ─────────
                    SegmentedTripleRow(
                        onHealth = { viewModel.navigateToHealth(navController) },
                        onBapsang = {
                            val intent = Intent(context, UnityPlayerGameActivity::class.java)
                            context.startActivity(intent)
                        }, // TODO: 밥상 라우트 구현되면 예: { navController.navigate("table") }
                        onFridge = { navController.navigate("fridge") }
                    )

                    Spacer(Modifier.height(12.dp))

                    /* 채널 생성은 다른 화면에서 처리 예정
                    if (!hasChannel) {
                        OutlinedButton(
                            onClick = { viewModel.navigateToChannelCreation(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("채널 생성하기") }
                        Spacer(Modifier.height(12.dp))
                    }
                    */

                }
            }
        }
    }
}

/** 소금페이: 제목 + 잔액 + 전폭 '결제' 바 */
@Composable
private fun SaltPayCard(
    balanceText: String,
    onPayClick: () -> Unit
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

            // 소금페이 관련 위치 조정
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "소금페이",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface
                )

                Spacer(Modifier.weight(1f))

                // 얇은 세로 실선
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(cs.outline.copy(alpha = 0.35f))
                )

                Spacer(Modifier.width(8.dp))

                val amount = balanceText.removeSuffix(" G") //
                Text(
                    text = buildAnnotatedString {
                        append(amount) // 숫자 크게
                        withStyle(SpanStyle(fontSize = 14.sp, color = cs.onSurfaceVariant)) {
                            append(" G") // 단위 작게
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
            ) { Text("결제") } // 라벨
        }
    }
}
/** 배민 마이페이지 느낌의 연결형 3분할 세그먼트(가운데는 미구현 비활성) */
@Composable
private fun SegmentedTripleRow(
    onHealth: () -> Unit,
    onBapsang: (() -> Unit)? = null, // null -> 비활성
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

            // 세로 실선
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
                onClick = { onBapsang?.invoke() }, // TODO: 연결 예정
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
