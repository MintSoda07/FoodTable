    package com.bcu.foodtable.JetpackCompose.Mypage

    import androidx.compose.foundation.Image
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.EmojiEvents
    import androidx.compose.material.icons.outlined.AccountCircle
    import androidx.compose.material3.*
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.vector.rememberVectorPainter
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import coil.compose.AsyncImage
    import com.bcu.foodtable.R
    import com.bcu.foodtable.useful.User

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ProfileMainScreen(
        user: User,
        paddingValues: PaddingValues,
        onEditClick: () -> Unit = {},
        onPurchaseClick: () -> Unit = {},
        onCreateChannelClick: () -> Unit = {},
        onHealthCheckClick: () -> Unit = {}
    ) {
        val colorScheme = MaterialTheme.colorScheme

        Scaffold(
            topBar = {
                // ✅ 여기에만 ProfileTopBar 호출
                ProfileTopBar(user = user, onChallengeClick = onHealthCheckClick)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ){
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
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 프로필 이미지
                            AsyncImage(
                                model = user.image,
                                contentDescription = "프로필 이미지",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primary.copy(alpha = 0.2f)),
                                placeholder = painterResource(id = R.drawable.baseline_person_24), // 기본 이미지

                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = colorScheme.onSurface
                                )
                            )

                            Text(
                                text = user.description.ifBlank { "자기소개가 없습니다." },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colorScheme.onSurfaceVariant
                                )
                            )

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
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                OutlinedButton(
                                    onClick = onEditClick,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = colorScheme.primary
                                    )
                                ) {
                                    Text("편집")
                                }

                                Button(
                                    onClick = onPurchaseClick,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    )
                                ) {
                                    Text("소금 구매")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onCreateChannelClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.secondary,
                                    contentColor = colorScheme.onSecondary
                                )
                            ) {
                                Text("채널 생성하기")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onHealthCheckClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.tertiary,
                                    contentColor = colorScheme.onTertiary
                                )
                            ) {
                                Text("건강 확인하기")
                            }
                        }
                    }
                }
                }
    }

