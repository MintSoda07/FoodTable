package com.bcu.foodtable.JetpackCompose.Mypage

import androidx.compose.runtime.Composable
import com.bcu.foodtable.useful.User

@Composable
fun ProfileScreen(
    user: User,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onPickImageClick: () -> Unit,
    onPurchaseClick: () -> Unit,
    onCreateChannelClick: () -> Unit
) {
    // 위에서 만든 UI 그대로 사용하면 됨 (이전 메시지 참고)
}
